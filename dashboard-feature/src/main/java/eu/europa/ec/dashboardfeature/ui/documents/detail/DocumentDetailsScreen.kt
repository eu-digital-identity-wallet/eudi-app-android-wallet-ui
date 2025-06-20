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

package eu.europa.ec.dashboardfeature.ui.documents.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
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
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.BottomSheetTextDataUi
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.DialogBottomSheet
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import eu.europa.ec.uilogic.component.wrap.SimpleBottomSheet
import eu.europa.ec.uilogic.component.wrap.TextConfig
import eu.europa.ec.uilogic.component.wrap.WrapButton
import eu.europa.ec.uilogic.component.wrap.WrapCard
import eu.europa.ec.uilogic.component.wrap.WrapListItems
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapText
import eu.europa.ec.uilogic.extension.clickableNoRipple
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.net.URI

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
        skipPartiallyExpanded = false
    )

    val toolbarConfig = ToolbarConfig(
        actions = if (state.error == null) {
            listOf(
                ToolbarActionUi(
                    icon = if (state.isDocumentBookmarked) AppIcons.BookmarkFilled else AppIcons.Bookmark,
                    onClick = { viewModel.setEvent(Event.BookmarkPressed) },
                    enabled = !state.isLoading,
                    throttleClicks = true,
                ),
                ToolbarActionUi(
                    icon = if (state.hideSensitiveContent) AppIcons.VisibilityOff else AppIcons.Visibility,
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
        broadcastAction = BroadcastAction(
            intentFilters = listOf(
                CoreActions.REVOCATION_WORK_REFRESH_DETAILS_ACTION
            ),
            callback = {
                val ids = it
                    ?.getStringArrayListExtra(CoreActions.REVOCATION_IDS_DETAILS_EXTRA)
                    ?.toList()
                    ?: emptyList()

                viewModel.setEvent(Event.OnRevocationStatusChanged(ids))
            }
        )
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
    state.documentDetailsUi?.let { safeDocumentDetailsUi ->
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
            state.title?.let { safeTitle ->
                ContentTitle(
                    title = safeTitle,
                )
            }

            AnimatedVisibility(
                visible = state.isRevoked
            ) {
                WrapCard(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(SPACING_MEDIUM.dp)
                    ) {
                        WrapText(
                            text = stringResource(
                                R.string.document_details_revoked_document_message
                            ),
                            textConfig = TextConfig(
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = Int.MAX_VALUE
                            )
                        )
                    }
                }

                VSpacer.Large()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                state.documentCredentialsInfoUi?.let { safeDocumentCredentialsInfo ->
                    ExpandableDocumentCredentialsSection(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = SPACING_SMALL.dp),
                        documentCredentialsInfoUi = safeDocumentCredentialsInfo,
                        isExpanded = state.documentCredentialsInfoIsExpanded,
                        onExpandedStateChanged = {
                            onEventSend(Event.ToggleExpansionStateOfDocumentCredentialsSection)
                        }
                    )
                    VSpacer.ExtraLarge()
                }

                DocumentDetails(
                    modifier = Modifier.fillMaxWidth(),
                    onEventSend = onEventSend,
                    sectionTitle = state.documentDetailsSectionTitle,
                    documentDetailsUi = safeDocumentDetailsUi,
                    hideSensitiveContent = state.hideSensitiveContent,
                )

                if (state.issuerName != null || state.issuerLogo != null) {
                    VSpacer.ExtraLarge()

                    IssuerDetails(
                        modifier = Modifier.fillMaxWidth(),
                        sectionTitle = state.documentIssuerSectionTitle,
                        issuerName = state.issuerName,
                        issuerLogo = state.issuerLogo,
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
                onNegativeClick = { onEventSent(Event.BottomSheet.Delete.SecondaryButtonPressed) }
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
    sectionTitle: String,
    issuerName: String?,
    issuerLogo: URI?,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        SectionTitle(
            modifier = Modifier.fillMaxWidth(),
            text = sectionTitle,
        )
        IssuerDetailsCard(
            modifier = Modifier.fillMaxWidth(),
            item = IssuerDetailsCardDataUi(
                issuerName = issuerName,
                issuerLogo = issuerLogo,
                issuerIsVerified = false,
            ),
            onClick = null,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ExpandableDocumentCredentialsSection(
    modifier: Modifier = Modifier,
    documentCredentialsInfoUi: DocumentCredentialsInfoUi,
    isExpanded: Boolean,
    onExpandedStateChanged: () -> Unit,
) {
    SharedTransitionLayout {
        AnimatedContent(
            targetState = isExpanded,
            modifier = modifier,
        ) { providedIsExpanded: Boolean ->
            if (providedIsExpanded) {
                documentCredentialsInfoUi.expandedInfo?.let { safeExpandedInfo ->
                    ExpandedDocumentCredentials(
                        modifier = Modifier.fillMaxWidth(),
                        title = documentCredentialsInfoUi.title,
                        expandedInfo = safeExpandedInfo,
                        onHideClicked = onExpandedStateChanged,
                        animatedVisibilityScope = this@AnimatedContent,
                        sharedTransitionScope = this@SharedTransitionLayout,
                    )
                }
            } else {
                documentCredentialsInfoUi.collapsedInfo?.let { safeCollapsedInfo ->
                    CollapsedDocumentCredentials(
                        modifier = Modifier.fillMaxWidth(),
                        title = documentCredentialsInfoUi.title,
                        collapsedInfo = safeCollapsedInfo,
                        onMoreInfoClicked = onExpandedStateChanged,
                        animatedVisibilityScope = this@AnimatedContent,
                        sharedTransitionScope = this@SharedTransitionLayout,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ExpandedDocumentCredentials(
    modifier: Modifier,
    title: String,
    expandedInfo: DocumentCredentialsInfoUi.ExpandedInfo,
    onHideClicked: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    with(sharedTransitionScope) {
        WrapCard(
            modifier = modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = SHARED_BOUNDS_KEY),
                    animatedVisibilityScope = animatedVisibilityScope,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = SPACING_MEDIUM.dp),
                verticalArrangement = Arrangement.spacedBy(SPACING_EXTRA_LARGE.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.success,
                    )
                    Text(
                        text = expandedInfo.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WrapButton(
                        modifier = Modifier.wrapContentWidth(),
                        buttonConfig = ButtonConfig(
                            type = ButtonType.PRIMARY,
                            onClick = onHideClicked,
                            buttonColors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                        )
                    ) {
                        Text(
                            text = expandedInfo.hideButtonText,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CollapsedDocumentCredentials(
    modifier: Modifier,
    title: String,
    collapsedInfo: DocumentCredentialsInfoUi.CollapsedInfo,
    onMoreInfoClicked: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    with(sharedTransitionScope) {
        Row(
            modifier = modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = SHARED_BOUNDS_KEY),
                    animatedVisibilityScope = animatedVisibilityScope,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WrapCard {
                Text(
                    modifier = Modifier
                        .padding(
                            vertical = SPACING_SMALL.dp,
                            horizontal = SPACING_MEDIUM.dp
                        ),
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.success,
                )
            }

            Text(
                modifier = Modifier.clickableNoRipple(
                    onClick = onMoreInfoClicked
                ),
                text = collapsedInfo.moreInfoText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
            )
        }
    }
}

@Composable
private fun DocumentDetails(
    modifier: Modifier = Modifier,
    onEventSend: (Event) -> Unit,
    sectionTitle: String,
    documentDetailsUi: DocumentDetailsUi,
    hideSensitiveContent: Boolean,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        SectionTitle(
            modifier = Modifier.fillMaxWidth(),
            text = sectionTitle,
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
private fun ButtonsSection(onEventSend: (Event) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = SPACING_MEDIUM.dp
            )
    ) {
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

private const val SHARED_BOUNDS_KEY = "bounds"

@OptIn(ExperimentalMaterial3Api::class)
@ThemeModePreviews
@Composable
private fun DocumentDetailsScreenPreview() {
    PreviewTheme {
        val availableCredentials = 3
        val totalCredentials = 15
        val state = State(
            documentCredentialsInfoUi = DocumentCredentialsInfoUi(
                availableCredentials = availableCredentials,
                totalCredentials = totalCredentials,
                title = stringResource(
                    R.string.document_details_document_credentials_info_text,
                    availableCredentials,
                    totalCredentials
                ),
                collapsedInfo = DocumentCredentialsInfoUi.CollapsedInfo(
                    moreInfoText = stringResource(R.string.document_details_document_credentials_info_more_info_text),
                ),
                expandedInfo = DocumentCredentialsInfoUi.ExpandedInfo(
                    subtitle = stringResource(R.string.document_details_document_credentials_info_expanded_text_subtitle),
                    updateNowButtonText = null,
                    hideButtonText = stringResource(R.string.document_details_document_credentials_info_expanded_button_hide_text),
                ),
            ),
            documentCredentialsInfoIsExpanded = false,
            documentDetailsSectionTitle = stringResource(R.string.document_details_main_section_text),
            documentIssuerSectionTitle = stringResource(R.string.document_details_issuer_section_text),
            documentDetailsUi = DocumentDetailsUi(
                documentId = "1",
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
            issuerName = "Digital Credentials Issuer",
            hideSensitiveContent = false,
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

@ThemeModePreviews
@Composable
private fun ExpandableDocumentCredentialsSectionPreview() {
    PreviewTheme {
        val availableCredentials = 3
        val totalCredentials = 15
        val documentCredentialsInfoUi = DocumentCredentialsInfoUi(
            availableCredentials = availableCredentials,
            totalCredentials = totalCredentials,
            title = stringResource(
                R.string.document_details_document_credentials_info_text,
                availableCredentials,
                totalCredentials
            ),
            collapsedInfo = DocumentCredentialsInfoUi.CollapsedInfo(
                moreInfoText = stringResource(R.string.document_details_document_credentials_info_more_info_text),
            ),
            expandedInfo = DocumentCredentialsInfoUi.ExpandedInfo(
                subtitle = stringResource(R.string.document_details_document_credentials_info_expanded_text_subtitle),
                updateNowButtonText = null,
                hideButtonText = stringResource(R.string.document_details_document_credentials_info_expanded_button_hide_text),
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = SPACING_MEDIUM.dp),
            verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
        ) {
            ExpandableDocumentCredentialsSection(
                modifier = Modifier.fillMaxWidth(),
                documentCredentialsInfoUi = documentCredentialsInfoUi,
                isExpanded = false,
                onExpandedStateChanged = {},
            )

            ExpandableDocumentCredentialsSection(
                modifier = Modifier.fillMaxWidth(),
                documentCredentialsInfoUi = documentCredentialsInfoUi,
                isExpanded = true,
                onExpandedStateChanged = {},
            )
        }
    }
}