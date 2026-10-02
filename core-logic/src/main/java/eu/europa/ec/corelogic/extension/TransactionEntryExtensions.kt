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
import eu.europa.ec.businesslogic.util.toLocalDateTime
import eu.europa.ec.corelogic.model.ClaimPathSegment
import eu.europa.ec.corelogic.model.ClaimRefDomain
import eu.europa.ec.corelogic.model.CredentialClaimsDomain
import eu.europa.ec.corelogic.model.CredentialRefDomain
import eu.europa.ec.corelogic.model.DpaContactDomain
import eu.europa.ec.corelogic.model.InteractingPartyDomain
import eu.europa.ec.corelogic.model.IssuanceDetailsDomain
import eu.europa.ec.corelogic.model.LocalizedTextDomain
import eu.europa.ec.corelogic.model.PresentationRegistrationDomain
import eu.europa.ec.corelogic.model.QualifiedIdentifierDomain
import eu.europa.ec.corelogic.model.TransactionLogDomain
import eu.europa.ec.corelogic.model.TransactionResultDomain
import eu.europa.ec.eudi.wallet.registration.QualifiedIdentifier
import eu.europa.ec.eudi.wallet.transactionLogging.model.ClaimInfo
import eu.europa.ec.eudi.wallet.transactionLogging.model.ClaimPath
import eu.europa.ec.eudi.wallet.transactionLogging.model.MultiLangString
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionEntry
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionResult
import eu.europa.ec.eudi.wallet.transfer.openId4vp.TransactionDataType
import java.time.Instant
import java.util.Locale

/**
 * Converts a Wallet Core transaction entry into the app's own model — the one place the SDK's
 * transaction types are read.
 *
 * Returns null for unsupported kinds or actions with invalid parent or method metadata.
 * Parent and method come from storage. Credential identifiers and claim paths are copied from
 * the record without consulting wallet documents or their display metadata.
 */
