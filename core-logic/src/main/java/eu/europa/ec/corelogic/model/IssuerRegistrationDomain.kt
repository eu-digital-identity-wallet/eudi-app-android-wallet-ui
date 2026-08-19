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

/**
 * Outcome of the registration-certificate evaluation for the provider behind an issuance.
 */
sealed interface IssuerRegistrationDomain {

    /** Registration authenticated and evaluated, and the offer is within the registered scope. */
    data class Verified(val details: RegistrationDetailsDomain) : IssuerRegistrationDomain

    /**
     * Issuance must be refused, not just warned about: the registration is valid but the offer
     * exceeds its registered scope.
     */
    data class Blocked(
        val reason: BlockedReasonDomain,
        val details: RegistrationDetailsDomain,
    ) : IssuerRegistrationDomain

    /**
     * Validation failed. [details] carries whatever was parsed before the failing check — shown
     * to the user, but never with the verified badge; null when nothing could be parsed.
     */
    data class NotVerified(
        val reason: RegistrationFailureReasonDomain,
        val details: RegistrationDetailsDomain?,
    ) : IssuerRegistrationDomain

    /**
     * The registration was not evaluated at all: policy disabled, unsigned metadata, or no
     * certificate carried in the metadata.
     */
    data object NotEvaluated : IssuerRegistrationDomain

    /**
     * Why an issuance is refused.
     */
    enum class BlockedReasonDomain {
        ATTESTATION_NOT_REGISTERED,
    }
}

/**
 * The one rule deciding whether a registration outcome refuses an issuance — the offer resolve
 * gate and the pre-flights of the flows with no approval screen consult this and nothing else.
 *
 * Anything short of [IssuerRegistrationDomain.Verified] refuses: over-providing and a failed
 * validation, and equally an outcome that was never established — a provider whose registration
 * cannot be confirmed does not issue.
 */
val IssuerRegistrationDomain.isBlockedForIssuance: Boolean
    get() = when (this) {
        is IssuerRegistrationDomain.Verified -> false

        is IssuerRegistrationDomain.Blocked,
        is IssuerRegistrationDomain.NotVerified,
        is IssuerRegistrationDomain.NotEvaluated -> true
    }