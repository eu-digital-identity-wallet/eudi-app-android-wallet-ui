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

package eu.europa.ec.onlineAuthentication.interactor

import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.onlineAuthentication.model.UserDataDomain
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class OnlineAuthenticationInteractorPartialState {
    data class Success(val userDataDomain: UserDataDomain) :
        OnlineAuthenticationInteractorPartialState()

    data class Failure(val error: String) : OnlineAuthenticationInteractorPartialState()
}

interface OnlineAuthenticationInteractor {
    fun getUserData(userId: String): Flow<OnlineAuthenticationInteractorPartialState>
}

class OnlineAuthenticationInteractorImpl(
    private val resourceProvider: ResourceProvider,
) : OnlineAuthenticationInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getUserData(userId: String): Flow<OnlineAuthenticationInteractorPartialState> =
        flow {
            delay(2_000L)

            emit(
                OnlineAuthenticationInteractorPartialState.Success(
                    userDataDomain = UserDataDomain(
                        id = "AG6743267807776",
                        dateOfBirth = "10/12/1990",
                        taxClearanceNumber = "67769685649007-9"
                    )
                )
            )
        }.safeAsync {
            OnlineAuthenticationInteractorPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }
}