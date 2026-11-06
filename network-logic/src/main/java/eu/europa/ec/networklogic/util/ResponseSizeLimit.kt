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

import io.ktor.client.call.replaceResponse
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.HttpReceivePipeline
import io.ktor.http.contentLength
import io.ktor.util.pipeline.PipelinePhase
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import kotlinx.coroutines.CoroutineScope
import java.io.IOException
import kotlin.coroutines.CoroutineContext

private const val BUFFER_SIZE = 8 * 1024

/** Settings of [ResponseSizeLimit]. */
class ResponseSizeLimitConfig {

    /** The largest response body read, in bytes. */
    var maxResponseBodySize: Long = Long.MAX_VALUE
}

/**
 * Thrown when a response body is larger than [ResponseSizeLimitConfig.maxResponseBodySize].
 *
 * @param maxResponseBodySize The limit the body passed, in bytes.
 */
class ResponseTooLargeException(maxResponseBodySize: Long) :
    IOException("Response body larger than $maxResponseBodySize bytes")

/**
 * Refuses a response body larger than [ResponseSizeLimitConfig.maxResponseBodySize]. It runs
 * before Ktor saves the body in memory: a body whose `Content-Length` passes the limit is refused
 * before any of it is read, and every other body is counted as it arrives and refused once it
 * passes the limit. It reads `HttpResponse.rawContent`, an `InternalAPI`, as Ktor's own
 * `BodyProgress` does, because no public API gives the body before it is saved.
 */
@OptIn(InternalAPI::class)
val ResponseSizeLimit = createClientPlugin("ResponseSizeLimit", ::ResponseSizeLimitConfig) {
    val maxResponseBodySize = pluginConfig.maxResponseBodySize
    val checkSizePhase = PipelinePhase("CheckResponseSize")
    client.receivePipeline.insertPhaseBefore(HttpReceivePipeline.Before, checkSizePhase)
    client.receivePipeline.intercept(checkSizePhase) { response ->
        val contentLength = response.contentLength()
        if (contentLength != null && contentLength > maxResponseBodySize) {
            throw ResponseTooLargeException(maxResponseBodySize)
        }
        val limited = response.rawContent.limitedTo(maxResponseBodySize, response.coroutineContext)
        proceedWith(response.call.replaceResponse { limited }.response)
    }
}

/**
 * Copies [this] channel to a new one, failing the copy with [ResponseTooLargeException] once more
 * than [maxResponseBodySize] bytes have passed. A failed copy also cancels [this] channel, so the
 * connection is not read any further.
 *
 * @param maxResponseBodySize The largest number of bytes copied.
 * @param context The response's coroutine context, which the copy runs in.
 * @return The channel the response body is then read from.
 */
private fun ByteReadChannel.limitedTo(
    maxResponseBodySize: Long,
    context: CoroutineContext,
): ByteReadChannel = CoroutineScope(context).writer {
    val buffer = ByteArray(BUFFER_SIZE)
    var total = 0L
    try {
        while (true) {
            val read = this@limitedTo.readAvailable(buffer)
            if (read == -1) break
            total += read
            if (total > maxResponseBodySize) throw ResponseTooLargeException(maxResponseBodySize)
            channel.writeFully(buffer, 0, read)
        }
    } catch (cause: Throwable) {
        this@limitedTo.cancel(cause)
        throw cause
    }
}.channel
