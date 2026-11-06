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

package eu.europa.ec.networklogic.util

import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TestResponseSizeLimit {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Test
    fun `Given a body within the limit, When it is read, Then it arrives whole`() =
        coroutineRule.runTest {
            createClient(ByteReadChannel(ByteArray(LIMIT))).use { client ->
                val body = client.get(URL).bodyAsBytes()

                assertEquals(LIMIT, body.size)
            }
        }

    @Test
    fun `Given a body without a length that passes the limit, When it is read, Then the request fails`() =
        coroutineRule.runTest {
            createClient(ByteReadChannel(ByteArray(LIMIT + 1))).use { client ->
                val failure = runCatching { client.get(URL).bodyAsBytes() }.exceptionOrNull()

                assertTrue(failure.isCausedByResponseTooLarge())
            }
        }

    @Test
    fun `Given a Content-Length past the limit, When the response arrives, Then the request fails before the body is read`() =
        coroutineRule.runTest {
            val headers = headersOf(HttpHeaders.ContentLength, (LIMIT + 1).toString())
            createClient(ByteReadChannel(ByteArray(1)), headers).use { client ->
                val failure = runCatching { client.get(URL).bodyAsBytes() }.exceptionOrNull()

                assertTrue(failure.isCausedByResponseTooLarge())
            }
        }

    /**
     * A body that passes the limit and never ends fails as soon as the limit is passed. Were the
     * limit checked only after Ktor had saved the body, the request would wait for the end of the
     * body and the timeout would fail the test.
     */
    @Test
    fun `Given a body that passes the limit and never ends, When it is read, Then the request fails without waiting for its end`() =
        coroutineRule.runTest {
            val server = CoroutineScope(Dispatchers.Default)
            try {
                createClient(server.createEndlessBody()).use { client ->
                    val failure = withContext(Dispatchers.Default) {
                        withTimeout(5.seconds) {
                            runCatching { client.get(URL).bodyAsBytes() }.exceptionOrNull()
                        }
                    }

                    assertTrue(failure.isCausedByResponseTooLarge())
                }
            } finally {
                server.cancel()
            }
        }

    /**
     * Creates a client with [ResponseSizeLimit] set to [LIMIT], whose every request gets [body].
     *
     * @param body The response body.
     * @param headers The response headers; none by default, so the body has no length.
     * @return The client.
     */
    private fun createClient(
        body: ByteReadChannel,
        headers: Headers = Headers.Empty,
    ): HttpClient = HttpClient(MockEngine { respond(body, HttpStatusCode.OK, headers) }) {
        install(ResponseSizeLimit) {
            maxResponseBodySize = LIMIT.toLong()
        }
    }

    /**
     * Creates a body, written in [this] scope, that sends more than [LIMIT] bytes and then stays
     * open until the scope is cancelled.
     *
     * @return The body.
     */
    private fun CoroutineScope.createEndlessBody(): ByteReadChannel = writer {
        channel.writeFully(ByteArray(LIMIT + 1))
        channel.flush()
        awaitCancellation()
    }.channel

    /**
     * Whether [this] failure, or one of its causes, is a [ResponseTooLargeException].
     *
     * @return True when the size limit failed the request.
     */
    private fun Throwable?.isCausedByResponseTooLarge(): Boolean =
        generateSequence(this) { it.cause }.any { it is ResponseTooLargeException }

    private companion object {
        const val LIMIT = 1024
        const val URL = "https://issuer.example/metadata"
    }
}
