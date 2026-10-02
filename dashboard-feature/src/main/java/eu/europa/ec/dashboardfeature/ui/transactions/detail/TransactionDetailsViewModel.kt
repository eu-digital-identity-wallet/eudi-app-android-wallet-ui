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

package eu.europa.ec.dashboardfeature.ui.transactions.detail

import android.net.Uri
import androidx.lifecycle.viewModelScope
import eu.europa.ec.dashboardfeature.interactor.TransactionDetailsInteractor
import eu.europa.ec.dashboardfeature.interactor.TransactionDetailsInteractorDeleteTransactionPartialState
import eu.europa.ec.dashboardfeature.interactor.TransactionDetailsInteractorPartialState
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.PresentationActionCountsUiState
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDataProtectionAction
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsBodyUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsSectionUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.extension.withExpansionIcon
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

data class State(
    val isLoading: Boolean = false,
    val error: ContentErrorConfig? = null,
    val isDeleting: Boolean = false,
    val isBottomSheetOpen: Boolean = false,
    val sheetContent: TransactionDetailsBottomSheetContent = TransactionDetailsBottomSheetContent.DeleteTransactionConfirmation,

    val title: String,
    val transactionDetailsUi: TransactionDetailsUi? = null,
    val expandedGroupIds: Set<String> = emptySet(),
    val isCardExpanded: Boolean = false,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object Pop : Event()
    data object DismissError : Event()
    data object DeletePressed : Event()

    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(val isOpen: Boolean) : BottomSheet()

        sealed class Delete : BottomSheet() {
            data object PrimaryButtonPressed : Delete()
            data object SecondaryButtonPressed : Delete()
        }
    }

    data class ExpandOrCollapseGroupItem(val itemId: String) : Event()
    data object ExpandOrCollapseCard : Event()
    data class OpenLink(val url: String) : Event()

    data object RequestDataDeletionPressed : Event()
    data object ReportSuspiciousTransactionPressed : Event()
    data class HistoryPressed(val action: TransactionDataProtectionAction) : Event()
    data object RetryPresentationActionCounts : Event()
}

sealed class Effect : ViewSideEffect {
    data object ShowBottomSheet : Effect()
    data object CloseBottomSheet : Effect()

    sealed class Navigation : Effect() {
        data object Pop : Navigation()
        data class OpenUrlExternally(val url: String) : Navigation()
        data class SwitchScreen(
            val screenRoute: String,
        ) : Navigation()
    }
}

sealed class TransactionDetailsBottomSheetContent {
    data object DeleteTransactionConfirmation : TransactionDetailsBottomSheetContent()
}

