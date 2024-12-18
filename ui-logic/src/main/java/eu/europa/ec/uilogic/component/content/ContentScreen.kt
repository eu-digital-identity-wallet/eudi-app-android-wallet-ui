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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.zIndex
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.IconData
import eu.europa.ec.uilogic.component.loader.LoadingIndicator
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.MAX_TOOLBAR_ACTIONS
import eu.europa.ec.uilogic.component.utils.TopSpacing
import eu.europa.ec.uilogic.component.utils.Z_STICKY
import eu.europa.ec.uilogic.component.utils.screenPaddings
import eu.europa.ec.uilogic.component.utils.stickyBottomPaddings
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapIconButton

enum class LoadingType {
    NORMAL, NONE
}

data class ToolbarAction(
    val icon: IconData,
    val order: Int = 100,
    val enabled: Boolean = true,
    val customTint: Color? = null,
    val clickable: Boolean = true,
    val throttleClicks: Boolean = true,
    val onClick: () -> Unit,
)

data class ToolbarConfig(
    val title: String = "",
    val actions: List<ToolbarAction> = listOf(),
    val navigationIconTint: Color? = null,
)

enum class ScreenNavigateAction {
    BACKABLE, CANCELABLE, NONE
}

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
    bodyContent: @Composable (PaddingValues) -> Unit
) {
    ContentScreen(
        loadingType = if (isLoading) LoadingType.NORMAL else LoadingType.NONE,
        toolBarConfig = toolBarConfig,
        navigatableAction = navigatableAction,
        onBack = onBack,
        topBar = topBar,
        bottomBar = bottomBar,
        stickyBottom = stickyBottom,
        fab = fab,
        fabPosition = fabPosition,
        snackbarHost = snackbarHost,
        contentErrorConfig = contentErrorConfig,
        bodyContent = bodyContent
    )
}

