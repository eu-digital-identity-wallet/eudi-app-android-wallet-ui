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

package eu.europa.ec.uilogic.util

import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.IOException

class TestRemoteImageHelper {

    private val request = Request.Builder().url("https://issuer.example/logo.png").build()

    @Test
    fun `Given an https URL, Then it may be loaded`() {
        assertEquals(
            "https://issuer.example/logo.png",
            remoteImageSourceOrNull("https://issuer.example/logo.png")
        )
    }

    @Test
    fun `Given a local scheme, plain http or a malformed address, Then it is not loaded`() {
        listOf(
            "content://eu.europa.ec.euidi.provider/logs/eudi-android-wallet-logs0.txt",
            "file:///data/data/eu.europa.ec.euidi/files/logs/eudi-android-wallet-logs0.txt",
            "android.resource://eu.europa.ec.euidi/drawable/logo",
            "data:image/png;base64,iVBORw0KGgo=",
            "http://issuer.example/logo.png",
            "https:///logo.png",
            "https://issuer.example/bad path.png",
            "null",
            "",
        ).forEach { source ->
            assertNull(source, remoteImageSourceOrNull(source))
        }
    }

    @Test
    fun `Given a response that declares more than the limit, Then it fails before it is read`() {
        val limit = ResponseSizeLimit(maxBytes = 4)

        assertThrows(IOException::class.java) {
            limit.intercept(chainReturning(body = "12345".toResponseBody(PNG)))
        }
    }

    @Test
    fun `Given a response of unknown length beyond the limit, Then reading it fails`() {
        val limit = ResponseSizeLimit(maxBytes = 4)

        val response = limit.intercept(chainReturning(body = unknownLength("12345")))

        assertThrows(IOException::class.java) { response.body!!.bytes() }
    }

    @Test
    fun `Given a response within the limit, Then it is passed on unchanged`() {
        val limit = ResponseSizeLimit(maxBytes = 4)

        val response = limit.intercept(chainReturning(body = unknownLength("1234")))

        assertEquals("1234", response.body!!.string())
    }

    @Test
    fun `Given a redirect within the image's origin, Then it is followed`() {
        val response = SameOriginRedirects().intercept(
            redirectChain(
                requested = "https://issuer.example/logo",
                current = "https://ISSUER.example/v2/logo"
            )
        )

        assertEquals(200, response.code)
    }

    @Test
    fun `Given a redirect to another host, another port or plain http, Then the request fails`() {
        listOf(
            "https://cdn.example/logo",
            "https://issuer.example:8443/logo",
            "http://issuer.example/logo",
            "http://issuer.example:443/logo",
        ).forEach { current ->
            assertThrows(current, IOException::class.java) {
                SameOriginRedirects().intercept(
                    redirectChain(requested = "https://issuer.example/logo", current = current)
                )
            }
        }
    }

    @Test
    fun `Given the image client, When it is built, Then it limits responses and checks every redirect`() {
        assertFalse(remoteImageHttpClient.followSslRedirects)
        assertTrue(remoteImageHttpClient.interceptors.any { interceptor -> interceptor is ResponseSizeLimit })
        assertTrue(
            remoteImageHttpClient.networkInterceptors.any { interceptor -> interceptor is SameOriginRedirects }
        )
    }

    private fun redirectChain(requested: String, current: String): Interceptor.Chain {
        val requestedRequest = Request.Builder().url(requested).build()
        val currentRequest = Request.Builder().url(current).build()
        val call: Call = mock { on { request() } doReturn requestedRequest }
        val response = Response.Builder()
            .request(currentRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("".toResponseBody(PNG))
            .build()
        return mock {
            on { call() } doReturn call
            on { request() } doReturn currentRequest
            on { proceed(any()) } doReturn response
        }
    }

    private fun chainReturning(body: ResponseBody): Interceptor.Chain {
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body)
            .build()
        return mock {
            on { request() } doReturn request
            on { proceed(any()) } doReturn response
        }
    }

    private fun unknownLength(content: String): ResponseBody = object : ResponseBody() {
        private val source = Buffer().writeUtf8(content)
        override fun contentType() = PNG
        override fun contentLength() = -1L
        override fun source(): BufferedSource = source
    }

    private companion object {
        val PNG = "image/png".toMediaType()
    }
}