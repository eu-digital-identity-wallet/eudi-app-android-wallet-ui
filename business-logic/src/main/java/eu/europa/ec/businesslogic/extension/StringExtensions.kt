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

import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import java.net.URLEncoder
import kotlin.math.min

/**
 * Encodes the string into a Base64 string using the UTF-8 charset.
 *
 * This function applies the following configuration:
 * - [Base64.NO_WRAP]: Omits all line terminators.
 * - [Base64.NO_PADDING]: Omits the padding '=' characters at the end.
 * - [Base64.URL_SAFE]: Uses '-' and '_' instead of '+' and '/' to make the output safe for URLs.
 *
 * @return A URL-safe Base64 encoded representation of the string.
 */
fun String.encodeToBase64(flags: Int = Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE): String =
    Base64.encodeToString(
        this.toByteArray(Charsets.UTF_8),
        flags
    )

/**
 * Decodes a Base64 encoded string back to its original UTF-8 string representation
 */
fun String.decodeFromBase64ToString(flags: Int = Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE): String =
    this.decodeFromBase64(flags).toString(Charsets.UTF_8)

/**
 * Decodes a Base64 encoded string back to its original byte array representation.
 *
 * This function reverses the encoding performed by [encodeToBase64], using the following configuration:
 * - Supports URL-safe decoding (interprets '-' as '+' and '_' as '/').
 * - Handles strings without padding characters and ignores line wraps.
 *
 * @return The decoded [ByteArray].
 */
fun String.decodeFromBase64(flags: Int = Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE): ByteArray =
    Base64.decode(
        this,
        flags
    )

/**
 * Encodes a string into a URL-safe format using UTF-8 encoding.
 *
 * This function takes a string as input and returns its URL-encoded version.  URL encoding
 * replaces unsafe characters (e.g., spaces, special characters) with a "%" followed by
 * their hexadecimal representation.  UTF-8 encoding is used to ensure proper handling
 * of Unicode characters.
 *
 * @return The URL-encoded string.
 *
 * Example:
 * ```
 * val input = "Hello, world!"
 * val encoded = input.urlEncode() // encoded will be "Hello%2C+world%21"
 * ```
 */
fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")

/**
 * Converts a string to a [Uri].
 *
 * If the string is a valid URI, it will be parsed and returned as a [Uri] object.
 * If the string is not a valid URI, an empty [Uri] ([Uri.EMPTY]) will be returned instead.
 * This prevents crashes due to invalid URI strings and provides a fallback mechanism.
 *
 * @return The parsed [Uri] if the string is a valid URI, otherwise [Uri.EMPTY].
 */
fun String.toUri(): Uri = try {
    Uri.parse(this)
} catch (_: Exception) {
    Uri.EMPTY
}

/**
 * Parses a JSON string into an object of the specified type [T].
 *
 * This function attempts to deserialize a JSON string using the Gson library.  If the parsing is successful,
 * it returns the resulting object of type [T].  If any exception occurs during parsing (e.g., invalid JSON format,
 * type mismatch), it gracefully handles the exception and returns `null`.
 *
 * **Important:**  This function relies on Gson for deserialization. Ensure that your project has the Gson library
 * added as a dependency.  Additionally, the type [T] must be a valid data class or class that Gson can deserialize
 * based on its default configuration.  Consider adding Gson annotations (e.g., `@SerializedName`) to your data
 * classes if you need custom JSON field mapping.
 *
 * @param T The type to deserialize the JSON string into.  This is reified, meaning the type information is
 *          preserved at runtime, allowing Gson to determine the target class.
 * @return An object of type [T] if parsing is successful, or `null` if an exception occurs during parsing.
 *
 * **Example Usage:**
 * ```kotlin
 * data class User(val id: Int, val name: String)
 *
 * val jsonString = """{"id":123,"name":"John Doe"}"""
 * val user: User? = jsonString.parseFromJson<User>()
 *
 * if (user != null) {
 *     println("Parsed user: $user") // Output: Parsed user: User(id=123, name=John Doe)
 * } else {
 *     println("Failed to parse user from JSON")
 * }
 *
 * val invalidJson = "not a valid json"
 * val result: User? = invalidJson.parseFromJson<User>()
 * println(result) // Output: null
 * ```
 */
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
            val line = substring(index, min(index + lineLength, length))
            result += (if (result.isEmpty()) line else "\n$line")
            index += lineLength
        }
        result
    }
}

/**
 * Returns the first part of the string before the specified separator.
 *
 * If the separator is found, this function returns the substring preceding the first occurrence.
 * If the separator is not found or the string is empty, the original string is returned.
 *
 * @param separator The string used to split the receiver string.
 * @return The substring before the first occurrence of the separator, or the original string if not found.
 */
fun String.firstPart(separator: String): String = this.split(separator).firstOrNull() ?: this

/**
 * Returns the current string if it is not `null`, empty, or blank.
 * Otherwise, returns the specified default string.
 *
 * This function checks both nullability and blankness, meaning
 * it treats strings containing only whitespace as invalid.
 *
 * @param default The string to return if the current string is `null`, empty, or blank.
 * @return The current string if it is not `null`, empty, or blank; otherwise, the default string.
 *
 * Example usage:
 * ```
 * val str1: String? = null
 * val str2: String? = "   "
 * val str3: String? = "Hello, Kotlin!"
 *
 * println(str1.ifEmptyOrNull("Default")) // Output: Default
 * println(str2.ifEmptyOrNull("Default")) // Output: Default
 * println(str3.ifEmptyOrNull("Default")) // Output: Hello, Kotlin!
 * ```
 */
fun String?.ifEmptyOrNull(default: String): String {
    return if (this.isNullOrBlank()) default else this
}

/**
 * Decodes a Base64 string into a list of possible [ByteArray] representations by attempting
 * multiple decoding strategies.
 *
 * This function handles various Base64 formats by:
 * 1. Extracting the payload if the string contains a "base64," prefix.
 * 2. Removing all whitespace characters.
 * 3. Attempting to decode both the raw string and a padded version of the string.
 * 4. Iterating through several decoding flags ([Base64.DEFAULT], [Base64.NO_WRAP], [Base64.URL_SAFE]).
 *
 * It is particularly useful when the exact encoding format (standard vs. URL-safe) or
 * padding status of the input string is uncertain.
 *
 * @return A list of successfully decoded [ByteArray] objects. Returns an empty list if
 * the string is blank or if all decoding attempts fail.
 */
fun String.decodeBase64ToByteArrays(): List<ByteArray> {

    fun String.extractBase64Payload(): String {
        return substringAfter(delimiter = "base64,", missingDelimiterValue = this)
    }

    fun String.removeWhitespace(): String {
        return filterNot { character ->
            character.isWhitespace()
        }
    }

    fun String.withBase64Padding(): String {
        val missingPadding = (4 - length % 4) % 4

        return if (missingPadding == 0) {
            this
        } else {
            this + "=".repeat(missingPadding)
        }
    }

    return runCatching {
        val sanitizedBase64 = this
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
            .map { (candidate, flags) ->
                candidate.decodeFromBase64(flags)
            }
    }.getOrDefault(emptyList())
}
