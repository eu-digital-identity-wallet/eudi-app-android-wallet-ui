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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import eu.europa.ec.corelogic.controller.WalletCoreDCAPIRegistrationController
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.model.RevokedDocumentDataDomain
import eu.europa.ec.corelogic.util.CoreActions
import eu.europa.ec.corelogic.util.CoreActions.REVOCATION_IDS_DETAILS_EXTRA
import eu.europa.ec.eudi.statium.Status
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.storagelogic.dao.RevokedDocumentDao
import eu.europa.ec.storagelogic.dao.StatusRefreshFailureDao
import eu.europa.ec.storagelogic.model.RevocationStatus
import eu.europa.ec.storagelogic.model.RevokedDocument
import eu.europa.ec.storagelogic.model.StatusRefreshFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [RevocationWorkManager] is a [CoroutineWorker] that checks the status of every issued document
 * and keeps the app's revocation records in step with it.
 *
 * For each document it resolves the status through
 * [WalletCoreDocumentsController.resolveDocumentStatus] and applies [revocationUpdateFor]:
 *  - [Status.Invalid] is recorded as [RevocationStatus.INVALID] and is permanent; a later
 *    [Status.Valid] does not clear it.
 *  - [Status.Suspended] is recorded as [RevocationStatus.SUSPENDED] and is cleared by a later
 *    [Status.Valid].
 *  - [Status.ApplicationSpecific] and [Status.Reserved] values, which the wallet cannot interpret,
 *    are recorded as [RevocationStatus.UNRECOGNISED] and block the document until a later
 *    [Status.Valid].
 *
 * Any record blocks presentation of the document. When a document that has a status reference
 * cannot be resolved (the status list could not be fetched, parsed or verified), the failure is
 * stored in [StatusRefreshFailureDao] so that document details can say the status could not be
 * refreshed; the next successful check removes it. A failure does not block the document.
 *
 * Broadcasts:
 *  - `CoreActions.REVOCATION_WORK_MESSAGE_ACTION`: the documents blocked for the first time, as
 *    [RevokedDocumentDataDomain] names and IDs.
 *  - `CoreActions.REVOCATION_WORK_REFRESH_DETAILS_ACTION`: the IDs of every document whose record
 *    or refresh-failure state changed, in `REVOCATION_IDS_DETAILS_EXTRA`.
 *  - `CoreActions.REVOCATION_WORK_REFRESH_ACTION`: a general refresh when any record changed.
 *
 * Runs one at a time within the process, since the periodic run and the run after login can
 * overlap.
 *
 * The worker returns [Result.success] after a full pass, and [Result.failure] if an
 * [IllegalArgumentException] occurred, which indicates a configuration or data issue.
 *
 * @param appContext The application context.
 * @param workerParams Parameters for the worker.
 */
