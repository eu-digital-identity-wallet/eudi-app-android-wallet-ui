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

package eu.europa.ec.onlineAuthentication.ui.userData

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.ContentError
import eu.europa.ec.uilogic.component.ContentScreen
import eu.europa.ec.uilogic.component.ContentTitle
import eu.europa.ec.uilogic.component.DialogBottomSheet
import eu.europa.ec.uilogic.component.InfoTextWithNameAndValue
import eu.europa.ec.uilogic.component.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.ScreenNavigateAction
import eu.europa.ec.uilogic.component.VSpacer
import eu.europa.ec.uilogic.component.WrapPrimaryButton
import eu.europa.ec.uilogic.component.WrapSecondaryButton
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDataScreen(
    navController: NavController,
    viewModel: UserDataViewModel
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
        onBack = { viewModel.setEvent(Event.GoBack) }
    ) { paddingValues ->
        UserDataScreenView(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                when (navigationEffect) {

                    is Effect.Navigation.SwitchScreen -> {
                        navController.navigate(navigationEffect.screenRoute)
                    }

                    is Effect.Navigation.PopBackStackUpTo -> {
                        navController.popBackStack(
                            route = navigationEffect.screenRoute,
                            inclusive = navigationEffect.inclusive
                        )
                    }
                }
            },
            paddingValues = paddingValues,
            coroutineScope = scope,
            modalBottomSheetState = bottomSheetState
        )

        if (isBottomSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setEvent(Event.UpdateBottomSheetState(isOpen = false)) },
                sheetState = bottomSheetState,
                dragHandle = null
            ) {
                DialogBottomSheet(
                    title = stringResource(id = R.string.online_authentication_userData_bottom_sheet_title),
                    message = stringResource(id = R.string.online_authentication_userData_bottom_sheet_subtitle),
                    positiveButtonText = stringResource(id = R.string.online_authentication_userData_bottom_sheet_primary_button_text),
                    negativeButtonText = stringResource(id = R.string.online_authentication_userData_bottom_sheet_secondary_button_text),
                    onPositiveClick = { viewModel.setEvent(Event.SheetPrimaryButtonPressed) },
                    onNegativeClick = { viewModel.setEvent(Event.SheetSecondaryButtonPressed) }
                )
            }
        }
    }

    state.error?.let {
        ContentError(
            errorSubTitle = it.errorMsg,
            onRetry = {
                viewModel.setEvent(it.event)
            },
            onCancel = {
                viewModel.setEvent(Event.DismissError)
            }
        )
    }

    OneTimeLaunchedEffect {
        viewModel.setEvent(Event.Init)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserDataScreenView(
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
            subtitle = state.screenSubtitle
        )

        // Screen Main Content.
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
        ) {
            state.userData?.let { userData ->
                item {
                    InfoTextWithNameAndValue(
                        infoName = stringResource(id = R.string.online_authentication_userData_id_number),
                        infoValue = userData.id
                    )
                }

                item {
                    InfoTextWithNameAndValue(
                        infoName = stringResource(id = R.string.online_authentication_userData_date_of_birth),
                        infoValue = userData.dateOfBirth
                    )
                }

                item {
                    InfoTextWithNameAndValue(
                        infoName = stringResource(id = R.string.online_authentication_userData_tax_clearance_number),
                        infoValue = userData.taxClearanceNumber
                    )
                }
            }
        }

        // Sticky Buttons.
        Column {
            VSpacer.ExtraSmall()
            WrapPrimaryButton(
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

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation -> onNavigationRequested(effect)

                is Effect.CloseBottomSheet -> {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                    }.invokeOnCompletion {
                        if (!modalBottomSheetState.isVisible) {
                            onEventSend(Event.UpdateBottomSheetState(isOpen = false))
                        }
                    }
                }

                is Effect.ShowBottomSheet -> {
                    onEventSend(Event.UpdateBottomSheetState(isOpen = true))
                }
            }
        }.collect()
    }
}