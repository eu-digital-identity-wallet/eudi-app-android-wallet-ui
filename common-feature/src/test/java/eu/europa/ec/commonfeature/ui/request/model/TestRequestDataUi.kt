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

package eu.europa.ec.commonfeature.ui.request.model

import eu.europa.ec.testfeature.util.mockedPidDocName
import eu.europa.ec.testfeature.util.mockedPidId
import eu.europa.ec.testfeature.util.mockedRequestCollapsedSupportingText
import eu.europa.ec.testfeature.util.mockedTransactionQueryId
import eu.europa.ec.testfeature.util.mockedValidPidWithBasicFieldsRequestMatch
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemSupportingContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
import org.junit.Test

class TestRequestDataUi {

    //region toggleTransactionDataExpansion

    // Case 1:
    // 1. A single combination contains a collapsed transaction section and credential data.
    //
    // Case 1 Expected Result:
    // Expansion changes the flag and arrow together; collapsing restores the complete original model.
    @Test
    fun `Given Case 1, When toggleTransactionDataExpansion is called, Then Case 1 Expected Result is returned`() {
        // Given
        val original = RequestDataUi.Single(mockedCombination(mockedFirstSectionId))

        // When
        val expanded =
            original.toggleTransactionDataExpansion(mockedFirstSectionId, mockedFirstSectionId)
        val collapsed =
            expanded.toggleTransactionDataExpansion(mockedFirstSectionId, mockedFirstSectionId)

        // Then
        val originalSection = original.combination.transactionData!!
        val expandedSection = expanded.selectedCombination!!.transactionData!!
        assertEquals(
            originalSection.copy(
                details = originalSection.details.copy(
                    isExpanded = true,
                    header = originalSection.details.header.copy(
                        trailingContentData = ListItemTrailingContentDataUi.Icon(
                            iconData = AppIcons.KeyboardArrowUp,
                        ),
                    ),
                ),
            ),
            expandedSection,
        )
        assertEquals(original.selectedDocuments, expanded.selectedDocuments)
        assertSame(original.combination.matches, expanded.selectedCombination!!.matches)
        assertEquals(original, collapsed)
    }

    // Case 2:
    // 1. Two cards have separate sections; the user expands each in turn.
    //
    // Case 2 Expected Result:
    // Switching cards preserves their independent expansion and the selected index.
    @Test
    fun `Given Case 2, When toggleTransactionDataExpansion is called, Then Case 2 Expected Result is returned`() {
        // Given
        val original = mockedMultiple()
        val firstExpanded =
            original.toggleTransactionDataExpansion(mockedFirstSectionId, mockedFirstSectionId)
                    as RequestDataUi.Multiple

        // When
        val secondExpanded = firstExpanded.copy(selectedIndex = 1)
            .toggleTransactionDataExpansion(
                mockedSecondSectionId,
                mockedSecondSectionId
            ) as RequestDataUi.Multiple
        val firstCollapsed = secondExpanded.copy(selectedIndex = 0)
            .toggleTransactionDataExpansion(
                mockedFirstSectionId,
                mockedFirstSectionId
            ) as RequestDataUi.Multiple

        // Then
        assertEquals(0, firstExpanded.selectedIndex)
        assertTrue(firstExpanded.combinations[0].transactionData!!.details.isExpanded)
        assertSame(original.combinations[1], firstExpanded.combinations[1])
        assertEquals(1, secondExpanded.selectedIndex)
        assertTrue(secondExpanded.combinations.all { combination ->
            combination.transactionData!!.details.isExpanded
        })
        assertFalse(firstCollapsed.combinations[0].transactionData!!.details.isExpanded)
        assertSame(secondExpanded.combinations[1], firstCollapsed.combinations[1])
        assertEquals(original.selectedDocuments, firstCollapsed.selectedDocuments)
    }

    // Case 3:
    // 1. The first card is selected and receives an unselected-card or stale section ID.
    //
    // Case 3 Expected Result:
    // The original model is returned without changing either card.
    @Test
    fun `Given Case 3, When toggleTransactionDataExpansion is called, Then Case 3 Expected Result is returned`() {
        // Given
        val original = mockedMultiple()

        // When
        val unselected =
            original.toggleTransactionDataExpansion(mockedSecondSectionId, mockedSecondSectionId)
        val stale = original.toggleTransactionDataExpansion(
            mockedReplacementSectionId,
            mockedReplacementSectionId
        )

        // Then
        assertSame(original, unselected)
        assertSame(original, stale)
    }

