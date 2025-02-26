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
import eu.europa.ec.commonfeature.util.keyIsPortrait
import eu.europa.ec.commonfeature.util.keyIsSignature
import eu.europa.ec.commonfeature.util.parseClaimsToDomain
import eu.europa.ec.corelogic.extension.getLocalizedClaimName
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.model.toDocumentIdentifier
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.DocItem
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.device.MsoMdocItem
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.ElementIdentifier
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.NameSpace
import eu.europa.ec.eudi.wallet.document.format.DocumentClaim
import eu.europa.ec.eudi.wallet.document.format.MsoMdocClaim
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcClaim
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
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

    sealed class Claim : DomainClaim() {
        abstract val path: List<String>

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
            val isRequired: Boolean,
            val value: String,
        ) : Claim()
    }

    data class NotAvailableClaim(
        override val key: ElementIdentifier,
        override val displayTitle: String,
    ) : DomainClaim()
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

            val requestDocumentClaims: MutableList<DomainClaim> = mutableListOf()

            requestDocument.requestedItems.keys.forEach { docItem ->

                val documentClaim = storageDocument.findClaimFromDocItem(docItem)
                val identifier = documentClaim?.identifier

                val isRequired = getMandatoryFields(
                    documentIdentifier = storageDocument.toDocumentIdentifier()
                ).contains(identifier)

                val display = storageDocument.metadata?.claims
                    ?.find { it.name.name == identifier }
                    ?.display

                val readableName: (String) -> String = { identifier ->
                    display.getLocalizedClaimName(
                        userLocale = userLocale,
                        fallback = identifier
                    )
                }

                val value = parseClaimsToDomain(
                    coreClaim = documentClaim,
                    readableName = readableName,
                    groupIdentifierKey = identifier,
                    resourceProvider = resourceProvider,
                    path = docItem.toPath(),
                    isRequired = isRequired
                )

                requestDocumentClaims.add(value)
            }

            resultList.add(
                DocumentPayloadDomain(
                    docName = docName,
                    docId = docId,
                    docNamespace = docNamespace,
                    docClaimsDomain = requestDocumentClaims.sortedBy { (it as? DomainClaim.Claim)?.displayTitle?.lowercase() }, //TODO
                )
            )
        }

        return@runCatching resultList
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
                            CheckboxData(
                                isChecked = true,
                                enabled = isRequired
                            )
                        )
                    )
                )
            }

            is DomainClaim.NotAvailableClaim -> {
                ExpandableListItem.SingleListItemData(
                    collapsed = ListItemData(
                        itemId = "", // TODO revisit
                        mainContentData = ListItemMainContentData.Text(text = displayTitle),
                        overlineText = key,
                        trailingContentData = ListItemTrailingContentData.Checkbox(
                            CheckboxData(
                                isChecked = false,
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
            data.claims.filterIsInstance<SdJwtVcClaim>()
                .firstOrNull { it.identifier == path.first() }
        }
    }
}