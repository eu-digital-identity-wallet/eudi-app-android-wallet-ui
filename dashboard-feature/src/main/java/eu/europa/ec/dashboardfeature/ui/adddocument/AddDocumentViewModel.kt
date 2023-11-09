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

package eu.europa.ec.dashboardfeature.ui.adddocument

import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.model.DocumentOptionItemUi
import eu.europa.ec.commonfeature.model.DocumentTypeUi
import eu.europa.ec.dashboardfeature.interactor.AddDocumentInteractor
import eu.europa.ec.dashboardfeature.interactor.AddDocumentInteractorPartialState
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
    data class NavigateToIssueDocument(val url: String, val type: DocumentTypeUi): Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
        data class SwitchScreen(val screenRoute: String) : Navigation()
    }
}

@KoinViewModel
class AddDocumentViewModel(
    private val addDocumentInteractor: AddDocumentInteractor
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State = State(
        title = addDocumentInteractor.getTitle(),
        subtitle = addDocumentInteractor.getSubtitle()
    )

    override fun handleEvents(event: Event) = when (event) {
        Event.Init -> getOptions(event)
        Event.Pop -> setEffect { Effect.Navigation.Pop }
        is Event.NavigateToIssueDocument -> {
            // Dummy
            val urlAndType = "${event.url}, ${event.type}"
            //setEffect { Effect.Navigation.SwitchScreen() }
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