    // Case 4:
    // 1. Initial, NoData and a combination without transaction data have no section.
    //
    // Case 4 Expected Result:
    // Expansion leaves every model unchanged.
    @Test
    fun `Given Case 4, When toggleTransactionDataExpansion is called, Then Case 4 Expected Result is returned`() {
        // Given
        val states = listOf(
            RequestDataUi.Initial,
            RequestDataUi.NoData,
            RequestDataUi.Single(
                mockedCombination(mockedFirstSectionId).copy(transactionData = null),
            ),
        )

        // When
        val results = states.map { state ->
            state.toggleTransactionDataExpansion(
                mockedFirstSectionId,
                mockedFirstSectionId
            )
        }

        // Then
        states.zip(results).forEach { (original, result) -> assertSame(original, result) }
    }

    // Case 5:
    // 1. A transaction section contains leaves and groups nested two levels deep.
    //
    // Case 5 Expected Result:
    // Only the requested group's flag and arrow change; collapsing restores the original tree.
    @Test
    fun `Given Case 5, When toggleTransactionDataExpansion is called, Then Case 5 Expected Result is returned`() {
        // Given
        val original = RequestDataUi.Single(mockedNestedCombination(mockedFirstSectionId))
        val originalSection = original.combination.transactionData!!
        val originalGroup =
            originalSection.details.nestedItems[1] as ExpandableListItemUi.NestedListItem
        val originalNested = originalGroup.nestedItems[1] as ExpandableListItemUi.NestedListItem

        // When
        val expanded =
            original.toggleTransactionDataExpansion(mockedFirstSectionId, mockedNestedGroupId)
        val collapsed =
            expanded.toggleTransactionDataExpansion(mockedFirstSectionId, mockedNestedGroupId)

        // Then
        val section = expanded.selectedCombination!!.transactionData!!
        val group = section.details.nestedItems[1] as ExpandableListItemUi.NestedListItem
        assertEquals(originalSection.details.header, section.details.header)
        assertEquals(originalGroup.header, group.header)
        assertEquals(
            originalNested.copy(
                isExpanded = true,
                header = originalNested.header.copy(
                    trailingContentData = ListItemTrailingContentDataUi.Icon(AppIcons.KeyboardArrowUp),
                ),
            ),
            group.nestedItems[1],
        )
        assertEquals(originalSection.details.nestedItems[2], section.details.nestedItems[2])
        assertSame(original.selectedDocuments, expanded.selectedDocuments)
        assertSame(original.combination.matches, expanded.selectedCombination!!.matches)
        assertEquals(original, collapsed)
    }

    // Case 6:
    // 1. Expansion targets a leaf, an unknown item or a group hidden by a collapsed ancestor.
    //
    // Case 6 Expected Result:
    // These targets leave the original state unchanged.
    @Test
    fun `Given Case 6, When toggleTransactionDataExpansion is called, Then Case 6 Expected Result is returned`() {
        // Given
        val original = RequestDataUi.Single(mockedNestedCombination(mockedFirstSectionId))
        val collapsedParent =
            original.toggleTransactionDataExpansion(mockedFirstSectionId, mockedParentGroupId)

        // When
        val leaf = original.toggleTransactionDataExpansion(
            mockedFirstSectionId,
            "$mockedFirstSectionId/location"
        )
        val unknown = original.toggleTransactionDataExpansion(
            mockedFirstSectionId,
            "$mockedFirstSectionId/unknown"
        )
        val hidden = collapsedParent.toggleTransactionDataExpansion(
            mockedFirstSectionId,
            mockedNestedGroupId
        )

        // Then
        assertSame(original, leaf)
        assertSame(original, unknown)
        assertSame(collapsedParent, hidden)
    }

    // Case 7:
    // 1. A nested group is expanded, then its outer section is closed and reopened.
    // 2. The user switches to another combination and returns.
    //
    // Case 7 Expected Result:
    // Nested expansion survives both transitions without changing the other combination.
    @Test
    fun `Given Case 7, When toggleTransactionDataExpansion is called, Then Case 7 Expected Result is returned`() {
        // Given
        val original = RequestDataUi.Multiple(
            combinations = listOf(
                mockedNestedCombination(mockedFirstSectionId),
                mockedCombination(mockedSecondSectionId),
            ),
            selectedIndex = 0,
        )
        val expanded =
            original.toggleTransactionDataExpansion(mockedFirstSectionId, mockedNestedGroupId)
                    as RequestDataUi.Multiple

        // When
        val closed =
            expanded.toggleTransactionDataExpansion(mockedFirstSectionId, mockedFirstSectionId)
                    as RequestDataUi.Multiple
        val switchedBack = closed.copy(selectedIndex = 1).copy(selectedIndex = 0)
        val reopened =
            switchedBack.toggleTransactionDataExpansion(mockedFirstSectionId, mockedFirstSectionId)

        // Then
        assertEquals(expanded, reopened)
        assertSame(original.combinations[1], closed.combinations[1])
    }

