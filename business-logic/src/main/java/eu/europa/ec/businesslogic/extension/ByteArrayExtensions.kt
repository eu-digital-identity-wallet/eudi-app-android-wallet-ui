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

package eu.europa.ec.businesslogic.extension

import android.util.Base64

/**
 * Converts Byte Array to encoded Pem base64 String
 *
 * @receiver Byte Array object
 * @return String object
 */
fun ByteArray.encodeToPemBase64String(): String? {
    val encodedString = this.encodeToBase64String(flags = Base64.NO_WRAP)
    return encodedString.splitToLines(64)
}

/**
 * Encodes a byte array into a Base64 string.
 *
 * @param flags An integer representing the encoding options. This is directly passed
 *             to the `Base64.encodeToString()` method. Common values include:
 *             - `Base64.DEFAULT`: Default encoding.
 *             - `Base64.NO_WRAP`: No line wrapping.
 *             - `Base64.URL_SAFE`: URL and filename safe encoding.
 *             - `Base64.NO_PADDING`: No padding with '=' characters.
 *             - `Base64.CRLF`: Using CRLF line separators
 *             These flags can be combined using bitwise OR.
 *             Defaults to `Base64.DEFAULT`.
 * @return The Base64 encoded string representation of the byte array.
 * @see Base64.encodeToString
 */
fun ByteArray.encodeToBase64String(flags: Int = Base64.DEFAULT): String {
    return Base64.encodeToString(this, flags)
}

/**
 * Decodes a Base64 encoded string into a list of possible byte array representations.
 *
 * This function attempts to decode the input string by:
 * 1. Stripping common prefixes (e.g., "base64,").
 * 2. Removing all whitespace characters.
 * 3. Attempting to fix potential missing padding.
 * 4. Iterating through various decoding flags (DEFAULT, NO_WRAP, URL_SAFE) to find valid outputs.
 *
 * @param value The Base64 encoded string to decode.
 * @return A list of [ByteArray] objects representing the successful decodings. Returns an empty list if no valid decodings are found or if the input is blank.
 */
fun decodeBase64ToByteArrays(value: String): List<ByteArray> {
    return runCatching {
        val sanitizedBase64 = value
            .extractBase64Payload()
            .removeWhitespace()
            .takeIf { base64 -> base64.isNotBlank() }
            ?: return emptyList()

        val candidates = listOf(
            sanitizedBase64,
            sanitizedBase64.withBase64Padding()
        ).distinct()

        val decodingFlags = listOf(
            Base64.DEFAULT,
            Base64.NO_WRAP,
            Base64.URL_SAFE,
            Base64.URL_SAFE or Base64.NO_WRAP
        )

        candidates
            .flatMap { candidate ->
                decodingFlags.map { flags ->
                    candidate to flags
                }
            }
            .mapNotNull { (candidate, flags) ->
                candidate.decodeBase64OrNull(flags)
            }
    }.getOrDefault(emptyList())
}

private fun String.extractBase64Payload(): String {
    return substringAfter(delimiter = "base64,", missingDelimiterValue = this)
}

private fun String.removeWhitespace(): String {
    return filterNot { character ->
        character.isWhitespace()
    }
}

private fun String.withBase64Padding(): String {
    val missingPadding = (4 - length % 4) % 4

    return if (missingPadding == 0) {
        this
    } else {
        this + "=".repeat(missingPadding)
    }
}

private fun String.decodeBase64OrNull(flags: Int): ByteArray? {
    return runCatching {
        Base64.decode(this, flags)
    }.getOrNull()
}