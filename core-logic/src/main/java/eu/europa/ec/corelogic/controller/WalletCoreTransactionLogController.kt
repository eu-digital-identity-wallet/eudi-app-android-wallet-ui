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

import eu.europa.ec.corelogic.extension.toCommunicationMethodDomainOrNull
import eu.europa.ec.corelogic.extension.toCoreTransactionLog
import eu.europa.ec.corelogic.extension.toStoredCommunicationMethod
import eu.europa.ec.corelogic.extension.toTransactionLogDomain
import eu.europa.ec.corelogic.model.CommunicationMethodDomain
import eu.europa.ec.corelogic.model.TransactionLogDomain
import eu.europa.ec.corelogic.model.TransactionLogDomain.PresentationAction
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogger
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionEntry
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
import eu.europa.ec.storagelogic.model.TransactionLog as TransactionStorage

interface WalletCoreTransactionLogController : TransactionLogger {
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

    init {
        coroutineScope.launch {
            for (operation in operations) {
                when (operation) {
                    is StorageOperation.Store -> store(operation.entry)

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
        operations.trySend(StorageOperation.Store(entry = transaction))
    }

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

    private fun TransactionStorage.toDomain(
        entry: TransactionEntry,
        userLocale: Locale,
    ): TransactionLogDomain? = entry.toTransactionLogDomain(
        id = identifier,
        userLocale = userLocale,
        parentPresentationId = parentPresentationId,
        communicationMethod = communicationMethod,
        transactionDataTypes = transactionDataTypes,
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

}