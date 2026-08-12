/*
 * Copyright (c) 2026 European Commission
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.ui.request.model.DocumentFormatDomain
import eu.europa.ec.commonfeature.ui.request.model.DocumentPayloadDomain
import eu.europa.ec.commonfeature.ui.request.model.RelyingPartyHeaderUi
import eu.europa.ec.commonfeature.ui.request.model.RequestCombinationUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDataUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi
import eu.europa.ec.commonfeature.util.TestTag
import eu.europa.ec.corelogic.model.ClaimDomain
import eu.europa.ec.corelogic.model.ClaimPathDomain
import eu.europa.ec.corelogic.model.ClaimType
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.warning
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ErrorInfo
import eu.europa.ec.uilogic.component.InfoLinkSection
import eu.europa.ec.uilogic.component.InfoSection
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemSupportingContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.RelyingParty
import eu.europa.ec.uilogic.component.RelyingPartyDataUi
import eu.europa.ec.uilogic.component.SectionTitle
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.BottomSheetTextDataUi
import eu.europa.ec.uilogic.component.wrap.CheckboxDataUi
import eu.europa.ec.uilogic.component.wrap.DialogBottomSheet
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import eu.europa.ec.uilogic.component.wrap.SimpleBottomSheet
import eu.europa.ec.uilogic.component.wrap.TextConfig
import eu.europa.ec.uilogic.component.wrap.TextStyleKey
import eu.europa.ec.uilogic.component.wrap.WrapExpandableListItem
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapSelectableCard
import eu.europa.ec.uilogic.extension.applyTestTag
import eu.europa.ec.uilogic.extension.finish
import eu.europa.ec.uilogic.extension.openUrl
import eu.europa.ec.uilogic.navigation.helper.IntentAction
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
    intentAction: IntentAction?,
    navController: NavController,
    viewModel: RequestViewModel,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val isBottomSheetOpen = state.isBottomSheetOpen
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ContentScreen(
        navigatableAction = ScreenNavigateAction.BACKABLE,
        isLoading = state.isLoading,
        onBack = { viewModel.setEvent(Event.OnBack) },
        stickyBottom = { paddingValues ->
            ConsentStickyBottomSection(
                modifier = Modifier.fillMaxWidth(),
                paddingValues = paddingValues,
                buttonsTestTag = TestTag.RequestScreen.BUTTON,
                warningSection = ConsentWarningSection(
                    registrationWarning = state.registrationWarning,
                    notVerifiedWarningText = stringResource(R.string.request_registration_not_verified_warning_text),
                    overaskedWarningText = stringResource(R.string.request_registration_overasked_warning_text),
                    acknowledgeText = stringResource(R.string.request_registration_acknowledge_text),
                    onAcknowledgeChange = { isAccepted ->
                        viewModel.setEvent(
                            Event.RegistrationRiskToggled(isAccepted = isAccepted)
                        )
                    },
                ),
                primaryButtonText = stringResource(R.string.request_sticky_button_text),
                cancelButtonText = stringResource(R.string.request_cancel_button_text),
                primaryButtonEnabled = !state.isLoading && state.allowShare,
                onPrimaryButtonClick = { viewModel.setEvent(Event.StickyButtonPressed) },
                onCancelButtonClick = { viewModel.setEvent(Event.OnBack) },
            )
        },
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

                    is Effect.Navigation.Finish -> {
                        context.finish()
                    }

                    is Effect.Navigation.OpenUrlExternally -> {
                        context.openUrl(uri = navigationEffect.url)
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
                        when (state.sheetContent) {
                            is RequestBottomSheetContent.Warning -> {
                                Event.BottomSheet.UpdateBottomSheetState(isOpen = false)
                            }

                            is RequestBottomSheetContent.VerifierNotTrusted -> {
                                Event.BottomSheet.VerifierNotTrusted.Close
                            }
                        }
                    )
                },
                sheetState = bottomSheetState
            ) {
                SheetContent(
                    sheetContent = state.sheetContent,
                    onEventSent = { viewModel.setEvent(it) },
                )
            }
        }
    }

    OneTimeLaunchedEffect {
        viewModel.setEvent(Event.Init(intentAction = intentAction))
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
    val rendersDocuments = state.requestDataUi is RequestDataUi.Single ||
            state.requestDataUi is RequestDataUi.Multiple

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                other = if (rendersDocuments) {
                    Modifier.verticalScroll(rememberScrollState())
                } else {
                    Modifier
                }
            )
            .padding(paddingValues),
        verticalArrangement = Arrangement.Top
    ) {
        // Screen Header.
        ContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.request_screen_title),
        )

        state.relyingPartyHeader?.let { safeRelyingPartyHeader ->
            VerifierHeaderSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SPACING_SMALL.dp),
                header = safeRelyingPartyHeader,
                onEventSend = onEventSend,
            )
        }

        // Screen Main Content.
        DisplayRequestContent(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SPACING_SMALL.dp),
            requestDataUi = state.requestDataUi,
            claimsAreSelectable = state.claimsAreSelectable,
            onEventSend = onEventSend,
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
                            onEventSend(Event.BottomSheet.FinishedClosing)
                        } else {
                            onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = true))
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

/**
 * The who-is-asking header: the requester's identity and the verified registration sections
 * (privacy policy, intended use).
 */
