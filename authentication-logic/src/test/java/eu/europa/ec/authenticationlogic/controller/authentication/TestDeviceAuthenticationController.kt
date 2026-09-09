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

import android.content.Context
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC
import androidx.biometric.BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL
import androidx.fragment.app.FragmentActivity
import eu.europa.ec.authenticationlogic.controller.storage.BiometryStorageController
import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.businesslogic.controller.crypto.CryptoController
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mockStatic
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
class TestDeviceAuthenticationController {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var biometric: BiometricAuthenticationController

    @Mock
    private lateinit var resources: ResourceProvider

    @Mock
    private lateinit var cryptoController: CryptoController

    @Mock
    private lateinit var storage: BiometryStorageController

    @Mock
    private lateinit var manager: BiometricManager

    @Mock
    private lateinit var context: Context

    private val outcomes = mutableListOf<String>()

    private val result = DeviceAuthenticationResult(
        onAuthenticationSuccess = { outcomes += "success" },
        onAuthenticationError = { outcomes += "error" },
        onAuthenticationFailure = { outcomes += "failure" },
    )

    private lateinit var host: ActivityController<FragmentActivity>
    private lateinit var controller: DeviceAuthenticationController

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        host = Robolectric.buildActivity(FragmentActivity::class.java).setup()

        whenever(resources.getString(any())).thenReturn("Authenticate")
        whenever(resources.getString(R.string.generic_cancel)).thenReturn("Cancel")
        whenever(resources.provideContext()).thenReturn(host.get())

