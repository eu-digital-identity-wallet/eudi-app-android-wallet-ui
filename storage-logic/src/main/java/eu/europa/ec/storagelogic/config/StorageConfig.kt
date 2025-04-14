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

package eu.europa.ec.storagelogic.config

import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.businesslogic.extension.encodeToPemBase64String
import eu.europa.ec.storagelogic.model.RealmBookmark
import eu.europa.ec.storagelogic.model.RealmRevokedDocument
import eu.europa.ec.storagelogic.model.RealmTransactionLog
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import java.security.SecureRandom

interface StorageConfig {
    val storageName: String
    val storageVersion: Long
    val realmConfiguration: RealmConfiguration
}

class StorageConfigImpl(
    private val prefKeys: PrefKeys,
) : StorageConfig {

    override val storageName: String
        get() = "eudi.app.wallet.storage"

    override val storageVersion: Long
        get() = 1L
    override val realmConfiguration: RealmConfiguration
        get() = RealmConfiguration.Builder(
            schema = setOf(
                RealmBookmark::class,
                RealmTransactionLog::class,
                RealmRevokedDocument::class
            )
        )
            .name(storageName)
            .schemaVersion(storageVersion)
            .encryptionKey(retrieveOrGenerateStorageKey())
            .deleteRealmIfMigrationNeeded()
            .build()

    private fun retrieveOrGenerateStorageKey(): ByteArray {
        val storedKey = prefKeys.getStorageKey()
        if (storedKey != null) {
            return storedKey
        }
        val key = ByteArray(Realm.ENCRYPTION_KEY_LENGTH)
        SecureRandom().nextBytes(key)
        prefKeys.setStorageKey(key.encodeToPemBase64String().orEmpty())
        return key
    }
}