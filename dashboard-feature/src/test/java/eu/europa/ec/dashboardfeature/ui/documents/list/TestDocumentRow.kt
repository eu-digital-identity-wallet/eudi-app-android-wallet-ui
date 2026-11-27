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

package eu.europa.ec.dashboardfeature.ui.documents.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.LayoutBoundsHolder
import androidx.compose.ui.layout.layoutBounds
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import eu.europa.ec.corelogic.model.DocumentCategory
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentIssuanceStateUi
import eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentUi
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-w400dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TestDocumentRow {

    private val motionScale = object : MotionDurationScale {
        override var scaleFactor = 1f
    }
    private val issuedAnnouncement =
        SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite) and
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "mDL has been issued."
                )

    @get:Rule
    val composeRule = createComposeRule(ComposeUiTestConfig(effectContext = motionScale))

    @Test
    fun `Given two Deferred rows issued behind a sheet, When the updated rows become visible, Then each starts its highlight once`() {
        // Given
        val canShow = mutableStateOf(true)
        val issuanceState = mutableStateOf<DocumentIssuanceStateUi>(DocumentIssuanceStateUi.Pending)
        val unseen = mutableStateOf(setOf("PID", "mDL"))
        val shown = mutableListOf<String>()
        composeRule.setContent {
            MaterialTheme {
                val viewport = remember { LayoutBoundsHolder() }
                Column(
                    modifier = Modifier
                        .size(width = 240.dp, height = 120.dp)
                        .layoutBounds(viewport)
                        .verticalScroll(rememberScrollState()),
                ) {
                    listOf("PID", "mDL", "Immediate").forEach { name ->
                        DocumentRow(
                            document = documentItem(
                                name,
                                if (name == "Immediate") {
                                    DocumentIssuanceStateUi.Issued
                                } else {
                                    issuanceState.value
                                },
                            ),
                            shape = RectangleShape,
                            listViewport = viewport,
                            canShowHighlights = canShow.value,
                            awaitingHighlight = name in unseen.value,
                            onHighlightShown = {
                                shown.add(name)
                                unseen.value -= name
                            },
                            onClick = {},
                        )
                        Spacer(modifier = Modifier.height(160.dp))
                    }
                }
            }
        }
        composeRule.runOnIdle { assertEquals(emptyList<String>(), shown) }

        // When
        composeRule.runOnIdle {
            canShow.value = false
            issuanceState.value = DocumentIssuanceStateUi.Issued
        }
        composeRule.runOnIdle {
            assertEquals(emptyList<String>(), shown)
            canShow.value = true
        }

        // Then
        composeRule.runOnIdle { assertEquals(listOf("PID"), shown) }

        // When
        composeRule.onNodeWithText("mDL").performScrollTo()
        composeRule.runOnIdle { assertEquals(listOf("PID", "mDL"), shown) }
        composeRule.onNodeWithText("Immediate").performScrollTo()
        composeRule.onNodeWithText("PID").performScrollTo()

        // Then
        composeRule.runOnIdle { assertEquals(listOf("PID", "mDL"), shown) }
    }

    @Test
    fun `Given a newly issued row, When its highlight starts, Then the tint holds continuously and the announcement clears after the fade`() {
        // Given
        composeRule.mainClock.autoAdvance = false
        var highlightStarts = 0
        showIssuedMdl(onHighlightShown = { highlightStarts++ })

        // When
        composeRule.mainClock.advanceTimeUntil { highlightStarts > 0 }
        composeRule.mainClock.advanceTimeBy(208)
        val tint = rowBackground("mDL")

        // Then
        composeRule.onNode(issuedAnnouncement).assertExists()

        // When
        composeRule.mainClock.advanceTimeBy(192)

        // Then
        assertEquals(tint, rowBackground("mDL"))
        composeRule.mainClock.advanceTimeBy(368)
        assertEquals(tint, rowBackground("mDL"))
        composeRule.onNode(issuedAnnouncement).assertExists()

        // When
        composeRule.mainClock.advanceTimeBy(672)

        // Then
        assertNotEquals(tint, rowBackground("mDL"))
        composeRule.onNode(issuedAnnouncement).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(1, highlightStarts) }
    }

    @Test
    fun `Given disabled animations, When a newly issued row is shown, Then the static highlight and announcement last for the hold`() {
        // Given
        motionScale.scaleFactor = 0f
        composeRule.mainClock.autoAdvance = false
        var highlightStarted = false
        var clicks = 0
        showIssuedMdl(onHighlightShown = { highlightStarted = true }, onClick = { clicks++ })

        // When
        composeRule.mainClock.advanceTimeUntil { highlightStarted }

        // Then
        composeRule.mainClock.advanceTimeBy(64)
        val tint = rowBackground("mDL")
        composeRule.mainClock.advanceTimeBy(336)
        assertEquals(tint, rowBackground("mDL"))
        composeRule.onNode(issuedAnnouncement).assertExists()

        // When
        composeRule.mainClock.advanceTimeBy(480)

        // Then
        composeRule.onNode(issuedAnnouncement).assertDoesNotExist()
        assertNotEquals(tint, rowBackground("mDL"))
        composeRule.onNodeWithText("mDL").assertHasClickAction().performClick()
        composeRule.runOnIdle { assertEquals(1, clicks) }
    }

    private fun showIssuedMdl(onHighlightShown: () -> Unit, onClick: () -> Unit = {}) {
        composeRule.setContent {
            MaterialTheme {
                val viewport = remember { LayoutBoundsHolder() }
                val unseen = remember { mutableStateOf(true) }
                Box(
                    Modifier
                        .size(240.dp)
                        .layoutBounds(viewport)
                ) {
                    DocumentRow(
                        document = documentItem("mDL", DocumentIssuanceStateUi.Issued),
                        shape = RectangleShape,
                        listViewport = viewport,
                        canShowHighlights = true,
                        awaitingHighlight = unseen.value,
                        onHighlightShown = {
                            unseen.value = false
                            onHighlightShown()
                        },
                        onClick = onClick,
                    )
                }
            }
        }
    }

    private fun rowBackground(name: String): Color {
        val pixels = composeRule.onNodeWithText(name).captureToImage().toPixelMap()
        return pixels[2, pixels.height / 2]
    }

    private fun documentItem(name: String, state: DocumentIssuanceStateUi) = DocumentUi(
        documentIssuanceState = state,
        uiData = ListItemDataUi(
            itemId = name,
            mainContentData = ListItemMainContentDataUi.Text(name),
        ),
        documentIdentifier = DocumentIdentifier.MdocPid,
        documentCategory = DocumentCategory.Government,
    )
}