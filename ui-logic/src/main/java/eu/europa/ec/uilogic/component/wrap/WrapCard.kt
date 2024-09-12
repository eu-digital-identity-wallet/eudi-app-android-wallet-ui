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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.theme.values.backgroundDefault
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_MEDIUM
import eu.europa.ec.uilogic.extension.throttledClickable

@Composable
fun WrapCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    throttleClicks: Boolean = true,
    shape: Shape? = null,
    colors: CardColors? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardShape = shape ?: RoundedCornerShape(SIZE_MEDIUM.dp)
    val cardColors = colors ?: CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.backgroundDefault
    )
    val cardModifier = Modifier
        .clip(cardShape)
        .then(modifier)
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