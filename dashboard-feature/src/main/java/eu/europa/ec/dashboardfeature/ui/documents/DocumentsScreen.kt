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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.model.DocumentUiIssuanceState
import eu.europa.ec.dashboardfeature.model.SearchItem
import eu.europa.ec.dashboardfeature.ui.FiltersSearchBar
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.warning
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.DualSelectorButtonData
import eu.europa.ec.uilogic.component.DualSelectorButtons
import eu.europa.ec.uilogic.component.ModalOptionUi
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.utils.HSpacer
import eu.europa.ec.uilogic.component.utils.LifecycleEffect
import eu.europa.ec.uilogic.component.utils.SIZE_XX_LARGE
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
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemData
import eu.europa.ec.uilogic.component.wrap.GenericBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapButton
import eu.europa.ec.uilogic.component.wrap.WrapExpandableListItem
import eu.europa.ec.uilogic.component.wrap.WrapIconButton
import eu.europa.ec.uilogic.component.wrap.WrapListItem
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import eu.europa.ec.uilogic.extension.finish
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

typealias DashboardEvent = eu.europa.ec.dashboardfeature.ui.dashboard_new.Event
typealias ShowSideMenuEvent = eu.europa.ec.dashboardfeature.ui.dashboard_new.Event.SideMenu.Show

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    navHostController: NavController,
    viewModel: DocumentsViewModel,
    onDashboardEventSent: (DashboardEvent) -> Unit,
) {
    val context = LocalContext.current
    val state = viewModel.viewState.value

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
            modalBottomSheetState = bottomSheetState,
            paddingValues = paddingValues,
        )
    }
}

