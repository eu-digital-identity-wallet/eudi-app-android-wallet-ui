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

package eu.europa.ec.issuancefeature.ui.document.add

import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.model.DocumentOptionItemUi
import eu.europa.ec.commonfeature.model.DocumentTypeUi
import eu.europa.ec.issuancefeature.interactor.document.AddDocumentInteractor
import eu.europa.ec.issuancefeature.interactor.document.AddDocumentInteractorPartialState
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
    val error: ContentErrorConfig? = null,
    val title: String = "",
    val subtitle: String = "",
    val options: List<DocumentOptionItemUi> = emptyList()
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object Pop : Event()
    data class NavigateToIssueDocument(val url: String, val type: DocumentTypeUi) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
        data class SwitchScreen(val screenRoute: String) : Navigation()
    }
}

@KoinViewModel
class AddDocumentViewModel(
    private val addDocumentInteractor: AddDocumentInteractor,
    private val resourceProvider: ResourceProvider
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State = State(
        title = resourceProvider.getString(R.string.add_document_title),
        subtitle = resourceProvider.getString(R.string.add_document_subtitle)
    )

    override fun handleEvents(event: Event) = when (event) {
        is Event.Init -> getOptions(event)

        is Event.Pop -> setEffect { Effect.Navigation.Pop }

        is Event.NavigateToIssueDocument -> {
            // TODO
        }
    }

    private fun getOptions(event: Event) {
        setState {
            copy(
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            addDocumentInteractor.getAddDocumentOption().collect { response ->
                when (response) {
                    is AddDocumentInteractorPartialState.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                options = response.options,
                                error = null
                            )
                        }
                    }

                    is AddDocumentInteractorPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                options = emptyList(),
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