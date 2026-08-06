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

import java.net.URI

/**
 * Who is asking, as the consent screen shows it. The two trust layers are kept apart:
 * [hasTrustedAccessCertificate] is the access-certificate (WRPAC) verdict, [registration] the
 * evaluation of the registration certificate (WRPRC).
 *
 * @property name the requester's display name, resolved across both layers — see
 * [resolveRequesterName].
 * @property uniqueId the requester's registered identifier — the intermediary's when the request
 * is intermediated, the certificate subject's otherwise.
 * @property hasTrustedAccessCertificate whether the request's access-certificate chain validated
 * against the configured trust source; also false when the request carried no reader
 * authentication at all.
 * @property logoUri the requester's logo, when a source exists (issuance only today).
 */
data class RelyingPartyDomain(
    val name: String?,
    val uniqueId: String?,
    val hasTrustedAccessCertificate: Boolean,
    val logoUri: URI?,
    val registration: RegistrationStatusDomain,
) {

    /**
     * Verified across **both** trust layers: a trusted access certificate and a registration that
     * raised no problem (one never evaluated is judged on the certificate alone). Every screen's
     * verified badge renders from this property.
     */
    val isFullyVerified: Boolean
        get() = hasTrustedAccessCertificate &&
                registration !is RegistrationStatusDomain.NotVerified
}

/** Outcome of the registration-certificate (WRPRC) evaluation for one interaction. */
sealed interface RegistrationStatusDomain {

    /**
     * Registration certificate authenticated and evaluated. [overaskedClaims] is the subset of
     * the request that the registration does not cover — empty when fully covered.
     */
    data class Verified(
        val details: RegistrationDetailsDomain,
        val overaskedClaims: List<OveraskedClaimDomain>,
    ) : RegistrationStatusDomain

    /**
     * Validation failed. [details] carries whatever was parsed before the failing check — shown
     * to the user, but never with the verified badge; null when nothing could be parsed
     * (certificate absent, malformed, unverifiable, or from an untrusted provider).
     */
    data class NotVerified(
        val reason: RegistrationFailureReasonDomain,
        val details: RegistrationDetailsDomain?,
    ) : RegistrationStatusDomain

    /** The registration was not evaluated at all (policy disabled, or no trust source). */
    data object NotEvaluated : RegistrationStatusDomain
}

/** Why a registration certificate failed validation. */
enum class RegistrationFailureReasonDomain {
    CERTIFICATE_ABSENT,
    MALFORMED,
    STATUS_MISSING,
    SIGNATURE_INVALID,
    UNTRUSTED_PROVIDER,
    EXPIRED,
    REVOKED,
    REVOCATION_STATUS_UNKNOWN,
    NOT_BOUND_TO_REQUESTER,
}

/**
 * Registration-certificate content the app displays. The certificate subject is the relying
 * party itself; when [intermediary] is present the request is presented on the subject's behalf
 * and the subject renders in the "on behalf of" block.
 */
data class RegistrationDetailsDomain(
    val tradeName: String?,
    val uniqueId: String?,
    val logoUri: URI?,
    val intendedUse: String?,
    val privacyPolicyUrl: String?,
    val serviceDescription: String?,
    val intermediary: RegistrationIntermediaryDomain?,
)

/** The party presenting the request on the registered relying party's behalf. */
data class RegistrationIntermediaryDomain(
    val uniqueId: String,
    val name: String?,
)

/**
 * A requested claim outside the registered scope, keyed the way the registration keys it:
 * attestation type + typed claim path — no query id; every row rendering the claim, in any
 * combination, is marked.
 *
 * @property path also carries the claim's credential format and, for MSO-mdoc, its namespace.
 * @property attestationTypes the doctypes/vcts the claim was requested from; empty means any
 * attestation of the path's format.
 */
data class OveraskedClaimDomain(
    val path: ClaimPathDomain,
    val attestationTypes: Set<FormatType>,
) {

    /** Whether this overasked claim concerns a document of [formatType]. */
    fun appliesTo(formatType: FormatType): Boolean {
        return attestationTypes.isEmpty() || attestationTypes.any { attestationType ->
            attestationType.equals(formatType, ignoreCase = true)
        }
    }
}

/** The overasked claims when the registration is verified; empty for every other outcome. */
fun RegistrationStatusDomain.overaskedClaimsOrEmpty(): List<OveraskedClaimDomain> {
    return (this as? RegistrationStatusDomain.Verified)?.overaskedClaims.orEmpty()
}

/**
 * The requester's registered identifier: the intermediary's when the request is intermediated,
 * the certificate subject's otherwise; null when no registration details exist.
 */
fun RegistrationStatusDomain.requesterUniqueIdOrNull(): String? {
    return detailsOrNull()?.let { safeDetails ->
        safeDetails.intermediary?.uniqueId ?: safeDetails.uniqueId
    }
}

/**
 * The requester's name as the registration certificate gives it: the intermediary's when the
 * request is intermediated, the certificate subject's otherwise.
 */
fun RegistrationStatusDomain.requesterNameOrNull(): String? {
    return detailsOrNull()?.let { safeDetails ->
        safeDetails.intermediary?.name ?: safeDetails.tradeName
    }
}

/**
 * The name to show for the requester, resolved across both trust layers.
 *
 * A verified registration outranks [accessCertificateName]: it is the registrar-attested
 * user-friendly name, and the evaluation proved the certificate belongs to the party that signed
 * this request — whereas the access-certificate name is a legal name or a bare certificate CN.
 * An unverified registration's name is only a last resort, and never carries the verified badge.
 */
fun RegistrationStatusDomain.resolveRequesterName(accessCertificateName: String?): String? {
    return when (this) {
        is RegistrationStatusDomain.Verified -> requesterNameOrNull() ?: accessCertificateName
        is RegistrationStatusDomain.NotVerified -> accessCertificateName ?: requesterNameOrNull()
        is RegistrationStatusDomain.NotEvaluated -> accessCertificateName
    }
}

/** The registration details when any were parsed, whatever the outcome. */
private fun RegistrationStatusDomain.detailsOrNull(): RegistrationDetailsDomain? {
    return when (this) {
        is RegistrationStatusDomain.Verified -> details
        is RegistrationStatusDomain.NotVerified -> details
        is RegistrationStatusDomain.NotEvaluated -> null
    }
}