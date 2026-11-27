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
    fun `Given a rejected scan, When a terminal error arrives, Then it is delivered once without relaunching the prompt`() =
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
                // Given
                startAuthentication(notifyOnFailure = false)
                callbacks.last().onAuthenticationFailed()
                coroutineRule.testScope.runCurrent()

                assertEquals(index, results.size)

                // When
                callbacks.last().onAuthenticationError(code, "system error $code")
                coroutineRule.testScope.runCurrent()

                // Then
                assertEquals(index + 1, prompts.constructed().size)
                assertEquals(index + 1, results.size)
                assertEquals(BiometricsAuthenticate.Failed("system error $code"), results.last())
            }
        }

    @Test
    fun `Given a started prompt, When the user or the system cancels it, Then no failure is reported`() =
        coroutineRule.runTest {
            val cancellationCodes = listOf(
                BiometricPrompt.ERROR_USER_CANCELED,
                BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                BiometricPrompt.ERROR_CANCELED
            )

            cancellationCodes.forEach { code ->
                // Given
                startAuthentication()

                // When
                callbacks.last().onAuthenticationError(code, "cancelled")
                coroutineRule.testScope.runCurrent()

                // Then
                assertEquals(BiometricsAuthenticate.Cancelled, results.last())
            }

            assertEquals(3, prompts.constructed().size)
            assertEquals(3, results.size)
        }

    @Test
    fun `Given a cipher that cannot be set up, When authentication starts, Then a failure is reported without opening a prompt`() =
        coroutineRule.runTest {
            // Given
            whenever(crypto.getCipher(any(), any(), any())).thenReturn(null)
            val expectedFailure = BiometricsAuthenticate.Failed(
                activity.getString(R.string.biometric_authentication_error)
            )

            // When
            startAuthentication()

            // Then
            assertEquals(listOf(expectedFailure), results)
            assertTrue(prompts.constructed().isEmpty())
        }

    @Test
    fun `Given a storage that throws, When authentication starts, Then a failure is reported and no attempt is left pending`() =
        coroutineRule.runTest {
            // Given
            whenever(storage.getBiometricAuthentication())
                .thenThrow(IllegalStateException("unavailable"))

            // When
            startAuthentication()

            // Then
            assertTrue(results.single() is BiometricsAuthenticate.Failed)
            assertTrue(prompts.constructed().isEmpty())
        }

    @Test
    fun `Given a prompt that fails on startup, When authentication starts, Then a failure is reported`() =
        coroutineRule.runTest {
            // Given
            prompts.close()
            prompts = mockConstruction(BiometricPrompt::class.java) { prompt, _ ->
                doThrow(IllegalArgumentException("unsupported")).whenever(prompt)
                    .authenticate(any(), any<BiometricPrompt.CryptoObject>())
            }

            // When
            startAuthentication()

            // Then
            assertTrue(results.single() is BiometricsAuthenticate.Failed)
            assertEquals(1, prompts.constructed().size)
        }

    /**
     * The test simulates even a synchronous cancellation callback from an OEM implementation.
     */
    @Test
    fun `Given a started authentication, When a scan is rejected, Then the prompt is cancelled before the failure is delivered and late callbacks are ignored`() =
        coroutineRule.runTest {
            // Given
            startAuthentication()
            val prompt = prompts.constructed().single()

            doAnswer {
                callbacks.single()
                    .onAuthenticationError(BiometricPrompt.ERROR_CANCELED, "cancelled")
            }.whenever(prompt).cancelAuthentication()

            // When
            callbacks.single().onAuthenticationFailed()
            callbacks.single().onAuthenticationSucceeded(successfulAuthenticationResult())
            coroutineRule.testScope.runCurrent()

            // Then
            verify(prompt).cancelAuthentication()
            assertTrue(results.single() is BiometricsAuthenticate.Failed)
            verify(storage, never()).setBiometricAuthentication(any())
        }

    @Test
    fun `Given a started authentication, When a scan is rejected and then the user cancels, Then the prompt stays open until the cancellation`() =
        coroutineRule.runTest {
            // Given
            startAuthentication(notifyOnFailure = false)

            // When
            callbacks.single().onAuthenticationFailed()
            coroutineRule.testScope.runCurrent()

            // Then
            assertTrue(results.isEmpty())
            verify(prompts.constructed().single(), never()).cancelAuthentication()

            // When
            callbacks.single()
                .onAuthenticationError(BiometricPrompt.ERROR_USER_CANCELED, "cancelled")
            coroutineRule.testScope.runCurrent()

            // Then
            assertEquals(listOf(BiometricsAuthenticate.Cancelled), results)
        }

    @Test
    fun `Given a started authentication, When scans are rejected repeatedly and then one succeeds, Then success is reported in the same prompt without a failure`() =
        coroutineRule.runTest {
            // Given
            startAuthentication(notifyOnFailure = false)

            // When
            repeat(3) {
                callbacks.single().onAuthenticationFailed()
                coroutineRule.testScope.runCurrent()

                // Then
                assertTrue(results.isEmpty())
                assertEquals(1, prompts.constructed().size)
                verify(storage, never()).setBiometricAuthentication(any())
            }

            // When
            callbacks.single().onAuthenticationSucceeded(successfulAuthenticationResult())
            coroutineRule.testScope.runCurrent()

            // Then
            assertEquals(listOf(BiometricsAuthenticate.Success), results)
            verify(storage).setBiometricAuthentication(any())
            verify(prompts.constructed().single(), never()).cancelAuthentication()
        }

    @Test
    fun `Given no stored crypto, When authentication starts and succeeds, Then strong biometrics are requested and the crypto is stored`() =
        coroutineRule.runTest {
            // When
            startAuthentication()

            // Then
            val info = argumentCaptor<BiometricPrompt.PromptInfo>()
            verify(prompts.constructed().single()).authenticate(
                info.capture(),
                any<BiometricPrompt.CryptoObject>()
            )
            assertEquals(BIOMETRIC_STRONG, info.firstValue.allowedAuthenticators)

            // When
            callbacks.single().onAuthenticationSucceeded(successfulAuthenticationResult())
            coroutineRule.testScope.runCurrent()

            // Then
            assertEquals(listOf(BiometricsAuthenticate.Success), results)

            val saved = argumentCaptor<BiometricAuthentication>()
            verify(storage).setBiometricAuthentication(saved.capture())
            assertEquals("challenge", saved.firstValue.randomString)
            assertTrue(saved.firstValue.encryptedString.isNotBlank())
            assertEquals(cipher.iv.encodeToPemBase64String(), saved.firstValue.ivString)
        }

    @Test
    fun `Given stored crypto, When authentication succeeds with a matching and then a wrong decryption, Then only the matching one succeeds`() =
        coroutineRule.runTest {
            // Given
            val stored = BiometricAuthentication(
                randomString = "challenge",
                encryptedString = byteArrayOf(1, 2, 3).encodeToPemBase64String().orEmpty(),
                ivString = cipher.iv.encodeToPemBase64String().orEmpty()
            )
            whenever(storage.getBiometricAuthentication()).thenReturn(stored)
            whenever(crypto.encryptDecrypt(any(), any())).thenReturn("challenge".toByteArray())

            // When
            startAuthentication()
            callbacks.last().onAuthenticationSucceeded(successfulAuthenticationResult())
            coroutineRule.testScope.runCurrent()

            // Then
            assertEquals(BiometricsAuthenticate.Success, results.single())

            // When
            whenever(
                crypto.encryptDecrypt(
                    any(),
                    any()
                )
            ).thenReturn("wrong challenge".toByteArray())

            startAuthentication()
            callbacks.last().onAuthenticationSucceeded(successfulAuthenticationResult())
            coroutineRule.testScope.runCurrent()

            // Then
            assertTrue(results.last() is BiometricsAuthenticate.Failed)
            verify(storage, never()).setBiometricAuthentication(any())
        }

    @Test
    fun `Given a key that is invalidated, When authentication succeeds, Then it fails instead of succeeding or crashing`() =
        coroutineRule.runTest {
            // Given
            whenever(crypto.encryptDecrypt(any(), any()))
                .thenThrow(IllegalStateException("invalidated key"))

            // When
            startAuthentication()
            callbacks.single().onAuthenticationSucceeded(successfulAuthenticationResult())
            coroutineRule.testScope.runCurrent()

            // Then
            assertTrue(results.single() is BiometricsAuthenticate.Failed)
            verify(storage, never()).setBiometricAuthentication(any())
        }

    /**
     * The test ends with a fresh activity so that teardown can run normally.
     */
    @Test
    fun `Given a started authentication, When the activity is destroyed, Then the prompt is cancelled and the caller is notified once`() =
        coroutineRule.runTest {
            // Given
            startAuthentication()

            // When
            host.pause().stop().destroy()
            coroutineRule.testScope.runCurrent()

            // Then
            verify(prompts.constructed().single()).cancelAuthentication()
            assertEquals(listOf(BiometricsAuthenticate.Cancelled), results)

            // When
            callbacks.single().onAuthenticationError(BiometricPrompt.ERROR_CANCELED, "cancelled")
            coroutineRule.testScope.runCurrent()

            // Then
            assertEquals(1, results.size)

            host = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        }

    @Test
    fun `Given a destroyed activity, When authentication starts, Then the caller is notified`() =
        coroutineRule.runTest {
            // Given
            host.pause().stop().destroy()

            // When
            startAuthentication()

            // Then
            assertEquals(listOf(BiometricsAuthenticate.Cancelled), results)
            assertTrue(prompts.constructed().isEmpty())

            host = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        }

    /**
     * Real AndroidX is used after the prompt mock is closed: it returns without a callback if
     * launched after state saving.
     */
    @Test
    fun `Given crypto still being prepared, When the app is backgrounded, Then the attempt is cancelled and a foreground retry works`() =

        coroutineRule.runTest {
            // Given
            val preparedCipher = CompletableDeferred<Cipher>()
            whenever(crypto.getCipher(any(), any(), any())).doSuspendableAnswer {
                preparedCipher.await()
            }

            // When
            startAuthentication()

            // Then
            assertTrue(prompts.constructed().isEmpty())

            // When
            host.pause().saveInstanceState(Bundle()).stop()

            // Then
            assertTrue(activity.supportFragmentManager.isStateSaved)

            // When
            prompts.close()
            preparedCipher.complete(cipher)
            coroutineRule.testScope.runCurrent()

            // Then
            assertEquals(listOf(BiometricsAuthenticate.Cancelled), results)

            // When
            host.restart().start().resume()

            // Then
            assertFalse(activity.supportFragmentManager.isStateSaved)

            // When
            prompts = mockBiometricPrompts()

            startAuthentication()

            // Then
            assertEquals(1, prompts.constructed().size)

            // When
            callbacks.single()
                .onAuthenticationError(BiometricPrompt.ERROR_USER_CANCELED, "cancelled")
            coroutineRule.testScope.runCurrent()

            // Then
            assertEquals(
                listOf(BiometricsAuthenticate.Cancelled, BiometricsAuthenticate.Cancelled),
                results
            )
        }

    @Test
    fun `Given a context that is not an activity, When authentication starts, Then a failure is reported immediately`() {
        // When
        controller.authenticate(activity.applicationContext, true) { results += it }

        // Then
        assertTrue(results.single() is BiometricsAuthenticate.Failed)
        assertTrue(prompts.constructed().isEmpty())
    }

    @Test
    fun `Given a pending suspend request, When the caller is cancelled, Then its prompt is dismissed`() =
        coroutineRule.runTest {
            // Given
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

            // When
            request.cancel()
            coroutineRule.testScope.runCurrent()

            // Then
            verify(prompts.constructed().single()).cancelAuthentication()
        }

    @Test
    fun `Given strong biometrics not enrolled and weak ones available, When availability is checked, Then the requested strength is enforced`() {
        // Given
        whenever(manager.canAuthenticate(BIOMETRIC_STRONG))
            .thenReturn(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)
        whenever(manager.canAuthenticate(BIOMETRIC_WEAK))
            .thenReturn(BiometricManager.BIOMETRIC_SUCCESS)

        mockStatic(BiometricManager::class.java).use { factory ->
            factory.`when`<BiometricManager> { BiometricManager.from(activity) }.thenReturn(manager)

            // When
            val strong = controller.getBiometricsAvailability(BIOMETRIC_STRONG)
            val weak = controller.getBiometricsAvailability(BIOMETRIC_WEAK)

            // Then
            assertEquals(
                BiometricsAvailability.Failure(activity.getString(R.string.biometric_strong_required)),
                strong
            )
            assertEquals(BiometricsAvailability.CanAuthenticate, weak)
            verify(manager).canAuthenticate(BIOMETRIC_STRONG)
        }
    }

    @Test
    fun `Given an activity, When the system enrollment screen is launched for crypto, Then strong biometrics are requested`() {
        // When
        controller.launchBiometricSystemScreen(BIOMETRIC_STRONG)

        // Then
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