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
import eu.europa.ec.eudi.iso18013.transfer.RequestDocument
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
import kotlinx.coroutines.flow.shareIn
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped

sealed class TransferEventPartialState {
    data object Connected : TransferEventPartialState()
    data object Connecting : TransferEventPartialState()
    data object Disconnected : TransferEventPartialState()
    data class Error(val error: String) : TransferEventPartialState()
    data class QrEngagementReady(val qrCode: String) : TransferEventPartialState()
    data class RequestReceived(val requestDocuments: List<RequestDocument>) :
        TransferEventPartialState()

    data object ResponseSent : TransferEventPartialState()
}

interface EudiWalletInteractor {

    val events: Flow<TransferEventPartialState>
    fun cancelScope()
    fun startQrEngagement()
}

@Scope(WalletPresentationScope::class)
@Scoped
class EudiWalletInteractorImpl(
    private val eudiWallet: EudiWallet,
    private val resourceProvider: ResourceProvider,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : EudiWalletInteractor {
    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

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
                trySendBlocking(
                    TransferEventPartialState.RequestReceived(requestDocuments = requestDocuments)
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

    override fun cancelScope() {
        coroutineScope.cancel()
    }
}