    //endregion

    //region transactionDocumentUrl

    // Case 1:
    // 1. The selected section is expanded and its document action has a prepared URL.
    //
    // Case 1 Expected Result:
    // The exact URL is returned and the UI model is unchanged.
    @Test
    fun `Given Case 1, When transactionDocumentUrl is called, Then Case 1 Expected Result is returned`() {
        // Given
        val original = RequestDataUi.Single(mockedCombination(mockedFirstSectionId))
            .toggleTransactionDataExpansion(mockedFirstSectionId, mockedFirstSectionId)
        val originalSection = original.selectedCombination!!.transactionData

        // When
        val url = original.transactionDocumentUrl(
            sectionId = mockedFirstSectionId,
            itemId = "$mockedFirstSectionId/open",
        )

        // Then
        assertEquals(mockedDocumentUrl, url)
        assertSame(originalSection, original.selectedCombination!!.transactionData)
    }

    // Case 2:
    // 1. An expanded section contains information rows and one allowed document action.
    //
    // Case 2 Expected Result:
    // Information rows, the header, unknown IDs and a mismatched section resolve no URL.
    @Test
    fun `Given Case 2, When transactionDocumentUrl is called, Then Case 2 Expected Result is returned`() {
        // Given
        val original = RequestDataUi.Single(mockedCombination(mockedFirstSectionId))
            .toggleTransactionDataExpansion(mockedFirstSectionId, mockedFirstSectionId)
        val nonActions = listOf(
            "$mockedFirstSectionId/location",
            "$mockedFirstSectionId/response",
            "$mockedFirstSectionId/otp",
            "$mockedFirstSectionId/hash",
            "$mockedFirstSectionId/unknown",
            mockedFirstSectionId,
        )

        // When
        val targets = nonActions.map { itemId ->
            original.transactionDocumentUrl(sectionId = mockedFirstSectionId, itemId = itemId)
        }
        val mismatched = original.transactionDocumentUrl(
            sectionId = mockedSecondSectionId,
            itemId = "$mockedFirstSectionId/open",
        )

        // Then
        assertTrue(targets.all { target -> target == null })
        assertNull(mismatched)
    }

    // Case 3:
    // 1. A link belongs to a collapsed, unselected or replaced section, or there is no section.
    //
    // Case 3 Expected Result:
    // The event cannot launch a document from those states.
    @Test
    fun `Given Case 3, When transactionDocumentUrl is called, Then Case 3 Expected Result is returned`() {
        // Given
        val firstExpanded = mockedMultiple().toggleTransactionDataExpansion(
            mockedFirstSectionId,
            mockedFirstSectionId
        )
                as RequestDataUi.Multiple
        val states = listOf(
            mockedMultiple(),
            firstExpanded.copy(selectedIndex = 1),
            RequestDataUi.Single(mockedCombination(mockedReplacementSectionId))
                .toggleTransactionDataExpansion(
                    mockedReplacementSectionId,
                    mockedReplacementSectionId
                ),
            RequestDataUi.Initial,
            RequestDataUi.NoData,
            RequestDataUi.Single(
                mockedCombination(mockedFirstSectionId).copy(transactionData = null),
            ),
        )

        // When
        val targets = states.map { state ->
            state.transactionDocumentUrl(
                sectionId = mockedFirstSectionId,
                itemId = "$mockedFirstSectionId/open",
            )
        }

        // Then
        assertTrue(targets.all { target -> target == null })
    }

