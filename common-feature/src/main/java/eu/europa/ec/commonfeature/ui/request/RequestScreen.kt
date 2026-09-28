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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.ui.request.model.DocumentFormatDomain
import eu.europa.ec.commonfeature.ui.request.model.DocumentPayloadDomain
import eu.europa.ec.commonfeature.ui.request.model.RelyingPartyHeaderUi
import eu.europa.ec.commonfeature.ui.request.model.RequestCombinationUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDataUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi
import eu.europa.ec.commonfeature.ui.request.model.RequestTransactionDataUi
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
import eu.europa.ec.uilogic.component.preview.LargeTextPreviews
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.OncePerViewModelEffect
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
            onEventSend = viewModel::setEvent,
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
                    onEventSent = viewModel::setEvent,
                )
            }
        }
    }

    OncePerViewModelEffect(viewModel) {
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
                onPrivacyPolicyClick = { onEventSend(Event.PrivacyPolicyLinkClicked) },
            )
        }

        // Screen Main Content.
        DisplayRequestContent(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SPACING_SMALL.dp),
            requestDataUi = state.requestDataUi,
            claimsAreSelectable = state.claimsAreSelectable,
            onCombinationSelected = { index ->
                onEventSend(Event.CombinationSelected(index = index))
            },
            onClaimClick = { itemId ->
                onEventSend(Event.UserIdentificationClicked(itemId = itemId))
            },
            onCredentialExpansionChange = { itemId ->
                onEventSend(Event.ExpandOrCollapseRequestDocumentItem(itemId = itemId))
            },
            onTransactionExpansionChange = { sectionId, itemId ->
                onEventSend(
                    Event.TransactionDataExpansionToggled(
                        sectionId = sectionId,
                        itemId = itemId
                    )
                )
            },
            onTransactionDocumentClick = { sectionId, itemId ->
                onEventSend(
                    Event.TransactionDocumentClicked(sectionId = sectionId, itemId = itemId),
                )
            },
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
    onPrivacyPolicyClick: () -> Unit,
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
                onLinkClick = onPrivacyPolicyClick,
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
    onCombinationSelected: (Int) -> Unit,
    onClaimClick: (String) -> Unit,
    onCredentialExpansionChange: (String) -> Unit,
    onTransactionExpansionChange: (String, String) -> Unit,
    onTransactionDocumentClick: (String, String) -> Unit,
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
            CombinationContent(
                modifier = Modifier.fillMaxWidth(),
                combination = requestDataUi.combination,
                claimsAreSelectable = claimsAreSelectable,
                transactionTitleStartPadding = 0.dp,
                onClaimClick = onClaimClick,
                onCredentialExpansionChange = onCredentialExpansionChange,
                onTransactionExpansionChange = onTransactionExpansionChange,
                onTransactionDocumentClick = onTransactionDocumentClick,
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
                onCombinationSelected = onCombinationSelected,
                onClaimClick = onClaimClick,
                onCredentialExpansionChange = onCredentialExpansionChange,
                onTransactionExpansionChange = onTransactionExpansionChange,
                onTransactionDocumentClick = onTransactionDocumentClick,
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
    onCombinationSelected: (Int) -> Unit,
    onClaimClick: (String) -> Unit,
    onCredentialExpansionChange: (String) -> Unit,
    onTransactionExpansionChange: (String, String) -> Unit,
    onTransactionDocumentClick: (String, String) -> Unit,
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
                onSelected = { onCombinationSelected(index) },
            ) {
                CombinationContent(
                    modifier = Modifier.fillMaxWidth(),
                    combination = combination,
                    claimsAreSelectable = claimsAreSelectable,
                    transactionTitleStartPadding = SPACING_SMALL.dp,
                    onClaimClick = onClaimClick,
                    onCredentialExpansionChange = onCredentialExpansionChange,
                    onTransactionExpansionChange = onTransactionExpansionChange,
                    onTransactionDocumentClick = onTransactionDocumentClick,
                )
            }
        }
    }
}

