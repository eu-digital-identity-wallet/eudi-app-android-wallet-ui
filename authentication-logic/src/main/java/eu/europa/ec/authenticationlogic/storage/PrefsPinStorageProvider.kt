/*
 * Copyright (c) 2025 European Commission
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PrefsPinStorageProvider(
    private val prefsController: PrefsController
) : PinStorageProvider {

    private companion object {
        private const val KEY_PIN_SALT = "PinSalt"
        private const val KEY_PIN_HASH = "PinHash"
        private const val KEY_PIN_ITERATIONS = "PinIterations"

        private const val SALT_SIZE_BYTES = 32
        private const val HASH_SIZE_BITS = 256
        private const val DEFAULT_ITERATIONS = 210_000
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    }

    override suspend fun hasPin(): Boolean = withContext(Dispatchers.IO) {
        val salt = prefsController.getString(KEY_PIN_SALT, "")
        val hash = prefsController.getString(KEY_PIN_HASH, "")
        val iterations = prefsController.getInt(KEY_PIN_ITERATIONS, 0)
        salt.isNotBlank() && hash.isNotBlank() && iterations > 0
    }

    override suspend fun setPin(pin: String) {

        val salt = ByteArray(SALT_SIZE_BYTES).also {
            SecureRandom().nextBytes(it)
        }

        val hash = withContext(Dispatchers.Default) {
            derivePinHash(
                pin = pin,
                salt = salt,
                iterations = DEFAULT_ITERATIONS
            )
        }

        withContext(Dispatchers.IO) {
            prefsController.setString(
                KEY_PIN_SALT,
                salt.encodeToBase64String(flags = Base64.NO_WRAP)
            )
            prefsController.setString(
                KEY_PIN_HASH,
                hash.encodeToBase64String(flags = Base64.NO_WRAP)
            )
            prefsController.setInt(KEY_PIN_ITERATIONS, DEFAULT_ITERATIONS)
        }
    }

    override suspend fun isPinValid(pin: String): Boolean {

        if (pin.isBlank()) return false

        val (saltBase64, hashBase64, iterations) = withContext(Dispatchers.IO) {
            Triple(
                prefsController.getString(KEY_PIN_SALT, ""),
                prefsController.getString(KEY_PIN_HASH, ""),
                prefsController.getInt(KEY_PIN_ITERATIONS, 0)
            )
        }

        if (saltBase64.isBlank() || hashBase64.isBlank() || iterations <= 0) {
            return false
        }

        val salt = try {
            saltBase64.decodeFromBase64(flags = Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            return false
        }

        val expectedHash = try {
            hashBase64.decodeFromBase64(flags = Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            return false
        }

        val candidateHash = withContext(Dispatchers.Default) {
            derivePinHash(
                pin = pin,
                salt = salt,
                iterations = iterations
            )
        }

        return constantTimeEquals(expectedHash, candidateHash)
    }

    private fun derivePinHash(
        pin: String,
        salt: ByteArray,
        iterations: Int
    ): ByteArray {
        val chars = pin.toCharArray()
        val spec = PBEKeySpec(
            chars,
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
            chars.fill('\u0000')
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