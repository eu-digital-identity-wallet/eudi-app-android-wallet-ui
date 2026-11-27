/*
 * Copyright (c) 2026 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package eu.europa.ec.corelogic.worker

import android.content.Context
import android.content.Intent
import eu.europa.ec.authenticationlogic.controller.session.SessionController
import eu.europa.ec.corelogic.config.DeferredIssuanceConfig
import eu.europa.ec.corelogic.controller.DeleteDocumentPartialState
import eu.europa.ec.corelogic.controller.IssueDeferredDocumentPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.controller.WalletDocumentOperationController
import eu.europa.ec.corelogic.controller.WalletResetStatus
import eu.europa.ec.corelogic.model.DeferredDocumentEventDomain
import eu.europa.ec.corelogic.model.DeferredDocumentRemovalReasonDomain
import eu.europa.ec.corelogic.util.CoreActions
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.storagelogic.dao.DeferredDocumentScheduleDao
import eu.europa.ec.storagelogic.model.DeferredDocumentSchedule
import eu.europa.ec.storagelogic.model.DeferredDocumentScheduleStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/** Schedules persisted Deferred deadlines; tracked attempts run independently of its timer. */
interface DeferredIssuanceScheduler {
    /** Retains successful Deferred IDs until Documents shows them, even with no broadcast receiver. */
    val unseenDeferredIssuanceIds: StateFlow<Set<String>>
    fun markDeferredIssuanceSeen(documentId: String)

    /** Activates the current wallet lifecycle from an unlocked Dashboard; repeated calls reuse the observer. */
    fun start()

    /** Includes retryable failures observed in this process and durable terminal states from Room. */
    suspend fun getFailedDocumentIds(): Set<String>
}

