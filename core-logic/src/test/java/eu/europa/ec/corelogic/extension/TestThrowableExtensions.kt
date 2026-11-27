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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException

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
    fun `Given missing signed metadata, When toUntrustedIssuerReasonOrNull is called, Then an access-certificate refusal is returned`() {
        // Given
        val failure = CredentialIssuerMetadataError.MissingSignedMetadata()

        // When
        val reason = failure.toUntrustedIssuerReasonOrNull()

        // Then
        assertEquals(UntrustedIssuerReasonDomain.ACCESS_CERTIFICATE, reason)
    }

    @Test
    fun `Given invalid signed metadata, When toUntrustedIssuerReasonOrNull is called, Then an access-certificate refusal is returned`() {
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
    fun `Given a registration policy failure, When toUntrustedIssuerReasonOrNull is called, Then a registration-certificate refusal is returned`() {
        // Given
        val failure = AuthorizationPolicyValidationError.MissingIssuerInfo()

        // When
        val reason = failure.toUntrustedIssuerReasonOrNull()

        // Then
        assertEquals(UntrustedIssuerReasonDomain.REGISTRATION_CERTIFICATE, reason)
    }

    @Test
    fun `Given a missing registration certificate, When toUntrustedIssuerReasonOrNull is called, Then a registration-certificate refusal is returned`() {
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
    fun `Given a refusal behind an offer request error, When toUntrustedIssuerReasonOrNull is called, Then it is found through the metadata reason`() {
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
    fun `Given an offer request error unrelated to trust, When toUntrustedIssuerReasonOrNull is called, Then no refusal is returned`() {
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
    fun `Given an unrelated failure, When toUntrustedIssuerReasonOrNull is called, Then no refusal is returned`() {
        // Given
        val failure = RuntimeException("no network")

        // When
        val reason = failure.toUntrustedIssuerReasonOrNull()

        // Then
        assertNull(reason)
    }

    //endregion

    /** A trust check that produced no result (an unreachable trust list) is asked again. */
    @Test
    fun `Given a refusal without a cause, When isDeferredTrustVerdict is called, Then false`() {
        // Given
        val failure = IssuerNotTrustedException(message = "no trust result")

        // When
        val isVerdict = failure.isDeferredTrustVerdict()

        // Then
        assertFalse(isVerdict)
    }

    @Test
    fun `Given a refusal with a chain cause, When isDeferredTrustVerdict is called, Then true`() {
        // Given
        val failure = mockedIssuerNotTrustedException

        // When
        val isVerdict = failure.isDeferredTrustVerdict()

        // Then
        assertTrue(isVerdict)
    }

    /**
     * 1. The issuer refusal is caused by a certificate that is not valid yet.
     *
     * Expected result:
     * false, because a wrong device clock can cause it.
     */
    @Test
    fun `Given a not-yet-valid certificate, When isDeferredTrustVerdict is called, Then false`() {
        // Given
        val failure = issuerRefusal(CertificateNotYetValidException("not yet valid"))

        // When
        val isVerdict = failure.isDeferredTrustVerdict()

        // Then
        assertFalse(isVerdict)
    }

    @Test
    fun `Given a nested expired certificate, When isDeferredTrustVerdict is called, Then false`() {
        // Given
        val failure = issuerRefusal(
            RuntimeException("chain", CertificateExpiredException("expired"))
        )

        // When
        val isVerdict = failure.isDeferredTrustVerdict()

        // Then
        assertFalse(isVerdict)
    }

    @Test
    fun `Given a path refused as expired, When isDeferredTrustVerdict is called, Then false`() {
        // Given
        val failure = issuerRefusal(
            pathValidationError(CertPathValidatorException.BasicReason.EXPIRED)
        )

        // When
        val isVerdict = failure.isDeferredTrustVerdict()

        // Then
        assertFalse(isVerdict)
    }

    @Test
    fun `Given a path not yet valid, When isDeferredTrustVerdict is called, Then false`() {
        // Given
        val failure = issuerRefusal(
            pathValidationError(CertPathValidatorException.BasicReason.NOT_YET_VALID)
        )

        // When
        val isVerdict = failure.isDeferredTrustVerdict()

        // Then
        assertFalse(isVerdict)
    }

    @Test
    fun `Given a path refused as revoked, When isDeferredTrustVerdict is called, Then true`() {
        // Given
        val failure = issuerRefusal(
            pathValidationError(CertPathValidatorException.BasicReason.REVOKED)
        )

        // When
        val isVerdict = failure.isDeferredTrustVerdict()

        // Then
        assertTrue(isVerdict)
    }

    @Test
    fun `Given a registration policy failure, When isDeferredTrustVerdict is called, Then true`() {
        // Given
        val failure = AuthorizationPolicyValidationError.MissingIssuerInfo()

        // When
        val isVerdict = failure.isDeferredTrustVerdict()

        // Then
        assertTrue(isVerdict)
    }

    /** Signed metadata that cannot be verified is asked again. */
    @Test
    fun `Given invalid signed metadata, When isDeferredTrustVerdict is called, Then false`() {
        // Given
        val failure = CredentialIssuerMetadataError.InvalidSignedMetadata(
            cause = RuntimeException("bad signature")
        )

        // When
        val isVerdict = failure.isDeferredTrustVerdict()

        // Then
        assertFalse(isVerdict)
    }

    /**
     * 1. A refusal with a chain cause and a registration policy failure, each wrapped twice.
     * 2. A refusal without a cause, wrapped twice.
     *
     * Expected result:
     * The same results as unwrapped: true, true and false.
     */
    @Test
    fun `Given wrapped failures, When isDeferredTrustVerdict is called, Then each is unchanged`() {
        // Given
        val chainRefusal = wrapped(mockedIssuerNotTrustedException)
        val policyFailure = wrapped(AuthorizationPolicyValidationError.MissingIssuerInfo())
        val noResult = wrapped(IssuerNotTrustedException(message = "no trust result"))

        // When
        val results = listOf(chainRefusal, policyFailure, noResult)
            .map { it.isDeferredTrustVerdict() }

        // Then
        assertEquals(listOf(true, true, false), results)
    }

    @Test
    fun `Given an unrelated failure, When isDeferredTrustVerdict is called, Then false`() {
        // Given
        val failure = RuntimeException("no network")

        // When
        val isVerdict = failure.isDeferredTrustVerdict()

        // Then
        assertFalse(isVerdict)
    }

    @Test
    fun `Given invalid_transaction_id, When isUnknownDeferredTransaction is called, Then true`() {
        // Given
        val failure = IllegalStateException("invalid_transaction_id")

        // When
        val isUnknown = failure.isUnknownDeferredTransaction()

        // Then
        assertTrue(isUnknown)
    }

    @Test
    fun `Given invalid_token, When isUnknownDeferredTransaction is called, Then false`() {
        // Given
        val failure = IllegalStateException("invalid_token")

        // When
        val isUnknown = failure.isUnknownDeferredTransaction()

        // Then
        assertFalse(isUnknown)
    }

    @Test
    fun `Given another type, When isUnknownDeferredTransaction is called, Then false`() {
        // Given
        val failure = RuntimeException("invalid_transaction_id")

        // When
        val isUnknown = failure.isUnknownDeferredTransaction()

        // Then
        assertFalse(isUnknown)
    }

    @Test
    fun `Given a wrapped invalid_transaction_id, When isUnknownDeferredTransaction is called, Then true`() {
        // Given
        val failure = RuntimeException(
            "deferred request failed",
            IllegalStateException("invalid_transaction_id"),
        )

        // When
        val isUnknown = failure.isUnknownDeferredTransaction()

        // Then
        assertTrue(isUnknown)
    }

    //region Mocked objects needed for tests.

    private val mockedIssuerNotTrustedException = IssuerNotTrustedException(
        message = "Issuer certificate chain is not trusted",
        cause = RuntimeException("untrusted chain"),
    )

    private fun issuerRefusal(cause: Throwable) = IssuerNotTrustedException(
        message = "Issuer certificate chain is not trusted",
        cause = cause,
    )

    private fun pathValidationError(reason: CertPathValidatorException.BasicReason) =
        CertPathValidatorException("path refused", null, null, -1, reason)

    private fun wrapped(failure: Throwable) =
        RuntimeException("outer", IllegalStateException("inner", failure))

    //endregion
}