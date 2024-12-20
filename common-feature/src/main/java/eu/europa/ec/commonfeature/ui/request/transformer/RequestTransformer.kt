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

import eu.europa.ec.businesslogic.extension.compareLocaleLanguage
import eu.europa.ec.commonfeature.ui.request.model.CollapsedUiItem
import eu.europa.ec.commonfeature.ui.request.model.DocumentDetailsDomain
import eu.europa.ec.commonfeature.ui.request.model.DocumentDomainPayload
import eu.europa.ec.commonfeature.ui.request.model.DocumentItemDomain
import eu.europa.ec.commonfeature.ui.request.model.ExpandedUiItem
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi
import eu.europa.ec.commonfeature.ui.request.model.generateUniqueFieldId
import eu.europa.ec.commonfeature.util.keyIsPortrait
import eu.europa.ec.commonfeature.util.keyIsSignature
import eu.europa.ec.commonfeature.util.parseKeyValueUi
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.model.toDocumentIdentifier
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.device.MsoMdocItem
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocData
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemLeadingContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.MainContentData
import eu.europa.ec.uilogic.component.wrap.CheckboxData

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

    fun transformToDomainItems(
        storageDocuments: List<IssuedDocument> = emptyList(),
        resourceProvider: ResourceProvider,
        requestDocuments: List<RequestedDocument>,
    ): Result<List<DocumentDomainPayload>> = runCatching {
        val resultList: MutableList<DocumentDomainPayload> = mutableListOf()

        requestDocuments.forEach { requestDocument ->
            val storageDocument = storageDocuments.first { it.id == requestDocument.documentId }

            val docName = storageDocument.name
            val docId = storageDocument.id
            val docNamespace = (storageDocument.data as MsoMdocData).nameSpaces.keys.first()
            val documentItemsDomain: MutableList<DocumentItemDomain> = mutableListOf()

            requestDocument.requestedItems.keys.forEach { docItem ->

                docItem as MsoMdocItem

                val isRequired = getMandatoryFields(
                    documentIdentifier = storageDocument.toDocumentIdentifier()
                ).contains(docItem.elementIdentifier)

                val item = storageDocument.data.claims.firstOrNull {
                    it.identifier == docItem.elementIdentifier
                }

                val readableName = item?.metadata?.display?.firstOrNull {
                    resourceProvider.getLocale().compareLocaleLanguage(it.locale)
                }?.name ?: docItem.elementIdentifier

                val (value, isAvailable) = try {
                    val values = StringBuilder()
                    parseKeyValueUi(
                        item = item?.value!!,
                        groupIdentifier = docItem.elementIdentifier,
                        resourceProvider = resourceProvider,
                        allItems = values
                    )
                    values.toString() to true
                } catch (_: Exception) {
                    resourceProvider.getString(R.string.request_element_identifier_not_available) to false
                }

                documentItemsDomain.add(
                    DocumentItemDomain(
                        elementIdentifier = docItem.elementIdentifier,
                        value = value,
                        readableName = readableName,
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
                        items = documentItemsDomain
                    )
                )
            )
        }

        return@runCatching resultList
    }

    fun transformToUiItems(
        documentsDomain: List<DocumentDomainPayload>,
        resourceProvider: ResourceProvider,
    ): List<RequestDocumentItemUi> {
        return documentsDomain.map { documentDomainPayload ->

            val collapsedItemId = documentDomainPayload.docId

            val expandedItems = documentDomainPayload.documentDetailsDomain.items.map { docItem ->
                val expandedItemId = generateUniqueFieldId(
                    elementIdentifier = docItem.elementIdentifier,
                    documentId = documentDomainPayload.docId,
                )

                val leadingContent = if (keyIsPortrait(key = docItem.elementIdentifier)) {
                    ListItemLeadingContentData.UserImage(userBase64Image = docItem.value)
                } else {
                    null
                }

                val mainText = when {
                    keyIsPortrait(key = docItem.elementIdentifier) && docItem.isAvailable -> {
                        MainContentData.Text(text = "")
                    }

                    keyIsSignature(key = docItem.elementIdentifier) && docItem.isAvailable -> {
                        MainContentData.Image(base64Image = docItem.value)
                    }

                    else -> {
                        MainContentData.Text(text = docItem.value)
                    }
                }

                ExpandedUiItem(
                    domainPayload = documentDomainPayload,
                    uiItem = ListItemData(
                        itemId = expandedItemId,
                        mainContentData = mainText,
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

            RequestDocumentItemUi(
                collapsedUiItem = CollapsedUiItem(
                    uiItem = ListItemData(
                        itemId = collapsedItemId,
                        mainContentData = MainContentData.Text(text = documentDomainPayload.docName),
                        supportingText = resourceProvider.getString(R.string.request_collapsed_supporting_text),
                        trailingContentData = ListItemTrailingContentData.Icon(
                            iconData = AppIcons.KeyboardArrowDown
                        )
                    ),
                    isExpanded = false
                ),
                expandedUiItems = expandedItems
            )
        }
    }

    fun createDisclosedDocuments(items: List<RequestDocumentItemUi>): DisclosedDocuments {
        // Collect all selected expanded items from the list
        val selectedItems = items.flatMap { requestItem ->
            requestItem.expandedUiItems.filter { uiPayload ->
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
                    val value = when (val mainContentData = selectedItem.uiItem.mainContentData) {
                        is MainContentData.Image -> mainContentData.base64Image

                        is MainContentData.Text -> {
                            (selectedItem.uiItem.leadingContentData as? ListItemLeadingContentData.UserImage)?.userBase64Image
                                ?: mainContentData.text
                        }
                    }
                    MsoMdocItem(
                        namespace = documentPayload.docNamespace,
                        elementIdentifier = documentPayload.documentDetailsDomain.items
                            .find { it.value == value }
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
}