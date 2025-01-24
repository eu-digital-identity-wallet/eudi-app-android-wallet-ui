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

package eu.europa.ec.uilogic.component.wrap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CardColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews

@Composable
fun WrapExpandableCard(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    onExpandedChange: (() -> Unit)? = null,
    cardCollapsedContent: @Composable () -> Unit,
    cardExpandedContent: @Composable ColumnScope.() -> Unit,
    shape: Shape? = null,
    colors: CardColors? = null,
    throttleClicks: Boolean = true,
) {
    WrapCard(
        modifier = modifier,
        shape = shape,
        colors = colors,
        onClick = onExpandedChange,
        throttleClicks = throttleClicks
    ) {
        Column {
            // Title Section (Always Visible)
            cardCollapsedContent()

            // Content Section (Animated Visibility)
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    cardExpandedContent()
                }
            }
        }
    }
}

@ThemeModePreviews
@Composable
private fun WrapExpandableCardCollapsedPreview() {
    PreviewTheme {
        var isExpanded by remember { mutableStateOf(false) }

        WrapExpandableCard(
            isExpanded = isExpanded,
            onExpandedChange = { isExpanded = !isExpanded },
            cardCollapsedContent = {
                Text(text = "Verification Data")
            },
            cardExpandedContent = {
                repeat(5) {
                    Text(text = "Data $it")
                }
            },
        )
    }
}

@ThemeModePreviews
@Composable
private fun WrapExpandableCardExpandedPreview() {
    PreviewTheme {
        var isExpanded by remember { mutableStateOf(true) }

        WrapExpandableCard(
            isExpanded = isExpanded,
            onExpandedChange = { isExpanded = !isExpanded },
            cardCollapsedContent = {
                Text(text = "Verification Data")
            },
            cardExpandedContent = {
                repeat(5) {
                    Text(text = "Data $it")
                }
            },
        )
    }
}