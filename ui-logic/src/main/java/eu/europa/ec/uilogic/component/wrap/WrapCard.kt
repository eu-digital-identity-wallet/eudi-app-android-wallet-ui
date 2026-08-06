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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.extension.throttledClickable

val shadowsAtElevation1: List<Shadow> = listOf(
    Shadow(
        radius = 3.dp,
        color = Color.Black.copy(alpha = 0.15f),
        spread = 1.dp,
        offset = DpOffset(x = 0.dp, y = 1.dp),
    ),
    Shadow(
        radius = 2.dp,
        color = Color.Black.copy(alpha = 0.30f),
        spread = 0.dp,
        offset = DpOffset(x = 0.dp, y = 1.dp),
    ),
)

@Composable
fun WrapCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    throttleClicks: Boolean = true,
    shape: Shape? = null,
    colors: CardColors? = null,
    border: BorderStroke? = null,
    shadows: List<Shadow> = emptyList(),
    content: @Composable ColumnScope.() -> Unit
) {
    val cardShape = shape ?: RoundedCornerShape(SIZE_SMALL.dp)
    val cardColors = colors ?: CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
    val shadowModifier = shadows.fold<Shadow, Modifier>(Modifier) { chained, shadow ->
        chained.dropShadow(cardShape, shadow)
    }

    val shapedModifier = when {
        shadows.isEmpty() -> Modifier
            .clip(cardShape)
            .then(modifier)

        else -> modifier
            .then(shadowModifier)
            .clip(cardShape)
    }

    val cardModifier = shapedModifier
        .then(
            if (enabled && onClick != null) {
                when (throttleClicks) {
                    true -> Modifier.throttledClickable {
                        onClick()
                    }

                    false -> Modifier.clickable {
                        onClick()
                    }
                }
            } else Modifier.clickable(enabled = false, onClick = {})
        )

    Card(
        modifier = cardModifier,
        shape = cardShape,
        colors = cardColors,
        border = border,
    ) {
        content()
    }
}

@ThemeModePreviews
@Composable
private fun WrapCardPreview() {
    PreviewTheme {
        WrapCard {
            Text(text = "This is a wrap card preview.")
        }
    }
}

@ThemeModePreviews
@Composable
private fun WrapCardElevation1Preview() {
    PreviewTheme {
        WrapCard(shadows = shadowsAtElevation1) {
            Text(text = "This is an elevated wrap card preview.")
        }
    }
}