private fun handleNavigationEffect(
    navigationEffect: Effect.Navigation,
    navController: NavController,
    context: Context
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
    Row(
        modifier = Modifier
            .height(SIZE_XX_LARGE.dp)
            .fillMaxSize()
            .padding(SPACING_MEDIUM.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        WrapIconButton(
            modifier = Modifier.offset(x = -SPACING_SMALL.dp),
            iconData = AppIcons.Menu,
            shape = null
        ) {
            onDashboardEventSent(ShowSideMenuEvent)
        }

        Text(
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineLarge,
            text = stringResource(R.string.documents_screen_title)
        )
        WrapIconButton(
            iconData = AppIcons.Add
        ) {
            onEventSend(Event.ShowAddDocumentBottomSheet(isOpen = true))
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
    modalBottomSheetState: SheetState,
    paddingValues: PaddingValues,
) {
    val isAddDocumentBottomSheetOpen = state.showAddDocumentBottomSheet
    val isFiltersBottomSheetOpen = state.showFiltersBottomSheet
    val isDeferredDocumentsReadyBottomSheetOpen = state.showDeferredDocumentsReadyBottomSheet
    val isDeferredDocumentPressedBottomSheet = state.showDeferredDocumentPressedBottomSheet

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
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        item {
            val searchItem =
                SearchItem(searchLabel = stringResource(R.string.documents_screen_search_label))
            FiltersSearchBar(
                placeholder = searchItem.searchLabel,
                onValueChange = { onEventSend(Event.OnSearchQueryChanged(it)) },
                onFilterClick = { onEventSend(Event.ShowFiltersBottomSheet(isOpen = true)) },
                isFilteringActive = state.isFilteringActive
            )
        }
        items(state.documents) { documentItem ->
            WrapListItem(
                item = documentItem.uiData,
                onItemClick = if (documentItem.uiData.itemId.isBlank()) {
                    null
                } else {
                    if (documentItem.documentIssuanceState == DocumentUiIssuanceState.Issued) {
                        { onEventSend(Event.GoToDocumentDetails(documentItem.uiData.itemId)) }
                    } else {
                        { onEventSend(Event.DeferredNotReadyYet.DocumentSelected(documentId = documentItem.uiData.itemId)) }
                    }
                },
                supportingTextColor = when (documentItem.documentIssuanceState) {
                    DocumentUiIssuanceState.Issued -> null
                    DocumentUiIssuanceState.Pending -> MaterialTheme.colorScheme.warning
                    DocumentUiIssuanceState.Failed -> MaterialTheme.colorScheme.error
                }
            )
        }
    }

    if (isAddDocumentBottomSheetOpen) {
        AddDocumentBottomSheet(onEventSend, modalBottomSheetState)
    }

    if (isFiltersBottomSheetOpen) {
        FiltersBottomSheet(
            state.filters,
            state.sortingOrderButtonData,
            onEventSend,
            modalBottomSheetState
        )
    }

    if (isDeferredDocumentsReadyBottomSheetOpen) {
        DeferredDocumentsReadyBottomSheet(
            onEventSend = onEventSend,
            modalBottomSheetState = modalBottomSheetState,
            successfullyIssuedDeferredDocumentsUi = state.successfullyIssuedDeferredDocumentsUi,
        )
    }

    state.deferredSelectedDocumentToBeDeleted?.let {
        if (isDeferredDocumentPressedBottomSheet) {
            DeferredDocumentPressedBottomSheet(
                onEventSend = onEventSend,
                modalBottomSheetState = modalBottomSheetState,
                documentIdToBeDeleted = it
            )
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

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation -> onNavigationRequested(effect)

                is Effect.DocumentsFetched -> {
                    onEventSend(Event.TryIssuingDeferredDocuments(effect.deferredDocs))
                }
            }
        }.collect()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDocumentBottomSheet(
    onEventSend: (Event) -> Unit,
    modalBottomSheetState: SheetState,
) {
    WrapModalBottomSheet(
        onDismissRequest = {
            onEventSend(Event.ShowAddDocumentBottomSheet(isOpen = false))
        },
        sheetState = modalBottomSheetState
    ) {
        BottomSheetWithTwoBigIcons(
            textData = BottomSheetTextData(
                title = stringResource(R.string.documents_screen_add_document_title),
                message = stringResource(R.string.documents_screen_add_document_description)
            ),
            options = listOf(
                ModalOptionUi(
                    title = stringResource(R.string.documents_screen_add_document_option_list),
                    leadingIcon = AppIcons.AddDocumentFromList,
                    leadingIconTint = MaterialTheme.colorScheme.primary,
                    event = Event.GoToAddDocument,
                ),
                ModalOptionUi(
                    title = stringResource(R.string.documents_screen_add_document_option_qr),
                    leadingIcon = AppIcons.AddDocumentFromQr,
                    leadingIconTint = MaterialTheme.colorScheme.primary,
                    event = Event.GoToQrScan,
                )
            ),
            onEventSent = { onEventSend(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersBottomSheet(
    filters: List<ExpandableListItemData>,
    sortOrderButtonData: DualSelectorButtonData,
    onEventSend: (Event) -> Unit,
    modalBottomSheetState: SheetState,
) {
    WrapModalBottomSheet(
        onDismissRequest = {
            onEventSend(Event.ShowFiltersBottomSheet(isOpen = false))
        },
        sheetState = modalBottomSheetState
    ) {

        GenericBottomSheet(
            titleContent = {
                Text(
                    text = stringResource(R.string.documents_screen_filters_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            bodyContent = {
                val expandStateList by remember {
                    mutableStateOf(filters.map { false }.toMutableStateList())
                }

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(SPACING_LARGE.dp)
                ) {
                    DualSelectorButtons(sortOrderButtonData) {
                        onEventSend(Event.OnSortingOrderChanged(it))
                    }
                    filters.forEachIndexed { index, filter ->
                        if (filter.expanded.isNotEmpty()) {
                            WrapExpandableListItem(
                                data = filter,
                                isExpanded = expandStateList[index],
                                onExpandedChange = {
                                    expandStateList[index] = !expandStateList[index]
                                },
                                onItemClick = {
                                    val id = it.itemId
                                    val groupId = filter.collapsed.itemId
                                    onEventSend(Event.OnFilterSelectionChanged(id, groupId))
                                }
                            )
                        }
                    }

                    VSpacer.Large()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WrapButton(
                            modifier = Modifier.weight(1f),
                            buttonConfig = ButtonConfig(type = ButtonType.SECONDARY, onClick = {
                                onEventSend(Event.OnFiltersReset)
                            })
                        ) {
                            Text(text = stringResource(R.string.documents_screen_filters_reset))
                        }
                        HSpacer.Small()
                        WrapButton(
                            modifier = Modifier.weight(1f),
                            buttonConfig = ButtonConfig(type = ButtonType.PRIMARY, onClick = {
                                onEventSend(Event.OnFiltersApply)
                            })
                        ) {
                            Text(text = stringResource(R.string.documents_screen_filters_apply))
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeferredDocumentsReadyBottomSheet(
    onEventSend: (Event) -> Unit,
    modalBottomSheetState: SheetState,
    successfullyIssuedDeferredDocumentsUi: List<ModalOptionUi<Event>>,
) {
    WrapModalBottomSheet(
        onDismissRequest = {
            onEventSend(Event.ShowDeferredDocumentsReadyBottomSheet(isOpen = false))
        },
        sheetState = modalBottomSheetState
    ) {
        BottomSheetWithOptionsList(
            textData = BottomSheetTextData(
                title = stringResource(
                    id = R.string.dashboard_bottom_sheet_deferred_documents_ready_title
                ),
                message = stringResource(
                    id = R.string.dashboard_bottom_sheet_deferred_documents_ready_subtitle
                ),
            ),
            options = successfullyIssuedDeferredDocumentsUi,
            onEventSent = onEventSend,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeferredDocumentPressedBottomSheet(
    onEventSend: (Event) -> Unit,
    modalBottomSheetState: SheetState,
    documentIdToBeDeleted: DocumentId,
) {
    WrapModalBottomSheet(
        onDismissRequest = {
            onEventSend(Event.ShowDeferredDocumentPressedBottomSheet(isOpen = false))
        },
        sheetState = modalBottomSheetState
    ) {
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
                onEventSend(
                    Event.DeferredNotReadyYet.PrimaryButtonPressed(
                        documentId = documentIdToBeDeleted
                    )
                )
            },
            onNegativeClick = {
                onEventSend(
                    Event.DeferredNotReadyYet.SecondaryButtonPressed(
                        documentId = documentIdToBeDeleted
                    )
                )
            }
        )
    }
}