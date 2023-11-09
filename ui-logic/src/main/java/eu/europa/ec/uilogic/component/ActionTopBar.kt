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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.theme.values.backgroundPaper
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.wrap.WrapIcon

@Composable
fun ActionTopBar(
    contentColor: Color,
    iconColor: Color,
    iconData: IconData,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(color = contentColor)
            .fillMaxWidth()
            .padding(
                start = SPACING_LARGE.dp,
                end = SPACING_LARGE.dp,
                top = SPACING_LARGE.dp,
                bottom = SPACING_LARGE.dp
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        WrapIcon(
            modifier = Modifier.clickable { onClick() },
            iconData = iconData,
            customTint = iconColor
        )
    }
}

@ThemeModePreviews
@Composable
private fun TopBarPreview() {
    PreviewTheme {
        ActionTopBar(
            contentColor = MaterialTheme.colorScheme.backgroundPaper,
            iconColor = MaterialTheme.colorScheme.primary,
            iconData = AppIcons.Close
        ) {}
    }
}