class RevocationWorkManager(
    appContext: Context,
    workerParams: WorkerParameters,
    private val revokedDocumentDao: RevokedDocumentDao,
    private val statusRefreshFailureDao: StatusRefreshFailureDao,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val walletCoreDCAPIRegistrationController: WalletCoreDCAPIRegistrationController,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val REVOCATION_WORK_NAME = "revocationWorker"
        const val REVOCATION_ON_START_WORK_NAME = "revocationOnStartWorker"
        private const val MAX_FAILURE_REASON_LENGTH = 300

        /** Held while the worker runs, so that the factory reset can wait until it has finished. */
        internal val runLock = Mutex()
    }

    override suspend fun doWork(): Result {
        val result = runLock.withLock { checkStatuses() }
        updateRegistryIfBlockedChanged()
        return result
    }

    /**
     * Updates the Digital Credential API registry when the blocked documents differ from those it
     * last left out. It runs outside [runLock], so that the factory reset does not wait for the
     * registry, and it compares with the registry rather than remembering a change, so a run that
     * was stopped after writing a record is caught by the next one.
     */
    private suspend fun updateRegistryIfBlockedChanged() {
        val blockedIds = try {
            revokedDocumentDao.retrieveAll().map { it.identifier }.toSet()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return
        }
        if (blockedIds != walletCoreDCAPIRegistrationController.excludedDocumentIds) {
            walletCoreDCAPIRegistrationController.registerCredentials()
        }
    }

    private suspend fun checkStatuses(): Result {
        try {

            val storedStatuses = revokedDocumentDao.retrieveAll()
                .associate { it.identifier to it.status }
            val previouslyFailedIds = statusRefreshFailureDao.retrieveAll()
                .map { it.identifier }
                .toSet()
            val newlyBlockedDocuments = mutableListOf<IssuedDocument>()
            val changedDocumentIds = mutableListOf<String>()

            walletCoreDocumentsController
                .getAllIssuedDocuments()
                .forEach { document ->
                    walletCoreDocumentsController.resolveDocumentStatus(document).fold(
                        onSuccess = { status ->
                            if (document.id in previouslyFailedIds) {
                                statusRefreshFailureDao.delete(document.id)
                                changedDocumentIds.add(document.id)
                            }

                            val storedStatus = storedStatuses[document.id]
                            when (val update = revocationUpdateFor(storedStatus, status)) {
                                is RevocationUpdate.Keep -> Unit

                                is RevocationUpdate.Clear -> {
                                    revokedDocumentDao.delete(document.id)
                                    changedDocumentIds.add(document.id)
                                }

                                is RevocationUpdate.Record -> {
                                    revokedDocumentDao.store(
                                        RevokedDocument(
                                            identifier = document.id,
                                            status = update.status,
                                        )
                                    )
                                    changedDocumentIds.add(document.id)
                                    if (storedStatus == null) {
                                        newlyBlockedDocuments.add(document)
                                    }
                                }
                            }
                        },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            val recorded = recordRefreshFailure(document, error)
                            if (recorded && document.id !in previouslyFailedIds) {
                                changedDocumentIds.add(document.id)
                            }
                        }
                    )
                }

            if (newlyBlockedDocuments.isNotEmpty()) {
                sendRevocationMessage(newlyBlockedDocuments)
            }

            if (changedDocumentIds.isNotEmpty()) {
                sendDetailsRefresh(changedDocumentIds.distinct())
                notifyDocumentsList()
            }

            return Result.success()
        } catch (_: IllegalArgumentException) {
            return Result.failure()
        }
    }

    /**
     * Stores a failed refresh for [document], unless it has no status reference and therefore no
     * status to refresh. Returns whether a failure was stored.
     */
    private suspend fun recordRefreshFailure(document: IssuedDocument, error: Throwable): Boolean {
        if (!walletCoreDocumentsController.hasStatusReference(document)) return false

        val reason = (error.message ?: error::class.simpleName.orEmpty())
            .take(MAX_FAILURE_REASON_LENGTH)

        statusRefreshFailureDao.store(
            StatusRefreshFailure(
                identifier = document.id,
                failedAt = System.currentTimeMillis(),
                reason = reason,
            )
        )
        return true
    }

    private fun sendRevocationMessage(revokedDocuments: List<IssuedDocument>) {
        val messageIntent = Intent(CoreActions.REVOCATION_WORK_MESSAGE_ACTION).apply {
            setPackage(applicationContext.packageName)
            putParcelableArrayListExtra(
                CoreActions.REVOCATION_IDS_EXTRA,
                ArrayList(
                    revokedDocuments.map {
                        RevokedDocumentDataDomain(name = it.name, id = it.id)
                    }
                )
            )
        }
        applicationContext.sendBroadcast(messageIntent)
    }

    private fun sendDetailsRefresh(documentIds: List<String>) {
        val detailsIntent = Intent(CoreActions.REVOCATION_WORK_REFRESH_DETAILS_ACTION).apply {
            setPackage(applicationContext.packageName)
            putStringArrayListExtra(
                REVOCATION_IDS_DETAILS_EXTRA,
                ArrayList(documentIds)
            )
        }
        applicationContext.sendBroadcast(detailsIntent)
    }

    private fun notifyDocumentsList() {
        val refreshIntent = Intent(CoreActions.REVOCATION_WORK_REFRESH_ACTION).apply {
            setPackage(applicationContext.packageName)
        }
        applicationContext.sendBroadcast(refreshIntent)
    }
}

/**
 * What to do with a document's revocation record after reading its status.
 */
internal sealed interface RevocationUpdate {
    data object Keep : RevocationUpdate
    data object Clear : RevocationUpdate
    data class Record(val status: RevocationStatus) : RevocationUpdate
}

/**
 * The change to a document's revocation record, given what is stored ([stored], null when there
 * is no record) and the status just read ([observed]). A stored [RevocationStatus.INVALID] never
 * changes. A [Status.Valid] clears a suspended or unrecognised record. Any other status is
 * recorded unless the same status is already stored.
 */
internal fun revocationUpdateFor(stored: RevocationStatus?, observed: Status): RevocationUpdate {
    val target = when (observed) {
        is Status.Valid -> return when (stored) {
            RevocationStatus.SUSPENDED, RevocationStatus.UNRECOGNISED -> RevocationUpdate.Clear
            RevocationStatus.INVALID, null -> RevocationUpdate.Keep
        }

        is Status.Invalid -> RevocationStatus.INVALID
        is Status.Suspended -> RevocationStatus.SUSPENDED
        is Status.ApplicationSpecific, is Status.Reserved -> RevocationStatus.UNRECOGNISED
    }

    return if (stored == RevocationStatus.INVALID || stored == target) {
        RevocationUpdate.Keep
    } else {
        RevocationUpdate.Record(target)
    }
}