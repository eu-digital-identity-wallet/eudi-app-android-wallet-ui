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

import eu.europa.ec.businesslogic.extension.getLocalizedValue
import eu.europa.ec.businesslogic.util.LocaleUtils
import eu.europa.ec.corelogic.model.ClaimPathDomain
import eu.europa.ec.corelogic.model.ClaimPathDomain.Companion.toClaimPathDomain
import eu.europa.ec.corelogic.model.ClaimPathSegment
import eu.europa.ec.corelogic.model.ClaimType
import eu.europa.ec.corelogic.model.IssuerRegistrationDomain
import eu.europa.ec.corelogic.model.OveraskedClaimDomain
import eu.europa.ec.corelogic.model.RegistrationDetailsDomain
import eu.europa.ec.corelogic.model.RegistrationFailureReasonDomain
import eu.europa.ec.corelogic.model.RegistrationStatusDomain
import eu.europa.ec.eudi.iso18013.transfer.response.WrpRegistrationInfo
import eu.europa.ec.eudi.wallet.registration.ClaimPathElement
import eu.europa.ec.eudi.wallet.registration.LocalizedText
import eu.europa.ec.eudi.wallet.registration.OverAskedClaim
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason
import java.util.Locale

private const val FORMAT_MSO_MDOC = "mso_mdoc"
private const val FORMAT_SD_JWT_VC = "dc+sd-jwt"

/**
 * Converts the Wallet Core registration evaluation attached to a presentation request into the
 * domain outcome — the single place absorbing Wallet Core WRPRC API changes. A request that
 * carried no evaluation at all maps to [RegistrationStatusDomain.NotEvaluated].
 */
fun WrpRegistrationInfo?.toRegistrationStatusDomain(locale: Locale): RegistrationStatusDomain {
    return when (this) {
        is RegistrationCertificateResult.Verified -> RegistrationStatusDomain.Verified(
            details = registration.toRegistrationDetailsDomain(locale = locale),
            overaskedClaims = overAskedClaims.mapNotNull { overAskedClaim ->
                overAskedClaim.toOveraskedClaimDomainOrNull()
            },
        )

        is RegistrationCertificateResult.Failed -> RegistrationStatusDomain.NotVerified(
            reason = reason.toRegistrationFailureReasonDomain(),
            details = registration?.toRegistrationDetailsDomain(locale = locale),
        )

        // null: nothing was evaluated that the app can show
        else -> RegistrationStatusDomain.NotEvaluated
    }
}

/**
 * Converts the Wallet Core registration evaluation of a credential issuer into the domain
 * outcome. An offer that carried no evaluation maps to [IssuerRegistrationDomain.NotEvaluated],
 * and a verified registration whose offer exceeds the registered scope is a hard stop
 * ([IssuerRegistrationDomain.Blocked]) rather than a warning.
 */
fun RegistrationCertificateResult?.toIssuerRegistrationDomain(
    locale: Locale,
): IssuerRegistrationDomain {
    return when (this) {
        is RegistrationCertificateResult.Verified -> {
            val details = registration.toRegistrationDetailsDomain(locale = locale)
            if (overProvidedAttestations.isEmpty()) {
                IssuerRegistrationDomain.Verified(details = details)
            } else {
                IssuerRegistrationDomain.Blocked(
                    reason = IssuerRegistrationDomain.BlockedReasonDomain.ATTESTATION_NOT_REGISTERED,
                    details = details,
                )
            }
        }

        is RegistrationCertificateResult.Failed -> IssuerRegistrationDomain.NotVerified(
            reason = reason.toRegistrationFailureReasonDomain(),
            details = registration?.toRegistrationDetailsDomain(locale = locale),
        )

        null -> IssuerRegistrationDomain.NotEvaluated
    }
}

private fun RegistrationCertificate.toRegistrationDetailsDomain(
    locale: Locale,
): RegistrationDetailsDomain {
    val naturalPersonName = listOfNotNull(givenName, familyName)
        .joinToString(separator = " ")
        .ifEmpty { null }
    val subjectName = name ?: legalName ?: naturalPersonName

    return RegistrationDetailsDomain(
        tradeName = subjectName,
        uniqueId = identifiers.firstOrNull()?.value,
        logoUri = null,
        intendedUse = purpose.localizedValueOrNull(locale = locale),
        privacyPolicyUrl = privacyPolicyUri,
        serviceDescription = serviceDescription.localizedValueOrNull(locale = locale),
    )
}

