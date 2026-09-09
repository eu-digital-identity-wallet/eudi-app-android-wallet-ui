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
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TestBiometricAuthenticationController {

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        whenever(resourceProvider.getString(STRONG_REQUIRED_RES_ID))
            .thenReturn(STRONG_REQUIRED_MESSAGE)
        whenever(resourceProvider.getString(NO_HARDWARE_RES_ID))
            .thenReturn(NO_HARDWARE_MESSAGE)
        whenever(resourceProvider.getString(UNKNOWN_ERROR_RES_ID))
            .thenReturn(UNKNOWN_ERROR_MESSAGE)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region resolveBiometricsAvailability

    @Test
    fun `Given weak biometrics but no strong hardware, Then explain the security requirement`() {
        val result = resolveAvailability(
            canAuthenticate = BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            requireStrong = true,
            canAuthenticateWeak = BiometricManager.BIOMETRIC_SUCCESS
        )

        assertEquals(BiometricsAvailability.Failure(STRONG_REQUIRED_MESSAGE), result)
    }

    @Test
    fun `Given availability is unknown, Then allow the prompt to enforce the requested strength`() {
        val result = resolveAvailability(
            canAuthenticate = BiometricManager.BIOMETRIC_STATUS_UNKNOWN,
            requireStrong = true,
            canAuthenticateWeak = BiometricManager.BIOMETRIC_SUCCESS
        )

        assertEquals(BiometricsAvailability.CanAuthenticate, result)
    }

    // Case: STRONG check succeeds, availability is CanAuthenticate
    @Test
    fun `Given canAuthenticate returns SUCCESS, When resolving availability, Then result is CanAuthenticate`() {
        val result = resolveAvailability(
            canAuthenticate = BiometricManager.BIOMETRIC_SUCCESS,
            requireStrong = true,
            canAuthenticateWeak = BiometricManager.BIOMETRIC_SUCCESS
        )

        assertTrue(result is BiometricsAvailability.CanAuthenticate)
    }

    // Case: only a WEAK biometric is enrolled; the crypto flow requires STRONG so the
    // result is a Failure with the strong-required message (previously the WEAK check
    // passed and the flow failed silently in the cipher path)
    @Test
    fun `Given only weak biometric enrolled, When resolving crypto availability, Then result is Failure with strong required message`() {
        val result = resolveAvailability(
            canAuthenticate = BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            requireStrong = true,
            canAuthenticateWeak = BiometricManager.BIOMETRIC_SUCCESS
        )

        assertEquals(BiometricsAvailability.Failure(STRONG_REQUIRED_MESSAGE), result)
    }

    // Case: nothing is enrolled at all (neither weak nor strong), result is NonEnrolled so
    // the user can be routed to the system enrollment screen
    @Test
    fun `Given no biometric enrolled, When resolving crypto availability, Then result is NonEnrolled`() {
        val result = resolveAvailability(
            canAuthenticate = BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            requireStrong = true,
            canAuthenticateWeak = BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
        )

        assertTrue(result is BiometricsAvailability.NonEnrolled)
    }

    // Case: WEAK check (non-crypto flow) with nothing enrolled, result is NonEnrolled
    @Test
    fun `Given no biometric enrolled, When resolving general availability, Then result is NonEnrolled`() {
        val result = resolveAvailability(
            canAuthenticate = BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            requireStrong = false,
            canAuthenticateWeak = BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
        )

        assertTrue(result is BiometricsAvailability.NonEnrolled)
    }

    // Case: no biometric hardware, result is Failure with the no-hardware message
    @Test
    fun `Given no biometric hardware, When resolving availability, Then result is Failure with no hardware message`() {
        val result = resolveAvailability(
            canAuthenticate = BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            requireStrong = false,
            canAuthenticateWeak = BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
        )

        assertEquals(BiometricsAvailability.Failure(NO_HARDWARE_MESSAGE), result)
    }

    // Case: unknown error code, result is Failure with the unknown-error message
    @Test
    fun `Given unknown canAuthenticate code, When resolving availability, Then result is Failure with unknown error message`() {
        val result = resolveAvailability(
            canAuthenticate = 999,
            requireStrong = true,
            canAuthenticateWeak = BiometricManager.BIOMETRIC_SUCCESS
        )

        assertEquals(BiometricsAvailability.Failure(UNKNOWN_ERROR_MESSAGE), result)
    }

    //endregion

    private fun resolveAvailability(
        canAuthenticate: Int,
        requireStrong: Boolean,
        canAuthenticateWeak: Int
    ): BiometricsAvailability = BiometricAuthenticationControllerImpl.resolveBiometricsAvailability(
        canAuthenticate = canAuthenticate,
        requireStrong = requireStrong,
        canAuthenticateWeak = canAuthenticateWeak,
        stringProvider = resourceProvider::getString
    )

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