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

package eu.europa.ec.businesslogic.controller.storage

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesFileSerializer
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.tink.AeadSerializer
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import eu.europa.ec.businesslogic.extension.decodeFromBase64
import eu.europa.ec.businesslogic.extension.encodeToBase64String
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

interface PrefsController {
    fun contains(key: String): Boolean
    fun clear(key: String)
    fun clear()
    fun setString(key: String, value: String)
    fun setLong(key: String, value: Long)
    fun setBool(key: String, value: Boolean)
    fun getString(key: String, defaultValue: String): String
    fun getLong(key: String, defaultValue: Long): Long
    fun getBool(key: String, defaultValue: Boolean): Boolean
    fun setInt(key: String, value: Int)
    fun getInt(key: String, defaultValue: Int): Int
}

/**
 * Implementation of [PrefsController] that provides a secure, encrypted key-value storage
 * using Jetpack DataStore and Google Tink for Authenticated Encryption with Associated Data (AEAD).
 *
 * This class handles the persistence of primitive types (String, Long, Boolean, Int) and ensures
 * that the underlying data is encrypted at rest using a master key stored in the Android Keystore.
 *
 * All operations are performed synchronously using [runBlocking] on [Dispatchers.IO] to maintain
 * compatibility.
 *
 * @property resourceProvider An instance of [ResourceProvider] used to access the application context.
 */
class PrefsControllerImpl(
    private val resourceProvider: ResourceProvider
) : PrefsController {

    private companion object {
        const val DATASTORE_FILE = "eudi-wallet.preferences_pb"
        const val KEYSET_PREFS_FILE = "eudi-wallet-datastore-keyset"
        const val KEYSET_PREFS_KEY = "prefs_datastore_keyset"
        const val MASTER_KEY_URI = "android-keystore://eudi-wallet-datastore-master-key"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val dataStore: DataStore<Preferences> by lazy {
        val context = resourceProvider.provideContext().applicationContext
        DataStoreFactory.create(
            serializer = AeadSerializer(
                aead = provideAead(context),
                wrappedSerializer = PreferencesFileSerializer,
                associatedData = DATASTORE_FILE.toByteArray(Charsets.UTF_8)
            ),
            scope = scope,
            produceFile = { context.preferencesDataStoreFile(DATASTORE_FILE) }
        )
    }

    override fun contains(key: String): Boolean {
        val prefs = read()
        return prefs.asMap().keys.any { it.name == key }
    }

    override fun clear(key: String) {
        edit { prefs ->
            prefs.remove(stringPreferencesKey(key))
            prefs.remove(longPreferencesKey(key))
            prefs.remove(booleanPreferencesKey(key))
            prefs.remove(intPreferencesKey(key))
        }
    }

    override fun clear() {
        edit { it.clear() }
    }

    override fun setString(key: String, value: String) {
        edit { prefs ->
            prefs[stringPreferencesKey(key)] = value
        }
    }

    override fun setLong(key: String, value: Long) {
        edit { prefs ->
            prefs[longPreferencesKey(key)] = value
        }
    }

    override fun setBool(key: String, value: Boolean) {
        edit { prefs ->
            prefs[booleanPreferencesKey(key)] = value
        }
    }

    override fun setInt(key: String, value: Int) {
        edit { prefs ->
            prefs[intPreferencesKey(key)] = value
        }
    }

    override fun getString(key: String, defaultValue: String): String {
        return read()[stringPreferencesKey(key)] ?: defaultValue
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return read()[longPreferencesKey(key)] ?: defaultValue
    }

    override fun getBool(key: String, defaultValue: Boolean): Boolean {
        return read()[booleanPreferencesKey(key)] ?: defaultValue
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return read()[intPreferencesKey(key)] ?: defaultValue
    }

    private fun read(): Preferences = runBlocking(Dispatchers.IO) {
        dataStore.data.first()
    }

    private fun edit(block: suspend (MutablePreferences) -> Unit) =
        runBlocking(Dispatchers.IO) {
            dataStore.edit { prefs ->
                block(prefs)
            }
        }

    private fun provideAead(context: Context): Aead {

        AeadConfig.register()

        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_PREFS_KEY, KEYSET_PREFS_FILE)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle

        return keysetHandle.getPrimitive(
            RegistryConfiguration.get(),
            Aead::class.java
        )
    }
}

interface PrefKeys {
    fun getCryptoAlias(): String
    fun setCryptoAlias(value: String)
    fun setSessionId(value: String)
    fun getSessionId(): String
    fun setDbKey(value: ByteArray)
    fun getDbKey(): ByteArray?
}

class PrefKeysImpl(
    private val prefsController: PrefsController
) : PrefKeys {

    override fun getCryptoAlias(): String {
        return prefsController.getString("CryptoAlias", "")
    }

    override fun setCryptoAlias(value: String) {
        prefsController.setString("CryptoAlias", value)
    }

    override fun setSessionId(value: String) {
        prefsController.setString("SessionId", value)
    }

    override fun getSessionId(): String {
        return prefsController.getString("SessionId", "")
    }

    override fun setDbKey(value: ByteArray) {
        val encoded = value.encodeToBase64String(flags = Base64.NO_WRAP)
        prefsController.setString("dbKey", encoded)
    }

    override fun getDbKey(): ByteArray? {
        val encoded = prefsController.getString("dbKey", "").ifBlank { return null }
        return encoded.decodeFromBase64(flags = Base64.NO_WRAP)
    }
}