internal fun TransactionEntry.toTransactionLogDomain(
    id: String,
    userLocale: Locale,
    parentPresentationId: String?,
    communicationMethod: String?,
    transactionDataTypes: List<TransactionDataType>,
): TransactionLogDomain? {
    val method = communicationMethod.toCommunicationMethodDomainOrNull()

    return when (this) {
        is TransactionEntry.Presentation -> TransactionLogDomain.Presentation(
            id = id,
            time = time.toLocalDateTime(),
            result = transactionResult.toTransactionResultDomain(),
            party = InteractingPartyDomain(
                name = interactingPartyName?.toLocalizedTextDomain(),
                identifier = interactingPartyIdentifier?.toQualifiedIdentifierDomain(),
                contacts = interactingPartyContact.orEmpty(),
            ),
            partyType = interactingPartyType,
            intermediary = InteractingPartyDomain(
                name = intermediaryName?.toLocalizedTextDomain(),
                identifier = intermediaryIdentifier?.toQualifiedIdentifierDomain(),
                contacts = intermediaryContact.orEmpty(),
            ).takeIf { intermediary ->
                intermediary.name != null || intermediary.identifier != null || intermediary.contacts.isNotEmpty()
            },
            registration = toPresentationRegistrationDomain(userLocale = userLocale),
            claimsRequested = listOfClaimsRequested.toCredentialClaimsDomain(),
            claimsPresented = listOfClaimsPresented.toCredentialClaimsDomain(),
            transactionData = transactionalData
                ?.toPresentationTransactionDataDomains(types = transactionDataTypes)
                .orEmpty(),
        )

        is TransactionEntry.CredentialIssuance -> TransactionLogDomain.CredentialIssuance(
            id = id,
            time = time.toLocalDateTime(),
            result = transactionResult.toTransactionResultDomain(),
            details = details.toIssuanceDetailsDomain(),
        )

        is TransactionEntry.CredentialReissuance -> TransactionLogDomain.CredentialReissuance(
            id = id,
            time = time.toLocalDateTime(),
            result = transactionResult.toTransactionResultDomain(),
            details = details.toIssuanceDetailsDomain(),
        )

        is TransactionEntry.CredentialDeletion -> TransactionLogDomain.CredentialDeletion(
            id = id,
            time = time.toLocalDateTime(),
            result = transactionResult.toTransactionResultDomain(),
            credential = CredentialRefDomain(identifier = credentialIdentifier),
            issuer = InteractingPartyDomain(
                name = credentialIssuerName?.toLocalizedTextDomain(),
                identifier = credentialIssuerIdentifier?.toQualifiedIdentifierDomain(),
                contacts = emptyList(),
            ),
        )

        is TransactionEntry.SigningSealing -> TransactionLogDomain.SigningSealing(
            id = id,
            time = time.toLocalDateTime(),
            result = transactionResult.toTransactionResultDomain(),
            service = InteractingPartyDomain(
                name = interactingPartyName?.toLocalizedTextDomain(),
                identifier = interactingPartyIdentifier?.toQualifiedIdentifierDomain(),
                contacts = interactingPartyContact.orEmpty(),
            ),
            serviceType = interactingPartyType,
            signingTransactionId = signingTransactionIdentifier,
            certificateSerialNumber = certificateIdentifier,
            fileName = fileName,
            fileSizeBytes = fileSize?.toLongOrNull(),
            dtbsr = dtbsr,
        )

        is TransactionEntry.DataDeletionRequest -> {
            if (parentPresentationId.isNullOrBlank() || parentPresentationId == id || method == null) {
                return null
            }

            TransactionLogDomain.DataDeletionRequest(
                id = id,
                time = time.toLocalDateTime(),
                result = transactionResult.toTransactionResultDomain(),
                parentPresentationId = parentPresentationId,
                communicationMethod = method,
                party = InteractingPartyDomain(
                    name = interactingPartyName?.toLocalizedTextDomain(),
                    identifier = interactingPartyIdentifier?.toQualifiedIdentifierDomain(),
                    contacts = emptyList(),
                ),
                claims = listOfClaims.toCredentialClaimsDomain(),
            )
        }

        is TransactionEntry.DPAReport -> {
            if (parentPresentationId.isNullOrBlank() || parentPresentationId == id || method == null) {
                return null
            }

            TransactionLogDomain.DpaReport(
                id = id,
                time = time.toLocalDateTime(),
                result = transactionResult.toTransactionResultDomain(),
                parentPresentationId = parentPresentationId,
                communicationMethod = method,
                dpaName = dpaName?.toLocalizedTextDomain(),
                dpaCountry = dpaCountry?.toLocalizedTextDomain(),
            )
        }

        else -> null
    }
}

private fun TransactionResult.toTransactionResultDomain(): TransactionResultDomain = when (this) {
    is TransactionResult.Completed -> TransactionResultDomain.Completed
    is TransactionResult.NotCompleted -> TransactionResultDomain.NotCompleted(reason = reason)
}

/**
 * The registered details of the relying party, or null when the request carried no registration
 * certificate and every one of them is absent.
 */
private fun TransactionEntry.Presentation.toPresentationRegistrationDomain(
    userLocale: Locale,
): PresentationRegistrationDomain? {
    val localizedPurpose = purpose?.localizedContentOrNull(userLocale = userLocale)
    val policyUrls = privacyPolicy?.map { policy -> policy.policyURI }.orEmpty()
    val dpaContact = toDpaContactDomain()

    val hasNothing = registrarURL == null
            && localizedPurpose == null
            && policyUrls.isEmpty()
            && dpaContact == null
    if (hasNothing) {
        return null
    }

    return PresentationRegistrationDomain(
        registrarUrl = registrarURL,
        purpose = localizedPurpose,
        privacyPolicyUrls = policyUrls,
        dpa = dpaContact,
    )
}

private fun TransactionEntry.Presentation.toDpaContactDomain(): DpaContactDomain? {
    val name = dpaName?.toLocalizedTextDomain()
    val country = dpaCountry?.toLocalizedTextDomain()
    val contacts = dpaContact.orEmpty()

    if (name == null && country == null && contacts.isEmpty()) {
        return null
    }

    return DpaContactDomain(
        name = name,
        country = country,
        contacts = contacts,
    )
}

