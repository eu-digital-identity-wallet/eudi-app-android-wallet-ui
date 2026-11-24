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

package eu.europa.ec.uilogic.component.wrap

import android.view.accessibility.AccessibilityNodeProvider
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests that [WrapExpandableListItem] brings a card the user opened into view, and that [WrapExpandableCard] reports
 * each opening once it has finished. Also tests that an open group of radio rows is reported to
 * accessibility services as a list, and that other groups are not.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp")
class TestWrapExpandableListItem {

    @get:Rule
    val composeRule = createComposeRule()

    /**
     * A card below a spacer that fills most of a short scroll area is opened, as the view model does by setting its
     * expanded state; once it has opened, the scroll area has moved to show its rows.
     */
    @Test
    fun `Given a card at the bottom of a scroll area, When it opens with the flag on, Then it is scrolled into view`() {
        val scroll = showCardBelowSpacer(shouldBringOpenedIntoView = true, isInitiallyExpanded = false)

        openCard()

        assertTrue(scroll().value > 0)
    }

    /**
     * The same card opened with the flag at its default leaves the scroll area where it was, as before.
     */
    @Test
    fun `Given a card at the bottom of a scroll area, When it opens with the flag off, Then nothing scrolls`() {
        val scroll = showCardBelowSpacer(shouldBringOpenedIntoView = false, isInitiallyExpanded = false)

        openCard()

        assertEquals(0, scroll().value)
    }

    /**
     * A card that is already open when the screen first appears does not move the scroll area.
     */
    @Test
    fun `Given a card that starts open, When the screen appears, Then nothing scrolls`() {
        val scroll = showCardBelowSpacer(shouldBringOpenedIntoView = true, isInitiallyExpanded = true)

        composeRule.waitForIdle()

        assertEquals(0, scroll().value)
    }

    /**
     * [WrapExpandableCard.onExpandFinished] runs once for each opening, and not for a close.
     */
    @Test
    fun `Given a card, When it opens, closes and opens again, Then onExpandFinished runs twice`() {
        var finishedOpenings = 0
        composeRule.setContent {
            MaterialTheme {
                WrapExpandableCard(
                    isExpanded = isExpanded,
                    cardCollapsedContent = { Text("Header") },
                    cardExpandedContent = { Text("Row") },
                    onExpandFinished = { finishedOpenings++ },
                )
            }
        }

        openCard()
        assertEquals(1, finishedOpenings)
        closeCard()
        assertEquals(1, finishedOpenings)
        openCard()
        assertEquals(2, finishedOpenings)
    }

    /**
     * An open group of three radio rows, as a single-choice filter group: TalkBack is given a list
     * of three rows and each row's position in it, so it reads "1 of 3", "2 of 3" and "3 of 3".
     */
    @Test
    fun `Given an open group of radio rows, When accessibility services read it, Then each row reports its position in the list`() {
        // Given
        val rows = listOf("First", "Second", "Third")
        showOpenGroup(rows = rows.map { radioItem(it) })

        // When
        val collectionInfo = createListNodeInfo().collectionInfo
        val itemInfos = rows.map { createRowNodeInfo(it).collectionItemInfo }

        // Then
        assertEquals(3, collectionInfo.rowCount)
        assertEquals(1, collectionInfo.columnCount)
        itemInfos.forEachIndexed { index, itemInfo ->
            assertEquals(index, itemInfo.rowIndex)
            assertEquals(0, itemInfo.columnIndex)
        }
        assertTrue(rows.all { isInList(it) })
    }

    /**
     * An open group that mixes a radio row with a plain row is not a radio-button group, so no row
     * reports a position.
     */
    @Test
    fun `Given an open group that is not all radio rows, When accessibility services read it, Then no row reports a position`() {
        // Given
        showOpenGroup(rows = listOf(radioItem("First"), listItem(id = "Second")))

        // When
        val itemInfos = listOf("First", "Second").map { createRowNodeInfo(it).collectionItemInfo }

        // Then
        assertEquals(listOf(null, null), itemInfos)
    }

    /**
     * An open group of checkbox rows, as a multiple-choice filter group, is not a radio-button
     * group either, so no row reports a position.
     */
    @Test
    fun `Given an open group of checkbox rows, When accessibility services read it, Then no row reports a position`() {
        // Given
        showOpenGroup(rows = listOf(checkboxItem("First"), checkboxItem("Second")))

        // When
        val itemInfos = listOf("First", "Second").map { createRowNodeInfo(it).collectionItemInfo }

        // Then
        assertEquals(listOf(null, null), itemInfos)
    }

    /**
     * A single radio row is no group either, so it reports no position.
     */
    @Test
    fun `Given an open group of one radio row, When accessibility services read it, Then it reports no position`() {
        // Given
        showOpenGroup(rows = listOf(radioItem("First")))

        // When
        val itemInfo = createRowNodeInfo("First").collectionItemInfo

        // Then
        assertNull(itemInfo)
    }

