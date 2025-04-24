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
import eu.europa.ec.businesslogic.util.toLocalDate
import eu.europa.ec.businesslogic.validator.model.SortOrder
import eu.europa.ec.corelogic.model.TransactionCategory
import eu.europa.ec.dashboardfeature.interactor.TransactionInteractorFilterPartialState
import eu.europa.ec.dashboardfeature.interactor.TransactionInteractorGetTransactionsPartialState
import eu.europa.ec.dashboardfeature.interactor.TransactionsInteractor
import eu.europa.ec.dashboardfeature.model.FilterDateRangeSelectionData
import eu.europa.ec.dashboardfeature.model.TransactionFilterIds
import eu.europa.ec.dashboardfeature.model.TransactionUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.DatePickerDialogConfig
import eu.europa.ec.uilogic.component.DatePickerDialogType
import eu.europa.ec.uilogic.component.DualSelectorButton
import eu.europa.ec.uilogic.component.DualSelectorButtonData
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import java.time.Instant
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

    val transactionsUi: List<Pair<TransactionCategory, List<TransactionUi>>> = emptyList(),
    val filtersUi: List<ExpandableListItem.NestedListItemData> = emptyList(),
    val shouldRevertFilterChanges: Boolean = true,
    val sortOrder: DualSelectorButtonData,
    val isDatePickerDialogVisible: Boolean = false,
    val datePickerDialogConfig: DatePickerDialogConfig = DatePickerDialogConfig(
        type = DatePickerDialogType.SelectStartDate
    ),
    // persisted selected date range applied for filtering
    val filterDateRangeSelectionData: FilterDateRangeSelectionData = FilterDateRangeSelectionData(),
    // temporary date range data while filter bottom sheet is open to collect date picker dialog selections
    val snapshotFilterDateRangeSelectionData: FilterDateRangeSelectionData = FilterDateRangeSelectionData(),
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
}

sealed class TransactionsBottomSheetContent {
    data class Filters(val filters: List<ExpandableListItem.SingleListItemData>) :
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
                val appliedStartDate =
                    viewState.value.snapshotFilterDateRangeSelectionData.startDate
                val minStartDate = appliedStartDate?.toLocalDate() ?: LocalDate.MIN

                val appliedEndDate = viewState.value.snapshotFilterDateRangeSelectionData.endDate
                val maxEndDate = appliedEndDate?.toLocalDate() ?: LocalDate.MAX

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
                val datePickerSelectionData =
                    viewState.value.snapshotFilterDateRangeSelectionData.copy(
                        startDate = event.selectedDateMillis,
                    )
                setState {
                    copy(
                        snapshotFilterDateRangeSelectionData = datePickerSelectionData
                    )
                }

                updateDateRangeFilter(
                    lowerLimit = datePickerSelectionData.startDate ?: Long.MIN_VALUE,
                    upperLimit = datePickerSelectionData.endDate ?: Long.MAX_VALUE,
                )
            }

            is Event.OnEndDateSelected -> {
                val datePickerSelectionData =
                    viewState.value.snapshotFilterDateRangeSelectionData.copy(
                        endDate = event.selectedDateMillis,
                    )
                setState {
                    copy(
                        snapshotFilterDateRangeSelectionData = datePickerSelectionData
                    )
                }
                updateDateRangeFilter(
                    lowerLimit = datePickerSelectionData.startDate ?: Long.MIN_VALUE,
                    upperLimit = datePickerSelectionData.endDate ?: Long.MAX_VALUE,
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
                filterDateRangeSelectionData = filterDateRangeSelectionData.copy(
                    startDate = snapshotFilterDateRangeSelectionData.startDate,
                    endDate = snapshotFilterDateRangeSelectionData.endDate
                ),
                snapshotFilterDateRangeSelectionData = FilterDateRangeSelectionData()
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
            setState {
                copy(
                    shouldRevertFilterChanges = true,
                    snapshotFilterDateRangeSelectionData = FilterDateRangeSelectionData()
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
                filterDateRangeSelectionData = FilterDateRangeSelectionData()
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
                snapshotFilterDateRangeSelectionData = filterDateRangeSelectionData
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
                    screen = IssuanceScreens.TransactionDetails,
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

                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                isFromOnPause = false
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

    private fun collectSearchAndFilterStateChanges() {
        viewModelScope.launch {
            interactor.onFilterStateChange().collect { result ->
                when (result) {
                    is TransactionInteractorFilterPartialState.FilterApplyResult -> {
                        val isDefaultFilterDateRangeSelected =
                            with(viewState.value.filterDateRangeSelectionData) { startDate == null && endDate == null }
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