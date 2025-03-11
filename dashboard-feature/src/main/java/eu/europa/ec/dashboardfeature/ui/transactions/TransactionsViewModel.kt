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

import androidx.lifecycle.viewModelScope
import eu.europa.ec.businesslogic.util.toDisplayedDate
import eu.europa.ec.businesslogic.util.toLocalDate
import eu.europa.ec.businesslogic.validator.model.SortOrder
import eu.europa.ec.corelogic.model.TransactionCategory
import eu.europa.ec.dashboardfeature.interactor.TransactionInteractorFilterPartialState
import eu.europa.ec.dashboardfeature.interactor.TransactionInteractorGetTransactionsPartialState
import eu.europa.ec.dashboardfeature.interactor.TransactionsInteractor
import eu.europa.ec.dashboardfeature.model.TransactionFilterIds
import eu.europa.ec.dashboardfeature.model.TransactionUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.DualSelectorButton
import eu.europa.ec.uilogic.component.DualSelectorButtonData
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemData
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

data class State(
    val isLoading: Boolean,
    val error: ContentErrorConfig? = null,

    val searchText: String = "",
    val isFilteringActive: Boolean,
    val showNoResultsFound: Boolean = false,
    val isBottomSheetOpen: Boolean = false,
    val sheetContent: TransactionsBottomSheetContent = TransactionsBottomSheetContent.Filters(
        filters = emptyList()
    ),

    val transactionsUi: List<Pair<TransactionCategory, List<TransactionUi>>> = emptyList(),
    val filtersUi: List<ExpandableListItemData> = emptyList(),
    val shouldRevertFilterChanges: Boolean = true,
    val sortOrder: DualSelectorButtonData,

    val filterDateRangeSelectionData: FilterDateRangeSelectionData = FilterDateRangeSelectionData(),
    val isDatePickerDialogVisible: Boolean = false,
    val datePickerDialogConfig: DatePickerDialogConfig = DatePickerDialogConfig(
        type = DatePickerDialogType.SelectStartDate
    )
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
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
    data class OnStartDateSelected(val selectedDateMillis: Long) : Event()
    data class OnEndDateSelected(val selectedDateMillis: Long) : Event()
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
    data object ResumeOnApplyFilter : Effect()
}

sealed class TransactionsBottomSheetContent {
    data class Filters(val filters: List<ExpandableListItemData>) : TransactionsBottomSheetContent()
}

enum class DatePickerDialogType {
    SelectStartDate, SelectEndDate
}

data class DatePickerDialogConfig(
    val type: DatePickerDialogType,
    val lowerLimit: LocalDate? = LocalDate.MIN,
    val upperLimit: LocalDate? = LocalDate.MAX
)

data class FilterDateRangeSelectionData(
    val startDate: Long? = null,
    val endDate: Long? = null
) {
    val displayedStartDate: String
        get() = startDate?.toDisplayedDate().orEmpty()

    val displayedEndDate: String
        get() = endDate?.toDisplayedDate().orEmpty()
}

