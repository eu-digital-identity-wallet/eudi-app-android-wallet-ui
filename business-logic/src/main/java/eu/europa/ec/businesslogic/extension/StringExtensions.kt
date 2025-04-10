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

/**
 * Encodes a string to a Base64 string using UTF-8 charset and URL-safe encoding.
 *
 * This function encodes the string using the standard Base64 algorithm, but with the following modifications:
 * - Uses UTF-8 encoding to convert the string to bytes.
 * - Removes padding characters from the end of the encoded string.
 * - Does not wrap the output.  It will be a single line.
 * - Uses URL-safe characters (replacing '+' with '-' and '/' with '_').
 *
 * These modifications are suitable for encoding data intended to be used in URLs or other contexts where standard Base64 encoding may be problematic due to padding or special characters.
 *
 * @return The Base64 encoded string.
 */
fun String.encodeToBase64(): String = Base64.encodeToString(
    this.toByteArray(Charsets.UTF_8),
    Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE
)

/**
 * Decodes a Base64 encoded string to its original UTF-8 representation.
 *  It uses URL_SAFE, NO_WRAP and NO_PADDING flags for decoding.
 *
 * @return The decoded string.
 */
fun String.decodeFromBase64(): String = Base64.decode(
    this, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE
).toString(Charsets.UTF_8)

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
            val line = substring(index, Math.min(index + lineLength, length))
            result += (if (result.isEmpty()) line else "\n$line")
            index += lineLength
        }
        result
    }
}

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

private val FY_SEED = intArrayOf(1, 3, 5, 7, 9, 2, 4, 6, 8)

internal fun String.shuffle(seed: IntArray = FY_SEED): String =
    encodeToBase64().computeShuffle(seed)

internal fun String.unShuffle(seed: IntArray = FY_SEED): String =
    computeShuffle(seed, true).decodeFromBase64()

private fun String.computeShuffle(seed: IntArray, unShuffle: Boolean = false): String {

    val items = mutableMapOf<Int, String?>()
    this.toCharArray().forEachIndexed { index, character -> items[index] = character.toString() }

    val iterator = mutableListOf<Int>()
    items.forEach { iterator.add(it.key) }

    if (unShuffle) iterator.reverse()

    iterator.forEach { i ->

        val k = seed[i % seed.size] % items.size

        val element1 = items[k]
        val element2 = items[i]

        items[k] = element2
        items[i] = element1
    }

    return items.map { it.value }.joinToString(separator = "")
}
