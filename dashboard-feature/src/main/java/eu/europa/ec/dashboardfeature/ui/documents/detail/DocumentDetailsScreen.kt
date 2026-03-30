/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.dashboardfeature.ui.documents.detail

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.util.CoreActions
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentDetailsUi
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentIssuanceStateUi
import eu.europa.ec.dashboardfeature.ui.documents.model.DocumentCredentialsInfoUi
import eu.europa.ec.dashboardfeature.util.TestTag
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.success
import eu.europa.ec.resourceslogic.theme.values.warning
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.IssuerDetailsCard
import eu.europa.ec.uilogic.component.IssuerDetailsCardDataUi
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemLeadingContentDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.SectionTitle
import eu.europa.ec.uilogic.component.content.BroadcastAction
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.content.ToolbarActionUi
import eu.europa.ec.uilogic.component.content.ToolbarConfig
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.LifecycleEffect
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.wrap.BottomSheetTextDataUi
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.DialogBottomSheet
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import eu.europa.ec.uilogic.component.wrap.SimpleBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapButton
import eu.europa.ec.uilogic.component.wrap.WrapListItems
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import eu.europa.ec.uilogic.extension.applyTestTag
import eu.europa.ec.uilogic.extension.cacheUri
import eu.europa.ec.uilogic.extension.getPendingUri
import eu.europa.ec.uilogic.extension.paddingFrom
import eu.europa.ec.uilogic.navigation.helper.handleDeepLinkAction
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
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()

    val isBottomSheetOpen = state.isBottomSheetOpen
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val context = LocalContext.current

    val toolbarConfig = getToolbarConfig(
        context = context,
        state = state,
        onEventSend = { viewModel.setEvent(it) }
    )

    ContentScreen(
        isLoading = state.isLoading,
        contentErrorConfig = state.error,
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { viewModel.setEvent(Event.Pop) },
        toolBarConfig = toolbarConfig,
        broadcastAction = BroadcastAction(
            intentFilters = listOf(
                CoreActions.REVOCATION_WORK_REFRESH_DETAILS_ACTION,
                CoreActions.VCI_RESUME_ACTION,
                CoreActions.VCI_DYNAMIC_PRESENTATION
            ),
            callback = {
                when (it?.action) {
                    CoreActions.VCI_RESUME_ACTION -> it.extras?.getString("uri")?.let { link ->
                        viewModel.setEvent(Event.OnResumeIssuance(link))
                    }

                    CoreActions.VCI_DYNAMIC_PRESENTATION -> it.extras?.getString("uri")
                        ?.let { link ->
                            viewModel.setEvent(Event.OnDynamicPresentation(link))
                        }

                    CoreActions.REVOCATION_IDS_DETAILS_EXTRA -> {
                        val ids = it
                            .getStringArrayListExtra(CoreActions.REVOCATION_IDS_DETAILS_EXTRA)
                            ?.toList()
                            ?: emptyList()

                        viewModel.setEvent(Event.OnRevocationStatusChanged(ids))
                    }
                }
            }
        )
    ) { paddingValues ->
        Content(
            context = context,
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(
                    context = context,
                    navigationEffect = navigationEffect,
                    navController = navController
                )
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
        lifecycleEvent = Lifecycle.Event.ON_PAUSE
    ) {
        viewModel.setEvent(Event.OnPause)
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME
    ) {
        viewModel.setEvent(Event.Init(context.getPendingUri()))
    }
}

@Composable
private fun getToolbarConfig(
    context: Context,
    state: State,
    onEventSend: (Event) -> Unit
): ToolbarConfig {
    return ToolbarConfig(
        actions = if (state.error == null) {
            listOf(
                ToolbarActionUi(
                    icon = if (state.isDocumentBookmarked) AppIcons.BookmarkFilled else AppIcons.Bookmark,
                    onClick = { onEventSend(Event.BookmarkPressed) },
                    enabled = !state.isLoading,
                    throttleClicks = true,
                ),
                ToolbarActionUi(
                    text = stringResource(R.string.document_details_toolbar_action_reissue),
                    icon = null,
                    onClick = { onEventSend(Event.IssuerDetails.OnActionButtonClicked(context)) },
                    enabled = !state.isLoading && state.issuerDetails?.documentState != IssuerDetailsCardDataUi.DocumentState.Revoked,
                    throttleClicks = true,
                ),
                ToolbarActionUi(
                    text = stringResource(R.string.document_details_toolbar_action_remove),
                    icon = null,
                    onClick = { onEventSend(Event.SecondaryButtonPressed) },
                    enabled = !state.isLoading,
                    throttleClicks = true,
                )
            )
        } else {
            emptyList()
        },
        maxVisibleActions = 1
    )
}

