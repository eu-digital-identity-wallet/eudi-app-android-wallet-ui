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

package eu.europa.ec.proximityfeature.interactor

import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.commonfeature.model.DocumentTypeUi
import eu.europa.ec.commonfeature.ui.request.model.UserDataDomain
import eu.europa.ec.commonfeature.ui.request.model.UserIdentificationDomain
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class ProximityRequestInteractorPartialState {
    data class Success(val userDataDomain: UserDataDomain) :
        ProximityRequestInteractorPartialState()

    data class Failure(val error: String) : ProximityRequestInteractorPartialState()
}

interface ProximityRequestInteractor {
    fun getUserData(): Flow<ProximityRequestInteractorPartialState>
}

class ProximityRequestInteractorImpl(
    private val resourceProvider: ResourceProvider,
) : ProximityRequestInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getUserData(): Flow<ProximityRequestInteractorPartialState> =
        flow {
            delay(2_000L)
            emit(
                ProximityRequestInteractorPartialState.Success(
                    userDataDomain = getFakeUserData()
                )
            )
        }.safeAsync {
            ProximityRequestInteractorPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
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
}