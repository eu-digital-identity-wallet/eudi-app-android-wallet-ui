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
import eu.europa.ec.corelogic.model.QesInlineDocumentDomain
import eu.europa.ec.corelogic.model.SigningAttributeDomain
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemSupportingContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.ThemeColorKey
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
            showOneTimePasswords = true,
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
                        iconData = AppIcons.KeyboardArrowUp,
                    ),
                ),
                nestedItems = rows,
                isExpanded = true,
            ),
            documentUrlsByItemId = documentUrlsByItemId.toMap(),
        )
    }

    /**
     * The rows of [transactions] as transaction history shows them: the same fields as on the
     * request, without document actions and without one-time passwords, which the log keeps but
     * history does not show again.
     */
    fun transformRecordedToUi(
        transactions: List<PresentationTransactionDataDomain>,
        sectionId: String,
    ): List<ExpandableListItemUi> {
        if (transactions.isEmpty()) return emptyList()
        return transactionRows(
            transactions = transactions,
            sectionId = sectionId,
            documentUrlsByItemId = null,
            showOneTimePasswords = false,
        )
    }

    /**
     * One row for every field of [transactions], since every field is bound into the response.
     * The trust-framework row is a static product label and carries no verification result.
     * The signed attributes, which the relying party chooses freely, are marked as not checked.
     */
    private fun transactionRows(
        transactions: List<PresentationTransactionDataDomain>,
        sectionId: String,
        documentUrlsByItemId: MutableMap<String, String>?,
        showOneTimePasswords: Boolean,
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
                        addFields(
                            itemId = "$transactionId/location",
                            labelRes = R.string.request_transaction_location,
                            values = transaction.locations,
                        )
                        addFields(
                            itemId = "$transactionId/data-hash-algorithm",
                            labelRes = R.string.request_transaction_data_hash_algorithm,
                            values = transaction.transactionDataHashAlgorithms,
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
                            addField(
                                itemId = "$documentId/circumstantial-data",
                                labelRes = R.string.request_transaction_circumstantial_data,
                                value = document.circumstantialData,
                            )
                            addSignedAttributes(
                                itemId = documentId,
                                attributes = document.signedProperties,
                            )
                            addDocumentAccess(
                                itemId = documentId,
                                accessMode = document.accessMode,
                                oneTimePassword = document.oneTimePassword,
                                showOneTimePasswords = showOneTimePasswords,
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
                        addFields(
                            itemId = "$transactionId/data-hash-algorithm",
                            labelRes = R.string.request_transaction_data_hash_algorithm,
                            values = transaction.transactionDataHashAlgorithms,
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
                            addInlineDocument(
                                itemId = documentId,
                                document = document.inlineDocument,
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
                            addField(
                                itemId = "$documentId/signed-envelope-property",
                                labelRes = R.string.request_transaction_signed_envelope_property,
                                value = document.signedEnvelopeProperty,
                            )
                            addField(
                                itemId = "$documentId/reference-uri",
                                labelRes = R.string.request_transaction_reference_uri,
                                value = document.referenceUri,
                            )
                            addField(
                                itemId = "$documentId/circumstantial-data",
                                labelRes = R.string.request_transaction_circumstantial_data,
                                value = document.circumstantialData,
                            )
                            addField(
                                itemId = "$documentId/signature-algorithm",
                                labelRes = R.string.request_transaction_signature_algorithm,
                                value = document.signAlgo?.let { oid -> signatureAlgorithm(oid = oid) },
                            )
                            addField(
                                itemId = "$documentId/signature-algorithm-parameters",
                                labelRes = R.string.request_transaction_signature_algorithm_parameters,
                                value = document.signAlgoParams,
                            )
                            addSignedAttributes(
                                itemId = documentId,
                                attributes = document.signedProperties,
                            )
                            addDocumentAccess(
                                itemId = documentId,
                                accessMode = document.accessMode,
                                oneTimePassword = document.oneTimePassword,
                                showOneTimePasswords = showOneTimePasswords,
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

    private fun MutableList<ExpandableListItemUi>.addFields(
        itemId: String,
        labelRes: Int,
        values: List<String>,
    ) {
        values.forEachIndexed { index, value ->
            addField(itemId = "$itemId-$index", labelRes = labelRes, value = value)
        }
    }

    /**
     * How the document is fetched and, on the request only, its one-time password. History does
     * not show the password again.
     */
    private fun MutableList<ExpandableListItemUi>.addDocumentAccess(
        itemId: String,
        accessMode: String?,
        oneTimePassword: String?,
        showOneTimePasswords: Boolean,
    ) {
        addField(
            itemId = "$itemId/access",
            labelRes = R.string.request_transaction_document_access,
            value = accessMode?.let { mode -> documentAccess(mode = mode) },
        )
        if (showOneTimePasswords) {
            addField(
                itemId = "$itemId/otp",
                labelRes = R.string.request_transaction_otp,
                value = oneTimePassword,
            )
        }
    }

    /** A document the request carries itself: that it is included, its type and its size. */
    private fun MutableList<ExpandableListItemUi>.addInlineDocument(
        itemId: String,
        document: QesInlineDocumentDomain?,
    ) {
        if (document == null) return
        addField(
            itemId = "$itemId/content",
            labelRes = R.string.request_transaction_document_content,
            value = resourceProvider.getString(R.string.request_transaction_document_content_inline),
        )
        addField(
            itemId = "$itemId/document-type",
            labelRes = R.string.request_transaction_document_type,
            value = when (document.documentType) {
                "sod" -> resourceProvider.getString(R.string.request_transaction_document_type_original)
                "sfd" -> resourceProvider.getString(R.string.request_transaction_document_type_formatted)
                else -> document.documentType
            },
        )
        addField(
            itemId = "$itemId/size",
            labelRes = R.string.request_transaction_document_size,
            value = document.sizeInBytes?.let { size ->
                resourceProvider.getQuantityString(
                    R.plurals.request_transaction_document_size_value,
                    size,
                    size,
                )
            },
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
        val setByRelyingParty = ListItemSupportingContentDataUi.Text(
            text = resourceProvider.getString(R.string.request_transaction_relying_party_attribute),
            textColorKey = ThemeColorKey.Warning,
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
                        supportingContentData = setByRelyingParty,
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

    private fun documentAccess(mode: String): String {
        val labelRes = when (mode) {
            "public" -> R.string.request_transaction_document_access_public
            "OTP" -> R.string.request_transaction_document_access_otp
            "Basic_Auth" -> R.string.request_transaction_document_access_basic
            "Digest_Auth" -> R.string.request_transaction_document_access_digest
            "OAuth_20" -> R.string.request_transaction_document_access_oauth
            else -> return mode
        }
        return resourceProvider.getString(labelRes)
    }

    private fun signatureAlgorithm(oid: String): String {
        return when (oid) {
            "1.2.840.113549.1.1.1" -> "RSA"
            "1.2.840.113549.1.1.10" -> "RSASSA-PSS"
            "1.2.840.113549.1.1.11" -> "RSA with SHA-256"
            "1.2.840.113549.1.1.12" -> "RSA with SHA-384"
            "1.2.840.113549.1.1.13" -> "RSA with SHA-512"
            "1.2.840.10045.2.1" -> "ECDSA"
            "1.2.840.10045.4.3.2" -> "ECDSA with SHA-256"
            "1.2.840.10045.4.3.3" -> "ECDSA with SHA-384"
            "1.2.840.10045.4.3.4" -> "ECDSA with SHA-512"
            else -> oid
        }
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