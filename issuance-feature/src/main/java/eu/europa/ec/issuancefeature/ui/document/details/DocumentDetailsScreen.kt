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

package eu.europa.ec.issuancefeature.ui.document.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import eu.europa.ec.businesslogic.util.safeLet
import eu.europa.ec.commonfeature.config.IssuanceFlowUiConfig
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.commonfeature.model.DocumentUiIssuanceState
import eu.europa.ec.commonfeature.ui.document_details.DetailsContent
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.ActionTopBar
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.HeaderData
import eu.europa.ec.uilogic.component.HeaderLarge
import eu.europa.ec.uilogic.component.content.ContentGradient
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.GradientEdge
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.content.ToolbarAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.LifecycleEffect
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.wrap.DialogBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapPrimaryButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailsScreen(
    navController: NavController,
    viewModel: DocumentDetailsViewModel
) {
    val state = viewModel.viewState.value
    val topBarColor = MaterialTheme.colorScheme.secondary

    val isBottomSheetOpen = state.isBottomSheetOpen
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ContentScreen(
        isLoading = state.isLoading,
        contentErrorConfig = state.error,
        navigatableAction = state.navigatableAction,
        onBack = state.onBackAction,
        topBar = if (state.hasCustomTopBar) {
            {
                ActionTopBar(
                    contentColor = topBarColor,
                    iconColor = MaterialTheme.colorScheme.primary,
                    iconData = AppIcons.Close,
                    toolbarActions = listOf(
                        ToolbarAction(
                            icon = AppIcons.Delete,
                            onClick = { viewModel.setEvent(Event.DeleteDocumentPressed) },
                            enabled = !state.isLoading
                        )
                    )
                ) { viewModel.setEvent(Event.Pop) }
            }
        } else {
            null
        }
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(navigationEffect, navController)
            },
            paddingValues = paddingValues,
            headerColor = topBarColor,
            coroutineScope = scope,
            modalBottomSheetState = bottomSheetState,
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
                    documentTypeUiName = state.document?.documentName.orEmpty(),
                    onEventSent = {
                        viewModel.setEvent(it)
                    }
                )
            }
        }
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME
    ) {
        viewModel.setEvent(Event.Init)
    }
}

private fun handleNavigationEffect(
    navigationEffect: Effect.Navigation,
    navController: NavController
) {
    when (navigationEffect) {
        is Effect.Navigation.SwitchScreen -> {
            navController.navigate(navigationEffect.screenRoute) {
                popUpTo(navigationEffect.popUpToScreenRoute) {
                    inclusive = navigationEffect.inclusive
                }
            }
        }

        is Effect.Navigation.Pop -> navController.popBackStack()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
    headerColor: Color,
    coroutineScope: CoroutineScope,
    modalBottomSheetState: SheetState,
) {
    safeLet(state.document, state.headerData) { documentUi, headerData ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = rememberContentBottomPadding(
                        hasBottomPadding = state.hasBottomPadding,
                        paddingValues = paddingValues
                    )
                )
        ) {
            // Header
            HeaderLarge(
                data = headerData,
                containerColor = headerColor,
                contentPadding = PaddingValues(
                    start = SPACING_LARGE.dp,
                    end = SPACING_LARGE.dp,
                    bottom = SPACING_LARGE.dp,
                    top = paddingValues.calculateTopPadding()
                )
            )

            // Main Content
            MainContent(
                detailsHaveBottomGradient = state.detailsHaveBottomGradient,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    DetailsContent(
                        modifier = Modifier
                            .padding(
                                start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                                end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                            ),
                        data = documentUi.documentDetails
                    )
                }
            }

            // Sticky Button
            if (state.shouldShowPrimaryButton) {
                WrapPrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                            end = paddingValues.calculateEndPadding(LayoutDirection.Ltr)
                        ),
                    onClick = {
                        onEventSend(Event.PrimaryButtonPressed)
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.issuance_document_details_primary_button_text),
                        style = MaterialTheme.typography.titleSmall
                    )
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
    documentTypeUiName: String,
    onEventSent: (event: Event) -> Unit
) {
    DialogBottomSheet(
        title = stringResource(
            id = R.string.document_details_bottom_sheet_delete_title,
            documentTypeUiName
        ),
        message = stringResource(
            id = R.string.document_details_bottom_sheet_delete_subtitle,
            documentTypeUiName
        ),
        positiveButtonText = stringResource(id = R.string.document_details_bottom_sheet_delete_primary_button_text),
        negativeButtonText = stringResource(id = R.string.document_details_bottom_sheet_delete_secondary_button_text),
        onPositiveClick = { onEventSent(Event.BottomSheet.Delete.PrimaryButtonPressed) },
        onNegativeClick = { onEventSent(Event.BottomSheet.Delete.SecondaryButtonPressed) }
    )
}

