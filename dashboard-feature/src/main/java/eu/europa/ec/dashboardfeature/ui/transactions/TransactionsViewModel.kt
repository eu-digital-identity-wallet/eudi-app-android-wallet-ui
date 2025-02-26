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
import eu.europa.ec.corelogic.model.TransactionCategory
import eu.europa.ec.dashboardfeature.interactor.TransactionInteractorGetTransactionsPartialState
import eu.europa.ec.dashboardfeature.interactor.TransactionsInteractor
import eu.europa.ec.dashboardfeature.model.TransactionUi
import eu.europa.ec.uilogic.component.DualSelectorButton
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import java.time.LocalDateTime

data class State(
    val isLoading: Boolean,
    val error: ContentErrorConfig? = null,

    val searchText: String = "",
    val isFilteringActive: Boolean,
    val showNoResultsFound: Boolean = false,
    val isBottomSheetOpen: Boolean = false,

    val transactionsUi: List<Pair<TransactionCategory, List<TransactionUi>>> = emptyList()
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
}

@KoinViewModel
class TransactionsViewModel(
    private val interactor: TransactionsInteractor,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State {
        return State(
            isLoading = false,
            isFilteringActive = false
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                getTransactions()
            }

            is Event.FiltersPressed -> {}
            is Event.OnFilterSelectionChanged -> {}
            is Event.OnFiltersApply -> {}
            is Event.OnFiltersReset -> {}
            is Event.OnSearchQueryChanged -> {
                applySearch(event.query)
            }

            is Event.OnSortingOrderChanged -> {}
            is Event.TransactionItemPressed -> {
                onTransactionItemClicked(itemId = event.itemId)
            }

            is Event.Pop -> setEffect { Effect.Navigation.Pop }
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
        setState {
            copy(searchText = queryText)
        }
    }

    private fun getTransactions() {
        viewModelScope.launch {
            interactor.getTransactions().collect { response ->
                when (response) {
                    is TransactionInteractorGetTransactionsPartialState.Success -> {
                        val groupedItems =
                            response.allTransactions.items.map { filterableTransactionItem ->
                                filterableTransactionItem.payload as TransactionUi
                            }.groupByCategory()

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

    private fun List<TransactionUi>.groupByCategory(): List<Pair<TransactionCategory, List<TransactionUi>>> {
        return groupBy { transactionUi ->
            when (val category = transactionUi.transactionCategory) {
                is TransactionCategory.Today -> category
                is TransactionCategory.ThisWeek -> category
                is TransactionCategory.Month -> {
                    val dateTime = category.dateRange?.start
                    TransactionCategory.Month(dateTime ?: LocalDateTime.now())
                }

                else -> category
            }
        }.toList().sortedBy { (category, _) -> category.order }
    }
}