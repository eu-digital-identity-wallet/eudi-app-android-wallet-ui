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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import eu.europa.ec.uilogic.component.content.ToolBarActions
import eu.europa.ec.uilogic.component.content.ToolbarAction
import eu.europa.ec.uilogic.component.content.precomputeToolbarActions
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.wrap.WrapIconButton

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ActionTopBar(
    contentColor: Color,
    iconColor: Color,
    iconData: IconData,
    toolbarActions: List<ToolbarAction>? = null,
    onClick: () -> Unit
) {
    val precomputedToolbarActions = remember(toolbarActions) {
        return@remember precomputeToolbarActions(
            toolBarActions = toolbarActions
        )
    }

    TopAppBar(
        title = {},
        navigationIcon = {
            WrapIconButton(
                iconData = iconData,
                onClick = { onClick.invoke() },
                customTint = iconColor
            )
        },
        actions = {
            ToolBarActions(precomputedToolbarActions = precomputedToolbarActions)
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = contentColor)
    )
}

@ThemeModePreviews
@Composable
private fun TopBarPreview() {
    PreviewTheme {
        ActionTopBar(
            contentColor = MaterialTheme.colorScheme.surface,
            iconColor = MaterialTheme.colorScheme.primary,
            iconData = AppIcons.Close
        ) {}
    }
}