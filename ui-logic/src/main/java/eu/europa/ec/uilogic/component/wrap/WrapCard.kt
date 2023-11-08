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

package eu.europa.ec.uilogic.component.wrap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.extension.throttledClickable

@Composable
fun WrapCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    throttleClicks: Boolean = true,
    shape: Shape? = null,
    colors: CardColors? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardShape = shape ?: RoundedCornerShape(16.dp)
    val cardColors = colors ?: CardDefaults.cardColors()
    val cardModifier = Modifier
        .clip(cardShape)
        .then(modifier)
        .then(
            if (onClick != null) {
                when (throttleClicks) {
                    true -> Modifier.throttledClickable {
                        onClick()
                    }

                    false -> Modifier.clickable {
                        onClick()
                    }
                }
            } else Modifier
        )

    Card(
        modifier = cardModifier,
        shape = cardShape,
        colors = cardColors
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