    /** The expanded state the tests set, as the view model does on the request screen. */
    private var isExpanded by mutableStateOf(false)

    /**
     * Shows a 300 dp scroll area with a 280 dp spacer above a card of ten rows, and returns a reader of its scroll
     * state.
     */
    private fun showCardBelowSpacer(
        shouldBringOpenedIntoView: Boolean,
        isInitiallyExpanded: Boolean,
    ): () -> ScrollState {
        isExpanded = isInitiallyExpanded
        lateinit var scrollState: ScrollState
        composeRule.setContent {
            MaterialTheme {
                scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .height(300.dp)
                        .verticalScroll(scrollState)
                ) {
                    Spacer(modifier = Modifier.height(280.dp))
                    WrapExpandableListItem(
                        modifier = Modifier.fillMaxWidth(),
                        header = listItem(id = "header"),
                        data = (1..10).map { ExpandableListItemUi.SingleListItem(listItem(id = "row$it")) },
                        onItemClick = null,
                        isItemClickable = { false },
                        isExpanded = isExpanded,
                        onExpandedChange = null,
                        shouldBringOpenedIntoView = shouldBringOpenedIntoView,
                    )
                }
            }
        }
        composeRule.waitForIdle()
        return { scrollState }
    }

    /** Opens the card and waits for its animation and any scroll to finish. */
    private fun openCard() {
        isExpanded = true
        composeRule.waitForIdle()
    }

    /** Closes the card and waits for its animation to finish. */
    private fun closeCard() {
        isExpanded = false
        composeRule.waitForIdle()
    }

    /** Shows an open card whose rows are [rows]. */
    private fun showOpenGroup(rows: List<ListItemDataUi>) {
        composeRule.setContent {
            MaterialTheme {
                WrapExpandableListItem(
                    modifier = Modifier.fillMaxWidth(),
                    header = listItem(id = "header"),
                    data = rows.map { ExpandableListItemUi.SingleListItem(it) },
                    onItemClick = {},
                    isItemClickable = { true },
                    isExpanded = true,
                    onExpandedChange = null,
                )
            }
        }
        composeRule.waitForIdle()
    }

    /** Matches the one semantics node that reports a list. */
    private val isList = SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionInfo)

    /** The accessibility node info of the row showing [text]. */
    private fun createRowNodeInfo(text: String): AccessibilityNodeInfoCompat =
        createNodeInfo(id = composeRule.onNodeWithText(text).fetchSemanticsNode().id)

    /** The accessibility node info of the one node that reports a list. */
    private fun createListNodeInfo(): AccessibilityNodeInfoCompat =
        createNodeInfo(id = composeRule.onNode(isList).fetchSemanticsNode().id)

    /** The accessibility node info Compose gives TalkBack for the semantics node [id]. */
    private fun createNodeInfo(id: Int): AccessibilityNodeInfoCompat =
        AccessibilityNodeInfoCompat.wrap(
            requireNotNull(accessibilityNodeProvider().createAccessibilityNodeInfo(id))
        )

    /** The node provider Compose gives accessibility services, with accessibility turned on. */
    private fun accessibilityNodeProvider(): AccessibilityNodeProvider {
        val root = composeRule.onRoot().fetchSemanticsNode().root as ViewRootForTest
        composeRule.runOnIdle { root.forceAccessibilityForTesting(true) }
        composeRule.waitForIdle()
        return requireNotNull(root.view.accessibilityNodeProvider)
    }

    /** True when the row showing [text] sits inside the node that reports a list. */
    private fun isInList(text: String): Boolean {
        val listId = composeRule.onNode(isList).fetchSemanticsNode().id
        val row = composeRule.onNodeWithText(text).fetchSemanticsNode()
        return generateSequence(row.parent) { it.parent }.any { it.id == listId }
    }

    /** A row with [text] as its id and text, ending in an unselected radio button. */
    private fun radioItem(text: String) = ListItemDataUi(
        itemId = text,
        mainContentData = ListItemMainContentDataUi.Text(text = text),
        trailingContentData = ListItemTrailingContentDataUi.RadioButton(
            radioButtonData = RadioButtonDataUi(isSelected = false, enabled = true),
        ),
    )

    /** A row with [text] as its id and text, ending in an unchecked checkbox. */
    private fun checkboxItem(text: String) = ListItemDataUi(
        itemId = text,
        mainContentData = ListItemMainContentDataUi.Text(text = text),
        trailingContentData = ListItemTrailingContentDataUi.Checkbox(
            checkboxData = CheckboxDataUi(isChecked = false, enabled = true),
        ),
    )

    /** A plain list item with [id] as its id and text. */
    private fun listItem(id: String) = ListItemDataUi(
        itemId = id,
        mainContentData = ListItemMainContentDataUi.Text(text = id),
    )
}
