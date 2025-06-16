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

import androidx.lifecycle.viewModelScope
import eu.europa.ec.businesslogic.util.localDateToUtcMillis
import eu.europa.ec.businesslogic.util.toLocalDateTime
import eu.europa.ec.businesslogic.util.utcMillisToLocalDate
import eu.europa.ec.businesslogic.validator.model.SortOrder
import eu.europa.ec.dashboardfeature.interactor.TransactionInteractorFilterPartialState
import eu.europa.ec.dashboardfeature.interactor.TransactionInteractorGetTransactionsPartialState
import eu.europa.ec.dashboardfeature.interactor.TransactionsInteractor
import eu.europa.ec.dashboardfeature.ui.transactions.list.model.FilterDateRangeSelectionUi
import eu.europa.ec.dashboardfeature.ui.transactions.list.model.TransactionCategoryUi
import eu.europa.ec.dashboardfeature.ui.transactions.list.model.TransactionFilterIds
import eu.europa.ec.dashboardfeature.ui.transactions.list.model.TransactionUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.DatePickerDialogConfig
import eu.europa.ec.uilogic.component.DatePickerDialogType
import eu.europa.ec.uilogic.component.DualSelectorButton
import eu.europa.ec.uilogic.component.DualSelectorButtonDataUi
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import java.time.LocalDate

data class State(
    val isLoading: Boolean,
    val error: ContentErrorConfig? = null,

    val searchText: String = "",
    val isFromOnPause: Boolean = true,
    val isFilteringActive: Boolean,
    val showNoResultsFound: Boolean = false,
    val isBottomSheetOpen: Boolean = false,
    val sheetContent: TransactionsBottomSheetContent = TransactionsBottomSheetContent.Filters(
        filters = emptyList()
    ),

    val transactionsUi: List<Pair<TransactionCategoryUi, List<TransactionUi>>> = emptyList(),
    val filtersUi: List<ExpandableListItemUi.NestedListItem> = emptyList(),
    val shouldRevertFilterChanges: Boolean = true,
    val sortOrder: DualSelectorButtonDataUi,
    val isDatePickerDialogVisible: Boolean = false,
    val datePickerDialogConfig: DatePickerDialogConfig = DatePickerDialogConfig(
        type = DatePickerDialogType.SelectStartDate
    ),
    // persisted selected date range applied for filtering
    val filterDateRangeSelectionUi: FilterDateRangeSelectionUi = FilterDateRangeSelectionUi(),
    // temporary date range data while filter bottom sheet is open to collect date picker dialog selections
    val snapshotFilterDateRangeSelectionUi: FilterDateRangeSelectionUi = FilterDateRangeSelectionUi(),
    // defines the overall date picker boundaries based on the fetched documents.
    val datePickerLimits: FilterDateRangeSelectionUi = FilterDateRangeSelectionUi(),
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object OnResume : Event()
    data object OnPause : Event()
    data object Pop : Event()

    data class OnSearchQueryChanged(val query: String) : Event()
    data class OnFilterSelectionChanged(val filterId: String, val groupId: String) : Event()
    data object OnFiltersReset : Event()
    data object OnFiltersApply : Event()
    data class OnSortingOrderChanged(val sortingOrder: DualSelectorButton) : Event()
    data object FiltersPressed : Event()

    data class TransactionItemPressed(val itemId: String) : Event()

    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(val isOpen: Boolean) : BottomSheet()
        data object Close : BottomSheet()
    }

    sealed class DatePickerDialog : Event() {
        data class UpdateDialogState(val isVisible: Boolean) : DatePickerDialog()
    }

    data class ShowDatePicker(val datePickerType: DatePickerDialogType) : Event()
    data class OnStartDateSelected(val selectedDateUtcMillis: Long) : Event()
    data class OnEndDateSelected(val selectedDateUtcMillis: Long) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
        data class SwitchScreen(
            val screenRoute: String,
            val popUpToScreenRoute: String = DashboardScreens.Dashboard.screenRoute,
            val inclusive: Boolean = false,
        ) : Navigation()
    }

    data object ShowBottomSheet : Effect()
    data object CloseBottomSheet : Effect()
    data object ShowDatePickerDialog : Effect()
}

sealed class TransactionsBottomSheetContent {
    data class Filters(val filters: List<ExpandableListItemUi.SingleListItem>) :
        TransactionsBottomSheetContent()
}

