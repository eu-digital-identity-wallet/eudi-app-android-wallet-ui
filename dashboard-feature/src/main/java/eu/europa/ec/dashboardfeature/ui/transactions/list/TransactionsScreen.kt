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

package eu.europa.ec.dashboardfeature.ui.transactions.list

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.model.TransactionUiStatus
import eu.europa.ec.corelogic.model.TransactionCategory
import eu.europa.ec.dashboardfeature.model.FilterDateRangeSelectionData
import eu.europa.ec.dashboardfeature.model.SearchItem
import eu.europa.ec.dashboardfeature.model.TransactionFilterIds
import eu.europa.ec.dashboardfeature.model.TransactionUi
import eu.europa.ec.eudi.rqesui.domain.util.safeLet
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.success
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.DatePickerDialogType
import eu.europa.ec.uilogic.component.DualSelectorButtonData
import eu.europa.ec.uilogic.component.DualSelectorButtons
import eu.europa.ec.uilogic.component.FiltersDatePickerDialog
import eu.europa.ec.uilogic.component.FiltersSearchBar
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.SectionTitle
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.HSpacer
import eu.europa.ec.uilogic.component.utils.LifecycleEffect
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem
import eu.europa.ec.uilogic.component.wrap.GenericBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapButton
import eu.europa.ec.uilogic.component.wrap.WrapExpandableCard
import eu.europa.ec.uilogic.component.wrap.WrapExpandableListItem
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapIconButton
import eu.europa.ec.uilogic.component.wrap.WrapListItem
import eu.europa.ec.uilogic.component.wrap.WrapListItems
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import eu.europa.ec.uilogic.extension.finish
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

typealias DashboardEvent = eu.europa.ec.dashboardfeature.ui.dashboard.Event
typealias ShowSideMenuEvent = eu.europa.ec.dashboardfeature.ui.dashboard.Event.SideMenu.Show

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    navHostController: NavController,
    viewModel: TransactionsViewModel,
    onDashboardEventSent: (DashboardEvent) -> Unit,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val datePickerDialogConfig = state.datePickerDialogConfig

    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ContentScreen(
        isLoading = state.isLoading,
        contentErrorConfig = state.error,
        navigatableAction = ScreenNavigateAction.NONE,
        onBack = { context.finish() },
        topBar = {
            TopBar(
                onDashboardEventSent = onDashboardEventSent
            )
        }
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
            modalBottomSheetState = bottomSheetState,
        )

        if (state.isBottomSheetOpen) {
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
                TransactionsSheetContent(
                    sheetContent = state.sheetContent,
                    filtersUi = state.filtersUi,
                    snapshotFilterDateRangeData = state.snapshotFilterDateRangeSelectionData,
                    sortOrder = state.sortOrder,
                    onEventSent = {
                        viewModel.setEvent(it)
                    }
                )
            }
        }

        if (state.isDatePickerDialogVisible) {
            FiltersDatePickerDialog(
                onDateSelected = { millis ->
                    safeLet(
                        datePickerDialogConfig.type,
                        millis,
                    ) { dateSelectionType, safeMillis ->
                        when (dateSelectionType) {
                            DatePickerDialogType.SelectStartDate -> {
                                viewModel.setEvent(
                                    Event.OnStartDateSelected(
                                        selectedDateUtcMillis = safeMillis
                                    )
                                )
                            }

                            DatePickerDialogType.SelectEndDate -> {
                                viewModel.setEvent(
                                    Event.OnEndDateSelected(
                                        selectedDateUtcMillis = safeMillis
                                    )
                                )
                            }
                        }
                    }
                },
                onDismiss = {
                    viewModel.setEvent(
                        Event.DatePickerDialog.UpdateDialogState(isVisible = false)
                    )
                },
                datePickerDialogConfig = datePickerDialogConfig
            )
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
                SearchItem(searchLabel = stringResource(R.string.transactions_screen_search_label))
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
            itemsIndexed(items = state.transactionsUi) { index, (documentCategory, documents) ->
                TransactionCategory(
                    modifier = Modifier.fillMaxWidth(),
                    category = documentCategory,
                    transactions = documents,
                    onEventSend = onEventSend
                )

                if (index != state.transactionsUi.lastIndex) {
                    VSpacer.ExtraLarge()
                }
            }
        }
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME
    ) {
        onEventSend(Event.OnResume)
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
                        if (!modalBottomSheetState.isVisible) {
                            onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                        }
                    }
                }

                is Effect.ShowBottomSheet -> {
                    onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = true))
                }

                is Effect.ShowDatePickerDialog -> {
                    onEventSend(Event.DatePickerDialog.UpdateDialogState(isVisible = true))
                }
            }
        }.collect()
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
private fun TransactionCategory(
    modifier: Modifier = Modifier,
    category: TransactionCategory,
    transactions: List<TransactionUi>,
    onEventSend: (Event) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        SectionTitle(
            modifier = Modifier.fillMaxWidth(),
            text = category.displayName ?: stringResource(category.stringResId)
        )

        val transactionItems = remember(key1 = transactions) {
            transactions.map { it.uiData }
        }
        val transactionMap = remember(key1 = transactions) {
            transactions.associateBy { it.uiData.header.itemId }
        }

        WrapListItems(
            modifier = Modifier.fillMaxWidth(),
            items = transactionItems,
            onItemClick = { item ->
                onEventSend(
                    Event.TransactionItemPressed(itemId = item.itemId)
                )
            },
            onExpandedChange = null,
            overlineTextStyle = { item ->
                val transactionUi = transactionMap[item.itemId]

                val overlineTextColor = when (transactionUi?.uiStatus) {
                    TransactionUiStatus.Completed -> MaterialTheme.colorScheme.success
                    TransactionUiStatus.Failed -> MaterialTheme.colorScheme.error
                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                MaterialTheme.typography.labelMedium.copy(
                    color = overlineTextColor
                )
            }
        )
    }
}

