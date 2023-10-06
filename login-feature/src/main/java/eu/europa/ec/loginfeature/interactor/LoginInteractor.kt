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

package eu.europa.ec.loginfeature.interactor

import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.loginfeature.repository.LoginRepoPartialState
import eu.europa.ec.loginfeature.repository.LoginRepository
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class LoginInteractorPartialState {
    data object Success : LoginInteractorPartialState()
    data class Failure(val error: String) : LoginInteractorPartialState()
}

interface LoginInteractor {
    fun test(): Flow<LoginInteractorPartialState>
}

class LoginInteractorImpl constructor(
    private val loginRepository: LoginRepository,
    private val resourceProvider: ResourceProvider
) : LoginInteractor {
    override fun test(): Flow<LoginInteractorPartialState> = flow {
        loginRepository.test().collect {
            when (it) {
                is LoginRepoPartialState.Failure -> emit(
                    LoginInteractorPartialState.Failure(
                        it.error ?: resourceProvider.genericErrorMessage()
                    )
                )

                is LoginRepoPartialState.Success -> emit(LoginInteractorPartialState.Success)
            }
        }

    }.safeAsync {
        LoginInteractorPartialState.Failure(
            it.localizedMessage ?: resourceProvider.genericErrorMessage()
        )
    }
}