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

package eu.europa.ec.loginfeature.repository

import eu.europa.ec.networklogic.api.ApiClient
import eu.europa.ec.networklogic.model.request.DummyRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow


sealed class LoginRepoPartialState {
    data object Success : LoginRepoPartialState()
    data class Failure(val error: String?) : LoginRepoPartialState()
}

interface LoginRepository {
    fun test(): Flow<LoginRepoPartialState>
}


class LoginRepositoryImpl(
    private val apiClient: ApiClient
) : LoginRepository {
    override fun test(): Flow<LoginRepoPartialState> = flow {

        val response = apiClient.test(DummyRequest(""))

        if (response.isSuccessful) {
            emit(LoginRepoPartialState.Success)
        } else {
            emit(
                LoginRepoPartialState.Failure(response.errorBody()?.string())
            )
        }
    }.catch {
        emit(
            LoginRepoPartialState.Failure(it.localizedMessage)
        )
    }
}