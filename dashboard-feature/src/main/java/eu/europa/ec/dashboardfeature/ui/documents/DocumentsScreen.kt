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

package eu.europa.ec.dashboardfeature.ui.documents

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.model.DocumentUiIssuanceState
import eu.europa.ec.corelogic.model.DocumentCategory
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.util.CoreActions
import eu.europa.ec.dashboardfeature.model.DocumentUi
import eu.europa.ec.dashboardfeature.model.SearchItem
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.warning
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.DualSelectorButton
import eu.europa.ec.uilogic.component.DualSelectorButtonData
import eu.europa.ec.uilogic.component.DualSelectorButtons
import eu.europa.ec.uilogic.component.FiltersSearchBar
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ModalOptionUi
import eu.europa.ec.uilogic.component.SectionTitle
import eu.europa.ec.uilogic.component.SystemBroadcastReceiver
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.HSpacer
import eu.europa.ec.uilogic.component.utils.LifecycleEffect
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.BottomSheetTextData
import eu.europa.ec.uilogic.component.wrap.BottomSheetWithOptionsList
import eu.europa.ec.uilogic.component.wrap.BottomSheetWithTwoBigIcons
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.DialogBottomSheet
import eu.europa.ec.uilogic.component.wrap.GenericBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapButton
import eu.europa.ec.uilogic.component.wrap.WrapExpandableListItem
import eu.europa.ec.uilogic.component.wrap.WrapIconButton
import eu.europa.ec.uilogic.component.wrap.WrapListItem
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import eu.europa.ec.uilogic.extension.finish
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

typealias DashboardEvent = eu.europa.ec.dashboardfeature.ui.dashboard.Event
typealias ShowSideMenuEvent = eu.europa.ec.dashboardfeature.ui.dashboard.Event.SideMenu.Show

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    navHostController: NavController,
    viewModel: DocumentsViewModel,
    onDashboardEventSent: (DashboardEvent) -> Unit,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val isBottomSheetOpen = state.isBottomSheetOpen
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = ScreenNavigateAction.NONE,
        onBack = { context.finish() },
        topBar = {
            TopBar(
                onEventSend = { viewModel.setEvent(it) },
                onDashboardEventSent = onDashboardEventSent
            )
        },
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(navigationEffect, navHostController, context)
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
                DocumentsSheetContent(
                    sheetContent = state.sheetContent,
                    state = state,
                    onEventSent = {
                        viewModel.setEvent(it)
                    }
                )
            }
        }
    }

    SystemBroadcastReceiver(
        actions = listOf(
            CoreActions.REVOCATION_WORK_REFRESH_ACTION
        )
    ) {
        viewModel.setEvent(Event.GetDocuments)
    }
}

