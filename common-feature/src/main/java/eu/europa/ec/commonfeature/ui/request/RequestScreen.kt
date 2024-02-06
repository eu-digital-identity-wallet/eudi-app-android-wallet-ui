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

package eu.europa.ec.commonfeature.ui.request

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.ui.request.model.RequestDataUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.content.TitleWithBadge
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.DialogBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapIconButton
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapPrimaryButton
import eu.europa.ec.uilogic.component.wrap.WrapSecondaryButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestScreen(
    navController: NavController,
    viewModel: RequestViewModel,
) {
    val state = viewModel.viewState.value

    val isBottomSheetOpen = state.isBottomSheetOpen
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ContentScreen(
        navigatableAction = ScreenNavigateAction.NONE,
        isLoading = state.isLoading,
        onBack = { viewModel.setEvent(Event.GoBack) },
        contentErrorConfig = state.error
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                when (navigationEffect) {

                    is Effect.Navigation.SwitchScreen -> {
                        navController.navigate(navigationEffect.screenRoute)
                    }

                    is Effect.Navigation.Pop -> {
                        navController.popBackStack()
                    }

                    is Effect.Navigation.PopTo -> {
                        navController.popBackStack(
                            route = navigationEffect.screenRoute,
                            inclusive = false
                        )
                    }
                }
            },
            paddingValues = paddingValues,
            coroutineScope = scope,
            modalBottomSheetState = bottomSheetState
        )

        if (isBottomSheetOpen) {
            WrapModalBottomSheet(
                onDismissRequest = {
                    viewModel.setEvent(
                        Event.BottomSheet.UpdateBottomSheetState(
                            isOpen = false
                        )
                    )
                },
                sheetState = bottomSheetState
            ) {
                SheetContent(
                    sheetContent = state.sheetContent,
                    onEventSent = {
                        viewModel.setEvent(it)
                    }
                )
            }
        }
    }

    OneTimeLaunchedEffect {
        viewModel.setEvent(Event.DoWork)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (navigationEffect: Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
    coroutineScope: CoroutineScope,
    modalBottomSheetState: SheetState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        verticalArrangement = Arrangement.Top
    ) {
        // Screen Title.
        ContentTitle(
            titleWithBadge = state.screenTitle,
            onTitleWithBadgeClick = if (state.screenTitle.isTrusted) {
                { onEventSend(Event.BadgeClicked) }
            } else {
                null
            },
            subtitle = state.screenSubtitle,
            clickableSubtitle = state.screenClickableSubtitle,
            onSubtitleClick = { onEventSend(Event.SubtitleClicked) },
            subtitleTrailingContent = {
                val icon = when (state.isShowingFullUserInfo) {
                    true -> AppIcons.VisibilityOff
                    false -> AppIcons.Visibility
                }
                WrapIconButton(
                    iconData = icon,
                    enabled = !state.isLoading,
                    customTint = MaterialTheme.colorScheme.primary,
                    onClick = { onEventSend(Event.ChangeContentVisibility) }
                )
            }
        )

        // Screen Main Content.
        Request(
            modifier = Modifier.weight(1f),
            items = state.items,
            noData = state.noItems,
            isShowingFullUserInfo = state.isShowingFullUserInfo,
            onEventSend = onEventSend,
            listState = rememberLazyListState(),
            contentPadding = paddingValues
        )

        // Sticky Bottom Section.
        StickyBottomSection(
            state = state,
            onEventSend = onEventSend
        )
    }

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation -> onNavigationRequested(effect)

                is Effect.CloseBottomSheet -> {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                    }.invokeOnCompletion {
                        if (!modalBottomSheetState.isVisible) {
                            onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                        }
                    }
                }

                is Effect.ShowBottomSheet -> {
                    onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = true))
                }
            }
        }.collect()
    }
}

