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

package eu.europa.ec.startupfeature.interactor

import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.startupfeature.repository.StartupRepoPartialState
import eu.europa.ec.startupfeature.repository.StartupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class StartupInteractorPartialState {
    data object Success : StartupInteractorPartialState()
    data class Failure(val error: String) : StartupInteractorPartialState()
}

interface StartupInteractor {
    fun test(): Flow<StartupInteractorPartialState>
}

class StartupInteractorImpl constructor(
    private val startupRepository: StartupRepository,
    private val resourceProvider: ResourceProvider
) : StartupInteractor {
    override fun test(): Flow<StartupInteractorPartialState> = flow {
        startupRepository.test().collect {
            when (it) {
                is StartupRepoPartialState.Failure -> emit(
                    StartupInteractorPartialState.Failure(
                        it.error ?: resourceProvider.genericErrorMessage()
                    )
                )

                is StartupRepoPartialState.Success -> emit(StartupInteractorPartialState.Success)
            }
        }

    }.safeAsync {
        StartupInteractorPartialState.Failure(
            it.localizedMessage ?: resourceProvider.genericErrorMessage()
        )
    }
}