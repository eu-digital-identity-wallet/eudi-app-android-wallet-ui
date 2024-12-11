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
import eu.europa.ec.commonfeature.ui.request.model.ExpandableFieldItemUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDataUi
import eu.europa.ec.commonfeature.ui.request.model.docType
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
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.wrap.CheckboxData
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemData
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

    fun transformToUiItems(
        storageDocuments: List<IssuedDocument> = emptyList(),
        resourceProvider: ResourceProvider,
        requestDocuments: List<RequestedDocument>
    ): List<RequestDataUi<Event>> {
        val items = mutableListOf<RequestDataUi<Event>>()

        requestDocuments.forEachIndexed { docIndex, requestDocument ->
            val storageDocument =
                storageDocuments.firstOrNull { it.id == requestDocument.documentId }
                    ?: return@forEachIndexed

            // Prepare expanded items (both required and optional)
            val expandedItems = mutableListOf<ListItemData>()
            val requiredFields = getMandatoryFields(storageDocument.toDocumentIdentifier())

            requestDocument.requestedItems.keys.forEach { docItem ->
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

                val itemId = produceDocUID(
                    docItem.elementIdentifier,
                    storageDocument.id,
                    storageDocument.docType
                )

                val isRequired = requiredFields.contains(docItem.elementIdentifier)
                val listItem = ListItemData(
                    itemId = itemId,
                    mainText = value,
                    overlineText = resourceProvider.getReadableElementIdentifier(docItem.elementIdentifier),
                    supportingText = null,
                    leadingIcon = if (keyIsBase64(key = docItem.elementIdentifier)) AppIcons.User else null,
                    trailingContentData = ListItemTrailingContentData.Checkbox(
                        checkboxData = CheckboxData(
                            isChecked = isAvailable,
                            enabled = !isRequired,
                            onCheckedChange = {
                                println("Checked changed for $itemId")
                                Event.UserIdentificationClicked(itemId = itemId)
                            }
                        )
                    )
                )

                expandedItems.add(listItem)
            }

            // Create a collapsed item for the document
            val collapsedItem = ListItemData(
                itemId = docIndex.toString(),
                mainText = storageDocument.toUiName(resourceProvider),
                overlineText = null,
                supportingText = "${expandedItems.size} fields requested",
                trailingContentData = null
            )

            // Add to the list as a single expandable card
            items += RequestDataUi.ExpandableField(
                expandableFieldItemUi = ExpandableFieldItemUi(
                    expandableListItem = ExpandableListItemData(
                        collapsed = collapsedItem,
                        expanded = expandedItems
                    ),
                    event = Event.ExpandOrCollapseRequiredDataList(docIndex)
                )
            )
        }

        return items
    }

    fun transformToDomainItems(uiItems: List<RequestDataUi<Event>>): DisclosedDocuments {
        // Extract selected items from ExpandableField
        val selectedUiItems = uiItems
            .flatMap {
                when (it) {
                    is RequestDataUi.ExpandableField -> {
                        val expandableList = it.expandableFieldItemUi.expandableListItem
                        expandableList.expanded.filter { listItem ->
                            listItem.trailingContentData is ListItemTrailingContentData.Checkbox &&
                                    (listItem.trailingContentData as ListItemTrailingContentData.Checkbox).checkboxData.isChecked
                        }
                    }

                    else -> emptyList()
                }
            }
            // Map selected items to their domain payloads
            .groupBy { listItem ->
                listItem.itemId // Use itemId to group items by their document
            }

        // Convert grouped data into domain model
        return DisclosedDocuments(
            selectedUiItems.map { entry ->
                val (documentId, selectedListItems) = entry
                DisclosedDocument(
                    documentId = documentId!!,
                    disclosedItems = selectedListItems.map { listItem ->
                        DocItem(
                            namespace = listItem.itemId!!, // Extract namespace if stored in ListItemData
                            elementIdentifier = listItem.mainText // Replace with appropriate field
                        )
                    },
                    keyUnlockData = null
                )
            }
        )
    }

    private fun JSONObject.getDocObject(nameSpace: String): JSONObject =
        this[nameSpace] as JSONObject
}