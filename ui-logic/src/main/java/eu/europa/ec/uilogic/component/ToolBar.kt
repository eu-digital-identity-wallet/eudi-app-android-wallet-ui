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

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DefaultToolBar(
    navigatableAction: ScreenNavigateAction,
    onBack: (() -> Unit)?,
    keyboardController: SoftwareKeyboardController?,
    toolbarConfig: ToolbarConfig?,
) {
    var dropDownMenuExpanded by remember {
        mutableStateOf(false)
    }

    TopAppBar(
        title = {
            Text(
                text = toolbarConfig?.title.orEmpty()
            )
        },
        navigationIcon = {
            // Check if we should add back/close button.
            if (navigatableAction != ScreenNavigateAction.NONE) {
                val navigationIcon = when (navigatableAction) {
                    ScreenNavigateAction.CANCELABLE -> AppIcons.Close
                    else -> AppIcons.ArrowBack
                }

                WrapIconButton(
                    iconData = navigationIcon,
                    onClick = {
                        onBack?.invoke()
                        keyboardController?.hide()
                    }
                )
            }
        },
        // Add toolbar actions.
        actions = {
            // Show first [MAX_TOOLBAR_ACTIONS] actions.
            toolbarConfig?.actions?.let { actions ->

                actions
                    .sortedByDescending { it.order }
                    .take(MAX_TOOLBAR_ACTIONS)
                    .map { visibleToolbarAction ->
                        WrapIconButton(
                            iconData = visibleToolbarAction.icon,
                            onClick = visibleToolbarAction.onClick,
                            enabled = visibleToolbarAction.enabled
                        )
                    }

                // Check if there are more actions to show.
                if (actions.size > MAX_TOOLBAR_ACTIONS) {
                    Box {
                        val iconMore = AppIcons.VerticalMore
                        WrapIconButton(
                            onClick = { dropDownMenuExpanded = !dropDownMenuExpanded },
                            iconData = iconMore,
                            enabled = true
                        )
                        DropdownMenu(
                            expanded = dropDownMenuExpanded,
                            onDismissRequest = { dropDownMenuExpanded = false }
                        ) {
                            actions
                                .sortedByDescending { it.order }
                                .drop(MAX_TOOLBAR_ACTIONS)
                                .map { dropDownMenuToolbarAction ->
                                    val dropDownMenuToolbarActionIcon =
                                        dropDownMenuToolbarAction.icon
                                    DropdownMenuItem(
                                        onClick = dropDownMenuToolbarAction.onClick,
                                        enabled = dropDownMenuToolbarAction.enabled,
                                        text = {
                                            Text(text = stringResource(id = dropDownMenuToolbarActionIcon.contentDescriptionId))
                                        },
                                        trailingIcon = {
                                            WrapIcon(iconData = dropDownMenuToolbarActionIcon)
                                        }
                                    )
                                }
                        }
                    }
                }
            }
        }
    )
}