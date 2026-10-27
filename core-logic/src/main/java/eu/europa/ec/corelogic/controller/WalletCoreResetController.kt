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
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

sealed class ResetWalletPartialState {
    data object Success : ResetWalletPartialState()
    data class Failure(val errorMessage: String) : ResetWalletPartialState()
}

/**
 * Factory reset. Deletes every document and its keys, the Digital Credential API registration, the
 * transaction history and the app's other records, the PIN, the biometric and other settings, the
 * logs and the cached files, while the app keeps running, so that the wallet can start again at
 * onboarding.
 */
interface WalletCoreResetController {

    /**
     * Runs the reset. Background work is stopped first, and the reset fails without deleting anything
     * if a worker is still working when the wait ends. Every document is then deleted through Wallet
     * Core, so that its keys are removed as Wallet Core intends, while the Digital Credential API
     * registration is held and cleared at the end. The database tables are emptied, the preferences
     * cleared, the log and cached files deleted, and the session is locked. Background work is
     * scheduled again only after a failure; after a successful reset it starts with the dashboard,
     * after the next login. Once started, the reset runs to the end even if its caller is
     * cancelled, and [WalletResetStatus.isResetting] is true meanwhile. A document that could not
     * be deleted, or a failed step, returns [ResetWalletPartialState.Failure]; running the reset
     * again is safe.
     */
    suspend fun resetWallet(): ResetWalletPartialState
}

/**
 * Whether a factory reset is running. The host activity reads it to drop links that arrive
 * meanwhile, and to forget a link it kept for later, so that neither opens after onboarding.
 */
class WalletResetStatus {

    private val resetting = MutableStateFlow(false)

    val isResetting: StateFlow<Boolean> = resetting.asStateFlow()

    internal fun setResetting(value: Boolean) {
        resetting.value = value
    }
}

class WalletCoreResetControllerImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val walletCoreDCAPIRegistrationController: WalletCoreDCAPIRegistrationController,
    private val prefKeys: PrefKeys,
    private val sessionController: SessionController,
    private val resourceProvider: ResourceProvider,
    private val resetStatus: WalletResetStatus,
    private val stopBackgroundWork: suspend () -> Boolean,
    private val scheduleBackgroundWork: () -> Unit,
    private val clearTransactionLogs: suspend () -> Unit,
    private val clearDatabase: () -> Unit,
    private val deleteLogFiles: () -> Unit,
    private val deleteCacheFiles: () -> Unit,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WalletCoreResetController {

    private companion object {
        const val DELETION_PASSES = 3
    }

    /**
     * Inside [NonCancellable] a cancellation can only come from a step itself, so it counts as a
     * failed step like any other exception. The flag is cleared on every path, and before the
     * background work is scheduled again after a failure, as on iOS.
     */
    override suspend fun resetWallet(): ResetWalletPartialState =
        withContext(dispatcher + NonCancellable) {
            resetStatus.setResetting(true)
            val result = try {
                runSteps()
            } catch (_: Exception) {
                failure()
            } finally {
                resetStatus.setResetting(false)
            }
            if (result is ResetWalletPartialState.Failure) {
                try {
                    scheduleBackgroundWork()
                } catch (_: Exception) {
                }
            }
            result
        }

    /**
     * The reset's steps in order. The transaction history is cleared through its own queue before
     * the tables are, so that a deletion entry Wallet Core logged for a removed document cannot be
     * written after the tables are empty.
     */
    private suspend fun runSteps(): ResetWalletPartialState {
        if (!stopBackgroundWork()) {
            return failure()
        }
        val documentsDeleted =
            walletCoreDCAPIRegistrationController.clearAround { deleteDocuments() }
        if (!documentsDeleted) {
            return failure()
        }
        clearTransactionLogs()
        clearDatabase()
        prefKeys.clearAllExceptSessionIdAndDbKey()
        deleteLogFiles()
        deleteCacheFiles()
        sessionController.lock()
        return ResetWalletPartialState.Success
    }

    /**
     * Deletes every document Wallet Core stores, the main PID last, as
     * [WalletCoreDocumentsController.deleteAllDocuments] does. Unlike that function it does not stop
     * when the wallet holds no PID, and a document that fails to delete does not stop the others. It
     * makes up to [DELETION_PASSES] passes, so that a document stored by a background job that was
     * still finishing is deleted too, and returns true once no document is left.
     */
    private suspend fun deleteDocuments(): Boolean {
        repeat(DELETION_PASSES) {
            val documents = walletCoreDocumentsController.getAllStoredDocuments()
            if (documents.isEmpty()) return true
            val mainPidId = walletCoreDocumentsController.getMainPidDocument()?.id
            documents
                .sortedBy { it.id == mainPidId }
                .forEach { document -> deleteDocument(document) }
        }
        return walletCoreDocumentsController.getAllStoredDocuments().isEmpty()
    }

    private suspend fun deleteDocument(document: Document) {
        try {
            walletCoreDocumentsController.deleteDocument(documentId = document.id).first()
        } catch (_: Exception) {
        }
    }

    private fun failure() =
        ResetWalletPartialState.Failure(errorMessage = resourceProvider.genericErrorMessage())
}