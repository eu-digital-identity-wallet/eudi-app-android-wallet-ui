/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.theme.values.backgroundPaper
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.utils.SIZE_EXTRA_LARGE

enum class GradientEdge {
    TOP, BOTTOM
}

@Composable
fun ContentGradient(
    modifier: Modifier,
    gradientStartColor: Color = MaterialTheme.colorScheme.backgroundPaper,
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

@Preview
@Composable
fun ContentGradientBottomPreview() {
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

@Preview
@Composable
fun ContentGradientTopPreview() {
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

@Preview
@Composable
fun ContentGradientColoredPreview() {
    PreviewTheme {
        ContentGradient(
            modifier = Modifier.size(100.dp, 100.dp),
            gradientEdge = GradientEdge.TOP,
            gradientStartColor = Color.Red
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.backgroundPaper)
            )
        }
    }
}