class DeferredIssuanceSchedulerImpl(
    private val scheduleDao: DeferredDocumentScheduleDao,
    private val walletDocumentOperationController: WalletDocumentOperationController,
    private val sessionController: SessionController,
    private val resetStatus: WalletResetStatus,
    private val config: DeferredIssuanceConfig,
    private val getDocumentsController: () -> WalletCoreDocumentsController,
    private val context: Context,
    private val now: () -> Long,
    private val coroutineScope: CoroutineScope,
) : DeferredIssuanceScheduler {

    override val unseenDeferredIssuanceIds: StateFlow<Set<String>>
        get() = walletDocumentOperationController.unseenDeferredIssuanceIds

    override fun markDeferredIssuanceSeen(documentId: String) =
        walletDocumentOperationController.markDeferredIssuanceSeen(documentId)

    private val policy = DeferredRetryPolicy(config)

    // Process-local backoff when storage or result handling fails.
    private val infrastructureRetryAt = MutableStateFlow(emptyMap<String, Long>())
    private var scheduling: Job? = null

    // Timer restarts await the same recovery operation, including one already in progress.
    private var reconciliation: Deferred<Unit>? = null

    // Reset invalidates the previous Dashboard activation even while this observer remains alive.
    private var generation = -1L
    private val dashboardGeneration = MutableStateFlow<Long?>(null)

    // A recovery restart must not repeat a removal already attempted in this wallet lifecycle.
    private val recoveryRemovalAttempts = mutableSetOf<String>()

    @Synchronized
    override fun start() {
        // Refresh activation before reusing an observer that survived reset.
        dashboardGeneration.value = walletDocumentOperationController.state.value.walletGeneration
        if (scheduling?.isActive == true) return
        scheduling = coroutineScope.launch {
            while (isActive) {
                try {
                    observeSchedule()
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    // A transient local storage failure must not turn into a hot polling loop.
                    delay(config.failureRetryDelay)
                }
            }
        }
    }

    private suspend fun observeSchedule() {
        combine(
            combine(scheduleDao.observeAll(), infrastructureRetryAt) { rows, _ -> rows },
            walletDocumentOperationController.state,
            sessionController.unlockedState,
            resetStatus.isResetting,
            dashboardGeneration,
        ) { rows, _, _, _, startedGeneration -> rows to startedGeneration }
            // Changes replace the timer; tracked attempts continue in the operation controller.
            .collectLatest { (rows, startedGeneration) ->
                val currentGeneration =
                    walletDocumentOperationController.state.value.walletGeneration
                if (!walletDocumentOperationController.canPoll(documentId = null) ||
                    startedGeneration != currentGeneration
                ) return@collectLatest
                if (generation != currentGeneration) {
                    generation = currentGeneration
                    reconciliation = null
                    infrastructureRetryAt.value = emptyMap()
                    recoveryRemovalAttempts.clear()
                }
                if (reconciliation == null) {
                    reconciliation =
                        walletDocumentOperationController.startOperation(isResetOperation = false) {
                            reconcile()
                        }
                }
                try {
                    reconciliation?.await() ?: return@collectLatest
                } catch (error: Exception) {
                    // Timer cancellation leaves recovery running; a failed recovery may be retried.
                    if (error !is CancellationException) reconciliation = null
                    throw error
                }
                while (true) {
                    val operationState = walletDocumentOperationController.state.value
                    val candidates = rows.filter { row ->
                        row.status == DeferredDocumentScheduleStatus.ACTIVE &&
                                row.identifier !in operationState.activeDocumentIds &&
                                row.identifier !in operationState.removingDocumentIds &&
                                row.identifier !in operationState.removedDocumentIds
                    }.sortedWith(compareBy({ row -> dueAt(row) }, { row -> row.identifier }))
                    val timestamp = now()
                    // Submit the oldest due entries first; freeing a slot triggers another pass.
                    for (row in candidates.takeWhile { candidate -> dueAt(candidate) <= timestamp }) {
                        if (!tryStartAttempt(documentId = row.identifier)) break
                    }
                    val next = candidates.firstOrNull { row -> dueAt(row) > timestamp }
                    if (next == null) {
                        // With no future deadline, wait for a Room or gate change instead of spinning.
                        awaitCancellation()
                    } else {
                        delay((dueAt(next) - now()).coerceAtLeast(1).milliseconds)
                    }
                }
            }
    }

    private fun tryStartAttempt(documentId: String): Boolean =
        walletDocumentOperationController.tryStartPolling(
            documentId = documentId,
            limit = config.maxConcurrentDeferredRequests,
        ) {
            try {
                val shouldRemove = walletDocumentOperationController.withDocumentLock(documentId) {
                    attempt(documentId)
                }
                // Trust cleanup takes the same removal lock as user Remove.
                if (shouldRemove) removeUntrustedDocument(documentId)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                postponeAfterLocalFailure(documentId)
            }
        }

    override suspend fun getFailedDocumentIds(): Set<String> {
        val terminalIds = scheduleDao.getTerminalDocumentIds()
        return walletDocumentOperationController.state.value.failedDeferredDocumentIds + terminalIds
    }

    /** Local backoff wakes the timer even when a failed lookup or write produced no Room emission. */
    private fun postponeAfterLocalFailure(documentId: String) {
        val retryAt = now() + config.failureRetryDelay.inWholeMilliseconds
        infrastructureRetryAt.update { deadlines -> deadlines + (documentId to retryAt) }
    }

    private fun dueAt(row: DeferredDocumentSchedule): Long =
        maxOf(row.nextAttemptAt, infrastructureRetryAt.value[row.identifier] ?: Long.MIN_VALUE)

    /** Repairs local schedules before polling, without making issuance requests to an issuer. */
    private suspend fun reconcile() {
        val controller = getDocumentsController()
        val localDocuments = controller.getAllDocuments()
        for (document in localDocuments) {
            walletDocumentOperationController.withDocumentLock(document.id) {
                if (document.id in walletDocumentOperationController.state.value.removedDocumentIds) {
                    return@withDocumentLock
                }
                // The initial list may predate issuance or Remove; re-read under the document lock.
                when (controller.getDocumentById(document.id)) {
                    // Insert-ignore keeps existing deadlines, original IDs and terminal states.
                    is DeferredDocument -> scheduleDao.store(
                        DeferredDocumentSchedule(
                            identifier = document.id,
                            nextAttemptAt = now() + config.initialPollingDelay.inWholeMilliseconds,
                            lastIntervalMillis = null,
                            originalDocumentId = null,
                        )
                    )

                    is IssuedDocument -> scheduleDao.delete(document.id)
                    else -> Unit
                }
            }
        }
        // Null/missing Core reads are ambiguous. Do not erase rows based on absence alone.
        for (row in scheduleDao.retrieveAll()) {
            if (row.status == DeferredDocumentScheduleStatus.PENDING_TRUST_REMOVAL &&
                recoveryRemovalAttempts.add(row.identifier)
            ) {
                removeUntrustedDocument(row.identifier)
            }
        }
    }

    /** Runs under the document lock. True requests trust cleanup after releasing it. */
    private suspend fun attempt(documentId: String): Boolean {
        val row = scheduleDao.retrieve(documentId) ?: return false
        if (row.status != DeferredDocumentScheduleStatus.ACTIVE ||
            !walletDocumentOperationController.canPoll(documentId) || dueAt(row) > now()
        ) return false

        infrastructureRetryAt.update { deadlines -> deadlines - documentId }
        val controller = getDocumentsController()
        when (controller.getDocumentById(documentId)) {
            is IssuedDocument -> {
                finishIssuance(documentId)
                return false
            }

            is DeferredDocument -> Unit
            else -> {
                // A null lookup can mean a local read failure. Keep the row and its UI state.
                postponeAfterLocalFailure(documentId)
                return false
            }
        }

        // If the process dies after submission, recovery will still respect this document's wait.
        scheduleDao.update(policy.prepareForAttempt(row, now()))
        when (val result = controller.issueDeferredDocument(documentId).first()) {
            is IssueDeferredDocumentPartialState.Paused -> scheduleDao.update(row)
            is IssueDeferredDocumentPartialState.Unavailable -> {
                postponeAfterLocalFailure(documentId)
                scheduleDao.update(row)
            }

            is IssueDeferredDocumentPartialState.NotReady -> {
                scheduleDao.update(policy.notReady(row, result.interval, now()))
                updateFailureIndication(documentId, failed = false)
            }

            is IssueDeferredDocumentPartialState.Failed -> {
                scheduleDao.update(policy.failed(row, now()))
                updateFailureIndication(documentId, failed = true)
            }

            is IssueDeferredDocumentPartialState.Expired -> {
                scheduleDao.update(row.copy(status = DeferredDocumentScheduleStatus.EXPIRED))
                walletDocumentOperationController.setRetryableDeferredFailure(
                    documentId,
                    failed = false
                )
                notifyDocumentChanged(DeferredDocumentEventDomain.StatusChanged(documentId))
            }

            is IssueDeferredDocumentPartialState.IssuerNotTrusted -> {
                scheduleDao.update(row.copy(status = DeferredDocumentScheduleStatus.PENDING_TRUST_REMOVAL))
                walletDocumentOperationController.setRetryableDeferredFailure(
                    documentId,
                    failed = false
                )
                if (documentId !in walletDocumentOperationController.state.value.removingDocumentIds) return true
                // The user already requested Remove. If it fails, the trust decision is still visible.
                notifyDocumentChanged(DeferredDocumentEventDomain.StatusChanged(documentId))
            }

            is IssueDeferredDocumentPartialState.Issued -> finishIssuance(documentId)
        }
        return false
    }

    private fun updateFailureIndication(documentId: String, failed: Boolean) {
        if (walletDocumentOperationController.setRetryableDeferredFailure(documentId, failed)) {
            notifyDocumentChanged(DeferredDocumentEventDomain.StatusChanged(documentId))
        }
    }

    private suspend fun finishIssuance(documentId: String) {
        scheduleDao.delete(documentId)
        walletDocumentOperationController.setRetryableDeferredFailure(documentId, failed = false)
        walletDocumentOperationController.recordDeferredIssuance(documentId)
        notifyDocumentChanged(DeferredDocumentEventDomain.Issued(documentId))
    }

    private suspend fun removeUntrustedDocument(documentId: String) {
        val controller = getDocumentsController()
        // The controller rechecks both the stored trust decision and the Core document under its lock.
        if (controller.deleteUntrustedDeferredDocument(documentId)
                .first() == DeleteDocumentPartialState.Success
        ) {
            notifyDocumentChanged(
                DeferredDocumentEventDomain.Removed(
                    documentId = documentId,
                    reason = DeferredDocumentRemovalReasonDomain.ISSUER_NOT_TRUSTED,
                )
            )
            return
        }

        walletDocumentOperationController.withDocumentLock(documentId) {
            // A failed schedule write can follow a confirmed Core deletion. Refresh Documents in both cases.
            if (documentId !in walletDocumentOperationController.state.value.removedDocumentIds &&
                controller.getDocumentById(documentId) is IssuedDocument
            ) {
                finishIssuance(documentId)
            } else {
                // A retained document stays Failed until a later launch retries trust cleanup.
                notifyDocumentChanged(DeferredDocumentEventDomain.StatusChanged(documentId))
            }
        }
    }

    private fun notifyDocumentChanged(event: DeferredDocumentEventDomain) {
        val action = when (event) {
            is DeferredDocumentEventDomain.Issued -> CoreActions.DEFERRED_DOCUMENT_ISSUED_ACTION
            is DeferredDocumentEventDomain.StatusChanged -> CoreActions.DEFERRED_DOCUMENT_STATUS_CHANGED_ACTION
            is DeferredDocumentEventDomain.Removed -> CoreActions.DEFERRED_DOCUMENT_REMOVED_ACTION
        }
        val intent = Intent(action).apply {
            setPackage(context.packageName)
            putExtra(CoreActions.DEFERRED_DOCUMENT_ID_EXTRA, event.documentId)
            if (event is DeferredDocumentEventDomain.Removed) {
                putExtra(CoreActions.DEFERRED_REMOVAL_REASON_EXTRA, event.reason.name)
            }
        }
        context.sendBroadcast(intent)
    }
}