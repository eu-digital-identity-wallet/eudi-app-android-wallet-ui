/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.networklogic.extension

import eu.europa.ec.networklogic.config.NetworkConfig
import eu.europa.ec.networklogic.util.ResponseSizeLimit
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout

/**
 * Applies the timeouts and the response-size limit of [networkConfig] to the client being built,
 * with [HttpTimeout] and [ResponseSizeLimit].
 *
 * @param networkConfig The limits to apply.
 */
fun HttpClientConfig<*>.installNetworkLimits(networkConfig: NetworkConfig) {
    install(HttpTimeout) {
        connectTimeoutMillis = networkConfig.connectTimeout.inWholeMilliseconds
        socketTimeoutMillis = networkConfig.socketTimeout.inWholeMilliseconds
        requestTimeoutMillis = networkConfig.requestTimeout.inWholeMilliseconds
    }
    install(ResponseSizeLimit) {
        maxResponseBodySize = networkConfig.maxResponseBodySize
    }
}
