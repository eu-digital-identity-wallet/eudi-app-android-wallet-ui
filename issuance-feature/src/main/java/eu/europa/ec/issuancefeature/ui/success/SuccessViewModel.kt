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

package eu.europa.ec.issuancefeature.ui.success

import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.config.IssuanceFlowUiConfig
import eu.europa.ec.corelogic.model.toDocumentType
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.issuancefeature.interactor.SuccessFetchDocumentByIdPartialState
import eu.europa.ec.issuancefeature.interactor.SuccessInteractor
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

data class State(
    val document: Document? = null,
    val documentName: String = "",
    val userFullName: String = "",
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object GoBack : Event()
    data object PrimaryButtonPressed : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(
            val screenRoute: String
        ) : Navigation()

        data object Pop : Navigation()
    }
}

@KoinViewModel
class SuccessViewModel(
    private val interactor: SuccessInteractor,
    @InjectedParam private val flowType: IssuanceFlowUiConfig,
    @InjectedParam private val documentId: String,
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State {
        return State()
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                viewModelScope.launch {
                    interactor.fetchDocumentById(id = documentId).collect { response ->
                        when (response) {
                            is SuccessFetchDocumentByIdPartialState.Failure -> {

                            }

                            is SuccessFetchDocumentByIdPartialState.Success -> {
                                setState {
                                    copy(
                                        document = response.document,
                                        documentName = response.documentName,
                                        userFullName = response.fullName
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is Event.GoBack -> setEffect { Effect.Navigation.Pop }

            is Event.PrimaryButtonPressed -> {
                viewState.value.document?.let { document ->
                    setEffect {
                        Effect.Navigation.SwitchScreen(
                            screenRoute = generateComposableNavigationLink(
                                screen = IssuanceScreens.DocumentDetails,
                                arguments = generateComposableArguments(
                                    mapOf(
                                        "detailsType" to IssuanceFlowUiConfig.fromIssuanceFlowUiConfig(
                                            flowType
                                        ),
                                        "documentId" to document.id,
                                        "documentType" to document.docType.toDocumentType().codeName,
                                    )
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}