private fun handleNavigationEffect(
    context: Context,
    navigationEffect: Effect.Navigation,
    navController: NavController,
) {
    when (navigationEffect) {
        is Effect.Navigation.SwitchScreen -> {
            navController.navigate(navigationEffect.screenRoute) {
                navigationEffect.popUpToScreenRoute?.let { safePopUpToScreenRoute ->
                    popUpTo(safePopUpToScreenRoute) {
                        inclusive = navigationEffect.inclusive == true
                    }
                }
            }
        }

        is Effect.Navigation.DeepLink -> {
            navigationEffect.routeToPop?.let {
                context.cacheUri(navigationEffect.link)
                navController.popBackStack(
                    route = it,
                    inclusive = false
                )
            } ?: handleDeepLinkAction(navController, navigationEffect.link)
        }

        is Effect.Navigation.Pop -> navController.popBackStack()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    context: Context,
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
    coroutineScope: CoroutineScope,
    modalBottomSheetState: SheetState,
) {
    state.documentDetailsUi?.let { safeDocumentDetailsUi ->
        Column(
            modifier = Modifier
                .paddingFrom(paddingValues, bottom = false)
        ) {

            // Screen title
            state.title?.let { safeTitle ->
                ContentTitle(
                    title = safeTitle,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(SPACING_EXTRA_LARGE.dp)
            ) {
                state.issuerDetails?.let { safeIssuerDetails ->
                    IssuerDetails(
                        modifier = Modifier.fillMaxWidth(),
                        data = safeIssuerDetails,
                        onExpandedStateChanged = {
                            onEventSend(Event.IssuerDetails.OnExpandedStateChanged)
                        },
                        onActionButtonClick = {
                            onEventSend(Event.IssuerDetails.OnActionButtonClicked(context))
                        }
                    )
                }

                DocumentDetails(
                    modifier = Modifier.fillMaxWidth(),
                    onEventSend = onEventSend,
                    documentDetailsUi = safeDocumentDetailsUi,
                    hideSensitiveContent = state.hideSensitiveContent,
                    isLoading = state.isLoading
                )

                BottomSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    credentialsInfoUi = state.documentCredentialsInfoUi,
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

                is Effect.BookmarkRemoved -> {
                    onEventSend(Event.OnBookmarkRemoved)
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
                textData = BottomSheetTextDataUi(
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
                positiveButtonTestTag = TestTag.DocumentDetailsScreen.BOTTOM_SHEET_DELETE_DOCUMENT_POSITIVE_BUTTON,
                onNegativeClick = { onEventSent(Event.BottomSheet.Delete.SecondaryButtonPressed) },
                negativeButtonTestTag = TestTag.DocumentDetailsScreen.BOTTOM_SHEET_DELETE_DOCUMENT_NEGATIVE_BUTTON,
            )

        is DocumentDetailsBottomSheetContent.BookmarkStoredInfo -> {
            SimpleBottomSheet(
                textData = sheetContent.bottomSheetTextData,
                leadingIcon = AppIcons.BookmarkFilled,
                leadingIconTint = MaterialTheme.colorScheme.warning,
            )
        }

        is DocumentDetailsBottomSheetContent.BookmarkRemovedInfo -> {
            SimpleBottomSheet(
                textData = sheetContent.bottomSheetTextData,
                leadingIcon = AppIcons.BookmarkFilled,
                leadingIconTint = MaterialTheme.colorScheme.error,
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
private fun IssuerDetails(
    modifier: Modifier = Modifier,
    data: IssuerDetailsCardDataUi,
    onExpandedStateChanged: () -> Unit,
    onActionButtonClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        SectionTitle(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.document_details_issuer_section_text),
        )
        IssuerDetailsCard(
            modifier = Modifier.fillMaxWidth(),
            data = data,
            onExpandedChange = onExpandedStateChanged,
            onActionButtonClick = onActionButtonClick,
        )
    }
}

@Composable
private fun DocumentDetails(
    modifier: Modifier = Modifier,
    onEventSend: (Event) -> Unit,
    documentDetailsUi: DocumentDetailsUi,
    hideSensitiveContent: Boolean,
    isLoading: Boolean,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        SectionTitle(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.document_details_main_section_text),
            icon = if (hideSensitiveContent) AppIcons.Visibility else AppIcons.VisibilityOff,
            onIconClick = { onEventSend(Event.ChangeContentVisibility) },
            iconEnabled = !isLoading,
            throttleIconClicks = false,
        )

        WrapListItems(
            modifier = Modifier.fillMaxWidth(),
            items = documentDetailsUi.documentClaims,
            hideSensitiveContent = hideSensitiveContent,
            onExpandedChange = { item ->
                onEventSend(Event.ClaimClicked(itemId = item.itemId))
            },
            onItemClick = null,
            throttleClicks = false,
        )
    }
}

@Composable
private fun BottomSection(
    modifier: Modifier = Modifier,
    credentialsInfoUi: DocumentCredentialsInfoUi?,
    onEventSend: (Event) -> Unit
) {
    Column(modifier = modifier) {
        WrapButton(
            modifier = Modifier
                .applyTestTag(TestTag.DocumentDetailsScreen.DELETE_BUTTON)
                .fillMaxWidth()
                .padding(bottom = SPACING_LARGE.dp),
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

        credentialsInfoUi?.let { safeCredentialsInfoUi ->
            Text(
                text = safeCredentialsInfoUi.title,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ThemeModePreviews
@Composable
private fun DocumentDetailsScreenPreview() {
    PreviewTheme {
        val availableCredentials = 3
        val totalCredentials = 15
        val state = State(
            issuerDetails = IssuerDetailsCardDataUi(
                issuerName = "Issuer Name",
                issuerLogo = null,
                documentState = IssuerDetailsCardDataUi.DocumentState.Issued(
                    issuanceDate = "16 February 2024 - 13:18",
                    expirationDate = "22 March 2030"
                ),
                isExpanded = true,
            ),
            documentCredentialsInfoUi = DocumentCredentialsInfoUi(
                availableCredentials = availableCredentials,
                totalCredentials = totalCredentials,
                title = stringResource(
                    R.string.document_details_document_credentials_info_text,
                    availableCredentials,
                    totalCredentials
                ),
            ),
            documentDetailsUi = DocumentDetailsUi(
                documentId = "1",
                issuerId = "Id",
                documentConfigId = "Id",
                documentName = "Mobile Driving License",
                documentIdentifier = DocumentIdentifier.OTHER(formatType = "org.iso.18013.5.1.mDL"),
                documentClaims = listOf(
                    ExpandableListItemUi.SingleListItem(
                        header = ListItemDataUi(
                            itemId = "1",
                            mainContentData = ListItemMainContentDataUi.Text(text = ""),
                            overlineText = "A reproduction of the mDL holder’s portrait.",
                            leadingContentData = ListItemLeadingContentDataUi.UserImage(
                                userBase64Image = ""
                            ),
                        )
                    ),
                    ExpandableListItemUi.SingleListItem(
                        header = ListItemDataUi(
                            itemId = "2",
                            mainContentData = ListItemMainContentDataUi.Text(text = "GR"),
                            overlineText = "Alpha-2 country code, as defined in ISO 3166-1 of the issuing authority’s country or territory.",
                        )
                    ),
                    ExpandableListItemUi.SingleListItem(
                        header = ListItemDataUi(
                            itemId = "3",
                            mainContentData = ListItemMainContentDataUi.Text(text = "12345678900"),
                            overlineText = "An audit control number assigned by the issuing authority.",
                        )
                    ),
                    ExpandableListItemUi.SingleListItem(
                        header = ListItemDataUi(
                            itemId = "4",
                            mainContentData = ListItemMainContentDataUi.Text(text = "31 Dec 2040"),
                            overlineText = "Date when mDL expires.",
                        )
                    )
                ),
                documentIssuanceStateUi = DocumentIssuanceStateUi.Issued,
            ),
            hideSensitiveContent = false,
            sheetContent = DocumentDetailsBottomSheetContent.DeleteDocumentConfirmation
        )

        Content(
            context = LocalContext.current,
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