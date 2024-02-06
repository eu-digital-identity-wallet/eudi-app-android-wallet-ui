/*
 * Copyright (c) 2023 European Commission
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

package eu.europa.ec.businesslogic.controller.walletcore

import androidx.activity.ComponentActivity
import eu.europa.ec.businesslogic.di.WalletPresentationScope
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.businesslogic.util.EudiWalletListenerWrapper
import eu.europa.ec.eudi.iso18013.transfer.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.RequestDocument
import eu.europa.ec.eudi.iso18013.transfer.ResponseResult
import eu.europa.ec.eudi.iso18013.transfer.engagement.NfcEngagementService
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.zip
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped
import java.net.URI

sealed class TransferEventPartialState {
    data object Connected : TransferEventPartialState()
    data object Connecting : TransferEventPartialState()
    data object Disconnected : TransferEventPartialState()
    data class Error(val error: String) : TransferEventPartialState()
    data class QrEngagementReady(val qrCode: String) : TransferEventPartialState()
    data class RequestReceived(
        val requestData: List<RequestDocument>,
        val verifierName: String?,
        val verifierIsTrusted: Boolean,
    ) : TransferEventPartialState()

    data object ResponseSent : TransferEventPartialState()
    data class Redirect(val uri: URI) : TransferEventPartialState()
}

sealed class SendRequestedDocumentsPartialState {
    data class Failure(val error: String) : SendRequestedDocumentsPartialState()
    data object UserAuthenticationRequired : SendRequestedDocumentsPartialState()
    data object RequestSent : SendRequestedDocumentsPartialState()
}

sealed class ResponseReceivedPartialState {
    data object Success : ResponseReceivedPartialState()
    data class Redirect(val uri: URI) : ResponseReceivedPartialState()
    data class Failure(val error: String) : ResponseReceivedPartialState()
}

sealed class WalletCorePartialState {
    data object UserAuthenticationRequired : WalletCorePartialState()
    data class Failure(val error: String) : WalletCorePartialState()
    data object Success : WalletCorePartialState()
    data class Redirect(val uri: URI) : WalletCorePartialState()
}

sealed class LoadSampleDataPartialState {
    data object Success : LoadSampleDataPartialState()
    data class Failure(val error: String) : LoadSampleDataPartialState()
}

/**
 * Common scoped interactor that has all the complexity and required interaction with the EudiWallet Core.
 * */
interface WalletCorePresentationController {
    /**
     * On initialization it adds the core listener and remove it when scope is canceled.
     * When the scope is canceled so does the presentation
     *
     * @return Hot Flow that emits the Core's status callback.
     * */
    val events: Flow<TransferEventPartialState>

    /**
     * User selection data for request step
     * */
    val disclosedDocuments: DisclosedDocuments?

    /**
     * Verifier name so it can be retrieve across screens
     * */
    val verifierName: String?

    fun setConfig(config: PresentationControllerConfig)

    /**
     * Terminates the presentation and kills the coroutine scope that [events] live in
     * */
    fun stopPresentation()

    /**
     * Starts QR engagement. This will trigger [events] emission.
     *
     * [TransferEventPartialState.QrEngagementReady] -> QR String to show QR
     *
     * [TransferEventPartialState.Connecting] -> Connecting
     *
     * [TransferEventPartialState.Connected] -> Connected. We can proceed to the next screen
     * */
    fun startQrEngagement()

    /**
     * Enable/Disable NFC service
     * */
    fun toggleNfcEngagement(componentActivity: ComponentActivity, toggle: Boolean)

    /**
     * Transform UI models to Domain and create -> sent the request.
     *
     * @return Flow that emits the creation state. On Success send the request.
     * The response of that request is emitted through [events]
     *  */
    fun sendRequestedDocuments(): Flow<SendRequestedDocumentsPartialState>

    /**
     * Updates the UI model
     * @param disclosedDocuments User updated data through UI Events
     * */
    fun updateRequestedDocuments(disclosedDocuments: DisclosedDocuments?)