@Composable
private fun VerifierHeaderSection(
    modifier: Modifier,
    header: RelyingPartyHeaderUi,
    onEventSend: (Event) -> Unit,
) {
    Column(modifier = modifier) {
        RelyingParty(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SPACING_SMALL.dp),
            relyingPartyData = header.relyingParty,
        )

        header.privacyPolicyUrl?.let { safePrivacyPolicyUrl ->
            InfoLinkSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SPACING_SMALL.dp),
                title = stringResource(R.string.request_privacy_policy_section_title),
                linkText = safePrivacyPolicyUrl,
                onLinkClick = { onEventSend(Event.PrivacyPolicyLinkClicked) },
            )
        }

        header.intendedUse?.let { safeIntendedUse ->
            InfoSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SPACING_SMALL.dp),
                title = stringResource(R.string.request_intended_use_section_title),
                body = safeIntendedUse,
            )
        }
    }
}

@Composable
private fun DisplayRequestContent(
    modifier: Modifier,
    requestDataUi: RequestDataUi,
    claimsAreSelectable: Boolean,
    onEventSend: (Event) -> Unit,
) {
    when (requestDataUi) {
        is RequestDataUi.Initial -> Unit // Nothing to render until the request resolves.

        is RequestDataUi.NoData -> ErrorInfo(
            modifier = modifier.fillMaxSize(),
            informativeText = stringResource(id = R.string.request_no_data),
        )

        is RequestDataUi.Single -> Column(
            modifier = modifier,
        ) {
            RequestedDataSectionTitle(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SPACING_SMALL.dp),
            )
            DisplayRequestItems(
                modifier = Modifier.fillMaxWidth(),
                requestDocuments = requestDataUi.combination.documents,
                claimsAreSelectable = claimsAreSelectable,
                onEventSend = onEventSend,
            )
        }

        is RequestDataUi.Multiple -> Column(
            modifier = modifier,
        ) {
            RequestedDataSectionTitle(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SPACING_SMALL.dp),
            )
            DisplayCombinationCards(
                modifier = Modifier.fillMaxWidth(),
                requestDataUi = requestDataUi,
                claimsAreSelectable = claimsAreSelectable,
                onEventSend = onEventSend,
            )
        }
    }
}

@Composable
private fun RequestedDataSectionTitle(
    modifier: Modifier,
) {
    SectionTitle(
        modifier = modifier,
        text = stringResource(R.string.request_requested_data_section_title),
        textConfig = TextConfig(
            styleKey = TextStyleKey.LabelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = Int.MAX_VALUE,
        )
    )
}

