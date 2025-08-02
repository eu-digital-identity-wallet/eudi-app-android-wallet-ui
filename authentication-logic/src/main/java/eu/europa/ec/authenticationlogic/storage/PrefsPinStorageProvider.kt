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

import eu.europa.ec.authenticationlogic.provider.PinStorageProvider
import eu.europa.ec.businesslogic.controller.crypto.CryptoController
import eu.europa.ec.businesslogic.controller.storage.PrefsController
import eu.europa.ec.businesslogic.extension.decodeFromBase64
import eu.europa.ec.businesslogic.extension.encodeToBase64String

class PrefsPinStorageProvider(
    private val prefsController: PrefsController,
    private val cryptoController: CryptoController
) : PinStorageProvider {

    /**
     * Retrieves the stored PIN after decrypting it.
     *
     * @return The decrypted PIN as a String. Returns an empty string if no PIN is stored or if decryption fails.
     */
    override fun retrievePin(): String = decryptedAndLoad()

    /**
     * Stores the given PIN in an encrypted format.
     * This method encrypts the provided PIN using cryptographic functions
     * and stores the encrypted data along with its initialization vector (IV)
     * in the preferences.
     *
     * @param pin The PIN to be stored.
     */
    override fun setPin(pin: String) {
        encryptAndStore(pin)
    }

    /**
     * Checks if the provided PIN is valid.
     *
     * @param pin The PIN to validate.
     * @return True if the provided PIN matches the stored PIN, false otherwise.
     */
    override fun isPinValid(pin: String): Boolean = retrievePin() == pin

    private fun encryptAndStore(pin: String) {

        val cipher = cryptoController.getCipher(
            encrypt = true,
            userAuthenticationRequired = false
        )

        val encryptedBytes = cryptoController.encryptDecrypt(
            cipher = cipher,
            byteArray = pin.toByteArray(Charsets.UTF_8)
        )

        val ivBytes = cipher?.iv ?: return

        prefsController.setString("PinEnc", encryptedBytes.encodeToBase64String())
        prefsController.setString("PinIv", ivBytes.encodeToBase64String())
    }

    private fun decryptedAndLoad(): String {

        val encryptedBase64 = prefsController.getString(
            "PinEnc", ""
        ).ifEmpty { return "" }

        val ivBase64 = prefsController.getString(
            "PinIv", ""
        ).ifEmpty { return "" }

        val cipher = cryptoController.getCipher(
            encrypt = false,
            ivBytes = decodeFromBase64(ivBase64),
            userAuthenticationRequired = false
        )

        val decryptedBytes = cryptoController.encryptDecrypt(
            cipher = cipher,
            byteArray = decodeFromBase64(encryptedBase64)
        )

        return decryptedBytes.toString(Charsets.UTF_8)
    }
}