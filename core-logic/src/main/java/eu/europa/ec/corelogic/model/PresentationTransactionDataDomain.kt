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

/** Display metadata shared by live requests and recorded presentations. */
sealed interface PresentationTransactionDataDomain {
    /**
     * A QES approval. Every field is shown to the user: the approval sent back is a hash of the
     * whole transaction data, so all of it is bound.
     */
    data class QesApproval(
        val displayName: String?,
        val credentialIds: List<String>,
        val credentialId: String?,
        val signatureQualifier: String?,
        val numSignatures: Int,
        val hashAlgorithmOid: String,
        val documentDigests: List<QesDocumentDigestDomain>,
        val locations: List<String> = emptyList(),
        val transactionDataHashAlgorithms: List<String> = emptyList(),
    ) : PresentationTransactionDataDomain

    /** A QES request; as with [QesApproval], every field is shown. */
    data class Qes(
        val displayName: String?,
        val credentialIds: List<String>,
        val signatureRequests: List<QesSignatureRequestDomain>,
        val transactionDataHashAlgorithms: List<String> = emptyList(),
    ) : PresentationTransactionDataDomain

    data object Unavailable : PresentationTransactionDataDomain
}

data class QesDocumentDigestDomain(
    val label: String?,
    val hash: String,
    val hashType: String,
    val signedProperties: List<SigningAttributeDomain>?,
    val href: String?,
    val checksum: DocumentChecksumDomain?,
    val oneTimePassword: String?,
    val circumstantialData: String? = null,
    val accessMode: String? = null,
)

data class QesSignatureRequestDomain(
    val label: String?,
    val signatureQualifier: String,
    val responseUri: String?,
    val signatureFormat: String?,
    val conformanceLevel: String?,
    val signedProperties: List<SigningAttributeDomain>?,
    val href: String?,
    val checksum: DocumentChecksumDomain?,
    val oneTimePassword: String?,
    val signedEnvelopeProperty: String? = null,
    val referenceUri: String? = null,
    val circumstantialData: String? = null,
    val signAlgo: String? = null,
    val signAlgoParams: String? = null,
    val accessMode: String? = null,
    val inlineDocument: QesInlineDocumentDomain? = null,
)

/**
 * A document the request carries itself rather than by reference: its [documentType] (`sod`, the
 * original document, or `sfd`, a formatted one) and its size, null when the content is not valid
 * Base64. The content is not kept.
 */
data class QesInlineDocumentDomain(
    val documentType: String,
    val sizeInBytes: Int?,
)

/** Expected document checksum, not a verification result. */
data class DocumentChecksumDomain(
    val value: String,
    val algorithmOid: String,
)

data class SigningAttributeDomain(
    val name: String,
    val value: String?,
)