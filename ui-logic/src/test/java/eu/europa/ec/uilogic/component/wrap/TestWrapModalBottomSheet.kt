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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp")
class TestWrapModalBottomSheet {

    @get:Rule
    val composeRule = createComposeRule()

    //region outside tap dismissal

    // Case 1:
    // 1. A short bottom sheet is fully expanded.
    // Case 1 Expected Result:
    // Tapping near the top of the scrim dismisses the sheet exactly once.
    @Test
    fun `Given a short sheet, When the upper scrim is tapped, Then the sheet is dismissed`() {
        assertUpperScrimDismissesSheet(sheetHeight = 120.dp)
    }

    // Case 2:
    // 1. A taller bottom sheet is fully expanded.
    // Case 2 Expected Result:
    // The same upper scrim tap still dismisses the sheet exactly once.
    @Test
    fun `Given a taller sheet, When the upper scrim is tapped, Then the sheet is dismissed`() {
        assertUpperScrimDismissesSheet(sheetHeight = 300.dp)
    }

    //endregion

    //region helper functions

    @OptIn(ExperimentalMaterial3Api::class)
    private fun assertUpperScrimDismissesSheet(sheetHeight: Dp) {
        // Given
        var dismissals = 0
        composeRule.setContent {
            MaterialTheme {
                var isOpen by remember { mutableStateOf(true) }
                if (isOpen) {
                    WrapModalBottomSheet(
                        onDismissRequest = {
                            dismissals++
                            isOpen = false
                        },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    ) {
                        Box(Modifier.fillMaxWidth().height(sheetHeight))
                    }
                }
            }
        }

        // When
        composeRule.onNode(isDialog()).performTouchInput {
            click(Offset(centerX, 80.dp.toPx()))
        }

        // Then
        composeRule.runOnIdle { assertEquals(1, dismissals) }
        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    //endregion
}