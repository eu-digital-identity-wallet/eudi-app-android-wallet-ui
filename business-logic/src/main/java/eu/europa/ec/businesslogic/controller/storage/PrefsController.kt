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

package eu.europa.ec.businesslogic.controller.storage

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import eu.europa.ec.businesslogic.model.BiometricData
import eu.europa.ec.resourceslogic.provider.ResourceProvider

interface PrefsController {

    /**
     * Defines if [SharedPreferences] contains a value for given [key]. This function will only
     * identify if a key exists in storage and will not check if corresponding value is valid.
     *
     * @param key The name of the preference to check.
     *
     * @return `true` if preferences contain given key. `false` otherwise.
     */
    fun contains(key: String): Boolean

    /**
     * Removes given preference key from shared preferences. Notice that this operation is
     * irreversible and may lead to data loss.
     */
    fun clear(key: String)

    /**
     * Removes all keys from shared preferences. Notice that this operation is
     * irreversible and may lead to data loss.
     */
    fun clear()

    /**
     * Assigns given [value] to device storage - shared preferences given [key]. You can
     * retrieve this value by calling [getString].
     *
     * Shared preferences are encrypted. Do not create your own instance to add or retrieve data.
     * Instead, call operations of this controller.
     *
     * @param key   Key used to add given [value].
     * @param value Value to add after given [key].
     */
    fun setString(key: String, value: String)

    /**
     * Assigns given [value] to device storage - shared preferences given [key]. You can
     * retrieve this value by calling [getString].
     *
     * Shared preferences are encrypted. Do not create your own instance to add or retrieve data.
     * Instead, call operations of this controller.
     *
     * @param key   Key used to add given [value].
     * @param value Value to add after given [key].
     */
    fun setLong(
        key: String, value: Long
    )

    /**
     * Assigns given [value] to device storage - shared preferences given [key]. You can
     * retrieve this value by calling [getString].
     *
     * Shared preferences are encrypted. Do not create your own instance to add or retrieve data.
     * Instead, call operations of this controller.
     *
     * @param key   Key used to add given [value].
     * @param value Value to add after given [key].
     */
    fun setBool(key: String, value: Boolean)

    /**
     * Retrieves a string value from device shared preferences that corresponds to given [key]. If
     * key does not exist or value of given key is null, [defaultValue] is returned.
     *
     * Shared preferences are encrypted. Do not create your own instance to add or retrieve data.
     * Instead, call operations of this controller.
     *
     * @param key          Key to get corresponding value.
     * @param defaultValue Default value to return if given [key] does not exist in prefs or if
     * key value is invalid.
     */
    fun getString(key: String, defaultValue: String): String

    /**
     * Retrieves a long value from device shared preferences that corresponds to given [key]. If
     * key does not exist or value of given key is null, [defaultValue] is returned.
     *
     * Shared preferences are encrypted. Do not create your own instance to add or retrieve data.
     * Instead, call operations of this controller.
     *
     * @param key          Key to get corresponding value.
     * @param defaultValue Default value to return if given [key] does not exist in prefs or if
     * key value is invalid.
     */
    fun getLong(key: String, defaultValue: Long): Long

    /**
     * Retrieves a boolean value from device shared preferences that corresponds to given [key]. If
     * key does not exist or value of given key is null, [defaultValue] is returned.
     *
     * Shared preferences are encrypted. Do not create your own instance to add or retrieve data.
     * Instead, call operations of this controller.
     *
     * @param key          Key to get corresponding value.
     * @param defaultValue Default value to return if given [key] does not exist in prefs or if
     * key value is invalid.
     */
    fun getBool(key: String, defaultValue: Boolean): Boolean
    fun setInt(key: String, value: Int)
    fun getInt(key: String, defaultValue: Int): Int

}

/**
 * Controller used to manipulate data stored in device [SharedPreferences]. Data are encrypted so
 * you are strongly advised to used this controller to set or get values.
 */
