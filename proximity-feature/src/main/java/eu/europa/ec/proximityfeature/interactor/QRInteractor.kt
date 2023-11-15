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

import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.commonfeature.corewrapper.EUDIWListenerWrapper
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.cancellable

sealed class QRInteractorPartialState {
    data class Success(val qRCode: String) : QRInteractorPartialState()
    data object Connected : QRInteractorPartialState()
    data class Failure(val error: String) : QRInteractorPartialState()
}

interface QRInteractor {
    fun startQrEngagement(): Flow<QRInteractorPartialState>
}

class QRInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val eudiWallet: EudiWallet
) : QRInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun startQrEngagement(): Flow<QRInteractorPartialState> = callbackFlow {
        val callback = EUDIWListenerWrapper(
            onConnected = {
                trySendBlocking(QRInteractorPartialState.Connected)
            },
            onQrEngagementReady = {
                trySendBlocking(QRInteractorPartialState.Success(it))
            },
            onError = {
                trySendBlocking(QRInteractorPartialState.Failure(it))
            },
        )
        eudiWallet.addTransferEventListener(callback)
        eudiWallet.startQrEngagement()
        awaitClose {
            eudiWallet.removeTransferEventListener(callback)
            eudiWallet.stopPresentation()
        }
    }.safeAsync {
        QRInteractorPartialState.Failure(error = it.localizedMessage ?: genericErrorMsg)
    }.cancellable()
}