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

package eu.europa.ec.dashboardfeature.ui.reset_wallet

import androidx.lifecycle.viewModelScope
import eu.europa.ec.dashboardfeature.interactor.ResetWalletInteractor
import eu.europa.ec.dashboardfeature.interactor.ResetWalletInteractorPartialState
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class State(
    val isLoading: Boolean = false,
    val error: ContentErrorConfig? = null,
    val title: String,
    val subtitle: String,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object Retry : Event()
    data object Close : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object StartAgain : Navigation()
    }
}

/**
 * Runs the factory reset once the user has confirmed it and passed the PIN or biometric check, then
 * starts the wallet again at the splash screen, which leads to onboarding. A screen restored after the
 * process ended finds the wallet locked and does not start the reset; the lock then routes to the
 * login screen. The screen offers no way back while the data is being deleted. If the reset fails,
 * the user can retry, or close the screen, which also starts the wallet again at the splash screen.
 */
class ResetWalletViewModel(
    private val resetWalletInteractor: ResetWalletInteractor,
    private val resourceProvider: ResourceProvider,
) : MviViewModel<Event, State, Effect>() {

    private var resetJob: Job? = null

    override fun setInitialState(): State = State(
        title = resourceProvider.getString(R.string.reset_wallet_screen_title),
        subtitle = resourceProvider.getString(R.string.reset_wallet_screen_subtitle),
    )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> if (resetWalletInteractor.isSessionUnlocked()) resetWallet()
            is Event.Retry -> resetWallet()
            is Event.Close -> setEffect { Effect.Navigation.StartAgain }
        }
    }

    private fun resetWallet() {
        if (resetJob?.isActive == true) return

        setState {
            copy(
                isLoading = true,
                error = null
            )
        }

        resetJob = viewModelScope.launch {
            when (val state = resetWalletInteractor.resetWallet()) {
                is ResetWalletInteractorPartialState.Success -> {
                    setEffect { Effect.Navigation.StartAgain }
                }

                is ResetWalletInteractorPartialState.Failure -> {
                    setState {
                        copy(
                            isLoading = false,
                            error = ContentErrorConfig(
                                errorSubTitle = state.errorMessage,
                                onRetry = { setEvent(Event.Retry) },
                                onCancel = { setEvent(Event.Close) },
                            )
                        )
                    }
                }
            }
        }
    }
}