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
import androidx.compose.material3.Text
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
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.RelyingPartyDataUi
import eu.europa.ec.uilogic.component.SectionTitle
import eu.europa.ec.uilogic.component.content.ContentHeader
import eu.europa.ec.uilogic.component.content.ContentHeaderConfig
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.BottomSheetTextDataUi
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.CheckboxDataUi
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import eu.europa.ec.uilogic.component.wrap.SimpleBottomSheet
import eu.europa.ec.uilogic.component.wrap.StickyBottomConfig
import eu.europa.ec.uilogic.component.wrap.StickyBottomType
import eu.europa.ec.uilogic.component.wrap.TextConfig
import eu.europa.ec.uilogic.component.wrap.TextStyleKey
import eu.europa.ec.uilogic.component.wrap.WrapExpandableListItem
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapSelectableCard
import eu.europa.ec.uilogic.component.wrap.WrapStickyBottomContent
import eu.europa.ec.uilogic.extension.applyTestTag
import eu.europa.ec.uilogic.extension.finish
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
            WrapStickyBottomContent(
                modifier = Modifier
                    .applyTestTag(TestTag.RequestScreen.BUTTON)
                    .fillMaxWidth()
                    .padding(paddingValues),
                stickyBottomConfig = StickyBottomConfig(
                    type = StickyBottomType.OneButton(
                        config = ButtonConfig(
                            type = ButtonType.PRIMARY,
                            enabled = !state.isLoading && state.allowShare,
                            onClick = { viewModel.setEvent(Event.StickyButtonPressed) }
                        )
                    )
                )
            ) {
                Text(text = stringResource(R.string.request_sticky_button_text))
            }
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
                    sheetContent = state.sheetContent,
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
        ContentHeader(
            modifier = Modifier.fillMaxWidth(),
            config = state.headerConfig,
            descriptionTestTag = TestTag.RequestScreen.CONTENT_HEADER_DESCRIPTION,
        )

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

        is RequestDataUi.Single -> DisplayRequestItems(
            modifier = modifier,
            requestDocuments = requestDataUi.combination.documents,
            claimsAreSelectable = claimsAreSelectable,
            onEventSend = onEventSend,
            showWarning = true,
        )

        is RequestDataUi.Multiple -> DisplayCombinationCards(
            modifier = modifier,
            requestDataUi = requestDataUi,
            claimsAreSelectable = claimsAreSelectable,
            onEventSend = onEventSend,
        )
    }
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
                    showWarning = false,
                )
            }
        }

        // the 'review-carefully' note renders once under the whole list here; the
        // single-combination branch renders it inside DisplayRequestItems instead
        RequestWarningNote(
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DisplayRequestItems(
    modifier: Modifier,
    requestDocuments: List<RequestDocumentItemUi>,
    claimsAreSelectable: Boolean,
    onEventSend: (Event) -> Unit,
    showWarning: Boolean,
) {
    Column(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
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

        if (showWarning && requestDocuments.isNotEmpty()) {
            RequestWarningNote(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SPACING_SMALL.dp),
            )
        }
    }
}

@Composable
private fun RequestWarningNote(
    modifier: Modifier,
) {
    SectionTitle(
        modifier = modifier,
        text = stringResource(R.string.request_warning_text),
        textConfig = TextConfig(
            styleKey = TextStyleKey.BodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    )
}

@Composable
private fun SheetContent(
    sheetContent: RequestBottomSheetContent,
) {
    when (sheetContent) {
        RequestBottomSheetContent.WARNING -> {
            SimpleBottomSheet(
                textData = BottomSheetTextDataUi(
                    title = stringResource(id = R.string.request_bottom_sheet_warning_title),
                    message = stringResource(id = R.string.request_bottom_sheet_warning_subtitle),
                ),
                leadingIcon = AppIcons.Warning,
                leadingIconTint = MaterialTheme.colorScheme.warning
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ThemeModePreviews
@Composable
private fun ContentPreview() {
    PreviewTheme {
        Content(
            state = State(
                headerConfig = ContentHeaderConfig(
                    description = stringResource(R.string.request_header_description),
                    mainText = stringResource(R.string.request_header_main_text),
                    relyingPartyData = RelyingPartyDataUi(
                        isVerified = true,
                        name = stringResource(R.string.request_relying_party_default_name),
                        description = stringResource(R.string.request_relying_party_description)
                    )
                ),
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
                headerConfig = ContentHeaderConfig(
                    description = stringResource(R.string.request_header_description),
                    mainText = stringResource(R.string.request_header_main_text),
                    relyingPartyData = RelyingPartyDataUi(
                        isVerified = true,
                        name = stringResource(R.string.request_relying_party_default_name),
                        description = stringResource(R.string.request_relying_party_description)
                    )
                ),
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
                headerConfig = ContentHeaderConfig(
                    description = stringResource(R.string.request_header_description),
                    mainText = stringResource(R.string.request_header_main_text),
                    relyingPartyData = RelyingPartyDataUi(
                        isVerified = true,
                        name = stringResource(R.string.request_relying_party_default_name),
                        description = stringResource(R.string.request_relying_party_description)
                    )
                ),
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
                supportingText = stringResource(R.string.request_collapsed_supporting_text),
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
            sheetContent = RequestBottomSheetContent.WARNING,
        )
    }
}