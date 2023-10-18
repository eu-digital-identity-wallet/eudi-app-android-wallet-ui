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
import eu.europa.ec.loginfeature.model.FaqItem
import eu.europa.ec.loginfeature.repository.LoginRepoPartialState
import eu.europa.ec.loginfeature.repository.LoginRepository
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class FaqInteractorPartialState {
    data object Success : FaqInteractorPartialState()
    data class Failure(val error: String) : FaqInteractorPartialState()
}

interface LoginInteractor {
    fun test(): Flow<FaqInteractorPartialState>
    fun initializeData(): List<FaqItem>
}

class FaqInteractorImpl constructor(
    private val loginRepository: LoginRepository,
    private val resourceProvider: ResourceProvider,
) : LoginInteractor {
    override fun test(): Flow<FaqInteractorPartialState> = flow {
        loginRepository.test().collect {
            when (it) {
                is LoginRepoPartialState.Failure -> emit(
                    FaqInteractorPartialState.Failure(
                        it.error ?: resourceProvider.genericErrorMessage()
                    )
                )

                is LoginRepoPartialState.Success -> emit(FaqInteractorPartialState.Success)
            }
        }

    }.safeAsync {
        FaqInteractorPartialState.Failure(
            it.localizedMessage ?: resourceProvider.genericErrorMessage()
        )
    }

    override fun initializeData(): List<FaqItem> {
       return  listOf(
            FaqItem(
                title = "Question A goes Here",
                description = "Lorem ipsum dolor sit amet," +
                        " consectetur adipiscing elit,"
            ),
            FaqItem(
                title = "Question B goes Here",
                description = "Duis aute irure dolor in reprehenderit in" +
                        " voluptate velit esse cillum dolore eu fugiat nulla pariatur."
            ),
            FaqItem(
                title = "Question C goes Here",
                description = "Excepteur sint occaecat cupidatat non proident, " +
                        "sunt in culpa qui officia deserunt mollit anim id est laborum."
            ),
            FaqItem(
                title = "Question D goes Here",
                description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
                        "sed  magn laboris nisi ut aliquip ex ea commodo consequat."
            ),
            FaqItem(
                title = "Question E goes Here",
                description = "Duis aute irure dolor in reprehenderit" +
                        " in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
            ),
            FaqItem(
                title = "Question F goes Here",
                description = "Excepteur sint occaecat cupidatat non proident, " +
                        "sunt in culpa qui officia deserunt mollit anim id est laborum."
            ))

    }
}