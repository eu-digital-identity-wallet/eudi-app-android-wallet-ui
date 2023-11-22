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
import eu.europa.ec.commonfeature.model.DocumentTypeUi
import eu.europa.ec.commonfeature.ui.request.model.UserDataDomain
import eu.europa.ec.commonfeature.ui.request.model.UserIdentificationDomain
import eu.europa.ec.eudi.iso18013.transfer.RequestDocument
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

sealed class ProximityRequestInteractorPartialState {
    data class Success(val userDataDomain: List<RequestDocument>) :
        ProximityRequestInteractorPartialState()

    data class Failure(val error: String) : ProximityRequestInteractorPartialState()
    data object Disconnect : ProximityRequestInteractorPartialState()
}

interface ProximityRequestInteractor {
    fun getUserData(): Flow<ProximityRequestInteractorPartialState>
    fun cancelTransfer()
}

class ProximityRequestInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val eudiWallet: EudiWallet,
    private val eudiWalletInteractor: EudiWalletInteractor
) : ProximityRequestInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getUserData(): Flow<ProximityRequestInteractorPartialState> =
        eudiWalletInteractor.events.mapNotNull {
            when (it) {
                is TransferEventPartialState.RequestReceived -> {
                    ProximityRequestInteractorPartialState.Success(emptyList())
                }

                is TransferEventPartialState.Error -> {
                    ProximityRequestInteractorPartialState.Failure(error = it.error)
                }

                is TransferEventPartialState.Disconnected -> {
                    ProximityRequestInteractorPartialState.Disconnect
                }

                else -> null
            }
        }

    override fun cancelTransfer() {
        eudiWallet.stopPresentation()
        eudiWalletInteractor.cancelScope()
    }
}


private fun getFakeUserData(): UserDataDomain {
    return UserDataDomain(
        documentTypeUi = DocumentTypeUi.DRIVING_LICENSE,
        optionalFields = listOf(
            UserIdentificationDomain(
                name = "Family Name",
                value = "Doe"
            ),
            UserIdentificationDomain(
                name = "First Name",
                value = "Jane"
            ),
            UserIdentificationDomain(
                name = "Date of Birth",
                value = "21 Oct 1994"
            ),
            UserIdentificationDomain(
                name = "Portrait",
                value = "user_picture"
            ),
            UserIdentificationDomain(
                name = "Family Name",
                value = "Doe"
            ),
            UserIdentificationDomain(
                name = "First Name",
                value = "Jane"
            ),
            UserIdentificationDomain(
                name = "Date of Birth",
                value = "21 Oct 1994"
            ),
            UserIdentificationDomain(
                name = "Portrait",
                value = "user_picture"
            )
        ),
        requiredFieldsTitle = "Verification Data",
        requiredFields = listOf(
            UserIdentificationDomain(
                name = "Issuance date",
                value = null
            ),
            UserIdentificationDomain(
                name = "Expiration date",
                value = null
            ),
            UserIdentificationDomain(
                name = "Country of issuance",
                value = null
            ),
            UserIdentificationDomain(
                name = "Issuing authority",
                value = null
            )
        )
    )
}