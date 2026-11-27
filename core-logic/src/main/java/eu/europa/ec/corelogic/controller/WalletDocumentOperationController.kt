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

import eu.europa.ec.authenticationlogic.controller.session.SessionController
import eu.europa.ec.corelogic.model.WalletDocumentOperationStateDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Shared coordination across document controller instances. Tracks document operations and their
 * writes for reset; the Deferred scheduler owns deadlines and retry decisions.
 */
interface WalletDocumentOperationController {
    val state: StateFlow<WalletDocumentOperationStateDomain>

    /** Completed Deferred issuances waiting for their first appearance in Documents in this process. */
    val unseenDeferredIssuanceIds: StateFlow<Set<String>>

    fun recordDeferredIssuance(documentId: String)
    fun markDeferredIssuanceSeen(documentId: String)

    fun setForeground(isForeground: Boolean)

    /** Updates the scheduler's process-local Failed indication and reports whether it changed. */
    fun setRetryableDeferredFailure(documentId: String, failed: Boolean): Boolean

    /**
     * Checks current lifecycle and removal gates; null skips the per-document checks.
     * Does not reserve a concurrency slot or check whether a document is due.
     */
    fun canPoll(documentId: String?): Boolean

    /** Records a confirmed Wallet Core deletion to reject late schedule registration for this ID. */
    fun recordRemoval(documentId: String)

    /** Blocks new ordinary operations. Call [awaitIdle] before starting reset deletion. */
    fun stop()
    fun resume()

    /** Called after operations drain and storage is cleared, while reset still blocks new operations. */
    fun onWalletReset()

    /** Waits for tracked actions and their writes. Stop new work first; the caller owns the timeout. */
    suspend fun awaitIdle()

    /**
     * Rechecks gates for an attempt reserved by [tryStartPolling], immediately before [start].
     * Reset and session lock are ordered with submission; callback completion is not awaited here.
     * False means [start] was not called; true does not describe the issuance result.
     */
    fun startCorePolling(documentId: String, start: () -> Unit): Boolean

    /** Reserves one attempt per ID through its result writes; null [limit] removes the global cap. */
    fun tryStartPolling(documentId: String, limit: Int?, action: suspend () -> Unit): Boolean

    /** Serializes work for one ID. Do not acquire the same ID's lock again from [action]. */
    suspend fun <T> withDocumentLock(documentId: String, action: suspend () -> T): T

    /** Blocks new pings before waiting for this ID's active attempt and its writes to finish. */
    suspend fun <T> withRemoval(documentId: String, action: suspend () -> T): T

    /**
     * Forwards states from [flowToCollect] up to and including the first state matching [completeWhen].
     * The flow must finish its writes before emitting that state. Tracking continues if the screen stops
     * observing, unless [isAbandonable] is true then: such an operation has written nothing yet and is
     * cancelled, so it cannot hold back reset or bulk removal.
     */
    fun <T> trackedOperationFlow(
        completeWhen: (state: T) -> Boolean,
        isResetOperation: Boolean,
        isAbandonable: () -> Boolean = { false },
        flowToCollect: () -> Flow<T>,
    ): Flow<T>

    /**
     * Tracks [action], which must await its calls and writes. Returns null if the operation cannot start.
     * When [isResetOperation] is true, the controller must be stopped and idle during an active reset.
     */
    fun <T> startOperation(isResetOperation: Boolean, action: suspend () -> T): Deferred<T>?

    /**
     * Blocks new document operations and waits for earlier ones before running [action].
     * Reset tracks the entire removal. Each deletion in [action] must run within this operation.
     *
     * @return The removal operation, or null if another bulk removal or reset is in progress
     * or document operations are stopped.
     */
    fun <T> tryStartRemovingAllDocuments(action: suspend () -> T): Deferred<T>?
}