@Composable
private fun SheetContent(
    sheetContent: RequestBottomSheetContent,
    onEventSent: (event: Event) -> Unit
) {
    when (sheetContent) {
        RequestBottomSheetContent.BADGE -> {
            DialogBottomSheet(
                title = stringResource(id = R.string.request_bottom_sheet_badge_title),
                message = stringResource(id = R.string.request_bottom_sheet_badge_subtitle),
                positiveButtonText = stringResource(id = R.string.request_bottom_sheet_badge_primary_button_text),
                onPositiveClick = { onEventSent(Event.BottomSheet.Badge.PrimaryButtonPressed) },
            )
        }

        RequestBottomSheetContent.SUBTITLE -> {
            DialogBottomSheet(
                title = stringResource(id = R.string.request_bottom_sheet_subtitle_title),
                message = stringResource(id = R.string.request_bottom_sheet_subtitle_subtitle),
                positiveButtonText = stringResource(id = R.string.request_bottom_sheet_subtitle_primary_button_text),
                onPositiveClick = { onEventSent(Event.BottomSheet.Subtitle.PrimaryButtonPressed) },
            )
        }

        RequestBottomSheetContent.CANCEL -> {
            DialogBottomSheet(
                title = stringResource(id = R.string.request_bottom_sheet_cancel_title),
                message = stringResource(id = R.string.request_bottom_sheet_cancel_subtitle),
                positiveButtonText = stringResource(id = R.string.request_bottom_sheet_cancel_primary_button_text),
                negativeButtonText = stringResource(id = R.string.request_bottom_sheet_cancel_secondary_button_text),
                onPositiveClick = { onEventSent(Event.BottomSheet.Cancel.PrimaryButtonPressed) },
                onNegativeClick = { onEventSent(Event.BottomSheet.Cancel.SecondaryButtonPressed) }
            )
        }
    }
}

@Composable
fun StickyBottomSection(
    state: State,
    onEventSend: (Event) -> Unit,
) {
    Column {
        VSpacer.ExtraSmall()

        AnimatedVisibility(
            visible = state.items.any {
                it is RequestDataUi.OptionalField
                        && !it.optionalFieldItemUi.requestDocumentItemUi.checked
            }
        ) {
            Column {
                WarningCard(warningText = state.warningText)
                VSpacer.Medium()
            }
        }

        WrapPrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && !state.noItems,
            onClick = { onEventSend(Event.PrimaryButtonPressed) }
        ) {
            Text(text = stringResource(id = R.string.request_primary_button_text))
        }
        VSpacer.Medium()

        WrapSecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onEventSend(Event.SecondaryButtonPressed) }
        ) {
            Text(text = stringResource(id = R.string.request_secondary_button_text))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ThemeModePreviews
@Composable
private fun ContentPreview() {
    PreviewTheme {
        Content(
            state = State(
                screenTitle = TitleWithBadge(isTrusted = false),
                screenSubtitle = "Subtitle ",
                screenClickableSubtitle = "clickable subtitle",
                warningText = "Warning",
            ),
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(SPACING_MEDIUM.dp),
            coroutineScope = rememberCoroutineScope(),
            modalBottomSheetState = rememberModalBottomSheetState()
        )
    }
}

@ThemeModePreviews
@Composable
private fun SheetContentCancelPreview() {
    PreviewTheme {
        SheetContent(
            sheetContent = RequestBottomSheetContent.CANCEL,
            onEventSent = {}
        )
    }
}

@ThemeModePreviews
@Composable
private fun SheetContentSubtitlePreview() {
    PreviewTheme {
        SheetContent(
            sheetContent = RequestBottomSheetContent.SUBTITLE,
            onEventSent = {}
        )
    }
}

@ThemeModePreviews
@Composable
private fun StickyBottomSectionPreview() {
    PreviewTheme {
        StickyBottomSection(
            state = State(
                screenTitle = TitleWithBadge(isTrusted = false),
                screenSubtitle = "Subtitle ",
                screenClickableSubtitle = "clickable subtitle",
                warningText = "Warning",
            ),
            onEventSend = {}
        )
    }
}