private fun TransactionEntry.CredentialIssuanceDetails.toIssuanceDetailsDomain(): IssuanceDetailsDomain {
    return IssuanceDetailsDomain(
        issuer = InteractingPartyDomain(
            name = interactingPartyName?.toLocalizedTextDomain(),
            identifier = interactingPartyIdentifier?.toQualifiedIdentifierDomain(),
            contacts = interactingPartyContact.orEmpty(),
        ),
        issuerType = interactingPartyType,
        requestedCount = credentialNumberRequested,
        issuedCount = credentialNumberIssued,
        credentials = credentialIdentifier.map { identifier ->
            CredentialRefDomain(identifier = identifier)
        },
        isUserTriggered = isUserTriggered,
    )
}

private fun List<ClaimInfo>.toCredentialClaimsDomain(): List<CredentialClaimsDomain> {
    return map { claimInfo ->
        CredentialClaimsDomain(
            credential = CredentialRefDomain(identifier = claimInfo.credentialIdentifier),
            claims = claimInfo.claims.map { claimPath ->
                ClaimRefDomain(segments = claimPath.toClaimPathSegments())
            },
        )
    }
}

private fun ClaimPath.toClaimPathSegments(): List<ClaimPathSegment> = segments.map { segment ->
    when (segment) {
        is ClaimPath.Segment.Key -> ClaimPathSegment.Key(name = segment.name)
        is ClaimPath.Segment.Index -> ClaimPathSegment.Index(index = segment.value)
        is ClaimPath.Segment.Wildcard -> ClaimPathSegment.AllElements
    }
}

private fun QualifiedIdentifier.toQualifiedIdentifierDomain(): QualifiedIdentifierDomain {
    return QualifiedIdentifierDomain(
        schemeUri = type,
        value = value,
    )
}

/** Entries tag each text with a language, which the shared resolution expects as a [Locale]. */
private fun List<MultiLangString>.localizedContentOrNull(userLocale: Locale): String? {
    return getLocalizedValue(
        userLocale = userLocale,
        localeExtractor = { text -> LocaleUtils.getLocaleFromSelectedLanguage(text.lang) },
        valueExtractor = { text -> text.content },
        fallback = null,
    )
}

/** Records only the data actually presented, with the new action's identity and time. */
internal fun TransactionLogDomain.Presentation.toDataDeletionRequestEntry(
    id: String,
    time: Instant,
): TransactionEntry.DataDeletionRequest {
    return TransactionEntry.DataDeletionRequest(
        transactionIdentifier = id,
        time = time,
        transactionResult = TransactionResult.Completed,
        listOfClaims = claimsPresented.toClaimInfo(),
        interactingPartyIdentifier = party.identifier?.toQualifiedIdentifier(),
        interactingPartyName = party.name?.toMultiLangString(),
    )
}

internal fun DpaContactDomain?.toDpaReportEntry(
    id: String,
    time: Instant,
): TransactionEntry.DPAReport {
    return TransactionEntry.DPAReport(
        transactionIdentifier = id,
        time = time,
        transactionResult = TransactionResult.Completed,
        dpaName = this?.name?.toMultiLangString(),
        dpaCountry = this?.country?.toMultiLangString(),
    )
}

private fun List<CredentialClaimsDomain>.toClaimInfo(): List<ClaimInfo> {
    return map { credentialClaims ->
        ClaimInfo(
            credentialIdentifier = credentialClaims.credential.identifier,
            claims = credentialClaims.claims.map { claim ->
                ClaimPath(
                    segments = claim.segments.map { segment ->
                        when (segment) {
                            is ClaimPathSegment.Key -> ClaimPath.Segment.Key(name = segment.name)
                            is ClaimPathSegment.Index -> ClaimPath.Segment.Index(value = segment.index)
                            is ClaimPathSegment.AllElements -> ClaimPath.Segment.Wildcard
                        }
                    }
                )
            },
        )
    }
}

private fun QualifiedIdentifierDomain.toQualifiedIdentifier(): QualifiedIdentifier {
    return QualifiedIdentifier(type = schemeUri, value = value)
}

private fun MultiLangString.toLocalizedTextDomain(): LocalizedTextDomain {
    return LocalizedTextDomain(languageTag = lang, text = content)
}

private fun LocalizedTextDomain.toMultiLangString(): MultiLangString {
    return MultiLangString(lang = languageTag, content = text)
}