    // Case 4:
    // 1. A document link is inside two nested groups.
    //
    // Case 4 Expected Result:
    // Its exact URL resolves only while every ancestor is expanded.
    @Test
    fun `Given Case 4, When transactionDocumentUrl is called, Then Case 4 Expected Result is returned`() {
        // Given
        val original = RequestDataUi.Single(mockedNestedCombination(mockedFirstSectionId))
        val expanded =
            original.toggleTransactionDataExpansion(mockedFirstSectionId, mockedNestedGroupId)
        val closedParent =
            expanded.toggleTransactionDataExpansion(mockedFirstSectionId, mockedParentGroupId)
        val reopenedParent =
            closedParent.toggleTransactionDataExpansion(mockedFirstSectionId, mockedParentGroupId)
        val closedSection =
            expanded.toggleTransactionDataExpansion(mockedFirstSectionId, mockedFirstSectionId)

        // When
        val urls =
            listOf(original, expanded, closedParent, reopenedParent, closedSection).map { state ->
                state.transactionDocumentUrl(
                    sectionId = mockedFirstSectionId,
                    itemId = mockedNestedActionId
                )
            }

        // Then
        assertEquals(listOf(null, mockedDocumentUrl, null, mockedDocumentUrl, null), urls)
    }

    // Case 5:
    // 1. A URL map includes a group header and an item absent from the tree.
    //
    // Case 5 Expected Result:
    // Neither target can open a document; only visible single items are eligible.
    @Test
    fun `Given Case 5, When transactionDocumentUrl is called, Then Case 5 Expected Result is returned`() {
        // Given
        val combination = mockedNestedCombination(mockedFirstSectionId)
        val section = combination.transactionData!!
        val unknownId = "$mockedFirstSectionId/unknown"
        val state = RequestDataUi.Single(
            combination.copy(
                transactionData = section.copy(
                    documentUrlsByItemId = section.documentUrlsByItemId + mapOf(
                        mockedParentGroupId to mockedDocumentUrl,
                        unknownId to mockedDocumentUrl,
                    ),
                ),
            ),
        )

        // When
        val headerUrl = state.transactionDocumentUrl(mockedFirstSectionId, mockedParentGroupId)
        val unknownUrl = state.transactionDocumentUrl(mockedFirstSectionId, unknownId)

        // Then
        assertNull(headerUrl)
        assertNull(unknownUrl)
    }

    //endregion

    //region withSelectedDocuments

    // Case 1:
    // 1. The single combination has an expanded transaction section.
    //
    // Case 1 Expected Result:
    // A credential update preserves its transaction section and original match list.
    @Test
    fun `Given Case 1, When withSelectedDocuments is called, Then Case 1 Expected Result is returned`() {
        // Given
        val original = RequestDataUi.Single(mockedCombination(mockedFirstSectionId))
            .toggleTransactionDataExpansion(mockedFirstSectionId, mockedFirstSectionId)
        val updatedDocuments = listOf(
            mockedDocument.copy(headerUi = mockedDocument.headerUi.copy(isExpanded = true)),
        )

        // When
        val updated = original.withSelectedDocuments(updatedDocuments)

        // Then
        assertEquals(updatedDocuments, updated.selectedDocuments)
        assertSame(
            original.selectedCombination!!.transactionData,
            updated.selectedCombination!!.transactionData
        )
        assertSame(original.selectedCombination!!.matches, updated.selectedCombination!!.matches)
    }

    // Case 2:
    // 1. The second card is selected and both cards have expanded transaction sections.
    //
    // Case 2 Expected Result:
    // A credential update affects only that card's documents; both transaction sections survive.
    @Test
    fun `Given Case 2, When withSelectedDocuments is called, Then Case 2 Expected Result is returned`() {
        // Given
        val firstExpanded = mockedMultiple().toggleTransactionDataExpansion(
            mockedFirstSectionId,
            mockedFirstSectionId
        )
                as RequestDataUi.Multiple
        val original = firstExpanded.copy(selectedIndex = 1)
            .toggleTransactionDataExpansion(
                mockedSecondSectionId,
                mockedSecondSectionId
            ) as RequestDataUi.Multiple
        val updatedDocuments = emptyList<RequestDocumentItemUi>()

        // When
        val updated = original.withSelectedDocuments(updatedDocuments) as RequestDataUi.Multiple

        // Then
        assertEquals(1, updated.selectedIndex)
        assertEquals(updatedDocuments, updated.selectedDocuments)
        assertSame(original.combinations[0], updated.combinations[0])
        assertSame(
            original.combinations[1].transactionData,
            updated.combinations[1].transactionData
        )
        assertSame(original.combinations[1].matches, updated.combinations[1].matches)
    }

    //endregion

    //region helpers

    private fun mockedMultiple(): RequestDataUi.Multiple = RequestDataUi.Multiple(
        combinations = listOf(
            mockedCombination(mockedFirstSectionId),
            mockedCombination(mockedSecondSectionId),
        ),
        selectedIndex = 0,
    )

