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

package eu.europa.ec.businesslogic.controller.crypto

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

typealias GUID = String

interface CryptoController {

    /**
     * Generates a code verifier for Proof Key for Code Exchange (PKCE).
     *
     * This function generates a cryptographically random string that is used as the code
     * verifier in the PKCE flow. The code verifier is a high-entropy string that is
     * difficult to guess. It is used to protect against authorization code interception attacks.
     *
     * The generated code verifier is a Base64 URL-safe encoded string without padding or wrapping.
     *
     * @return A [String] representing the generated code verifier.
     */
    suspend fun generateCodeVerifier(): String

    /**
     * Retrieves a [Cipher] instance configured for either encryption or decryption.
     *
     * This function initializes a [Cipher] object using the AES/GCM/NoPadding transformation.
     * The behavior of the cipher (encryption or decryption) is determined by the [encrypt] parameter.
     *
     * - If [encrypt] is `true`, the cipher is initialized in `Cipher.ENCRYPT_MODE`.
     * - If [encrypt] is `false`, the cipher is initialized in `Cipher.DECRYPT_MODE`, and the
     *   [ivBytes] parameter is required to provide the Initialization Vector (IV) for the
     *   GCMParameterSpec.
     *
     * The secret key used for cipher initialization is retrieved or generated via the
     * `keystoreController`.
     *
     * @param encrypt A [Boolean] indicating whether the cipher should be initialized for
     * encryption (`true`) or decryption (`false`). Defaults to `false`.
     * @param ivBytes An optional [ByteArray] containing the Initialization Vector. This is
     * required only when `encrypt` is `false` (i.e., for decryption).
     * @param userAuthenticationRequired A [Boolean] indicating if user authentication is
     * required for the cryptographic operation. Defaults to `true`.
     * @return A configured [Cipher] instance if initialization is successful, or `null` if an
     * exception occurs during initialization.
     */
    suspend fun getCipher(
        encrypt: Boolean = false,
        ivBytes: ByteArray? = null,
        userAuthenticationRequired: Boolean = true
    ): Cipher?


    /**
     * Returns the [ByteArray] after the encryption/decryption from the given [Cipher].
     * [cipher] the cipher needed. This can be null but then an empty [ByteArray] is
     * returned.
     * [byteArray] that needed to be encrypted or decrypted (Depending always on [Cipher] provided.
     */
    suspend fun encryptDecrypt(cipher: Cipher?, byteArray: ByteArray): ByteArray

    /**
     * Generates a random cryptographic key as a byte array.
     *
     * This function utilizes [SecureRandom] to generate a high-entropy 32-byte array,
     * which is then converted into a hexadecimal string and returned as UTF-8 bytes.
     * This is primarily used for securing sensitive data or generating passphrases.
     *
     * @return A [ByteArray] containing the generated hexadecimal key.
     */
    suspend fun createRandomKey(context: Context): ByteArray
}

class CryptoControllerImpl(
    private val keystoreController: KeystoreController
) : CryptoController {

    companion object {
        private const val AES_EXTERNAL_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 128
        const val MAX_GUID_LENGTH = 64
    }

    override suspend fun generateCodeVerifier(): String {
        val code = ByteArray(32)
        SecureRandom().apply {
            nextBytes(code)
        }
        return Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    override suspend fun getCipher(
        encrypt: Boolean,
        ivBytes: ByteArray?,
        userAuthenticationRequired: Boolean
    ): Cipher? =
        try {
            Cipher.getInstance(AES_EXTERNAL_TRANSFORMATION).apply {
                if (encrypt) {
                    init(
                        Cipher.ENCRYPT_MODE,
                        keystoreController.retrieveOrGenerateSecretKey(userAuthenticationRequired)
                    )
                } else {
                    init(
                        Cipher.DECRYPT_MODE,
                        keystoreController.retrieveOrGenerateSecretKey(userAuthenticationRequired),
                        GCMParameterSpec(IV_SIZE, ivBytes)
                    )
                }
            }
        } catch (_: Exception) {
            null
        }


    override suspend fun encryptDecrypt(cipher: Cipher?, byteArray: ByteArray): ByteArray {
        return cipher?.doFinal(byteArray) ?: ByteArray(0)
    }

    override suspend fun createRandomKey(context: Context): ByteArray {
        val hex: String = run {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            val generated = bytes.joinToString("") { "%02x".format(it) }
            generated
        }
        return hex.toByteArray(Charsets.UTF_8)
    }
}
