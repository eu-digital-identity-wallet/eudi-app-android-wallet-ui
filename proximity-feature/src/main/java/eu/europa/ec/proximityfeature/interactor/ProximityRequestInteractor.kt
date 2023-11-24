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
import eu.europa.ec.commonfeature.interactor.TransferEventPartialState
import eu.europa.ec.commonfeature.ui.request.Event
import eu.europa.ec.commonfeature.ui.request.model.RequestDataUi
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

sealed class ProximityRequestInteractorPartialState {
    data class Success(
        val verifierName: String? = null,
        val requestDocuments: List<RequestDataUi<Event>>
    ) :
        ProximityRequestInteractorPartialState()

    data class Failure(val error: String) : ProximityRequestInteractorPartialState()
    data object Disconnect : ProximityRequestInteractorPartialState()
}

interface ProximityRequestInteractor {
    fun getRequestDocuments(): Flow<ProximityRequestInteractorPartialState>
    fun stopPresentation()
    fun updateRequestedDocuments(items: List<RequestDataUi<Event>>)
}

class ProximityRequestInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val eudiWalletInteractor: EudiWalletInteractor
) : ProximityRequestInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getRequestDocuments(): Flow<ProximityRequestInteractorPartialState> =
        eudiWalletInteractor.events.mapNotNull { response ->
            when (response) {
                is TransferEventPartialState.RequestReceived -> {
                    ProximityRequestInteractorPartialState.Success(
                        verifierName = response.verifierName,
                        requestDocuments = response.requestDataUi
                    )
                }

                is TransferEventPartialState.Error -> {
                    ProximityRequestInteractorPartialState.Failure(error = response.error)
                }

                is TransferEventPartialState.Disconnected -> {
                    ProximityRequestInteractorPartialState.Disconnect
                }

                else -> null
            }
        }.safeAsync {
            ProximityRequestInteractorPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun stopPresentation() {
        eudiWalletInteractor.stopPresentation()
    }

    override fun updateRequestedDocuments(items: List<RequestDataUi<Event>>) {
        eudiWalletInteractor.updateRequestedDocuments(items)
    }
}