@Composable
private fun ColumnScope.MainContent(
    detailsHaveBottomGradient: Boolean,
    content: @Composable () -> Unit
) {
    if (detailsHaveBottomGradient) {
        ContentGradient(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            gradientEdge = GradientEdge.BOTTOM
        ) {
            content()
        }
    } else {
        content()
    }
}

@Composable
private fun rememberContentBottomPadding(
    hasBottomPadding: Boolean,
    paddingValues: PaddingValues
): Dp {
    return remember(hasBottomPadding) {
        if (hasBottomPadding) {
            paddingValues.calculateBottomPadding()
        } else {
            0.dp
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ThemeModePreviews
@Composable
private fun IssuanceDocumentDetailsScreenPreview() {
    PreviewTheme {
        val state = State(
            detailsType = IssuanceFlowUiConfig.NO_DOCUMENT,
            navigatableAction = ScreenNavigateAction.NONE,
            shouldShowPrimaryButton = true,
            hasCustomTopBar = false,
            hasBottomPadding = true,
            detailsHaveBottomGradient = true,
            document = DocumentUi(
                documentId = "2",
                documentName = "National ID",
                documentIdentifier = DocumentIdentifier.PID,
                documentExpirationDateFormatted = "30 Mar 2050",
                documentHasExpired = false,
                documentImage = "image3",
                documentDetails = emptyList(),
                documentIssuanceState = DocumentUiIssuanceState.Issued,
            ),
            headerData = HeaderData(
                title = "Title",
                subtitle = "subtitle",
                documentHasExpired = false,
                base64Image = "",
                icon = AppIcons.IdStroke
            )
        )

        Content(
            state = state,
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(SPACING_LARGE.dp),
            headerColor = MaterialTheme.colorScheme.secondary,
            coroutineScope = rememberCoroutineScope(),
            modalBottomSheetState = rememberModalBottomSheetState(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ThemeModePreviews
@Composable
private fun DashboardDocumentDetailsScreenPreview() {
    PreviewTheme {
        val state = State(
            detailsType = IssuanceFlowUiConfig.EXTRA_DOCUMENT,
            navigatableAction = ScreenNavigateAction.CANCELABLE,
            shouldShowPrimaryButton = false,
            hasCustomTopBar = true,
            hasBottomPadding = false,
            detailsHaveBottomGradient = false,
            document = DocumentUi(
                documentId = "2",
                documentName = "National ID",
                documentIdentifier = DocumentIdentifier.PID,
                documentExpirationDateFormatted = "30 Mar 2050",
                documentHasExpired = false,
                documentImage = "image3",
                documentDetails = emptyList(),
                documentIssuanceState = DocumentUiIssuanceState.Issued,
            ),
            headerData = HeaderData(
                title = "Title",
                subtitle = "subtitle",
                documentHasExpired = false,
                base64Image = "",
                icon = AppIcons.IdStroke
            )
        )

        Content(
            state = state,
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(SPACING_LARGE.dp),
            headerColor = MaterialTheme.colorScheme.secondary,
            coroutineScope = rememberCoroutineScope(),
            modalBottomSheetState = rememberModalBottomSheetState(),
        )
    }
}