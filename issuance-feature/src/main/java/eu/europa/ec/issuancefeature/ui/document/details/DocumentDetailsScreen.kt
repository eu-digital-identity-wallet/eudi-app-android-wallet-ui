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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import eu.europa.ec.businesslogic.util.safeLet
import eu.europa.ec.commonfeature.config.IssuanceFlowUiConfig
import eu.europa.ec.commonfeature.model.DocumentTypeUi
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.commonfeature.ui.document_details.model.DocumentDetailsUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.ActionTopBar
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.HeaderData
import eu.europa.ec.uilogic.component.HeaderLarge
import eu.europa.ec.uilogic.component.InfoTextWithNameAndValueData
import eu.europa.ec.uilogic.component.content.ContentGradient
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.GradientEdge
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.details.DetailsContent
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.LifecycleEffect
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.wrap.WrapPrimaryButton
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun DocumentDetailsScreen(
    navController: NavController,
    viewModel: DocumentDetailsViewModel
) {
    val state = viewModel.viewState.value
    val topBarColor = MaterialTheme.colorScheme.secondary

    ContentScreen(
        isLoading = state.isLoading,
        contentErrorConfig = state.error,
        navigatableAction = state.navigatableAction,
        onBack = when (state.detailsType) {
            IssuanceFlowUiConfig.NO_DOCUMENT -> {
                null
            }

            IssuanceFlowUiConfig.EXTRA_DOCUMENT -> {
                { viewModel.setEvent(Event.Pop) }
            }
        },
        topBar = if (state.hasCustomTopBar) {
            {
                ActionTopBar(
                    contentColor = topBarColor,
                    iconColor = MaterialTheme.colorScheme.primary,
                    iconData = AppIcons.Close
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
            headerColor = topBarColor
        )
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
                popUpTo(IssuanceScreens.DocumentDetails.screenRoute) {
                    inclusive = true
                }
            }
        }

        is Effect.Navigation.Pop -> navController.popBackStack()
    }
}

@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
    headerColor: Color,
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
                        data = documentUi.documentDetails.mapNotNull { documentDetailsUi ->
                            when (documentDetailsUi) {
                                is DocumentDetailsUi.DefaultItem -> {
                                    documentDetailsUi.infoText.infoValues
                                        ?.toTypedArray()
                                        ?.let { infoValues ->
                                            InfoTextWithNameAndValueData.create(
                                                title = documentDetailsUi.infoText.title,
                                                *infoValues
                                            )
                                        }
                                }

                                DocumentDetailsUi.Unknown -> null
                            }
                        }
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
            }
        }.collect()
    }
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
                documentName = "Digital Id",
                documentType = DocumentTypeUi.DIGITAL_ID,
                documentExpirationDateFormatted = "30 Mar 2050",
                documentImage = "image3",
                documentDetails = emptyList()
            ),
            headerData = HeaderData(
                title = "Title",
                subtitle = "subtitle",
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
        )
    }
}

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
                documentName = "Digital Id",
                documentType = DocumentTypeUi.DIGITAL_ID,
                documentExpirationDateFormatted = "30 Mar 2050",
                documentImage = "image3",
                documentDetails = emptyList()
            ),
            headerData = HeaderData(
                title = "Title",
                subtitle = "subtitle",
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
        )
    }
}