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
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException

/**
 * Which trust layer refused the issuer behind this failure, or null when the failure is not about
 * trust and the caller should surface it as an ordinary error. An untrusted issuer chain or
 * missing/invalid signed metadata reads as [UntrustedIssuerReasonDomain.ACCESS_CERTIFICATE]; an
 * absent registration certificate, or one that fails policy validation, reads as
 * [UntrustedIssuerReasonDomain.REGISTRATION_CERTIFICATE].
 */
fun Throwable.toUntrustedIssuerReasonOrNull(): UntrustedIssuerReasonDomain? {
    var throwable: Throwable? = this
    while (throwable != null) {
        if (throwable is IssuerNotTrustedException ||
            throwable is CredentialIssuerMetadataError.InvalidSignedMetadata ||
            throwable is CredentialIssuerMetadataError.MissingSignedMetadata
        ) {
            return UntrustedIssuerReasonDomain.ACCESS_CERTIFICATE
        }

        if (throwable is AuthorizationPolicyValidationError) {
            return UntrustedIssuerReasonDomain.REGISTRATION_CERTIFICATE
        }

        // CredentialOfferRequestException carries its error in a property, not in `cause`
        val offerError = (throwable as? CredentialOfferRequestException)?.error
        throwable =
            if (offerError is CredentialOfferRequestError.UnableToResolveCredentialIssuerMetadata) {
                offerError.reason
            } else {
                throwable.cause
            }
    }
    return null
}

/**
 * Whether a failed deferred request carries a definite trust verdict, which stops the requests: a
 * registration that fails the issuance policy, or an issuer chain that was evaluated and found
 * untrusted for a reason other than its validity period. A trust check that could not run (no
 * result, for example an unreachable trust list), a chain refused for its validity period (a wrong
 * device clock) and signed metadata that cannot be verified are asked again.
 */
fun Throwable.isDeferredTrustVerdict(): Boolean {
    var throwable: Throwable? = this
    while (throwable != null) {
        if (throwable is AuthorizationPolicyValidationError) return true
        if (throwable is IssuerNotTrustedException) {
            val reason = throwable.cause ?: return false
            return !reason.isCertificateValidityError()
        }
        throwable = throwable.cause
    }
    return false
}

/**
 * Whether the issuer answered a deferred request with `invalid_transaction_id`: the transaction
 * was never issued or was already used, so asking again cannot succeed.
 */
fun Throwable.isUnknownDeferredTransaction(): Boolean =
    this is IllegalStateException && message == INVALID_TRANSACTION_ID

private const val INVALID_TRANSACTION_ID = "invalid_transaction_id"

private fun Throwable.isCertificateValidityError(): Boolean {
    var throwable: Throwable? = this
    while (throwable != null) {
        if (throwable is CertificateExpiredException ||
            throwable is CertificateNotYetValidException
        ) return true
        if (throwable is CertPathValidatorException &&
            (throwable.reason == CertPathValidatorException.BasicReason.EXPIRED ||
                    throwable.reason == CertPathValidatorException.BasicReason.NOT_YET_VALID)
        ) return true
        throwable = throwable.cause
    }
    return false
}