private fun handleNavigationEffect(
    navigationEffect: Effect.Navigation,
    navController: NavController,
    context: Context,
) {
    when (navigationEffect) {
        is Effect.Navigation.Pop -> context.finish()
        is Effect.Navigation.SwitchScreen -> {
            navController.navigate(navigationEffect.screenRoute) {
                popUpTo(navigationEffect.popUpToScreenRoute) {
                    inclusive = navigationEffect.inclusive
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    onEventSend: (Event) -> Unit,
    onDashboardEventSent: (DashboardEvent) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SPACING_SMALL.dp,
                vertical = SPACING_MEDIUM.dp
            )
    ) {
        WrapIconButton(
            modifier = Modifier.align(Alignment.CenterStart),
            iconData = AppIcons.Menu,
            customTint = MaterialTheme.colorScheme.onSurface,
        ) {
            onDashboardEventSent(ShowSideMenuEvent)
        }

        Text(
            modifier = Modifier.align(Alignment.Center),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium,
            text = stringResource(R.string.documents_screen_title)
        )

        WrapIconButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            iconData = AppIcons.Add,
            customTint = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            onEventSend(Event.AddDocumentPressed)
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                paddingValues = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 0.dp,
                    start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                    end = paddingValues.calculateEndPadding(LayoutDirection.Ltr)
                )
            ),
        contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding()),
    ) {
        item {
            val searchItem =
                SearchItem(searchLabel = stringResource(R.string.documents_screen_search_label))
            FiltersSearchBar(
                placeholder = searchItem.searchLabel,
                onValueChange = { onEventSend(Event.OnSearchQueryChanged(it)) },
                onFilterClick = { onEventSend(Event.FiltersPressed) },
                onClearClick = { onEventSend(Event.OnSearchQueryChanged("")) },
                isFilteringActive = state.isFilteringActive,
                text = state.searchText
            )
            VSpacer.Large()
        }

        if (state.showNoResultsFound) {
            item {
                NoResults(modifier = Modifier.fillMaxWidth())
            }
        } else {
            itemsIndexed(items = state.documentsUi) { index, (documentCategory, documents) ->
                DocumentCategory(
                    modifier = Modifier.fillMaxWidth(),
                    category = documentCategory,
                    documents = documents,
                    onEventSend = onEventSend
                )

                if (index != state.documentsUi.lastIndex) {
                    VSpacer.ExtraLarge()
                }
            }
        }
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME
    ) {
        onEventSend(Event.GetDocuments)
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_PAUSE
    ) {
        onEventSend(Event.OnPause)
    }

    OneTimeLaunchedEffect {
        onEventSend(Event.Init)
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

                is Effect.DocumentsFetched -> {
                    onEventSend(Event.TryIssuingDeferredDocuments(effect.deferredDocs))
                }

                is Effect.ResumeOnApplyFilter -> {
                    onEventSend(Event.GetDocuments)
                }
            }
        }.collect()
    }
}

