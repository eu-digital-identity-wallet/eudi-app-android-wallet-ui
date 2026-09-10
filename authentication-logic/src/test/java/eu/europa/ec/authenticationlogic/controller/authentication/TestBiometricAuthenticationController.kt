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
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package eu.europa.ec.authenticationlogic.controller.authentication

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import eu.europa.ec.authenticationlogic.controller.storage.BiometryStorageController
import eu.europa.ec.businesslogic.controller.crypto.CryptoController
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testlogic.base.getMockedContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TestBiometricAuthenticationController {

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var cryptoController: CryptoController

    @Mock
    private lateinit var biometryStorageController: BiometryStorageController

    @Mock
    private lateinit var biometricManager: BiometricManager

    private lateinit var controller: BiometricAuthenticationController
    private lateinit var biometricManagerFactory: MockedStatic<BiometricManager>
    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        val context = getMockedContext()
        whenever(resourceProvider.provideContext()).thenReturn(context)

        biometricManagerFactory = mockStatic(BiometricManager::class.java)
        biometricManagerFactory.`when`<BiometricManager> { BiometricManager.from(context) }
            .thenReturn(biometricManager)

        controller = BiometricAuthenticationControllerImpl(
            resourceProvider = resourceProvider,
            cryptoController = cryptoController,
            biometryStorageController = biometryStorageController
        )

        whenever(resourceProvider.getString(STRONG_REQUIRED_RES_ID))
            .thenReturn(STRONG_REQUIRED_MESSAGE)
        whenever(resourceProvider.getString(NO_HARDWARE_RES_ID))
            .thenReturn(NO_HARDWARE_MESSAGE)
        whenever(resourceProvider.getString(UNKNOWN_ERROR_RES_ID))
            .thenReturn(UNKNOWN_ERROR_MESSAGE)
    }

    @After
    fun after() {
        try {
            biometricManagerFactory.close()
        } finally {
            closeable.close()
        }
    }

    //region getBiometricsAvailability

    @Test
    fun `Given weak biometrics but no strong hardware, Then explain the security requirement`() {
        whenever(biometricManager.canAuthenticate(BIOMETRIC_STRONG))
            .thenReturn(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE)
        whenever(biometricManager.canAuthenticate(BIOMETRIC_WEAK))
            .thenReturn(BiometricManager.BIOMETRIC_SUCCESS)

        val result = controller.getBiometricsAvailability(BIOMETRIC_STRONG)

        assertEquals(BiometricsAvailability.Failure(STRONG_REQUIRED_MESSAGE), result)
    }

    @Test
    fun `Given availability is unknown, Then allow the prompt to enforce the requested strength`() {
        whenever(biometricManager.canAuthenticate(BIOMETRIC_STRONG))
            .thenReturn(BiometricManager.BIOMETRIC_STATUS_UNKNOWN)
        whenever(biometricManager.canAuthenticate(BIOMETRIC_WEAK))
            .thenReturn(BiometricManager.BIOMETRIC_SUCCESS)

        val result = controller.getBiometricsAvailability(BIOMETRIC_STRONG)

        assertEquals(BiometricsAvailability.CanAuthenticate, result)
    }

    // Case: STRONG check succeeds, availability is CanAuthenticate
    @Test
    fun `Given canAuthenticate returns SUCCESS, When checking availability, Then result is CanAuthenticate`() {
        whenever(biometricManager.canAuthenticate(BIOMETRIC_STRONG))
            .thenReturn(BiometricManager.BIOMETRIC_SUCCESS)

        val result = controller.getBiometricsAvailability(BIOMETRIC_STRONG)

        assertTrue(result is BiometricsAvailability.CanAuthenticate)
        verify(biometricManager, never()).canAuthenticate(BIOMETRIC_WEAK)
    }

    // Case: only a WEAK biometric is enrolled; the crypto flow requires STRONG so the
    // result is a Failure with the strong-required message (previously the WEAK check
    // passed and the flow failed silently in the cipher path)
    @Test
    fun `Given only weak biometric enrolled, When checking strong availability, Then result is Failure with strong required message`() {
        whenever(biometricManager.canAuthenticate(BIOMETRIC_STRONG))
            .thenReturn(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)
        whenever(biometricManager.canAuthenticate(BIOMETRIC_WEAK))
            .thenReturn(BiometricManager.BIOMETRIC_SUCCESS)

        val result = controller.getBiometricsAvailability(BIOMETRIC_STRONG)

        assertEquals(BiometricsAvailability.Failure(STRONG_REQUIRED_MESSAGE), result)
    }

    // Case: nothing is enrolled at all (neither weak nor strong), result is NonEnrolled so
    // the user can be routed to the system enrollment screen
    @Test
    fun `Given no biometric enrolled, When checking strong availability, Then result is NonEnrolled`() {
        whenever(biometricManager.canAuthenticate(BIOMETRIC_STRONG))
            .thenReturn(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)
        whenever(biometricManager.canAuthenticate(BIOMETRIC_WEAK))
            .thenReturn(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)

        val result = controller.getBiometricsAvailability(BIOMETRIC_STRONG)

        assertTrue(result is BiometricsAvailability.NonEnrolled)
    }

    // Case: WEAK check (non-crypto flow) with nothing enrolled, result is NonEnrolled
    @Test
    fun `Given no biometric enrolled, When checking weak availability, Then result is NonEnrolled`() {
        whenever(biometricManager.canAuthenticate(BIOMETRIC_WEAK))
            .thenReturn(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)

        val result = controller.getBiometricsAvailability(BIOMETRIC_WEAK)

        assertTrue(result is BiometricsAvailability.NonEnrolled)
    }

    // Case: no biometric hardware, result is Failure with the no-hardware message
    @Test
    fun `Given no biometric hardware, When checking availability, Then result is Failure with no hardware message`() {
        whenever(biometricManager.canAuthenticate(BIOMETRIC_WEAK))
            .thenReturn(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE)

        val result = controller.getBiometricsAvailability(BIOMETRIC_WEAK)

        assertEquals(BiometricsAvailability.Failure(NO_HARDWARE_MESSAGE), result)
    }

    // Case: unknown error code, result is Failure with the unknown-error message
    @Test
    fun `Given unknown canAuthenticate code, When checking availability, Then result is Failure with unknown error message`() {
        whenever(biometricManager.canAuthenticate(BIOMETRIC_STRONG))
            .thenReturn(999)
        whenever(biometricManager.canAuthenticate(BIOMETRIC_WEAK))
            .thenReturn(BiometricManager.BIOMETRIC_SUCCESS)

        val result = controller.getBiometricsAvailability(BIOMETRIC_STRONG)

        assertEquals(BiometricsAvailability.Failure(UNKNOWN_ERROR_MESSAGE), result)
    }

    //endregion

    //region Mocked objects

    private companion object {
        val STRONG_REQUIRED_RES_ID = R.string.biometric_strong_required
        val NO_HARDWARE_RES_ID = R.string.biometric_no_hardware
        val UNKNOWN_ERROR_RES_ID = R.string.biometric_unknown_error

        const val STRONG_REQUIRED_MESSAGE = "strong required"
        const val NO_HARDWARE_MESSAGE = "no hardware"
        const val UNKNOWN_ERROR_MESSAGE = "unknown error"
    }

    //endregion
}