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

package eu.europa.ec.commonfeature.ui.request.model

import eu.europa.ec.corelogic.model.ClaimPathDomain
import eu.europa.ec.corelogic.model.ClaimType
import eu.europa.ec.corelogic.model.OveraskedClaimDomain
import eu.europa.ec.corelogic.model.RegistrationDetailsDomain
import eu.europa.ec.corelogic.model.RegistrationFailureReasonDomain
import eu.europa.ec.corelogic.model.RegistrationStatusDomain
import eu.europa.ec.corelogic.model.RelyingPartyDomain
import eu.europa.ec.testfeature.util.mockedVerifierName
import eu.europa.ec.uilogic.component.RelyingPartyDataUi
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test
import java.net.URI

class TestRelyingPartyHeaderUi {

    private val mockedFallbackName = "Unknown relying party"
    private val mockedRequesterLogoUri = URI("https://rpservices.example/logo.png")
    private val mockedDetailsLogoUri = URI("https://nordicbank.example/logo.png")
    private val mockedTradeName = "NordicBank A/S"

    private val mockedDetails = RegistrationDetailsDomain(
        tradeName = mockedTradeName,
        uniqueId = "rp:nordicbank:prod",
        logoUri = mockedDetailsLogoUri,
        intendedUse = "mocked intended use",
        privacyPolicyUrl = "https://nordicbank.example/privacy",
        serviceDescription = "mocked service description",
    )

    private val mockedOveraskedClaim = OveraskedClaimDomain(
        path = ClaimPathDomain.ofPlainKeys(
            names = listOf("family_name"),
            type = ClaimType.SdJwtVc,
        ),
        attestationTypes = emptySet(),
    )

    //region toRelyingPartyHeaderUi

    @Test
    fun `Given a verified registration, When toRelyingPartyHeaderUi is called, Then the header shows the badge and the registration sections`() {
        // Given
        val relyingParty = buildRelyingPartyDomain(
            name = mockedVerifierName,
            uniqueId = mockedDetails.uniqueId,
            hasTrustedAccessCertificate = true,
            registration = RegistrationStatusDomain.Verified(
                details = mockedDetails,
                overaskedClaims = emptyList(),
            ),
        )

        // When
        val result = relyingParty.toRelyingPartyHeaderUi(fallbackName = mockedFallbackName)

        // Then
        assertEquals(
            RelyingPartyHeaderUi(
                relyingParty = RelyingPartyDataUi(
                    logo = mockedRequesterLogoUri,
                    isVerified = true,
                    name = mockedVerifierName,
                    uniqueId = mockedDetails.uniqueId,
                    description = null,
                ),
                intendedUse = mockedDetails.intendedUse,
                privacyPolicyUrl = mockedDetails.privacyPolicyUrl,
            ),
            result,
        )
    }

    @Test
    fun `Given a verified registration behind an untrusted access certificate, When toRelyingPartyHeaderUi is called, Then the sections render without the badge`() {
        // Given
        val relyingParty = buildRelyingPartyDomain(
            name = mockedVerifierName,
            uniqueId = mockedDetails.uniqueId,
            hasTrustedAccessCertificate = false,
            registration = RegistrationStatusDomain.Verified(
                details = mockedDetails,
                overaskedClaims = emptyList(),
            ),
        )

        // When
        val result = relyingParty.toRelyingPartyHeaderUi(fallbackName = mockedFallbackName)

        // Then
        assertEquals(false, result.relyingParty.isVerified)
        assertEquals(mockedDetails.intendedUse, result.intendedUse)
        assertEquals(mockedDetails.privacyPolicyUrl, result.privacyPolicyUrl)
    }

    @Test
    fun `Given a not verified registration with no parsed details, When toRelyingPartyHeaderUi is called, Then the badge is hidden and the registration sections are absent`() {
        // Given
        val relyingParty = buildRelyingPartyDomain(
            name = mockedVerifierName,
            uniqueId = null,
            hasTrustedAccessCertificate = true,
            registration = RegistrationStatusDomain.NotVerified(
                reason = RegistrationFailureReasonDomain.CERTIFICATE_ABSENT,
                details = null,
            ),
        )

        // When
        val result = relyingParty.toRelyingPartyHeaderUi(fallbackName = mockedFallbackName)

        // Then
        assertEquals(false, result.relyingParty.isVerified)
        assertNull(result.intendedUse)
        assertNull(result.privacyPolicyUrl)
    }

    @Test
    fun `Given a not verified registration carrying parsed details, When toRelyingPartyHeaderUi is called, Then the registration sections render without the badge`() {
        // Given
        val relyingParty = buildRelyingPartyDomain(
            name = mockedVerifierName,
            uniqueId = mockedDetails.uniqueId,
            hasTrustedAccessCertificate = true,
            registration = RegistrationStatusDomain.NotVerified(
                reason = RegistrationFailureReasonDomain.REVOCATION_STATUS_UNKNOWN,
                details = mockedDetails,
            ),
        )

        // When
        val result = relyingParty.toRelyingPartyHeaderUi(fallbackName = mockedFallbackName)

        // Then
        assertEquals(false, result.relyingParty.isVerified)
        assertEquals(mockedDetails.intendedUse, result.intendedUse)
        assertEquals(mockedDetails.privacyPolicyUrl, result.privacyPolicyUrl)
    }

