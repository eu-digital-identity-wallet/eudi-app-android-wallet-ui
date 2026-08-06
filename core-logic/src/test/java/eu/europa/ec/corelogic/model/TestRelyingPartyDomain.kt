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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TestRelyingPartyDomain {

    private val mockedAccessCertificateName = "Verifier Signer dev"
    private val mockedSubjectName = "NordicBank A/S"
    private val mockedSubjectUniqueId = "LEIXG-123456789"

    private val mockedDetails = RegistrationDetailsDomain(
        tradeName = mockedSubjectName,
        uniqueId = mockedSubjectUniqueId,
        logoUri = null,
        intendedUse = "mocked intended use",
        privacyPolicyUrl = "https://nordicbank.example/privacy",
        serviceDescription = "mocked service description",
    )

    //region resolveRequesterName

    @Test
    fun `Given a verified registration, When resolveRequesterName is called, Then the registered name outranks the access certificate name`() {
        // Given
        val registration = verified(details = mockedDetails)

        // When
        val result = registration.resolveRequesterName(
            accessCertificateName = mockedAccessCertificateName,
        )

        // Then
        assertEquals(mockedSubjectName, result)
    }

    @Test
    fun `Given a verified registration with no registered name, When resolveRequesterName is called, Then the access certificate name is used`() {
        // Given
        val registration = verified(details = mockedDetails.copy(tradeName = null))

        // When
        val result = registration.resolveRequesterName(
            accessCertificateName = mockedAccessCertificateName,
        )

        // Then
        assertEquals(mockedAccessCertificateName, result)
    }

    @Test
    fun `Given a not verified registration carrying details, When resolveRequesterName is called, Then the access certificate name outranks the parsed name`() {
        // Given
        val registration = notVerified(details = mockedDetails)

        // When
        val result = registration.resolveRequesterName(
            accessCertificateName = mockedAccessCertificateName,
        )

        // Then
        assertEquals(mockedAccessCertificateName, result)
    }

    @Test
    fun `Given a not verified registration and no access certificate name, When resolveRequesterName is called, Then the parsed name is the last resort`() {
        // Given
        val registration = notVerified(details = mockedDetails)

        // When
        val result = registration.resolveRequesterName(accessCertificateName = null)

        // Then
        assertEquals(mockedSubjectName, result)
    }

    @Test
    fun `Given a not verified registration with no parsed details and no access certificate name, When resolveRequesterName is called, Then no name is resolved`() {
        // Given
        val registration = notVerified(details = null)

        // When
        val result = registration.resolveRequesterName(accessCertificateName = null)

        // Then
        assertNull(result)
    }

    @Test
    fun `Given an unevaluated registration, When resolveRequesterName is called, Then the access certificate name is used`() {
        // Given
        val registration = RegistrationStatusDomain.NotEvaluated

        // When
        val result = registration.resolveRequesterName(
            accessCertificateName = mockedAccessCertificateName,
        )

        // Then
        assertEquals(mockedAccessCertificateName, result)
    }

    //endregion

    //region requesterUniqueIdOrNull

    @Test
    fun `Given a verified registration, When requesterUniqueIdOrNull is called, Then the certificate subject identifies the requester`() {
        // Given
        val registration = verified(details = mockedDetails)

        // When
        val result = registration.requesterUniqueIdOrNull()

        // Then
        assertEquals(mockedSubjectUniqueId, result)
    }

    @Test
    fun `Given a not verified registration carrying details, When requesterUniqueIdOrNull is called, Then the parsed identifier is still returned`() {
        // Given
        val registration = notVerified(details = mockedDetails)

        // When
        val result = registration.requesterUniqueIdOrNull()

        // Then
        assertEquals(mockedSubjectUniqueId, result)
    }

    @Test
    fun `Given an unevaluated registration, When requesterUniqueIdOrNull is called, Then no identifier is returned`() {
        // Given
        val registration = RegistrationStatusDomain.NotEvaluated

        // When
        val result = registration.requesterUniqueIdOrNull()

        // Then
        assertNull(result)
    }

    //endregion

    //region helper functions

    private fun verified(details: RegistrationDetailsDomain): RegistrationStatusDomain =
        RegistrationStatusDomain.Verified(details = details, overaskedClaims = emptyList())

    private fun notVerified(details: RegistrationDetailsDomain?): RegistrationStatusDomain =
        RegistrationStatusDomain.NotVerified(
            reason = RegistrationFailureReasonDomain.REVOCATION_STATUS_UNKNOWN,
            details = details,
        )

    //endregion
}