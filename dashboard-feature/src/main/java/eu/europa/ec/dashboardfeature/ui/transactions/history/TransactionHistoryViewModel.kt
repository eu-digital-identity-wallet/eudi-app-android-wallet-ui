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

package eu.europa.ec.dashboardfeature.ui.transactions.history

import androidx.lifecycle.viewModelScope
import eu.europa.ec.dashboardfeature.interactor.TransactionHistoryInteractor
import eu.europa.ec.dashboardfeature.interactor.TransactionHistoryInteractorPartialState
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDataProtectionAction
import eu.europa.ec.dashboardfeature.ui.transactions.history.model.TransactionHistoryUi
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

data class State(
    val isLoading: Boolean = false,
    val error: ContentErrorConfig? = null,
    val action: TransactionDataProtectionAction? = null,
    val history: TransactionHistoryUi? = null,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object Pop : Event()
}

sealed class Effect : ViewSideEffect {
    data object Pop : Effect()
    data object PopToDashboard : Effect()
}

@KoinViewModel
internal class TransactionHistoryViewModel(
    private val interactor: TransactionHistoryInteractor,
    @InjectedParam private val transactionId: String,
    @InjectedParam private val actionType: String,
) : MviViewModel<Event, State, Effect>() {

    private var historyJob: Job? = null

    override fun setInitialState(): State = State(
        action = TransactionDataProtectionAction.entries.firstOrNull { action ->
            action.name == actionType
        },
    )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> observeHistory()

            is Event.Pop -> {
                historyJob?.cancel()
                setEffect { Effect.Pop }
            }
        }
    }

    private fun observeHistory() {
        historyJob?.cancel()
        val action = viewState.value.action
        if (action == null || transactionId.isBlank()) {
            setEffect { Effect.Pop }
            return
        }

        historyJob = viewModelScope.launch {
            interactor.observeHistory(
                presentationId = transactionId,
                action = action,
            ).collect { response ->
                when (response) {
                    is TransactionHistoryInteractorPartialState.Loading -> {
                        setState {
                            copy(
                                isLoading = true,
                                error = null,
                                history = null
                            )
                        }
                    }

                    is TransactionHistoryInteractorPartialState.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                history = response.history
                            )
                        }
                    }

                    is TransactionHistoryInteractorPartialState.ParentNotFound -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                history = null
                            )
                        }
                        setEffect { Effect.PopToDashboard }
                        historyJob?.cancel()
                    }

                    is TransactionHistoryInteractorPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                history = null,
                                error = ContentErrorConfig(
                                    errorSubTitle = response.errorMessage,
                                    onRetry = { setEvent(Event.Init) },
                                    onCancel = { setEvent(Event.Pop) },
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}