@KoinViewModel
internal class TransactionDetailsViewModel(
    private val interactor: TransactionDetailsInteractor,
    private val resourceProvider: ResourceProvider,
    @InjectedParam private val transactionId: String,
) : MviViewModel<Event, State, Effect>() {
    private var transactionDetailsJob: Job? = null
    private var presentationActionCountsJob: Job? = null

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
                if (viewState.value.isDeleting) return
                setState { copy(error = null) }
                setEffect { Effect.Navigation.Pop }
            }

            is Event.DeletePressed -> {
                if (viewState.value.isLoading || viewState.value.isDeleting ||
                    viewState.value.transactionDetailsUi == null
                ) return
                setState { copy(error = null) }
                showBottomSheet(
                    sheetContent = TransactionDetailsBottomSheetContent.DeleteTransactionConfirmation,
                )
            }

            is Event.BottomSheet.UpdateBottomSheetState -> {
                setState { copy(isBottomSheetOpen = event.isOpen) }
            }

            is Event.BottomSheet.Delete.PrimaryButtonPressed -> {
                if (!viewState.value.isBottomSheetOpen || viewState.value.error != null) return
                deleteTransaction()
            }

            is Event.BottomSheet.Delete.SecondaryButtonPressed -> {
                hideBottomSheet()
            }

            is Event.RequestDataDeletionPressed -> {
                if (viewState.value.isLoading) return

                val body = viewState.value.transactionDetailsUi?.body
                        as? TransactionDetailsBodyUi.Presentation ?: return

                if (body.deletionContacts.isEmpty()) return

                setState { copy(error = null) }
                setEffect {
                    Effect.Navigation.SwitchScreen(
                        screenRoute = generateComposableNavigationLink(
                            screen = DashboardScreens.DataDeletionRequest,
                            arguments = generateComposableArguments(
                                mapOf("transactionId" to Uri.encode(transactionId))
                            ),
                        )
                    )
                }
            }

            is Event.ReportSuspiciousTransactionPressed -> {
                if (viewState.value.isLoading) return

                val body = viewState.value.transactionDetailsUi?.body
                        as? TransactionDetailsBodyUi.Presentation ?: return

                if (body.reportContacts.isEmpty()) return

                setState { copy(error = null) }
                setEffect {
                    Effect.Navigation.SwitchScreen(
                        screenRoute = generateComposableNavigationLink(
                            screen = DashboardScreens.DpaReport,
                            arguments = generateComposableArguments(
                                mapOf("transactionId" to Uri.encode(transactionId))
                            ),
                        )
                    )
                }
            }

            is Event.HistoryPressed -> {
                if (viewState.value.isLoading) return
                val details = viewState.value.transactionDetailsUi ?: return
                val body = details.body as? TransactionDetailsBodyUi.Presentation ?: return
                val counts = body.actionCounts as? PresentationActionCountsUiState.Content ?: return
                val count = when (event.action) {
                    TransactionDataProtectionAction.RequestDataDeletion -> counts.dataDeletionRequests
                    TransactionDataProtectionAction.ReportSuspiciousTransaction -> counts.dpaReports
                }
                if (count <= 0) return
                setEffect {
                    Effect.Navigation.SwitchScreen(
                        screenRoute = generateComposableNavigationLink(
                            screen = DashboardScreens.TransactionHistory,
                            arguments = generateComposableArguments(
                                mapOf(
                                    "transactionId" to Uri.encode(details.transactionId),
                                    "actionType" to event.action.name,
                                )
                            ),
                        )
                    )
                }
            }

            is Event.RetryPresentationActionCounts -> {
                observePresentationActionCounts()
            }

            is Event.ExpandOrCollapseGroupItem -> {
                setState {
                    val details = transactionDetailsUi ?: return@setState this
                    val updatedIds = if (event.itemId in expandedGroupIds) {
                        expandedGroupIds - event.itemId
                    } else {
                        expandedGroupIds + event.itemId
                    }
                    copy(
                        expandedGroupIds = updatedIds,
                        transactionDetailsUi = details.copy(
                            body = details.body.withExpansionIcons(updatedIds),
                        ),
                    )
                }
            }

            is Event.ExpandOrCollapseCard -> {
                if (viewState.value.isLoading) return
                val card = viewState.value.transactionDetailsUi?.transactionDetailsCardUi ?: return
                if (card.metadata.isEmpty()) return
                setState { copy(isCardExpanded = !isCardExpanded) }
            }

            is Event.OpenLink -> {
                if (viewState.value.isLoading || viewState.value.transactionDetailsUi == null) return
                setEffect { Effect.Navigation.OpenUrlExternally(event.url) }
            }
        }
    }

    private fun TransactionDetailsBodyUi.withExpansionIcons(
        expandedGroupIds: Set<String>,
    ): TransactionDetailsBodyUi = when (this) {
        is TransactionDetailsBodyUi.Presentation -> copy(
            requested = requested.withExpansionIcons(expandedGroupIds),
            shared = shared.withExpansionIcons(expandedGroupIds),
            transactionData = transactionData?.withExpansionIcons(expandedGroupIds),
        )

        is TransactionDetailsBodyUi.DataDeletionRequest -> copy(
            claims = claims.withExpansionIcons(expandedGroupIds),
        )

        is TransactionDetailsBodyUi.Issuance,
        is TransactionDetailsBodyUi.Reissuance,
        is TransactionDetailsBodyUi.Deletion,
        is TransactionDetailsBodyUi.Signing,
        is TransactionDetailsBodyUi.DpaReport -> this
    }

    private fun TransactionDetailsSectionUi.withExpansionIcons(
        expandedGroupIds: Set<String>,
    ): TransactionDetailsSectionUi = copy(
        groups = groups.map { group ->
            group.copy(
                header = group.header.withExpansionIcon(isExpanded = group.header.itemId in expandedGroupIds),
            )
        },
    )

    private fun deleteTransaction() {
        if (viewState.value.isLoading || viewState.value.isDeleting) return

        setState {
            copy(
                isLoading = true,
                isDeleting = true,
                error = null
            )
        }

        hideBottomSheet()

        viewModelScope.launch {
            interactor.deleteTransaction(transactionId = transactionId)
                .collect { response ->
                    when (response) {
                        is TransactionDetailsInteractorDeleteTransactionPartialState.Success -> {
                            setEffect { Effect.Navigation.Pop }
                        }

                        is TransactionDetailsInteractorDeleteTransactionPartialState.Failure -> {
                            setState {
                                copy(
                                    isLoading = false,
                                    isDeleting = false,
                                    error = ContentErrorConfig(
                                        onRetry = { setEvent(Event.DeletePressed) },
                                        errorSubTitle = response.errorMessage,
                                        onCancel = { setEvent(Event.DismissError) },
                                    )
                                )
                            }
                        }
                    }
                }
        }
    }

    private fun showBottomSheet(sheetContent: TransactionDetailsBottomSheetContent) {
        setState { copy(sheetContent = sheetContent) }
        setEffect { Effect.ShowBottomSheet }
    }

    private fun hideBottomSheet() {
        if (!viewState.value.isBottomSheetOpen) return
        setEffect { Effect.CloseBottomSheet }
    }

    private fun observePresentationActionCounts() {
        val details = viewState.value.transactionDetailsUi ?: return
        if (details.body !is TransactionDetailsBodyUi.Presentation) return

        presentationActionCountsJob?.cancel()
        presentationActionCountsJob = viewModelScope.launch {
            interactor.observePresentationActionCounts(presentationId = details.transactionId)
                .collect { actionCounts ->
                    setState {
                        val currentDetails = transactionDetailsUi ?: return@setState this
                        if (currentDetails.transactionId != details.transactionId) return@setState this
                        val presentation =
                            currentDetails.body as? TransactionDetailsBodyUi.Presentation
                                ?: return@setState this
                        copy(
                            transactionDetailsUi = currentDetails.copy(
                                body = presentation.copy(actionCounts = actionCounts),
                            )
                        )
                    }
                }
        }
    }

    private fun getTransactionDetails(event: Event) {
        transactionDetailsJob?.cancel()
        presentationActionCountsJob?.cancel()

        setState {
            copy(
                isLoading = true,
                error = null
            )
        }

        transactionDetailsJob = viewModelScope.launch {
            interactor.getTransactionDetails(
                transactionId = transactionId,
            ).collect { response ->
                when (response) {
                    is TransactionDetailsInteractorPartialState.Success -> {
                        val details = response.transactionDetailsUi
                        setState {
                            val isSameTransaction =
                                transactionDetailsUi?.transactionId == details.transactionId
                            val retainedGroupIds =
                                if (isSameTransaction) expandedGroupIds else emptySet()
                            copy(
                                isLoading = false,
                                error = null,
                                transactionDetailsUi = details.copy(
                                    body = details.body.withExpansionIcons(retainedGroupIds),
                                ),
                                expandedGroupIds = retainedGroupIds,
                                isCardExpanded = isSameTransaction && isCardExpanded,
                            )
                        }
                        observePresentationActionCounts()
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

}