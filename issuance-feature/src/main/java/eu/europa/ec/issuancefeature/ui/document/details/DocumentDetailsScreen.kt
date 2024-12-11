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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.config.IssuanceFlowUiConfig
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.commonfeature.model.DocumentUiIssuanceState
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.success
import eu.europa.ec.resourceslogic.theme.values.warning
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.IssuerDetailsCard
import eu.europa.ec.uilogic.component.IssuerDetailsCardData
import eu.europa.ec.uilogic.component.SectionTitle
import eu.europa.ec.uilogic.component.content.ContentGradient
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.GradientEdge
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.content.ToolbarAction
import eu.europa.ec.uilogic.component.content.ToolbarConfig
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.LifecycleEffect
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.BottomSheetTextData
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.DialogBottomSheet
import eu.europa.ec.uilogic.component.wrap.GenericBaseSheetContent
import eu.europa.ec.uilogic.component.wrap.WrapButton
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapListItems
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
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
        toolBarConfig = ToolbarConfig(
            actions = listOf(
                ToolbarAction(
                    icon = state.bookmarkIcon,
                    customTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {
                        viewModel.setEvent(
                            Event.BookmarkPressed(isBookmarked = state.isDocumentBookmarked)
                        )
                    },
                    enabled = !state.isLoading
                ),
                ToolbarAction(
                    icon = state.sensitiveInfoIcon,
                    customTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { viewModel.setEvent(Event.ChangeContentVisibility) },
                    enabled = !state.isLoading
                )
            ),
            navigationIconTint = MaterialTheme.colorScheme.onSurfaceVariant
        ),
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(navigationEffect, navController)
            },
            paddingValues = paddingValues,
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
                    sheetContent = state.sheetContent,
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
    coroutineScope: CoroutineScope,
    modalBottomSheetState: SheetState,
) {
    state.document?.let { documentUi ->
        Column(
            modifier = Modifier.padding(
                start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                top = paddingValues.calculateTopPadding(),
                bottom = rememberContentBottomPadding(
                    hasBottomPadding = state.hasBottomPadding,
                    paddingValues = paddingValues
                )
            )
        ) {
            // Screen Headline
            ContentTitle(
                title = state.headline
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Main Content
                MainContent(
                    detailsHaveBottomGradient = state.detailsHaveBottomGradient,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Document Details title and content
                        SectionTitle(
                            modifier = Modifier.padding(vertical = SPACING_MEDIUM.dp),
                            sectionTitle = state.documentDetailsSectionTitle.orEmpty()
                        )

                        WrapListItems(
                            items = documentUi.documentDetails,
                            hideSensitiveContent = state.isShowingFullUserInfo.not(),
                            onItemClick = null
                        )

                        // Issuer title and content
                        SectionTitle(
                            modifier = Modifier.padding(
                                top = SPACING_LARGE.dp,
                                bottom = SPACING_MEDIUM.dp,
                            ),
                            sectionTitle = state.documentIssuerSectionTitle.orEmpty()
                        )

                        IssuerDetailsCard(
                            item = IssuerDetailsCardData(
                                issuerName = stringResource(R.string.placeholder_content_issuer),
                                issuerLogo = AppIcons.IssuerPlaceholder,
                                issuerCategory = stringResource(R.string.placeholder_content_category),
                                issuerLocation = stringResource(R.string.placeholder_content_location),
                                issuerIsVerified = true,
                            ),
                            onClick = {
                                onEventSend(Event.IssuerCardPressed)
                            }
                        )
                    }
                }

                if (state.shouldShowActionButtons) {
                    WrapButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = SPACING_EXTRA_SMALL.dp),
                        buttonConfig = ButtonConfig(
                            type = ButtonType.PRIMARY,
                            isWithoutContainerBackground = true,
                            onClick = {
                                onEventSend(Event.PrimaryButtonPressed)
                            }
                        )
                    ) {
                        Text(
                            text = stringResource(id = R.string.document_details_primary_button_text),
                            style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.primary)
                        )
                    }

                    WrapButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = SPACING_MEDIUM.dp),
                        buttonConfig = ButtonConfig(
                            type = ButtonType.SECONDARY,
                            isWarning = true,
                            onClick = {
                                onEventSend(Event.SecondaryButtonPressed)
                            }
                        )
                    ) {
                        Text(
                            text = stringResource(id = R.string.document_details_secondary_button_text),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
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
    sheetContent: DocumentDetailsBottomSheetContent,
    onEventSent: (event: Event) -> Unit
) {
    when (sheetContent) {
        is DocumentDetailsBottomSheetContent.DeleteDocumentConfirmation ->
            DialogBottomSheet(
                textData = BottomSheetTextData(
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
                    isPositiveButtonWarning = true
                ),
                leadingIcon = AppIcons.Delete,
                leadingIconTint = MaterialTheme.colorScheme.error,
                onPositiveClick = { onEventSent(Event.BottomSheet.Delete.PrimaryButtonPressed) },
                onNegativeClick = { onEventSent(Event.BottomSheet.Delete.SecondaryButtonPressed) }
            )

        is DocumentDetailsBottomSheetContent.BookmarkStoredInfo -> {
            GenericBaseSheetContent(
                titleContent = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp)
                    ) {
                        WrapIcon(
                            iconData = AppIcons.BookmarkFilled,
                            customTint = MaterialTheme.colorScheme.warning
                        )
                        Text(
                            text = stringResource(R.string.document_details_bottom_sheet_bookmark_info_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }, bodyContent = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.document_details_bottom_sheet_bookmark_info_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            )
        }

        is DocumentDetailsBottomSheetContent.IssuerInfo -> {
            GenericBaseSheetContent(
                titleContent = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp)
                    ) {
                        WrapIcon(
                            iconData = AppIcons.Verified,
                            customTint = MaterialTheme.colorScheme.success
                        )
                        Text(
                            text = stringResource(R.string.document_details_bottom_sheet_badge_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }, bodyContent = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.document_details_bottom_sheet_badge_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            )
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@ThemeModePreviews
@Composable
private fun IssuanceDocumentDetailsScreenPreview() {
    PreviewTheme {
        val state = State(
            detailsType = IssuanceFlowUiConfig.NO_DOCUMENT,
            navigatableAction = ScreenNavigateAction.NONE,
            shouldShowActionButtons = true,
            hasCustomTopBar = false,
            hasBottomPadding = true,
            detailsHaveBottomGradient = true,
            documentDetailsSectionTitle = "DOCUMENT DETAILS",
            documentIssuerSectionTitle = "ISSUER",
            document = DocumentUi(
                documentId = "2",
                documentName = "National ID",
                documentIdentifier = DocumentIdentifier.MdocPid,
                documentExpirationDateFormatted = "30 Mar 2050",
                documentHasExpired = false,
                documentImage = "image3",
                documentDetails = emptyList(),
                documentIssuanceState = DocumentUiIssuanceState.Issued,
            ),
        )

        Content(
            state = state,
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(SPACING_LARGE.dp),
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
            shouldShowActionButtons = false,
            hasCustomTopBar = true,
            hasBottomPadding = false,
            detailsHaveBottomGradient = false,
            documentDetailsSectionTitle = "DOCUMENT DETAILS",
            documentIssuerSectionTitle = "ISSUER",
            document = DocumentUi(
                documentId = "2",
                documentName = "National ID",
                documentIdentifier = DocumentIdentifier.MdocPid,
                documentExpirationDateFormatted = "30 Mar 2050",
                documentHasExpired = false,
                documentImage = "image3",
                documentDetails = emptyList(),
                documentIssuanceState = DocumentUiIssuanceState.Issued,
            ),
        )

        Content(
            state = state,
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(SPACING_LARGE.dp),
            coroutineScope = rememberCoroutineScope(),
            modalBottomSheetState = rememberModalBottomSheetState(),
        )
    }
}