    @Test
    fun `Given an unevaluated registration and a trusted access certificate, When toRelyingPartyHeaderUi is called, Then the badge falls back to the access certificate trust`() {
        // Given
        val relyingParty = buildRelyingPartyDomain(
            name = mockedVerifierName,
            uniqueId = null,
            hasTrustedAccessCertificate = true,
            registration = RegistrationStatusDomain.NotEvaluated,
        )

        // When
        val result = relyingParty.toRelyingPartyHeaderUi(fallbackName = mockedFallbackName)

        // Then
        assertEquals(true, result.relyingParty.isVerified)
        assertNull(result.intendedUse)
        assertNull(result.privacyPolicyUrl)
    }

    @Test
    fun `Given an unevaluated registration and an untrusted access certificate, When toRelyingPartyHeaderUi is called, Then the badge is hidden`() {
        // Given
        val relyingParty = buildRelyingPartyDomain(
            name = mockedVerifierName,
            uniqueId = null,
            hasTrustedAccessCertificate = false,
            registration = RegistrationStatusDomain.NotEvaluated,
        )

        // When
        val result = relyingParty.toRelyingPartyHeaderUi(fallbackName = mockedFallbackName)

        // Then
        assertEquals(false, result.relyingParty.isVerified)
    }

    @Test
    fun `Given a null relying party name, When toRelyingPartyHeaderUi is called, Then the fallback name is used`() {
        // Given
        val relyingParty = buildRelyingPartyDomain(
            name = null,
            uniqueId = null,
            hasTrustedAccessCertificate = true,
            registration = RegistrationStatusDomain.NotEvaluated,
        )

        // When
        val result = relyingParty.toRelyingPartyHeaderUi(fallbackName = mockedFallbackName)

        // Then
        assertEquals(mockedFallbackName, result.relyingParty.name)
    }

    //endregion

    //region toRegistrationWarningUi

    @Test
    fun `Given a verified registration with no overasked claims, When toRegistrationWarningUi is called, Then no warning is returned`() {
        // Given
        val relyingParty = buildRelyingPartyDomain(
            name = mockedVerifierName,
            uniqueId = mockedDetails.uniqueId,
            hasTrustedAccessCertificate = true,
            registration = RegistrationStatusDomain.Verified(
                details = mockedDetails,
                overaskedClaims = emptyList(),
            ),
        )

        // When
        val result = relyingParty.toRegistrationWarningUi()

        // Then
        assertNull(result)
    }

    @Test
    fun `Given a verified registration with overasked claims, When toRegistrationWarningUi is called, Then an unaccepted overasked warning is returned`() {
        // Given
        val relyingParty = buildRelyingPartyDomain(
            name = mockedVerifierName,
            uniqueId = mockedDetails.uniqueId,
            hasTrustedAccessCertificate = true,
            registration = RegistrationStatusDomain.Verified(
                details = mockedDetails,
                overaskedClaims = listOf(mockedOveraskedClaim),
            ),
        )

        // When
        val result = relyingParty.toRegistrationWarningUi()

        // Then
        assertEquals(
            RegistrationWarningUi(
                variant = RegistrationWarningVariantUi.OVERASKED,
                riskAccepted = false,
            ),
            result,
        )
    }

    @Test
    fun `Given a not verified registration, When toRegistrationWarningUi is called, Then an unaccepted not verified warning is returned`() {
        // Given
        val relyingParty = buildRelyingPartyDomain(
            name = mockedVerifierName,
            uniqueId = null,
            hasTrustedAccessCertificate = true,
            registration = RegistrationStatusDomain.NotVerified(
                reason = RegistrationFailureReasonDomain.CERTIFICATE_ABSENT,
                details = null,
            ),
        )

        // When
        val result = relyingParty.toRegistrationWarningUi()

        // Then
        assertEquals(
            RegistrationWarningUi(
                variant = RegistrationWarningVariantUi.NOT_VERIFIED,
                riskAccepted = false,
            ),
            result,
        )
    }

    @Test
    fun `Given an unevaluated registration, When toRegistrationWarningUi is called, Then no warning is returned`() {
        // Given
        val relyingParty = buildRelyingPartyDomain(
            name = mockedVerifierName,
            uniqueId = null,
            hasTrustedAccessCertificate = true,
            registration = RegistrationStatusDomain.NotEvaluated,
        )

        // When
        val result = relyingParty.toRegistrationWarningUi()

        // Then
        assertNull(result)
    }

    //endregion

    //region helper functions

    private fun buildRelyingPartyDomain(
        name: String?,
        uniqueId: String?,
        hasTrustedAccessCertificate: Boolean,
        registration: RegistrationStatusDomain,
    ): RelyingPartyDomain = RelyingPartyDomain(
        name = name,
        uniqueId = uniqueId,
        hasTrustedAccessCertificate = hasTrustedAccessCertificate,
        logoUri = mockedRequesterLogoUri,
        registration = registration,
    )

    //endregion
}