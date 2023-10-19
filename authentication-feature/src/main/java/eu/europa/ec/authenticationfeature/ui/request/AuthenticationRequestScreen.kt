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

package eu.europa.ec.authenticationfeature.ui.request

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.primaryDark
import eu.europa.ec.resourceslogic.theme.values.textPrimaryDark
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.CardWithIconAndText
import eu.europa.ec.uilogic.component.CheckboxWithContent
import eu.europa.ec.uilogic.component.InfoTextWithNameAndValue
import eu.europa.ec.uilogic.component.InfoTextWithNameAndValueData
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.CheckboxData
import eu.europa.ec.uilogic.component.wrap.DialogBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapIconButton
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapPrimaryButton
import eu.europa.ec.uilogic.component.wrap.WrapSecondaryButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationRequestScreen(
    navController: NavController,
    viewModel: AuthenticationRequestViewModel
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
        viewModel.setEvent(Event.Init)
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
            title = state.screenTitle,
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
                    customTint = MaterialTheme.colorScheme.primary,
                    onClick = { onEventSend(Event.ChangeContentVisibility) }
                )
            }
        )

        if (!state.isLoading) {
            // Screen Main Content.
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp)
            ) {

                item {
                    VSpacer.Small()
                }

                item {
                    RelayingPartyCard(
                        cardText = state.cardText,
                        paddingValues = paddingValues
                    )
                    VSpacer.Small()
                }

                itemsIndexed(
                    items = state.userDataUi
                ) { index, userDataUi ->
                    CheckboxWithContent(
                        checkboxData = CheckboxData(
                            isChecked = userDataUi.checked,
                            enabled = userDataUi.enabled,
                            onCheckedChange = {
                                onEventSend(
                                    Event.UserDataItemCheckedStatusChanged(
                                        items = state.userDataUi,
                                        itemId = index
                                    )
                                )
                            }
                        )
                    ) {
                        val infoValueStyle = if (userDataUi.checked) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyLarge
                        }
                        if (state.isShowingFullUserInfo) {
                            InfoTextWithNameAndValue(
                                itemData = InfoTextWithNameAndValueData(
                                    infoName = userDataUi.userDataDomain.name,
                                    infoValue = userDataUi.userDataDomain.value,
                                ),
                                infoValueTextStyle = infoValueStyle
                            )
                        } else {
                            Text(
                                text = userDataUi.userDataDomain.name,
                                style = infoValueStyle
                            )
                        }
                    }

                    if (index != state.userDataUi.lastIndex) {
                        Divider()
                    }
                }
            }

            // Sticky Bottom Section.
            Column {
                VSpacer.ExtraSmall()

                AnimatedVisibility(
                    visible = state.userDataUi.any {
                        !it.checked
                    }
                ) {
                    WarningCard(warningText = state.warningText)
                }
                VSpacer.Medium()

                WrapPrimaryButton(
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onEventSend(Event.PrimaryButtonPressed) }
                ) {
                    Text(text = stringResource(id = R.string.online_authentication_userData_primary_button_text))
                }
                VSpacer.Medium()

                WrapSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onEventSend(Event.SecondaryButtonPressed) }
                ) {
                    Text(text = stringResource(id = R.string.online_authentication_userData_secondary_button_text))
                }
            }
        }
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
    sheetContent: AuthenticationRequestBottomSheetContent,
    onEventSent: (event: Event) -> Unit
) {
    when (sheetContent) {
        AuthenticationRequestBottomSheetContent.SUBTITLE -> {
            DialogBottomSheet(
                title = stringResource(id = R.string.online_authentication_userData_bottom_sheet_subtitle_title),
                message = stringResource(id = R.string.online_authentication_userData_bottom_sheet_subtitle_subtitle),
                positiveButtonText = stringResource(id = R.string.online_authentication_userData_bottom_sheet_subtitle_primary_button_text),
                onPositiveClick = { onEventSent(Event.BottomSheet.Subtitle.PrimaryButtonPressed) },
            )
        }

        AuthenticationRequestBottomSheetContent.CANCEL -> {
            DialogBottomSheet(
                title = stringResource(id = R.string.online_authentication_userData_bottom_sheet_cancel_title),
                message = stringResource(id = R.string.online_authentication_userData_bottom_sheet_cancel_subtitle),
                positiveButtonText = stringResource(id = R.string.online_authentication_userData_bottom_sheet_cancel_primary_button_text),
                negativeButtonText = stringResource(id = R.string.online_authentication_userData_bottom_sheet_cancel_secondary_button_text),
                onPositiveClick = { onEventSent(Event.BottomSheet.Cancel.PrimaryButtonPressed) },
                onNegativeClick = { onEventSent(Event.BottomSheet.Cancel.SecondaryButtonPressed) }
            )
        }
    }
}

@Composable
private fun RelayingPartyCard(
    cardText: String,
    paddingValues: PaddingValues
) {
    CardWithIconAndText(
        modifier = Modifier.padding(
            start = paddingValues.calculateStartPadding(LayoutDirection.Ltr)
        ),
        text = {
            Text(
                text = cardText,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primaryDark
            )
        },
        icon = {
            WrapIcon(
                modifier = Modifier.size(50.dp),
                iconData = AppIcons.Id,
                customTint = MaterialTheme.colorScheme.primary
            )
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryDark.copy(alpha = 0.12f),
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    )
}

@Composable
private fun WarningCard(warningText: String) {
    CardWithIconAndText(
        text = {
            Text(
                text = warningText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.textPrimaryDark
            )
        },
        icon = {
            WrapIcon(
                modifier = Modifier.size(32.dp),
                iconData = AppIcons.Error,
                customTint = MaterialTheme.colorScheme.secondary
            )
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp)
    )
}