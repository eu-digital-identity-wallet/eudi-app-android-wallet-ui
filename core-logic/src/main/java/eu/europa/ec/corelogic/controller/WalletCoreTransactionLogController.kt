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

package eu.europa.ec.corelogic.controller

import eu.europa.ec.corelogic.extension.recordedRegistrationCheckOf
import eu.europa.ec.corelogic.extension.toCommunicationMethodDomainOrNull
import eu.europa.ec.corelogic.extension.toCoreTransactionLog
import eu.europa.ec.corelogic.extension.toRecordedRegistrationCheck
import eu.europa.ec.corelogic.extension.toRegistrationStatusDomain
import eu.europa.ec.corelogic.extension.toStoredCommunicationMethod
import eu.europa.ec.corelogic.extension.toStoredRegistrationCheck
import eu.europa.ec.corelogic.extension.toStoredRegistrationFailureReason
import eu.europa.ec.corelogic.extension.toTransactionLogDomain
import eu.europa.ec.corelogic.model.CommunicationMethodDomain
import eu.europa.ec.corelogic.model.RecordedRegistrationCheckDomain
import eu.europa.ec.corelogic.model.TransactionLogDomain
import eu.europa.ec.corelogic.model.TransactionLogDomain.PresentationAction
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogger
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionEntry
import eu.europa.ec.eudi.wallet.transactionLogging.producers.presentation.PresentationLogBuilder
import eu.europa.ec.eudi.wallet.transactionLogging.toJson
import eu.europa.ec.eudi.wallet.transfer.openId4vp.TransactionDataType
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.storagelogic.dao.TransactionLogDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import eu.europa.ec.storagelogic.model.TransactionLog as TransactionStorage

/**
 * The wallet's transaction history. Wallet Core writes the entries through [TransactionLogger]. As a
 * [TransferEvent.Listener] registered after Wallet Core's own, it also records beside each presentation
 * whether the relying party's registration was verified, which Wallet Core does not keep in the entry.
 */
interface WalletCoreTransactionLogController : TransactionLogger, TransferEvent.Listener {
    suspend fun getTransactionLogs(): List<TransactionLogDomain>
    suspend fun getTransactionLog(id: String): TransactionLogDomain?

    /** Observes this presentation's deletion requests and reports, newest first. */
    fun observePresentationActions(presentationId: String): Flow<List<PresentationAction>>

    /** Returns true when saved, or false when the action is rejected. */
    suspend fun recordPresentationAction(
        parentPresentationId: String,
        entry: TransactionEntry,
        communicationMethod: CommunicationMethodDomain,
    ): Boolean

    suspend fun deleteTransactionLog(id: String)

    /**
     * Deletes every entry, after the writes already queued by [log]. Only the factory reset calls it:
     * the history is a legal record, so the app offers no other way to clear it.
     */
    suspend fun deleteAllTransactionLogs()
}

/**
 * Stores transaction logs and provides access to their history and related presentation actions.
 *
 * Saves and deletions share a [Channel] and run one at a time, in the order they were queued.
 * This prevents an earlier save from restoring a transaction that was later deleted.
 *
 * [log] queues a save and returns immediately. Recording a presentation action and deleting logs
 * wait for their queued operation to finish.
 *
 * The registration check is matched to its entry by order. When a request arrives, Wallet Core's
 * listener, registered first, writes a new pending entry through [log] before this listener sees
 * the same request; the check of that request is then recorded for that entry, once, after its
 * relying party is confirmed to be the one in the entry. An entry for which no check could be
 * matched reads as not verified. If a second entry is written before the first one's request
 * reaches this listener, as can happen when two requests are dispatched from different threads,
 * neither check is recorded. Wallet Core's `PresentationLogger.removeAllTransferEventListeners`
 * adds back only its own listener; calling it would silently drop this one.
 */
