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

package eu.europa.ec.proximityfeature.interactor

import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.resourceslogic.provider.ResourceProvider

sealed class QRInteractorPartialState {
    data class Success(val qRCode: String) : QRInteractorPartialState()
    data object Connected : QRInteractorPartialState()
    data class Failure(val error: String) : QRInteractorPartialState()
}

interface QRInteractor {
    fun startQrEngagement(stateChanged: (QRInteractorPartialState) -> Unit)
}

class QRInteractorImpl(
    private val resourceProvider: ResourceProvider,
) : QRInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    private var onStateChange: ((QRInteractorPartialState) -> Unit)? = null

    private var transferListener:TransferEvent.Listener? = null

    override fun startQrEngagement(stateChanged: (QRInteractorPartialState) -> Unit) {
        transferListener = TransferEvent.Listener { event ->
            println("event is $event")
            when (event) {
                is TransferEvent.QrEngagementReady -> {
                    onStateChange?.invoke(
                        QRInteractorPartialState.Success(
                            qRCode = event.qrCode.content
                        )
                    )
                }

                is TransferEvent.Error -> {
                    onStateChange?.invoke(
                        QRInteractorPartialState.Failure(
                            error = event.error.message ?: genericErrorMsg
                        )
                    )
                }

                TransferEvent.Connected -> {
                    EudiWallet.removeTransferEventListener(transferListener!!)
                    onStateChange?.invoke(
                        QRInteractorPartialState.Connected
                    )
                }

                is TransferEvent.RequestReceived -> {}
                TransferEvent.Connecting -> {}
                TransferEvent.Disconnected -> {}
                TransferEvent.ResponseSent -> {}
            }
        }

        EudiWallet.addTransferEventListener(transferListener!!)
        EudiWallet.startQrEngagement()

        onStateChange = stateChanged
    }
}