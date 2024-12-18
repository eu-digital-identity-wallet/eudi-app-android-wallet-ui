/*
 * Copyright (c) 2023 European Commission
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

import eu.europa.ec.commonfeature.ui.request.Event
import eu.europa.ec.commonfeature.ui.request.model.DocumentItemDomainPayload
import eu.europa.ec.commonfeature.ui.request.model.DocumentItemUi
import eu.europa.ec.commonfeature.ui.request.model.OptionalFieldItemUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDataUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi
import eu.europa.ec.commonfeature.ui.request.model.RequiredFieldsItemUi
import eu.europa.ec.commonfeature.ui.request.model.formatType
import eu.europa.ec.commonfeature.ui.request.model.produceDocUID
import eu.europa.ec.commonfeature.ui.request.model.toRequestDocumentItemUi
import eu.europa.ec.commonfeature.util.parseKeyValueUi
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.model.toDocumentIdentifier
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.device.MsoMdocItem
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider

private fun getMandatoryFields(documentIdentifier: DocumentIdentifier): List<String> =
    when (documentIdentifier) {

        DocumentIdentifier.MdocPid, DocumentIdentifier.SdJwtPid -> listOf(
            "issuance_date",
            "expiry_date",
            "issuing_authority",
            "document_number",
            "administrative_number",
            "issuing_country",
            "issuing_jurisdiction",
            "portrait",
            "portrait_capture_date"
        )

        DocumentIdentifier.MdocPseudonym -> listOf(
            "issuance_date",
            "expiry_date",
            "issuing_country",
            "issuing_authority",
        )

        else -> emptyList()
    }

object RequestTransformer {

    fun transformToUiItems(
        storageDocuments: List<IssuedDocument> = emptyList(),
        resourceProvider: ResourceProvider,
        requestDocuments: List<RequestedDocument>,
        requiredFieldsTitle: String
    ): List<RequestDataUi<Event>> {
        val items = mutableListOf<RequestDataUi<Event>>()

        requestDocuments.forEachIndexed { docIndex, requestDocument ->
            val storageDocument = storageDocuments.first { it.id == requestDocument.documentId }
            // Add document item.
            items += RequestDataUi.Document(
                documentItemUi = DocumentItemUi(
                    title = storageDocument.name
                )
            )
            items += RequestDataUi.Space()

            val required = mutableListOf<RequestDocumentItemUi<Event>>()

            // Add optional field items.
            requestDocument.requestedItems.keys.forEachIndexed { itemIndex, docItem ->
                docItem as MsoMdocItem
                val (value, isAvailable) = try {
                    val values = StringBuilder()
                    parseKeyValueUi(
                        item = storageDocument.data.claims.first {
                            it.identifier == docItem.elementIdentifier
                        }.value!!,
                        groupIdentifier = docItem.elementIdentifier,
                        resourceProvider = resourceProvider,
                        allItems = values
                    )
                    (values.toString() to true)
                } catch (_: Exception) {
                    (resourceProvider.getString(R.string.request_element_identifier_not_available) to false)
                }

                if (
                    getMandatoryFields(documentIdentifier = storageDocument.toDocumentIdentifier())
                        .contains(docItem.elementIdentifier)
                ) {
                    required.add(
                        docItem.toRequestDocumentItemUi(
                            uID = produceDocUID(
                                elementIdentifier = docItem.elementIdentifier,
                                documentId = storageDocument.id,
                                docType = storageDocument.formatType
                            ),
                            docPayload = DocumentItemDomainPayload(
                                docId = storageDocument.id,
                                formatType = storageDocument.formatType,
                                namespace = docItem.namespace,
                                elementIdentifier = docItem.elementIdentifier,
                            ),
                            optional = false,
                            isChecked = isAvailable,
                            event = null,
                            readableName = docItem.elementIdentifier,
                            value = value
                        )
                    )
                } else {
                    val uID = produceDocUID(
                        elementIdentifier = docItem.elementIdentifier,
                        documentId = storageDocument.id,
                        docType = storageDocument.formatType
                    )

                    items += RequestDataUi.Space()
                    items += RequestDataUi.OptionalField(
                        optionalFieldItemUi = OptionalFieldItemUi(
                            requestDocumentItemUi = docItem.toRequestDocumentItemUi(
                                uID = uID,
                                docPayload = DocumentItemDomainPayload(
                                    docId = storageDocument.id,
                                    formatType = storageDocument.formatType,
                                    namespace = docItem.namespace,
                                    elementIdentifier = docItem.elementIdentifier,
                                ),
                                optional = isAvailable,
                                isChecked = isAvailable,
                                event = Event.UserIdentificationClicked(itemId = uID),
                                readableName = docItem.elementIdentifier,
                                value = value
                            )
                        )
                    )

                    if (itemIndex != requestDocument.requestedItems.keys.toList().lastIndex) {
                        items += RequestDataUi.Space()
                        items += RequestDataUi.Divider()
                    }
                }
            }

            items += RequestDataUi.Space()

            // Add required fields item.
            if (required.isNotEmpty()) {
                items += RequestDataUi.RequiredFields(
                    requiredFieldsItemUi = RequiredFieldsItemUi(
                        id = docIndex,
                        requestDocumentItemsUi = required,
                        expanded = false,
                        title = requiredFieldsTitle,
                        event = Event.ExpandOrCollapseRequiredDataList(id = docIndex)
                    )
                )
                items += RequestDataUi.Space()
            }
        }

        return items
    }

    fun transformToDomainItems(uiItems: List<RequestDataUi<Event>>): DisclosedDocuments {
        val selectedUiItems = uiItems
            .flatMap {
                when (it) {
                    is RequestDataUi.RequiredFields -> {
                        it.requiredFieldsItemUi.requestDocumentItemsUi
                    }

                    is RequestDataUi.OptionalField -> {
                        listOf(it.optionalFieldItemUi.requestDocumentItemUi)
                    }

                    else -> {
                        emptyList()
                    }
                }
            }
            // Get selected
            .filter { it.checked }
            // Create a Map with document as a key
            .groupBy {
                it.domainPayload
            }

        return DisclosedDocuments(
            selectedUiItems.map { entry ->
                val (document, selectedDocumentItems) = entry
                DisclosedDocument(
                    documentId = document.docId,
                    disclosedItems = selectedDocumentItems.map {
                        MsoMdocItem(
                            it.domainPayload.namespace,
                            it.domainPayload.elementIdentifier
                        )
                    },
                    keyUnlockData = null
                )
            }
        )
    }
}