    private fun mockedCombination(sectionId: String): RequestCombinationUi = RequestCombinationUi(
        documents = listOf(mockedDocument),
        matches = listOf(mockedValidPidWithBasicFieldsRequestMatch),
        transactionData = RequestTransactionDataUi(
            title = "Data to be signed",
            details = ExpandableListItemUi.NestedListItem(
                header = ListItemDataUi(
                    itemId = sectionId,
                    mainContentData = ListItemMainContentDataUi.Text("Signature details"),
                    supportingContentData = ListItemSupportingContentDataUi.Text(
                        mockedRequestCollapsedSupportingText,
                    ),
                    trailingContentData = ListItemTrailingContentDataUi.Icon(AppIcons.KeyboardArrowDown),
                ),
                nestedItems = listOf(
                    mockedRow("$sectionId/location", mockedDocumentUrl),
                    mockedRow("$sectionId/response", mockedResponseUri),
                    mockedRow("$sectionId/otp", "000123"),
                    mockedRow("$sectionId/hash", "YWJjZA=="),
                    mockedRow("$sectionId/open", "Open document"),
                ),
                isExpanded = false,
            ),
            documentUrlsByItemId = mapOf("$sectionId/open" to mockedDocumentUrl),
        ),
    )

    private fun mockedNestedCombination(sectionId: String): RequestCombinationUi {
        val combination = mockedCombination(sectionId)
        val section = combination.transactionData!!
        val groupId = "$sectionId/group"
        val nestedId = "$groupId/nested"
        val actionId = "$nestedId/open"
        return combination.copy(
            transactionData = section.copy(
                details = mockedGroup(
                    itemId = sectionId,
                    isExpanded = true,
                    nestedItems = listOf(
                        mockedRow("$sectionId/location", mockedDocumentUrl),
                        mockedGroup(
                            itemId = groupId,
                            isExpanded = true,
                            nestedItems = listOf(
                                mockedRow("$groupId/name", mockedPidDocName),
                                mockedGroup(
                                    itemId = nestedId,
                                    isExpanded = false,
                                    nestedItems = listOf(mockedRow(actionId, "Open document")),
                                ),
                            ),
                        ),
                        mockedGroup(
                            itemId = "$sectionId/sibling",
                            isExpanded = false,
                            nestedItems = emptyList(),
                        ),
                    ),
                ),
                documentUrlsByItemId = mapOf(actionId to mockedDocumentUrl),
            ),
        )
    }

    private fun mockedGroup(
        itemId: String,
        isExpanded: Boolean,
        nestedItems: List<ExpandableListItemUi>,
    ): ExpandableListItemUi.NestedListItem = ExpandableListItemUi.NestedListItem(
        header = ListItemDataUi(
            itemId = itemId,
            mainContentData = ListItemMainContentDataUi.Text(itemId),
            trailingContentData = ListItemTrailingContentDataUi.Icon(
                if (isExpanded) AppIcons.KeyboardArrowUp else AppIcons.KeyboardArrowDown,
            ),
        ),
        nestedItems = nestedItems,
        isExpanded = isExpanded,
    )

    private fun mockedRow(itemId: String, text: String): ExpandableListItemUi.SingleListItem =
        ExpandableListItemUi.SingleListItem(
            header = ListItemDataUi(
                itemId = itemId,
                mainContentData = ListItemMainContentDataUi.Text(text),
            ),
        )

    private val mockedFirstSectionId = "transaction-data:request-id:0"
    private val mockedSecondSectionId = "transaction-data:request-id:1"
    private val mockedReplacementSectionId = "transaction-data:replacement-id:0"
    private val mockedParentGroupId = "$mockedFirstSectionId/group"
    private val mockedNestedGroupId = "$mockedParentGroupId/nested"
    private val mockedNestedActionId = "$mockedNestedGroupId/open"
    private val mockedDocumentUrl = "https://documents.example.org/contract.pdf?token=a%2Fb#page=1"
    private val mockedResponseUri = "https://signer.example.org/response"
    private val mockedDocument = RequestDocumentItemUi(
        domainPayload = DocumentPayloadDomain(
            docName = mockedPidDocName,
            docId = mockedPidId,
            docFormatDomain = DocumentFormatDomain.MsoMdoc,
            docClaimsDomain = emptyList(),
            queryId = mockedTransactionQueryId,
        ),
        headerUi = ExpandableListItemUi.NestedListItem(
            header = ListItemDataUi(
                itemId = mockedPidId,
                mainContentData = ListItemMainContentDataUi.Text(mockedPidDocName),
            ),
            nestedItems = emptyList(),
            isExpanded = false,
        ),
    )

    //endregion
}