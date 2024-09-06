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

package eu.europa.ec.uilogic.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity

/**
 * A composable function that measures the height of its content when it's not yet visible
 * and dynamically reserves space for it. This ensures smooth animations when the content
 * becomes visible, as the layout doesn't need to adjust to the content's size.

 * @param modifier Modifier to be applied to the layout.
 * @param showContent Whether the content should be visible.
 * @param content The composable content to be measured and displayed.
 */
@Composable
fun PreMeasuredContentWithAnimatedVisibility(
    modifier: Modifier = Modifier,
    showContent: Boolean,
    content: @Composable () -> Unit
) {
    // State to store the measured height of the content
    var contentHeight by remember { mutableIntStateOf(0) }
    var isHeightMeasured by remember { mutableStateOf(false) }

    // Box to reserve space dynamically
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // SubcomposeLayout to pre-measure the content only if not already measured
        if (!isHeightMeasured) {
            SubcomposeLayout { constraints ->
                // Subcompose the content when it's not yet visible to measure its height
                val contentPlaceables = subcompose("Content") {
                    content()
                }.map { it.measure(constraints) }

                // Update height only if it is not yet measured
                if (contentHeight == 0 && contentPlaceables.isNotEmpty()) {
                    contentHeight = contentPlaceables.maxOf { it.height }
                    isHeightMeasured = true // Cache height to prevent further measurement
                }

                // Reserve the required space dynamically based on measured height
                layout(width = constraints.maxWidth, height = contentHeight) {
                    // We don't draw anything here, just measure the space
                }
            }
        }

        // The actual content that is visible when showContent is true
        AnimatedVisibility(visible = showContent) {
            content()
        }

        // Spacer to dynamically reserve space based on the height of the content
        Spacer(modifier = Modifier.height(with(LocalDensity.current) { contentHeight.toDp() }))
    }
}