@KoinViewModel
class TransactionsViewModel(
    private val interactor: TransactionsInteractor,
    private val resourceProvider: ResourceProvider
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State {
        return State(
            isLoading = false,
            sortOrder = DualSelectorButtonData(
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
                getTransactions()
            }

            is Event.FiltersPressed -> {
                showBottomSheet(
                    sheetContent = TransactionsBottomSheetContent.Filters(
                        filters = viewState.value.filtersUi
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
                if (viewState.value.sheetContent is TransactionsBottomSheetContent.Filters
                    && !event.isOpen
                ) {
                    setEffect { Effect.ResumeOnApplyFilter }
                }
                revertFilters(event.isOpen)
            }

            is Event.Pop -> setEffect { Effect.Navigation.Pop }
            is Event.BottomSheet.Close -> {
                hideBottomSheet()
            }

            is Event.ShowDatePicker -> {
                val givenStartDate = viewState.value.filterDateRangeSelectionData.startDate
                val minStartDate = givenStartDate?.toLocalDate() ?: LocalDate.MIN

                val givenEndDate = viewState.value.filterDateRangeSelectionData.endDate
                val maxEndDate = givenEndDate?.toLocalDate() ?: LocalDate.MAX

                val datePickerDialogConfig = when (event.datePickerType) {
                    DatePickerDialogType.SelectStartDate -> DatePickerDialogConfig(
                        type = DatePickerDialogType.SelectStartDate,
                        upperLimit = maxEndDate
                    )

                    DatePickerDialogType.SelectEndDate -> {
                        DatePickerDialogConfig(
                            type = DatePickerDialogType.SelectEndDate,
                            lowerLimit = minStartDate
                        )
                    }
                }
                showDatePickerDialog(datePickerDialogConfig = datePickerDialogConfig)
            }

            is Event.OnStartDateSelected -> {
                val datePickerSelectionData = viewState.value.filterDateRangeSelectionData.copy(
                    startDate = event.selectedDateMillis,
                )
                setState {
                    copy(
                        filterDateRangeSelectionData = datePickerSelectionData
                    )
                }

                viewState.value.filterDateRangeSelectionData.let {
                    updateDateRangeFilter(
                        filterId = TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_RANGE,
                        groupId = TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_GROUP_ID,
                        lowerLimit = it.startDate ?: Long.MIN_VALUE,
                        upperLimit = it.endDate ?: Long.MAX_VALUE,
                    )
                }
            }

            is Event.OnEndDateSelected -> {
                val datePickerSelectionData = viewState.value.filterDateRangeSelectionData.copy(
                    endDate = event.selectedDateMillis
                )
                setState {
                    copy(
                        filterDateRangeSelectionData = datePickerSelectionData
                    )
                }
                viewState.value.filterDateRangeSelectionData.let {
                    updateDateRangeFilter(
                        filterId = TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_RANGE,
                        groupId = TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_GROUP_ID,
                        lowerLimit = it.startDate ?: Long.MIN_VALUE,
                        upperLimit = it.endDate ?: Long.MAX_VALUE,
                    )
                }
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
                shouldRevertFilterChanges = false
            )
        }
        hideBottomSheet()
    }

    private fun updateFilter(filterId: String, groupId: String) {
        setState { copy(shouldRevertFilterChanges = true) }
        interactor.updateFilter(filterGroupId = groupId, filterId = filterId)
    }


    private fun updateDateRangeFilter(
        filterId: String,
        groupId: String,
        lowerLimit: Long,
        upperLimit: Long
    ) {
        setState { copy(shouldRevertFilterChanges = true) }

        interactor.updateDateFilterById(
            filterGroupId = groupId,
            filterId = filterId,
            lowerLimitDate = Instant.ofEpochMilli(lowerLimit),
            upperLimitDate = Instant.ofEpochMilli(upperLimit)
        )
    }

    private fun revertFilters(isOpening: Boolean) {
        if (viewState.value.sheetContent is TransactionsBottomSheetContent.Filters
            && !isOpening
            && viewState.value.shouldRevertFilterChanges
        ) {
            interactor.revertFilters()
            setState { copy(shouldRevertFilterChanges = true) }
        }

        setState {
            copy(isBottomSheetOpen = isOpening)
        }
    }

    private fun resetFilters() {
        setState {
            copy(filterDateRangeSelectionData = FilterDateRangeSelectionData())
        }
        interactor.resetFilters()
        hideBottomSheet()
    }

    private fun showDatePickerDialog(datePickerDialogConfig: DatePickerDialogConfig) {
        setState {
            copy(datePickerDialogConfig = datePickerDialogConfig)
        }
        setEffect {
            Effect.ShowDatePickerDialog
        }
    }

    private fun showBottomSheet(sheetContent: TransactionsBottomSheetContent) {
        setState {
            copy(sheetContent = sheetContent)
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

    private fun getTransactions() {
        viewModelScope.launch {
            interactor.getTransactions().collect { response ->
                when (response) {
                    is TransactionInteractorGetTransactionsPartialState.Success -> {
                        val sortOrder = SortOrder.Descending()
                            .takeIf { viewState.value.sortOrder.selectedButton == DualSelectorButton.FIRST }
                            ?: SortOrder.Ascending()

                        val groupedItems =
                            response.allTransactions.items.map { filterableTransactionItem ->
                                filterableTransactionItem.payload as TransactionUi
                            }.groupByCategory(sortOrder)

                        interactor.initializeFilters(filterableList = response.allTransactions)
                        interactor.applyFilters()

                        setState {
                            copy(
                                isLoading = transactionsUi.isEmpty(),
                                error = null,
                                transactionsUi = groupedItems
                            )
                        }
                    }

                    is TransactionInteractorGetTransactionsPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = ContentErrorConfig(
                                    errorSubTitle = response.error,
                                    onCancel = {
                                        setState {
                                            copy(error = null)
                                        }
                                        setEvent(Event.Pop)
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun collectSearchAndFilterStateChanges() {
        viewModelScope.launch {
            interactor.onFilterStateChange().collect { result ->
                when (result) {
                    is TransactionInteractorFilterPartialState.FilterApplyResult -> {
                        val transactionsUiWithCategoryItemsSorted =
                            result.transactions.sortCategoryItems(result.sortOrder)
                        setState {
                            copy(
                                isFilteringActive = !result.allDefaultFiltersAreSelected,
                                transactionsUi = transactionsUiWithCategoryItemsSorted,
                                showNoResultsFound = result.transactions.isEmpty(),
                                filtersUi = result.filters,
                                sortOrder = sortOrder.copy(selectedButton = result.sortOrder)
                            )
                        }
                    }

                    is TransactionInteractorFilterPartialState.FilterUpdateResult -> {
                        setState {
                            copy(
                                filtersUi = result.filters,
                                sortOrder = sortOrder.copy(selectedButton = result.sortOrder)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun List<TransactionUi>.groupByCategory(
        sortOrder: SortOrder
    ): List<Pair<TransactionCategory, List<TransactionUi>>> {
        val groupedTransactions = groupBy { transactionUi ->
            when (val category = transactionUi.transactionCategory) {
                is TransactionCategory.Today -> category
                is TransactionCategory.ThisWeek -> category
                is TransactionCategory.Month -> {
                    val dateTime = category.dateRange?.start
                    TransactionCategory.Month(dateTime ?: LocalDateTime.now())
                }

                else -> category
            }
        }.toList()

        return groupedTransactions
            .map { (category, transactions) ->
                // Sort transactions within each category
                category to when (sortOrder) {
                    is SortOrder.Descending -> transactions.sortedByDescending { it.uiData.supportingText }
                    is SortOrder.Ascending -> transactions.sortedBy { it.uiData.supportingText }

                }
            }.sortByOrder(sortOrder = sortOrder) { (category, _) ->
                category.order
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

    private fun <T> List<T>.sortByOrder(
        sortOrder: SortOrder,
        selector: (T) -> Int
    ): List<T> {
        return when (sortOrder) {
            is SortOrder.Ascending -> this.sortedBy(selector)
            is SortOrder.Descending -> this.sortedByDescending(selector)
        }
    }

    private fun List<Pair<TransactionCategory, List<TransactionUi>>>.sortCategoryItems(
        sortOrder: DualSelectorButton
    ): List<Pair<TransactionCategory, List<TransactionUi>>> {
        return this.map { (category, transactionList) ->
            val sortedTransactions = when (sortOrder) {
                DualSelectorButton.FIRST -> transactionList.sortedByDescending { it.uiData.supportingText }
                DualSelectorButton.SECOND -> transactionList.sortedBy { it.uiData.supportingText }
            }
            category to sortedTransactions
        }
    }
}