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

package eu.europa.ec.loginfeature.ui.faq

import eu.europa.ec.loginfeature.interactor.FaqInteractor
import eu.europa.ec.loginfeature.model.FaqUiModel
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import org.koin.android.annotation.KoinViewModel

sealed class Event : ViewEvent {
    data object Pop : Event()
    data class Search(val queryString: String) : Event()
}

data class State(
    val initialFaqItems: List<FaqUiModel>,
    val presentableFaqItems: List<FaqUiModel>,
    val noSearchResult: Boolean = false
) : ViewState


sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
        data class SwitchScreen(val screen: String) : Navigation()
    }
}

@KoinViewModel
class FaqScreenViewModel(
    private val interactor: FaqInteractor
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State = State(
        initialFaqItems = interactor.initializeData(),
        presentableFaqItems = interactor.initializeData()
    )

    override fun handleEvents(event: Event) {
        when (event) {
            Event.Pop -> setEffect { Effect.Navigation.Pop }
            is Event.Search -> setState {
                val searchResult = initialFaqItems.query(event.queryString)
                copy(presentableFaqItems = searchResult, noSearchResult = searchResult.isEmpty())
            }
        }
    }

    private fun List<FaqUiModel>.query(queryString: String): List<FaqUiModel> {
        return filter {
            it.title.contains(other = queryString, true) ||
                    it.description.contains(other = queryString, true)
        }
    }
}