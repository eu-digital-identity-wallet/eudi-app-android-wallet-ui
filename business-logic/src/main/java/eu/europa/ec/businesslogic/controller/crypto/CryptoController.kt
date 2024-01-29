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

class CryptoControllerImpl(
    private val keystoreController: KeystoreController
) : CryptoController {

    companion object {
        private const val AES_EXTERNAL_TRANSFORMATION = "AES/GCM/NoPadding"
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
            Cipher.getInstance(AES_EXTERNAL_TRANSFORMATION).apply {
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
