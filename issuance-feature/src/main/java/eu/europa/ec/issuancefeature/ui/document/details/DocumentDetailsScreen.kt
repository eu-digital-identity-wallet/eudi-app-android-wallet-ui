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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
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
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.content.ToolbarAction
import eu.europa.ec.uilogic.component.content.ToolbarConfig
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.LifecycleEffect
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.wrap.BottomSheetTextData
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.DialogBottomSheet
import eu.europa.ec.uilogic.component.wrap.SimpleBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapButton
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
    viewModel: DocumentDetailsViewModel,
) {
    val state = viewModel.viewState.value

    val isBottomSheetOpen = state.isBottomSheetOpen
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    val toolbarConfig = ToolbarConfig(
        actions = if (state.error == null) {
            listOf(
                ToolbarAction(
                    icon = if (state.isDocumentBookmarked) AppIcons.BookmarkFilled else AppIcons.Bookmark,
                    onClick = { viewModel.setEvent(Event.BookmarkPressed) },
                    enabled = !state.isLoading,
                    throttleClicks = true,
                ),
                ToolbarAction(
                    icon = if (state.hideSensitiveContent) AppIcons.Visibility else AppIcons.VisibilityOff,
                    onClick = { viewModel.setEvent(Event.ChangeContentVisibility) },
                    enabled = !state.isLoading,
                    throttleClicks = false,
                )
            )
        } else {
            emptyList()
        }
    )

    ContentScreen(
        isLoading = state.isLoading,
        contentErrorConfig = state.error,
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { viewModel.setEvent(Event.Pop) },
        toolBarConfig = toolbarConfig,
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
    navController: NavController,
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
                PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 0.dp,
                    start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                    end = paddingValues.calculateStartPadding(LayoutDirection.Ltr)
                )
            )
        ) {
            // Screen title
            ContentTitle(title = state.title)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {

                SectionTitle(
                    modifier = Modifier.padding(vertical = SPACING_MEDIUM.dp),
                    text = state.documentDetailsSectionTitle,
                )

                WrapListItems(
                    items = documentUi.documentDetails,
                    hideSensitiveContent = state.hideSensitiveContent,
                    onItemClick = null,
                )

                if (state.issuerName != null || state.issuerLogo != null) {
                    SectionTitle(
                        modifier = Modifier.padding(
                            top = SPACING_LARGE.dp,
                            bottom = SPACING_MEDIUM.dp,
                        ),
                        text = state.documentIssuerSectionTitle,
                    )
                    IssuerDetailsCard(
                        item = IssuerDetailsCardData(
                            issuerName = state.issuerName,
                            issuerLogo = state.issuerLogo,
                            issuerIsVerified = false,
                        ),
                        onClick = { onEventSend(Event.IssuerCardPressed) }
                    )
                }

                ButtonsSection(
                    onEventSend = onEventSend
                )
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

                is Effect.BookmarkStored -> {
                    onEventSend(Event.OnBookmarkStored)
                }
            }
        }.collect()
    }
}

@Composable
private fun SheetContent(
    sheetContent: DocumentDetailsBottomSheetContent,
    onEventSent: (event: Event) -> Unit,
) {
    when (sheetContent) {
        is DocumentDetailsBottomSheetContent.DeleteDocumentConfirmation ->
            DialogBottomSheet(
                textData = BottomSheetTextData(
                    title = stringResource(
                        id = R.string.document_details_bottom_sheet_delete_title
                    ),
                    message = stringResource(
                        id = R.string.document_details_bottom_sheet_delete_subtitle
                    ),
                    positiveButtonText = stringResource(id = R.string.document_details_bottom_sheet_delete_primary_button_text),
                    negativeButtonText = stringResource(id = R.string.document_details_bottom_sheet_delete_secondary_button_text),
                    isPositiveButtonWarning = true,
                ),
                leadingIcon = AppIcons.Delete,
                leadingIconTint = MaterialTheme.colorScheme.error,
                onPositiveClick = { onEventSent(Event.BottomSheet.Delete.PrimaryButtonPressed) },
                onNegativeClick = { onEventSent(Event.BottomSheet.Delete.SecondaryButtonPressed) }
            )

        is DocumentDetailsBottomSheetContent.BookmarkStoredInfo -> {
            SimpleBottomSheet(
                textData = sheetContent.bottomSheetTextData,
                leadingIcon = AppIcons.BookmarkFilled,
                leadingIconTint = MaterialTheme.colorScheme.warning,
            )
        }

        is DocumentDetailsBottomSheetContent.TrustedRelyingPartyInfo -> {
            SimpleBottomSheet(
                textData = sheetContent.bottomSheetTextData,
                leadingIcon = AppIcons.Verified,
                leadingIconTint = MaterialTheme.colorScheme.success,
            )
        }
    }
}

@Composable
private fun ButtonsSection(onEventSend: (Event) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = SPACING_EXTRA_SMALL.dp,
                bottom = SPACING_MEDIUM.dp,
            )
    ) {
        WrapButton(
            modifier = Modifier.fillMaxWidth(),
            buttonConfig = ButtonConfig(
                type = ButtonType.PRIMARY,
                onClick = { onEventSend(Event.PrimaryButtonPressed) },
                isWithoutContainerBackground = true,
            )
        ) {
            Text(
                text = stringResource(id = R.string.document_details_primary_button_text),
                style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary)
            )
        }

        WrapButton(
            modifier = Modifier.fillMaxWidth(),
            buttonConfig = ButtonConfig(
                type = ButtonType.SECONDARY,
                onClick = { onEventSend(Event.SecondaryButtonPressed) },
                isWarning = true,
            )
        ) {
            Text(
                text = stringResource(id = R.string.document_details_secondary_button_text),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ThemeModePreviews
@Composable
private fun DocumentDetailsScreenPreview() {
    PreviewTheme {
        val state = State(
            documentDetailsSectionTitle = "DOCUMENT DETAILS",
            documentIssuerSectionTitle = "ISSUER",
            document = DocumentUi(
                documentId = "1",
                documentName = "National ID",
                documentIdentifier = DocumentIdentifier.MdocPid,
                documentExpirationDateFormatted = "30 Mar 2050",
                documentHasExpired = false,
                documentImage = "image1",
                documentDetails = emptyList(),
                documentIssuanceState = DocumentUiIssuanceState.Issued,
            ),
            sheetContent = DocumentDetailsBottomSheetContent.DeleteDocumentConfirmation
        )

        Content(
            state = state,
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(SPACING_MEDIUM.dp),
            coroutineScope = rememberCoroutineScope(),
            modalBottomSheetState = rememberModalBottomSheetState(),
        )
    }
}