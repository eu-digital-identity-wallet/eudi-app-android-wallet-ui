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

package eu.europa.ec.uilogic.component.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_EXTRA_LARGE

enum class GradientEdge {
    TOP, BOTTOM
}

@Composable
fun ContentGradient(
    modifier: Modifier,
    gradientStartColor: Color = MaterialTheme.colorScheme.background,
    gradientEndColor: Color = Color.Transparent,
    gradientEdge: GradientEdge = GradientEdge.BOTTOM,
    height: Dp = SIZE_EXTRA_LARGE.dp,
    bodyContent: @Composable () -> Unit
) {

    val colorStops: Array<Pair<Float, Color>>
    val gradientAlignment: Alignment
    when (gradientEdge) {
        GradientEdge.BOTTOM -> {
            colorStops = arrayOf(
                0F to gradientEndColor,
                0.8F to gradientStartColor
            )
            gradientAlignment = Alignment.BottomCenter
        }

        GradientEdge.TOP -> {
            colorStops = arrayOf(
                0F to gradientStartColor,
                0.8F to gradientEndColor
            )
            gradientAlignment = Alignment.TopCenter
        }
    }
    Box(modifier = modifier) {
        bodyContent()
        BoxGradient(
            modifier = Modifier
                .align(gradientAlignment)
                .fillMaxWidth(),
            height = height,
            colorStops = colorStops
        )
    }
}

@Composable
private fun BoxGradient(
    modifier: Modifier,
    height: Dp,
    colorStops: Array<Pair<Float, Color>>
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height = height)
            .background(brush = Brush.verticalGradient(*colorStops))
    )
}

@ThemeModePreviews
@Composable
private fun ContentGradientBottomPreview() {
    PreviewTheme {
        ContentGradient(
            modifier = Modifier.size(100.dp, 100.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@ThemeModePreviews
@Composable
private fun ContentGradientTopPreview() {
    PreviewTheme {
        ContentGradient(
            modifier = Modifier.size(100.dp, 100.dp),
            gradientEdge = GradientEdge.TOP
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@ThemeModePreviews
@Composable
private fun ContentGradientColoredPreview() {
    PreviewTheme {
        ContentGradient(
            modifier = Modifier.size(100.dp, 100.dp),
            gradientEdge = GradientEdge.TOP,
            gradientStartColor = Color.Red
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }
    }
}