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

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import java.security.KeyStore
import java.util.UUID
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

interface KeystoreController {
    fun retrieveOrGenerateBiometricSecretKey(): SecretKey?
}

class KeystoreControllerImpl constructor(
    private val prefKeys: PrefKeys,
    private val logController: LogController,
) : KeystoreController {

    companion object {
        private const val STORE_TYPE = "AndroidKeyStore"
    }

    private var androidKeyStore: KeyStore? = null

    init {
        loadKeyStore()
    }

    /**
     * Load/Init KeyStore
     */
    private fun loadKeyStore() {
        try {
            androidKeyStore = KeyStore.getInstance(STORE_TYPE)
            androidKeyStore?.load(null)
        } catch (e: Exception) {
            logController.e(this.javaClass.simpleName, e)
        }
    }

    /**
     * Retrieves the existing biometric secret key if exists or generates a new one if it is the
     * first time.
     */
    override fun retrieveOrGenerateBiometricSecretKey(): SecretKey? {
        return androidKeyStore?.let {
            val alias = prefKeys.getBiometricAlias()
            if (alias.isEmpty()) {
                val newAlias = createPublicKey()
                generateBiometricSecretKey(newAlias)
                prefKeys.setBiometricAlias(newAlias)
                getBiometricSecretKey(it, newAlias)
            } else {
                getBiometricSecretKey(it, alias)
            }
        }
    }

    private fun generateBiometricSecretKey(alias: String) {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, STORE_TYPE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                /* Commented out on purpose as some devices failed when crypto object is returned
                from success authentication. This should be re-enabled when google has fixed that.
                 */
//                .setUserAuthenticationRequired(true)
//                .setInvalidatedByBiometricEnrollment(true)
//                .setUserAuthenticationValidityDurationSeconds(-1)
                .build()
        )
        keyGenerator.generateKey()
    }

    private fun getBiometricSecretKey(keyStore: KeyStore, alias: String): SecretKey {
        keyStore.load(null)
        return keyStore.getKey(alias, null) as SecretKey
    }

    /**
     * Get random GUID string
     *
     * @return a string containing 64 characters
     */
    private fun createPublicKey(): GUID =
        (UUID.randomUUID().toString() + UUID.randomUUID()
            .toString()).take(CryptoControllerImpl.MAX_GUID_LENGTH)
}