    /**
     * @return flow that maps the state from [events] emission to what we consider as success state
     * */
    fun mappedCallbackStateFlow(): Flow<ResponseReceivedPartialState>

    /**
     * The main observation point for collecting state for the Request flow.
     * Exposes a single flow for two operations([sendRequestedDocuments] - [mappedCallbackStateFlow])
     * and a single state
     * @return flow that emits the create, sent, receive states
     * */
    fun observeSentDocumentsRequest(): Flow<WalletCorePartialState>
}

@Scope(WalletPresentationScope::class)
@Scoped
class WalletCorePresentationControllerImpl(
    private val eudiWallet: EudiWallet,
    private val resourceProvider: ResourceProvider,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WalletCorePresentationController {

    private val genericErrorMessage = resourceProvider.genericErrorMessage()

    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    private lateinit var _config: PresentationControllerConfig

    override var disclosedDocuments: DisclosedDocuments? = null
        private set
    override var verifierName: String? = null
        private set

    override fun setConfig(config: PresentationControllerConfig) {
        _config = config
    }

    override val events = callbackFlow {
        val eventListenerWrapper = EudiWalletListenerWrapper(
            onQrEngagementReady = { qrCode ->
                trySendBlocking(
                    TransferEventPartialState.QrEngagementReady(qrCode = qrCode)
                )
            },
            onConnected = {
                trySendBlocking(
                    TransferEventPartialState.Connected
                )
            },
            onConnecting = {
                trySendBlocking(
                    TransferEventPartialState.Connecting
                )
            },
            onDisconnected = {
                trySendBlocking(
                    TransferEventPartialState.Disconnected
                )
            },
            onError = { errorMessage ->
                trySendBlocking(
                    TransferEventPartialState.Error(error = errorMessage)
                )
            },
            onRequestReceived = { requestDocuments ->
                verifierName =
                    requestDocuments.firstOrNull()?.docRequest?.readerAuth?.readerCommonName
                val verifierIsTrusted =
                    requestDocuments.firstOrNull()?.docRequest?.readerAuth?.readerSignIsValid == true
                trySendBlocking(
                    TransferEventPartialState.RequestReceived(
                        requestData = requestDocuments,
                        verifierName = verifierName,
                        verifierIsTrusted = verifierIsTrusted
                    )
                )
            },
            onResponseSent = {
                trySendBlocking(
                    TransferEventPartialState.ResponseSent
                )
            },
            onRedirect = { uri ->
                trySendBlocking(
                    TransferEventPartialState.Redirect(
                        uri = uri
                    )
                )
            }
        )

        addListener(eventListenerWrapper)
        awaitClose {
            removeListener(eventListenerWrapper)
            eudiWallet.stopPresentation()
        }
    }.shareIn(coroutineScope, SharingStarted.Lazily, 1)
        .safeAsync {
            TransferEventPartialState.Error(
                error = it.localizedMessage ?: resourceProvider.genericErrorMessage()
            )
        }

    override fun startQrEngagement() {
        eudiWallet.startQrEngagement()
    }

    override fun toggleNfcEngagement(componentActivity: ComponentActivity, toggle: Boolean) {
        try {
            if (toggle) {
                NfcEngagementService.enable(componentActivity)
            } else {
                NfcEngagementService.disable(componentActivity)
            }
        } catch (_: Exception) {
        }
    }

    override fun sendRequestedDocuments() =
        flow {
            disclosedDocuments?.let { documents ->
                when (val response = eudiWallet.createResponse(documents)) {
                    is ResponseResult.Failure -> {
                        emit(SendRequestedDocumentsPartialState.Failure(genericErrorMessage))
                    }

                    is ResponseResult.Response -> {
                        sendResponse(response.bytes)
                        emit(SendRequestedDocumentsPartialState.RequestSent)
                    }

                    is ResponseResult.UserAuthRequired -> {
                        emit(SendRequestedDocumentsPartialState.UserAuthenticationRequired)
                    }
                }
            }
        }.safeAsync {
            SendRequestedDocumentsPartialState.Failure(
                it.localizedMessage ?: genericErrorMessage
            )
        }

    override fun mappedCallbackStateFlow(): Flow<ResponseReceivedPartialState> {
        return events.mapNotNull { response ->
            when (response) {

                // Fix: Wallet core should return Success state here
                is TransferEventPartialState.Error -> {
                    if (response.error == "Peer disconnected without proper session termination") {
                        ResponseReceivedPartialState.Success
                    } else {
                        ResponseReceivedPartialState.Failure(error = response.error)
                    }
                }

                is TransferEventPartialState.Disconnected -> {
                    if (requireInit { _config } is PresentationControllerConfig.OpenId4VP) {
                        ResponseReceivedPartialState.Success
                    } else null
                }

                is TransferEventPartialState.Redirect -> {
                    ResponseReceivedPartialState.Redirect(uri = response.uri)
                }

                else -> null
            }
        }.safeAsync {
            ResponseReceivedPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMessage
            )
        }
    }

    override fun observeSentDocumentsRequest(): Flow<WalletCorePartialState> =
        sendRequestedDocuments().zip(mappedCallbackStateFlow()) { createResponseState, sentResponseState ->
            when {
                createResponseState is SendRequestedDocumentsPartialState.Failure -> {
                    WalletCorePartialState.Failure(createResponseState.error)
                }

                createResponseState is SendRequestedDocumentsPartialState.UserAuthenticationRequired -> {
                    WalletCorePartialState.UserAuthenticationRequired
                }

                sentResponseState is ResponseReceivedPartialState.Failure -> {
                    WalletCorePartialState.Failure(sentResponseState.error)
                }

                sentResponseState is ResponseReceivedPartialState.Redirect -> {
                    WalletCorePartialState.Redirect(
                        uri = sentResponseState.uri
                    )
                }

                createResponseState is SendRequestedDocumentsPartialState.RequestSent &&
                        sentResponseState !is ResponseReceivedPartialState.Success -> {
                    null
                }

                else -> {
                    WalletCorePartialState.Success
                }
            }
        }.filterNotNull()
            .safeAsync {
                WalletCorePartialState.Failure(it.localizedMessage ?: genericErrorMessage)
            }

    override fun updateRequestedDocuments(disclosedDocuments: DisclosedDocuments?) {
        this.disclosedDocuments = disclosedDocuments
    }

    override fun stopPresentation() {
        eudiWallet.stopPresentation()
        coroutineScope.cancel()
    }

    private fun addListener(listener: EudiWalletListenerWrapper) {
        when (val config = requireInit { _config }) {
            is PresentationControllerConfig.OpenId4VP -> {
                eudiWallet.openId4vpManager.addTransferEventListener(listener)
                eudiWallet.openId4vpManager.resolveRequestUri(config.uri)
            }

            PresentationControllerConfig.Ble -> {
                eudiWallet.addTransferEventListener(listener)
            }
        }
    }

    private fun removeListener(listener: EudiWalletListenerWrapper) {
        when (requireInit { _config }) {
            is PresentationControllerConfig.OpenId4VP -> {
                eudiWallet.openId4vpManager.removeTransferEventListener(listener)
            }

            PresentationControllerConfig.Ble -> {
                eudiWallet.removeTransferEventListener(listener)
            }
        }
    }

    private fun <T> requireInit(block: () -> T): T {
        if (!::_config.isInitialized) {
            throw IllegalStateException("setConfig() must be called before using the WalletCorePresentationController")
        }
        return block()
    }

    private fun sendResponse(responseBytes: ByteArray) {
        when (requireInit { _config }) {
            is PresentationControllerConfig.OpenId4VP -> {
                eudiWallet.openId4vpManager.sendResponse(responseBytes)
            }

            is PresentationControllerConfig.Ble -> {
                eudiWallet.sendResponse(responseBytes)
            }
        }
    }
}