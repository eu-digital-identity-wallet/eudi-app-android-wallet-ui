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

package eu.europa.ec.commonfeature.ui.trustmark

import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.config.TrustMarkMode
import eu.europa.ec.commonfeature.config.TrustMarkUiConfig
import eu.europa.ec.commonfeature.interactor.CompleteTrustMarkIntroductionPartialState
import eu.europa.ec.commonfeature.interactor.LoadTrustMarkPartialState
import eu.europa.ec.commonfeature.interactor.TrustMarkInteractor
import eu.europa.ec.commonfeature.ui.trustmark.model.TrustMarkUi
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

data class State(
    val config: TrustMarkUiConfig,
    val trustMark: TrustMarkUi? = null,
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val isCompleting: Boolean = false,
    val completionError: String? = null,
    val isNavigating: Boolean = false,
) : ViewState

sealed interface Event : ViewEvent {
    data object Init : Event
    data object Retry : Event
    data object Continue : Event
    data object Back : Event
    data class OpenLink(val url: String) : Event
}

sealed interface Effect : ViewSideEffect {
    sealed interface Navigation : Effect {
        data object Pop : Navigation
        data object Finish : Navigation
        data class Continue(val route: String) : Navigation
    }

    data class OpenUrl(val url: String) : Effect
}

@KoinViewModel
class TrustMarkViewModel(
    private val interactor: TrustMarkInteractor,
    private val uiSerializer: UiSerializer,
    @InjectedParam private val trustMarkConfig: String,
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State {
        val config = uiSerializer.fromBase64(
            payload = trustMarkConfig,
            model = TrustMarkUiConfig::class.java,
            parser = TrustMarkUiConfig.Parser,
        ) ?: throw RuntimeException("TrustMarkUiConfig:: is Missing or invalid")

        return State(config = config)
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init,
            is Event.Retry -> {
                loadTrustMark()
            }

            is Event.Continue -> {
                completeIntroduction()
            }

            is Event.Back -> {
                goBack()
            }

            is Event.OpenLink -> {
                openLink(event.url)
            }
        }
    }

    private fun loadTrustMark() {
        if (viewState.value.isLoading) return

        setState {
            copy(
                isLoading = true,
                loadError = null
            )
        }
        viewModelScope.launch {
            when (val result = interactor.getTrustMark()) {
                is LoadTrustMarkPartialState.Success -> {
                    setState {
                        copy(
                            isLoading = false,
                            trustMark = result.trustMark,
                        )
                    }
                }

                is LoadTrustMarkPartialState.Failure -> {
                    setState {
                        copy(
                            isLoading = false,
                            trustMark = null,
                            loadError = result.error
                        )
                    }
                }
            }
        }
    }

    private fun completeIntroduction() {
        val currentState = viewState.value
        val mode = currentState.config.mode as? TrustMarkMode.Welcome ?: return
        if (currentState.isCompleting || currentState.isNavigating) return

        setState {
            copy(
                isCompleting = true,
                completionError = null
            )
        }
        viewModelScope.launch {
            when (val result = interactor.completeIntroduction()) {
                is CompleteTrustMarkIntroductionPartialState.Success -> {
                    setState {
                        copy(
                            isCompleting = false,
                            isNavigating = true
                        )
                    }
                    setEffect { Effect.Navigation.Continue(route = mode.continuationRoute) }
                }

                is CompleteTrustMarkIntroductionPartialState.Failure -> {
                    setState {
                        copy(
                            isCompleting = false,
                            completionError = result.error
                        )
                    }
                }
            }
        }
    }

    private fun goBack() {
        if (viewState.value.isCompleting || viewState.value.isNavigating) return

        setState { copy(isNavigating = true) }
        setEffect {
            if (viewState.value.config.mode is TrustMarkMode.Welcome) {
                Effect.Navigation.Finish
            } else {
                Effect.Navigation.Pop
            }
        }
    }

    private fun openLink(url: String) {
        val currentState = viewState.value
        val trustMark = currentState.trustMark ?: return
        if (currentState.isNavigating) return
        if (url != trustMark.certifiedWalletsUrl && url != trustMark.walletSolutionUrl) return

        setEffect { Effect.OpenUrl(url = url) }
    }
}