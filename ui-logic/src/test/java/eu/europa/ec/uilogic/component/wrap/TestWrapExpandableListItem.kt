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
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests that [WrapExpandableListItem] brings a card the user opened into view, and that [WrapExpandableCard] reports
 * each opening once it has finished.
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

    /** A plain list item with [id] as its id and text. */
    private fun listItem(id: String) = ListItemDataUi(
        itemId = id,
        mainContentData = ListItemMainContentDataUi.Text(text = id),
    )
}
