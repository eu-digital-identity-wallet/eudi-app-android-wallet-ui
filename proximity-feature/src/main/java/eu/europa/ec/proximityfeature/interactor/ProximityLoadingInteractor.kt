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
import eu.europa.ec.commonfeature.interactor.EudiWalletInteractor
import eu.europa.ec.commonfeature.interactor.SendRequestedDocumentsPartialState
import eu.europa.ec.commonfeature.interactor.TransferEventPartialState
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.zip

sealed class ProximityLoadingPartialState {
    data object Success : ProximityLoadingPartialState()
    data class Failure(val error: String) : ProximityLoadingPartialState()
}

sealed class ProximityLoadingCombinedPartialState {
    data object UserAuthenticationRequired : ProximityLoadingCombinedPartialState()
    data class Failure(val error: String) : ProximityLoadingCombinedPartialState()
    data object Success : ProximityLoadingCombinedPartialState()
}

interface ProximityLoadingInteractor {
    fun stopPresentation()
    fun sendRequestedDocuments(): Flow<ProximityLoadingCombinedPartialState>
    fun sendResponse(): Flow<SendRequestedDocumentsPartialState>
    fun observeResponse(): Flow<ProximityLoadingPartialState>
}

class ProximityLoadingInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val eudiWalletInteractor: EudiWalletInteractor
) : ProximityLoadingInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun sendRequestedDocuments(): Flow<ProximityLoadingCombinedPartialState> =
        sendResponse().zip(observeResponse()) { createResponseState, sendResponseState ->

            when {
                createResponseState is SendRequestedDocumentsPartialState.Failure -> {
                    ProximityLoadingCombinedPartialState.Failure(createResponseState.error)
                }

                createResponseState is SendRequestedDocumentsPartialState.UserAuthenticationRequired -> {
                    ProximityLoadingCombinedPartialState.UserAuthenticationRequired
                }

                sendResponseState is ProximityLoadingPartialState.Failure -> {
                    ProximityLoadingCombinedPartialState.Failure(sendResponseState.error)
                }

                createResponseState is SendRequestedDocumentsPartialState.RequestSend &&
                        sendResponseState !is ProximityLoadingPartialState.Success -> {
                    null
                }

                else -> {
                    ProximityLoadingCombinedPartialState.Success
                }
            }
        }.filterNotNull()
            .safeAsync {
                ProximityLoadingCombinedPartialState.Failure(it.localizedMessage ?: genericErrorMsg)
            }

    override fun sendResponse() = eudiWalletInteractor.sendRequestedDocuments()

    override fun observeResponse(): Flow<ProximityLoadingPartialState> {
        return eudiWalletInteractor.events.mapNotNull { response ->
            when (response) {

                // TODO This state should be fixed by Scytales
                is TransferEventPartialState.Error -> {
                    if (response.error == "Peer disconnected without proper session termination") {
                        ProximityLoadingPartialState.Success
                    } else {
                        ProximityLoadingPartialState.Failure(error = response.error)
                    }
                }

                else -> null
            }
        }.safeAsync {
            ProximityLoadingPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }
    }

    override fun stopPresentation() {
        eudiWalletInteractor.stopPresentation()
    }
}