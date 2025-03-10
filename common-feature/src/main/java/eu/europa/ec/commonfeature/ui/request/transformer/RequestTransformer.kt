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

import eu.europa.ec.commonfeature.extensions.toSelectiveExpandableListItems
import eu.europa.ec.commonfeature.ui.request.model.DocumentPayloadDomain
import eu.europa.ec.commonfeature.ui.request.model.DomainDocumentFormat
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi
import eu.europa.ec.commonfeature.util.docNamespace
import eu.europa.ec.commonfeature.util.transformPathsToDomainClaims
import eu.europa.ec.corelogic.model.ClaimPath
import eu.europa.ec.corelogic.model.DomainClaim
import eu.europa.ec.corelogic.model.toDocumentIdentifier
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.DocItem
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.device.MsoMdocItem
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.transfer.openId4vp.SdJwtVcItem
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem

object RequestTransformer {

    fun transformToDomainItems(
        storageDocuments: List<IssuedDocument>,
        resourceProvider: ResourceProvider,
        requestDocuments: List<RequestedDocument>,
    ): Result<List<DocumentPayloadDomain>> = runCatching {
        val resultList = mutableListOf<DocumentPayloadDomain>()

        requestDocuments.forEach { requestDocument ->
            val storageDocument =
                storageDocuments.first { it.id == requestDocument.documentId }

            val requestDocumentClaims = mutableListOf<DomainClaim>()
            val requestedItemsPaths = requestDocument.requestedItems.keys
                .map {
                    it.toClaimPath()
                }

            val domains = transformPathsToDomainClaims(
                paths = requestedItemsPaths,
                claims = storageDocument.data.claims,
                metadata = storageDocument.metadata,
                resourceProvider = resourceProvider,
                documentIdentifier = storageDocument.toDocumentIdentifier(),
            )

            if (requestedItemsPaths.isNotEmpty()) {
                requestDocumentClaims.addAll(
                    domains
                )
            }

            resultList.add(
                DocumentPayloadDomain(
                    docName = storageDocument.name,
                    docId = storageDocument.id,
                    domainDocFormat = DomainDocumentFormat.getFormat(
                        format = storageDocument.format,
                        namespace = storageDocument.docNamespace
                    ),
                    docClaimsDomain = requestDocumentClaims/*.sortedBy { (it as? DomainClaim.Claim)?.displayTitle?.lowercase() }*/, //TOD
                )
            )
        }

        resultList
    }


    fun transformToUiItems(
        documentsDomain: List<DocumentPayloadDomain>,
        resourceProvider: ResourceProvider,
    ): List<RequestDocumentItemUi> {
        return documentsDomain.map {
            RequestDocumentItemUi(
                domainPayload = it,
                headerUi = ExpandableListItem.NestedListItemData(
                    header = ListItemData(
                        itemId = it.docId,
                        mainContentData = ListItemMainContentData.Text(text = it.docName),
                        supportingText = resourceProvider.getString(R.string.request_collapsed_supporting_text),
                        trailingContentData = ListItemTrailingContentData.Icon(
                            iconData = AppIcons.KeyboardArrowDown
                        )
                    ),
                    nestedItems = it.toSelectiveExpandableListItems(resourceProvider.getString(R.string.request_element_identifier_not_available_id)),
                    isExpanded = false,
                ),
            )
        }
    }

    fun createDisclosedDocuments(items: List<RequestDocumentItemUi>): DisclosedDocuments {
        // Collect all selected expanded items from the list
        fun ExpandableListItem.collectSingles(): List<ExpandableListItem.SingleListItemData> =
            when (this) {
                is ExpandableListItem.SingleListItemData -> {
                    val isSelected =
                        header.trailingContentData is ListItemTrailingContentData.Checkbox &&
                                (header.trailingContentData as ListItemTrailingContentData.Checkbox)
                                    .checkboxData.isChecked
                    if (isSelected) listOf(this) else emptyList()
                }

                is ExpandableListItem.NestedListItemData -> nestedItems.flatMap { it.collectSingles() }
            }

        val groupedByDocument = items.map { document ->
            document.domainPayload to document.headerUi.nestedItems.flatMap { uiItem ->
                uiItem.collectSingles()
                    .distinctBy { listItemData ->
                        listItemData.header.itemId // Distinct by item ID to prevent duplicate sending of the same group
                    }
            }
        }

        // Convert to the format required by DisclosedDocuments
        val disclosedDocuments =
            groupedByDocument.map { (documentPayload, selectedItemsForDocument) ->

                val disclosedItems = selectedItemsForDocument.map { selectedItem ->

                    val selectedItemId = selectedItem.header.itemId

                    when (documentPayload.domainDocFormat) {
                        is DomainDocumentFormat.SdJwtVc -> SdJwtVcItem(
                            path = ClaimPath.toSdJwtVcPath(selectedItemId)
                        )

                        is DomainDocumentFormat.MsoMdoc -> MsoMdocItem(
                            namespace = documentPayload.domainDocFormat.namespace,
                            elementIdentifier = ClaimPath.toElementIdentifier(selectedItemId)
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

fun DocItem.toClaimPath(): ClaimPath {
    return when (this) {
        is MsoMdocItem -> return ClaimPath(listOf(this.elementIdentifier))
        is SdJwtVcItem -> return ClaimPath(this.path)
        else -> ClaimPath(emptyList())
    }
}
