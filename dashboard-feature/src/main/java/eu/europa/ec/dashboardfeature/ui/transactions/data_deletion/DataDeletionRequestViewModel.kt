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

package eu.europa.ec.dashboardfeature.ui.transactions.data_deletion

import androidx.lifecycle.viewModelScope
import eu.europa.ec.corelogic.controller.RecordTransactionPartialState
import eu.europa.ec.dashboardfeature.interactor.TransactionDetailsInteractor
import eu.europa.ec.dashboardfeature.interactor.TransactionDetailsInteractorDataDeletionPartialState
import eu.europa.ec.dashboardfeature.interactor.TransactionDetailsInteractorDataProtectionPartialState
import eu.europa.ec.dashboardfeature.ui.transactions.data_deletion.model.DataDeletionRequestUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.PendingTransactionActionUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDataProtectionAction
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import java.time.Instant

data class State(
    val isLoading: Boolean = false,
    val isPerformingAction: Boolean = false,
    val error: ContentErrorConfig? = null,
    val request: DataDeletionRequestUi? = null,
    val pendingAction: PendingTransactionActionUi? = null,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object Pop : Event()
    data object Continue : Event()
    data class ChannelOpened(val actionId: String, val opened: Boolean) : Event()
    data class RetryLaunch(val actionId: String) : Event()
    data class RetrySave(val actionId: String) : Event()
}

sealed class Effect : ViewSideEffect {
    data object Pop : Effect()
    data class OpenChannel(val actionId: String, val url: String) : Effect()
}

@KoinViewModel
internal class DataDeletionRequestViewModel(
    private val interactor: TransactionDetailsInteractor,
    private val resourceProvider: ResourceProvider,
    @InjectedParam private val transactionId: String,
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State = State()

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> loadRequest()

            is Event.Pop -> {
                if (viewState.value.isPerformingAction) return
                setEffect { Effect.Pop }
            }

            is Event.Continue -> {
                if (viewState.value.error != null) return
                val request = viewState.value.request ?: return
                prepareAction(contactUrl = request.contactUrl)
            }

            is Event.ChannelOpened -> {
                val pendingAction = viewState.value.pendingAction ?: return
                if (event.actionId != pendingAction.id || !viewState.value.isPerformingAction ||
                    pendingAction.launchedAt != null
                ) return
                if (event.opened) {
                    saveAction(pendingAction.copy(launchedAt = Instant.now()))
                } else {
                    setState {
                        copy(
                            isLoading = false,
                            isPerformingAction = false,
                            error = ContentErrorConfig(
                                errorSubTitle = resourceProvider.getString(R.string.transaction_details_action_open_failed),
                                onRetry = { setEvent(Event.RetryLaunch(pendingAction.id)) },
                                onCancel = { setEvent(Event.Pop) },
                            ),
                        )
                    }
                }
            }

            is Event.RetryLaunch -> {
                val pendingAction = viewState.value.pendingAction ?: return
                if (pendingAction.id != event.actionId || pendingAction.launchedAt != null) return
                prepareAction(contactUrl = pendingAction.contactUrl)
            }

            is Event.RetrySave -> {
                if (viewState.value.isLoading) return
                val pendingAction = viewState.value.pendingAction ?: return
                if (pendingAction.id != event.actionId || pendingAction.launchedAt == null) return
                saveAction(pendingAction)
            }
        }
    }

    private fun loadRequest() {
        if (viewState.value.isLoading) return
        setState {
            copy(
                isLoading = true,
                error = null,
                request = null,
                pendingAction = null
            )
        }

        viewModelScope.launch {
            interactor.getDataDeletionRequest(transactionId = transactionId).collect { response ->
                when (response) {
                    is TransactionDetailsInteractorDataDeletionPartialState.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                request = response.request
                            )
                        }
                    }

                    is TransactionDetailsInteractorDataDeletionPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
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

    private fun prepareAction(contactUrl: String) {
        if (viewState.value.isLoading) return
        setState {
            copy(
                isLoading = true,
                isPerformingAction = true,
                error = null,
                pendingAction = null
            )
        }

        viewModelScope.launch {
            interactor.prepareDataProtectionAction(
                transactionId = transactionId,
                action = TransactionDataProtectionAction.RequestDataDeletion,
                contactUrl = contactUrl,
            ).collect { response ->
                when (response) {
                    is TransactionDetailsInteractorDataProtectionPartialState.Success -> {
                        setState { copy(pendingAction = response.pendingAction) }
                        setEffect {
                            Effect.OpenChannel(
                                actionId = response.pendingAction.id,
                                url = response.pendingAction.launchUrl,
                            )
                        }
                    }

                    is TransactionDetailsInteractorDataProtectionPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                isPerformingAction = false,
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

    private fun saveAction(pendingAction: PendingTransactionActionUi) {
        setState {
            copy(
                isLoading = true,
                isPerformingAction = true,
                error = null,
                pendingAction = pendingAction,
            )
        }

        viewModelScope.launch {
            interactor.recordDataProtectionAction(pendingAction).collect { response ->
                when (response) {
                    is RecordTransactionPartialState.Success -> {
                        setEffect { Effect.Pop }
                    }

                    is RecordTransactionPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                isPerformingAction = false,
                                error = ContentErrorConfig(
                                    errorSubTitle = resourceProvider.getString(R.string.transaction_details_action_save_failed),
                                    onRetry = { setEvent(Event.RetrySave(pendingAction.id)) },
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