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

import eu.europa.ec.corelogic.model.UntrustedIssuerReasonDomain
import eu.europa.ec.eudi.openid4vci.AuthorizationPolicyValidationError
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadataError
import eu.europa.ec.eudi.openid4vci.CredentialOfferRequestError
import eu.europa.ec.eudi.openid4vci.CredentialOfferRequestException
import eu.europa.ec.eudi.wallet.trust.IssuerNotTrustedException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TestThrowableExtensions {

    //region toUntrustedIssuerReasonOrNull

    // Case 1:
    // 1. The failure is an IssuerNotTrustedException caused by an untrusted issuer chain.
    //
    // Case 1 Expected Result:
    // UntrustedIssuerReasonDomain.ACCESS_CERTIFICATE.
    @Test
    fun `Given an untrusted issuer chain, When toUntrustedIssuerReasonOrNull is called, Then an access-certificate refusal is returned`() {
        // Given
        val failure = mockedIssuerNotTrustedException

        // When
        val reason = failure.toUntrustedIssuerReasonOrNull()

        // Then
        assertEquals(UntrustedIssuerReasonDomain.ACCESS_CERTIFICATE, reason)
    }

    @Test
    fun `missing signed metadata is an access-certificate refusal`() {
        // Given
        val failure = CredentialIssuerMetadataError.MissingSignedMetadata()

        // When
        val reason = failure.toUntrustedIssuerReasonOrNull()

        // Then
        assertEquals(UntrustedIssuerReasonDomain.ACCESS_CERTIFICATE, reason)
    }

    @Test
    fun `invalid signed metadata is an access-certificate refusal`() {
        // Given
        val failure = CredentialIssuerMetadataError.InvalidSignedMetadata(
            cause = RuntimeException("bad signature")
        )

        // When
        val reason = failure.toUntrustedIssuerReasonOrNull()

        // Then
        assertEquals(UntrustedIssuerReasonDomain.ACCESS_CERTIFICATE, reason)
    }

    @Test
    fun `a registration policy failure is a registration-certificate refusal`() {
        // Given
        val failure = AuthorizationPolicyValidationError.MissingIssuerInfo()

        // When
        val reason = failure.toUntrustedIssuerReasonOrNull()

        // Then
        assertEquals(UntrustedIssuerReasonDomain.REGISTRATION_CERTIFICATE, reason)
    }

    @Test
    fun `a missing registration certificate is a registration-certificate refusal`() {
        // Given
        val failure = AuthorizationPolicyValidationError.MissingRegistrationCertificate()

        // When
        val reason = failure.toUntrustedIssuerReasonOrNull()

        // Then
        assertEquals(UntrustedIssuerReasonDomain.REGISTRATION_CERTIFICATE, reason)
    }

    // Case 6:
    // 1. An IssuerNotTrustedException is wrapped in an IllegalStateException and a RuntimeException.
    //
    // Case 6 Expected Result:
    // UntrustedIssuerReasonDomain.ACCESS_CERTIFICATE.
    @Test
    fun `Given an issuer refusal wrapped in a cause chain, When toUntrustedIssuerReasonOrNull is called, Then an access-certificate refusal is returned`() {
        // Given
        val failure = RuntimeException(
            "outer",
            IllegalStateException(
                "inner",
                mockedIssuerNotTrustedException,
            ),
        )

        // When
        val reason = failure.toUntrustedIssuerReasonOrNull()

        // Then
        assertEquals(UntrustedIssuerReasonDomain.ACCESS_CERTIFICATE, reason)
    }

    // A credential-offer failure carries its error in a property rather than in `cause`, so the
    // walk has to descend into the issuer-metadata reason to reach the refusal.
    @Test
    fun `a refusal behind an offer request error is found through the metadata reason`() {
        // Given
        val failure = CredentialOfferRequestException(
            error = CredentialOfferRequestError.UnableToResolveCredentialIssuerMetadata(
                reason = CredentialIssuerMetadataError.MissingSignedMetadata(),
            ),
        )

        // When
        val reason = failure.toUntrustedIssuerReasonOrNull()

        // Then
        assertEquals(UntrustedIssuerReasonDomain.ACCESS_CERTIFICATE, reason)
    }

    @Test
    fun `an offer request error unrelated to trust yields no refusal`() {
        // Given
        val failure = CredentialOfferRequestException(
            error = CredentialOfferRequestError.NonParseableCredentialOffer(
                reason = RuntimeException("malformed json"),
            ),
        )

        // When
        val reason = failure.toUntrustedIssuerReasonOrNull()

        // Then
        assertNull(reason)
    }

    @Test
    fun `an unrelated failure yields no refusal`() {
        // Given
        val failure = RuntimeException("no network")

        // When
        val reason = failure.toUntrustedIssuerReasonOrNull()

        // Then
        assertNull(reason)
    }

    //endregion

    //region Mocked objects needed for tests.

    private val mockedIssuerNotTrustedException = IssuerNotTrustedException(
        message = "Issuer certificate chain is not trusted",
        cause = RuntimeException("untrusted chain"),
    )

    //endregion
}