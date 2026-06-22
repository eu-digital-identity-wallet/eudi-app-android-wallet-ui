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

import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.commonfeature.extension.toExpandableListItems
import eu.europa.ec.commonfeature.extension.toSelectiveExpandableListItems
import eu.europa.ec.commonfeature.ui.request.model.DocumentFormatDomain
import eu.europa.ec.commonfeature.ui.request.model.DocumentPayloadDomain
import eu.europa.ec.commonfeature.ui.request.model.RequestCombinationUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi
import eu.europa.ec.commonfeature.util.transformPathsToDomainClaims
import eu.europa.ec.corelogic.extension.toClaimPaths
import eu.europa.ec.corelogic.model.ClaimDomain
import eu.europa.ec.corelogic.model.ClaimItemId
import eu.europa.ec.corelogic.model.ClaimPathDomain
import eu.europa.ec.corelogic.model.PresentationCombinationDomain
import eu.europa.ec.corelogic.model.PresentationMatchDomain
import eu.europa.ec.corelogic.model.PresentationSelectionDomain
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi

object RequestTransformer {

    /**
     * Projects the domain combinations into request-screen cards.
     * [claimsAreSelectable] = checkbox vs read-only leaves.
     */
    fun transformToCombinationsUi(
        storageDocuments: List<IssuedDocument>,
        resourceProvider: ResourceProvider,
        uuidProvider: UuidProvider,
        combinationsDomain: List<PresentationCombinationDomain>,
        claimsAreSelectable: Boolean,
    ): Result<List<RequestCombinationUi>> {
        return runCatching {
            combinationsDomain.map { combinationDomain ->
                val documentsDomain = transformToDomainItems(
                    storageDocuments = storageDocuments,
                    resourceProvider = resourceProvider,
                    uuidProvider = uuidProvider,
                    requestMatchesDomain = combinationDomain.matches,
                ).getOrThrow()

                RequestCombinationUi(
                    documents = transformToUiItems(
                        documentsDomain = documentsDomain,
                        resourceProvider = resourceProvider,
                        claimsAreSelectable = claimsAreSelectable,
                    ),
                    matches = combinationDomain.matches,
                )
            }
        }
    }


    /**
     * One [DocumentPayloadDomain] per match: look up the stored document, expand its claim paths,
     * keep the ones the verifier asked for (path matching per [ClaimPathDomain.matches]).
     */
    fun transformToDomainItems(
        storageDocuments: List<IssuedDocument>,
        resourceProvider: ResourceProvider,
        uuidProvider: UuidProvider,
        requestMatchesDomain: List<PresentationMatchDomain>,
    ): Result<List<DocumentPayloadDomain>> = runCatching {
        val resultList = mutableListOf<DocumentPayloadDomain>()

        requestMatchesDomain.forEach { presentationMatchDomain ->

            val storageDocument = storageDocuments.firstOrNull {
                it.id == presentationMatchDomain.documentId
            } ?: return@forEach

            val claimsPaths = storageDocument.data.claims.flatMap { claim ->
                claim.toClaimPaths()
            }

            val requestedItemsPaths = presentationMatchDomain.requestedClaims

            val filteredPaths = claimsPaths.filter { available ->
                requestedItemsPaths.any { requested ->
                    requested.matches(available)
                }
            }

            val domainClaims = transformPathsToDomainClaims(
                paths = filteredPaths,
                claims = storageDocument.data.claims,
                resourceProvider = resourceProvider,
                uuidProvider = uuidProvider
            )

            if (domainClaims.isNotEmpty()) {
                resultList.add(
                    DocumentPayloadDomain(
                        docName = storageDocument.name,
                        docId = storageDocument.id,
                        docFormatDomain = DocumentFormatDomain.getFormat(
                            format = storageDocument.format,
                        ),
                        docClaimsDomain = domainClaims,
                        queryId = presentationMatchDomain.queryId,
                    )
                )
            }
        }

        resultList
    }


    /**
     * Builds the request-screen rows.
     * [claimsAreSelectable] = selectable (checkbox) vs read-only
     * leaves; the document headers are the same either way.
     */
    fun transformToUiItems(
        documentsDomain: List<DocumentPayloadDomain>,
        resourceProvider: ResourceProvider,
        claimsAreSelectable: Boolean,
    ): List<RequestDocumentItemUi> {
        return documentsDomain.map { documentDomain ->
            RequestDocumentItemUi(
                domainPayload = documentDomain,
                headerUi = ExpandableListItemUi.NestedListItem(
                    header = ListItemDataUi(
                        itemId = ClaimItemId.DocumentHeader(
                            docId = documentDomain.docId,
                            queryId = documentDomain.queryId,
                        ).encode(),
                        mainContentData = ListItemMainContentDataUi.Text(text = documentDomain.docName),
                        supportingText = resourceProvider.getString(R.string.request_collapsed_supporting_text),
                        trailingContentData = ListItemTrailingContentDataUi.Icon(
                            iconData = AppIcons.KeyboardArrowDown
                        )
                    ),
                    nestedItems = if (claimsAreSelectable) {
                        documentDomain.toSelectiveExpandableListItems()
                    } else {
                        documentDomain.toExpandableListItems()
                    },
                    isExpanded = false,
                ),
            )
        }
    }

