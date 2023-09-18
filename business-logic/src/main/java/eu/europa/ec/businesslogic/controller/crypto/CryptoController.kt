/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.businesslogic.controller.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

typealias GUID = String

interface CryptoController {
    fun generateCodeVerifier(): String

    /**
     * Returns the [Cipher] needed to create the [androidx.biometric.BiometricPrompt.CryptoObject]
     * for biometric authentication.
     * [encrypt] should be set to true if the cipher should encrypt, false otherwise.
     * [ivBytes] is needed only for decryption to create the [IvParameterSpec].
     */
    fun getBiometricCipher(encrypt: Boolean = false, ivBytes: ByteArray? = null): Cipher?

    /**
     * Returns the [ByteArray] after the encryption/decryption from the given [Cipher].
     * [cipher] the biometric cipher needed. This can be null but then an empty [ByteArray] is
     * returned.
     * [byteArray] that needed to be encrypted or decrypted (Depending always on [Cipher] provided.
     */
    fun encryptDecryptBiometric(cipher: Cipher?, byteArray: ByteArray): ByteArray
}

class CryptoControllerImpl constructor(
    private val keystoreController: KeystoreController
) : CryptoController {

    companion object {
        private const val AES_7_EXTERNAL_TRANSFORMATION = "AES/CBC/PKCS7Padding"
        const val MAX_GUID_LENGTH = 64
    }

    override fun generateCodeVerifier(): String {
        val code = ByteArray(32)
        SecureRandom().apply {
            nextBytes(code)
        }
        return Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    override fun getBiometricCipher(encrypt: Boolean, ivBytes: ByteArray?): Cipher? =
        try {
            Cipher.getInstance(AES_7_EXTERNAL_TRANSFORMATION).apply {
                if (encrypt) {
                    init(
                        Cipher.ENCRYPT_MODE,
                        keystoreController.retrieveOrGenerateBiometricSecretKey()
                    )
                } else {
                    init(
                        Cipher.DECRYPT_MODE,
                        keystoreController.retrieveOrGenerateBiometricSecretKey(),
                        IvParameterSpec(ivBytes)
                    )
                }
            }
        } catch (e: Exception) {
            null
        }


    override fun encryptDecryptBiometric(cipher: Cipher?, byteArray: ByteArray): ByteArray {
        return cipher?.doFinal(byteArray) ?: ByteArray(0)
    }
}
