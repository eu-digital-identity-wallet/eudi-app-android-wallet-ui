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

package eu.europa.ec.proximityfeature.ui.qr

import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.corelogic.di.getOrNullKoinScope
import eu.europa.ec.proximityfeature.interactor.ProximityQRInteractor
import eu.europa.ec.proximityfeature.interactor.ProximityQRPartialState
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.ProximityScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class State(
    val isLoading: Boolean = true,
    val error: ContentErrorConfig? = null,

    val qrCode: String = "",
    val presentationScopeId: String = ""
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object GoBack : Event()
    data class OnStop(
        val isChangingConfigurations: Boolean,
        val isCurrentScreen: Boolean
    ) : Event()

    data class NfcEngagement(
        val componentActivity: ComponentActivity,
        val enable: Boolean
    ) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(
            val screenRoute: String
        ) : Navigation()

        data object Pop : Navigation()
    }
}

class ProximityQRViewModel(
    private val interactor: ProximityQRInteractor,
    private val uiSerializer: UiSerializer,
    private val requestUriConfigRaw: String
) : MviViewModel<Event, State, Effect>() {

    private var interactorJob: Job? = null

    /** True once a reader has connected and the request screen has taken the session over. */
    private var sessionHandedOver = false

    /** True once the session has been stopped, so that it is stopped only once. */
    private var cleanedUp = false

    override fun setInitialState(): State = State()

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                initializeConfig()
                generateQrCode()
            }

            is Event.GoBack -> {
                cleanUp()
                setState { copy(error = null) }
                setEffect { Effect.Navigation.Pop }
            }

            is Event.OnStop -> {
                if (shouldStopOnLeavingForeground(event.isChangingConfigurations)) {
                    cleanUp()
                    if (event.isCurrentScreen) setEffect { Effect.Navigation.Pop }
                }
            }

            is Event.NfcEngagement -> {
                if (event.enable && cleanedUp) return
                interactor.toggleNfcEngagement(
                    event.componentActivity,
                    event.enable
                )
            }
        }
    }

    /**
     * Whether the activity stopping stops the session, so that the wallet does not keep
     * advertising over BLE for a reader while the user is elsewhere or the screen is off. An
     * activity stopped only to be recreated by a configuration change keeps the session, and so
     * does a session the request screen has taken over. One already stopped, or never started, has
     * nothing to stop. The screen then closes only while it is the current one
     * ([Event.OnStop.isCurrentScreen]): when the activity stops while another screen, such as the
     * login screen after a lock, is taking its place, closing it would remove that screen instead.
     *
     * @param isChangingConfigurations Whether the activity stops only to be recreated.
     * @return True when the session is to be stopped and the screen closed.
     */
    private fun shouldStopOnLeavingForeground(isChangingConfigurations: Boolean): Boolean =
        !isChangingConfigurations &&
                !sessionHandedOver &&
                !cleanedUp &&
                viewState.value.presentationScopeId.isNotEmpty()

    private fun initializeConfig() {
        val requestUriConfig = uiSerializer.fromBase64(
            requestUriConfigRaw,
            RequestUriConfig::class.java,
            RequestUriConfig.Parser
        ) ?: throw RuntimeException("RequestUriConfig:: is Missing or invalid")

        setState {
            copy(presentationScopeId = requestUriConfig.presentationScopeId)
        }

        interactor.setConfig(requestUriConfig)
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
                        sessionHandedOver = true
                        setEffect {
                            Effect.Navigation.SwitchScreen(
                                screenRoute = generateComposableNavigationLink(
                                    screen = ProximityScreens.Request,
                                    arguments = generateComposableArguments(
                                        mapOf(
                                            "scopeId" to viewState.value.presentationScopeId
                                        )
                                    )
                                )
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
        cleanedUp = true
        unsubscribe()
        interactor.cancelTransfer()
        getOrNullKoinScope(viewState.value.presentationScopeId)?.close()
    }

    /**
     * Stops the session, with its BLE advertising and its presentation scope, whenever the screen
     * is removed without handing it over: by Back, by a link that takes the user elsewhere, or by
     * the session lock. Leaving the app stops it earlier, on [Event.OnStop]. After a reader has
     * connected, the request screen owns the session and stops it. A screen removed before its
     * session started has nothing to stop.
     */
    override fun onCleared() {
        if (!sessionHandedOver && !cleanedUp && viewState.value.presentationScopeId.isNotEmpty()) {
            cleanUp()
        }
    }
}