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

import java.time.LocalDateTime

/** Credential and claim details use raw identifiers, without display names or claim values. */
sealed interface TransactionLogDomain {

    /** Identifies the stored entry; never parsed. */
    val id: String

    /** When the transaction started, which is not when it reached [result]. */
    val time: LocalDateTime

    val result: TransactionResultDomain

    /**
     * Credentials presented to a verifier, or an attempt to.
     *
     * @property registration details from the relying party's registration certificate, or null
     * when the request contained none.
     */
    data class Presentation(
        override val id: String,
        override val time: LocalDateTime,
        override val result: TransactionResultDomain,
        val party: InteractingPartyDomain,
        val partyType: String?,
        val intermediary: InteractingPartyDomain?,
        val registration: PresentationRegistrationDomain?,
        val claimsRequested: List<CredentialClaimsDomain>,
        val claimsPresented: List<CredentialClaimsDomain>,
        val transactionData: List<PresentationTransactionDataDomain>,
    ) : TransactionLogDomain {
        /** A failed presentation may still have shared data. */
        val canRequestDataDeletion: Boolean
            get() = claimsPresented.any { credentialClaims -> credentialClaims.claims.isNotEmpty() }
    }

    data class CredentialIssuance(
        override val id: String,
        override val time: LocalDateTime,
        override val result: TransactionResultDomain,
        val details: IssuanceDetailsDomain,
    ) : TransactionLogDomain

    data class CredentialReissuance(
        override val id: String,
        override val time: LocalDateTime,
        override val result: TransactionResultDomain,
        val details: IssuanceDetailsDomain,
    ) : TransactionLogDomain

    /** One entry per deleted credential. */
    data class CredentialDeletion(
        override val id: String,
        override val time: LocalDateTime,
        override val result: TransactionResultDomain,
        val credential: CredentialRefDomain,
        val issuer: InteractingPartyDomain,
    ) : TransactionLogDomain

    /**
     * A document signed or sealed through the RQES flow.
     *
     * @property dtbsr the hash value used for signing.
     */
    data class SigningSealing(
        override val id: String,
        override val time: LocalDateTime,
        override val result: TransactionResultDomain,
        val service: InteractingPartyDomain,
        val serviceType: String?,
        val signingTransactionId: String?,
        val certificateSerialNumber: String?,
        val fileName: String?,
        val fileSizeBytes: Long?,
        val dtbsr: String?,
    ) : TransactionLogDomain

    /** Records an attempted deletion request or report, without confirming delivery. */
    sealed interface PresentationAction : TransactionLogDomain {
        val parentPresentationId: String
        val communicationMethod: CommunicationMethodDomain
    }

    /**
     * A request sent to a relying party to delete data it holds.
     *
     * @property claims the data shared in the original presentation.
     */
    data class DataDeletionRequest(
        override val id: String,
        override val time: LocalDateTime,
        override val result: TransactionResultDomain,
        override val parentPresentationId: String,
        override val communicationMethod: CommunicationMethodDomain,
        val party: InteractingPartyDomain,
        val claims: List<CredentialClaimsDomain>,
    ) : PresentationAction

    /** A transaction reported to a data protection authority. */
    data class DpaReport(
        override val id: String,
        override val time: LocalDateTime,
        override val result: TransactionResultDomain,
        override val parentPresentationId: String,
        override val communicationMethod: CommunicationMethodDomain,
        val dpaName: LocalizedTextDomain?,
        val dpaCountry: LocalizedTextDomain?,
    ) : PresentationAction
}

sealed interface TransactionResultDomain {

    data object Completed : TransactionResultDomain

    /**
     * Also covers issuance that is still awaiting a credential.
     * The reason is the recorded message and may not be localized.
     */
    data class NotCompleted(val reason: String?) : TransactionResultDomain
}

/**
 * The other side of a transaction: a verifier, a credential issuer, a signing service, or an
 * authority.
 *
 * @property contacts Plain strings without a declared type or purpose. Each value requires
 * format validation before use as a contact method. Some values may not be recognized, and
 * a recognized format alone does not indicate the contact's intended use.
 */
data class InteractingPartyDomain(
    val name: LocalizedTextDomain?,
    val identifier: QualifiedIdentifierDomain?,
    val contacts: List<String>,
)

/** Keeps the recorded text and its original language tag. */
data class LocalizedTextDomain(
    val languageTag: String,
    val text: String,
)

data class QualifiedIdentifierDomain(
    /** URI of the register the identifier belongs to. */
    val schemeUri: String,
    val value: String,
)

/** What a relying party's registration certificate declared about a presentation. */
data class PresentationRegistrationDomain(
    val registrarUrl: String?,
    val purpose: String?,
    val privacyPolicyUrls: List<String>,
    val dpa: DpaContactDomain?,
)

data class DpaContactDomain(
    val name: LocalizedTextDomain?,
    val country: LocalizedTextDomain?,
    val contacts: List<String>,
)

data class IssuanceDetailsDomain(
    val issuer: InteractingPartyDomain,
    val issuerType: String?,
    /** May differ from [issuedCount] when issuance is incomplete. */
    val requestedCount: Int,
    val issuedCount: Int,
    val credentials: List<CredentialRefDomain>,
    /** False for issuer-initiated issuance or automatic renewal; null when unknown. */
    val isUserTriggered: Boolean?,
)

/** The claims of one credential that took part in a transaction. */
data class CredentialClaimsDomain(
    val credential: CredentialRefDomain,
    val claims: List<ClaimRefDomain>,
)

/** The recorded docType or vct. */
data class CredentialRefDomain(
    val identifier: FormatType,
)

/**
 * A claim a transaction refers to. The log records which claim, never its value.
 *
 * @property segments the claim's path; for mdoc it is namespace followed by element name.
 */
data class ClaimRefDomain(
    val segments: List<ClaimPathSegment>,
)