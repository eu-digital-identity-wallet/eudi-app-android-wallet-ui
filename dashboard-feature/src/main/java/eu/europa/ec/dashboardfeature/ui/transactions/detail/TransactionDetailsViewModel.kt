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

package eu.europa.ec.dashboardfeature.ui.transactions.detail

import androidx.lifecycle.viewModelScope
import eu.europa.ec.dashboardfeature.interactor.TransactionDetailsInteractor
import eu.europa.ec.dashboardfeature.interactor.TransactionDetailsInteractorPartialState
import eu.europa.ec.dashboardfeature.model.TransactionDetailsUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.extension.toggleExpansionState
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

data class State(
    val isLoading: Boolean = false,
    val error: ContentErrorConfig? = null,

    val title: String,
    val transactionDetailsUi: TransactionDetailsUi? = null,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object Pop : Event()
    data object DismissError : Event()

    data class ExpandOrCollapseGroupItem(val itemId: String) : Event()

    data object RequestDataDeletionPressed : Event()
    data object ReportSuspiciousTransactionPressed : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
        data class SwitchScreen(
            val screenRoute: String,
            val popUpToScreenRoute: String,
            val inclusive: Boolean
        ) : Navigation()
    }
}

@KoinViewModel
internal class TransactionDetailsViewModel(
    private val interactor: TransactionDetailsInteractor,
    private val resourceProvider: ResourceProvider,
    @InjectedParam private val transactionId: String,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State = State(
        title = resourceProvider.getString(R.string.transaction_details_screen_title),
    )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                getTransactionDetails(event)
            }

            is Event.DismissError -> {
                setState { copy(error = null) }
            }

            is Event.Pop -> {
                setState { copy(error = null) }
                setEffect { Effect.Navigation.Pop }
            }

            is Event.RequestDataDeletionPressed -> {
                viewModelScope.launch {
                    interactor.requestDataDeletion(transactionId = transactionId).collect()
                }
            }

            is Event.ReportSuspiciousTransactionPressed -> {
                viewModelScope.launch {
                    interactor.reportSuspiciousTransaction(transactionId = transactionId).collect()
                }
            }

            is Event.ExpandOrCollapseGroupItem -> {
                expandOrCollapseGroupItem(event.itemId)
            }
        }
    }

    private fun getTransactionDetails(event: Event) {
        setState {
            copy(
                isLoading = false,
                error = null
            )
        }

        viewModelScope.launch {
            interactor.getTransactionDetails(
                transactionId = transactionId,
            ).collect { response ->
                when (response) {
                    is TransactionDetailsInteractorPartialState.Success -> {
                        val transactionDetailsUi = response.transactionDetailsUi
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                transactionDetailsUi = transactionDetailsUi
                            )
                        }
                    }

                    is TransactionDetailsInteractorPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = ContentErrorConfig(
                                    onRetry = { setEvent(event) },
                                    errorSubTitle = response.error,
                                    onCancel = { setEvent(Event.Pop) }
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun expandOrCollapseGroupItem(itemId: String) {
        viewState.value.transactionDetailsUi?.let { safeTransactionDetailsUi ->

            val updatedItems =
                safeTransactionDetailsUi.transactionDetailsDataShared.dataSharedItems.map { dataSharedItem ->
                    val newHeader = if (dataSharedItem.header.itemId == itemId) {
                        val newIsExpanded = !dataSharedItem.isExpanded
                        val newCollapsed = dataSharedItem.header.copy(
                            trailingContentData = ListItemTrailingContentData.Icon(
                                iconData = if (newIsExpanded) {
                                    AppIcons.KeyboardArrowUp
                                } else {
                                    AppIcons.KeyboardArrowDown
                                }
                            )
                        )

                        dataSharedItem.copy(
                            header = newCollapsed,
                            isExpanded = newIsExpanded
                        )
                    } else {
                        dataSharedItem
                    }

                    dataSharedItem.copy(
                        header = newHeader.header,
                        isExpanded = newHeader.isExpanded,
                        nestedItems = newHeader.nestedItems.toggleExpansionState(itemId)
                    )
                }

            setState {
                copy(
                    transactionDetailsUi = safeTransactionDetailsUi.copy(
                        transactionDetailsDataShared = safeTransactionDetailsUi.transactionDetailsDataShared.copy(
                            dataSharedItems = updatedItems
                        )
                    )
                )
            }
        }
    }

}