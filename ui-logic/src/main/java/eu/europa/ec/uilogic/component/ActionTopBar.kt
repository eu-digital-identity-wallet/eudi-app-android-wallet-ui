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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import eu.europa.ec.resourceslogic.theme.values.backgroundPaper
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.wrap.WrapIconButton

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ActionTopBar(
    contentColor: Color,
    iconColor: Color,
    iconData: IconData,
    onClick: () -> Unit
) {
    TopAppBar(
        title = {},
        navigationIcon = {
            WrapIconButton(
                iconData = iconData,
                onClick = { onClick.invoke() },
                customTint = iconColor
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = contentColor)
    )
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