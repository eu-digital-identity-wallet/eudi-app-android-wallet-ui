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

package eu.europa.ec.issuancefeature.ui.authenticate

import androidx.lifecycle.viewModelScope
import eu.europa.ec.businesslogic.extension.validateAndFormatUrl
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

data class State(
    val isLoading: Boolean = true,
    val error: ContentErrorConfig? = null,

    val url: String,
    val userWasRedirected: Boolean,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object GoBack : Event()
    data object CheckIfUserWasRedirected : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(
            val screenRoute: String
        ) : Navigation()

        data object Pop : Navigation()
        data object OpenDeepLinkAction : Navigation()
    }

    data class OpenUrlExternally(val url: String) : Effect()
}

@KoinViewModel
class AuthenticateViewModel(
    @InjectedParam private val authUrl: String,
    @InjectedParam internal val docType: String,
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State {
        return State(
            url = authUrl.validateAndFormatUrl(),
            userWasRedirected = false
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                init()
            }

            is Event.GoBack -> {
                setEffect { Effect.Navigation.Pop }
            }

            is Event.CheckIfUserWasRedirected -> {
                if (viewState.value.userWasRedirected) {
                    setState { copy(isLoading = false) }
                }
            }
        }
    }

    private fun init() {
        viewModelScope.launch {
            delay(1500L)

            setState {
                copy(userWasRedirected = true)
            }

            setEffect { Effect.Navigation.OpenDeepLinkAction }

            setEffect { Effect.OpenUrlExternally(viewState.value.url) }
        }
    }

}