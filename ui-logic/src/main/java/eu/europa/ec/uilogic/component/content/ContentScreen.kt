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

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.IconDataUi
import eu.europa.ec.uilogic.component.SystemBroadcastReceiver
import eu.europa.ec.uilogic.component.loader.LoadingIndicator
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.ALPHA_DISABLED
import eu.europa.ec.uilogic.component.utils.ALPHA_ENABLED
import eu.europa.ec.uilogic.component.utils.MAX_TOOLBAR_ACTIONS
import eu.europa.ec.uilogic.component.utils.SIZE_EXTRA_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_XX_LARGE
import eu.europa.ec.uilogic.component.utils.TopSpacing
import eu.europa.ec.uilogic.component.utils.Z_STICKY
import eu.europa.ec.uilogic.component.utils.screenPaddings
import eu.europa.ec.uilogic.component.utils.stickyBottomPaddings
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapIconButton
import eu.europa.ec.uilogic.extension.throttledClickable

data class ToolbarActionUi(
    val icon: IconDataUi?,
    val text: String? = null,
    val order: Int = 100,
    val enabled: Boolean = true,
    val customTint: Color? = null,
    val clickable: Boolean = true,
    val throttleClicks: Boolean = true,
    val onClick: () -> Unit,
)

data class ToolbarConfig(
    val title: String = "",
    val actions: List<ToolbarActionUi> = listOf(),
    val maxVisibleActions: Int = MAX_TOOLBAR_ACTIONS,
)

enum class ScreenNavigateAction {
    BACKABLE, CANCELABLE, NONE
}

enum class ImePaddingConfig {
    NO_PADDING, WITH_BOTTOM_BAR, ONLY_CONTENT
}

data class BroadcastAction(val intentFilters: List<String>, val callback: (Intent?) -> Unit)