@Composable
private fun CombinationContent(
    modifier: Modifier,
    combination: RequestCombinationUi,
    claimsAreSelectable: Boolean,
    transactionTitleStartPadding: Dp,
    onClaimClick: (String) -> Unit,
    onCredentialExpansionChange: (String) -> Unit,
    onTransactionExpansionChange: (String, String) -> Unit,
    onTransactionDocumentClick: (String, String) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
    ) {
        DisplayRequestItems(
            modifier = Modifier.fillMaxWidth(),
            requestDocuments = combination.documents,
            claimsAreSelectable = claimsAreSelectable,
            onClaimClick = onClaimClick,
            onExpansionChange = onCredentialExpansionChange,
        )
        combination.transactionData?.let { safeTransactionData ->
            TransactionDataSection(
                modifier = Modifier.fillMaxWidth(),
                transactionData = safeTransactionData,
                titleStartPadding = transactionTitleStartPadding,
                onExpansionChange = { itemId ->
                    onTransactionExpansionChange(safeTransactionData.details.header.itemId, itemId)
                },
                onDocumentClick = { itemId ->
                    onTransactionDocumentClick(safeTransactionData.details.header.itemId, itemId)
                },
            )
        }
    }
}

@Composable
private fun DisplayRequestItems(
    modifier: Modifier,
    requestDocuments: List<RequestDocumentItemUi>,
    claimsAreSelectable: Boolean,
    onClaimClick: (String) -> Unit,
    onExpansionChange: (String) -> Unit,
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
                isItemClickable = { true },
                onItemClick = if (claimsAreSelectable) {
                    { item -> onClaimClick(item.itemId) }
                } else {
                    null
                },
                onExpandedChange = { expandedItem ->
                    onExpansionChange(expandedItem.itemId)
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
private fun TransactionDataSection(
    modifier: Modifier,
    transactionData: RequestTransactionDataUi,
    titleStartPadding: Dp,
    onExpansionChange: (String) -> Unit,
    onDocumentClick: (String) -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceDim)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp),
    ) {
        SectionTitle(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = titleStartPadding),
            text = transactionData.title,
            textConfig = TextConfig(
                styleKey = TextStyleKey.LabelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = Int.MAX_VALUE,
            ),
        )
        WrapExpandableListItem(
            modifier = Modifier.fillMaxWidth(),
            header = transactionData.details.header,
            data = transactionData.details.nestedItems,
            isExpanded = transactionData.details.isExpanded,
            onExpandedChange = { item -> onExpansionChange(item.itemId) },
            onItemClick = { item -> onDocumentClick(item.itemId) },
            isItemClickable = { item -> item.itemId in transactionData.documentUrlsByItemId },
            collapsedMainContentVerticalPadding = SPACING_MEDIUM.dp,
            expandedMainContentVerticalPadding = SPACING_MEDIUM.dp,
            colors = colors,
            throttleClicks = false,
        )
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
                        transactionData = null,
                        documents = listOf(previewRequestDocumentItem(isExpanded = true)),
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
        val previewItem = previewRequestDocumentItem(isExpanded = true)
        Content(
            state = State(
                relyingPartyHeader = previewRelyingPartyHeader(),
                requestDataUi = RequestDataUi.Multiple(
                    combinations = listOf(
                        RequestCombinationUi(
                            transactionData = null,
                            documents = listOf(previewItem),
                            matches = emptyList()
                        ),
                        RequestCombinationUi(
                            transactionData = null,
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
private fun previewRequestDocumentItem(isExpanded: Boolean): RequestDocumentItemUi {
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
                    iconData = if (isExpanded) AppIcons.KeyboardArrowUp else AppIcons.KeyboardArrowDown,
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
            isExpanded = isExpanded,
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

@ThemeModePreviews
@Composable
private fun TransactionDataCollapsedRequestPreview() {
    PreviewTheme {
        TransactionRequestContentPreview(isExpanded = false)
    }
}

@ThemeModePreviews
@Composable
private fun TransactionDataExpandedRequestPreview() {
    PreviewTheme {
        TransactionRequestContentPreview(isExpanded = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionRequestContentPreview(isExpanded: Boolean) {
    Content(
        state = State(
            isLoading = false,
            claimsAreSelectable = false,
            relyingPartyHeader = previewRelyingPartyHeader(),
            requestDataUi = RequestDataUi.Single(
                combination = RequestCombinationUi(
                    documents = listOf(previewRequestDocumentItem(isExpanded = false)),
                    matches = emptyList(),
                    transactionData = previewTransactionData(
                        sectionId = "preview-single",
                        scenario = TransactionDataPreviewScenario.Approval,
                        isExpanded = isExpanded,
                    ),
                ),
            ),
        ),
        effectFlow = Channel<Effect>().receiveAsFlow(),
        onEventSend = {},
        onNavigationRequested = {},
        paddingValues = PaddingValues(SPACING_MEDIUM.dp),
        coroutineScope = rememberCoroutineScope(),
        modalBottomSheetState = rememberModalBottomSheetState(),
    )
}

@ThemeModePreviews
@Composable
private fun TransactionDataMixedCombinationsPreview() {
    PreviewTheme {
        val documents = listOf(previewRequestDocumentItem(isExpanded = false))
        DisplayRequestContent(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(SPACING_MEDIUM.dp),
            requestDataUi = RequestDataUi.Multiple(
                combinations = listOf(
                    RequestCombinationUi(
                        documents = documents,
                        matches = emptyList(),
                        transactionData = previewTransactionData(
                            sectionId = "preview-option-0",
                            scenario = TransactionDataPreviewScenario.Approval,
                            isExpanded = false,
                        ),
                    ),
                    RequestCombinationUi(
                        documents = documents,
                        matches = emptyList(),
                        transactionData = null,
                    ),
                    RequestCombinationUi(
                        documents = documents,
                        matches = emptyList(),
                        transactionData = previewTransactionData(
                            sectionId = "preview-option-2",
                            scenario = TransactionDataPreviewScenario.ReferencedQes,
                            isExpanded = false,
                        ),
                    ),
                ),
                selectedIndex = 0,
            ),
            claimsAreSelectable = false,
            onCombinationSelected = {},
            onClaimClick = {},
            onCredentialExpansionChange = {},
            onTransactionExpansionChange = { _, _ -> },
            onTransactionDocumentClick = { _, _ -> },
        )
    }
}

@ThemeModePreviews
@Composable
private fun TransactionDataExpandedCombinationPreview() {
    PreviewTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(SPACING_MEDIUM.dp),
        ) {
            WrapSelectableCard(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.request_combination_option_title, 1, 2),
                isSelected = true,
                onSelected = {},
            ) {
                CombinationContent(
                    modifier = Modifier.fillMaxWidth(),
                    combination = RequestCombinationUi(
                        documents = listOf(previewRequestDocumentItem(isExpanded = false)),
                        matches = emptyList(),
                        transactionData = previewTransactionData(
                            sectionId = "preview-expanded-option",
                            scenario = TransactionDataPreviewScenario.ReferencedQes,
                            isExpanded = true,
                        ),
                    ),
                    claimsAreSelectable = false,
                    transactionTitleStartPadding = SPACING_SMALL.dp,
                    onClaimClick = {},
                    onCredentialExpansionChange = {},
                    onTransactionExpansionChange = { _, _ -> },
                    onTransactionDocumentClick = { _, _ -> },
                )
            }
        }
    }
}

@ThemeModePreviews
@Composable
private fun TransactionDataSectionPreview(
    @PreviewParameter(TransactionDataPreviewProvider::class) scenario: TransactionDataPreviewScenario,
) {
    PreviewTheme {
        TransactionDataSection(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(SPACING_MEDIUM.dp),
            transactionData = previewTransactionData(
                sectionId = "preview-section",
                scenario = scenario,
                isExpanded = true,
            ),
            titleStartPadding = 0.dp,
            onExpansionChange = {},
            onDocumentClick = {},
        )
    }
}

@LargeTextPreviews
@Composable
private fun TransactionDataLongDocumentActionPreview() {
    PreviewTheme {
        val actionId = "preview-long-document/open"
        val section = previewTransactionSection(
            sectionId = "preview-long-document",
            isExpanded = true,
            rows = listOf(
                previewTransactionField(
                    itemId = "preview-long-document/name",
                    label = stringResource(R.string.request_transaction_document),
                    value = PREVIEW_LONG_DOCUMENT_NAME,
                ),
                ExpandableListItemUi.SingleListItem(
                    header = ListItemDataUi(
                        itemId = actionId,
                        mainContentData = ListItemMainContentDataUi.Text(
                            text = "Open the document supplied for this signature",
                        ),
                        trailingContentData = ListItemTrailingContentDataUi.Icon(AppIcons.OpenNew),
                    ),
                ),
            ),
            documentUrlsByItemId = mapOf(actionId to PREVIEW_DOCUMENT_URL),
        )
        TransactionDataSection(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(SPACING_MEDIUM.dp),
            transactionData = section,
            titleStartPadding = 0.dp,
            onExpansionChange = {},
            onDocumentClick = {},
        )
    }
}

private enum class TransactionDataPreviewScenario {
    Approval,
    ReferencedQes,
    InlineQes,
    MultipleTransactions,
    NestedItems,
}

private class TransactionDataPreviewProvider :
    PreviewParameterProvider<TransactionDataPreviewScenario> {
    override val values: Sequence<TransactionDataPreviewScenario> =
        TransactionDataPreviewScenario.entries.asSequence()
}

@Composable
private fun previewTransactionData(
    sectionId: String,
    scenario: TransactionDataPreviewScenario,
    isExpanded: Boolean,
): RequestTransactionDataUi {
    val rows = listOf(
        previewTransactionField(
            itemId = "$sectionId/framework",
            label = stringResource(R.string.request_transaction_trust_framework),
            value = stringResource(R.string.request_transaction_trust_framework_value),
        ),
    ) + when (scenario) {
        TransactionDataPreviewScenario.Approval -> previewApprovalRows(sectionId = sectionId)
        TransactionDataPreviewScenario.ReferencedQes -> previewReferencedQesRows(sectionId = sectionId)
        TransactionDataPreviewScenario.InlineQes -> previewInlineQesRows(sectionId = sectionId)
        TransactionDataPreviewScenario.NestedItems -> listOf(
            previewTransactionGroup(
                itemId = "$sectionId/document",
                title = "Document 1",
                isExpanded = true,
                rows = previewReferencedQesRows(sectionId = "$sectionId/document") +
                        previewTransactionGroup(
                            itemId = "$sectionId/document/attributes",
                            title = stringResource(R.string.request_transaction_signed_attributes),
                            isExpanded = true,
                            rows = listOf(
                                previewTransactionField(
                                    itemId = "$sectionId/document/attributes/reason",
                                    label = "Reason",
                                    value = "Agreement",
                                ),
                            ),
                        ),
            ),
            previewTransactionGroup(
                itemId = "$sectionId/other-document",
                title = "Document 2",
                isExpanded = false,
                rows = previewApprovalRows(sectionId = "$sectionId/other-document"),
            ),
        )

        TransactionDataPreviewScenario.MultipleTransactions -> {
            listOf(
                previewTransactionField(
                    itemId = "$sectionId/transaction-0/title",
                    label = null,
                    value = stringResource(R.string.request_transaction_numbered, 1),
                ),
            ) + previewApprovalRows(sectionId = "$sectionId/transaction-0") + listOf(
                previewTransactionField(
                    itemId = "$sectionId/transaction-1/title",
                    label = null,
                    value = stringResource(R.string.request_transaction_numbered, 2),
                ),
            ) + previewInlineQesRows(sectionId = "$sectionId/transaction-1") + listOf(
                previewTransactionField(
                    itemId = "$sectionId/transaction-2/title",
                    label = null,
                    value = stringResource(R.string.request_transaction_numbered, 3),
                ),
                previewTransactionField(
                    itemId = "$sectionId/transaction-2/unavailable",
                    label = null,
                    value = stringResource(R.string.request_transaction_unavailable),
                ),
            )
        }
    }
    return previewTransactionSection(
        sectionId = sectionId,
        isExpanded = isExpanded,
        rows = rows,
        documentUrlsByItemId = when (scenario) {
            TransactionDataPreviewScenario.ReferencedQes -> mapOf("$sectionId/open" to PREVIEW_DOCUMENT_URL)
            TransactionDataPreviewScenario.NestedItems -> mapOf("$sectionId/document/open" to PREVIEW_DOCUMENT_URL)
            else -> emptyMap()
        },
    )
}

private fun previewTransactionGroup(
    itemId: String,
    title: String,
    isExpanded: Boolean,
    rows: List<ExpandableListItemUi>,
): ExpandableListItemUi.NestedListItem = ExpandableListItemUi.NestedListItem(
    header = ListItemDataUi(
        itemId = itemId,
        mainContentData = ListItemMainContentDataUi.Text(title),
        trailingContentData = ListItemTrailingContentDataUi.Icon(
            if (isExpanded) AppIcons.KeyboardArrowUp else AppIcons.KeyboardArrowDown,
        ),
    ),
    nestedItems = rows,
    isExpanded = isExpanded,
)

@Composable
private fun previewTransactionSection(
    sectionId: String,
    isExpanded: Boolean,
    rows: List<ExpandableListItemUi>,
    documentUrlsByItemId: Map<String, String>,
): RequestTransactionDataUi = RequestTransactionDataUi(
    title = stringResource(R.string.request_transaction_section_title),
    details = ExpandableListItemUi.NestedListItem(
        header = ListItemDataUi(
            itemId = sectionId,
            mainContentData = ListItemMainContentDataUi.Text(
                text = stringResource(R.string.request_transaction_details_title),
            ),
            supportingContentData = ListItemSupportingContentDataUi.Text(
                text = stringResource(R.string.request_collapsed_supporting_text),
            ),
            trailingContentData = ListItemTrailingContentDataUi.Icon(
                iconData = if (isExpanded) AppIcons.KeyboardArrowUp else AppIcons.KeyboardArrowDown,
            ),
        ),
        nestedItems = rows,
        isExpanded = isExpanded,
    ),
    documentUrlsByItemId = documentUrlsByItemId,
)

@Composable
private fun previewApprovalRows(sectionId: String): List<ExpandableListItemUi> = listOf(
    previewTransactionField(
        itemId = "$sectionId/type",
        label = stringResource(R.string.request_transaction_type),
        value = "QES approval",
    ),
    previewTransactionField(
        itemId = "$sectionId/references",
        label = stringResource(R.string.request_transaction_requested_credentials),
        value = "query_0",
    ),
    previewTransactionField(
        itemId = "$sectionId/credential",
        label = stringResource(R.string.request_transaction_signing_credential_id),
        value = "a5900d2e-6862-4272-9f36-95ed540f6efa",
    ),
    previewTransactionField(
        itemId = "$sectionId/count",
        label = stringResource(R.string.request_transaction_signature_count),
        value = "1",
    ),
    previewTransactionField(
        itemId = "$sectionId/document",
        label = stringResource(R.string.request_transaction_document),
        value = "file-sample_150kB.pdf",
    ),
    previewTransactionField(
        itemId = "$sectionId/representation",
        label = stringResource(R.string.request_transaction_hash_representation),
        value = "DTBSR",
    ),
    previewTransactionField(
        itemId = "$sectionId/hash",
        label = stringResource(R.string.request_transaction_dtbsr_hash),
        value = PREVIEW_DOCUMENT_HASH,
    ),
    previewTransactionField(
        itemId = "$sectionId/algorithm",
        label = stringResource(R.string.request_transaction_dtbsr_algorithm),
        value = "SHA-256",
    ),
)

@Composable
private fun previewReferencedQesRows(sectionId: String): List<ExpandableListItemUi> = listOf(
    previewTransactionField(
        itemId = "$sectionId/type",
        label = stringResource(R.string.request_transaction_type),
        value = "QES request",
    ),
    previewTransactionField(
        itemId = "$sectionId/references",
        label = stringResource(R.string.request_transaction_requested_credentials),
        value = "query_0\nquery_1",
    ),
    previewTransactionField(
        itemId = "$sectionId/document",
        label = stringResource(R.string.request_transaction_document),
        value = PREVIEW_LONG_DOCUMENT_NAME,
    ),
    previewTransactionField(
        itemId = "$sectionId/location",
        label = stringResource(R.string.request_transaction_document_location),
        value = PREVIEW_DOCUMENT_URL,
    ),
    ExpandableListItemUi.SingleListItem(
        header = ListItemDataUi(
            itemId = "$sectionId/open",
            mainContentData = ListItemMainContentDataUi.Text(
                text = stringResource(R.string.request_transaction_open_document),
            ),
            trailingContentData = ListItemTrailingContentDataUi.Icon(AppIcons.OpenNew),
        ),
    ),
    previewTransactionField(
        itemId = "$sectionId/checksum",
        label = stringResource(R.string.request_transaction_expected_checksum),
        value = PREVIEW_DOCUMENT_HASH,
    ),
    previewTransactionField(
        itemId = "$sectionId/checksum-algorithm",
        label = stringResource(R.string.request_transaction_checksum_algorithm),
        value = "SHA-256",
    ),
    previewTransactionField(
        itemId = "$sectionId/signature-type",
        label = stringResource(R.string.request_transaction_signature_type),
        value = stringResource(R.string.request_transaction_qeseal),
    ),
    previewTransactionField(
        itemId = "$sectionId/format",
        label = stringResource(R.string.request_transaction_signature_format),
        value = "PAdES",
    ),
    previewTransactionField(
        itemId = "$sectionId/conformance",
        label = stringResource(R.string.request_transaction_conformance_level),
        value = "B-LT",
    ),
    previewTransactionField(
        itemId = "$sectionId/attributes",
        label = null,
        value = stringResource(R.string.request_transaction_signed_attributes),
    ),
    previewTransactionField(
        itemId = "$sectionId/reason",
        label = "Reason",
        value = "Agreement covering the complete annual financial statement and its supporting " +
                "documents, including the appended declaration of representation.",
    ),
    previewTransactionField(
        itemId = "$sectionId/name-only-attribute",
        label = null,
        value = "Signer role supplied without a value",
    ),
    previewTransactionField(
        itemId = "$sectionId/otp",
        label = stringResource(R.string.request_transaction_otp),
        value = "000123",
    ),
    previewTransactionField(
        itemId = "$sectionId/response",
        label = stringResource(R.string.request_transaction_response_uri),
        value = "https://signer.example.org/signatures/response?request_id=" +
                "726fe88d-277d-47e5-85fb-124e63dc9b15&redirect_uri=https%3A%2F%2Fwallet.example.org%2Fdone",
    ),
)

@Composable
private fun previewInlineQesRows(sectionId: String): List<ExpandableListItemUi> = listOf(
    previewTransactionField(
        itemId = "$sectionId/type",
        label = stringResource(R.string.request_transaction_type),
        value = "QES request",
    ),
    previewTransactionField(
        itemId = "$sectionId/document",
        label = stringResource(R.string.request_transaction_document),
        value = stringResource(R.string.request_transaction_document_numbered, 1),
    ),
    previewTransactionField(
        itemId = "$sectionId/signature-type",
        label = stringResource(R.string.request_transaction_signature_type),
        value = "vendor_signature_profile_v2",
    ),
    previewTransactionField(
        itemId = "$sectionId/format",
        label = stringResource(R.string.request_transaction_signature_format),
        value = "JAdES",
    ),
)

private fun previewTransactionField(
    itemId: String,
    label: String?,
    value: String,
): ExpandableListItemUi.SingleListItem = ExpandableListItemUi.SingleListItem(
    header = ListItemDataUi(
        itemId = itemId,
        overlineText = label,
        mainContentData = ListItemMainContentDataUi.Text(text = value),
    ),
)

private const val PREVIEW_LONG_DOCUMENT_NAME =
    "Annual_financial_statement_and_declaration_of_representation_2026_" +
            "726fe88d-277d-47e5-85fb-124e63dc9b15_final_signed_copy.pdf"
private const val PREVIEW_DOCUMENT_HASH = "jDudt7/CCqNAacyhuKo4A6aiDzUYUG5MWQibmUGMPXg="
private const val PREVIEW_DOCUMENT_URL =
    "https://documents.example.org/signing/2026/annual-statements/" +
            "726fe88d-277d-47e5-85fb-124e63dc9b15/document.pdf?access_token=abC%2BDeF%2F123%3D#page=1"