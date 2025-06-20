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

package eu.europa.ec.commonfeature.interactor

import eu.europa.ec.authenticationlogic.controller.authentication.BiometricAuthenticationController
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAuthenticate
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAvailability
import eu.europa.ec.authenticationlogic.controller.storage.BiometryStorageController
import eu.europa.ec.testfeature.util.mockedNotifyOnAuthenticationFailure
import eu.europa.ec.testlogic.base.TestApplication
import eu.europa.ec.testlogic.base.getMockedContext
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import junit.framework.TestCase.assertEquals
import org.junit.After
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class TestBiometricInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var biometryStorageController: BiometryStorageController

    @Mock
    private lateinit var biometricAuthenticationController: BiometricAuthenticationController

    @Mock
    private lateinit var quickPinInteractor: QuickPinInteractor

    private lateinit var interactor: BiometricInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = BiometricInteractorImpl(
            biometryStorageController = biometryStorageController,
            biometricAuthenticationController = biometricAuthenticationController,
            quickPinInteractor = quickPinInteractor
        )
    }

    @After
    fun after() {
        closeable.close()
    }

    //region isPinValid

    // Case: isPinValid behaviour
    // When isCurrentPinValid returns QuickPinInteractorPinValidPartialState.Success,
    // the expected result of isPinValid is Success
    @Test
    fun `Given isCurrentPinValid returns state Success, When isPinValid is called, Then assert the result is the expected`() =
        coroutineRule.runTest {
            // Given
            whenever(quickPinInteractor.isCurrentPinValid(mockedPin)).thenReturn(
                QuickPinInteractorPinValidPartialState.Success.toFlow()
            )

            // When
            interactor.isPinValid(mockedPin).runFlowTest {
                // Then
                val expectedResult = QuickPinInteractorPinValidPartialState.Success
                assertEquals(expectedResult, awaitItem())
            }
        }
    //endregion

    //region launchBiometricSystemScreen

    // Case: launchBiometricSystemScreen behaviour
    @Test
    fun `When launchBiometricSystemScreen is called, Then verify function is executed on the controller`() {
        // When
        interactor.launchBiometricSystemScreen()

        // Then
        verify(biometricAuthenticationController).launchBiometricSystemScreen()
    }
    //endregion

    ///region getBiometricUserSelection

    // Case: getBiometricUserSelection behaviour
    // When getUseBiometricsAuth returns true, the expected result of getBiometricUserSelection
    // should be true
    @Test
    fun `When getBiometricUserSelection is called, Then assert the correct value is returned`() {
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
    fun `When storeBiometricsUsageDecision is called, Then verify setUseBiometricsAuth is executed`() {
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
    fun `When getBiometricsAvailability is called, Then verify deviceSupportsBiometrics is executed`() {
        // Given
        val mockListener: (BiometricsAvailability) -> Unit = mock()

        // When
        interactor.getBiometricsAvailability(mockListener)

        // Then
        verify(biometricAuthenticationController).deviceSupportsBiometrics(mockListener)
    }
    //endregion

    //region authenticateWithBiometrics

    // Case: authenticateWithBiometrics behaviour
    // Defining a mock BiometricsAuthenticate function callback to verify that authenticate function
    // on the biometricAuthenticationController is executed when authenticateWithBiometrics is called
    @Test
    fun `When authenticateWithBiometrics is called, Then verify authenticate is executed with correct parameters`() {
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

    //region Mocked objects
    private val mockedPin = "1234"
    //endregion
}