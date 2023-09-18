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
