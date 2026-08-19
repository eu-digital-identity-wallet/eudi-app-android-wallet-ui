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

package eu.europa.ec.corelogic.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestIssuerRegistrationDomain {

    //region isBlockedForIssuance

    @Test
    fun `Given a verified registration, When isBlockedForIssuance is read, Then the issuance proceeds`() {
        // Given
        val registration = IssuerRegistrationDomain.Verified(details = mockedDetails)

        // When
        val result = registration.isBlockedForIssuance

        // Then
        assertFalse(result)
    }

    @Test
    fun `Given an offer beyond the registered scope, When isBlockedForIssuance is read, Then the issuance is refused`() {
        // Given
        val registration = IssuerRegistrationDomain.Blocked(
            reason = IssuerRegistrationDomain.BlockedReasonDomain.ATTESTATION_NOT_REGISTERED,
            details = mockedDetails,
        )

        // When
        val result = registration.isBlockedForIssuance

        // Then
        assertTrue(result)
    }

    @Test
    fun `Given a failed validation, When isBlockedForIssuance is read, Then the issuance is refused`() {
        // Given
        val registration = IssuerRegistrationDomain.NotVerified(
            reason = RegistrationFailureReasonDomain.SIGNATURE_INVALID,
            details = mockedDetails,
        )

        // When
        val result = registration.isBlockedForIssuance

        // Then
        assertTrue(result)
    }

    // a registration that carries no parsed details is refused on the same grounds as one that does
    @Test
    fun `Given a failed validation with nothing parsed, When isBlockedForIssuance is read, Then the issuance is refused`() {
        // Given
        val registration = IssuerRegistrationDomain.NotVerified(
            reason = RegistrationFailureReasonDomain.SIGNATURE_INVALID,
            details = null,
        )

        // When
        val result = registration.isBlockedForIssuance

        // Then
        assertTrue(result)
    }

    // the case the registration-check setting produces: with the check off Wallet Core evaluates
    // nothing, so the gates must ask whether the check is running before consulting this
    @Test
    fun `Given a registration that was never evaluated, When isBlockedForIssuance is read, Then the issuance is refused`() {
        // Given
        val registration = IssuerRegistrationDomain.NotEvaluated

        // When
        val result = registration.isBlockedForIssuance

        // Then
        assertTrue(result)
    }

    //endregion

    //region Mocked objects needed for tests.

    private val mockedDetails = RegistrationDetailsDomain(
        tradeName = "NordicBank A/S",
        uniqueId = "LEIXG-123456789",
        logoUri = null,
        intendedUse = "mocked intended use",
        privacyPolicyUrl = "https://nordicbank.example/privacy",
        serviceDescription = "mocked service description",
    )

    //endregion
}