/*
 * Copyright (c) 2023 European Commission
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
    val encodedString = Base64.encodeToString(this, Base64.NO_WRAP) ?: return null
    return encodedString.splitToLines(64)
}

/**
 * Encodes a byte array into a Base64 string.
 *
 * @param flag An integer representing the encoding options. This is directly passed
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
fun ByteArray.encodeToBase64String(flag: Int = Base64.DEFAULT): String {
    return Base64.encodeToString(this, flag)
}

/**
 * Attempts to decode a [Base64] encoded String.
 * @return A [ByteArray] with the encoded bytes if it succeeds,
 * empty if it fails.
 */
fun decodeFromBase64(base64EncodedString: String, flag: Int = Base64.DEFAULT): ByteArray {
    return try {
        Base64.decode(base64EncodedString, flag)
    } catch (e: Exception) {
        ByteArray(size = 0)
    }
}