@Composable
private fun DocumentCategory(
    modifier: Modifier = Modifier,
    category: DocumentCategory,
    documents: List<DocumentUi>,
    onEventSend: (Event) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        SectionTitle(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(category.stringResId)
        )

        documents.forEach { documentItem: DocumentUi ->
            WrapListItem(
                modifier = Modifier.fillMaxWidth(),
                item = documentItem.uiData,
                onItemClick = {
                    val onItemClickEvent = if (
                        documentItem.documentIssuanceState == DocumentUiIssuanceState.Pending
                        || documentItem.documentIssuanceState == DocumentUiIssuanceState.Failed
                    ) {
                        Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.DocumentSelected(
                            documentId = documentItem.uiData.itemId
                        )
                    } else {
                        Event.GoToDocumentDetails(documentItem.uiData.itemId)
                    }
                    onEventSend(onItemClickEvent)
                },
                supportingTextColor = when (documentItem.documentIssuanceState) {
                    DocumentUiIssuanceState.Issued -> null
                    DocumentUiIssuanceState.Pending -> MaterialTheme.colorScheme.warning
                    DocumentUiIssuanceState.Failed -> MaterialTheme.colorScheme.error
                    DocumentUiIssuanceState.Expired -> MaterialTheme.colorScheme.error
                    DocumentUiIssuanceState.Revoked -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@Composable
private fun NoResults(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        WrapListItem(
            item = ListItemData(
                itemId = stringResource(R.string.documents_screen_search_no_results_id),
                mainContentData = ListItemMainContentData.Text(text = stringResource(R.string.documents_screen_search_no_results)),
            ),
            onItemClick = null,
            modifier = Modifier.fillMaxWidth(),
            mainContentVerticalPadding = SPACING_MEDIUM.dp,
        )
    }
}

@Composable
private fun DocumentsSheetContent(
    sheetContent: DocumentsBottomSheetContent,
    state: State,
    onEventSent: (event: Event) -> Unit,
) {
    when (sheetContent) {
        is DocumentsBottomSheetContent.Filters -> {
            GenericBottomSheet(
                titleContent = {
                    Text(
                        text = stringResource(R.string.documents_screen_filters_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                bodyContent = {
                    val expandStateList by remember {
                        mutableStateOf(state.filtersUi.map { false }.toMutableStateList())
                    }

                    var buttonsRowHeight by remember { mutableIntStateOf(0) }

                    Box {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = with(LocalDensity.current) { buttonsRowHeight.toDp() }),
                            verticalArrangement = Arrangement.spacedBy(SPACING_LARGE.dp)
                        ) {
                            DualSelectorButtons(state.sortOrder) {
                                onEventSent(Event.OnSortingOrderChanged(it))
                            }
                            state.filtersUi.forEachIndexed { index, filter ->
                                if (filter.nestedItems.isNotEmpty()) {
                                    WrapExpandableListItem(
                                        header = filter.header,
                                        data = filter.nestedItems,
                                        isExpanded = expandStateList[index],
                                        onExpandedChange = {
                                            expandStateList[index] = !expandStateList[index]
                                        },
                                        onItemClick = {
                                            val id = it.itemId
                                            val groupId = filter.header.itemId
                                            onEventSent(Event.OnFilterSelectionChanged(id, groupId))
                                        },
                                        addDivider = false,
                                        collapsedMainContentVerticalPadding = SPACING_MEDIUM.dp,
                                        expandedMainContentVerticalPadding = SPACING_MEDIUM.dp,
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                .onGloballyPositioned { coordinates ->
                                    buttonsRowHeight = coordinates.size.height
                                }
                                .padding(top = SPACING_LARGE.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            WrapButton(
                                modifier = Modifier.weight(1f),
                                buttonConfig = ButtonConfig(
                                    type = ButtonType.SECONDARY,
                                    onClick = {
                                        onEventSent(Event.OnFiltersReset)
                                    }
                                )
                            ) {
                                Text(text = stringResource(R.string.documents_screen_filters_reset))
                            }
                            HSpacer.Small()
                            WrapButton(
                                modifier = Modifier.weight(1f),
                                buttonConfig = ButtonConfig(
                                    type = ButtonType.PRIMARY,
                                    onClick = {
                                        onEventSent(Event.OnFiltersApply)
                                    }
                                )
                            ) {
                                Text(text = stringResource(R.string.documents_screen_filters_apply))
                            }
                        }
                    }
                }
            )
        }

        is DocumentsBottomSheetContent.AddDocument -> {
            BottomSheetWithTwoBigIcons(
                textData = BottomSheetTextData(
                    title = stringResource(R.string.documents_screen_add_document_title),
                    message = stringResource(R.string.documents_screen_add_document_description)
                ),
                options = listOf(
                    ModalOptionUi(
                        title = stringResource(R.string.documents_screen_add_document_option_list),
                        leadingIcon = AppIcons.AddDocumentFromList,
                        event = Event.BottomSheet.AddDocument.FromList,
                    ),
                    ModalOptionUi(
                        title = stringResource(R.string.documents_screen_add_document_option_qr),
                        leadingIcon = AppIcons.AddDocumentFromQr,
                        event = Event.BottomSheet.AddDocument.ScanQr,
                    )
                ),
                onEventSent = onEventSent
            )
        }

        is DocumentsBottomSheetContent.DeferredDocumentPressed -> {
            DialogBottomSheet(
                textData = BottomSheetTextData(
                    title = stringResource(
                        id = R.string.dashboard_bottom_sheet_deferred_document_pressed_title
                    ),
                    message = stringResource(
                        id = R.string.dashboard_bottom_sheet_deferred_document_pressed_subtitle
                    ),
                    positiveButtonText = stringResource(id = R.string.dashboard_bottom_sheet_deferred_document_pressed_primary_button_text),
                    negativeButtonText = stringResource(id = R.string.dashboard_bottom_sheet_deferred_document_pressed_secondary_button_text),
                ),
                onPositiveClick = {
                    onEventSent(
                        Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.PrimaryButtonPressed(
                            documentId = sheetContent.documentId
                        )
                    )
                },
                onNegativeClick = {
                    onEventSent(
                        Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.SecondaryButtonPressed(
                            documentId = sheetContent.documentId
                        )
                    )
                }
            )
        }

        is DocumentsBottomSheetContent.DeferredDocumentsReady -> {
            BottomSheetWithOptionsList(
                textData = BottomSheetTextData(
                    title = stringResource(
                        id = R.string.dashboard_bottom_sheet_deferred_documents_ready_title
                    ),
                    message = stringResource(
                        id = R.string.dashboard_bottom_sheet_deferred_documents_ready_subtitle
                    ),
                ),
                options = sheetContent.options,
                onEventSent = onEventSent,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ThemeModePreviews
@Composable
private fun DocumentsScreenPreview() {
    PreviewTheme {
        val scope = rememberCoroutineScope()
        val bottomSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
        ContentScreen(
            isLoading = false,
            navigatableAction = ScreenNavigateAction.NONE,
            onBack = { },
            topBar = {
                TopBar(
                    onEventSend = { },
                    onDashboardEventSent = {}
                )
            },
        ) { paddingValues ->
            val issuerName = "Issuer name"
            val validUntil = "Valid Until"
            val documentsList = listOf(
                DocumentUi(
                    documentIssuanceState = DocumentUiIssuanceState.Issued,
                    uiData = ListItemData(
                        itemId = "id1",
                        mainContentData = ListItemMainContentData.Text(text = "Document 1"),
                        overlineText = issuerName,
                        supportingText = validUntil,
                        leadingContentData = null,
                        trailingContentData = null
                    ),
                    documentIdentifier = DocumentIdentifier.MdocPid,
                    documentCategory = DocumentCategory.Government
                ),
                DocumentUi(
                    documentIssuanceState = DocumentUiIssuanceState.Issued,
                    uiData = ListItemData(
                        itemId = "id2",
                        mainContentData = ListItemMainContentData.Text(text = "Document 2"),
                        overlineText = issuerName,
                        supportingText = validUntil,
                        leadingContentData = null,
                        trailingContentData = null
                    ),
                    documentIdentifier = DocumentIdentifier.MdocPid,
                    documentCategory = DocumentCategory.Government
                ),
                DocumentUi(
                    documentIssuanceState = DocumentUiIssuanceState.Issued,
                    uiData = ListItemData(
                        itemId = "id3",
                        mainContentData = ListItemMainContentData.Text(text = "Document 3"),
                        overlineText = issuerName,
                        supportingText = validUntil,
                        leadingContentData = null,
                        trailingContentData = null
                    ),
                    documentIdentifier = DocumentIdentifier.OTHER(formatType = ""),
                    documentCategory = DocumentCategory.Finance
                ),
                DocumentUi(
                    documentIssuanceState = DocumentUiIssuanceState.Issued,
                    uiData = ListItemData(
                        itemId = "id4",
                        mainContentData = ListItemMainContentData.Text(text = "Document 4"),
                        overlineText = issuerName,
                        supportingText = validUntil,
                        leadingContentData = null,
                        trailingContentData = null
                    ),
                    documentIdentifier = DocumentIdentifier.OTHER(formatType = ""),
                    documentCategory = DocumentCategory.Other
                ),
            )
            Content(
                state = State(
                    isLoading = false,
                    isFilteringActive = false,
                    sortOrder = DualSelectorButtonData(
                        first = "first",
                        second = "second",
                        selectedButton = DualSelectorButton.FIRST,
                    ),
                    documentsUi = documentsList.groupBy { it.documentCategory }.toList(),
                ),
                effectFlow = Channel<Effect>().receiveAsFlow(),
                onEventSend = {},
                onNavigationRequested = {},
                paddingValues = paddingValues,
                coroutineScope = scope,
                modalBottomSheetState = bottomSheetState,
            )
        }
    }
}