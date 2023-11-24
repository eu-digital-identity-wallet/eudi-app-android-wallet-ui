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

package eu.europa.ec.proximityfeature.ui.qr

import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.di.getPresentationScope
import eu.europa.ec.proximityfeature.interactor.ProximityQRInteractor
import eu.europa.ec.proximityfeature.interactor.ProximityQRPartialState
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.ProximityScreens
import kotlinx.coroutines.Job
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
class ProximityQRViewModel(
    private val interactor: ProximityQRInteractor
) : MviViewModel<Event, State, Effect>() {

    private var interactorJob: Job? = null

    override fun setInitialState(): State = State()

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                generateQrCode()
            }

            is Event.GoBack -> {
                cleanUp()
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

        interactorJob = viewModelScope.launch {
            interactor.startQrEngagement().collect { response ->
                when (response) {
                    is ProximityQRPartialState.Error -> {
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

                    is ProximityQRPartialState.QrReady -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                qrCode = response.qrCode
                            )
                        }
                    }

                    is ProximityQRPartialState.Connected -> {
                        unsubscribe()
                        setEffect {
                            Effect.Navigation.SwitchScreen(
                                ProximityScreens.Request.screenRoute
                            )
                        }
                    }

                    is ProximityQRPartialState.Disconnected -> {
                        unsubscribe()
                        setEvent(Event.GoBack)
                    }
                }
            }
        }
    }

    /**
     * Required in order to stop receiving emissions from interactor Flow
     * */
    private fun unsubscribe() {
        interactorJob?.cancel()
    }

    /**
     * Stop presentation and remove scope/listeners
     * */
    private fun cleanUp() {
        unsubscribe()
        getPresentationScope().close()
        interactor.cancelTransfer()
    }
}