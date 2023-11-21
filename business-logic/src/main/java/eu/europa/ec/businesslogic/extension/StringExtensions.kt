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

import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import java.net.URLEncoder

fun String.encodeToBase64(): String = Base64.encodeToString(
    this.toByteArray(Charsets.UTF_8),
    Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE
)

fun String.decodeFromBase64(): String = Base64.decode(
    this, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE
).toString(Charsets.UTF_8)

fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")

fun String.toUri(): Uri? = try {
    Uri.parse(this)
} catch (e: Exception) {
    null
}

fun String.validateAndFormatUrl(): String {
    if (isEmpty() || contains("http") || contains("https")) {
        return this
    }
    return "https://${this.lowercase()}"
}

inline fun <reified T> String.parseFromJson(): T? {
    return try {
        Gson().fromJson(this, T::class.java)
    } catch (_: Exception) {
        null
    }
}

/**
 * Converts Pem base64 String to Byte Array
 *
 * @receiver String object
 * @return Byte Array object
 */
fun String.decodeFromPemBase64String(): ByteArray? {
    return Base64.decode(this.replace("\n", ""), Base64.NO_WRAP)
}

/**
 * String split into substrings. The maximum length of the substring
 * is determined by a parameter @param lineLength
 *
 * @receiver String object
 * @param lineLength - maximum number of characters per line
 * @return String object
 */
fun String.splitToLines(lineLength: Int): String {
    return if (lineLength <= 0 || this.length <= lineLength) this
    else {
        var result = ""
        var index = 0
        while (index < length) {
            val line = substring(index, Math.min(index + lineLength, length))
            result += (if (result.isEmpty()) line else "\n$line")
            index += lineLength
        }
        result
    }
}