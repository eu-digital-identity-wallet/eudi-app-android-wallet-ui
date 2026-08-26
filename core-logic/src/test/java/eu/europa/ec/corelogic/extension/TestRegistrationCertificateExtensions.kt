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

package eu.europa.ec.corelogic.extension

import eu.europa.ec.corelogic.model.ClaimPathDomain
import eu.europa.ec.corelogic.model.ClaimPathSegment
import eu.europa.ec.corelogic.model.ClaimType
import eu.europa.ec.corelogic.model.IssuerRegistrationDomain
import eu.europa.ec.corelogic.model.RegistrationDetailsDomain
import eu.europa.ec.corelogic.model.RegistrationFailureReasonDomain
import eu.europa.ec.corelogic.model.RegistrationStatusDomain
import eu.europa.ec.eudi.iso18013.transfer.response.WrpRegistrationInfo
import eu.europa.ec.eudi.wallet.registration.ClaimPathElement
import eu.europa.ec.eudi.wallet.registration.CredentialMeta
import eu.europa.ec.eudi.wallet.registration.LocalizedText
import eu.europa.ec.eudi.wallet.registration.OverAskedClaim
import eu.europa.ec.eudi.wallet.registration.OverProvidedAttestation
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason
import eu.europa.ec.eudi.wallet.registration.RegistrationIdentifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class TestRegistrationCertificateExtensions {

    //region toIssuerRegistrationDomain

    @Test
    fun `Given a verified registration covering the offer, When toIssuerRegistrationDomain is called, Then the issuer is verified`() {
        // Given
        val result = RegistrationCertificateResult.Verified(
            registration = mockedCertificate,
        )

        // When
        val domain = result.toIssuerRegistrationDomain(locale = mockedLocale)

        // Then
        assertEquals(IssuerRegistrationDomain.Verified(details = mockedDetails), domain)
    }

    @Test
    fun `Given a verified registration and an over-provided offer, When toIssuerRegistrationDomain is called, Then the issuance is blocked as over-provided`() {
        // Given
        val result = RegistrationCertificateResult.Verified(
            registration = mockedCertificate,
            overProvidedAttestations = listOf(
                OverProvidedAttestation(
                    format = FORMAT_MSO_MDOC,
                    meta = CredentialMeta(doctypeValue = MDL_DOCTYPE),
                ),
            ),
        )

        // When
        val domain = result.toIssuerRegistrationDomain(locale = mockedLocale)

        // Then
        assertEquals(
            IssuerRegistrationDomain.Blocked(
                reason = IssuerRegistrationDomain.BlockedReasonDomain.ATTESTATION_OVER_PROVIDED,
                details = mockedDetails,
            ),
            domain,
        )
    }

    @Test
    fun `Given a missing entitlement carrying the parsed registration, When toIssuerRegistrationDomain is called, Then the issuance is blocked on the entitlement`() {
        // Given
        val result = RegistrationCertificateResult.Failed(
            reason = RegistrationFailureReason.ENTITLEMENT_MISSING,
            registration = mockedCertificate,
        )

        // When
        val domain = result.toIssuerRegistrationDomain(locale = mockedLocale)

        // Then
        assertEquals(
            IssuerRegistrationDomain.Blocked(
                reason = IssuerRegistrationDomain.BlockedReasonDomain.ENTITLEMENT_MISSING,
                details = mockedDetails,
            ),
            domain,
        )
    }

    // Blocked carries non-null details by construction, so a missing entitlement with nothing
    // parsed cannot be promoted. It stays the failure it is.
    @Test
    fun `Given a missing entitlement with nothing parsed, When toIssuerRegistrationDomain is called, Then the outcome is not verified rather than blocked`() {
        // Given
        val result = RegistrationCertificateResult.Failed(
            reason = RegistrationFailureReason.ENTITLEMENT_MISSING,
            registration = null,
        )

        // When
        val domain = result.toIssuerRegistrationDomain(locale = mockedLocale)

        // Then
        assertEquals(
            IssuerRegistrationDomain.NotVerified(
                reason = RegistrationFailureReasonDomain.ENTITLEMENT_MISSING,
                details = null,
            ),
            domain,
        )
    }

    // the guard is on the entitlement reason alone: no other failure is promoted, however much
    // of the registration was parsed before it
    @Test
    fun `Given a validation failure carrying the parsed registration, When toIssuerRegistrationDomain is called, Then the outcome is not verified rather than blocked`() {
        // Given
        val result = RegistrationCertificateResult.Failed(
            reason = RegistrationFailureReason.SIGNATURE_INVALID,
            registration = mockedCertificate,
        )

        // When
        val domain = result.toIssuerRegistrationDomain(locale = mockedLocale)

        // Then
        assertEquals(
            IssuerRegistrationDomain.NotVerified(
                reason = RegistrationFailureReasonDomain.SIGNATURE_INVALID,
                details = mockedDetails,
            ),
            domain,
        )
    }

    @Test
    fun `Given an offer that carried no evaluation, When toIssuerRegistrationDomain is called, Then the registration was not evaluated`() {
        // Given
        val result: RegistrationCertificateResult? = null

        // When
        val domain = result.toIssuerRegistrationDomain(locale = mockedLocale)

        // Then
        assertEquals(IssuerRegistrationDomain.NotEvaluated, domain)
    }

    //endregion

    //region toRegistrationStatusDomain

    @Test
    fun `Given a verified registration asking within its scope, When toRegistrationStatusDomain is called, Then the status is verified with no overasked claims`() {
        // Given
        val info = RegistrationCertificateResult.Verified(
            registration = mockedCertificate,
        )

        // When
        val domain = info.toRegistrationStatusDomain(locale = mockedLocale)

        // Then
        assertEquals(
            RegistrationStatusDomain.Verified(
                details = mockedDetails,
                overaskedClaims = emptyList(),
            ),
            domain,
        )
    }

    @Test
    fun `Given a validation failure carrying the parsed registration, When toRegistrationStatusDomain is called, Then the parsed details are kept`() {
        // Given
        val info = RegistrationCertificateResult.Failed(
            reason = RegistrationFailureReason.REVOKED,
            registration = mockedCertificate,
        )

        // When
        val domain = info.toRegistrationStatusDomain(locale = mockedLocale)

        // Then
        assertEquals(
            RegistrationStatusDomain.NotVerified(
                reason = RegistrationFailureReasonDomain.REVOKED,
                details = mockedDetails,
            ),
            domain,
        )
    }

    @Test
    fun `Given a validation failure with nothing parsed, When toRegistrationStatusDomain is called, Then no details are carried`() {
        // Given
        val info = RegistrationCertificateResult.Failed(
            reason = RegistrationFailureReason.MALFORMED,
            registration = null,
        )

        // When
        val domain = info.toRegistrationStatusDomain(locale = mockedLocale)

        // Then
        assertEquals(
            RegistrationStatusDomain.NotVerified(
                reason = RegistrationFailureReasonDomain.MALFORMED,
                details = null,
            ),
            domain,
        )
    }

    @Test
    fun `Given a request that carried no evaluation, When toRegistrationStatusDomain is called, Then the registration was not evaluated`() {
        // Given
        val info: WrpRegistrationInfo? = null

        // When
        val domain = info.toRegistrationStatusDomain(locale = mockedLocale)

        // Then
        assertEquals(RegistrationStatusDomain.NotEvaluated, domain)
    }

    // Wallet Core can add other WrpRegistrationInfo types. An evaluation the app does not
    // recognise must never be read as a verified one.
    @Test
    fun `Given an evaluation of an unrecognised shape, When toRegistrationStatusDomain is called, Then the registration was not evaluated`() {
        // Given
        val info = object : WrpRegistrationInfo {}

        // When
        val domain = info.toRegistrationStatusDomain(locale = mockedLocale)

        // Then
        assertEquals(RegistrationStatusDomain.NotEvaluated, domain)
    }

    //endregion

    //region toRegistrationStatusDomain overasked claim typing

    @Test
    fun `Given an overasked mdoc claim, When toRegistrationStatusDomain is called, Then the path carries the namespace and the element`() {
        // Given
        val info = verifiedWithOverAsked(
            OverAskedClaim(
                format = FORMAT_MSO_MDOC,
                path = listOf(
                    ClaimPathElement.Claim(name = MDL_NAMESPACE),
                    ClaimPathElement.Claim(name = "family_name"),
                ),
            )
        )

        // When
        val claims = info.overaskedClaimsOf(locale = mockedLocale)

        // Then
        assertEquals(
            listOf(
                ClaimPathDomain.ofPlainKeys(
                    names = listOf("family_name"),
                    type = ClaimType.MsoMdoc(namespace = MDL_NAMESPACE),
                )
            ),
            claims.map { claim -> claim.path },
        )
    }

    @Test
    fun `Given an overasked nested SD-JWT claim, When toRegistrationStatusDomain is called, Then every key of the path is kept in order`() {
        // Given
        val info = verifiedWithOverAsked(
            OverAskedClaim(
                format = FORMAT_SD_JWT_VC,
                path = listOf(
                    ClaimPathElement.Claim(name = "address"),
                    ClaimPathElement.Claim(name = "street_address"),
                ),
            )
        )

        // When
        val claims = info.overaskedClaimsOf(locale = mockedLocale)

        // Then
        assertEquals(
            listOf(
                ClaimPathDomain(
                    segments = listOf(
                        ClaimPathSegment.Key(name = "address"),
                        ClaimPathSegment.Key(name = "street_address"),
                    ),
                    type = ClaimType.SdJwtVc,
                )
            ),
            claims.map { claim -> claim.path },
        )
    }

    @Test
    fun `Given an overasked SD-JWT array element, When toRegistrationStatusDomain is called, Then the index is kept as a typed segment`() {
        // Given
        val info = verifiedWithOverAsked(
            OverAskedClaim(
                format = FORMAT_SD_JWT_VC,
                path = listOf(
                    ClaimPathElement.Claim(name = "nationalities"),
                    ClaimPathElement.ArrayElement(index = 1),
                ),
            )
        )

        // When
        val claims = info.overaskedClaimsOf(locale = mockedLocale)

        // Then
        assertEquals(
            listOf(
                ClaimPathDomain(
                    segments = listOf(
                        ClaimPathSegment.Key(name = "nationalities"),
                        ClaimPathSegment.Index(index = 1),
                    ),
                    type = ClaimType.SdJwtVc,
                )
            ),
            claims.map { claim -> claim.path },
        )
    }

    @Test
    fun `Given an overasked SD-JWT wildcard path, When toRegistrationStatusDomain is called, Then the wildcard is kept as a typed segment`() {
        // Given
        val info = verifiedWithOverAsked(
            OverAskedClaim(
                format = FORMAT_SD_JWT_VC,
                path = listOf(
                    ClaimPathElement.Claim(name = "nationalities"),
                    ClaimPathElement.AllArrayElements,
                ),
            )
        )

        // When
        val claims = info.overaskedClaimsOf(locale = mockedLocale)

        // Then
        assertEquals(
            listOf(
                ClaimPathDomain(
                    segments = listOf(
                        ClaimPathSegment.Key(name = "nationalities"),
                        ClaimPathSegment.AllElements,
                    ),
                    type = ClaimType.SdJwtVc,
                )
            ),
            claims.map { claim -> claim.path },
        )
    }

    // a mis-typed path would mark the wrong rows, so an untypeable claim is dropped rather than
    // guessed at. the request still shows as verified, just without that row marked.
    @Test
    fun `Given an overasked claim of an unknown format, When toRegistrationStatusDomain is called, Then the claim is dropped`() {
        // Given
        val info = verifiedWithOverAsked(
            OverAskedClaim(
                format = "unknown_format",
                path = listOf(ClaimPathElement.Claim(name = "family_name")),
            )
        )

        // When
        val claims = info.overaskedClaimsOf(locale = mockedLocale)

        // Then
        assertTrue(claims.isEmpty())
    }

    @Test
    fun `Given an overasked mdoc claim that is not a namespace and element pair, When toRegistrationStatusDomain is called, Then the claim is dropped`() {
        // Given
        val info = verifiedWithOverAsked(
            OverAskedClaim(
                format = FORMAT_MSO_MDOC,
                path = listOf(ClaimPathElement.Claim(name = "family_name")),
            )
        )

        // When
        val claims = info.overaskedClaimsOf(locale = mockedLocale)

        // Then
        assertTrue(claims.isEmpty())
    }

    @Test
    fun `Given an overasked mdoc claim addressed by an index, When toRegistrationStatusDomain is called, Then the claim is dropped`() {
        // Given
        val info = verifiedWithOverAsked(
            OverAskedClaim(
                format = FORMAT_MSO_MDOC,
                path = listOf(
                    ClaimPathElement.Claim(name = MDL_NAMESPACE),
                    ClaimPathElement.ArrayElement(index = 0),
                ),
            )
        )

        // When
        val claims = info.overaskedClaimsOf(locale = mockedLocale)

        // Then
        assertTrue(claims.isEmpty())
    }

    @Test
    fun `Given an overasked SD-JWT claim with an empty path, When toRegistrationStatusDomain is called, Then the claim is dropped`() {
        // Given
        val info = verifiedWithOverAsked(
            OverAskedClaim(
                format = FORMAT_SD_JWT_VC,
                path = emptyList(),
            )
        )

        // When
        val claims = info.overaskedClaimsOf(locale = mockedLocale)

        // Then
        assertTrue(claims.isEmpty())
    }

    @Test
    fun `Given a mix of typeable and untypeable overasked claims, When toRegistrationStatusDomain is called, Then only the untypeable one is dropped`() {
        // Given
        val info = verifiedWithOverAsked(
            OverAskedClaim(
                format = "unknown_format",
                path = listOf(ClaimPathElement.Claim(name = "family_name")),
            ),
            OverAskedClaim(
                format = FORMAT_SD_JWT_VC,
                path = listOf(ClaimPathElement.Claim(name = "birth_date")),
            ),
        )

        // When
        val claims = info.overaskedClaimsOf(locale = mockedLocale)

        // Then
        assertEquals(
            listOf(
                ClaimPathDomain.ofPlainKeys(
                    names = listOf("birth_date"),
                    type = ClaimType.SdJwtVc,
                )
            ),
            claims.map { claim -> claim.path },
        )
    }

    @Test
    fun `Given an overasked claim naming both a doctype and vcts, When toRegistrationStatusDomain is called, Then every attestation type is kept`() {
        // Given
        val info = verifiedWithOverAsked(
            OverAskedClaim(
                format = FORMAT_SD_JWT_VC,
                meta = CredentialMeta(
                    vctValues = listOf(PID_VCT, EHIC_VCT),
                    doctypeValue = MDL_DOCTYPE,
                ),
                path = listOf(ClaimPathElement.Claim(name = "family_name")),
            )
        )

        // When
        val claims = info.overaskedClaimsOf(locale = mockedLocale)

        // Then
        assertEquals(
            setOf(MDL_DOCTYPE, PID_VCT, EHIC_VCT),
            claims.single().attestationTypes,
        )
    }

    // an empty set means the claim was overasked of any attestation of its format
    @Test
    fun `Given an overasked claim naming no attestation type, When toRegistrationStatusDomain is called, Then no attestation type is kept`() {
        // Given
        val info = verifiedWithOverAsked(
            OverAskedClaim(
                format = FORMAT_SD_JWT_VC,
                meta = null,
                path = listOf(ClaimPathElement.Claim(name = "family_name")),
            )
        )

        // When
        val claims = info.overaskedClaimsOf(locale = mockedLocale)

        // Then
        assertTrue(claims.single().attestationTypes.isEmpty())
    }

    //endregion

    //region registration details mapping

    @Test
    fun `Given a registration naming the subject, When the details are mapped, Then the name outranks the legal and natural person names`() {
        // Given
        val certificate = RegistrationCertificate(
            name = "NordicBank A/S",
            legalName = "Nordic Bank Aktieselskab",
            givenName = "Astrid",
            familyName = "Lund",
        )

        // When
        val details = detailsOf(certificate)

        // Then
        assertEquals("NordicBank A/S", details.tradeName)
    }

    @Test
    fun `Given a registration with no name, When the details are mapped, Then the legal name outranks the natural person name`() {
        // Given
        val certificate = RegistrationCertificate(
            legalName = "Nordic Bank Aktieselskab",
            givenName = "Astrid",
            familyName = "Lund",
        )

        // When
        val details = detailsOf(certificate)

        // Then
        assertEquals("Nordic Bank Aktieselskab", details.tradeName)
    }

    @Test
    fun `Given a registration of a natural person, When the details are mapped, Then the given and family names are joined`() {
        // Given
        val certificate = RegistrationCertificate(
            givenName = "Astrid",
            familyName = "Lund",
        )

        // When
        val details = detailsOf(certificate)

        // Then
        assertEquals("Astrid Lund", details.tradeName)
    }

    @Test
    fun `Given a registration carrying only a family name, When the details are mapped, Then that name is used on its own`() {
        // Given
        val certificate = RegistrationCertificate(familyName = "Lund")

        // When
        val details = detailsOf(certificate)

        // Then
        assertEquals("Lund", details.tradeName)
    }

    @Test
    fun `Given a registration naming the subject in no way, When the details are mapped, Then no trade name is carried`() {
        // Given
        val certificate = RegistrationCertificate()

        // When
        val details = detailsOf(certificate)

        // Then
        assertNull(details.tradeName)
    }

    @Test
    fun `Given a registration carrying several identifiers, When the details are mapped, Then the first one identifies the subject`() {
        // Given
        val certificate = RegistrationCertificate(
            identifiers = listOf(
                RegistrationIdentifier(type = "LEI", value = "LEIXG-123456789"),
                RegistrationIdentifier(type = "VAT", value = "DK-99999999"),
            ),
        )

        // When
        val details = detailsOf(certificate)

        // Then
        assertEquals("LEIXG-123456789", details.uniqueId)
    }

    @Test
    fun `Given a registration carrying no identifier, When the details are mapped, Then no unique id is carried`() {
        // Given
        val certificate = RegistrationCertificate(identifiers = emptyList())

        // When
        val details = detailsOf(certificate)

        // Then
        assertNull(details.uniqueId)
    }

    // the certificate carries no logo. the one shown next to the name comes from the offer or the
    // request, so the mapping never fills this in.
    @Test
    fun `Given any registration, When the details are mapped, Then no logo is taken from the certificate`() {
        // Given
        val certificate = mockedCertificate

        // When
        val details = detailsOf(certificate)

        // Then
        assertNull(details.logoUri)
    }

    @Test
    fun `Given a registration carrying the privacy policy, When the details are mapped, Then the url is kept as it stands`() {
        // Given
        val certificate = RegistrationCertificate(
            privacyPolicyUri = "https://nordicbank.example/privacy",
        )

        // When
        val details = detailsOf(certificate)

        // Then
        assertEquals("https://nordicbank.example/privacy", details.privacyPolicyUrl)
    }

    //endregion

    //region multi-language field resolution

    @Test
    fun `Given a purpose written in the user language, When the details are mapped, Then that language is used`() {
        // Given
        val certificate = RegistrationCertificate(
            purpose = listOf(
                LocalizedText(language = "da", value = "Aldersbekraeftelse"),
                LocalizedText(language = "en", value = "Age verification"),
            ),
        )

        // When
        val details = detailsOf(certificate)

        // Then
        assertEquals("Age verification", details.intendedUse)
    }

    // the shared resolution falls back to the first entry rather than showing nothing
    @Test
    fun `Given a purpose written in no matching language, When the details are mapped, Then the first entry is used`() {
        // Given
        val certificate = RegistrationCertificate(
            purpose = listOf(
                LocalizedText(language = "da", value = "Aldersbekraeftelse"),
                LocalizedText(language = "de", value = "Altersnachweis"),
            ),
        )

        // When
        val details = detailsOf(certificate)

        // Then
        assertEquals("Aldersbekraeftelse", details.intendedUse)
    }

    @Test
    fun `Given a registration carrying no purpose, When the details are mapped, Then no intended use is carried`() {
        // Given
        val certificate = RegistrationCertificate(purpose = emptyList())

        // When
        val details = detailsOf(certificate)

        // Then
        assertNull(details.intendedUse)
    }

    @Test
    fun `Given a service description written in the user language, When the details are mapped, Then that language is used`() {
        // Given
        val certificate = RegistrationCertificate(
            serviceDescription = listOf(
                LocalizedText(language = "da", value = "Bankkonto"),
                LocalizedText(language = "en", value = "Bank account"),
            ),
        )

        // When
        val details = detailsOf(certificate)

        // Then
        assertEquals("Bank account", details.serviceDescription)
    }

    @Test
    fun `Given a registration carrying no service description, When the details are mapped, Then no service description is carried`() {
        // Given
        val certificate = RegistrationCertificate(serviceDescription = emptyList())

        // When
        val details = detailsOf(certificate)

        // Then
        assertNull(details.serviceDescription)
    }

    //endregion

    //region helper functions

    private fun verifiedWithOverAsked(
        vararg overAskedClaims: OverAskedClaim,
    ): RegistrationCertificateResult.Verified {
        return RegistrationCertificateResult.Verified(
            registration = mockedCertificate,
            overAskedClaims = overAskedClaims.toList(),
        )
    }

    private fun WrpRegistrationInfo.overaskedClaimsOf(locale: Locale) =
        (toRegistrationStatusDomain(locale = locale) as RegistrationStatusDomain.Verified)
            .overaskedClaims

    private fun detailsOf(certificate: RegistrationCertificate) =
        (RegistrationCertificateResult.Verified(registration = certificate)
            .toIssuerRegistrationDomain(locale = mockedLocale) as IssuerRegistrationDomain.Verified)
            .details

    //endregion

    //region Mocked objects needed for tests.

    private val mockedLocale = Locale.forLanguageTag("en")

    private val mockedCertificate = RegistrationCertificate(
        identifiers = listOf(
            RegistrationIdentifier(type = "LEI", value = "LEIXG-123456789"),
        ),
        name = "NordicBank A/S",
        privacyPolicyUri = "https://nordicbank.example/privacy",
        purpose = listOf(
            LocalizedText(language = "en", value = "mocked intended use"),
        ),
        serviceDescription = listOf(
            LocalizedText(language = "en", value = "mocked service description"),
        ),
    )

    /** The details [mockedCertificate] maps to. */
    private val mockedDetails = RegistrationDetailsDomain(
        tradeName = "NordicBank A/S",
        uniqueId = "LEIXG-123456789",
        logoUri = null,
        intendedUse = "mocked intended use",
        privacyPolicyUrl = "https://nordicbank.example/privacy",
        serviceDescription = "mocked service description",
    )

    //endregion

    private companion object {
        const val FORMAT_MSO_MDOC = "mso_mdoc"
        const val FORMAT_SD_JWT_VC = "dc+sd-jwt"

        const val MDL_DOCTYPE = "org.iso.18013.5.1.mDL"
        const val MDL_NAMESPACE = "org.iso.18013.5.1"
        const val PID_VCT = "urn:eudi:pid:1"
        const val EHIC_VCT = "urn:eu.europa.ec.eudi:ehic:1"
    }
}