class PrefsControllerImpl constructor(
    private val resourceProvider: ResourceProvider
) : PrefsController {

    /**
     * Master key used to encrypt/decrypt shared preferences.
     */
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(resourceProvider.provideContext())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    /**
     * Pref key scheme used to initialize [EncryptedSharedPreferences] instance.
     */
    private val prefKeyEncryptionScheme by lazy {
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV
    }

    /**
     * Pref value scheme used to initialize [EncryptedSharedPreferences] instance.
     */
    private val prefValueEncryptionScheme by lazy {
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    }

    /**
     * Initializes and returns a new [SharedPreferences] instance. Instance is using an encryption
     * to store data in device.
     *
     * @return A new [SharedPreferences] instance.
     */
    private fun getSharedPrefs(): SharedPreferences {
        return EncryptedSharedPreferences.create(
            resourceProvider.provideContext(),
            "secret_shared_prefs",
            masterKey,
            prefKeyEncryptionScheme,
            prefValueEncryptionScheme
        )
    }

    /**
     * Defines if [SharedPreferences] contains a value for given [key]. This function will only
     * identify if a key exists in storage and will not check if corresponding value is valid.
     *
     * @param key The name of the preference to check.
     *
     * @return `true` if preferences contain given key. `false` otherwise.
     */
    override fun contains(key: String): Boolean {
        return getSharedPrefs().contains(key)
    }

    /**
     * Removes given preference key from shared preferences. Notice that this operation is
     * irreversible and may lead to data loss.
     */
    override fun clear(key: String) {
        getSharedPrefs().edit().remove(key).apply()
    }

    /**
     * Removes all keys from shared preferences. Notice that this operation is
     * irreversible and may lead to data loss.
     */
    override fun clear() {
        getSharedPrefs().edit().clear().apply()
    }

    /**
     * Assigns given [value] to device storage - shared preferences given [key]. You can
     * retrieve this value by calling [getString].
     *
     * Shared preferences are encrypted. Do not create your own instance to add or retrieve data.
     * Instead, call operations of this controller.
     *
     * @param key   Key used to add given [value].
     * @param value Value to add after given [key].
     */
    override fun setString(key: String, value: String) {
        getSharedPrefs().edit()
            .putString(key, value)
            .apply()
    }

    /**
     * Assigns given [value] to device storage - shared preferences given [key]. You can
     * retrieve this value by calling [getString].
     *
     * Shared preferences are encrypted. Do not create your own instance to add or retrieve data.
     * Instead, call operations of this controller.
     *
     * @param key   Key used to add given [value].
     * @param value Value to add after given [key].
     */
    override fun setLong(
        key: String, value: Long
    ) {
        getSharedPrefs().edit()
            .putLong(key, value)
            .apply()
    }

    /**
     * Assigns given [value] to device storage - shared preferences given [key]. You can
     * retrieve this value by calling [getString].
     *
     * Shared preferences are encrypted. Do not create your own instance to add or retrieve data.
     * Instead, call operations of this controller.
     *
     * @param key   Key used to add given [value].
     * @param value Value to add after given [key].
     */
    override fun setBool(key: String, value: Boolean) {
        getSharedPrefs().edit()
            .putBoolean(key, value)
            .apply()
    }

    /**
     * Retrieves a string value from device shared preferences that corresponds to given [key]. If
     * key does not exist or value of given key is null, [defaultValue] is returned.
     *
     * Shared preferences are encrypted. Do not create your own instance to add or retrieve data.
     * Instead, call operations of this controller.
     *
     * @param key          Key to get corresponding value.
     * @param defaultValue Default value to return if given [key] does not exist in prefs or if
     * key value is invalid.
     */
    override fun getString(key: String, defaultValue: String): String {
        return getSharedPrefs().getString(key, defaultValue) ?: defaultValue
    }

    /**
     * Retrieves a long value from device shared preferences that corresponds to given [key]. If
     * key does not exist or value of given key is null, [defaultValue] is returned.
     *
     * Shared preferences are encrypted. Do not create your own instance to add or retrieve data.
     * Instead, call operations of this controller.
     *
     * @param key          Key to get corresponding value.
     * @param defaultValue Default value to return if given [key] does not exist in prefs or if
     * key value is invalid.
     */
    override fun getLong(key: String, defaultValue: Long): Long {
        return getSharedPrefs().getLong(key, defaultValue)
    }

    /**
     * Retrieves a boolean value from device shared preferences that corresponds to given [key]. If
     * key does not exist or value of given key is null, [defaultValue] is returned.
     *
     * Shared preferences are encrypted. Do not create your own instance to add or retrieve data.
     * Instead, call operations of this controller.
     *
     * @param key          Key to get corresponding value.
     * @param defaultValue Default value to return if given [key] does not exist in prefs or if
     * key value is invalid.
     */
    override fun getBool(key: String, defaultValue: Boolean): Boolean {
        return getSharedPrefs().getBoolean(key, defaultValue)
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return getSharedPrefs().getInt(key, defaultValue)
    }

    override fun setInt(key: String, value: Int) {
        getSharedPrefs().edit()
            .putInt(key, value)
            .apply()
    }
}

interface PrefKeys {
    fun getBiometricAlias(): String
    fun setBiometricAlias(value: String)
    fun getBiometricData(): BiometricData?
    fun setBiometricData(value: BiometricData?)
}

class PrefKeysImpl constructor(
    private val prefsController: PrefsController
) : PrefKeys {

    /**
     * Returns the biometric alias in order to find the biometric secret key in android keystore.
     */
    override fun getBiometricAlias(): String {
        return prefsController.getString("BiometricAlias", "")
    }

    /**
     * Stores the biometric alias used for the secret key in android keystore.
     *
     * @param value the biometric alias value.
     */
    override fun setBiometricAlias(value: String) {
        prefsController.setString("BiometricAlias", value)
    }

    /**
     * Returns the biometric data in order to validate that biometric is not tampered in any way.
     */
    override fun getBiometricData(): BiometricData? {
        return try {
            Gson().fromJson(
                prefsController.getString("BiometricData", ""),
                BiometricData::class.java
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Stores the biometric data used to validate that biometric is not tampered in any way.
     *
     * @param value the biometric data.
     */
    override fun setBiometricData(value: BiometricData?) {
        if (value == null) prefsController.clear("BiometricData")
        prefsController.setString("BiometricData", Gson().toJson(value))
    }
}