@Composable
private fun NoResults(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        WrapListItem(
            item = ListItemData(
                itemId = stringResource(R.string.transactions_screen_search_no_results_id),
                mainContentData = ListItemMainContentData.Text(
                    text = stringResource(R.string.transactions_screen_search_no_results)
                ),
            ),
            onItemClick = null,
            modifier = Modifier.fillMaxWidth(),
            mainContentVerticalPadding = SPACING_MEDIUM.dp,
        )
    }
}

@Composable
private fun TopBar(
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
            text = stringResource(R.string.transactions_screen_title)
        )
    }
}

@Composable
private fun TransactionsSheetContent(
    sheetContent: TransactionsBottomSheetContent,
    filtersUi: List<ExpandableListItem.NestedListItemData>,
    snapshotFilterDateRangeData: FilterDateRangeSelectionData,
    sortOrder: DualSelectorButtonData,
    onEventSent: (event: Event) -> Unit,
) {
    when (sheetContent) {
        is TransactionsBottomSheetContent.Filters -> {
            GenericBottomSheet(
                titleContent = {
                    Text(
                        text = stringResource(R.string.transactions_screen_filters_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                bodyContent = {
                    val expandStateList by remember {
                        mutableStateOf(filtersUi.map { false }.toMutableStateList())
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
                            DualSelectorButtons(sortOrder) {
                                onEventSent(
                                    Event.OnSortingOrderChanged(it)
                                )
                            }

                            filtersUi.forEachIndexed { index, filter ->
                                when {
                                    filter.header.itemId == TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_GROUP_ID -> {
                                        WrapExpandableCard(
                                            cardCollapsedContent = {
                                                WrapListItem(
                                                    mainContentVerticalPadding = SPACING_MEDIUM.dp,
                                                    item = filter.header,
                                                    onItemClick = {
                                                        expandStateList[index] =
                                                            !expandStateList[index]
                                                    },
                                                    mainContentTextStyle = MaterialTheme.typography.bodyLarge.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                            },
                                            cardExpandedContent = {
                                                Column(
                                                    modifier = Modifier.padding(
                                                        start = SPACING_MEDIUM.dp,
                                                        end = SPACING_MEDIUM.dp,
                                                        bottom = SPACING_MEDIUM.dp
                                                    )
                                                ) {
                                                    FiltersDatePickerField(
                                                        dialogType = DatePickerDialogType.SelectStartDate,
                                                        selectDateLabel = stringResource(R.string.transactions_screen_filters_date_from),
                                                        displayedSelectedDate = snapshotFilterDateRangeData.displayedStartDate,
                                                        onEventSent = onEventSent
                                                    )

                                                    FiltersDatePickerField(
                                                        dialogType = DatePickerDialogType.SelectEndDate,
                                                        selectDateLabel = stringResource(R.string.transactions_screen_filters_date_to),
                                                        displayedSelectedDate = snapshotFilterDateRangeData.displayedEndDate,
                                                        onEventSent = onEventSent
                                                    )
                                                }
                                            },
                                            isExpanded = expandStateList[index],
                                        )
                                    }

                                    filter.nestedItems.isNotEmpty() -> {
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
                                                onEventSent(
                                                    Event.OnFilterSelectionChanged(
                                                        filterId = id,
                                                        groupId
                                                    )
                                                )
                                            },
                                            addDivider = false,
                                            collapsedMainContentVerticalPadding = SPACING_MEDIUM.dp,
                                            expandedMainContentVerticalPadding = SPACING_MEDIUM.dp,
                                        )
                                    }
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
                                Text(text = stringResource(R.string.transactions_screen_filters_reset))
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
                                Text(text = stringResource(R.string.transactions_screen_filters_apply))
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun FiltersDatePickerField(
    modifier: Modifier = Modifier,
    dialogType: DatePickerDialogType,
    selectDateLabel: String,
    displayedSelectedDate: String,
    onEventSent: (event: Event) -> Unit
) {
    OutlinedTextField(
        readOnly = true,
        value = displayedSelectedDate,
        onValueChange = {},
        label = { Text(selectDateLabel) },
        placeholder = { Text(stringResource(R.string.transactions_screen_text_field_date_pattern)) },
        trailingIcon = { WrapIcon(AppIcons.DateRange) },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(displayedSelectedDate) {
                awaitEachGesture {
                    awaitFirstDown(pass = PointerEventPass.Initial)
                    val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    if (upEvent != null) {
                        onEventSent(
                            Event.ShowDatePicker(datePickerType = dialogType)
                        )
                    }
                }
            }
    )
}

@ThemeModePreviews
@Composable
private fun TransactionsScreenPreview() {
    ContentScreen(
        isLoading = false,
        navigatableAction = ScreenNavigateAction.NONE,
        onBack = { },
        topBar = {
            TopBar(
                onDashboardEventSent = {}
            )
        },
    ) {}
}