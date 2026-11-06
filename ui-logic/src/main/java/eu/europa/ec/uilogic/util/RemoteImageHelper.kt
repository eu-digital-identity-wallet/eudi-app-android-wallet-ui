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

import eu.europa.ec.uilogic.navigation.helper.isOpenableExternalLink
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import java.io.IOException

/** The largest response, after decompression, that the wallet downloads for one remote image. */
const val MAX_REMOTE_IMAGE_BYTES: Long = 2L * 1024 * 1024

/**
 * [source] when the wallet may load it as a remote image: an `https` URL with a host. Images come
 * from issuer, relying-party or Trust Mark metadata, so local schemes such as `content://`,
 * `file://` or `android.resource://`, plain `http`, and anything that cannot be parsed are refused.
 */
fun remoteImageSourceOrNull(source: String): String? = source.takeIf(::isOpenableExternalLink)

/**
 * The client that downloads remote images: limited to [MAX_REMOTE_IMAGE_BYTES] per response, and
 * following a redirect only within the origin of the image asked for, over `https`.
 */
internal val remoteImageHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .followSslRedirects(false)
        .addInterceptor(ResponseSizeLimit(maxBytes = MAX_REMOTE_IMAGE_BYTES))
        .addNetworkInterceptor(SameOriginRedirects())
        .build()
}

/**
 * Fails a request that a redirect has taken off `https` or off the origin of the image first asked
 * for, so that an image on an allowed origin cannot send the wallet to another host.
 */
internal class SameOriginRedirects : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requested = chain.call().request().url
        val current = chain.request().url
        if (!current.isHttps || current.host != requested.host || current.port != requested.port) {
            throw IOException("Redirect off the origin of the image")
        }
        return chain.proceed(chain.request())
    }
}

/**
 * Fails a response whose body is larger than [maxBytes]: at once when the declared length is
 * larger, and otherwise as soon as more bytes than that have been read.
 */
internal class ResponseSizeLimit(private val maxBytes: Long) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val body = response.body ?: return response
        if (body.contentLength() > maxBytes) {
            response.close()
            throw IOException("Response larger than $maxBytes bytes")
        }
        return response.newBuilder()
            .body(LimitedResponseBody(body = body, maxBytes = maxBytes))
            .build()
    }
}

private class LimitedResponseBody(
    private val body: ResponseBody,
    private val maxBytes: Long,
) : ResponseBody() {

    private val limitedSource: BufferedSource by lazy {
        object : ForwardingSource(body.source()) {
            private var bytesRead = 0L

            override fun read(sink: Buffer, byteCount: Long): Long {
                val read = super.read(sink, byteCount)
                if (read > 0) {
                    bytesRead += read
                    if (bytesRead > maxBytes) {
                        throw IOException("Response larger than $maxBytes bytes")
                    }
                }
                return read
            }
        }.buffer()
    }

    override fun contentType(): MediaType? = body.contentType()

    override fun contentLength(): Long = body.contentLength()

    override fun source(): BufferedSource = limitedSource
}