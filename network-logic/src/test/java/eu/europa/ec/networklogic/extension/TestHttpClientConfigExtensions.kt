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
import eu.europa.ec.networklogic.util.ResponseTooLargeException
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TestHttpClientConfigExtensions {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Test
    fun `Given the limits installed, When a server answers within them, Then the response arrives`() =
        coroutineRule.runTest {
            createClient { respond(ByteArray(LIMIT), HttpStatusCode.OK) }.use { client ->
                val body = withContext(Dispatchers.Default) { client.get(URL).bodyAsBytes() }

                assertEquals(LIMIT, body.size)
            }
        }

    @Test
    fun `Given the limits installed, When a server takes longer than the request timeout, Then the request fails`() =
        coroutineRule.runTest {
            createClient(timeout = SHORT_REQUEST_TIMEOUT) {
                delay(5.seconds)
                respond(ByteArray(1), HttpStatusCode.OK)
            }.use { client ->
                val failure = withContext(Dispatchers.Default) {
                    runCatching { client.get(URL).bodyAsBytes() }.exceptionOrNull()
                }

                assertTrue(failure is HttpRequestTimeoutException)
            }
        }

    @Test
    fun `Given the limits installed, When a body passes the configured size, Then the request fails`() =
        coroutineRule.runTest {
            createClient { respond(ByteArray(LIMIT + 1), HttpStatusCode.OK) }.use { client ->
                val failure = withContext(Dispatchers.Default) {
                    runCatching { client.get(URL).bodyAsBytes() }.exceptionOrNull()
                }

                assertTrue(
                    generateSequence(failure) { it.cause }.any { it is ResponseTooLargeException }
                )
            }
        }

    /**
     * Creates a client with a [LIMIT]-byte body limit and the given request timeout, whose every
     * request is answered by [handler]. Only the timeout test passes a short timeout: on a slow
     * machine a short one would fail the other tests before their checks run.
     *
     * @param timeout The request timeout.
     * @param handler The mock server.
     * @return The client.
     */
    private fun createClient(
        timeout: Duration = DEFAULT_TIMEOUT,
        handler: MockRequestHandler
    ): HttpClient {
        val networkConfig = object : NetworkConfig {
            override val connectTimeout: Duration = DEFAULT_TIMEOUT
            override val socketTimeout: Duration = DEFAULT_TIMEOUT
            override val requestTimeout: Duration = timeout
            override val maxResponseBodySize: Long = LIMIT.toLong()
        }
        return HttpClient(MockEngine(handler)) {
            installNetworkLimits(networkConfig)
        }
    }

    private companion object {
        const val LIMIT = 1024
        const val URL = "https://issuer.example/metadata"
        val DEFAULT_TIMEOUT = 30.seconds
        val SHORT_REQUEST_TIMEOUT = 200.milliseconds
    }
}