class WalletCoreTransactionLogControllerImpl(
    private val transactionLogDao: TransactionLogDao,
    private val resourceProvider: ResourceProvider,
    private val transactionDataTypes: List<TransactionDataType>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    coroutineScope: CoroutineScope = CoroutineScope(dispatcher + SupervisorJob()),
) : WalletCoreTransactionLogController {

    // Save and delete in order so an earlier save cannot restore a deleted transaction.
    private val operations = Channel<StorageOperation>(
        capacity = Channel.UNLIMITED,
        onUndeliveredElement = { operation ->
            (operation as? StorageOperation.Awaited)?.completion?.cancel()
        },
    )

    /**
     * The presentation entry Wallet Core wrote last, until the request it belongs to is seen, or
     * [PendingPresentation.Ambiguous] when a second one was written first.
     */
    private val pendingPresentation = AtomicReference<PendingPresentation?>(null)

    /** Recent presentation entry ids, so that a later write of an entry is not taken for a new one. */
    private val seenPresentationIds = ArrayDeque<String>()

    init {
        coroutineScope.launch {
            for (operation in operations) {
                when (operation) {
                    is StorageOperation.Store -> store(operation.entry)

                    is StorageOperation.StoreRegistrationCheck -> storeRegistrationCheck(operation)

                    is StorageOperation.StorePresentationAction -> completeOperation(operation.completion) {
                        persist(
                            entry = operation.entry,
                            parentPresentationId = operation.parentPresentationId,
                            communicationMethod = operation.communicationMethod,
                        )
                    }

                    is StorageOperation.Delete -> completeOperation(operation.completion) {
                        transactionLogDao.delete(operation.id)
                        true
                    }

                    is StorageOperation.DeleteAll -> completeOperation(operation.completion) {
                        transactionLogDao.deleteAll()
                        true
                    }
                }
            }
        }.invokeOnCompletion {
            // Cancel pending operations so callers are not left waiting.
            operations.cancel()
        }
    }

    override fun log(transaction: TransactionEntry) {
        if (transaction is TransactionEntry.Presentation && isNewPresentation(transaction.transactionIdentifier)) {
            pendingPresentation.getAndUpdate { current ->
                if (current == null) PendingPresentation.Entry(transaction) else PendingPresentation.Ambiguous
            }
        }
        operations.trySend(StorageOperation.Store(entry = transaction))
    }

    override fun onTransferEvent(event: TransferEvent) {
        if (event !is TransferEvent.RequestReceived) return
        val pending =
            (pendingPresentation.getAndSet(null) as? PendingPresentation.Entry)?.entry ?: return
        val request = event.processedRequest.getOrNull() ?: return
        if (pending.relyingPartyFields() != event.processedRequest.loggedRelyingPartyFields()) return
        val check = request.wrpRegistration
            .toRegistrationStatusDomain(locale = resourceProvider.getLocale())
            .toRecordedRegistrationCheck()
        operations.trySend(
            StorageOperation.StoreRegistrationCheck(
                id = pending.transactionIdentifier,
                check = check,
            )
        )
    }

    /** Whether [id] has not been seen before; remembers it if so. */
    private fun isNewPresentation(id: String): Boolean = synchronized(seenPresentationIds) {
        if (id in seenPresentationIds) return@synchronized false
        seenPresentationIds.addLast(id)
        if (seenPresentationIds.size > MAX_SEEN_PRESENTATIONS) seenPresentationIds.removeFirst()
        true
    }

    /** The relying-party fields Wallet Core records for [this] request. */
    private fun RequestProcessor.ProcessedRequest.loggedRelyingPartyFields(): List<Any?> {
        val builder = PresentationLogBuilder()
        return builder.withRelyingParty(builder.createEmptyPresentationLog(), this)
            .relyingPartyFields()
    }

    private fun TransactionEntry.Presentation.relyingPartyFields(): List<Any?> = listOf(
        interactingPartyName,
        interactingPartyIdentifier,
        interactingPartyContact,
        isIntermediary,
        intermediaryIdentifier,
        intermediaryName,
        intermediaryContact,
        registrarURL,
        purpose,
        privacyPolicy,
        dpaName,
        dpaCountry,
        dpaContact,
    )

    override suspend fun getTransactionLogs(): List<TransactionLogDomain> =
        withContext(dispatcher) {
            val userLocale = resourceProvider.getLocale()

            transactionLogDao.retrieveAll().mapNotNull { storedTransaction ->
                val entry = storedTransaction.toCoreTransactionLog()
                if (entry == null) {
                    deleteTransactionLog(storedTransaction.identifier)
                    return@mapNotNull null
                }
                storedTransaction.toDomain(entry = entry, userLocale = userLocale)
            }
        }

    override suspend fun getTransactionLog(id: String): TransactionLogDomain? =
        withContext(dispatcher) {
            val storedTransaction = transactionLogDao.retrieve(id) ?: return@withContext null
            storedTransaction.toCoreTransactionLog()?.let { entry ->
                storedTransaction.toDomain(entry = entry, userLocale = resourceProvider.getLocale())
            }
        }

    override fun observePresentationActions(presentationId: String): Flow<List<PresentationAction>> =
        transactionLogDao.observeByParentPresentationId(parentPresentationId = presentationId)
            .map { relatedPresentationActions ->
                val userLocale = resourceProvider.getLocale()

                relatedPresentationActions.mapNotNull { storedTransaction ->
                    val entry = storedTransaction.toCoreTransactionLog()
                    if (entry == null) {
                        deleteTransactionLog(storedTransaction.identifier)
                        return@mapNotNull null
                    }

                    (storedTransaction.toDomain(
                        entry = entry,
                        userLocale = userLocale,
                    ) as? PresentationAction)?.let { safePresentationAction ->
                        entry.time to safePresentationAction
                    }
                }.sortedByDescending { (recordedTime, _) ->
                    recordedTime
                }.map { (_, presentationAction) -> presentationAction }
            }.flowOn(dispatcher)

    override suspend fun recordPresentationAction(
        parentPresentationId: String,
        entry: TransactionEntry,
        communicationMethod: CommunicationMethodDomain,
    ): Boolean {
        return enqueueAndAwait(
            StorageOperation.StorePresentationAction(
                parentPresentationId = parentPresentationId,
                entry = entry,
                communicationMethod = communicationMethod,
                completion = CompletableDeferred(),
            )
        )
    }

    override suspend fun deleteTransactionLog(id: String) {
        enqueueAndAwait(
            StorageOperation.Delete(
                id = id,
                completion = CompletableDeferred()
            )
        )
    }

    override suspend fun deleteAllTransactionLogs() {
        enqueueAndAwait(
            StorageOperation.DeleteAll(
                completion = CompletableDeferred()
            )
        )
    }

    private suspend fun enqueueAndAwait(operation: StorageOperation.Awaited): Boolean {
        currentCoroutineContext().ensureActive()
        operations.send(operation)
        return operation.completion.await()
    }

    private suspend fun store(entry: TransactionEntry) {
        // An SDK write has no waiting caller; its failure must not stop subsequent writes.
        runCatching {
            persist(entry = entry, parentPresentationId = null, communicationMethod = null)
        }.onFailure { error ->
            if (error is CancellationException) throw error
        }
    }

    private suspend fun storeRegistrationCheck(operation: StorageOperation.StoreRegistrationCheck) {
        val check = operation.check.toStoredRegistrationCheck() ?: return
        runCatching {
            transactionLogDao.recordRegistrationCheck(
                identifier = operation.id,
                check = check,
                failureReason = operation.check.toStoredRegistrationFailureReason(),
            )
        }.onFailure { error ->
            if (error is CancellationException) throw error
        }
    }

    private fun TransactionStorage.toDomain(
        entry: TransactionEntry,
        userLocale: Locale,
    ): TransactionLogDomain? = entry.toTransactionLogDomain(
        id = identifier,
        userLocale = userLocale,
        parentPresentationId = parentPresentationId,
        communicationMethod = communicationMethod,
        transactionDataTypes = transactionDataTypes,
        registrationCheck = recordedRegistrationCheckOf(
            check = registrationCheck,
            failureReason = registrationFailureReason,
        ),
    )

    private suspend fun persist(
        entry: TransactionEntry,
        parentPresentationId: String?,
        communicationMethod: CommunicationMethodDomain?,
    ): Boolean {
        val storedTransaction = transactionLogDao.retrieve(entry.transactionIdentifier)

        val parentId = storedTransaction?.parentPresentationId
            ?: parentPresentationId
        val method = storedTransaction?.communicationMethod.toCommunicationMethodDomainOrNull()
            ?: communicationMethod

        if (entry.isPresentationAction()) {
            if (parentId.isNullOrBlank() || parentId == entry.transactionIdentifier || method == null)
                return false

            // A retry succeeds only if its payload and supplied metadata match the stored attempt.
            // Missing metadata keeps the stored parent/method; an existing attempt is never rewritten.
            if (storedTransaction != null) {
                return storedTransaction.value == entry.toJson() &&
                        (parentPresentationId == null || parentPresentationId == storedTransaction.parentPresentationId) &&
                        (communicationMethod == null ||
                                communicationMethod.toStoredCommunicationMethod() == storedTransaction.communicationMethod)
            }

            val parent = transactionLogDao.retrieve(parentId)?.toCoreTransactionLog()
            if (parent !is TransactionEntry.Presentation)
                return false
        }

        transactionLogDao.store(
            TransactionStorage(
                identifier = entry.transactionIdentifier,
                value = entry.toJson(),
                parentPresentationId = parentId,
                communicationMethod = method?.toStoredCommunicationMethod(),
            )
        )
        return true
    }

    private fun TransactionEntry.isPresentationAction(): Boolean =
        this is TransactionEntry.DataDeletionRequest || this is TransactionEntry.DPAReport

    private suspend fun completeOperation(
        completion: CompletableDeferred<Boolean>,
        operation: suspend () -> Boolean,
    ) {
        try {
            completion.complete(operation())
        } catch (error: CancellationException) {
            completion.cancel(error)
            throw error
        } catch (error: Exception) {
            completion.completeExceptionally(error)
        } finally {
            // Make sure the caller is not left waiting after an unexpected error.
            completion.cancel()
        }
    }

    private sealed interface StorageOperation {
        data class Store(val entry: TransactionEntry) : StorageOperation

        data class StoreRegistrationCheck(
            val id: String,
            val check: RecordedRegistrationCheckDomain,
        ) : StorageOperation

        sealed interface Awaited : StorageOperation {
            val completion: CompletableDeferred<Boolean>
        }

        data class StorePresentationAction(
            val parentPresentationId: String,
            val entry: TransactionEntry,
            val communicationMethod: CommunicationMethodDomain,
            override val completion: CompletableDeferred<Boolean>,
        ) : Awaited

        data class Delete(
            val id: String,
            override val completion: CompletableDeferred<Boolean>
        ) : Awaited

        data class DeleteAll(
            override val completion: CompletableDeferred<Boolean>
        ) : Awaited
    }

    /** What [log] left for the next request to claim. */
    private sealed interface PendingPresentation {

        data class Entry(val entry: TransactionEntry.Presentation) : PendingPresentation

        /** A second entry was written before the first one's request was seen. */
        data object Ambiguous : PendingPresentation
    }

    private companion object {
        const val MAX_SEEN_PRESENTATIONS = 32
    }
}