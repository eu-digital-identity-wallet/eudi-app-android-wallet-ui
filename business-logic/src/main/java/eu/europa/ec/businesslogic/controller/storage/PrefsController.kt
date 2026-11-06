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
import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetReader
import com.google.crypto.tink.LegacyKeysetSerialization
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.integration.android.SharedPrefKeysetReader
import eu.europa.ec.businesslogic.extension.decodeFromBase64
import eu.europa.ec.businesslogic.extension.encodeToBase64String
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.security.GeneralSecurityException

interface PrefsController {
    suspend fun contains(key: String): Boolean
    suspend fun clear(key: String)
    suspend fun clear()

    /** Removes every preference except those named in [keys], in a single edit. */
    suspend fun clearExcept(keys: Set<String>)
    suspend fun setString(key: String, value: String)
    suspend fun setLong(key: String, value: Long)
    suspend fun setBool(key: String, value: Boolean)
    suspend fun getString(key: String, defaultValue: String): String
    suspend fun getLong(key: String, defaultValue: Long): Long
    suspend fun getBool(key: String, defaultValue: Boolean): Boolean
    suspend fun setInt(key: String, value: Int)
    suspend fun getInt(key: String, defaultValue: Int): Int
}

/**
 * Implementation of [PrefsController] that provides a secure, encrypted key-value storage
 * using Jetpack DataStore and Google Tink for Authenticated Encryption with Associated Data (AEAD).
 *
 * This class handles the persistence of primitive types (String, Long, Boolean, Int) and ensures
 * that the underlying data is encrypted at rest using a master key stored in the Android Keystore.
 *
 * All operations are exposed as suspend functions and executed by DataStore on its IO-backed scope.
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

    override suspend fun contains(key: String): Boolean {
        val prefs = read()
        return prefs.asMap().keys.any { it.name == key }
    }

    override suspend fun clear(key: String) {
        edit { prefs ->
            prefs.remove(stringPreferencesKey(key))
            prefs.remove(longPreferencesKey(key))
            prefs.remove(booleanPreferencesKey(key))
            prefs.remove(intPreferencesKey(key))
        }
    }

    override suspend fun clear() {
        edit { it.clear() }
    }

    override suspend fun clearExcept(keys: Set<String>) {
        edit { prefs ->
            prefs.asMap().keys
                .filter { it.name !in keys }
                .forEach { prefs.remove(it) }
        }
    }

    override suspend fun setString(key: String, value: String) {
        edit { prefs ->
            prefs[stringPreferencesKey(key)] = value
        }
    }

    override suspend fun setLong(key: String, value: Long) {
        edit { prefs ->
            prefs[longPreferencesKey(key)] = value
        }
    }

    override suspend fun setBool(key: String, value: Boolean) {
        edit { prefs ->
            prefs[booleanPreferencesKey(key)] = value
        }
    }

    override suspend fun setInt(key: String, value: Int) {
        edit { prefs ->
            prefs[intPreferencesKey(key)] = value
        }
    }

    override suspend fun getString(key: String, defaultValue: String): String {
        return read()[stringPreferencesKey(key)] ?: defaultValue
    }

    override suspend fun getLong(key: String, defaultValue: Long): Long {
        return read()[longPreferencesKey(key)] ?: defaultValue
    }

    override suspend fun getBool(key: String, defaultValue: Boolean): Boolean {
        return read()[booleanPreferencesKey(key)] ?: defaultValue
    }

    override suspend fun getInt(key: String, defaultValue: Int): Int {
        return read()[intPreferencesKey(key)] ?: defaultValue
    }

    private suspend fun read(): Preferences = dataStore.data.first()

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        dataStore.edit { prefs ->
            block(prefs)
        }
    }

    private fun provideAead(context: Context): Aead {

        AeadConfig.register()

        val keysetHandle = keystoreBackedKeysetManager(context).keysetHandle

        return keysetHandle.getPrimitive(
            RegistryConfiguration.get(),
            Aead::class.java
        )
    }

    /**
     * The preferences keyset, refused unless the Android Keystore protects it. When Tink cannot use
     * its Keystore master key, it writes the keyset in cleartext to the app's shared preferences and
     * still returns a working AEAD, so the preferences, which hold the PIN hash and the database key,
     * would be encrypted with a key stored beside them. A keyset written that way stays in cleartext
     * after the Keystore works again, while `isUsingKeystore` then reports true, so the stored keyset
     * is checked as well. A keyset that is not protected and has encrypted nothing yet, because the
     * preferences file does not exist, is discarded and created again once; otherwise, or if the
     * second one is not protected either, [KeystoreUnavailableException] is thrown and the
     * preferences are not opened.
     */
    private fun keystoreBackedKeysetManager(context: Context): AndroidKeysetManager {
        repeat(2) {
            val manager = AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_PREFS_KEY, KEYSET_PREFS_FILE)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
            val storedKeyset = SharedPrefKeysetReader(context, KEYSET_PREFS_KEY, KEYSET_PREFS_FILE)
            if (manager.isUsingKeystore && !isCleartextKeyset(storedKeyset)) return manager
            if (!discardUnusedKeyset(context)) throw KeystoreUnavailableException()
        }
        throw KeystoreUnavailableException()
    }

    /** Removes the stored keyset unless the preferences file exists; returns whether it did. */
    private fun discardUnusedKeyset(context: Context): Boolean {
        if (context.preferencesDataStoreFile(DATASTORE_FILE).exists()) return false
        context.getSharedPreferences(KEYSET_PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .remove(KEYSET_PREFS_KEY)
            .apply()
        return true
    }
}

