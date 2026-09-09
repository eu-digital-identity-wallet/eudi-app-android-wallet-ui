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

package eu.europa.ec.authenticationlogic.controller.authentication

import android.os.Bundle
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC
import androidx.fragment.app.FragmentActivity
import eu.europa.ec.authenticationlogic.controller.storage.BiometryStorageController
import eu.europa.ec.authenticationlogic.model.BiometricAuthentication
import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.businesslogic.controller.crypto.CryptoController
import eu.europa.ec.businesslogic.extension.encodeToPemBase64String
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedConstruction
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
class TestBiometricAuthenticationFlow {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var crypto: CryptoController

    @Mock
    private lateinit var storage: BiometryStorageController

    @Mock
    private lateinit var resources: ResourceProvider

    @Mock
    private lateinit var manager: BiometricManager

    private val callbacks = mutableListOf<BiometricPrompt.AuthenticationCallback>()
    private val results = mutableListOf<BiometricsAuthenticate>()

    private lateinit var host: ActivityController<FragmentActivity>
    private lateinit var activity: FragmentActivity
    private lateinit var prompts: MockedConstruction<BiometricPrompt>
    private lateinit var controller: BiometricAuthenticationController
    private lateinit var cipher: Cipher

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        host = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        activity = host.get()

        whenever(resources.provideContext()).thenReturn(activity)
        whenever(resources.getString(any())).thenAnswer {
            activity.getString(it.getArgument(0))
        }

        cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(ByteArray(32) { 1 }, "AES"))
        }
        whenever { crypto.getCipher(any(), any(), any()) }.thenReturn(cipher)
        whenever { crypto.generateCodeVerifier() }.thenReturn("challenge")
        whenever { crypto.encryptDecrypt(any(), any()) }.thenAnswer {
            it.getArgument<Cipher>(0).doFinal(it.getArgument<ByteArray>(1))
        }

        controller = BiometricAuthenticationControllerImpl(
            resourceProvider = resources,
            cryptoController = crypto,
            biometryStorageController = storage,
            dispatcher = coroutineRule.testDispatcher
        )
        prompts = mockBiometricPrompts()
    }

    @After
    fun after() {
        try {
            if (::host.isInitialized && !host.get().isDestroyed) {
                host.pause().stop().destroy()
            }
            coroutineRule.testScope.runCurrent()
        } finally {
            if (::prompts.isInitialized && !prompts.isClosed) {
                prompts.close()
            }
            closeable.close()
        }
    }

    @Test
    fun `terminal errors after a rejected scan are delivered once without relaunching the prompt`() =
        coroutineRule.runTest {
            val errors = listOf(
                BiometricPrompt.ERROR_NO_BIOMETRICS,
                BiometricPrompt.ERROR_HW_UNAVAILABLE,
                BiometricPrompt.ERROR_LOCKOUT,
                BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
                BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                BiometricPrompt.ERROR_TIMEOUT,
                BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED,
            )

            errors.forEachIndexed { index, code ->
                startAuthentication(notifyOnFailure = false)
                callbacks.last().onAuthenticationFailed()
                coroutineRule.testScope.runCurrent()

                assertEquals(index, results.size)

                callbacks.last().onAuthenticationError(code, "system error $code")
                coroutineRule.testScope.runCurrent()

                assertEquals(index + 1, prompts.constructed().size)
                assertEquals(index + 1, results.size)
                assertEquals(BiometricsAuthenticate.Failed("system error $code"), results.last())
            }
        }

    @Test
    fun `user negative-button and system cancellations do not become failures`() =
        coroutineRule.runTest {
            val cancellationCodes = listOf(
                BiometricPrompt.ERROR_USER_CANCELED,
                BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                BiometricPrompt.ERROR_CANCELED
            )

            cancellationCodes.forEach { code ->
                startAuthentication()
                callbacks.last().onAuthenticationError(code, "cancelled")
                coroutineRule.testScope.runCurrent()

                assertEquals(BiometricsAuthenticate.Cancelled, results.last())
            }

            assertEquals(3, prompts.constructed().size)
            assertEquals(3, results.size)
        }

    @Test
    fun `cipher setup failure reports failure without opening a prompt`() = coroutineRule.runTest {
        whenever(crypto.getCipher(any(), any(), any())).thenReturn(null)
        val expectedFailure = BiometricsAuthenticate.Failed(
            activity.getString(R.string.biometric_authentication_error)
        )

        startAuthentication()

        assertEquals(listOf(expectedFailure), results)
        assertTrue(prompts.constructed().isEmpty())
    }

    @Test
    fun `storage exception reports failure without leaving a pending attempt`() =
        coroutineRule.runTest {
            whenever(storage.getBiometricAuthentication())
                .thenThrow(IllegalStateException("unavailable"))

            startAuthentication()

            assertTrue(results.single() is BiometricsAuthenticate.Failed)
            assertTrue(prompts.constructed().isEmpty())
        }

    @Test
    fun `prompt startup exception reports failure`() = coroutineRule.runTest {
        prompts.close()
        prompts = mockConstruction(BiometricPrompt::class.java) { prompt, _ ->
            doThrow(IllegalArgumentException("unsupported")).whenever(prompt)
                .authenticate(any(), any<BiometricPrompt.CryptoObject>())
        }

        startAuthentication()

        assertTrue(results.single() is BiometricsAuthenticate.Failed)
        assertEquals(1, prompts.constructed().size)
    }

    @Test
    fun `rejected scan cancels prompt before delivering failure and ignores late callbacks`() =
        coroutineRule.runTest {
            startAuthentication()
            val prompt = prompts.constructed().single()

            // Simulate even a synchronous cancellation callback from an OEM implementation.
            doAnswer {
                callbacks.single()
                    .onAuthenticationError(BiometricPrompt.ERROR_CANCELED, "cancelled")
            }.whenever(prompt).cancelAuthentication()

            callbacks.single().onAuthenticationFailed()
            callbacks.single().onAuthenticationSucceeded(successfulAuthenticationResult())
            coroutineRule.testScope.runCurrent()

            verify(prompt).cancelAuthentication()
            assertTrue(results.single() is BiometricsAuthenticate.Failed)
            verify(storage, never()).setBiometricAuthentication(any())
        }

    @Test
    fun `rejected scan stays in same prompt until the user cancels`() = coroutineRule.runTest {
        startAuthentication(notifyOnFailure = false)
        callbacks.single().onAuthenticationFailed()
        coroutineRule.testScope.runCurrent()

        assertTrue(results.isEmpty())
        verify(prompts.constructed().single(), never()).cancelAuthentication()

        callbacks.single().onAuthenticationError(BiometricPrompt.ERROR_USER_CANCELED, "cancelled")
        coroutineRule.testScope.runCurrent()

        assertEquals(listOf(BiometricsAuthenticate.Cancelled), results)
    }

    @Test
    fun `repeated rejected scans allow success in the same prompt without reporting failure`() =
        coroutineRule.runTest {
            startAuthentication(notifyOnFailure = false)

            repeat(3) {
                callbacks.single().onAuthenticationFailed()
                coroutineRule.testScope.runCurrent()

                assertTrue(results.isEmpty())
                assertEquals(1, prompts.constructed().size)
                verify(storage, never()).setBiometricAuthentication(any())
            }

            callbacks.single().onAuthenticationSucceeded(successfulAuthenticationResult())
            coroutineRule.testScope.runCurrent()

            assertEquals(listOf(BiometricsAuthenticate.Success), results)
            verify(storage).setBiometricAuthentication(any())
            verify(prompts.constructed().single(), never()).cancelAuthentication()
        }

    @Test
    fun `first successful authentication stores crypto and requests strong biometrics`() =
        coroutineRule.runTest {
            startAuthentication()

            val info = argumentCaptor<BiometricPrompt.PromptInfo>()
            verify(prompts.constructed().single()).authenticate(
                info.capture(),
                any<BiometricPrompt.CryptoObject>()
            )
            assertEquals(BIOMETRIC_STRONG, info.firstValue.allowedAuthenticators)

            callbacks.single().onAuthenticationSucceeded(successfulAuthenticationResult())
            coroutineRule.testScope.runCurrent()

            assertEquals(listOf(BiometricsAuthenticate.Success), results)

            val saved = argumentCaptor<BiometricAuthentication>()
            verify(storage).setBiometricAuthentication(saved.capture())
            assertEquals("challenge", saved.firstValue.randomString)
            assertTrue(saved.firstValue.encryptedString.isNotBlank())
            assertEquals(cipher.iv.encodeToPemBase64String(), saved.firstValue.ivString)
        }

    @Test
    fun `existing crypto must decrypt to the stored challenge before success`() =
        coroutineRule.runTest {
            val stored = BiometricAuthentication(
                randomString = "challenge",
                encryptedString = byteArrayOf(1, 2, 3).encodeToPemBase64String().orEmpty(),
                ivString = cipher.iv.encodeToPemBase64String().orEmpty()
            )
            whenever(storage.getBiometricAuthentication()).thenReturn(stored)
            whenever(crypto.encryptDecrypt(any(), any())).thenReturn("challenge".toByteArray())

            startAuthentication()
            callbacks.last().onAuthenticationSucceeded(successfulAuthenticationResult())
            coroutineRule.testScope.runCurrent()

            assertEquals(BiometricsAuthenticate.Success, results.single())

            whenever(
                crypto.encryptDecrypt(
                    any(),
                    any()
                )
            ).thenReturn("wrong challenge".toByteArray())

            startAuthentication()
            callbacks.last().onAuthenticationSucceeded(successfulAuthenticationResult())
            coroutineRule.testScope.runCurrent()

            assertTrue(results.last() is BiometricsAuthenticate.Failed)
            verify(storage, never()).setBiometricAuthentication(any())
        }

    @Test
    fun `crypto verification exception cannot produce success or crash`() = coroutineRule.runTest {
        whenever(crypto.encryptDecrypt(any(), any()))
            .thenThrow(IllegalStateException("invalidated key"))

        startAuthentication()
        callbacks.single().onAuthenticationSucceeded(successfulAuthenticationResult())
        coroutineRule.testScope.runCurrent()

        assertTrue(results.single() is BiometricsAuthenticate.Failed)
        verify(storage, never()).setBiometricAuthentication(any())
    }

    @Test
    fun `destroyed activity cancels prompt and notifies caller once`() = coroutineRule.runTest {
        startAuthentication()

        host.pause().stop().destroy()
        coroutineRule.testScope.runCurrent()

        verify(prompts.constructed().single()).cancelAuthentication()
        assertEquals(listOf(BiometricsAuthenticate.Cancelled), results)

        callbacks.single().onAuthenticationError(BiometricPrompt.ERROR_CANCELED, "cancelled")
        coroutineRule.testScope.runCurrent()

        assertEquals(1, results.size)

        // Use a fresh activity so teardown can run normally.
        host = Robolectric.buildActivity(FragmentActivity::class.java).setup()
    }

    @Test
    fun `already destroyed activity still notifies caller`() = coroutineRule.runTest {
        host.pause().stop().destroy()

        startAuthentication()

        assertEquals(listOf(BiometricsAuthenticate.Cancelled), results)
        assertTrue(prompts.constructed().isEmpty())

        host = Robolectric.buildActivity(FragmentActivity::class.java).setup()
    }

    @Test
    fun `backgrounding during crypto preparation cancels the attempt and permits a foreground retry`() =
        coroutineRule.runTest {
            val preparedCipher = CompletableDeferred<Cipher>()
            whenever(crypto.getCipher(any(), any(), any())).doSuspendableAnswer {
                preparedCipher.await()
            }

            startAuthentication()

            assertTrue(prompts.constructed().isEmpty())

            host.pause().saveInstanceState(Bundle()).stop()
            assertTrue(activity.supportFragmentManager.isStateSaved)

            // Use real AndroidX here: it returns without a callback if launched after state saving.
            prompts.close()
            preparedCipher.complete(cipher)
            coroutineRule.testScope.runCurrent()

            assertEquals(listOf(BiometricsAuthenticate.Cancelled), results)

            host.restart().start().resume()
            assertFalse(activity.supportFragmentManager.isStateSaved)
            prompts = mockBiometricPrompts()

            startAuthentication()

            assertEquals(1, prompts.constructed().size)

            callbacks.single()
                .onAuthenticationError(BiometricPrompt.ERROR_USER_CANCELED, "cancelled")
            coroutineRule.testScope.runCurrent()

            assertEquals(
                listOf(BiometricsAuthenticate.Cancelled, BiometricsAuthenticate.Cancelled),
                results
            )
        }

    @Test
    fun `non activity context reports failure immediately`() {
        controller.authenticate(activity.applicationContext, true) { results += it }

        assertTrue(results.single() is BiometricsAuthenticate.Failed)
        assertTrue(prompts.constructed().isEmpty())
    }

    @Test
    fun `cancelling suspend caller dismisses its prompt`() = coroutineRule.runTest {
        val request = async {
            controller.authenticate(
                activity,
                BiometricCrypto(null),
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Test")
                    .setNegativeButtonText("Cancel")
                    .build(),
                false
            )
        }
        coroutineRule.testScope.runCurrent()

        request.cancel()
        coroutineRule.testScope.runCurrent()

        verify(prompts.constructed().single()).cancelAuthentication()
    }

    @Test
    fun `availability enforces the requested biometric strength`() {
        whenever(manager.canAuthenticate(BIOMETRIC_STRONG))
            .thenReturn(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)
        whenever(manager.canAuthenticate(BIOMETRIC_WEAK))
            .thenReturn(BiometricManager.BIOMETRIC_SUCCESS)

        mockStatic(BiometricManager::class.java).use { factory ->
            factory.`when`<BiometricManager> { BiometricManager.from(activity) }.thenReturn(manager)

            assertEquals(
                BiometricsAvailability.Failure(activity.getString(R.string.biometric_strong_required)),
                controller.getBiometricsAvailability(BIOMETRIC_STRONG)
            )
            assertEquals(
                BiometricsAvailability.CanAuthenticate,
                controller.getBiometricsAvailability(BIOMETRIC_WEAK)
            )
            verify(manager).canAuthenticate(BIOMETRIC_STRONG)
        }
    }

    @Test
    fun `crypto enrollment requests strong biometrics`() {
        controller.launchBiometricSystemScreen(BIOMETRIC_STRONG)

        val intent = shadowOf(activity).nextStartedActivity
        assertEquals(Settings.ACTION_BIOMETRIC_ENROLL, intent.action)
        assertEquals(
            BIOMETRIC_STRONG,
            intent.getIntExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, 0)
        )
    }

    private fun startAuthentication(notifyOnFailure: Boolean = true) {
        controller.authenticate(activity, notifyOnFailure) { results += it }
        coroutineRule.testScope.runCurrent()
    }

    private fun successfulAuthenticationResult() = BiometricPrompt.AuthenticationResult(
        BiometricPrompt.CryptoObject(cipher),
        AUTHENTICATION_RESULT_TYPE_BIOMETRIC
    )

    private fun mockBiometricPrompts(): MockedConstruction<BiometricPrompt> =
        mockConstruction(BiometricPrompt::class.java) { _, context ->
            callbacks += context.arguments()[2] as BiometricPrompt.AuthenticationCallback
        }
}