@Composable
private fun DisplayCombinationCards(
    modifier: Modifier,
    requestDataUi: RequestDataUi.Multiple,
    claimsAreSelectable: Boolean,
    onEventSend: (Event) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
    ) {
        requestDataUi.combinations.forEachIndexed { index, combination ->
            WrapSelectableCard(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(
                    R.string.request_combination_option_title,
                    index + 1,
                    requestDataUi.combinations.size,
                ),
                isSelected = index == requestDataUi.selectedIndex,
                onSelected = { onEventSend(Event.CombinationSelected(index = index)) },
            ) {
                DisplayRequestItems(
                    modifier = Modifier.fillMaxWidth(),
                    requestDocuments = combination.documents,
                    claimsAreSelectable = claimsAreSelectable,
                    onEventSend = onEventSend,
                )
            }
        }
    }
}

@Composable
private fun DisplayRequestItems(
    modifier: Modifier,
    requestDocuments: List<RequestDocumentItemUi>,
    claimsAreSelectable: Boolean,
    onEventSend: (Event) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        requestDocuments.forEachIndexed { index, requestDocument ->
            WrapExpandableListItem(
                modifier = Modifier
                    .applyTestTag(TestTag.RequestScreen.requestedDocument(index = index))
                    .fillMaxWidth(),
                header = requestDocument.headerUi.header,
                data = requestDocument.headerUi.nestedItems,
                onItemClick = if (claimsAreSelectable) {
                    { item -> onEventSend(Event.UserIdentificationClicked(itemId = item.itemId)) }
                } else {
                    null
                },
                onExpandedChange = { expandedItem ->
                    onEventSend(Event.ExpandOrCollapseRequestDocumentItem(itemId = expandedItem.itemId))
                },
                isExpanded = requestDocument.headerUi.isExpanded,
                throttleClicks = false,
                hideSensitiveContent = false,
                collapsedMainContentVerticalPadding = SPACING_MEDIUM.dp,
                expandedMainContentVerticalPadding = SPACING_MEDIUM.dp,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceDim,
                ),
            )
        }
    }
}

@Composable
private fun SheetContent(
    sheetContent: RequestBottomSheetContent,
    onEventSent: (Event) -> Unit,
) {
    when (sheetContent) {
        is RequestBottomSheetContent.Warning -> {
            SimpleBottomSheet(
                textData = BottomSheetTextDataUi(
                    title = stringResource(id = R.string.request_bottom_sheet_warning_title),
                    message = stringResource(id = R.string.request_bottom_sheet_warning_subtitle),
                ),
                leadingIcon = AppIcons.Warning,
                leadingIconTint = MaterialTheme.colorScheme.warning
            )
        }

        is RequestBottomSheetContent.VerifierNotTrusted -> {
            DialogBottomSheet(
                textData = BottomSheetTextDataUi(
                    title = stringResource(id = R.string.request_blocked_bottom_sheet_title),
                    message = stringResource(id = R.string.request_blocked_bottom_sheet_message),
                    positiveButtonText = stringResource(id = R.string.request_blocked_bottom_sheet_primary_button_text),
                ),
                leadingIcon = AppIcons.Warning,
                leadingIconTint = MaterialTheme.colorScheme.warning,
                onPositiveClick = { onEventSent(Event.BottomSheet.VerifierNotTrusted.Close) },
            )
        }
    }
}