/**
 * Whether [keyset] holds a keyset in cleartext, the same test Tink applies: an encrypted keyset does
 * not parse as a plain one.
 */
internal fun isCleartextKeyset(keyset: KeysetReader): Boolean = try {
    LegacyKeysetSerialization.parseKeyset(keyset, InsecureSecretKeyAccess.get())
    true
} catch (_: IOException) {
    false
} catch (_: GeneralSecurityException) {
    false
}

/**
 * The preferences keyset is not protected by the Android Keystore, so the wallet does not open
 * its preferences.
 */
class KeystoreUnavailableException : GeneralSecurityException(
    "The preferences keyset is not protected by the Android Keystore"
)

interface PrefKeys {
    suspend fun getTrustMarkIntroductionCompleted(): Boolean
    suspend fun setTrustMarkIntroductionCompleted(value: Boolean)

    suspend fun getShowBatchIssuanceCounter(): Boolean
    suspend fun setShowBatchIssuanceCounter(value: Boolean)

    suspend fun getRegistrationCheckEnabled(): Boolean
    suspend fun setRegistrationCheckEnabled(value: Boolean)

    /** Whether the wallet offers its documents through the Digital Credential API; on by default. */
    suspend fun getDCAPIEnabled(): Boolean
    suspend fun setDCAPIEnabled(value: Boolean)

    suspend fun getCryptoAlias(): String
    suspend fun setCryptoAlias(value: String)
    suspend fun setSessionId(value: String)
    suspend fun getSessionId(): String
    suspend fun setDbKey(value: ByteArray)
    suspend fun getDbKey(): ByteArray?

    /**
     * The task and link fingerprint the host activity recorded when a screen last used the link the
     * task was started with, or empty. It holds no link, only a SHA-256 fingerprint.
     */
    suspend fun getOpenedLaunchLink(): String
    suspend fun setOpenedLaunchLink(value: String)

    /**
     * Clears every preference except the session id and the database key, in a single edit, for the
     * factory reset: the running app still needs both, and a reader never sees them missing.
     */
    suspend fun clearAllExceptSessionIdAndDbKey()
}

class PrefKeysImpl(
    private val prefsController: PrefsController
) : PrefKeys {

    private companion object {
        const val SESSION_ID = "SessionId"
        const val DB_KEY = "dbKey"
    }

    override suspend fun getTrustMarkIntroductionCompleted(): Boolean {
        return prefsController.getBool("TrustMarkIntroductionCompleted", false)
    }

    override suspend fun setTrustMarkIntroductionCompleted(value: Boolean) {
        prefsController.setBool("TrustMarkIntroductionCompleted", value)
    }

    override suspend fun getShowBatchIssuanceCounter(): Boolean {
        return prefsController.getBool("ShowBatchIssuanceCounter", true)
    }

    override suspend fun setShowBatchIssuanceCounter(value: Boolean) {
        prefsController.setBool("ShowBatchIssuanceCounter", value)
    }

    override suspend fun getRegistrationCheckEnabled(): Boolean {
        return prefsController.getBool("RegistrationCheckEnabled", false)
    }

    override suspend fun setRegistrationCheckEnabled(value: Boolean) {
        prefsController.setBool("RegistrationCheckEnabled", value)
    }

    override suspend fun getDCAPIEnabled(): Boolean {
        return prefsController.getBool("DCAPIEnabled", true)
    }

    override suspend fun setDCAPIEnabled(value: Boolean) {
        prefsController.setBool("DCAPIEnabled", value)
    }

    override suspend fun getCryptoAlias(): String {
        return prefsController.getString("CryptoAlias", "")
    }

    override suspend fun setCryptoAlias(value: String) {
        prefsController.setString("CryptoAlias", value)
    }

    override suspend fun setSessionId(value: String) {
        prefsController.setString(SESSION_ID, value)
    }

    override suspend fun getSessionId(): String {
        return prefsController.getString(SESSION_ID, "")
    }

    override suspend fun setDbKey(value: ByteArray) {
        val encoded = value.encodeToBase64String(flags = Base64.NO_WRAP)
        prefsController.setString(DB_KEY, encoded)
    }

    override suspend fun getDbKey(): ByteArray? {
        val encoded = prefsController.getString(DB_KEY, "").ifBlank { return null }
        return encoded.decodeFromBase64(flags = Base64.NO_WRAP)
    }

    override suspend fun getOpenedLaunchLink(): String {
        return prefsController.getString("OpenedLaunchLink", "")
    }

    override suspend fun setOpenedLaunchLink(value: String) {
        prefsController.setString("OpenedLaunchLink", value)
    }

    override suspend fun clearAllExceptSessionIdAndDbKey() {
        prefsController.clearExcept(keys = setOf(SESSION_ID, DB_KEY))
    }
}