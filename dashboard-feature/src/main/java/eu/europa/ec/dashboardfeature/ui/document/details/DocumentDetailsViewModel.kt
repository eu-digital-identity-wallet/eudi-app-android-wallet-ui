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

package eu.europa.ec.dashboardfeature.ui.document.details

import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.dashboardfeature.interactor.document.DocumentDetailsInteractor
import eu.europa.ec.dashboardfeature.interactor.document.DocumentDetailsInteractorPartialState
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.HeaderData
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

data class State(
    val isLoading: Boolean = false,
    val error: ContentErrorConfig? = null,
    val document: DocumentUi? = null,
    val headerData: HeaderData? = null
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object Pop : Event()
}


sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
    }
}

@KoinViewModel
class DocumentDetailsViewModel(
    private val documentDetailsInteractor: DocumentDetailsInteractor,
    @InjectedParam private val documentId: String
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State = State()

    override fun handleEvents(event: Event) = when (event) {
        is Event.Init -> getDocument(event)
        is Event.Pop -> setEffect { Effect.Navigation.Pop }
    }

    private fun getDocument(event: Event) {

        setState {
            copy(
                isLoading = document == null,
                error = null
            )
        }

        viewModelScope.launch {
            documentDetailsInteractor.getDocument(documentId).collect { response ->
                when (response) {
                    is DocumentDetailsInteractorPartialState.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                document = response.document,
                                headerData = HeaderData(
                                    title = response.document.documentType.title,
                                    subtitle = response.userName,
                                    AppIcons.User,
                                    AppIcons.IdStroke
                                )
                            )
                        }
                    }

                    is DocumentDetailsInteractorPartialState.Failure -> {
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