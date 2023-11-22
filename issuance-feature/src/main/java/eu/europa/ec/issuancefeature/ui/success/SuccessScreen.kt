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

package eu.europa.ec.issuancefeature.ui.success

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
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
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.allCorneredShapeSmall
import eu.europa.ec.resourceslogic.theme.values.backgroundPaper
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.BigImageAndMediumIcon
import eu.europa.ec.uilogic.component.IconData
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.DialogBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapPrimaryButton
import eu.europa.ec.uilogic.component.wrap.WrapSecondaryButton
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuccessScreen(
    navController: NavController,
    viewModel: SuccessViewModel
) {
    val state = viewModel.viewState.value

    val isBottomSheetOpen = state.isBottomSheetOpen
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ContentScreen(
        navigatableAction = ScreenNavigateAction.NONE,
        isLoading = false,
        onBack = { viewModel.setEvent(Event.GoBack) },
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                when (navigationEffect) {
                    is Effect.Navigation.SwitchScreen -> {
                        navController.navigate(navigationEffect.screenRoute) {
                            popUpTo(IssuanceScreens.Success.screenRoute) {
                                inclusive = true
                            }
                        }
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
                    docType = state.docType,
                    onEventSent = {
                        viewModel.setEvent(it)
                    }
                )
            }
        }
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
            title = stringResource(id = R.string.issuance_success_title),
            subtitle = stringResource(id = R.string.issuance_success_subtitle, state.docType),
        )

        // Screen Main Content.
        UserImageAndIcon(
            modifier = Modifier.weight(1f),
            image = AppIcons.User,
            icon = AppIcons.IdStroke,
            username = "Jane Doe"
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
    docType: String,
    onEventSent: (event: Event) -> Unit
) {
    DialogBottomSheet(
        title = stringResource(id = R.string.issuance_success_bottom_sheet_cancel_title, docType),
        message = stringResource(
            id = R.string.issuance_success_bottom_sheet_cancel_subtitle,
            docType
        ),
        positiveButtonText = stringResource(id = R.string.issuance_success_bottom_sheet_cancel_primary_button_text),
        negativeButtonText = stringResource(id = R.string.issuance_success_bottom_sheet_cancel_secondary_button_text),
        onPositiveClick = { onEventSent(Event.BottomSheet.Cancel.PrimaryButtonPressed) },
        onNegativeClick = { onEventSent(Event.BottomSheet.Cancel.SecondaryButtonPressed) }
    )
}

@Composable
private fun UserImageAndIcon(
    modifier: Modifier = Modifier,
    image: IconData,
    icon: IconData,
    username: String,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.allCorneredShapeSmall
            )
            .padding(SPACING_LARGE.dp)
    ) {
        BigImageAndMediumIcon(
            image = image,
            icon = icon
        )
        VSpacer.Large()
        Text(
            text = username,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.backgroundPaper
        )
    }
}

@Composable
private fun StickyBottomSection(
    state: State,
    onEventSend: (Event) -> Unit,
) {
    Column {
        VSpacer.ExtraSmall()

        Text(
            text = stringResource(
                id = R.string.issuance_success_confirmation_message,
                state.docType
            ),
            style = MaterialTheme.typography.titleLarge
        )
        VSpacer.Medium()

        WrapPrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onEventSend(Event.PrimaryButtonPressed) }
        ) {
            Text(text = stringResource(id = R.string.issuance_success_primary_button_text))
        }
        VSpacer.Medium()

        WrapSecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onEventSend(Event.SecondaryButtonPressed) }
        ) {
            Text(text = stringResource(id = R.string.issuance_success_secondary_button_text))
        }
    }
}