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

package eu.europa.ec.corelogic.util

import io.ktor.client.engine.HttpClientEngineFactory
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.multipaz.securearea.CreateKeySettings
import org.multipaz.securearea.KeyInfo
import org.multipaz.securearea.KeyUnlockData
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.cloud.CloudSecureArea
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.EcPublicKey
import org.multipaz.crypto.EcSignature
import org.multipaz.storage.ephemeral.EphemeralStorage
import kotlin.time.ExperimentalTime

/**
 * Wrapper for CloudSecureArea that provides non-suspend create function
 * and implements lazy initialization pattern.
 */
@OptIn(ExperimentalTime::class)
class CloudSecureAreaWrapper private constructor(
    private val wrappedIdentifier: String,
    private val serverUrl: String,
    private val httpClientEngineFactory: HttpClientEngineFactory<*>
) : SecureArea {

    private var cloudSecureArea: CloudSecureArea? = null
    private var isInitialized = false
    private val initializationMutex = Mutex()

    companion object {
        /**
         * Non-suspend create function that only instantiates the wrapper.
         * Actual initialization is deferred until first use.
         */
        fun create(
            identifier: String,
            serverUrl: String,
            httpClientEngineFactory: HttpClientEngineFactory<*>
        ): CloudSecureAreaWrapper {
            return CloudSecureAreaWrapper(identifier, serverUrl, httpClientEngineFactory)
        }
    }

    /**
     * Lazy initialization - called before any suspend operation
     */
    private suspend fun ensureInitialized() {
        if (!isInitialized) {
            initializationMutex.withLock {
                if (!isInitialized) {
                    cloudSecureArea = CloudSecureArea.create(
                        storage = EphemeralStorage(),
                        identifier = wrappedIdentifier,
                        serverUrl = serverUrl,
                        httpClientEngineFactory = httpClientEngineFactory
                    )
                    isInitialized = true
                }
            }
        }
    }

    private fun requireInitialized(): CloudSecureArea {
        return cloudSecureArea ?: throw IllegalStateException("CloudSecureArea not initialized. Call a suspend function first.")
    }

    override val displayName: String
        get() = if (isInitialized) requireInitialized().displayName else "CloudSecureArea (not initialized)"

    override val identifier: String
        get() = wrappedIdentifier

    override val supportedAlgorithms: List<Algorithm>
        get() = if (isInitialized) requireInitialized().supportedAlgorithms else emptyList()

    override suspend fun createKey(alias: String?, createKeySettings: CreateKeySettings): KeyInfo {
        ensureInitialized()
        return requireInitialized().createKey(alias, createKeySettings)
    }

    override suspend fun deleteKey(alias: String) {
        ensureInitialized()
        requireInitialized().deleteKey(alias)
    }

    override suspend fun sign(alias: String, dataToSign: ByteArray, keyUnlockData: KeyUnlockData?): EcSignature {
        ensureInitialized()
        return requireInitialized().sign(alias, dataToSign, keyUnlockData)
    }

    override suspend fun keyAgreement(alias: String, otherKey: EcPublicKey, keyUnlockData: KeyUnlockData?): ByteArray {
        ensureInitialized()
        return requireInitialized().keyAgreement(alias, otherKey, keyUnlockData)
    }

    override suspend fun getKeyInfo(alias: String): KeyInfo {
        ensureInitialized()
        return requireInitialized().getKeyInfo(alias)
    }

    override suspend fun getKeyInvalidated(alias: String): Boolean {
        ensureInitialized()
        return requireInitialized().getKeyInvalidated(alias)
    }
}
