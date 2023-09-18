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

package eu.europa.ec.startupfeature.repository

import eu.europa.ec.networklogic.api.ApiClient
import eu.europa.ec.networklogic.model.request.DummyRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

sealed class StartupRepoPartialState {
    data object Success : StartupRepoPartialState()
    data class Failure(val error: String?) : StartupRepoPartialState()
}

interface StartupRepository {
    fun test(): Flow<StartupRepoPartialState>
}

class StartupRepositoryImpl(
    private val apiClient: ApiClient
) : StartupRepository {
    override fun test(): Flow<StartupRepoPartialState> = flow {

        val response = apiClient.test(DummyRequest(""))

        if (response.isSuccessful) {
            emit(StartupRepoPartialState.Success)
        } else {
            emit(
                StartupRepoPartialState.Failure(response.errorBody()?.string())
            )
        }
    }.catch {
        emit(
            StartupRepoPartialState.Failure(it.localizedMessage)
        )
    }
}