    /**
     * Inverse of the projection: the user's kept rows → [PresentationSelectionDomain]s, one per
     * rendered document (matched back by `(documentId, queryId)`; documents with no match, or
     * nothing kept, are dropped). When [claimsAreSelectable] is false each document discloses the verifier's full set.
     */
    fun createSelectionsDomain(
        documentItemsUi: List<RequestDocumentItemUi>,
        matchesDomain: List<PresentationMatchDomain>,
        claimsAreSelectable: Boolean,
    ): List<PresentationSelectionDomain> {
        // key by (documentId, queryId): one doc can match several DCQL queries, so documentId alone isn't unique
        val matchesDomainByKey = matchesDomain.associateBy { it.documentId to it.queryId }

        return documentItemsUi.mapNotNull { documentUi ->
            val matchKey = documentUi.domainPayload.docId to documentUi.domainPayload.queryId
            val matchDomain = matchesDomainByKey[matchKey] ?: return@mapNotNull null

            if (claimsAreSelectable) {
                documentUi.toSelectionDomainOrNull(matchDomain = matchDomain)
            } else {
                matchDomain.toFullSelectionDomain()
            }
        }
    }

    private fun PresentationMatchDomain.toFullSelectionDomain(): PresentationSelectionDomain {
        return PresentationSelectionDomain(
            documentId = documentId,
            credentialId = credentialId,
            queryId = queryId,
            selectedClaims = requestedClaims.toSet(),
        )
    }

    /**
     * One rendered card → its selection, or null if the user kept nothing. Checked-row ids are
     * resolved to storage paths by re-encoding the stored leaves (never by parsing the id).
     */
    private fun RequestDocumentItemUi.toSelectionDomainOrNull(
        matchDomain: PresentationMatchDomain,
    ): PresentationSelectionDomain? {
        val docId = domainPayload.docId
        val queryId = domainPayload.queryId

        // ids of the checked rows
        val checkedIds = headerUi.nestedItems
            .flatMap { item -> item.collectCheckedItemIds() }
            .toSet()
        if (checkedIds.isEmpty()) return null

        // rebuild id -> storage-path by re-encoding this doc's stored leaves
        val storagePathById = domainPayload.docClaimsDomain
            .flatMap { claim -> claim.leafStoragePaths() }
            .associateBy { path ->
                ClaimItemId.Claim(
                    docId = docId,
                    queryId = queryId,
                    path = path,
                ).encode()
            }

        // checked ids -> their storage paths
        val selectedStoragePaths = checkedIds.mapNotNull { id -> storagePathById[id] }

        // keep the requested paths a kept leaf satisfies; wildcard match: [nationalities,null] <- [nationalities,0]
        val selectedClaims = matchDomain.requestedClaims
            .filter { requested -> selectedStoragePaths.any { kept -> requested.matches(kept) } }
            .toSet()
        if (selectedClaims.isEmpty()) return null

        return PresentationSelectionDomain(
            documentId = matchDomain.documentId,
            credentialId = matchDomain.credentialId,
            queryId = matchDomain.queryId,
            selectedClaims = selectedClaims,
        )
    }
}

/** Storage paths of the Primitive leaves; group nodes contribute nothing. */
private fun ClaimDomain.leafStoragePaths(): List<ClaimPathDomain> {
    return when (this) {
        is ClaimDomain.Primitive -> {
            listOf(path)
        }

        is ClaimDomain.Group -> {
            items.flatMap { claimDomain ->
                claimDomain.leafStoragePaths()
            }
        }
    }
}

/** Depth-first ids of the checked leaf rows; group nodes carry no checkbox. */
private fun ExpandableListItemUi.collectCheckedItemIds(): List<String> {
    return when (this) {
        is ExpandableListItemUi.SingleListItem -> {
            val checkbox = header.trailingContentData as? ListItemTrailingContentDataUi.Checkbox
            if (checkbox?.checkboxData?.isChecked == true) {
                listOf(header.itemId)
            } else {
                emptyList()
            }
        }

        is ExpandableListItemUi.NestedListItem -> {
            nestedItems.flatMap { item ->
                item.collectCheckedItemIds()
            }
        }
    }
}