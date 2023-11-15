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

package eu.europa.ec.proximityfeature.ui

import androidx.lifecycle.viewModelScope
import eu.europa.ec.proximityfeature.interactor.QRInteractor
import eu.europa.ec.proximityfeature.interactor.QRInteractorPartialState
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.PresentationScreens
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class State(
    val isLoading: Boolean = true,
    val error: ContentErrorConfig? = null,

    val qrCode: String = "",
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object GoBack : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(
            val screenRoute: String
        ) : Navigation()

        data object Pop : Navigation()
    }
}

@KoinViewModel
class QRViewModel(
    private val interactor: QRInteractor
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State = State()

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                generateQrCode()
            }

            is Event.GoBack -> {
                setEffect { Effect.Navigation.Pop }
            }
        }
    }

    private fun generateQrCode() {
        setState {
            copy(
                isLoading = true,
                error = null
            )
        }
        viewModelScope.launch {
            interactor.startQrEngagement().collect { response ->
                when (response) {
                    is QRInteractorPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = ContentErrorConfig(
                                    onRetry = { setEvent(Event.Init) },
                                    errorSubTitle = response.error,
                                    onCancel = { setEvent(Event.GoBack) }
                                )
                            )
                        }
                    }

                    is QRInteractorPartialState.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                qrCode = response.qRCode
                            )
                        }
                    }

                    is QRInteractorPartialState.Connected -> {
                        setEffect {
                            Effect.Navigation.SwitchScreen(PresentationScreens.CrossDeviceRequest.screenRoute)
                        }
                    }
                }
            }
        }
    }
}