@Composable
fun ContentScreen(
    isLoading: Boolean = false,
    toolBarConfig: ToolbarConfig? = null,
    navigatableAction: ScreenNavigateAction = ScreenNavigateAction.BACKABLE,
    onBack: (() -> Unit)? = null,
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    stickyBottom: @Composable ((PaddingValues) -> Unit)? = null,
    fab: @Composable () -> Unit = {},
    fabPosition: FabPosition = FabPosition.End,
    snackbarHost: @Composable () -> Unit = {},
    contentErrorConfig: ContentErrorConfig? = null,
    broadcastAction: BroadcastAction? = null,
    imePaddingConfig: ImePaddingConfig = ImePaddingConfig.NO_PADDING,
    bodyContent: @Composable (PaddingValues) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val hasToolBar = contentErrorConfig != null
            || navigatableAction != ScreenNavigateAction.NONE
            || topBar != null
            || toolBarConfig?.actions?.isNotEmpty() == true
    val topSpacing = if (hasToolBar) TopSpacing.WithToolbar else TopSpacing.WithoutToolbar

    Scaffold(
        topBar = {
            if (topBar != null && contentErrorConfig == null) {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .statusBarsPadding()
                ) {
                    topBar()
                }
            } else if (hasToolBar) {
                DefaultToolBar(
                    navigatableAction = contentErrorConfig?.let {
                        ScreenNavigateAction.CANCELABLE
                    } ?: navigatableAction,
                    onBack = contentErrorConfig?.onCancel ?: onBack,
                    keyboardController = keyboardController,
                    toolbarConfig = toolBarConfig,
                )
            }
        },
        bottomBar = {
            bottomBar?.let {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .then(
                            if (imePaddingConfig == ImePaddingConfig.WITH_BOTTOM_BAR) {
                                Modifier.imePadding()
                            } else {
                                Modifier
                            }
                        )
                ) {
                    bottomBar()
                }
            }
        },
        floatingActionButton = fab,
        floatingActionButtonPosition = fabPosition,
        snackbarHost = snackbarHost,
    ) { padding ->

        val screenPaddingsIgnoringSticky = screenPaddings(
            hasStickyBottom = false,
            append = padding,
            topSpacing = topSpacing
        )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            if (contentErrorConfig != null) {
                ContentError(
                    config = contentErrorConfig,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(screenPaddingsIgnoringSticky)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (imePaddingConfig == ImePaddingConfig.ONLY_CONTENT) {
                                Modifier.imePadding()
                            } else {
                                Modifier
                            }
                        )
                ) {

                    Box(modifier = Modifier.weight(1f)) {
                        bodyContent(
                            screenPaddings(
                                hasStickyBottom = stickyBottom != null,
                                append = padding,
                                topSpacing = topSpacing
                            )
                        )
                    }

                    stickyBottom?.let { stickyBottomContent ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .zIndex(Z_STICKY),
                            contentAlignment = Alignment.Center
                        ) {
                            stickyBottomContent(
                                stickyBottomPaddings(
                                    contentScreenPaddings = screenPaddingsIgnoringSticky,
                                    layoutDirection = LocalLayoutDirection.current
                                )
                            )
                        }
                    }
                }

                if (isLoading) LoadingIndicator()
            }
        }
    }

    BackHandler(enabled = true) {
        contentErrorConfig?.let {
            contentErrorConfig.onCancel()
        } ?: onBack?.invoke()
    }

    broadcastAction?.let {
        SystemBroadcastReceiver(
            intentFilters = it.intentFilters
        ) { intent ->
            it.callback(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultToolBar(
    navigatableAction: ScreenNavigateAction,
    onBack: (() -> Unit)?,
    keyboardController: SoftwareKeyboardController?,
    toolbarConfig: ToolbarConfig?,
) {

    TopAppBar(
        title = {
            Text(
                text = toolbarConfig?.title.orEmpty(),
                color = MaterialTheme.colorScheme.primary
            )
        },
        navigationIcon = {
            // Check if we should add back/close button.
            if (navigatableAction != ScreenNavigateAction.NONE) {
                val navigationIcon = when (navigatableAction) {
                    ScreenNavigateAction.CANCELABLE -> AppIcons.Close
                    else -> AppIcons.ArrowBack
                }

                ToolbarItem(
                    toolbarAction = ToolbarActionUi(
                        icon = navigationIcon,
                        onClick = {
                            onBack?.invoke()
                            keyboardController?.hide()
                        }
                    )
                )
            }
        },
        // Add toolbar actions.
        actions = {
            ToolBarActions(
                toolBarActions = toolbarConfig?.actions,
                overflowThreshold = toolbarConfig?.maxVisibleActions ?: MAX_TOOLBAR_ACTIONS
            )
        }
    )
}

@Composable
internal fun ToolBarActions(
    toolBarActions: List<ToolbarActionUi>?,
    overflowThreshold: Int,
) {
    toolBarActions?.let { actions ->

        var dropDownMenuExpanded by remember { mutableStateOf(false) }

        // Show first [overflowThreshold] actions.
        actions
            .sortedByDescending { it.order }
            .take(overflowThreshold)
            .map { visibleToolbarAction ->
                ToolbarItem(toolbarAction = visibleToolbarAction)
            }

        // Check if there are more actions to show.
        if (actions.size > overflowThreshold) {
            Box {
                ToolbarItem(
                    toolbarAction = ToolbarActionUi(
                        icon = AppIcons.VerticalMore,
                        onClick = { dropDownMenuExpanded = !dropDownMenuExpanded },
                        enabled = true,
                    )
                )
                DropdownMenu(
                    expanded = dropDownMenuExpanded,
                    onDismissRequest = { dropDownMenuExpanded = false },
                    shape = RoundedCornerShape(SIZE_EXTRA_SMALL.dp)
                ) {
                    actions
                        .sortedByDescending { it.order }
                        .drop(overflowThreshold)
                        .map { dropDownMenuToolbarAction ->
                            ToolbarItem(
                                toolbarAction = dropDownMenuToolbarAction,
                                onItemClicked = { dropDownMenuExpanded = false }
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun ToolbarItem(
    toolbarAction: ToolbarActionUi,
    onItemClicked: () -> Unit = {}
) {
    val customIconTint = toolbarAction.customTint
        ?: MaterialTheme.colorScheme.onSurface

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        toolbarAction.icon?.let { safeIcon ->
            if (toolbarAction.clickable) {
                WrapIconButton(
                    iconData = safeIcon,
                    onClick = {
                        toolbarAction.onClick()
                        onItemClicked()
                    },
                    enabled = toolbarAction.enabled,
                    customTint = customIconTint,
                    throttleClicks = toolbarAction.throttleClicks
                )
            } else {
                WrapIcon(
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    iconData = safeIcon,
                    enabled = toolbarAction.enabled,
                    customTint = customIconTint,
                )
            }
        }

        toolbarAction.text?.let { safeText ->
            val textClickModifier: Modifier = if (toolbarAction.clickable) {
                if (toolbarAction.throttleClicks) {
                    Modifier.throttledClickable(
                        enabled = toolbarAction.enabled,
                        onClick = {
                            toolbarAction.onClick()
                            onItemClicked()
                        }
                    )
                } else {
                    Modifier.clickable(
                        enabled = toolbarAction.enabled,
                        onClick = {
                            toolbarAction.onClick()
                            onItemClicked()
                        }
                    )
                }
            } else {
                Modifier
            }

            Text(
                text = safeText,
                modifier = Modifier
                    .then(textClickModifier)
                    .padding(vertical = SPACING_SMALL.dp, horizontal = 12.dp)
                    .padding(end = SPACING_XX_LARGE.dp)
                    .alpha(
                        if (toolbarAction.enabled) ALPHA_ENABLED
                        else ALPHA_DISABLED
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@ThemeModePreviews
@Composable
private fun ToolbarItemClickablePreview() {
    PreviewTheme {
        val action = ToolbarActionUi(
            icon = AppIcons.Verified,
            onClick = {},
            enabled = true,
            clickable = true,
        )

        ToolbarItem(toolbarAction = action)
    }
}

@ThemeModePreviews
@Composable
private fun ToolbarItemNotClickablePreview() {
    PreviewTheme {
        val action = ToolbarActionUi(
            icon = AppIcons.Verified,
            onClick = {},
            enabled = true,
            clickable = false,
        )

        ToolbarItem(toolbarAction = action)
    }
}

@ThemeModePreviews
@Composable
private fun ToolBarActionsWithFourActionsPreview() {
    PreviewTheme {
        val toolBarActions = listOf(
            ToolbarActionUi(
                icon = AppIcons.Verified,
                onClick = {},
                enabled = true,
                clickable = true,
            ),
            ToolbarActionUi(
                icon = AppIcons.Verified,
                onClick = {},
                enabled = false,
                clickable = true,
            ),
            ToolbarActionUi(
                icon = AppIcons.Verified,
                onClick = {},
                enabled = true,
                clickable = false,
            ),
            ToolbarActionUi(
                icon = AppIcons.Verified,
                onClick = {},
                enabled = false,
                clickable = false,
            )
        )

        Row {
            ToolBarActions(
                toolBarActions = toolBarActions,
                overflowThreshold = MAX_TOOLBAR_ACTIONS,
            )
        }
    }
}