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

package eu.europa.ec.commonfeature.interactor

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import eu.europa.ec.authenticationlogic.config.AuthenticationConfig
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricAuthenticationController
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAuthenticate
import eu.europa.ec.authenticationlogic.controller.session.SessionController
import eu.europa.ec.authenticationlogic.controller.storage.BiometryStorageController
import eu.europa.ec.authenticationlogic.controller.throttle.PinThrottleController
import eu.europa.ec.authenticationlogic.provider.PinLockoutState
import eu.europa.ec.authenticationlogic.secure.SecurePinImpl
import eu.europa.ec.testfeature.util.mockedNotifyOnAuthenticationFailure
import eu.europa.ec.testlogic.base.getMockedContext
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TestBiometricInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var biometryStorageController: BiometryStorageController

    @Mock
    private lateinit var biometricAuthenticationController: BiometricAuthenticationController

    @Mock
    private lateinit var quickPinInteractor: QuickPinInteractor

    @Mock
    private lateinit var pinThrottleController: PinThrottleController

    @Mock
    private lateinit var authenticationConfig: AuthenticationConfig

    @Mock
    private lateinit var sessionController: SessionController

    private lateinit var interactor: BiometricInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = BiometricInteractorImpl(
            biometryStorageController = biometryStorageController,
            biometricAuthenticationController = biometricAuthenticationController,
            quickPinInteractor = quickPinInteractor,
            pinThrottleController = pinThrottleController,
            authenticationConfig = authenticationConfig,
            sessionController = sessionController
        )
    }

    @After
    fun after() {
        closeable.close()
    }

    @Test
    fun `Given a session controller, When unlockSession is called, Then the session is unlocked`() {
        // When
        interactor.unlockSession()

        // Then
        verify(sessionController).unlock()
    }

    //region isPinValid

    // Case: isPinValid behaviour
    // When isCurrentPinValid returns QuickPinInteractorPinValidPartialState.Success,
    // the expected result of isPinValid is Success
    @Test
    fun `Given isCurrentPinValid returns state Success, When isPinValid is called, Then assert the result is the expected`() =
        coroutineRule.runTest {
            // Given
            val pin = SecurePinImpl(mockedPin)
            whenever(quickPinInteractor.isCurrentPinValid(pin)).thenReturn(
                QuickPinInteractorPinValidPartialState.Success.toFlow()
            )

            // When
            interactor.isPinValid(pin).runFlowTest {
                // Then
                val expectedResult = QuickPinInteractorPinValidPartialState.Success
                assertEquals(expectedResult, awaitItem())
            }
        }
    //endregion

    //region launchBiometricSystemScreen

    // Case: launchBiometricSystemScreen behaviour
    @Test
    fun `Given a biometric controller, When launchBiometricSystemScreen is called, Then the controller launches it for strong biometrics`() {
        // When
        interactor.launchBiometricSystemScreen()

        // Then
        verify(biometricAuthenticationController).launchBiometricSystemScreen(BIOMETRIC_STRONG)
    }
    //endregion

    //region getBiometricUserSelection

    // Case: getBiometricUserSelection behaviour
    // When getUseBiometricsAuth returns true, the expected result of getBiometricUserSelection
    // should be true
    @Test
    fun `Given biometrics use is stored as true, When getBiometricUserSelection is called, Then true is returned`() =
        coroutineRule.runTest {
            // Given
            whenever(biometryStorageController.getUseBiometricsAuth()).thenReturn(true)

            // When
            val result = interactor.getBiometricUserSelection()

            // Then
            assertEquals(true, result)
            verify(biometryStorageController).getUseBiometricsAuth()
        }
    //endregion

    //region storeBiometricsUsageDecision

    // Case: storeBiometricsUsageDecision behaviour
    @Test
    fun `Given a decision to use biometrics, When storeBiometricsUsageDecision is called, Then setUseBiometricsAuth is executed`() =
        coroutineRule.runTest {
            // Given
            val shouldUseBiometrics = true

            // When
            interactor.storeBiometricsUsageDecision(shouldUseBiometrics = shouldUseBiometrics)

            // Then
            verify(biometryStorageController).setUseBiometricsAuth(shouldUseBiometrics)
        }
    //endregion

    //region getBiometricsAvailability

    // Case: getBiometricsAvailability behaviour
    @Test
    fun `Given a biometric controller, When getBiometricsAvailability is called, Then the controller checks strong biometrics`() {
        // When
        interactor.getBiometricsAvailability()

        // Then
        verify(biometricAuthenticationController).getBiometricsAvailability(BIOMETRIC_STRONG)
    }
    //endregion

    //region authenticateWithBiometrics

    // Case: authenticateWithBiometrics behaviour
    // Defining a mock BiometricsAuthenticate function callback to verify that authenticate function
    // on the biometricAuthenticationController is executed when authenticateWithBiometrics is called
    @Test
    fun `Given a context and a listener, When authenticateWithBiometrics is called, Then authenticate is executed with the same parameters`() {
        // Given
        val mockListener: (BiometricsAuthenticate) -> Unit = mock()
        val context = getMockedContext()

        // When
        interactor.authenticateWithBiometrics(
            context = context,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            listener = mockListener
        )

        // Then
        verify(biometricAuthenticationController).authenticate(
            context,
            mockedNotifyOnAuthenticationFailure,
            mockListener
        )
    }
    //endregion

    //region pin throttle delegation

    @Test
    fun `Given a configured maximum of failed PIN attempts, When maxFailedPinAttempts is read, Then the configured value is returned`() {
        // Given
        whenever(authenticationConfig.maxFailedPinAttempts).thenReturn(5)

        // When
        val result = interactor.maxFailedPinAttempts

        // Then
        assertEquals(5, result)
    }

    @Test
    fun `Given a throttle state, When getPinLockoutState is called, Then the throttle controller state is returned`() =
        coroutineRule.runTest {
            // Given
            val expected = PinLockoutState.Idle
            whenever(pinThrottleController.getState()).thenReturn(expected)

            // When
            val result = interactor.getPinLockoutState()

            // Then
            assertEquals(expected, result)
            verify(pinThrottleController).getState()
        }

    @Test
    fun `Given a throttle controller, When recordPinFailure is called, Then its recorded failure state is returned`() =
        coroutineRule.runTest {
            // Given
            val expected = PinLockoutState.Idle
            whenever(pinThrottleController.recordFailure()).thenReturn(expected)

            // When
            val result = interactor.recordPinFailure()

            // Then
            assertEquals(expected, result)
            verify(pinThrottleController).recordFailure()
        }

    @Test
    fun `Given a throttle controller, When resetPinThrottle is called, Then a success is recorded`() =
        coroutineRule.runTest {
            // When
            interactor.resetPinThrottle()

            // Then
            verify(pinThrottleController).recordSuccess()
        }
    //endregion

    @Test
    fun `Given a device without a screen lock, When the login notice is read, Then it is the notice setup shows`() {
        // Given
        val notice = DeviceLockNotice.Blocking("Set a screen lock to use the wallet.")
        whenever(quickPinInteractor.getDeviceLockNotice()).thenReturn(notice)

        // When
        val result = interactor.getDeviceLockNotice()

        // Then
        assertEquals(notice, result)
    }

    //region Mocked objects
    private val mockedPin = "1234"
    //endregion
}