@Composable
fun ContentScreen(
    loadingType: LoadingType = LoadingType.NONE,
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
    bodyContent: @Composable (PaddingValues) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val hasToolBar = contentErrorConfig != null
            || navigatableAction != ScreenNavigateAction.NONE
            || topBar != null
    val topSpacing = if (hasToolBar) TopSpacing.WithToolbar else TopSpacing.WithoutToolbar

    Scaffold(
        topBar = {
            if (topBar != null && contentErrorConfig == null) topBar.invoke()
            else if (hasToolBar) {
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
        bottomBar = bottomBar ?: {},
        floatingActionButton = fab,
        floatingActionButtonPosition = fabPosition,
        snackbarHost = snackbarHost,
    ) { padding ->

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            if (contentErrorConfig != null) {
                ContentError(
                    config = contentErrorConfig,
                    paddingValues = screenPaddings(padding)
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {

                    Box(modifier = Modifier.weight(1f)) {
                        bodyContent(screenPaddings(padding, topSpacing))
                    }

                    stickyBottom?.let { stickyBottomContent ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(Z_STICKY),
                            contentAlignment = Alignment.Center
                        ) {
                            stickyBottomContent(
                                stickyBottomPaddings(
                                    contentScreenPaddings = screenPaddings(padding),
                                    layoutDirection = LocalLayoutDirection.current
                                )
                            )
                        }
                    }
                }

                if (loadingType == LoadingType.NORMAL) LoadingIndicator()
            }
        }
    }

    BackHandler(enabled = true) {
        contentErrorConfig?.let {
            contentErrorConfig.onCancel()
        } ?: onBack?.invoke()
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
    // Precompute toolbar actions
    val precomputedToolbarActions = remember(toolbarConfig?.actions, navigatableAction, onBack) {
        val additionalActions: List<ToolbarAction> = buildList {
            // Add Cancelable action if needed
            if (navigatableAction == ScreenNavigateAction.CANCELABLE) {
                add(
                    ToolbarAction(
                        icon = AppIcons.Close,
                        order = Int.MAX_VALUE,// Ensure it appears as the rightmost action
                        onClick = {
                            onBack?.invoke()
                            keyboardController?.hide()
                        }
                    )
                )
            }
        }

        return@remember precomputeToolbarActions(
            toolBarActions = (toolbarConfig?.actions.orEmpty() + additionalActions)
        )
    }

    TopAppBar(
        title = {
            Text(
                text = toolbarConfig?.title.orEmpty(),
                color = MaterialTheme.colorScheme.primary
            )
        },
        navigationIcon = {
            // Show navigation icon for BACKABLE action only
            if (navigatableAction == ScreenNavigateAction.BACKABLE) {
                ToolbarIcon(
                    toolbarAction = ToolbarAction(
                        icon = AppIcons.ArrowBack,
                        onClick = {
                            onBack?.invoke()
                            keyboardController?.hide()
                        },
                        customTint = toolbarConfig?.navigationIconTint
                    )
                )
            }
        },
        // Add toolbar actions.
        actions = {
            ToolBarActions(precomputedToolbarActions = precomputedToolbarActions)
        }
    )
}

/**
 * Renders the toolbar actions for a screen.
 *
 * This composable displays toolbar actions based on the provided [PrecomputedToolbarActions].
 * It handles rendering visible actions, overflow actions (in a dropdown menu), and the close action.
 *
 * Visible actions are displayed directly on the toolbar.
 * Overflow actions, if any, are grouped under a "More" icon (vertical ellipsis) in a dropdown menu.
 * The close action, if provided, is displayed as the rightmost icon on the toolbar.
 *
 * @param precomputedToolbarActions The pre-calculated toolbar actions to render. This includes
 * visible actions, overflow actions, and the close action.
 */
@Composable
internal fun ToolBarActions(
    precomputedToolbarActions: PrecomputedToolbarActions
) {
    var dropDownMenuExpanded by remember { mutableStateOf(false) }

    // Render visible actions
    precomputedToolbarActions.visibleActions.forEach { action ->
        ToolbarIcon(toolbarAction = action)
    }

    // Render Vertical More if overflow exists
    if (precomputedToolbarActions.actionsOverflow) {
        Box {
            ToolbarIcon(
                toolbarAction = ToolbarAction(
                    icon = AppIcons.VerticalMore,
                    onClick = { dropDownMenuExpanded = !dropDownMenuExpanded },
                    enabled = true,
                    customTint = MaterialTheme.colorScheme.primary
                )
            )
            DropdownMenu(
                expanded = dropDownMenuExpanded,
                onDismissRequest = { dropDownMenuExpanded = false }
            ) {
                precomputedToolbarActions.overflowActions.forEach { overflowAction ->
                    ToolbarIcon(toolbarAction = overflowAction)
                }
            }
        }
    }

    // Render Close action as the rightmost icon, if present
    precomputedToolbarActions.closeAction?.let {
        ToolbarIcon(toolbarAction = it)
    }
}

@Composable
private fun ToolbarIcon(toolbarAction: ToolbarAction) {
    val customIconTint = toolbarAction.customTint
        ?: MaterialTheme.colorScheme.onSurface

    if (toolbarAction.clickable) {
        WrapIconButton(
            iconData = toolbarAction.icon,
            onClick = toolbarAction.onClick,
            enabled = toolbarAction.enabled,
            customTint = customIconTint,
            throttleClicks = toolbarAction.throttleClicks
        )
    } else {
        WrapIcon(
            modifier = Modifier.minimumInteractiveComponentSize(),
            iconData = toolbarAction.icon,
            enabled = toolbarAction.enabled,
            customTint = customIconTint,
        )
    }
}

data class PrecomputedToolbarActions(
    val visibleActions: List<ToolbarAction>,
    val overflowActions: List<ToolbarAction>,
    val closeAction: ToolbarAction?,
    val actionsOverflow: Boolean,
)

/**
 * Precomputes the toolbar actions to be displayed, separating them into visible, overflow, and close actions.
 *
 * This function takes a list of [ToolbarAction]s and determines which actions should be visible on the toolbar,
 * which should be placed in the overflow menu, and which action represents the close action.
 *
 * @param toolBarActions The list of toolbar actions to precompute.
 * @param maxActionsShown The maximum number of actions to show directly on the toolbar. Defaults to [MAX_TOOLBAR_ACTIONS].
 *
 * @return A [PrecomputedToolbarActions] object containing the visible actions, overflow actions, close action,
 * and a flag indicating if there are actions in the overflow menu.
 */
fun precomputeToolbarActions(
    toolBarActions: List<ToolbarAction>?,
    maxActionsShown: Int = MAX_TOOLBAR_ACTIONS,
): PrecomputedToolbarActions {
    if (toolBarActions.isNullOrEmpty()) {
        return PrecomputedToolbarActions(
            visibleActions = emptyList(),
            overflowActions = emptyList(),
            closeAction = null,
            actionsOverflow = false,
        )
    }

    val (closeActions, otherActions) = toolBarActions.partition { it.icon == AppIcons.Close }
    val closeAction = closeActions.firstOrNull()

    val sortedActions = otherActions.sortedByDescending { it.order }
    val visibleActions = sortedActions.take(maxActionsShown)
    val overflowActions = sortedActions.drop(maxActionsShown)
    val actionsOverflow = overflowActions.isNotEmpty()

    return PrecomputedToolbarActions(
        visibleActions = visibleActions,
        overflowActions = overflowActions,
        closeAction = closeAction,
        actionsOverflow = actionsOverflow,
    )
}

@ThemeModePreviews
@Composable
private fun ToolbarIconClickablePreview() {
    PreviewTheme {
        val action = ToolbarAction(
            icon = AppIcons.Verified,
            onClick = {},
            enabled = true,
            clickable = true,
        )

        ToolbarIcon(toolbarAction = action)
    }
}

@ThemeModePreviews
@Composable
private fun ToolbarIconNotClickablePreview() {
    PreviewTheme {
        val action = ToolbarAction(
            icon = AppIcons.Verified,
            onClick = {},
            enabled = true,
            clickable = false,
        )

        ToolbarIcon(toolbarAction = action)
    }
}

@ThemeModePreviews
@Composable
private fun ToolBarActionsWithFourActionsPreview() {
    PreviewTheme {
        val toolBarActions = listOf(
            ToolbarAction(
                icon = AppIcons.Verified,
                onClick = {},
                enabled = true,
                clickable = true,
            ),
            ToolbarAction(
                icon = AppIcons.Verified,
                onClick = {},
                enabled = false,
                clickable = true,
            ),
            ToolbarAction(
                icon = AppIcons.Verified,
                onClick = {},
                enabled = true,
                clickable = false,
            ),
            ToolbarAction(
                icon = AppIcons.Verified,
                onClick = {},
                enabled = false,
                clickable = false,
            )
        )
        val precomputedToolbarActions = precomputeToolbarActions(
            toolBarActions = toolBarActions
        )

        Row {
            ToolBarActions(
                precomputedToolbarActions = precomputedToolbarActions
            )
        }
    }
}