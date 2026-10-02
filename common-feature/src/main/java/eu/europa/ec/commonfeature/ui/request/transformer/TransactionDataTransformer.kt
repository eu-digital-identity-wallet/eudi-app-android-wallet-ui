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
package eu.europa.ec.commonfeature.ui.request.transformer

import eu.europa.ec.commonfeature.ui.request.model.RequestTransactionDataUi
import eu.europa.ec.corelogic.model.DocumentChecksumDomain
import eu.europa.ec.corelogic.model.PresentationMatchDomain
import eu.europa.ec.corelogic.model.PresentationTransactionDataDomain
import eu.europa.ec.corelogic.model.SigningAttributeDomain
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemSupportingContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import java.net.URI

class TransactionDataTransformer(
    private val resourceProvider: ResourceProvider,
) {
    /** Matches belong to this combination only. */
    fun transformToUi(
        matches: List<PresentationMatchDomain>,
        sectionId: String,
    ): RequestTransactionDataUi? {
        val transactions = matches.flatMap { match -> match.transactionData }
        if (transactions.isEmpty()) return null

        val documentUrlsByItemId = mutableMapOf<String, String>()
        val rows = transactionRows(
            transactions = transactions,
            sectionId = sectionId,
            documentUrlsByItemId = documentUrlsByItemId,
        )
        return RequestTransactionDataUi(
            title = resourceProvider.getString(R.string.request_transaction_section_title),
            details = ExpandableListItemUi.NestedListItem(
                header = ListItemDataUi(
                    itemId = sectionId,
                    mainContentData = ListItemMainContentDataUi.Text(
                        text = resourceProvider.getString(R.string.request_transaction_details_title),
                    ),
                    supportingContentData = ListItemSupportingContentDataUi.Text(
                        text = resourceProvider.getString(R.string.request_collapsed_supporting_text),
                    ),
                    trailingContentData = ListItemTrailingContentDataUi.Icon(
                        iconData = AppIcons.KeyboardArrowDown,
                    ),
                ),
                nestedItems = rows,
                isExpanded = false,
            ),
            documentUrlsByItemId = documentUrlsByItemId.toMap(),
        )
    }

    fun transformRecordedToUi(
        transactions: List<PresentationTransactionDataDomain>,
        sectionId: String,
    ): List<ExpandableListItemUi> {
        if (transactions.isEmpty()) return emptyList()
        return transactionRows(
            transactions = transactions,
            sectionId = sectionId,
            documentUrlsByItemId = null,
        )
    }

    private fun transactionRows(
        transactions: List<PresentationTransactionDataDomain>,
        sectionId: String,
        documentUrlsByItemId: MutableMap<String, String>?,
    ): List<ExpandableListItemUi> {
        return buildList {
            addField(
                itemId = "$sectionId/framework",
                labelRes = R.string.request_transaction_trust_framework,
                value = resourceProvider.getString(R.string.request_transaction_trust_framework_value),
            )
            transactions.forEachIndexed { index, transaction ->
                val transactionId = "$sectionId/transaction-$index"
                if (transactions.size > 1) {
                    addField(
                        itemId = "$transactionId/title",
                        labelRes = null,
                        value = resourceProvider.getString(
                            R.string.request_transaction_numbered,
                            index + 1,
                        ),
                    )
                }
                when (transaction) {
                    is PresentationTransactionDataDomain.QesApproval -> {
                        addField(
                            itemId = "$transactionId/type",
                            labelRes = R.string.request_transaction_type,
                            value = transaction.displayName,
                        )
                        addField(
                            itemId = "$transactionId/signature-type",
                            labelRes = R.string.request_transaction_signature_type,
                            value = transaction.signatureQualifier?.let { qualifier ->
                                signatureType(qualifier = qualifier)
                            },
                        )
                        addCredentialReferences(
                            itemId = transactionId,
                            credentialIds = transaction.credentialIds,
                        )
                        addField(
                            itemId = "$transactionId/credential",
                            labelRes = R.string.request_transaction_signing_credential_id,
                            value = transaction.credentialId,
                        )
                        addField(
                            itemId = "$transactionId/count",
                            labelRes = R.string.request_transaction_signature_count,
                            value = transaction.numSignatures.toString(),
                        )
                        transaction.documentDigests.forEachIndexed { documentIndex, document ->
                            val documentId = "$transactionId/document-$documentIndex"
                            addDocumentName(
                                itemId = documentId,
                                label = document.label,
                                index = documentIndex,
                            )
                            addDocumentReference(
                                itemId = documentId,
                                href = document.href,
                                checksum = document.checksum,
                                documentUrlsByItemId = documentUrlsByItemId,
                            )
                            addField(
                                itemId = "$documentId/representation",
                                labelRes = R.string.request_transaction_hash_representation,
                                value = when (document.hashType) {
                                    "dtbsr" -> "DTBSR"
                                    "sdr" -> "SDR"
                                    "sodr" -> "SODR"
                                    else -> document.hashType
                                },
                            )
                            addField(
                                itemId = "$documentId/hash",
                                labelRes = when (document.hashType) {
                                    "dtbsr" -> R.string.request_transaction_dtbsr_hash
                                    "sdr" -> R.string.request_transaction_sdr_hash
                                    "sodr" -> R.string.request_transaction_sodr_hash
                                    else -> R.string.request_transaction_document_hash
                                },
                                value = document.hash,
                            )
                            addField(
                                itemId = "$documentId/hash-algorithm",
                                labelRes = when (document.hashType) {
                                    "dtbsr" -> R.string.request_transaction_dtbsr_algorithm
                                    "sdr" -> R.string.request_transaction_sdr_algorithm
                                    "sodr" -> R.string.request_transaction_sodr_algorithm
                                    else -> R.string.request_transaction_document_hash_algorithm
                                },
                                value = hashAlgorithm(oid = transaction.hashAlgorithmOid),
                            )
                            addSignedAttributes(
                                itemId = documentId,
                                attributes = document.signedProperties,
                            )
                            addField(
                                itemId = "$documentId/otp",
                                labelRes = R.string.request_transaction_otp,
                                value = document.oneTimePassword,
                            )
                        }
                    }

                    is PresentationTransactionDataDomain.Qes -> {
                        addField(
                            itemId = "$transactionId/type",
                            labelRes = R.string.request_transaction_type,
                            value = transaction.displayName,
                        )
                        addCredentialReferences(
                            itemId = transactionId,
                            credentialIds = transaction.credentialIds,
                        )
                        transaction.signatureRequests.forEachIndexed { documentIndex, document ->
                            val documentId = "$transactionId/document-$documentIndex"
                            addDocumentName(
                                itemId = documentId,
                                label = document.label,
                                index = documentIndex,
                            )
                            addDocumentReference(
                                itemId = documentId,
                                href = document.href,
                                checksum = document.checksum,
                                documentUrlsByItemId = documentUrlsByItemId,
                            )
                            addField(
                                itemId = "$documentId/signature-type",
                                labelRes = R.string.request_transaction_signature_type,
                                value = signatureType(qualifier = document.signatureQualifier),
                            )
                            addField(
                                itemId = "$documentId/format",
                                labelRes = R.string.request_transaction_signature_format,
                                value = when (document.signatureFormat) {
                                    "C" -> "CAdES"
                                    "X" -> "XAdES"
                                    "P" -> "PAdES"
                                    "J" -> "JAdES"
                                    else -> document.signatureFormat
                                },
                            )
                            addField(
                                itemId = "$documentId/conformance",
                                labelRes = R.string.request_transaction_conformance_level,
                                value = document.conformanceLevel,
                            )
                            addSignedAttributes(
                                itemId = documentId,
                                attributes = document.signedProperties,
                            )
                            addField(
                                itemId = "$documentId/otp",
                                labelRes = R.string.request_transaction_otp,
                                value = document.oneTimePassword,
                            )
                            addField(
                                itemId = "$documentId/response-uri",
                                labelRes = R.string.request_transaction_response_uri,
                                value = document.responseUri,
                            )
                        }
                    }

                    is PresentationTransactionDataDomain.Unavailable -> addField(
                        itemId = "$transactionId/unavailable",
                        labelRes = null,
                        value = resourceProvider.getString(R.string.request_transaction_unavailable),
                    )
                }
            }
        }
    }

    private fun MutableList<ExpandableListItemUi>.addField(
        itemId: String,
        labelRes: Int?,
        value: String?,
    ) {
        if (value == null) return
        add(
            ExpandableListItemUi.SingleListItem(
                header = ListItemDataUi(
                    itemId = itemId,
                    overlineText = labelRes?.let { resourceId ->
                        resourceProvider.getString(resourceId)
                    },
                    mainContentData = ListItemMainContentDataUi.Text(text = value),
                ),
            )
        )
    }

    private fun MutableList<ExpandableListItemUi>.addCredentialReferences(
        itemId: String,
        credentialIds: List<String>,
    ) {
        credentialIds.forEachIndexed { index, queryId ->
            addField(
                itemId = "$itemId/query-$index",
                labelRes = R.string.request_transaction_requested_credentials,
                value = queryId,
            )
        }
    }

    private fun MutableList<ExpandableListItemUi>.addDocumentName(
        itemId: String,
        label: String?,
        index: Int,
    ) {
        addField(
            itemId = "$itemId/name",
            labelRes = R.string.request_transaction_document,
            value = label ?: resourceProvider.getString(
                R.string.request_transaction_document_numbered,
                index + 1,
            ),
        )
    }

    private fun MutableList<ExpandableListItemUi>.addDocumentReference(
        itemId: String,
        href: String?,
        checksum: DocumentChecksumDomain?,
        documentUrlsByItemId: MutableMap<String, String>?,
    ) {
        addField(
            itemId = "$itemId/location",
            labelRes = R.string.request_transaction_document_location,
            value = href,
        )
        href?.takeIf { documentUrlsByItemId != null }?.let { location ->
            documentUrl(value = location)?.let { url ->
                val actionId = "$itemId/open"
                documentUrlsByItemId?.put(actionId, url)
                add(
                    ExpandableListItemUi.SingleListItem(
                        header = ListItemDataUi(
                            itemId = actionId,
                            mainContentData = ListItemMainContentDataUi.Text(
                                text = resourceProvider.getString(R.string.request_transaction_open_document),
                            ),
                            trailingContentData = ListItemTrailingContentDataUi.Icon(
                                iconData = AppIcons.OpenNew,
                            ),
                        ),
                    )
                )
            }
        }
        addField(
            itemId = "$itemId/checksum",
            labelRes = R.string.request_transaction_expected_checksum,
            value = checksum?.value,
        )
        addField(
            itemId = "$itemId/checksum-algorithm",
            labelRes = R.string.request_transaction_checksum_algorithm,
            value = checksum?.let { expectedChecksum ->
                hashAlgorithm(oid = expectedChecksum.algorithmOid)
            },
        )
    }

    private fun MutableList<ExpandableListItemUi>.addSignedAttributes(
        itemId: String,
        attributes: List<SigningAttributeDomain>?,
    ) {
        if (attributes.isNullOrEmpty()) return
        addField(
            itemId = "$itemId/attributes",
            labelRes = null,
            value = resourceProvider.getString(R.string.request_transaction_signed_attributes),
        )
        attributes.forEachIndexed { index, attribute ->
            add(
                ExpandableListItemUi.SingleListItem(
                    header = ListItemDataUi(
                        itemId = "$itemId/attribute-$index",
                        overlineText = attribute.value?.let { attribute.name },
                        mainContentData = ListItemMainContentDataUi.Text(
                            text = attribute.value ?: attribute.name,
                        ),
                    ),
                )
            )
        }
    }

    private fun signatureType(qualifier: String): String {
        val labelRes = when (qualifier) {
            "eu_eidas_qes" -> R.string.request_transaction_qes
            "eu_eidas_qeseal" -> R.string.request_transaction_qeseal
            "eu_eidas_aes" -> R.string.request_transaction_aes
            "eu_eidas_aeseal" -> R.string.request_transaction_aeseal
            "eu_eidas_aesqc" -> R.string.request_transaction_aesqc
            "eu_eidas_aesealqc" -> R.string.request_transaction_aesealqc
            else -> return qualifier
        }
        return resourceProvider.getString(labelRes)
    }

    private fun hashAlgorithm(oid: String): String {
        return when (oid) {
            "2.16.840.1.101.3.4.2.1" -> "SHA-256"
            "2.16.840.1.101.3.4.2.2" -> "SHA-384"
            "2.16.840.1.101.3.4.2.3" -> "SHA-512"
            else -> oid
        }
    }

    private fun documentUrl(value: String): String? {
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        return value.takeIf {
            (uri.scheme.equals("https", ignoreCase = true)
                    || uri.scheme.equals("http", ignoreCase = true))
                    && !uri.host.isNullOrBlank()
                    && uri.rawUserInfo == null
                    && (uri.port == -1 || uri.port in 1..65535)
        }
    }
}