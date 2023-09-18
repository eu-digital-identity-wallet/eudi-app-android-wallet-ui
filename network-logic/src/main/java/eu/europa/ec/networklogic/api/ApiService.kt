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

package eu.europa.ec.networklogic.api

import eu.europa.ec.networklogic.model.request.DummyRequest
import eu.europa.ec.networklogic.model.response.DummyResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface Api {
    @POST("test/path")
    suspend fun test(
        @Body body: DummyRequest
    ): Response<DummyResponse>
}

interface ApiClient {
    suspend fun test(
        body: DummyRequest
    ): Response<DummyResponse>
}

class ApiClientImpl constructor(private val apiService: Api) : ApiClient {
    override suspend fun test(body: DummyRequest): Response<DummyResponse> {
        return apiService.test(body)
    }
}