class WalletDocumentOperationControllerImpl(
    private val sessionController: SessionController,
    private val resetStatus: WalletResetStatus,
    private val coroutineScope: CoroutineScope,
) : WalletDocumentOperationController {

    // Share the lock for reset and operation state; suspending work uses per-document mutexes.
    private val monitor = resetStatus.operationStateLock
    private val operations = mutableSetOf<Job>()

    // Keep locks until reset; replacing one could leave callers waiting on a different lock.
    private val documentLocks = mutableMapOf<String, Mutex>()

    // One removal finishing must not reopen polling while another removal is still waiting.
    private val removing = mutableMapOf<String, Int>()
    private val mutableState = MutableStateFlow(
        WalletDocumentOperationStateDomain(
            isForeground = false,
            isStopped = false,
            isRemovingAllDocuments = false,
            failedDeferredDocumentIds = emptySet(),
            activeDocumentIds = emptySet(),
            removingDocumentIds = emptySet(),
            removedDocumentIds = emptySet(),
            pendingOperations = 0,
            walletGeneration = 0,
        )
    )

    override val state: StateFlow<WalletDocumentOperationStateDomain> = mutableState.asStateFlow()

    // A row becoming visible should not wake the polling timer.
    private val mutableUnseenDeferredIssuanceIds = MutableStateFlow(emptySet<String>())
    override val unseenDeferredIssuanceIds = mutableUnseenDeferredIssuanceIds.asStateFlow()

    override fun recordDeferredIssuance(documentId: String) = synchronized(monitor) {
        mutableUnseenDeferredIssuanceIds.value += documentId
    }

    override fun markDeferredIssuanceSeen(documentId: String) = synchronized(monitor) {
        mutableUnseenDeferredIssuanceIds.value -= documentId
    }

    override fun setForeground(isForeground: Boolean) = synchronized(monitor) {
        mutableState.value = state.value.copy(isForeground = isForeground)
    }

    override fun setRetryableDeferredFailure(documentId: String, failed: Boolean): Boolean =
        synchronized(monitor) {
            val failedIds = state.value.failedDeferredDocumentIds
            if ((documentId in failedIds) == failed) return false
            mutableState.value = state.value.copy(
                failedDeferredDocumentIds = if (failed) failedIds + documentId else failedIds - documentId,
            )
            true
        }

    override fun canPoll(documentId: String?): Boolean = synchronized(monitor) {
        val current = state.value
        current.isForeground && !current.isStopped && !current.isRemovingAllDocuments &&
                sessionController.isUnlocked && !resetStatus.isResetting.value &&
                documentId !in current.removingDocumentIds && documentId !in current.removedDocumentIds
    }

    override fun recordRemoval(documentId: String) = synchronized(monitor) {
        mutableUnseenDeferredIssuanceIds.value -= documentId
        mutableState.value = state.value.copy(
            removedDocumentIds = state.value.removedDocumentIds + documentId,
            failedDeferredDocumentIds = state.value.failedDeferredDocumentIds - documentId,
        )
    }

    override fun stop() = synchronized(monitor) {
        mutableState.value = state.value.copy(isStopped = true)
    }

    override fun resume() = synchronized(monitor) {
        mutableState.value = state.value.copy(isStopped = false)
    }

    override fun onWalletReset() = synchronized(monitor) {
        check(operations.isEmpty())
        documentLocks.clear()
        mutableUnseenDeferredIssuanceIds.value = emptySet()
        mutableState.value = state.value.copy(
            isStopped = false,
            failedDeferredDocumentIds = emptySet(),
            removedDocumentIds = emptySet(),
            walletGeneration = state.value.walletGeneration + 1,
        )
    }

    override suspend fun awaitIdle() {
        state.first { current -> current.pendingOperations == 0 }
    }

    override fun startCorePolling(documentId: String, start: () -> Unit): Boolean =
        synchronized(monitor) {
            if (!canPoll(documentId)) return false
            // Lock operation state before the session, and release both after Core submission.
            sessionController.runIfUnlocked(start)
        }

    override fun tryStartPolling(
        documentId: String,
        limit: Int?,
        action: suspend () -> Unit,
    ): Boolean = synchronized(monitor) {
        val active = state.value.activeDocumentIds
        if (!canPoll(documentId) || documentId in active || (limit != null && active.size >= limit)) {
            return false
        }
        mutableState.value = state.value.copy(activeDocumentIds = active + documentId)
        val operation = startOperation(isResetOperation = false) {
            try {
                action()
            } finally {
                releasePolling(documentId)
            }
        }
        if (operation == null) releasePolling(documentId)
        operation != null
    }

    override suspend fun <T> withDocumentLock(documentId: String, action: suspend () -> T): T {
        val lock = synchronized(monitor) { documentLocks.getOrPut(documentId) { Mutex() } }
        return lock.withLock { action() }
    }

    override suspend fun <T> withRemoval(documentId: String, action: suspend () -> T): T {
        synchronized(monitor) {
            removing[documentId] = (removing[documentId] ?: 0) + 1
            mutableState.value = state.value.copy(removingDocumentIds = removing.keys.toSet())
        }
        try {
            return withDocumentLock(documentId = documentId, action = action)
        } finally {
            synchronized(monitor) {
                val remaining = removing.getValue(documentId) - 1
                if (remaining == 0) removing.remove(documentId) else removing[documentId] =
                    remaining
                mutableState.value = state.value.copy(removingDocumentIds = removing.keys.toSet())
            }
        }
    }

    override fun <T> trackedOperationFlow(
        completeWhen: (state: T) -> Boolean,
        isResetOperation: Boolean,
        isAbandonable: () -> Boolean,
        flowToCollect: () -> Flow<T>,
    ): Flow<T> = flow {
        // Buffer intermediate states without making Core observation wait for the screen.
        val intermediateStates = Channel<T>(Channel.UNLIMITED)
        val trackedOperation = startOperation(isResetOperation = isResetOperation) {
            try {
                flowToCollect().first { state ->
                    if (completeWhen(state)) {
                        true
                    } else {
                        intermediateStates.trySend(state)
                        false
                    }
                }
            } finally {
                intermediateStates.close()
            }
        }

        if (trackedOperation == null) {
            intermediateStates.close(IllegalStateException("Wallet document operations are temporarily unavailable"))
        }

        try {
            for (intermediateState in intermediateStates) {
                emit(intermediateState)
            }

            if (trackedOperation != null) emit(trackedOperation.await())
        } finally {
            intermediateStates.cancel()
            if (trackedOperation?.isActive == true && isAbandonable()) trackedOperation.cancel()
        }
    }

    override fun <T> startOperation(
        isResetOperation: Boolean,
        action: suspend () -> T,
    ): Deferred<T>? = synchronized(monitor) {
        if (state.value.isRemovingAllDocuments) return null
        if (!isResetOperation && (state.value.isStopped || resetStatus.isResetting.value)) return null
        if (isResetOperation &&
            (!state.value.isStopped || !resetStatus.isResetting.value || operations.isNotEmpty())
        ) return null

        launchTrackedOperation(action = action)
    }

    override fun <T> tryStartRemovingAllDocuments(action: suspend () -> T): Deferred<T>? =
        synchronized(monitor) {
            if (state.value.isRemovingAllDocuments || state.value.isStopped || resetStatus.isResetting.value) {
                return null
            }
            // Capture earlier work before registering this removal; waiting for itself would deadlock.
            val operationsToAwait = operations.toList()
            mutableState.value = state.value.copy(isRemovingAllDocuments = true)
            launchTrackedOperation {
                try {
                    operationsToAwait.joinAll()
                    action()
                } finally {
                    synchronized(monitor) {
                        mutableState.value = state.value.copy(isRemovingAllDocuments = false)
                    }
                }
            }
        }

    /** Called under [monitor] after the caller has checked whether this operation can start. */
    private fun <T> launchTrackedOperation(action: suspend () -> T): Deferred<T> {
        // Register before execution, even if the action can finish immediately.
        lateinit var operation: Deferred<T>
        operation = coroutineScope.async(start = CoroutineStart.LAZY) {
            try {
                action()
            } finally {
                synchronized(monitor) {
                    operations.remove(operation)
                    mutableState.value = state.value.copy(pendingOperations = operations.size)
                }
            }
        }
        operations.add(operation)
        mutableState.value = state.value.copy(pendingOperations = operations.size)
        operation.start()
        return operation
    }

    private fun releasePolling(documentId: String) = synchronized(monitor) {
        mutableState.value = state.value.copy(
            activeDocumentIds = state.value.activeDocumentIds - documentId,
        )
    }
}