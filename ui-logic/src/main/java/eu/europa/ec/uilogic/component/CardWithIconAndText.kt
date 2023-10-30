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

package eu.europa.ec.uilogic.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.utils.HSpacer
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_SMALL
import eu.europa.ec.uilogic.component.wrap.WrapCard
import eu.europa.ec.uilogic.component.wrap.WrapIcon

@Composable
fun CardWithIconAndText(
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit,
    icon: @Composable (() -> Unit)? = null,
    shape: Shape? = null,
    colors: CardColors? = null,
    contentPadding: PaddingValues = PaddingValues(all = SPACING_EXTRA_SMALL.dp)
) {
    WrapCard(
        modifier = modifier,
        shape = shape,
        colors = colors,
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.invoke()

            HSpacer.Small()

            text.invoke()
        }
    }
}

@Preview
@Composable
private fun CardWithIconAndTextPreview() {
    PreviewTheme {
        CardWithIconAndText(
            text = {
                Text(text = "This is a card with icon and text.")
            },
            icon = {
                WrapIcon(iconData = AppIcons.Warning)
            }
        )
    }
}