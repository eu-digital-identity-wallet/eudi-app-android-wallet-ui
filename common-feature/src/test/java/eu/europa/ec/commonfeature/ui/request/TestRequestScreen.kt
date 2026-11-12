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

package eu.europa.ec.commonfeature.ui.request

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import eu.europa.ec.commonfeature.ui.request.model.DocumentFormatDomain
import eu.europa.ec.commonfeature.ui.request.model.DocumentPayloadDomain
import eu.europa.ec.commonfeature.ui.request.model.RequestCombinationUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDataUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi
import eu.europa.ec.commonfeature.ui.request.model.RequestTransactionDataUi
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.wrap.CheckboxDataUi
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests that only the selected option of a request takes clicks on its content
 * ([DisplayCombinationCards]): an option that is not selected is read-only, so accessibility
 * services cannot act on what its card keeps from touch. Also tests that a claim the user can
 * select reads as a checkbox with its checked or unchecked state.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h1600dp")
class TestRequestScreen {

    @get:Rule
    val composeRule = createComposeRule()

    /**
     * Two options, the first selected: its document card takes a click, the second option's
     * does not.
     */
    @Test
    fun `Given two options, When the first is selected, Then only its document takes clicks`() {
        setCards(selectedIndex = 0)

        composeRule.onNodeWithText(documentName(0)).assertHasClickAction()
        composeRule.onNodeWithText(documentName(1)).assertHasNoClickAction()
    }

    /**
     * The same two options with the second selected: the clicks follow the selection.
     */
    @Test
    fun `Given two options, When the second is selected, Then only its document takes clicks`() {
        setCards(selectedIndex = 1)

        composeRule.onNodeWithText(documentName(0)).assertHasNoClickAction()
        composeRule.onNodeWithText(documentName(1)).assertHasClickAction()
    }

    /**
     * Claims the user can select, as on a proximity request, with both documents open: only the
     * selected option's claim row takes a click.
     */
    @Test
    fun `Given selectable claims in two open options, Then only the selected claim takes clicks`() {
        setCards(selectedIndex = 0, claimsAreSelectable = true, isExpanded = true)

        composeRule.onNodeWithText(claimValue(0)).assertHasClickAction()
        composeRule.onNodeWithText(claimValue(1)).assertHasNoClickAction()
    }

    /**
     * Claims the user can select, with both documents open and the first option selected: the
     * selected option's claim is unchecked and the other option's checked, so the state each row
     * reports comes from its claim, not from whether its option is selected.
     */
    @Test
    fun `Given selectable claims in two open options, Then each claim reads its checked state`() {
        setCards(selectedIndex = 0, claimsAreSelectable = true, isExpanded = true)

        composeRule.onNodeWithText(claimValue(0))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsOff()
        composeRule.onNodeWithText(claimValue(1))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsOn()
    }

    /**
     * Each option carries open transaction data with a document link: only the selected option's
     * transaction-data card and link take a click.
     */
    @Test
    fun `Given transaction data in two options, Then only the selected card takes clicks`() {
        setCards(selectedIndex = 0, hasTransactionData = true)

        composeRule.onNodeWithText(transactionName(0)).assertHasClickAction()
        composeRule.onNodeWithText(transactionDocument(0)).assertHasClickAction()
        composeRule.onNodeWithText(transactionName(1)).assertHasNoClickAction()
        composeRule.onNodeWithText(transactionDocument(1)).assertHasNoClickAction()
    }

    private fun setCards(
        selectedIndex: Int,
        claimsAreSelectable: Boolean = false,
        isExpanded: Boolean = false,
        hasTransactionData: Boolean = false,
    ) {
        val requestDataUi = RequestDataUi.Multiple(
            combinations = List(2) { index ->
                RequestCombinationUi(
                    documents = listOf(documentItem(index, isExpanded, claimsAreSelectable)),
                    matches = emptyList(),
                    transactionData = if (hasTransactionData) transactionData(index) else null,
                )
            },
            selectedIndex = selectedIndex,
        )
        composeRule.setContent {
            MaterialTheme {
                DisplayCombinationCards(
                    modifier = Modifier,
                    requestDataUi = requestDataUi,
                    claimsAreSelectable = claimsAreSelectable,
                    onCombinationSelected = {},
                    onClaimClick = {},
                    onCredentialExpansionChange = {},
                    onTransactionExpansionChange = { _, _ -> },
                    onTransactionDocumentClick = { _, _ -> },
                )
            }
        }
    }

    private fun documentItem(
        index: Int,
        isExpanded: Boolean,
        claimsAreSelectable: Boolean,
    ): RequestDocumentItemUi =
        RequestDocumentItemUi(
            domainPayload = DocumentPayloadDomain(
                docName = documentName(index),
                docId = "doc-$index",
                docFormatDomain = DocumentFormatDomain.MsoMdoc,
                docClaimsDomain = emptyList(),
            ),
            headerUi = ExpandableListItemUi.NestedListItem(
                header = ListItemDataUi(
                    itemId = "doc-$index",
                    mainContentData = ListItemMainContentDataUi.Text(text = documentName(index)),
                ),
                nestedItems = listOf(
                    ExpandableListItemUi.SingleListItem(
                        ListItemDataUi(
                            itemId = "doc-$index-claim",
                            mainContentData = ListItemMainContentDataUi.Text(
                                text = claimValue(index),
                            ),
                            trailingContentData = if (claimsAreSelectable) {
                                ListItemTrailingContentDataUi.Checkbox(
                                    checkboxData = CheckboxDataUi(isChecked = index == 1),
                                )
                            } else {
                                null
                            },
                        )
                    )
                ),
                isExpanded = isExpanded,
            ),
            userImageItemIds = emptySet(),
        )

    private fun transactionData(index: Int): RequestTransactionDataUi =
        RequestTransactionDataUi(
            title = "Data to sign",
            details = ExpandableListItemUi.NestedListItem(
                header = ListItemDataUi(
                    itemId = "transaction-$index",
                    mainContentData = ListItemMainContentDataUi.Text(text = transactionName(index)),
                ),
                nestedItems = listOf(
                    ExpandableListItemUi.SingleListItem(
                        ListItemDataUi(
                            itemId = "transaction-$index-document",
                            mainContentData = ListItemMainContentDataUi.Text(
                                text = transactionDocument(index),
                            ),
                        )
                    )
                ),
                isExpanded = true,
            ),
            documentUrlsByItemId = mapOf(
                "transaction-$index-document" to "https://example.com/document-$index.pdf",
            ),
        )

    private fun documentName(index: Int) = "Document $index"

    private fun claimValue(index: Int) = "Claim value $index"

    private fun transactionName(index: Int) = "Transaction $index"

    private fun transactionDocument(index: Int) = "Document to sign $index"
}
