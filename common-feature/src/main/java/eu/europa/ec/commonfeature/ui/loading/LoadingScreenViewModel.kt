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

package eu.europa.ec.commonfeature.ui.loading

import androidx.lifecycle.viewModelScope
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.Screen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class LoadingError(
    val errorMsg: String
)

data class State(
    val error: LoadingError?,
    val screenTitle: String,
    val screenSubtitle: String,
) : ViewState

sealed class Event : ViewEvent {
    data object DoWork : Event()
    data object GoBack : Event()
    data object DismissError : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(val screenRoute: String) : Navigation()
        data class PopBackStackUpTo(
            val screenRoute: String,
            val inclusive: Boolean
        ) : Navigation()
    }
}

abstract class CommonLoadingViewModel : MviViewModel<Event, State, Effect>() {
    private lateinit var job: Job

    /**
     * The title of the re-usable [LoadingScreen] .
     */
    abstract fun getTitle(): String

    /**
     * The subtitle of the re-usable [LoadingScreen] .
     */
    abstract fun getSubtitle(): String

    /**
     * The [Screen] the user will be navigated to:
     * 1. If they press the "X" button--cancel the [LoadingScreen] .
     * 2. If they press the "X" button of the Error screen (should any Error happen).
     */
    abstract fun getPreviousScreen(): Screen

    /**
     * The [Screen] (with its possible arguments) which will be opened
     * when the [LoadingScreen] finishes successfully.
     */
    abstract fun getNextScreen(): String

    /**
     * The [Screen] which opened the re-usable [LoadingScreen] .
     * It will be erased from the back-stack when user successfully moves to the next step [Screen].
     */
    abstract fun getCallerScreen(): Screen

    /**
     * Used to perform any kind of work the calling viewModel needs to.
     * Gets called once upon initialization of the [LoadingScreen] +
     * each time the user presses "Try again" in its Error screen.
     */
    abstract fun doWork()

    override fun setInitialState(): State {
        return State(
            screenTitle = getTitle(),
            screenSubtitle = getSubtitle(),
            error = null
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.DoWork -> {
                job = viewModelScope.launch {
                    setState {
                        copy(error = null)
                    }

                    delay(2_000L)

                    doWork()
                }
            }

            is Event.GoBack -> {
                job.cancel()
                doNavigation(NavigationType.POP)
            }

            is Event.DismissError -> {
                setState {
                    copy(error = null)
                }
            }
        }
    }

    protected fun setErrorState(errorMsg: String) {
        setState {
            copy(error = LoadingError(errorMsg))
        }
    }

    protected fun doNavigation(navigationType: NavigationType) {
        when (navigationType) {
            NavigationType.PUSH -> {
                setEffect {
                    Effect.Navigation.SwitchScreen(getNextScreen())
                }
            }

            NavigationType.POP -> {
                setEffect {
                    Effect.Navigation.PopBackStackUpTo(
                        screenRoute = getPreviousScreen().screenRoute,
                        inclusive = false
                    )
                }
            }

            NavigationType.DEEPLINK -> {}
        }
    }
}