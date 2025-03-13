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

package eu.europa.ec.issuancefeature.ui.transaction.details

import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.model.TransactionDetailsUi
import eu.europa.ec.commonfeature.ui.transaction_details.transformer.transformToTransactionDetailsUi
import eu.europa.ec.issuancefeature.interactor.document.TransactionDetailsInteractor
import eu.europa.ec.issuancefeature.interactor.document.TransactionDetailsInteractorPartialState
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class State(
    val isLoading: Boolean = false,
    val title: String? = null,
    val error: ContentErrorConfig? = null,
    val detailsDataSharedSection: String,
    val detailsDataSignedSection: String,
    val transactionDetailsCardData: TransactionDetailsCardData,
    val transactionDetailsUi: TransactionDetailsUi? = null,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object Pop : Event()
    data object PrimaryButtonPressed : Event()
    data object SecondaryButtonPressed : Event()
    data object DismissError : Event()

    data class ExpandOrCollapseTransactionDataSharedItem(val itemId: String) : Event()
    data object ExpandOrCollapseTransactionDataSignedItem : Event()
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
    private val transactionDetailsInteractor: TransactionDetailsInteractor,
    private val resourceProvider: ResourceProvider,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State = State(
        title = resourceProvider.getString(R.string.transaction_details_screen_title),
        detailsDataSharedSection = resourceProvider.getString(R.string.transaction_details_data_shared),
        detailsDataSignedSection = resourceProvider.getString(R.string.transaction_details_data_signed),
        transactionDetailsCardData = TransactionDetailsCardData(
            transactionType = "e-Signature",
            transactionItemLabel = "File_signed.pdf",
            relyingPartyName = "Central issuer",
            transactionDate = "16 February 2024",
            status = "Completed",
            isVerified = true
        )
    )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                getTransactionDetails(event)
            }

            is Event.DismissError -> TODO()
            is Event.Pop -> {
                setState { copy(error = null) }
                setEffect { Effect.Navigation.Pop }
            }

            is Event.PrimaryButtonPressed -> {}
            is Event.SecondaryButtonPressed -> {}
            is Event.ExpandOrCollapseTransactionDataSharedItem -> {}
            is Event.ExpandOrCollapseTransactionDataSignedItem -> {}
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
            transactionDetailsInteractor.getTransactionDetails(
                transactionId = "transactionId",
            ).collect { response ->
                when (response) {
                    is TransactionDetailsInteractorPartialState.Success -> {
                        val detailsUiTitle =
                            response.detailsTitle
                        val transactionDetailsUi =
                            response.transactionDetailsDomain.transformToTransactionDetailsUi()
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                title = detailsUiTitle,
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
}