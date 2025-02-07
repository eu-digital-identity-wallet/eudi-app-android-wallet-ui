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

import eu.europa.ec.commonfeature.ui.request.model.CollapsedUiItem
import eu.europa.ec.commonfeature.ui.request.model.DocumentPayloadDomain
import eu.europa.ec.commonfeature.ui.request.model.ExpandedUiItem
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentClaim
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi
import eu.europa.ec.commonfeature.util.docNamespace
import eu.europa.ec.commonfeature.util.generateUniqueFieldId
import eu.europa.ec.commonfeature.util.keyIsPortrait
import eu.europa.ec.commonfeature.util.keyIsSignature
import eu.europa.ec.commonfeature.util.parseKeyValueUi
import eu.europa.ec.corelogic.extension.getLocalizedClaimName
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.model.toDocumentIdentifier
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.device.MsoMdocItem
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.NameSpace
import eu.europa.ec.eudi.wallet.transfer.openId4vp.SdJwtVcItem
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemLeadingContentData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
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
    ): Result<List<DocumentPayloadDomain>> = runCatching {
        val userLocale = resourceProvider.getLocale()
        val resultList: MutableList<DocumentPayloadDomain> = mutableListOf()

        requestDocuments.forEach { requestDocument ->
            val storageDocument = storageDocuments.first { it.id == requestDocument.documentId }

            val docName: String = storageDocument.name
            val docId: DocumentId = storageDocument.id
            val docNamespace: NameSpace? = storageDocument.docNamespace

            val requestDocumentClaims: MutableList<RequestDocumentClaim> = mutableListOf()

            requestDocument.requestedItems.keys.forEach { docItem ->

                val isRequired = getMandatoryFields(
                    documentIdentifier = storageDocument.toDocumentIdentifier()
                ).contains(docItem.elementIdentifier)

                val documentClaim = storageDocument.data.claims.find {
                    it.identifier == docItem.elementIdentifier
                }

                val display = storageDocument.metadata?.claims
                    ?.find { it.name.name == docItem.elementIdentifier }
                    ?.display

                val readableName: String = display.getLocalizedClaimName(
                    userLocale = userLocale,
                    fallback = docItem.elementIdentifier
                )

                val (value, isAvailable) = try {
                    val values = StringBuilder()
                    parseKeyValueUi(
                        item = documentClaim?.value!!,
                        groupIdentifier = readableName,
                        groupIdentifierKey = docItem.elementIdentifier,
                        resourceProvider = resourceProvider,
                        allItems = values
                    )
                    values.toString() to true
                } catch (_: Exception) {
                    resourceProvider.getString(R.string.request_element_identifier_not_available) to false
                }

                requestDocumentClaims.add(
                    RequestDocumentClaim(
                        elementIdentifier = docItem.elementIdentifier,
                        value = value,
                        readableName = readableName,
                        isRequired = isRequired,
                        isAvailable = isAvailable,
                    )
                )
            }

            resultList.add(
                DocumentPayloadDomain(
                    docName = docName,
                    docId = docId,
                    docNamespace = docNamespace,
                    docClaimsDomain = requestDocumentClaims.sortedBy { it.readableName.lowercase() },
                )
            )
        }

        return@runCatching resultList
    }

    fun transformToUiItems(
        documentsDomain: List<DocumentPayloadDomain>,
        resourceProvider: ResourceProvider,
    ): List<RequestDocumentItemUi> {
        return documentsDomain.map { docPayloadDomain ->

            val collapsedItemId = docPayloadDomain.docId

            val expandedItems = docPayloadDomain.docClaimsDomain.map { docClaimDomain ->
                val expandedItemId = generateUniqueFieldId(
                    elementIdentifier = docClaimDomain.elementIdentifier,
                    documentId = docPayloadDomain.docId,
                )

                val leadingContent =
                    if (keyIsPortrait(key = docClaimDomain.elementIdentifier) && docClaimDomain.isAvailable) {
                        ListItemLeadingContentData.UserImage(userBase64Image = docClaimDomain.value)
                    } else {
                        null
                    }

                val mainContent = when {
                    keyIsPortrait(key = docClaimDomain.elementIdentifier) && docClaimDomain.isAvailable -> {
                        ListItemMainContentData.Text(text = "")
                    }

                    keyIsSignature(key = docClaimDomain.elementIdentifier) && docClaimDomain.isAvailable -> {
                        ListItemMainContentData.Image(base64Image = docClaimDomain.value)
                    }

                    else -> {
                        ListItemMainContentData.Text(text = docClaimDomain.value)
                    }
                }

                ExpandedUiItem(
                    domainPayload = docPayloadDomain,
                    uiItem = ListItemData(
                        itemId = expandedItemId,
                        mainContentData = mainContent,
                        overlineText = docClaimDomain.readableName,
                        leadingContentData = leadingContent,
                        trailingContentData = ListItemTrailingContentData.Checkbox(
                            checkboxData = CheckboxData(
                                isChecked = docClaimDomain.isAvailable,
                                enabled = docClaimDomain.isAvailable && !docClaimDomain.isRequired,
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
                        mainContentData = ListItemMainContentData.Text(text = docPayloadDomain.docName),
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
                        is ListItemMainContentData.Image -> mainContentData.base64Image

                        is ListItemMainContentData.Text -> {
                            (selectedItem.uiItem.leadingContentData as? ListItemLeadingContentData.UserImage)?.userBase64Image
                                ?: mainContentData.text
                        }

                        is ListItemMainContentData.Actionable<*> -> {
                            (selectedItem.uiItem.leadingContentData as? ListItemLeadingContentData.UserImage)?.userBase64Image
                                ?: mainContentData.text
                        }
                    }

                    val elementIdentifier = documentPayload.docClaimsDomain
                        .find { it.value == value }
                        ?.elementIdentifier ?: ""

                    when (documentPayload.docNamespace) {
                        null -> SdJwtVcItem(
                            elementIdentifier = elementIdentifier
                        )

                        else -> MsoMdocItem(
                            namespace = documentPayload.docNamespace,
                            elementIdentifier = elementIdentifier
                        )
                    }
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