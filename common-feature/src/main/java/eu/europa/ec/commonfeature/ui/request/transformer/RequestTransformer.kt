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

import eu.europa.ec.commonfeature.ui.request.model.DocumentPayloadDomain
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi
import eu.europa.ec.commonfeature.util.createKeyValue
import eu.europa.ec.commonfeature.util.docNamespace
import eu.europa.ec.commonfeature.util.getReadableNameFromIdentifier
import eu.europa.ec.commonfeature.util.keyIsPortrait
import eu.europa.ec.commonfeature.util.keyIsSignature
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.model.toDocumentIdentifier
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.DocItem
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.device.MsoMdocItem
import eu.europa.ec.eudi.wallet.document.ElementIdentifier
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.DocumentClaim
import eu.europa.ec.eudi.wallet.document.format.MsoMdocClaim
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcClaim
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.document.metadata.DocumentMetaData
import eu.europa.ec.eudi.wallet.transfer.openId4vp.SdJwtVcItem
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemLeadingContentData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.wrap.CheckboxData
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem
import eu.europa.ec.uilogic.component.wrap.PATH_SEPARATOR

private fun getMandatoryFields(documentIdentifier: DocumentIdentifier): List<String> =
    when (documentIdentifier) {

        DocumentIdentifier.MdocPid, DocumentIdentifier.SdJwtPid -> listOf(
            "issuance_date",
            "iat",
            "expiry_date",
            "exp",
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

sealed class DomainClaim {
    abstract val key: ElementIdentifier
    abstract val displayTitle: String
    abstract val path: List<String>

    sealed class Claim : DomainClaim() {
        data class Group(
            override val key: ElementIdentifier,
            override val displayTitle: String,
            override val path: List<String>,
            val items: List<DomainClaim>,
        ) : Claim()

        data class Primitive(
            override val key: ElementIdentifier,
            override val displayTitle: String,
            override val path: List<String>,
            val value: String,
            val isRequired: Boolean,
        ) : Claim()
    }

    data class NotAvailableClaim(
        override val key: ElementIdentifier,
        override val displayTitle: String,
        override val path: List<String>,
        val value: String,
    ) : DomainClaim()
}

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
                    it.toPath()
                }

            val domains = buildDomainTree(
                paths = requestedItemsPaths,
                claims = storageDocument.data.claims,
                metadata = storageDocument.metadata,
                resourceProvider = resourceProvider,
                documentIdentifier = storageDocument.toDocumentIdentifier(),
            )
            println(domains)

            if (requestedItemsPaths.isNotEmpty()) {
                requestDocumentClaims.addAll(
                    domains
//                    storageDocument.findSdJwtVcClaimFromPath(
//                        paths = sdJwtVcPaths,
//                        metadata = storageDocument.metadata,
//                        resourceProvider = resourceProvider,
//                        documentIdentifier = storageDocument.toDocumentIdentifier(),
//                    )
                )
            }

            resultList.add(
                DocumentPayloadDomain(
                    docName = storageDocument.name,
                    docId = storageDocument.id,
                    docNamespace = storageDocument.docNamespace,
                    docClaimsDomain = requestDocumentClaims.sortedBy { (it as? DomainClaim.Claim)?.displayTitle?.lowercase() }, //TOD
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
                headerUi = ExpandableListItem.SingleListItemData(
                    collapsed = ListItemData(
                        itemId = it.docId,
                        mainContentData = ListItemMainContentData.Text(text = it.docName),
                        supportingText = resourceProvider.getString(R.string.request_collapsed_supporting_text),
                        trailingContentData = ListItemTrailingContentData.Icon(
                            iconData = AppIcons.KeyboardArrowDown
                        )
                    )
                ),
                claimsUi = it.toExpandableListItem()
            )
        }
    }

    fun DocumentPayloadDomain.toExpandableListItem(): List<ExpandableListItem> {
        return this.docClaimsDomain.map { claim ->
            claim.toExpandableListItem(docId)
        }
    }

    fun DomainClaim.toExpandableListItem(docId: String): ExpandableListItem {
        return when (this) {
            is DomainClaim.Claim.Group -> {
                ExpandableListItem.NestedListItemData(
                    collapsed = ListItemData(
                        itemId = (listOf(docId) + path).joinToString(separator = PATH_SEPARATOR), // TODO find a better way to create/extract the itemId
                        mainContentData = ListItemMainContentData.Text(text = displayTitle),
                        trailingContentData = ListItemTrailingContentData.Icon(iconData = AppIcons.KeyboardArrowDown)
                    ),
                    expanded = items.map { it.toExpandableListItem(docId) },
                    isExpanded = true //TODO make it false by default
                )
            }

            is DomainClaim.Claim.Primitive -> {
                val leadingContent =
                    if (keyIsPortrait(key = key)) {
                        ListItemLeadingContentData.UserImage(userBase64Image = value)
                    } else {
                        null
                    }

                val mainContent = when {
                    keyIsPortrait(key = key) -> {
                        ListItemMainContentData.Text(text = "")
                    }

                    keyIsSignature(key = key) -> {
                        ListItemMainContentData.Image(base64Image = value)
                    }

                    else -> {
                        ListItemMainContentData.Text(text = value)
                    }
                }

                ExpandableListItem.SingleListItemData(
                    collapsed = ListItemData(
                        itemId = (listOf(docId) + path).joinToString(separator = PATH_SEPARATOR),// TODO find a better way to create/extract the itemId
                        mainContentData = mainContent,
                        overlineText = displayTitle,
                        leadingContentData = leadingContent,
                        trailingContentData = ListItemTrailingContentData.Checkbox(
                            checkboxData = CheckboxData(
                                isChecked = true,
                                enabled = !isRequired
                            )
                        )
                    )
                )
            }

            is DomainClaim.NotAvailableClaim -> {
                ExpandableListItem.SingleListItemData(
                    collapsed = ListItemData(
                        itemId = "-1", // TODO revisit
                        mainContentData = ListItemMainContentData.Text(text = value),
                        overlineText = displayTitle,
                        trailingContentData = ListItemTrailingContentData.Checkbox(
                            checkboxData = CheckboxData(
                                isChecked = false, //TODO should this, business-wise be true/Primitive.required
                                enabled = false
                            )
                        )
                    )
                )
            }
        }
    }


    fun createDisclosedDocuments(items: List<RequestDocumentItemUi>): DisclosedDocuments {
        // Collect all selected expanded items from the list
        fun ExpandableListItem.collectSingles(): List<ExpandableListItem.SingleListItemData> =
            when (this) {
                is ExpandableListItem.SingleListItemData -> {
                    val isSelected =
                        collapsed.trailingContentData is ListItemTrailingContentData.Checkbox &&
                                (collapsed.trailingContentData as ListItemTrailingContentData.Checkbox)
                                    .checkboxData.isChecked
                    if (isSelected) listOf(this) else emptyList()
                }

                is ExpandableListItem.NestedListItemData -> expanded.flatMap { it.collectSingles() }
            }

        val groupedByDocument = items.map {
            it.domainPayload to it.claimsUi.flatMap { uiItem ->
                uiItem.collectSingles()
                    .distinctBy {
                        it.collapsed.itemId // Distinct by item ID to prevent duplicate sending of the same group
                    }
            }
        }

        // Convert to the format required by DisclosedDocuments
        val disclosedDocuments =
            groupedByDocument.map { (documentPayload, selectedItemsForDocument) ->

                val disclosedItems = selectedItemsForDocument.map { selectedItem ->

                    when (documentPayload.docNamespace) {
                        null -> SdJwtVcItem(
                            selectedItem.collapsed.itemId
                                .split(PATH_SEPARATOR) //or just have/carry the DocItem in the class and use it here as is
                                .drop(1)
                        )

                        else -> MsoMdocItem(
                            namespace = documentPayload.docNamespace,
                            elementIdentifier = selectedItem.collapsed.itemId
                                .split(PATH_SEPARATOR) //or just have/carry the DocItem in the class and use it here as is
                                .drop(1)
                                .first() //TODO
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

fun IssuedDocument.findClaimFromDocItem(docItem: DocItem) =
    findClaimFromPath(docItem.toPath())

fun DocItem.toPath(): List<String> {
    return when (this) {
        is MsoMdocItem -> return listOf(this.elementIdentifier)
        is SdJwtVcItem -> return this.path
        else -> emptyList()
    }
}

fun IssuedDocument.findClaimFromPath(path: List<String>): DocumentClaim? {
    return when (format) {
        is MsoMdocFormat -> {
            if (path.size == 2) null
            val nameSpace = path.first()
            val elementIdentifier = path.last()
            data.claims.filterIsInstance<MsoMdocClaim>().firstOrNull {
                it.identifier == elementIdentifier && it.nameSpace == nameSpace
            }

        }

        is SdJwtVcFormat -> {
            /* data.claims.filterIsInstance<SdJwtVcClaim>()
                 .firstOrNull { it.identifier == path.first() }*/

            var claim: SdJwtVcClaim? = null
            for (claimId in path) {
                claim = claim?.children?.firstOrNull {
                    it.identifier == claimId
                }
                    ?: data.claims.filterIsInstance<SdJwtVcClaim>()
                        .firstOrNull {
                            it.identifier == claimId
                        }
            }
            claim
        }
    }
}

fun insertPath(
    tree: List<DomainClaim>,
    path: List<String>,
    disclosurePath: List<String>,
    claims: List<DocumentClaim>,
    metadata: DocumentMetaData?,
    resourceProvider: ResourceProvider,
    documentIdentifier: DocumentIdentifier,
    //isSdJwtVc: Boolean,
): List<DomainClaim> {
    if (path.isEmpty()) return tree

    val userLocale = resourceProvider.getLocale()

    val key = path.first()

    val existingNode = tree.find { it.key == key }

    val currentClaim: DocumentClaim? = claims
        .firstOrNull {
            it.identifier == key
        }

    val isRequired = getMandatoryFields(
        documentIdentifier = documentIdentifier
    ).contains(currentClaim?.identifier) //TODO change this, it should be its path, e.g. "address.formatted"

    println(currentClaim)

    return if (path.size == 1) {
        // Leaf node (Primitive)
        if (existingNode == null) {
            if (currentClaim == null) {
                tree + DomainClaim.NotAvailableClaim(
                    key = key,
                    displayTitle = getReadableNameFromIdentifier(
                        metadata = metadata,
                        userLocale = userLocale,
                        identifier = key
                    ),
                    path = disclosurePath,
                    value = resourceProvider.getString(R.string.request_element_identifier_not_available)
                )
            } else {
                val formattedValue = buildList {
                    createKeyValue(
                        item = currentClaim.value!!,
                        groupKey = currentClaim.identifier,
                        //keyIdentifier = keyIdentifier,
                        resourceProvider = resourceProvider,
                        allItems = this
                    )
                }

                val newEntry = if (formattedValue.size == 1) {
                    DomainClaim.Claim.Primitive(
                        key = currentClaim.identifier,
                        displayTitle = getReadableNameFromIdentifier(
                            metadata = metadata,
                            userLocale = userLocale,
                            identifier = currentClaim.identifier
                        ),
                        path = disclosurePath,
                        isRequired = isRequired,
                        value = formattedValue.first().second //TODO
                    )
                } else {
                    DomainClaim.Claim.Group(
                        key = currentClaim.identifier,
                        displayTitle = getReadableNameFromIdentifier(
                            metadata = metadata,
                            userLocale = userLocale,
                            identifier = currentClaim.identifier
                        ),
                        path = disclosurePath,
                        items = formattedValue.map {
                            DomainClaim.Claim.Primitive(
                                key = it.first,
                                displayTitle = getReadableNameFromIdentifier(
                                    metadata = metadata,
                                    userLocale = userLocale,
                                    identifier = it.first
                                ),
                                path = disclosurePath,
                                isRequired = isRequired,
                                value = it.second //TODO
                            )
                        }
                    )
                }
                tree + newEntry
            }
        } else {
            tree // Already exists, return unchanged
        }
    } else {
        // Group node (Intermediate)
        val childClaims =
            (claims.find { key == it.identifier } as? SdJwtVcClaim)?.children ?: claims
        val updatedNode = if (existingNode is DomainClaim.Claim.Group) {
            // Update existing group by inserting the next path segment into its items
            existingNode.copy(
                items = insertPath(
                    tree = existingNode.items,
                    path = path.drop(1),
                    disclosurePath = disclosurePath,
                    claims = childClaims,
                    metadata = metadata,
                    resourceProvider = resourceProvider,
                    documentIdentifier = documentIdentifier,
                )
            )
        } else {
            if (currentClaim == null) { //TODO is this necessary?
                DomainClaim.NotAvailableClaim(
                    key = key,
                    displayTitle = getReadableNameFromIdentifier(
                        metadata = metadata,
                        userLocale = userLocale,
                        identifier = key
                    ),
                    path = disclosurePath,
                    value = resourceProvider.getString(R.string.request_element_identifier_not_available)
                )
            } else {
                // Create a new group and insert the next path segment
                DomainClaim.Claim.Group(
                    key = currentClaim.identifier,
                    displayTitle = getReadableNameFromIdentifier(
                        metadata = metadata,
                        userLocale = userLocale,
                        identifier = currentClaim.identifier
                    ),
                    path = path.take(path.size - 1),
                    items = insertPath(
                        tree = emptyList(),
                        path = path.drop(1),
                        disclosurePath = disclosurePath,
                        claims = childClaims,
                        metadata = metadata,
                        resourceProvider = resourceProvider,
                        documentIdentifier = documentIdentifier,
                    )
                )
            }
        }

        tree.filter { it.key != key } + updatedNode // Replace or add the updated node
    }
}

// Function to build the tree from a list of paths
fun buildDomainTree(
    paths: List<List<String>>,
    claims: List<DocumentClaim>,
    metadata: DocumentMetaData?,
    resourceProvider: ResourceProvider,
    documentIdentifier: DocumentIdentifier,
): List<DomainClaim> {
    return paths.fold(emptyList()) { acc, path ->
        insertPath(
            tree = acc,
            path = path,
            disclosurePath = path,
            claims = claims,
            metadata = metadata,
            resourceProvider = resourceProvider,
            documentIdentifier = documentIdentifier,
        )
    }
}