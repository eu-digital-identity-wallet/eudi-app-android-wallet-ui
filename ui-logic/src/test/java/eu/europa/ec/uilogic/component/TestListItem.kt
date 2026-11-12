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

package eu.europa.ec.uilogic.component

import android.view.accessibility.AccessibilityNodeProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import eu.europa.ec.uilogic.component.wrap.CheckboxDataUi
import eu.europa.ec.uilogic.component.wrap.RadioButtonDataUi
import eu.europa.ec.uilogic.component.wrap.SwitchDataUi
import eu.europa.ec.uilogic.component.wrap.WrapListItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tests what accessibility services read and act on in a [ListItem] row whose trailing content is
 * a checkbox (a claim the user can leave out of a response), a radio button (a single-choice
 * filter) or a switch (a setting), and that touch on such a row works as before. The rows are
 * drawn through [WrapListItem], whose default clickable area is the entire row, as on those
 * screens; a row with a trailing icon checks that other rows stay plain clickable rows.
 *
 * Runs in Robolectric's native graphics mode, as the selectable card's test does: Compose works
 * out which nodes the accessibility node provider reports with `android.graphics.Region`, which
 * the legacy mode does not implement.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TestListItem {

    @get:Rule
    val composeRule = createComposeRule()

    private val itemId = "claim-0"
    private val claimName = "Family name"
    private val claimValue = "Doe"
    private val radioButtonText = "Next 7 days"
    private val switchText = "Show batch issuance counter"

    /**
     * A checked checkbox row that takes the click is one checkbox node that holds the row's text,
     * and the node information TalkBack reads for it is checkable, checked and clickable.
     */
    @Test
    fun `Given a checked checkbox row, Then accessibility services read a checked checkbox`() {
        setCheckboxRow(isChecked = true)

        composeRule.onNodeWithText(claimValue)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assert(hasText(claimName))
        val rowInfo = createRowNodeInfo(claimValue)

        assertTrue(rowInfo.isCheckable)
        assertEquals(AccessibilityNodeInfoCompat.CHECKED_STATE_TRUE, rowInfo.checked)
        assertTrue(rowInfo.isClickable)
    }

    /**
     * An unchecked checkbox row reads as an unchecked checkbox and offers the click action.
     */
    @Test
    fun `Given an unchecked checkbox row, Then it reads as an unchecked checkbox`() {
        setCheckboxRow(isChecked = false)

        composeRule.onNodeWithText(claimValue)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsOff()
            .assertHasClickAction()
    }

    /**
     * An accessibility service runs the click action of a checkbox row; the row's click handler
     * runs once, with the row's item.
     */
    @Test
    fun `Given a checkbox row, When its click action runs, Then its handler runs once`() {
        val clickedIds = mutableListOf<String>()
        setCheckboxRow(isChecked = true, onItemClick = { clickedIds.add(it.itemId) })

        composeRule.onNodeWithText(claimValue).performSemanticsAction(SemanticsActions.OnClick)

        composeRule.runOnIdle { assertEquals(listOf(itemId), clickedIds) }
    }

    /**
     * A tap anywhere on a checkbox row runs the row's click handler once, as on a plain
     * clickable row.
     */
    @Test
    fun `Given a checkbox row, When it is tapped, Then its handler runs once`() {
        val clickedIds = mutableListOf<String>()
        setCheckboxRow(isChecked = true, onItemClick = { clickedIds.add(it.itemId) })

        composeRule.onNodeWithText(claimName, useUnmergedTree = true)
            .performTouchInput { click() }

        composeRule.runOnIdle { assertEquals(listOf(itemId), clickedIds) }
    }

    /**
     * The row's handler flips the item's checked state, as the request screen's view model does:
     * the state the row reports follows the item, from checked to unchecked and back.
     */
    @Test
    fun `Given a checkbox row, When its handler flips the item, Then the reported state follows`() {
        composeRule.setContent {
            var isChecked by remember { mutableStateOf(true) }
            MaterialTheme {
                WrapListItem(
                    item = createCheckboxItem(isChecked = isChecked),
                    onItemClick = { isChecked = !isChecked },
                    throttleClicks = false,
                )
            }
        }

        composeRule.onNodeWithText(claimValue).assertIsOn().performClick()
        composeRule.onNodeWithText(claimValue).assertIsOff().performClick()
        composeRule.onNodeWithText(claimValue).assertIsOn()
    }

    /**
     * The row is the only checkbox that accessibility services find: the drawn checkbox adds no
     * second one.
     */
    @Test
    fun `Given a checkbox row, Then the row is its only checkbox node`() {
        setCheckboxRow(isChecked = true)

        composeRule.onAllNodes(isToggleable(), useUnmergedTree = true).assertCountEquals(1)
    }

    /**
     * A disabled checkbox gives its row no click handler: the row reads as a disabled checked
     * checkbox with no click action, and a tap runs nothing.
     */
    @Test
    fun `Given a disabled checkbox, Then its row reads as a disabled checkbox without action`() {
        var clicks = 0
        setCheckboxRow(isChecked = true, isEnabled = false, onItemClick = { clicks++ })

        composeRule.onNodeWithText(claimValue)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsOn()
            .assertIsNotEnabled()
            .assertHasNoClickAction()
            .performTouchInput { click() }

        composeRule.runOnIdle { assertEquals(0, clicks) }
    }

    /**
     * An unchecked checkbox row without a click handler, such as a claim in a request option that
     * is not selected, reports its state read-only: an enabled, unchecked checkbox with no click
     * action.
     */
    @Test
    fun `Given a checkbox row without a handler, Then it reports its state read-only`() {
        setCheckboxRow(isChecked = false, onItemClick = null)

        composeRule.onNodeWithText(claimValue)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsOff()
            .assertIsEnabled()
            .assertHasNoClickAction()
    }

    /**
     * A checkbox that takes its own clicks ([ClickableArea.TRAILING_CONTENT]) reports its own
     * state, so the row adds no checkbox semantics of its own and takes no click itself.
     */
    @Test
    fun `Given a checkbox that takes its own clicks, Then the row adds no second checkbox`() {
        setCheckboxRow(isChecked = true, clickableAreas = listOf(ClickableArea.TRAILING_CONTENT))

        composeRule.onAllNodes(isToggleable(), useUnmergedTree = true).assertCountEquals(1)
        composeRule.onNodeWithText(claimValue)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertHasNoClickAction()
    }

    /**
     * A selected radio button row that takes the click reads as a selected radio button, and the
     * node information TalkBack reads for it is checkable, checked and "Selected". Android offers
     * an already selected radio button as not clickable, since it cannot be selected again.
     */
    @Test
    fun `Given a selected radio button row, Then accessibility services read it as selected`() {
        setRow(item = createRadioButtonItem(isSelected = true))

        composeRule.onNodeWithText(radioButtonText)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsSelected()
        val rowInfo = createRowNodeInfo(radioButtonText)

        assertTrue(rowInfo.isCheckable)
        assertEquals(AccessibilityNodeInfoCompat.CHECKED_STATE_TRUE, rowInfo.checked)
        assertEquals("Selected", rowInfo.stateDescription?.toString())
        assertFalse(rowInfo.isClickable)
    }

    /**
     * An unselected radio button row is offered as clickable; an accessibility service runs its
     * click action, and the row's click handler runs once, with the row's item.
     */
    @Test
    fun `Given an unselected radio button row, When its click action runs, Then it runs once`() {
        val clickedIds = mutableListOf<String>()
        setRow(
            item = createRadioButtonItem(isSelected = false),
            onItemClick = { clickedIds.add(it.itemId) },
        )

        composeRule.onNodeWithText(radioButtonText)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsNotSelected()
        assertTrue(createRowNodeInfo(radioButtonText).isClickable)
        composeRule.onNodeWithText(radioButtonText)
            .performSemanticsAction(SemanticsActions.OnClick)

        composeRule.runOnIdle { assertEquals(listOf(itemId), clickedIds) }
    }

    /**
     * A radio button row without a click handler reports its selection read-only: an enabled,
     * selected radio button with no click action.
     */
    @Test
    fun `Given a radio button row without a handler, Then it reports its selection read-only`() {
        setRow(item = createRadioButtonItem(isSelected = true), onItemClick = null)

        composeRule.onNodeWithText(radioButtonText)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsSelected()
            .assertIsEnabled()
            .assertHasNoClickAction()
    }

    /**
     * A disabled radio button gives its row no click handler: the row reads as a disabled radio
     * button with no click action, and a tap runs nothing.
     */
    @Test
    fun `Given a disabled radio button, Then its row reads as disabled without action`() {
        var clicks = 0
        setRow(
            item = createRadioButtonItem(isSelected = false, isEnabled = false),
            onItemClick = { clicks++ },
        )

        composeRule.onNodeWithText(radioButtonText)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsNotSelected()
            .assertIsNotEnabled()
            .assertHasNoClickAction()
            .performTouchInput { click() }

        composeRule.runOnIdle { assertEquals(0, clicks) }
    }

    /**
     * A switch row that is on and takes the click reads as a switch, and the node information
     * TalkBack reads for it is checkable, checked, "On" and clickable.
     */
    @Test
    fun `Given a switch row that is on, Then accessibility services read a switch that is on`() {
        setRow(item = createSwitchItem(isChecked = true))

        composeRule.onNodeWithText(switchText)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsOn()
        val rowInfo = createRowNodeInfo(switchText)

        assertTrue(rowInfo.isCheckable)
        assertEquals(AccessibilityNodeInfoCompat.CHECKED_STATE_TRUE, rowInfo.checked)
        assertEquals("On", rowInfo.stateDescription?.toString())
        assertTrue(rowInfo.isClickable)
    }

    /**
     * A tap on a switch row runs the row's click handler once, as on a plain clickable row.
     */
    @Test
    fun `Given a switch row, When it is tapped, Then its handler runs once`() {
        val clickedIds = mutableListOf<String>()
        setRow(
            item = createSwitchItem(isChecked = false),
            onItemClick = { clickedIds.add(it.itemId) },
        )

        composeRule.onNodeWithText(switchText).performTouchInput { click() }

        composeRule.runOnIdle { assertEquals(listOf(itemId), clickedIds) }
    }

    /**
     * A disabled switch gives its row no click handler: the row reads as a disabled switch that
     * is off, with no click action, and a tap runs nothing.
     */
    @Test
    fun `Given a disabled switch, Then its row reads as a disabled switch without action`() {
        var clicks = 0
        setRow(
            item = createSwitchItem(isChecked = false, isEnabled = false),
            onItemClick = { clicks++ },
        )

        composeRule.onNodeWithText(switchText)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsOff()
            .assertIsNotEnabled()
            .assertHasNoClickAction()
            .performTouchInput { click() }

        composeRule.runOnIdle { assertEquals(0, clicks) }
    }

    /**
     * A switch that takes its own clicks ([ClickableArea.TRAILING_CONTENT]) reports its own state,
     * so the row adds no switch semantics of its own and takes no click itself.
     */
    @Test
    fun `Given a switch that takes its own clicks, Then the row adds no second switch`() {
        setRow(
            item = createSwitchItem(isChecked = true),
            clickableAreas = listOf(ClickableArea.TRAILING_CONTENT),
        )

        composeRule.onAllNodes(isToggleable(), useUnmergedTree = true).assertCountEquals(1)
        composeRule.onNodeWithText(switchText)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertHasNoClickAction()
    }

    /**
     * A row whose trailing content is an icon stays a plain clickable row, with no role and no
     * state.
     */
    @Test
    fun `Given a row with a trailing icon, Then it stays a plain clickable row`() {
        setRow(
            item = ListItemDataUi(
                itemId = itemId,
                mainContentData = ListItemMainContentDataUi.Text(text = claimValue),
                trailingContentData = ListItemTrailingContentDataUi.Icon(
                    iconData = AppIcons.KeyboardArrowRight,
                ),
            ),
        )

        composeRule.onNodeWithText(claimValue)
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ToggleableState))
    }

    private fun setCheckboxRow(
        isChecked: Boolean,
        isEnabled: Boolean = true,
        onItemClick: ((ListItemDataUi) -> Unit)? = {},
        clickableAreas: List<ClickableArea>? = null,
    ) {
        setRow(
            item = createCheckboxItem(isChecked = isChecked, isEnabled = isEnabled),
            onItemClick = onItemClick,
            clickableAreas = clickableAreas,
        )
    }

    private fun setRow(
        item: ListItemDataUi,
        onItemClick: ((ListItemDataUi) -> Unit)? = {},
        clickableAreas: List<ClickableArea>? = null,
    ) {
        composeRule.setContent {
            MaterialTheme {
                WrapListItem(
                    item = item,
                    onItemClick = onItemClick,
                    throttleClicks = false,
                    clickableAreas = clickableAreas,
                )
            }
        }
    }

    private fun createCheckboxItem(isChecked: Boolean, isEnabled: Boolean = true): ListItemDataUi =
        ListItemDataUi(
            itemId = itemId,
            overlineText = claimName,
            mainContentData = ListItemMainContentDataUi.Text(text = claimValue),
            trailingContentData = ListItemTrailingContentDataUi.Checkbox(
                checkboxData = CheckboxDataUi(
                    isChecked = isChecked,
                    enabled = isEnabled,
                ),
            ),
        )

    private fun createRadioButtonItem(
        isSelected: Boolean,
        isEnabled: Boolean = true,
    ): ListItemDataUi = ListItemDataUi(
        itemId = itemId,
        mainContentData = ListItemMainContentDataUi.Text(text = radioButtonText),
        trailingContentData = ListItemTrailingContentDataUi.RadioButton(
            radioButtonData = RadioButtonDataUi(isSelected = isSelected, enabled = isEnabled),
        ),
    )

    private fun createSwitchItem(isChecked: Boolean, isEnabled: Boolean = true): ListItemDataUi =
        ListItemDataUi(
            itemId = itemId,
            mainContentData = ListItemMainContentDataUi.Text(text = switchText),
            trailingContentData = ListItemTrailingContentDataUi.Switch(
                switchData = SwitchDataUi(isChecked = isChecked, enabled = isEnabled),
            ),
        )

    private fun createRowNodeInfo(text: String): AccessibilityNodeInfoCompat {
        val rowNodeId = composeRule.onNodeWithText(text).fetchSemanticsNode().id
        return AccessibilityNodeInfoCompat.wrap(
            requireNotNull(accessibilityNodeProvider().createAccessibilityNodeInfo(rowNodeId))
        )
    }

    private fun accessibilityNodeProvider(): AccessibilityNodeProvider {
        val root = composeRule.onRoot().fetchSemanticsNode().root as ViewRootForTest
        composeRule.runOnIdle { root.forceAccessibilityForTesting(true) }
        composeRule.waitForIdle()
        return requireNotNull(root.view.accessibilityNodeProvider)
    }
}
