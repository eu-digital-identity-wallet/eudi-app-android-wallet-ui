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

package eu.europa.ec.dashboardfeature.ui.transactions

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.corelogic.model.TransactionCategory
import eu.europa.ec.dashboardfeature.model.SearchItem
import eu.europa.ec.dashboardfeature.model.TransactionFilterIds
import eu.europa.ec.dashboardfeature.model.TransactionUi
import eu.europa.ec.dashboardfeature.model.TransactionUiStatus
import eu.europa.ec.dashboardfeature.ui.FiltersSearchBar
import eu.europa.ec.eudi.rqesui.domain.util.safeLet
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.success
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.DualSelectorButtonData
import eu.europa.ec.uilogic.component.DualSelectorButtons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.SectionTitle
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.HSpacer
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemData
import eu.europa.ec.uilogic.component.wrap.GenericBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapButton
import eu.europa.ec.uilogic.component.wrap.WrapExpandableCard
import eu.europa.ec.uilogic.component.wrap.WrapExpandableListItem
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapIconButton
import eu.europa.ec.uilogic.component.wrap.WrapListItem
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import eu.europa.ec.uilogic.extension.finish
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

    val isBottomSheetOpen = state.isBottomSheetOpen
    val isDatePickerDialogVisible = state.isDatePickerDialogVisible
    val datePickerDialogConfig = state.datePickerDialogConfig

    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ContentScreen(
            isLoading = false,
            navigatableAction = ScreenNavigateAction.NONE,
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
                    TransactionsSheetContent(
                        sheetContent = state.sheetContent,
                        filtersUi = state.filtersUi,
                        filterDateRangeSelectionData = state.filterDateRangeSelectionData,
                        sortOrder = state.sortOrder,
                        onEventSent = {
                            viewModel.setEvent(it)
                        }
                    )
                }
            }

            if (isDatePickerDialogVisible) {
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
                                            selectedDateMillis = safeMillis
                                        )
                                    )
                                }

                                DatePickerDialogType.SelectEndDate -> {
                                    viewModel.setEvent(
                                        Event.OnEndDateSelected(
                                            selectedDateMillis = safeMillis
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

                is Effect.ResumeOnApplyFilter -> {
                    // TODO check if needed
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

        CategoryItems(
            transactions = transactions,
            onEventSend = onEventSend
        )
    }
}

@Composable
private fun CategoryItems(
    transactions: List<TransactionUi>,
    onEventSend: (Event) -> Unit,
) {
    Column {
        transactions.forEachIndexed { index, transactionUi ->
            val itemModifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = if (index == 0) SPACING_SMALL.dp else 0.dp,
                    bottom = if (index == transactions.lastIndex) SPACING_SMALL.dp else 0.dp,
                )
            val cardShape = when {
                transactions.size == 1 -> RoundedCornerShape(SIZE_SMALL.dp)

                index == 0 -> RoundedCornerShape(
                    topStart = SIZE_SMALL.dp,
                    topEnd = SIZE_SMALL.dp
                )

                index == transactions.lastIndex -> RoundedCornerShape(
                    bottomStart = SIZE_SMALL.dp,
                    bottomEnd = SIZE_SMALL.dp
                )

                else -> RectangleShape
            }

            WrapListItem(
                modifier = itemModifier,
                item = transactionUi.uiData,
                onItemClick = { item ->
                    onEventSend(
                        Event.TransactionItemPressed(itemId = item.itemId)
                    )
                },
                overlineTextStyle = MaterialTheme.typography.labelMedium.copy(
                    when (transactionUi.uiStatus) {
                        TransactionUiStatus.Completed -> MaterialTheme.colorScheme.success
                        TransactionUiStatus.Failed -> MaterialTheme.colorScheme.error
                    }
                ),
                shape = cardShape
            )

            if (index < transactions.lastIndex) {
                Row(modifier = Modifier.background(color = MaterialTheme.colorScheme.surfaceContainer)) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = SPACING_MEDIUM.dp))
                }
            }
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
    filtersUi: List<ExpandableListItemData>,
    filterDateRangeSelectionData: FilterDateRangeSelectionData,
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
                            filtersUi.forEachIndexed { index, filter ->
                                when {
                                    filter.collapsed.itemId == TransactionFilterIds.FILTER_SORT_GROUP_ID -> {
                                        WrapExpandableCard(
                                            cardCollapsedContent = {
                                                WrapListItem(
                                                    mainContentVerticalPadding = SPACING_MEDIUM.dp,
                                                    item = filter.collapsed,
                                                    onItemClick = {
                                                        expandStateList[index] =
                                                            !expandStateList[index]
                                                    }
                                                )
                                            },
                                            cardExpandedContent = {
                                                Row(modifier = Modifier.padding(top = SPACING_MEDIUM.dp)) {
                                                    DualSelectorButtons(sortOrder) {
                                                        onEventSent(
                                                            Event.OnSortingOrderChanged(it)
                                                        )
                                                    }
                                                }
                                            },
                                            isExpanded = expandStateList[index],
                                        )
                                    }

                                    filter.collapsed.itemId == TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_GROUP_ID -> {
                                        WrapExpandableCard(
                                            cardCollapsedContent = {
                                                WrapListItem(
                                                    mainContentVerticalPadding = SPACING_MEDIUM.dp,
                                                    item = filter.collapsed,
                                                    onItemClick = {
                                                        expandStateList[index] =
                                                            !expandStateList[index]
                                                    }
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
                                                        displayedSelectedDate = filterDateRangeSelectionData.displayedStartDate,
                                                        onEventSent = onEventSent
                                                    )

                                                    FiltersDatePickerField(
                                                        dialogType = DatePickerDialogType.SelectEndDate,
                                                        selectDateLabel = stringResource(R.string.transactions_screen_filters_date_to),
                                                        displayedSelectedDate = filterDateRangeSelectionData.displayedEndDate,
                                                        onEventSent = onEventSent
                                                    )
                                                }
                                            },
                                            isExpanded = expandStateList[index],
                                        )
                                    }

                                    filter.expanded.isNotEmpty() -> {
                                        WrapExpandableListItem(
                                            data = filter,
                                            isExpanded = expandStateList[index],
                                            onExpandedChange = {
                                                expandStateList[index] = !expandStateList[index]
                                            },
                                            onItemClick = {
                                                val id = it.itemId
                                                val groupId = filter.collapsed.itemId
                                                onEventSent(
                                                    Event.OnFilterSelectionChanged(
                                                        filterId = id,
                                                        groupId
                                                    )
                                                )
                                            },
                                            expandedAddDivider = false,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersDatePickerDialog(
    datePickerDialogConfig: DatePickerDialogConfig,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val customSelectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            val date = Instant.ofEpochMilli(utcTimeMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            val min = datePickerDialogConfig.lowerLimit ?: LocalDate.MIN
            val max = datePickerDialogConfig.upperLimit ?: LocalDate.MAX
            return !date.isBefore(min) && !date.isAfter(max)
        }
    }

    val datePickerState = rememberDatePickerState(selectableDates = customSelectableDates)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text(stringResource(R.string.generic_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.generic_cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
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