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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tests what accessibility services and touch can reach on a [WrapSelectableCard].
 *
 * Runs in Robolectric's native graphics mode: Compose leaves out of the accessibility tree the
 * nodes a sibling covers, and it works that out with `android.graphics.Region`, which the legacy
 * mode does not implement.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TestWrapSelectableCard {

    @get:Rule
    val composeRule = createComposeRule()

    private val title = "Option 2 of 2"
    private val documentName = "PID (SD-JWT VC)"
    private val readOnlyRow = "Family name"

    /**
     * An unselected card holds a clickable document item. Accessibility services reach the item:
     * the select layer above it does not hide it from them.
     */
    @Test
    fun `Given an unselected card, Then accessibility services reach its content`() {
        setCard(isSelected = false)

        val documentNodeId = composeRule.onNodeWithText(documentName, useUnmergedTree = true)
            .fetchSemanticsNode()
            .id
        val documentInfo = accessibilityNodeProvider().createAccessibilityNodeInfo(documentNodeId)

        assertNotNull(documentInfo)
        assertTrue(documentInfo?.isVisibleToUser == true)
        assertEquals(documentName, documentInfo?.text?.toString())
    }

    /**
     * Every clickable node an unselected card offers to accessibility services has a label, so
     * the select layer is not offered as an unlabelled control.
     */
    @Test
    fun `Given an unselected card, Then every control it offers has a label`() {
        setCard(isSelected = false)

        val provider = accessibilityNodeProvider()
        val offeredClickableNodes = composeRule.onAllNodes(hasClickAction())
            .fetchSemanticsNodes()
            .filter { node ->
                provider.createAccessibilityNodeInfo(node.id)
                    ?.let { it.isVisibleToUser && it.isImportantForAccessibility } == true
            }

        assertTrue(offeredClickableNodes.isNotEmpty())
        offeredClickableNodes.forEach { node ->
            assertTrue("node ${node.id} has no label", node.hasLabel())
        }
    }

    /**
     * The title row of an unselected card is a radio button that reads the title and is not
     * selected.
     */
    @Test
    fun `Given an unselected card, Then its title reads as an unselected radio button`() {
        setCard(isSelected = false)

        composeRule.onNodeWithText(title)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsNotSelected()
    }

    /**
     * An accessibility service runs the click action of an unselected card's title row; the
     * card's selection callback runs once.
     */
    @Test
    fun `Given an unselected card, When its title's click action runs, Then it is selected`() {
        var selections = 0
        setCard(isSelected = false, onSelected = { selections++ })

        composeRule.onNodeWithText(title).performSemanticsAction(SemanticsActions.OnClick)

        composeRule.runOnIdle { assertEquals(1, selections) }
    }

    /**
     * The title row of a selected card reads as a selected radio button and offers no select
     * action.
     */
    @Test
    fun `Given a selected card, Then its title reads as selected and offers no select action`() {
        setCard(isSelected = true)

        composeRule.onNodeWithText(title)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsSelected()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    /**
     * A read-only row of a selected card is a node of its own: screen readers step through it by
     * itself, not as part of the radio button.
     */
    @Test
    fun `Given a selected card, Then a read-only row is not read as part of the radio button`() {
        setCard(isSelected = true)

        composeRule.onNodeWithText(readOnlyRow)
            .assertTextEquals(readOnlyRow)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
    }

    /**
     * A tap on the content of an unselected card selects the card; the content's own click
     * handler does not run.
     */
    @Test
    fun `Given an unselected card, When its content is tapped, Then the card is selected`() {
        var selections = 0
        var itemClicks = 0
        setCard(isSelected = false, onSelected = { selections++ }, onItemClick = { itemClicks++ })

        composeRule.onNodeWithText(documentName, useUnmergedTree = true)
            .performTouchInput { click() }

        composeRule.runOnIdle {
            assertEquals(1, selections)
            assertEquals(0, itemClicks)
        }
    }

    /**
     * A tap on the content of a selected card reaches the content's own click handler and does
     * not select the card again.
     */
    @Test
    fun `Given a selected card, When its content is tapped, Then the content handles the tap`() {
        var selections = 0
        var itemClicks = 0
        setCard(isSelected = true, onSelected = { selections++ }, onItemClick = { itemClicks++ })

        composeRule.onNodeWithText(documentName, useUnmergedTree = true)
            .performTouchInput { click() }

        composeRule.runOnIdle {
            assertEquals(0, selections)
            assertEquals(1, itemClicks)
        }
    }

    private fun setCard(
        isSelected: Boolean,
        onSelected: () -> Unit = {},
        onItemClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                WrapSelectableCard(
                    title = title,
                    isSelected = isSelected,
                    onSelected = onSelected,
                ) {
                    WrapListItem(
                        item = ListItemDataUi(
                            itemId = "0",
                            mainContentData = ListItemMainContentDataUi.Text(text = documentName),
                        ),
                        onItemClick = { onItemClick() },
                        throttleClicks = false,
                    )
                    WrapListItem(
                        item = ListItemDataUi(
                            itemId = "1",
                            mainContentData = ListItemMainContentDataUi.Text(text = readOnlyRow),
                        ),
                        onItemClick = null,
                    )
                }
            }
        }
    }

    private fun accessibilityNodeProvider(): AccessibilityNodeProvider {
        val root = composeRule.onRoot().fetchSemanticsNode().root as ViewRootForTest
        composeRule.runOnIdle { root.forceAccessibilityForTesting(true) }
        composeRule.waitForIdle()
        return requireNotNull(root.view.accessibilityNodeProvider)
    }

    private fun SemanticsNode.hasLabel(): Boolean {
        val texts = config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        val descriptions = config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        return (texts + descriptions).any { it.isNotBlank() }
    }
}
