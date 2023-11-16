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