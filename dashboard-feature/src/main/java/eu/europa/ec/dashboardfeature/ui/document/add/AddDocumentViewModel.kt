/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.dashboardfeature.ui.document.add

import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.model.DocumentOptionItemUi
import eu.europa.ec.commonfeature.model.DocumentTypeUi
import eu.europa.ec.dashboardfeature.interactor.document.AddDocumentInteractor
import eu.europa.ec.dashboardfeature.interactor.document.AddDocumentLoadData
import eu.europa.ec.dashboardfeature.interactor.document.AddDocumentOptions
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
    data object AddDocuments : Event()
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
        Event.Init -> getOptions(event)
        Event.Pop -> setEffect { Effect.Navigation.Pop }
        Event.AddDocuments -> loadSampleData(event)
        is Event.NavigateToIssueDocument -> {
            setEffect { Effect.Navigation.SwitchScreen("${event.url}, ${event.type}") }
        }
    }

    private fun loadSampleData(event: Event) {
        setState {
            copy(
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            addDocumentInteractor.addSampleData().collect { result ->
                when (result) {
                    is AddDocumentLoadData.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                options = emptyList(),
                                error = ContentErrorConfig(
                                    onRetry = { setEvent(event) },
                                    errorSubTitle = result.error,
                                    onCancel = { setEvent(Event.Pop) }
                                )
                            )
                        }
                    }

                    AddDocumentLoadData.Success -> {
                        setEffect { Effect.Navigation.Pop }
                    }
                }
            }
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
                    is AddDocumentOptions.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                options = response.options,
                                error = null
                            )
                        }
                    }

                    is AddDocumentOptions.Failure -> {
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