@Composable
private fun previewRelyingPartyHeader(): RelyingPartyHeaderUi {
    return RelyingPartyHeaderUi(
        relyingParty = RelyingPartyDataUi(
            logo = null,
            isVerified = true,
            name = "NordicBank A/S",
            uniqueId = "rp:nordicbank:prod",
            description = null,
        ),
        intendedUse = "We will use your identity and age to verify you for a new current " +
                "account. Your data will be used once to complete onboarding and to meet " +
                "anti-money laundering requirements.",
        privacyPolicyUrl = "https://nordicbank.example/privacy",
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@ThemeModePreviews
@Composable
private fun ContentPreview() {
    PreviewTheme {
        Content(
            state = State(
                relyingPartyHeader = previewRelyingPartyHeader(),
                requestDataUi = RequestDataUi.Single(
                    combination = RequestCombinationUi(
                        documents = listOf(previewRequestDocumentItem()),
                        matches = emptyList(),
                    ),
                ),
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

@OptIn(ExperimentalMaterial3Api::class)
@ThemeModePreviews
@Composable
private fun ContentNoDataPreview() {
    PreviewTheme {
        Content(
            state = State(
                relyingPartyHeader = previewRelyingPartyHeader(),
                requestDataUi = RequestDataUi.NoData,
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

@OptIn(ExperimentalMaterial3Api::class)
@ThemeModePreviews
@Composable
private fun ContentMultipleCombinationsPreview() {
    PreviewTheme {
        val previewItem = previewRequestDocumentItem()
        Content(
            state = State(
                relyingPartyHeader = previewRelyingPartyHeader(),
                requestDataUi = RequestDataUi.Multiple(
                    combinations = listOf(
                        RequestCombinationUi(
                            documents = listOf(previewItem),
                            matches = emptyList()
                        ),
                        RequestCombinationUi(
                            documents = listOf(previewItem),
                            matches = emptyList()
                        ),
                    ),
                    selectedIndex = 0,
                ),
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

@Composable
private fun previewRequestDocumentItem(): RequestDocumentItemUi {
    return RequestDocumentItemUi(
        domainPayload = DocumentPayloadDomain(
            docName = "docName",
            docId = "docId",
            docFormatDomain = DocumentFormatDomain.MsoMdoc,
            docClaimsDomain = listOf(
                ClaimDomain.Primitive(
                    key = "key",
                    displayTitle = "title",
                    value = "value",
                    isRequired = false,
                    path = ClaimPathDomain.ofPlainKeys(
                        names = listOf(),
                        type = ClaimType.MsoMdoc(namespace = "namespace")
                    )
                ),
            )
        ),
        headerUi = ExpandableListItemUi.NestedListItem(
            header = ListItemDataUi(
                itemId = "000",
                mainContentData = ListItemMainContentDataUi.Text(text = "Digital ID"),
                supportingContentData = ListItemSupportingContentDataUi.Text(
                    text = stringResource(R.string.request_collapsed_supporting_text),
                ),
                trailingContentData = ListItemTrailingContentDataUi.Icon(
                    iconData = AppIcons.KeyboardArrowDown
                ),
            ),
            nestedItems = listOf(
                ExpandableListItemUi.SingleListItem(
                    ListItemDataUi(
                        itemId = "00",
                        overlineText = "Family name",
                        mainContentData = ListItemMainContentDataUi.Text(text = "Doe"),
                        trailingContentData = ListItemTrailingContentDataUi.Checkbox(
                            checkboxData = CheckboxDataUi(
                                isChecked = true
                            )
                        )
                    )
                ),
                ExpandableListItemUi.SingleListItem(
                    ListItemDataUi(
                        itemId = "01",
                        overlineText = "Given name",
                        mainContentData = ListItemMainContentDataUi.Text(text = "John"),
                        trailingContentData = ListItemTrailingContentDataUi.Checkbox(
                            checkboxData = CheckboxDataUi(
                                isChecked = true
                            )
                        )
                    ),
                )

            ),
            isExpanded = true
        )
    )
}

@ThemeModePreviews
@Composable
private fun SheetContentWarningPreview() {
    PreviewTheme {
        SheetContent(
            sheetContent = RequestBottomSheetContent.Warning,
            onEventSent = {},
        )
    }
}

@ThemeModePreviews
@Composable
private fun SheetContentVerifierNotTrustedPreview() {
    PreviewTheme {
        SheetContent(
            sheetContent = RequestBottomSheetContent.VerifierNotTrusted,
            onEventSent = {},
        )
    }
}