        controller = DeviceAuthenticationControllerImpl(
            resourceProvider = resources,
            biometricAuthenticationController = biometric
        )
    }

    @After
    fun after() {
        try {
            if (::host.isInitialized && !host.get().isDestroyed) {
                host.pause().stop().destroy()
            }
            coroutineRule.testScope.runCurrent()
        } finally {
            closeable.close()
        }
    }

    @Test
    fun `invalid host reports error instead of leaving caller waiting`() {
        controller.authenticate(context, BiometricCrypto(null), true, result)

        assertEquals(listOf("error"), outcomes)
    }

    @Test
    fun `device authentication retains weak biometrics and device credential fallback`() =
        coroutineRule.runTest {
            assertNonCryptoPromptAccepted()
        }

    @Test
    @Config(sdk = [29])
    fun `Android 10 non crypto request retains device credential fallback`() =
        coroutineRule.runTest {
            assertNonCryptoPromptAccepted()
        }

    @Test
    fun `terminal errors and rejected scans keep their existing device callbacks`() =
        coroutineRule.runTest {
            whenever(biometric.authenticate(any(), any(), any(), any()))
                .thenReturn(BiometricPromptData(null, BiometricPrompt.ERROR_LOCKOUT, "Locked"))

            controller.authenticate(host.get(), BiometricCrypto(null), true, result)
            coroutineRule.testScope.runCurrent()

            assertEquals(listOf("error"), outcomes)

            whenever(biometric.authenticate(any(), any(), any(), any()))
                .thenReturn(BiometricPromptData(null))

            controller.authenticate(host.get(), BiometricCrypto(null), true, result)
            coroutineRule.testScope.runCurrent()

            assertEquals(listOf("error", "failure"), outcomes)
        }

    @Test
    @Config(sdk = [29])
    fun `Android 10 crypto request uses strong biometrics and a cancel button`() =
        coroutineRule.runTest {
            assertCryptoPromptAccepted(BIOMETRIC_STRONG, "Cancel")
        }

    @Test
    fun `crypto request allows strong biometrics or device credentials`() = coroutineRule.runTest {
        assertCryptoPromptAccepted(BIOMETRIC_STRONG or DEVICE_CREDENTIAL, "")
    }

    @Test
    @Config(sdk = [30])
    fun `Android 11 crypto request allows strong biometrics or device credentials`() =
        coroutineRule.runTest {
            assertCryptoPromptAccepted(BIOMETRIC_STRONG or DEVICE_CREDENTIAL, "")
        }

    @Test
    fun `device credential alone satisfies crypto availability without enrolled biometrics`() {
        whenever(manager.canAuthenticate(BIOMETRIC_STRONG))
            .thenReturn(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)
        whenever(manager.canAuthenticate(BIOMETRIC_WEAK))
            .thenReturn(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)
        whenever(manager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL))
            .thenReturn(BiometricManager.BIOMETRIC_SUCCESS)

        withBiometricManager {
            val availability = realDeviceController().deviceSupportsBiometrics(cryptoRequest())

            assertEquals(BiometricsAvailability.CanAuthenticate, availability)
        }
    }

    @Test
    fun `device credential alone satisfies non crypto availability`() {
        whenever(manager.canAuthenticate(BIOMETRIC_WEAK))
            .thenReturn(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)
        whenever(manager.canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL))
            .thenReturn(BiometricManager.BIOMETRIC_SUCCESS)

        withBiometricManager {
            val availability =
                realDeviceController().deviceSupportsBiometrics(BiometricCrypto(null))

            assertEquals(BiometricsAvailability.CanAuthenticate, availability)
        }
    }

    @Test
    @Config(sdk = [29])
    fun `Android 10 crypto availability rejects weak only enrollment`() {
        whenever(manager.canAuthenticate(BIOMETRIC_STRONG))
            .thenReturn(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)
        whenever(manager.canAuthenticate(BIOMETRIC_WEAK))
            .thenReturn(BiometricManager.BIOMETRIC_SUCCESS)

        withBiometricManager {
            val availability = realDeviceController().deviceSupportsBiometrics(cryptoRequest())

            assertTrue(availability is BiometricsAvailability.Failure)
            verify(manager).canAuthenticate(BIOMETRIC_STRONG)
        }
    }

    @Test
    @Config(sdk = [29])
    fun `Android 10 unknown strong availability permits a crypto authentication attempt`() {
        whenever(manager.canAuthenticate(BIOMETRIC_STRONG))
            .thenReturn(BiometricManager.BIOMETRIC_STATUS_UNKNOWN)
        whenever(manager.canAuthenticate(BIOMETRIC_WEAK))
            .thenReturn(BiometricManager.BIOMETRIC_SUCCESS)

        withBiometricManager {
            val availability = realDeviceController().deviceSupportsBiometrics(cryptoRequest())

            assertEquals(BiometricsAvailability.CanAuthenticate, availability)
        }
    }

    @Test
    @Config(sdk = [30])
    fun `Android 11 enrollment includes the authenticators accepted by each request`() {
        val realController = realDeviceController()

        realController.launchBiometricSystemScreen(cryptoRequest())

        val cryptoIntent = shadowOf(host.get()).nextStartedActivity
        assertEquals(Settings.ACTION_BIOMETRIC_ENROLL, cryptoIntent.action)
        assertEquals(
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL,
            cryptoIntent.getIntExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, 0)
        )

        realController.launchBiometricSystemScreen(BiometricCrypto(null))

        val plainIntent = shadowOf(host.get()).nextStartedActivity
        assertEquals(
            BIOMETRIC_WEAK or DEVICE_CREDENTIAL,
            plainIntent.getIntExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, 0)
        )
    }

    @Test
    @Config(sdk = [29])
    fun `Android 10 enrollment uses the security settings fallback`() {
        realDeviceController().launchBiometricSystemScreen(cryptoRequest())

        val intent = shadowOf(host.get()).nextStartedActivity
        assertEquals(Settings.ACTION_SECURITY_SETTINGS, intent.action)
    }

    @Test
    fun `prompt startup exception invokes error once`() = coroutineRule.runTest {
        whenever(biometric.authenticate(any(), any(), any(), any()))
            .thenThrow(IllegalArgumentException("Prompt unavailable"))

        controller.authenticate(host.get(), cryptoRequest(), true, result)
        coroutineRule.testScope.runCurrent()

        assertEquals(listOf("error"), outcomes)
    }

    @Test
    fun `coroutine cancellation terminates an unfinished device request`() = coroutineRule.runTest {
        whenever(biometric.authenticate(any(), any(), any(), any()))
            .thenThrow(CancellationException("Activity destroyed"))

        controller.authenticate(host.get(), cryptoRequest(), true, result)
        coroutineRule.testScope.runCurrent()

        assertEquals(listOf("error"), outcomes)
    }

    @Test
    fun `activity destruction terminates a pending device request exactly once`() =
        coroutineRule.runTest {
            whenever(biometric.authenticate(any(), any(), any(), any())).doSuspendableAnswer {
                CompletableDeferred<BiometricPromptData>().await()
            }

            controller.authenticate(host.get(), cryptoRequest(), false, result)
            coroutineRule.testScope.runCurrent()

            assertTrue(outcomes.isEmpty())

            host.pause().stop().destroy()
            coroutineRule.testScope.runCurrent()

            assertEquals(listOf("error"), outcomes)

            host = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        }

    @Test
    fun `already destroyed activity still terminates a device request`() = coroutineRule.runTest {
        host.pause().stop().destroy()

        controller.authenticate(host.get(), cryptoRequest(), false, result)
        coroutineRule.testScope.runCurrent()

        assertEquals(listOf("error"), outcomes)
        verify(biometric, never()).authenticate(any(), any(), any(), any())

        host = Robolectric.buildActivity(FragmentActivity::class.java).setup()
    }

    @Test
    fun `cancellation after success callback begins does not report a second outcome`() =
        coroutineRule.runTest {
            val completion = CompletableDeferred<Unit>()
            val resultWaitingForCompletion = result.copy(
                onAuthenticationSuccess = {
                    outcomes += "success"
                    completion.await()
                }
            )
            whenever(biometric.authenticate(any(), any(), any(), any()))
                .thenReturn(successfulPromptData())

            controller.authenticate(
                host.get(),
                BiometricCrypto(null),
                false,
                resultWaitingForCompletion
            )
            coroutineRule.testScope.runCurrent()

            assertEquals(listOf("success"), outcomes)

            host.pause().stop().destroy()
            coroutineRule.testScope.runCurrent()

            assertEquals(listOf("success"), outcomes)

            host = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        }

    private suspend fun assertNonCryptoPromptAccepted() {
        whenever(biometric.authenticate(any(), any(), any(), any()))
            .thenReturn(successfulPromptData())

        controller.authenticate(host.get(), BiometricCrypto(null), false, result)
        coroutineRule.testScope.runCurrent()

        val info = argumentCaptor<BiometricPrompt.PromptInfo>()
        verify(biometric).authenticate(
            eq(host.get()),
            eq(BiometricCrypto(null)),
            info.capture(),
            eq(false)
        )
        assertEquals(BIOMETRIC_WEAK or DEVICE_CREDENTIAL, info.firstValue.allowedAuthenticators)
        assertTrue(info.firstValue.negativeButtonText.isEmpty())

        controller.deviceSupportsBiometrics(BiometricCrypto(null))
        verify(biometric).getBiometricsAvailability(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)

        controller.launchBiometricSystemScreen(BiometricCrypto(null))
        verify(biometric).launchBiometricSystemScreen(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)

        assertEquals(listOf("success"), outcomes)
    }

    private suspend fun assertCryptoPromptAccepted(authenticators: Int, negativeButton: String) {
        val crypto = cryptoRequest()
        whenever(biometric.authenticate(any(), any(), any(), any()))
            .thenReturn(
                successfulPromptData(
                    crypto.cryptoObject,
                    AUTHENTICATION_RESULT_TYPE_BIOMETRIC
                )
            )

        controller.authenticate(host.get(), crypto, false, result)
        coroutineRule.testScope.runCurrent()

        val info = argumentCaptor<BiometricPrompt.PromptInfo>()
        verify(biometric).authenticate(eq(host.get()), eq(crypto), info.capture(), eq(false))
        assertEquals(authenticators, info.firstValue.allowedAuthenticators)
        assertEquals(negativeButton, info.firstValue.negativeButtonText.toString())
        assertEquals(listOf("success"), outcomes)

        controller.deviceSupportsBiometrics(crypto)
        verify(biometric).getBiometricsAvailability(authenticators)

        controller.launchBiometricSystemScreen(crypto)
        verify(biometric).launchBiometricSystemScreen(authenticators)

        // Exercise the real AndroidX validation, which rejects WEAK + CryptoObject.
        val prompt = BiometricPrompt(
            host.get(),
            { it.run() },
            object : BiometricPrompt.AuthenticationCallback() {}
        )
        prompt.authenticate(info.firstValue, requireNotNull(crypto.cryptoObject))
        prompt.cancelAuthentication()
    }

    private fun cryptoRequest(): BiometricCrypto {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(ByteArray(32) { 1 }, "AES"))
        }

        return BiometricCrypto(BiometricPrompt.CryptoObject(cipher))
    }

    private fun successfulPromptData(
        cryptoObject: BiometricPrompt.CryptoObject? = null,
        authenticationType: Int = AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL
    ) = BiometricPromptData(
        BiometricPrompt.AuthenticationResult(cryptoObject, authenticationType)
    )

    private fun withBiometricManager(block: () -> Unit) {
        mockStatic(BiometricManager::class.java).use { factory ->
            factory.`when`<BiometricManager> { BiometricManager.from(host.get()) }
                .thenReturn(manager)

            block()
        }
    }

    private fun realDeviceController() = DeviceAuthenticationControllerImpl(
        resourceProvider = resources,
        biometricAuthenticationController = BiometricAuthenticationControllerImpl(
            resourceProvider = resources,
            cryptoController = cryptoController,
            biometryStorageController = storage,
            dispatcher = coroutineRule.testDispatcher
        )
    )
}