@KoinViewModel
class TransactionsViewModel(
    private val interactor: TransactionsInteractor,
    private val resourceProvider: ResourceProvider
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State {
        return State(
            isLoading = true,
            sortOrder = DualSelectorButtonDataUi(
                first = resourceProvider.getString(R.string.transactions_screen_filters_descending),
                second = resourceProvider.getString(R.string.transactions_screen_filters_ascending),
                selectedButton = DualSelectorButton.FIRST,
            ),
            isFilteringActive = false,
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                collectSearchAndFilterStateChanges()
            }

            is Event.OnResume -> {
                getTransactions(event)
            }

            is Event.OnPause -> {
                setState { copy(isFromOnPause = true) }
            }

            is Event.FiltersPressed -> {
                showBottomSheet(
                    sheetContent = TransactionsBottomSheetContent.Filters(
                        filters = emptyList()
                    )
                )
            }

            is Event.OnFilterSelectionChanged -> {
                updateFilter(event.filterId, event.groupId)
            }

            is Event.OnFiltersApply -> {
                applySelectedFilters()
            }

            is Event.OnFiltersReset -> {
                resetFilters()
            }

            is Event.OnSearchQueryChanged -> {
                applySearch(event.query)
            }

            is Event.OnSortingOrderChanged -> {
                sortOrderChanged(event.sortingOrder)
            }

            is Event.TransactionItemPressed -> {
                onTransactionItemClicked(itemId = event.itemId)
            }

            is Event.BottomSheet.UpdateBottomSheetState -> {
                revertFilters(event.isOpen)
            }

            is Event.Pop -> setEffect { Effect.Navigation.Pop }
            is Event.BottomSheet.Close -> {
                hideBottomSheet()
            }

            is Event.ShowDatePicker -> {
                /* Computes dynamic date limits for the DatePicker:
                    - startUpperLimit: the earliest non-null end date from either the selected filter or picker constraints.
                    - endLowerLimit: the latest non-null start date from either the selected filter or picker constraints.
                    Safely handles nullable dates using listOfNotNull(...).minOrNull() and maxOrNull().
                */
                val viewStateValue = viewState.value
                val snapshotData = viewStateValue.snapshotFilterDateRangeSelectionUi
                val limits = viewStateValue.datePickerLimits

                val startLowerLimit = limits.startDate
                val startUpperLimit = listOfNotNull(
                    snapshotData.endDate,
                    limits.endDate
                ).minOrNull()
                val selectedStartDate = snapshotData.startDate ?: limits.startDate

                val endLowerLimit = listOfNotNull(
                    snapshotData.startDate,
                    limits.startDate
                ).maxOrNull()
                val endUpperLimit = limits.endDate
                val selectedEndDate = snapshotData.endDate ?: limits.endDate

                val datePickerDialogConfig = when (event.datePickerType) {
                    DatePickerDialogType.SelectStartDate -> DatePickerDialogConfig(
                        type = DatePickerDialogType.SelectStartDate,
                        lowerLimit = startLowerLimit,
                        upperLimit = startUpperLimit,
                        selectedUtcDateMillis = selectedStartDate?.let {
                            localDateToUtcMillis(
                                localDate = it
                            )
                        }
                    )

                    DatePickerDialogType.SelectEndDate -> {
                        DatePickerDialogConfig(
                            type = DatePickerDialogType.SelectEndDate,
                            lowerLimit = endLowerLimit,
                            upperLimit = endUpperLimit,
                            selectedUtcDateMillis = selectedEndDate?.let {
                                localDateToUtcMillis(
                                    localDate = it
                                )
                            }
                        )
                    }
                }
                showDatePickerDialog(datePickerDialogConfig = datePickerDialogConfig)
            }

            is Event.OnStartDateSelected -> {
                val datePickerSelectionData =
                    viewState.value.snapshotFilterDateRangeSelectionUi.copy(
                        startDate = utcMillisToLocalDate(
                            utcMillis = event.selectedDateUtcMillis
                        )
                    )

                setState {
                    copy(
                        snapshotFilterDateRangeSelectionUi = datePickerSelectionData
                    )
                }

                updateDateRangeFilter(
                    lowerLimit = datePickerSelectionData.startDate ?: LocalDate.MIN,
                    upperLimit = datePickerSelectionData.endDate ?: LocalDate.MAX,
                )
            }

            is Event.OnEndDateSelected -> {
                val datePickerSelectionData =
                    viewState.value.snapshotFilterDateRangeSelectionUi.copy(
                        endDate = utcMillisToLocalDate(
                            utcMillis = event.selectedDateUtcMillis
                        )
                    )

                setState {
                    copy(
                        snapshotFilterDateRangeSelectionUi = datePickerSelectionData
                    )
                }

                updateDateRangeFilter(
                    lowerLimit = datePickerSelectionData.startDate ?: LocalDate.MIN,
                    upperLimit = datePickerSelectionData.endDate ?: LocalDate.MAX,
                )
            }

            is Event.DatePickerDialog.UpdateDialogState -> {
                setState {
                    copy(isDatePickerDialogVisible = event.isVisible)
                }
            }
        }
    }

    private fun applySelectedFilters() {
        interactor.applyFilters()
        setState {
            copy(
                shouldRevertFilterChanges = false,
                filterDateRangeSelectionUi = filterDateRangeSelectionUi.copy(
                    startDate = snapshotFilterDateRangeSelectionUi.startDate,
                    endDate = snapshotFilterDateRangeSelectionUi.endDate
                ),
                snapshotFilterDateRangeSelectionUi = FilterDateRangeSelectionUi()
                // reset snapshot to default date range (with null long values)
            )
        }
        hideBottomSheet()
    }

    private fun updateFilter(filterId: String, groupId: String) {
        setState { copy(shouldRevertFilterChanges = true) }
        interactor.updateFilter(filterGroupId = groupId, filterId = filterId)
    }

    private fun updateDateRangeFilter(
        groupId: String = TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_GROUP_ID,
        filterId: String = TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_RANGE,
        lowerLimit: LocalDate,
        upperLimit: LocalDate
    ) {
        setState { copy(shouldRevertFilterChanges = true) }

        interactor.updateDateFilterById(
            filterGroupId = groupId,
            filterId = filterId,
            lowerLimitDate = lowerLimit.toLocalDateTime(),
            upperLimitDate = upperLimit.toLocalDateTime(),
        )
    }

    private fun revertFilters(isOpening: Boolean) {
        if (viewState.value.sheetContent is TransactionsBottomSheetContent.Filters
            && !isOpening
            && viewState.value.shouldRevertFilterChanges
        ) {
            interactor.revertFilters()
            setState {
                copy(
                    shouldRevertFilterChanges = true,
                    snapshotFilterDateRangeSelectionUi = FilterDateRangeSelectionUi()
                )
            }
        }

        setState {
            copy(isBottomSheetOpen = isOpening)
        }
    }

    private fun resetFilters() {
        setState {
            copy(
                filterDateRangeSelectionUi = datePickerLimits,
                snapshotFilterDateRangeSelectionUi = FilterDateRangeSelectionUi()
            )
        }
        interactor.resetFilters()
        hideBottomSheet()
    }

    private fun showDatePickerDialog(
        datePickerDialogConfig: DatePickerDialogConfig
    ) {
        setState {
            copy(datePickerDialogConfig = datePickerDialogConfig)
        }
        setEffect {
            Effect.ShowDatePickerDialog
        }
    }

    private fun showBottomSheet(sheetContent: TransactionsBottomSheetContent) {
        setState {
            copy(
                sheetContent = sheetContent,
                snapshotFilterDateRangeSelectionUi = if (filterDateRangeSelectionUi.isEmpty) {
                    datePickerLimits
                } else {
                    filterDateRangeSelectionUi
                }
            )
        }
        setEffect {
            Effect.ShowBottomSheet
        }
    }

    private fun hideBottomSheet() {
        setEffect {
            Effect.CloseBottomSheet
        }
    }

    private fun onTransactionItemClicked(itemId: String) {
        goToTransactionDetails(transactionId = itemId)
    }

    private fun goToTransactionDetails(transactionId: String) {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = generateComposableNavigationLink(
                    screen = DashboardScreens.TransactionDetails,
                    arguments = generateComposableArguments(
                        mapOf(
                            "transactionId" to transactionId
                        )
                    )
                )
            )
        }
    }

    private fun applySearch(queryText: String) {
        interactor.applySearch(queryText)
        setState {
            copy(searchText = queryText)
        }
    }

    private fun getTransactions(event: Event) {
        setState {
            copy(
                isLoading = transactionsUi.isEmpty(),
                error = null
            )
        }

        viewModelScope.launch {
            interactor.getTransactions().collect { response ->
                when (response) {
                    is TransactionInteractorGetTransactionsPartialState.Success -> {

                        if (viewState.value.isFromOnPause) {
                            interactor.initializeFilters(filterableList = response.allTransactions)
                        } else {
                            interactor.updateLists(filterableList = response.allTransactions)
                        }

                        interactor.applyFilters()

                        val oldDatePickerAllowedLimits = viewState.value.datePickerLimits
                        val newDatePickerAllowedLimits = FilterDateRangeSelectionUi(
                            startDate = response.availableDates?.first,
                            endDate = response.availableDates?.second
                        )
                        val hasNewLimits = newDatePickerAllowedLimits != oldDatePickerAllowedLimits

                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                isFromOnPause = false,
                                datePickerLimits = newDatePickerAllowedLimits,
                                filterDateRangeSelectionUi = resolveUpdatedFilterDateRange(
                                    hasNewLimits = hasNewLimits,
                                    currentFilterDateRangeSelectionUi = filterDateRangeSelectionUi,
                                    oldLimits = oldDatePickerAllowedLimits,
                                    newLimits = newDatePickerAllowedLimits
                                )
                            )
                        }
                    }

                    is TransactionInteractorGetTransactionsPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = ContentErrorConfig(
                                    onRetry = { setEvent(event) },
                                    errorSubTitle = response.error,
                                    onCancel = {
                                        setState { copy(error = null) }
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun resolveUpdatedFilterDateRange(
        hasNewLimits: Boolean,
        currentFilterDateRangeSelectionUi: FilterDateRangeSelectionUi,
        oldLimits: FilterDateRangeSelectionUi,
        newLimits: FilterDateRangeSelectionUi
    ): FilterDateRangeSelectionUi {
        return when {
            hasNewLimits && currentFilterDateRangeSelectionUi.isEmpty -> newLimits
            hasNewLimits && currentFilterDateRangeSelectionUi == oldLimits -> newLimits
            hasNewLimits -> currentFilterDateRangeSelectionUi
            !currentFilterDateRangeSelectionUi.isEmpty -> currentFilterDateRangeSelectionUi
            else -> FilterDateRangeSelectionUi(startDate = null, endDate = null)
        }
    }

    private fun collectSearchAndFilterStateChanges() {
        viewModelScope.launch {
            interactor.onFilterStateChange().collect { result ->
                when (result) {
                    is TransactionInteractorFilterPartialState.FilterApplyResult -> {
                        val isDefaultFilterDateRangeSelected = with(viewState.value) {
                            (!datePickerLimits.isEmpty && filterDateRangeSelectionUi == datePickerLimits)
                                    || (datePickerLimits.isEmpty && filterDateRangeSelectionUi.isEmpty)
                        }
                        setState {
                            copy(
                                isFilteringActive = !result.allDefaultFiltersAreSelected || !isDefaultFilterDateRangeSelected,
                                transactionsUi = result.transactions,
                                showNoResultsFound = result.transactions.isEmpty(),
                                filtersUi = result.filters,
                                sortOrder = sortOrder.copy(
                                    selectedButton = when (result.sortOrder) {
                                        is SortOrder.Descending -> DualSelectorButton.FIRST
                                        is SortOrder.Ascending -> DualSelectorButton.SECOND
                                    }
                                )
                            )
                        }
                    }

                    is TransactionInteractorFilterPartialState.FilterUpdateResult -> {
                        setState {
                            copy(
                                filtersUi = result.filters,
                                sortOrder = sortOrder.copy(
                                    selectedButton = when (result.sortOrder) {
                                        is SortOrder.Descending -> DualSelectorButton.FIRST
                                        is SortOrder.Ascending -> DualSelectorButton.SECOND
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun sortOrderChanged(orderButton: DualSelectorButton) {
        val sortOrder = when (orderButton) {
            DualSelectorButton.FIRST -> SortOrder.Descending(isDefault = true)
            DualSelectorButton.SECOND -> SortOrder.Ascending()
        }
        setState { copy(shouldRevertFilterChanges = true) }
        interactor.updateSortOrder(sortOrder)
    }
}