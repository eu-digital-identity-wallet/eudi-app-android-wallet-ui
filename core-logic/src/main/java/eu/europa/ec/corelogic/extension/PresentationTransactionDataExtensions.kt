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

import eu.europa.ec.corelogic.model.DocumentChecksumDomain
import eu.europa.ec.corelogic.model.PresentationTransactionDataDomain
import eu.europa.ec.corelogic.model.QesDocumentDigestDomain
import eu.europa.ec.corelogic.model.QesSignatureRequestDomain
import eu.europa.ec.corelogic.model.SigningAttributeDomain
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionalData
import eu.europa.ec.eudi.wallet.transfer.openId4vp.TransactionDataType
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.Attribute
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.Checksum
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.DocumentDigest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.QesApprovalRequest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.QesRequest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.SignatureRequest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.payloads
import kotlinx.serialization.json.JsonArray
import org.multipaz.presentment.TransactionData

fun TransactionData<*>.toPresentationTransactionDataDomain(): PresentationTransactionDataDomain =
    payload.toTransactionPayloadDomain(displayName = type.displayName)

internal fun TransactionalData.toPresentationTransactionDataDomains(
    types: List<TransactionDataType>,
): List<PresentationTransactionDataDomain> = content.map { recorded ->
    // Read separately so a malformed type field cannot hide the other recorded entries.
    runCatching {
        TransactionalData(content = JsonArray(listOf(recorded)))
            .payloads(types = types)
            .single()
            .toTransactionPayloadDomain(displayName = null)
    }.getOrDefault(PresentationTransactionDataDomain.Unavailable)
}

private fun Any.toTransactionPayloadDomain(displayName: String?): PresentationTransactionDataDomain =
    when (val transactionPayload = this) {
        is QesApprovalRequest -> PresentationTransactionDataDomain.QesApproval(
            displayName = displayName,
            credentialIds = transactionPayload.credentialIds.toList(),
            credentialId = transactionPayload.credentialId,
            signatureQualifier = transactionPayload.signatureQualifier,
            numSignatures = transactionPayload.numSignatures,
            hashAlgorithmOid = transactionPayload.hashAlgorithmOid,
            documentDigests = transactionPayload.documentDigests.map { documentDigest ->
                documentDigest.toQesDocumentDigestDomain()
            },
        )

        is QesRequest -> PresentationTransactionDataDomain.Qes(
            displayName = displayName,
            credentialIds = transactionPayload.credentialIds.toList(),
            signatureRequests = transactionPayload.signatureRequests.map { signatureRequest ->
                signatureRequest.toQesSignatureRequestDomain()
            },
        )

        else -> PresentationTransactionDataDomain.Unavailable
    }

private fun DocumentDigest.toQesDocumentDigestDomain(): QesDocumentDigestDomain =
    QesDocumentDigestDomain(
        label = label,
        hash = hash,
        hashType = hashType,
        signedProperties = signedProperties?.map { attribute -> attribute.toSigningAttributeDomain() },
        href = href,
        checksum = checksum?.toDocumentChecksumDomain(),
        oneTimePassword = access?.oneTimePassword,
    )

private fun SignatureRequest.toQesSignatureRequestDomain(): QesSignatureRequestDomain {
    val documentReference = this as? SignatureRequest.WithDocumentReference
    return QesSignatureRequestDomain(
        label = label,
        signatureQualifier = signatureQualifier,
        responseUri = responseUri,
        signatureFormat = signatureFormat,
        conformanceLevel = conformanceLevel,
        signedProperties = signedProperties?.map { attribute -> attribute.toSigningAttributeDomain() },
        href = documentReference?.href,
        checksum = documentReference?.checksum?.toDocumentChecksumDomain(),
        oneTimePassword = documentReference?.access?.oneTimePassword,
    )
}

private fun Checksum.toDocumentChecksumDomain(): DocumentChecksumDomain =
    DocumentChecksumDomain(
        value = value,
        algorithmOid = algorithmOid,
    )

private fun Attribute.toSigningAttributeDomain(): SigningAttributeDomain =
    SigningAttributeDomain(
        name = name,
        value = value,
    )