/**
 * An overasked claim re-keyed for row marking, or null when its path cannot be typed (unknown
 * format, or an mdoc path that is not the `[namespace, element]` pair) — a mis-typed path would
 * mark the wrong rows, so untypeable claims are dropped instead.
 */
private fun OverAskedClaim.toOveraskedClaimDomainOrNull(): OveraskedClaimDomain? {
    val claimPath = when (format) {
        FORMAT_MSO_MDOC -> path.toMsoMdocClaimPathOrNull()
        FORMAT_SD_JWT_VC -> path.toSdJwtVcClaimPathOrNull()
        else -> null
    } ?: return null

    return OveraskedClaimDomain(
        path = claimPath,
        attestationTypes = buildSet {
            meta?.doctypeValue?.let { doctypeValue -> add(doctypeValue) }
            meta?.vctValues?.let { vctValues -> addAll(vctValues) }
        },
    )
}

/** An mdoc claim is addressed by a namespace and an element name; an index or wildcard never is. */
private fun List<ClaimPathElement>.toMsoMdocClaimPathOrNull(): ClaimPathDomain? {
    if (size != 2) return null
    val namespace = (first() as? ClaimPathElement.Claim)?.name ?: return null
    val element = (last() as? ClaimPathElement.Claim)?.name ?: return null

    return ClaimPathDomain.ofPlainKeys(
        names = listOf(element),
        type = ClaimType.MsoMdoc(namespace = namespace),
    )
}

private fun List<ClaimPathElement>.toSdJwtVcClaimPathOrNull(): ClaimPathDomain? {
    if (isEmpty()) return null
    val segments = map { element -> element.toClaimPathSegment() }

    return segments.toClaimPathDomain(type = ClaimType.SdJwtVc)
}

private fun ClaimPathElement.toClaimPathSegment(): ClaimPathSegment {
    return when (this) {
        is ClaimPathElement.Claim -> ClaimPathSegment.Key(name = name)
        is ClaimPathElement.ArrayElement -> ClaimPathSegment.Index(index = index)
        is ClaimPathElement.AllArrayElements -> ClaimPathSegment.AllElements
    }
}

private fun RegistrationFailureReason.toRegistrationFailureReasonDomain(): RegistrationFailureReasonDomain {
    return when (this) {
        RegistrationFailureReason.CERTIFICATE_ABSENT -> RegistrationFailureReasonDomain.CERTIFICATE_ABSENT
        RegistrationFailureReason.MALFORMED -> RegistrationFailureReasonDomain.MALFORMED
        RegistrationFailureReason.STATUS_MISSING -> RegistrationFailureReasonDomain.STATUS_MISSING
        RegistrationFailureReason.SIGNATURE_INVALID -> RegistrationFailureReasonDomain.SIGNATURE_INVALID
        RegistrationFailureReason.UNTRUSTED_PROVIDER -> RegistrationFailureReasonDomain.UNTRUSTED_PROVIDER
        RegistrationFailureReason.EXPIRED -> RegistrationFailureReasonDomain.EXPIRED
        RegistrationFailureReason.REVOKED -> RegistrationFailureReasonDomain.REVOKED
        RegistrationFailureReason.REVOCATION_STATUS_UNKNOWN -> RegistrationFailureReasonDomain.REVOCATION_STATUS_UNKNOWN
        RegistrationFailureReason.NOT_BOUND_TO_REQUESTER -> RegistrationFailureReasonDomain.NOT_BOUND_TO_REQUESTER
    }
}

/**
 * The [locale]-appropriate text of one of the certificate's multi-language fields, or null when it
 * carries none. Certificates express the language as a tag, so it is parsed into the [Locale] the
 * shared resolution expects.
 */
private fun List<LocalizedText>.localizedValueOrNull(locale: Locale): String? {
    return getLocalizedValue(
        userLocale = locale,
        localeExtractor = { localizedText -> LocaleUtils.getLocaleFromSelectedLanguage(localizedText.language) },
        valueExtractor = { localizedText -> localizedText.value },
        fallback = null,
    )
}