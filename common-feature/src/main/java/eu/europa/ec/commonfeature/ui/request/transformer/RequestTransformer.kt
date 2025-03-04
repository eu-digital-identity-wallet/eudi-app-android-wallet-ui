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
import eu.europa.ec.commonfeature.util.docNamespace
import eu.europa.ec.commonfeature.util.getReadableNameFromIdentifier
import eu.europa.ec.commonfeature.util.keyIsPortrait
import eu.europa.ec.commonfeature.util.keyIsSignature
import eu.europa.ec.commonfeature.util.parseKeyValueUi
import eu.europa.ec.corelogic.model.DocumentIdentifier
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

fun IssuedDocument.findSdJwtVcClaimFromPath(
    paths: List<List<String>>,
    metadata: DocumentMetaData?,
    resourceProvider: ResourceProvider,
    documentIdentifier: DocumentIdentifier
): List<DomainClaim> {
    if (paths.isEmpty()) return emptyList()

    val groupedClaims = mutableMapOf<String, MutableList<DomainClaim>>()

    paths.forEach { path ->
        if (path.isEmpty()) return@forEach

        var currentClaim: SdJwtVcClaim? = data.claims
            .filterIsInstance<SdJwtVcClaim>()
            .firstOrNull { it.identifier == path.first() }

        if (currentClaim == null) {
            groupedClaims.getOrPut(path.first()) { mutableListOf() }.add(
                DomainClaim.NotAvailableClaim(
                    key = path.last(),
                    displayTitle = getReadableNameFromIdentifier(
                        metadata = metadata,
                        userLocale = resourceProvider.getLocale(),
                        identifier = path.last()
                    ),
                    path = path,
                    value = resourceProvider.getString(R.string.request_element_identifier_not_available)
                )
            )
            return@forEach
        }

        val collectedClaims = mutableListOf<SdJwtVcClaim>()
        var parentClaim: SdJwtVcClaim? = currentClaim

        for (nextId in path.drop(1)) {
            val nextClaim = parentClaim?.children?.firstOrNull { it.identifier == nextId }
            if (nextClaim == null) {
                groupedClaims.getOrPut(path.first()) { mutableListOf() }.add(
                    DomainClaim.NotAvailableClaim(
                        key = nextId,
                        displayTitle = getReadableNameFromIdentifier(
                            metadata = metadata,
                            userLocale = resourceProvider.getLocale(),
                            identifier = path.last()
                        ),
                        path = path,
                        value = resourceProvider.getString(R.string.request_element_identifier_not_available)
                    )
                )
                return@forEach
            }
            collectedClaims.add(parentClaim)
            parentClaim = nextClaim
        }

        val formattedValue = buildString {
            parseKeyValueUi(
                item = parentClaim?.value ?: "",
                groupIdentifierKey = parentClaim?.identifier ?: "",
                resourceProvider = resourceProvider,
                allItems = this
            )
        }

        val isRequired = getMandatoryFields(
            documentIdentifier = documentIdentifier
        ).contains(parentClaim!!.identifier)

        val primitiveClaim = DomainClaim.Claim.Primitive(
            key = parentClaim.identifier,
            displayTitle = getReadableNameFromIdentifier(
                metadata = metadata,
                userLocale = resourceProvider.getLocale(),
                identifier = parentClaim.identifier
            ),
            path = path,
            isRequired = isRequired,
            value = formattedValue
        )

        val groupKey = collectedClaims.firstOrNull()?.identifier ?: path.first()
        groupedClaims.getOrPut(groupKey) { mutableListOf() }.add(primitiveClaim)
    }

    return groupedClaims.map { (key, claims) ->
        val parentClaim =
            data.claims.filterIsInstance<SdJwtVcClaim>().firstOrNull { it.identifier == key }

        if (claims.size > 1 || parentClaim?.children?.isNotEmpty() == true) {
            DomainClaim.Claim.Group(
                key = key,
                displayTitle = getReadableNameFromIdentifier(
                    metadata = metadata,
                    userLocale = resourceProvider.getLocale(),
                    identifier = key
                ),
                path = listOf(key),
                items = claims
            )
        } else {
            claims.first()
        }
    }
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
            val sdJwtVcPaths = requestDocument.requestedItems.keys
                .filterIsInstance<SdJwtVcItem>()
                .map { it.path }

            val domains = buildDomainTree(sdJwtVcPaths, storageDocument.data.claims)
            println(domains)

            if (sdJwtVcPaths.isNotEmpty()) {
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
                        itemId = path.joinToString(separator = PATH_SEPARATOR),
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
                        itemId = path.joinToString(separator = PATH_SEPARATOR),
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
                        itemId = "", // TODO revisit
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
            it.domainPayload to it.claimsUi.flatMap { uiItem -> uiItem.collectSingles() }
        }

        // Convert to the format required by DisclosedDocuments
        val disclosedDocuments =
            groupedByDocument.map { (documentPayload, selectedItemsForDocument) ->

                val disclosedItems = selectedItemsForDocument.map { selectedItem ->

                    when (documentPayload.docNamespace) {
                        null -> SdJwtVcItem(
                            selectedItem.collapsed.itemId.split(PATH_SEPARATOR) //or just have/carry the DocItem in the class and use it here as is
                        )

                        else -> MsoMdocItem(
                            namespace = documentPayload.docNamespace,
                            elementIdentifier = selectedItem.collapsed.itemId
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
        is MsoMdocItem -> return listOf(this.namespace, this.elementIdentifier)
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
    claims: List<DocumentClaim>
): List<DomainClaim> {
    if (path.isEmpty()) return tree

    val key = path.first()
    val existingNode = tree.find { it.key == key }

    var currentClaim: SdJwtVcClaim? = claims.filterIsInstance<SdJwtVcClaim>()
        .firstOrNull {
            it.identifier == key
        }

    println(currentClaim)

    return if (path.size == 1) {
        // Leaf node (Primitive)
        if (existingNode == null) {
            if (currentClaim == null) {
                tree + DomainClaim.NotAvailableClaim(
                    key = key,
                    displayTitle = "Not available",
                    path = disclosurePath,
                    value = "not available"
                )
            } else {
                tree + DomainClaim.Claim.Primitive(
                    key = currentClaim.identifier,
                    displayTitle = "${currentClaim.identifier} TITLE",
                    path = disclosurePath,
                    isRequired = true,
                    value = "${currentClaim.identifier} FORMATTEd"
                )
            }
        } else {
            tree // Already exists, return unchanged
        }
    } else {
        // Group node (Intermediate)
        val castClaims = (claims.first { key == it.identifier } as? SdJwtVcClaim)?.children ?: claims
        val updatedNode = if (existingNode is DomainClaim.Claim.Group) {
            // Update existing group by inserting the next path segment into its items
            existingNode.copy(items = insertPath(existingNode.items, path.drop(1), disclosurePath, castClaims))
        } else {
            // Create a new group and insert the next path segment
            DomainClaim.Claim.Group(
                key = key,
                displayTitle = key,
                path = path.take(path.size - 1),
                items = insertPath(emptyList(), path.drop(1), disclosurePath, castClaims)
            )
        }

        tree.filter { it.key != key } + updatedNode // Replace or add the updated node
    }
}

// Function to build the tree from a list of paths
fun buildDomainTree(data: List<List<String>>, claims: List<DocumentClaim>): List<DomainClaim> {
    return data.fold(emptyList()) { acc, path -> insertPath(acc, path, path, claims) }
}