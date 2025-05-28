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

package eu.europa.ec.businesslogic.controller.storage

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.core.content.edit
import eu.europa.ec.businesslogic.extension.shuffle
import eu.europa.ec.businesslogic.extension.unShuffle
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
     * @param key   Key used to add given [value].
     * @param value Value to add after given [key].
     */
    fun setString(key: String, value: String)

    /**
     * Assigns given [value] to device storage - shared preferences given [key]. You can
     * retrieve this value by calling [getString].
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
     * @param key   Key used to add given [value].
     * @param value Value to add after given [key].
     */
    fun setBool(key: String, value: Boolean)

    /**
     * Retrieves a string value from device shared preferences that corresponds to given [key]. If
     * key does not exist or value of given key is null, [defaultValue] is returned.
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
     * @param key          Key to get corresponding value.
     * @param defaultValue Default value to return if given [key] does not exist in prefs or if
     * key value is invalid.
     */
    fun getLong(key: String, defaultValue: Long): Long

    /**
     * Retrieves a boolean value from the device's shared preferences associated with the given [key].
     *
     * If the [key] is not found in the preferences, or if the value associated with the [key] is null,
     * the [defaultValue] is returned.  Note that if a value exists for the key but is not a valid
     * boolean (e.g., a String or an Int), the platform may also return the [defaultValue], depending on
     * the underlying shared preferences implementation.
     *
     * @param key The key used to retrieve the boolean value.
     * @param defaultValue The boolean value to return if the [key] is not found or has a null value.
     * @return The boolean value associated with the [key], or the [defaultValue] if the [key] is not found or has a null value.
     */
    fun getBool(key: String, defaultValue: Boolean): Boolean

    /**
     * Sets an integer value associated with the given key in the underlying data store.
     * If a value already exists for the key, it will be overwritten.
     *
     * @param key The unique identifier for the integer value.  Must not be null or empty.
     * @param value The integer value to store.
     */
    fun setInt(key: String, value: Int)

    /**
     * Retrieves an integer value associated with the given key from a data source.
     * If the key is not found or the value is not an integer, it returns the specified default value.
     *
     * @param key The key associated with the integer value to retrieve.
     * @param defaultValue The default integer value to return if the key is not found or the value is not an integer.
     * @return The integer value associated with the key, or the default value if the key is not found or the value is invalid.
     */
    fun getInt(key: String, defaultValue: Int): Int
}

/**
 * Implementation of the [PrefsController] interface for managing application preferences.
 *
 * This class provides methods for storing and retrieving various data types (String, Long, Boolean, Int)
 * in the application's SharedPreferences.  All SharedPreferences are
 * stored within a file named "eudi-wallet" accessible only to this application.
 *
 * @property resourceProvider An instance of [ResourceProvider] used to access application resources,
 *                           including the application context for obtaining SharedPreferences.
 */
class PrefsControllerImpl(
    private val resourceProvider: ResourceProvider
) : PrefsController {


    /**
     * Retrieves the SharedPreferences instance for the application.
     *
     * This function provides access to the SharedPreferences object used by the application
     * for persistent storage of key-value pairs. The SharedPreferences are named "eudi-wallet"
     * and are accessed with private mode, meaning only this application can read or write to them.
     *
     * @return The SharedPreferences instance.
     */
    private fun getSharedPrefs(): SharedPreferences {
        return resourceProvider.provideContext().getSharedPreferences("eudi-wallet", MODE_PRIVATE)
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
        getSharedPrefs().edit { remove(key) }
    }

    /**
     * Removes all keys from shared preferences. Notice that this operation is
     * irreversible and may lead to data loss.
     */
    override fun clear() {
        getSharedPrefs().edit { clear() }
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
        getSharedPrefs().edit {
            putString(key, value.shuffle())
        }
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
        getSharedPrefs().edit {
            putLong(key, value)
        }
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
        getSharedPrefs().edit {
            putBoolean(key, value)
        }
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
        return getSharedPrefs().getString(key, null)?.unShuffle() ?: defaultValue
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

    /**
     * Retrieves an integer value from SharedPreferences associated with the given key.
     * If no value is found for the key, returns the provided default value.
     *
     * @param key The key associated with the integer value to retrieve.
     * @param defaultValue The default integer value to return if no value is found for the key.
     * @return The integer value associated with the key, or the default value if no value is found.
     */
    override fun getInt(key: String, defaultValue: Int): Int {
        return getSharedPrefs().getInt(key, defaultValue)
    }

    /**
     * Sets an integer value in the shared preferences.
     *
     * @param key The key under which the value should be stored.
     * @param value The integer value to be stored.
     */
    override fun setInt(key: String, value: Int) {
        getSharedPrefs().edit {
            putInt(key, value)
        }
    }
}

interface PrefKeys {
    fun getBiometricAlias(): String
    fun setBiometricAlias(value: String)
}

class PrefKeysImpl(
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
}