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

package eu.europa.ec.authenticationlogic.storage

import android.util.Base64
import eu.europa.ec.authenticationlogic.provider.PinStorageProvider
import eu.europa.ec.businesslogic.controller.storage.PrefsController
import eu.europa.ec.businesslogic.extension.decodeFromBase64
import eu.europa.ec.businesslogic.extension.encodeToBase64String
import eu.europa.ec.businesslogic.model.SecurePin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PrefsPinStorageProvider(
    private val prefsController: PrefsController
) : PinStorageProvider {

    private companion object {
        const val KEY_PIN_SALT = "PinSalt"
        const val KEY_PIN_HASH = "PinHash"
        const val KEY_PIN_ITERATIONS = "PinIterations"
        const val SALT_SIZE_BYTES = 32
        const val HASH_SIZE_BITS = 256
        const val DEFAULT_ITERATIONS = 210_000
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
    }

    /**
     * Checks if a PIN has been previously stored in the persistent storage.
     *
     * It verifies that all required components (salt, hash, and iteration count)
     * are present and valid.
     *
     * @return `true` if a valid PIN configuration exists, `false` otherwise.
     */
    override suspend fun hasPin(): Boolean = withContext(Dispatchers.IO) {
        val salt = prefsController.getString(KEY_PIN_SALT, "")
        val hash = prefsController.getString(KEY_PIN_HASH, "")
        val iterations = prefsController.getInt(KEY_PIN_ITERATIONS, 0)
        salt.isNotBlank() && hash.isNotBlank() && iterations > 0
    }

    /**
     * Hashes and securely stores a new PIN using PBKDF2 with a randomly generated salt.
     *
     * This method generates a unique salt, derives a hash from the provided [pin],
     * and persists the salt, hash, and iteration count to the preference storage.
     *
     * @param pin The PIN to be stored. The value is consumed and cleared by this method.
     */
    override suspend fun setPin(pin: SecurePin) {

        val salt = ByteArray(SALT_SIZE_BYTES).also {
            SecureRandom().nextBytes(it)
        }

        val pinData = pin.getAndClear()
        var hash: ByteArray? = null
        try {
            hash = withContext(Dispatchers.Default) {
                pinData.useChars { chars ->
                    derivePinHash(
                        pin = chars,
                        salt = salt,
                        iterations = DEFAULT_ITERATIONS
                    )
                }
            }
            val currentHash = hash

            withContext(Dispatchers.IO) {
                prefsController.setString(
                    KEY_PIN_SALT,
                    salt.encodeToBase64String(flags = Base64.NO_WRAP)
                )
                prefsController.setString(
                    KEY_PIN_HASH,
                    currentHash.encodeToBase64String(flags = Base64.NO_WRAP)
                )
                prefsController.setInt(KEY_PIN_ITERATIONS, DEFAULT_ITERATIONS)
            }
        } finally {
            pinData.close()
            salt.fill(0)
            hash?.fill(0)
        }
    }

    /**
     * Validates whether the provided [pin] matches the stored hash.
     *
     * This method retrieves the salt, hash, and iteration count from the storage provider,
     * derives a hash from the candidate PIN, and performs a constant-time comparison
     * to determine validity while mitigating timing attacks.
     *
     * @param pin The candidate PIN to be validated. The value is consumed and cleared by this method.
     * @return `true` if the PIN is valid and matches the stored credentials; `false` if the
     * PIN is blank, storage is uninitialized, or the PIN is incorrect.
     */
    override suspend fun isPinValid(pin: SecurePin): Boolean {

        if (pin.length == 0) {
            pin.close()
            return false
        }

        val (saltBase64, hashBase64, iterations) = withContext(Dispatchers.IO) {
            Triple(
                prefsController.getString(KEY_PIN_SALT, ""),
                prefsController.getString(KEY_PIN_HASH, ""),
                prefsController.getInt(KEY_PIN_ITERATIONS, 0)
            )
        }

        if (saltBase64.isBlank() || hashBase64.isBlank() || iterations <= 0) {
            pin.close()
            return false
        }

        val salt = try {
            saltBase64.decodeFromBase64(flags = Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            pin.close()
            return false
        }

        val expectedHash = try {
            hashBase64.decodeFromBase64(flags = Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            pin.close()
            salt.fill(0)
            return false
        }

        val pinData = pin.getAndClear()
        var candidateHash: ByteArray? = null
        return try {
            candidateHash = withContext(Dispatchers.Default) {
                pinData.useChars { chars ->
                    derivePinHash(
                        pin = chars,
                        salt = salt,
                        iterations = iterations
                    )
                }
            }
            constantTimeEquals(expectedHash, candidateHash)
        } finally {
            pinData.close()
            salt.fill(0)
            expectedHash.fill(0)
            candidateHash?.fill(0)
        }
    }

    private fun derivePinHash(
        pin: CharArray,
        salt: ByteArray,
        iterations: Int
    ): ByteArray {
        val spec = PBEKeySpec(
            pin,
            salt,
            iterations,
            HASH_SIZE_BITS
        )

        return try {
            SecretKeyFactory
                .getInstance(ALGORITHM)
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
            pin.fill('\u0000')
        }
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false

        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }
}