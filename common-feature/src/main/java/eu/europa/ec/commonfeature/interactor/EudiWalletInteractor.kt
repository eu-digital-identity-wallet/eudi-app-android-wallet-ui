/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.commonfeature.interactor

import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.commonfeature.corewrapper.EUDIWListenerWrapper
import eu.europa.ec.commonfeature.di.WalletPresentationScope
import eu.europa.ec.commonfeature.ui.request.Event
import eu.europa.ec.commonfeature.ui.request.model.RequestDataUi
import eu.europa.ec.commonfeature.ui.request.transformer.RequestTransformer
import eu.europa.ec.eudi.iso18013.transfer.ResponseResult
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.resourceslogic.R
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

sealed class TransferEventPartialState {
    data object Connected : TransferEventPartialState()
    data object Connecting : TransferEventPartialState()
    data object Disconnected : TransferEventPartialState()
    data class Error(val error: String) : TransferEventPartialState()
    data class QrEngagementReady(val qrCode: String) : TransferEventPartialState()
    data class RequestReceived(
        val requestDataUi: List<RequestDataUi<Event>>,
        val verifierName: String?
    ) :
        TransferEventPartialState()

    data object ResponseSent : TransferEventPartialState()
}

sealed class SendRequestedDocumentsPartialState {
    data class Failure(val error: String) : SendRequestedDocumentsPartialState()
    data object UserAuthenticationRequired : SendRequestedDocumentsPartialState()
    data object RequestSent : SendRequestedDocumentsPartialState()
}

sealed class ResponseReceivedPartialState {
    data object Success : ResponseReceivedPartialState()
    data class Failure(val error: String) : ResponseReceivedPartialState()
}

sealed class EudiWalletProximityPartialState {
    data object UserAuthenticationRequired : EudiWalletProximityPartialState()
    data class Failure(val error: String) : EudiWalletProximityPartialState()
    data object Success : EudiWalletProximityPartialState()
}

/**
 * Common scoped interactor that has all the complexity and required interaction with the EudiWallet Core.
 * */
interface EudiWalletInteractor {
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
    val requestDataUi: List<RequestDataUi<Event>>
    /**
     * Verifier name so it can be retrieve across screens
     * */
    val verifierName: String?
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
     * Transform UI models to Domain and create -> sent the request.
     *
     * @return Flow that emits the creation state. On Success send the request.
     * The response of that request is emitted through [events]
     *  */
    fun sendRequestedDocuments(): Flow<SendRequestedDocumentsPartialState>
    /**
     * Updates the UI model
     * @param items User updated data through UI Events
     * */
    fun updateRequestedDocuments(items: List<RequestDataUi<Event>>)
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
    fun observeSentDocumentsRequest(): Flow<EudiWalletProximityPartialState>
}

@Scope(WalletPresentationScope::class)
@Scoped
class EudiWalletInteractorImpl(
    private val eudiWallet: EudiWallet,
    private val resourceProvider: ResourceProvider,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : EudiWalletInteractor {

    private val genericErrorMessage = resourceProvider.genericErrorMessage()

    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    override var requestDataUi: List<RequestDataUi<Event>> = emptyList()
        private set
    override var verifierName: String? = null
        private set

    override val events = callbackFlow {
        val eventListenerWrapper = EUDIWListenerWrapper(
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
                requestDataUi = RequestTransformer.transformToUiItems(
                    requestDocuments = requestDocuments,
                    requiredFieldsTitle = resourceProvider.getString(R.string.request_required_fields_title),
                    resourceProvider = resourceProvider
                )
                verifierName =
                    requestDocuments.firstOrNull()?.docRequest?.readerAuth?.readerCommonName
                trySendBlocking(
                    TransferEventPartialState.RequestReceived(
                        requestDataUi = requestDataUi,
                        verifierName = verifierName
                    )
                )
            },
            onResponseSent = {
                trySendBlocking(
                    TransferEventPartialState.ResponseSent
                )
            }
        )

        eudiWallet.addTransferEventListener(eventListenerWrapper)
        awaitClose {
            eudiWallet.removeTransferEventListener(eventListenerWrapper)
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

    override fun sendRequestedDocuments() =
        flow {
            val disclosedDocuments = RequestTransformer.transformToDomainItems(requestDataUi)
            when (val response = eudiWallet.createResponse(disclosedDocuments)) {
                is ResponseResult.Failure -> {
                    emit(SendRequestedDocumentsPartialState.Failure(resourceProvider.genericErrorMessage()))
                }

                is ResponseResult.Response -> {
                    val responseBytes = response.bytes
                    eudiWallet.sendResponse(responseBytes)
                    emit(SendRequestedDocumentsPartialState.RequestSent)
                }

                is ResponseResult.UserAuthRequired -> {
                    emit(SendRequestedDocumentsPartialState.UserAuthenticationRequired)
                }
            }
        }

    override fun mappedCallbackStateFlow(): Flow<ResponseReceivedPartialState> {
        return events.mapNotNull { response ->
            when (response) {

                // This state should be fixed by Scytales. Right now verifier sends this error for success
                is TransferEventPartialState.Error -> {
                    if (response.error == "Peer disconnected without proper session termination") {
                        ResponseReceivedPartialState.Success
                    } else {
                        ResponseReceivedPartialState.Failure(error = response.error)
                    }
                }

                else -> null
            }
        }.safeAsync {
            ResponseReceivedPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMessage
            )
        }
    }

    override fun observeSentDocumentsRequest(): Flow<EudiWalletProximityPartialState> =
        sendRequestedDocuments().zip(mappedCallbackStateFlow()) { createResponseState, sentResponseState ->
            when {
                createResponseState is SendRequestedDocumentsPartialState.Failure -> {
                    EudiWalletProximityPartialState.Failure(createResponseState.error)
                }

                createResponseState is SendRequestedDocumentsPartialState.UserAuthenticationRequired -> {
                    EudiWalletProximityPartialState.UserAuthenticationRequired
                }

                sentResponseState is ResponseReceivedPartialState.Failure -> {
                    EudiWalletProximityPartialState.Failure(sentResponseState.error)
                }

                createResponseState is SendRequestedDocumentsPartialState.RequestSent &&
                        sentResponseState !is ResponseReceivedPartialState.Success -> {
                    null
                }

                else -> {
                    EudiWalletProximityPartialState.Success
                }
            }
        }.filterNotNull()
            .safeAsync {
                EudiWalletProximityPartialState.Failure(it.localizedMessage ?: genericErrorMessage)
            }

    override fun updateRequestedDocuments(items: List<RequestDataUi<Event>>) {
        requestDataUi = items
    }

    override fun stopPresentation() {
        eudiWallet.stopPresentation()
        coroutineScope.cancel()
    }
}