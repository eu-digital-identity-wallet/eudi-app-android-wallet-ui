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

import eu.europa.ec.commonfeature.interactor.EudiWalletInteractor
import eu.europa.ec.commonfeature.interactor.TransferEventPartialState
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.mapNotNull

sealed class ProximityQRPartialState {
    data class QrReady(val qrCode: String) : ProximityQRPartialState()
    data class Error(val error: String) : ProximityQRPartialState()
    data object Connected : ProximityQRPartialState()
    data object Disconnected : ProximityQRPartialState()
}

interface ProximityQRInteractor {
    fun startQrEngagement(): Flow<ProximityQRPartialState>
    fun cancelTransfer()
}

class ProximityQRInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val eudiWallet: EudiWallet,
    private val eudiWalletInteractor: EudiWalletInteractor
) : ProximityQRInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun startQrEngagement(): Flow<ProximityQRPartialState> {
        eudiWallet.startQrEngagement()
        return eudiWalletInteractor.events.mapNotNull {
            when (it) {
                is TransferEventPartialState.Connected -> {
                    ProximityQRPartialState.Connected
                }

                is TransferEventPartialState.Error -> {
                    ProximityQRPartialState.Error(error = it.error)
                }

                is TransferEventPartialState.QrEngagementReady -> {
                    ProximityQRPartialState.QrReady(qrCode = it.qrCode)
                }

                is TransferEventPartialState.Disconnected -> {
                    ProximityQRPartialState.Disconnected
                }

                else -> null
            }
        }.cancellable()
    }

    override fun cancelTransfer() {
        eudiWallet.stopPresentation()
        eudiWalletInteractor.cancelScope()
    }
}