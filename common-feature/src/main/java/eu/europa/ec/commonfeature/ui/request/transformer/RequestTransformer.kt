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

import eu.europa.ec.commonfeature.model.toUiName
import eu.europa.ec.commonfeature.ui.request.Event
import eu.europa.ec.commonfeature.ui.request.model.DocumentDetailsDomain
import eu.europa.ec.commonfeature.ui.request.model.DocumentDomainPayload
import eu.europa.ec.commonfeature.ui.request.model.DocumentItem
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi2
import eu.europa.ec.commonfeature.ui.request.model.UiCollapsedPayload
import eu.europa.ec.commonfeature.ui.request.model.UiExpandedPayload
import eu.europa.ec.commonfeature.ui.request.model.produceDocUID
import eu.europa.ec.commonfeature.util.keyIsBase64
import eu.europa.ec.commonfeature.util.parseKeyValueUi
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.model.toDocumentIdentifier
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.DocItem
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.nameSpacedDataJSONObject
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemLeadingContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.wrap.CheckboxData
import org.json.JSONObject

private fun getMandatoryFields(documentIdentifier: DocumentIdentifier): List<String> =
    when (documentIdentifier) {

        DocumentIdentifier.PID -> listOf(
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

        DocumentIdentifier.AGE -> listOf(
            "issuance_date",
            "expiry_date",
            "issuing_country",
            "issuing_authority",
        )

        else -> emptyList()
    }

object RequestTransformer {

    fun transformToDomainItems(
        storageDocuments: List<IssuedDocument> = emptyList(),
        resourceProvider: ResourceProvider,
        requestDocuments: List<RequestedDocument>,
    ): Result<List<DocumentDomainPayload>> = runCatching {
        val resultList: MutableList<DocumentDomainPayload> = mutableListOf()

        requestDocuments.forEach { requestDocument ->
            val storageDocument = storageDocuments.first { it.id == requestDocument.documentId }

            val docName = storageDocument.toUiName(resourceProvider)
            val docId = storageDocument.id
            val docNamespace = storageDocument.nameSpaces.keys.first()
            val documentItems: MutableList<DocumentItem> = mutableListOf()

            requestDocument.requestedItems.keys.forEach { docItem ->

                val isRequired = getMandatoryFields(
                    documentIdentifier = storageDocument.toDocumentIdentifier()
                ).contains(docItem.elementIdentifier)

                val (value, isAvailable) = try {
                    val values = StringBuilder()
                    parseKeyValueUi(
                        json = storageDocument.nameSpacedDataJSONObject.getDocObject(
                            nameSpace = docItem.namespace
                        )[docItem.elementIdentifier],
                        groupIdentifier = docItem.elementIdentifier,
                        resourceProvider = resourceProvider,
                        allItems = values
                    )
                    values.toString() to true
                } catch (ex: Exception) {
                    resourceProvider.getString(R.string.request_element_identifier_not_available) to false
                }

                documentItems.add(
                    DocumentItem(
                        elementIdentifier = docItem.elementIdentifier,
                        value = value,
                        readableName = resourceProvider.getReadableElementIdentifier(docItem.elementIdentifier),
                        isRequired = isRequired,
                        isAvailable = isAvailable
                    )
                )
            }

            resultList.add(
                DocumentDomainPayload(
                    docName = docName,
                    docId = docId,
                    docNamespace = docNamespace,
                    documentDetailsDomain = DocumentDetailsDomain(
                        items = documentItems
                    )
                )
            )
        }

        return@runCatching resultList
    }

    fun transformToUiItems(
        documentsDomain: List<DocumentDomainPayload>,
        resourceProvider: ResourceProvider,
    ): List<RequestDocumentItemUi2<Event>> {
        return documentsDomain.map { documentDomainPayload ->

            val collapsedItemId = documentDomainPayload.docId

            val expandedItems = documentDomainPayload.documentDetailsDomain.items.map { docItem ->
                val expandedItemId = produceDocUID(
                    elementIdentifier = docItem.elementIdentifier,
                    documentId = documentDomainPayload.docId,
                )

                val leadingContent = if (keyIsBase64(docItem.elementIdentifier)) {
                    ListItemLeadingContentData.UserImage(userBase64Image = docItem.value)
                } else {
                    null
                }

                val mainText = if (keyIsBase64(docItem.elementIdentifier) && docItem.isAvailable) {
                    ""
                } else {
                    docItem.value
                }

                UiExpandedPayload(
                    domainPayload = documentDomainPayload,
                    uiItem = ListItemData<Event>(
                        event = Event.UserIdentificationClicked(itemId = expandedItemId),
                        itemId = expandedItemId,
                        mainText = mainText,
                        overlineText = docItem.readableName,
                        leadingContentData = leadingContent,
                        trailingContentData = ListItemTrailingContentData.Checkbox(
                            checkboxData = CheckboxData(
                                isChecked = docItem.isAvailable,
                                enabled = docItem.isAvailable && !docItem.isRequired,
                                onCheckedChange = null,
                            )
                        )
                    )
                )
            }

            RequestDocumentItemUi2(
                uiCollapsedItem = UiCollapsedPayload(
                    uiItem = ListItemData(
                        event = Event.ExpandOrCollapseRequiredDataList(itemId = collapsedItemId),
                        itemId = collapsedItemId,
                        mainText = documentDomainPayload.docName,
                        supportingText = resourceProvider.getString(R.string.request_collapsed_supporting_text),
                        trailingContentData = ListItemTrailingContentData.Icon(
                            iconData = AppIcons.KeyboardArrowDown
                        )
                    ),
                    isExpanded = false
                ),
                uiExpandedItems = expandedItems
            )
        }
    }


    fun createDisclosedDocuments(items: List<RequestDocumentItemUi2<Event>>): DisclosedDocuments {
        // Collect all selected expanded items from the list
        val selectedItems = items.flatMap { requestItem ->
            requestItem.uiExpandedItems.filter { uiPayload ->
                // Filter only the items the user has selected
                uiPayload.uiItem.trailingContentData is ListItemTrailingContentData.Checkbox &&
                        (uiPayload.uiItem.trailingContentData as ListItemTrailingContentData.Checkbox)
                            .checkboxData.isChecked
            }
        }

        // Group the selected items by their domain payload (document-level grouping)
        val groupedByDocument = selectedItems.groupBy { it.domainPayload }

        // Convert to the format required by DisclosedDocuments
        val disclosedDocuments =
            groupedByDocument.map { (documentPayload, selectedItemsForDocument) ->
                val disclosedItems = selectedItemsForDocument.map { selectedItem ->
                    DocItem(
                        namespace = documentPayload.docNamespace,
                        elementIdentifier = documentPayload.documentDetailsDomain.items
                            .find { it.value == selectedItem.uiItem.mainText }
                            ?.elementIdentifier ?: ""
                    )
                }

                DisclosedDocument(
                    documentId = documentPayload.docId,
                    disclosedItems = disclosedItems,
                    keyUnlockData = null
                )
            }

        return DisclosedDocuments(disclosedDocuments)
    }

    private fun JSONObject.getDocObject(nameSpace: String): JSONObject =
        this[nameSpace] as JSONObject
}