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
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.crypto.tink.Aead
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.LegacyKeysetSerialization
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import eu.europa.ec.resourceslogic.provider.ResourceProviderImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class TestPrefsController {

    private val context: Context = RuntimeEnvironment.getApplication()

    private val keysetPrefs
        get() = context.getSharedPreferences("eudi-wallet-datastore-keyset", Context.MODE_PRIVATE)

    @Before
    fun before() {
        AeadConfig.register()
    }

    @Test
    fun `Given a keyset stored in cleartext, When it is checked, Then it is reported as cleartext`() {
        val bytes = ByteArrayOutputStream()
        LegacyKeysetSerialization.serializeKeyset(
            newKeyset(),
            BinaryKeysetWriter.withOutputStream(bytes),
            InsecureSecretKeyAccess.get()
        )

        assertTrue(isCleartextKeyset(BinaryKeysetReader.withBytes(bytes.toByteArray())))
    }

    @Test
    fun `Given an encrypted keyset, When it is checked, Then it is not reported as cleartext`() {
        val bytes = ByteArrayOutputStream()
        val wrappingKey = newKeyset().getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        LegacyKeysetSerialization.serializeEncryptedKeyset(
            newKeyset(),
            BinaryKeysetWriter.withOutputStream(bytes),
            wrappingKey,
            ByteArray(0)
        )

        assertFalse(isCleartextKeyset(BinaryKeysetReader.withBytes(bytes.toByteArray())))
    }

    @Test
    fun `Given no Android Keystore, When the preferences are opened, Then they are refused and no cleartext keyset is left`() {
        val prefs = PrefsControllerImpl(ResourceProviderImpl(context))

        assertThrows(KeystoreUnavailableException::class.java) {
            runBlocking { prefs.getBool("any", false) }
        }
        assertFalse(keysetPrefs.contains("prefs_datastore_keyset"))
        assertFalse(context.preferencesDataStoreFile("eudi-wallet.preferences_pb").exists())
    }

    private fun newKeyset(): KeysetHandle = KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM"))
}