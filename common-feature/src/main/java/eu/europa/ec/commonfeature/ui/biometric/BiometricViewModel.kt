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

package eu.europa.ec.commonfeature.ui.biometric

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAuthenticate
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAvailability
import eu.europa.ec.authenticationlogic.provider.PinLockoutState
import eu.europa.ec.authenticationlogic.secure.SecurePin
import eu.europa.ec.businesslogic.extension.toUri
import eu.europa.ec.commonfeature.config.BiometricUiConfig
import eu.europa.ec.commonfeature.interactor.BiometricInteractor
import eu.europa.ec.commonfeature.interactor.QuickPinInteractorPinValidPartialState
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.FlowCompletion
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

sealed class Event : ViewEvent {
    data class OnBiometricsClicked(
        val context: Context,
        val shouldThrowErrorIfNotAvailable: Boolean
    ) : Event()

    data object LaunchBiometricSystemScreen : Event()
    data object OnNavigateBack : Event()
    data object OnErrorDismiss : Event()
    data object Init : Event()
    data class OnQuickPinEntered(val quickPin: SecurePin) : Event()
    data class OnQuickPinLengthChanged(val length: Int) : Event()
}

data class State(
    val isLoading: Boolean = false,
    val error: ContentErrorConfig? = null,
    val config: BiometricUiConfig,
    val quickPinError: String? = null,
    val userBiometricsAreEnabled: Boolean = false,
    val isBackable: Boolean = false,
    val notifyOnAuthenticationFailure: Boolean = true,
    val quickPinSize: Int = 6,
    val isLockedOut: Boolean = false,
    val lockoutMessage: String? = null
) : ViewState

sealed class Effect : ViewSideEffect {
    data object InitializeBiometricAuthOnCreate : Effect()
    sealed class Navigation : Effect() {
        data class SwitchScreen(
            val screen: String,
            val screenPopUpTo: String
        ) : Navigation()

        data class PopBackStackUpTo(
            val screenRoute: String,
            val inclusive: Boolean,
            val indicateFlowCompletion: FlowCompletion
        ) : Navigation()

        data object LaunchBiometricsSystemScreen : Navigation()
        data class Deeplink(
            val link: Uri,
            val isPreAuthorization: Boolean,
            val routeToPop: String? = null
        ) : Navigation()

        data object Pop : Navigation()
        data object Finish : Navigation()
    }
}

@KoinViewModel
class BiometricViewModel(
    private val biometricInteractor: BiometricInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
    @InjectedParam private val biometricConfig: String
) : MviViewModel<Event, State, Effect>() {

    private val biometricUiConfig
        get() = viewState.value.config

    private var lockoutTickJob: Job? = null

    override fun setInitialState(): State {
        val config = uiSerializer.fromBase64(
            biometricConfig,
            BiometricUiConfig::class.java,
            BiometricUiConfig.Parser
        ) ?: throw RuntimeException("BiometricUiConfig:: is Missing or invalid")
        return State(
            config = config,
            userBiometricsAreEnabled = false,
            isBackable = config.onBackNavigationConfig.isBackable
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                viewModelScope.launch {
                    val userBiometricsAreEnabled = biometricInteractor.getBiometricUserSelection()
                    setState {
                        copy(userBiometricsAreEnabled = userBiometricsAreEnabled)
                    }
                    when (val lockoutState = biometricInteractor.getPinLockoutState()) {
                        is PinLockoutState.Active -> startLockoutTick(lockoutState.remaining.inWholeMilliseconds)
                        PinLockoutState.Idle -> Unit
                    }
                    if (
                        biometricUiConfig.shouldInitializeBiometricAuthOnCreate
                        && userBiometricsAreEnabled
                    ) {
                        setEffect {
                            Effect.InitializeBiometricAuthOnCreate
                        }
                    }
                }
            }

            is Event.OnBiometricsClicked -> {
                setState { copy(error = null) }
                when (val availability = biometricInteractor.getBiometricsAvailability()) {
                    is BiometricsAvailability.CanAuthenticate -> authenticate(
                        event.context
                    )

                    is BiometricsAvailability.NonEnrolled -> {
                        if (!event.shouldThrowErrorIfNotAvailable) {
                            return
                        }
                        setEffect {
                            Effect.Navigation.LaunchBiometricsSystemScreen
                        }
                    }

                    is BiometricsAvailability.Failure -> {
                        if (!event.shouldThrowErrorIfNotAvailable) {
                            return
                        }
                        setState {
                            copy(
                                error = ContentErrorConfig(
                                    errorSubTitle = availability.errorMessage,
                                    onCancel = { setEvent(Event.OnErrorDismiss) }
                                )
                            )
                        }
                    }
                }
            }

            is Event.LaunchBiometricSystemScreen -> {
                setState { copy(error = null) }
                biometricInteractor.launchBiometricSystemScreen()
            }

            is Event.OnNavigateBack -> {
                setState { copy(error = null) }
                biometricUiConfig.onBackNavigationConfig.onBackNavigation?.let {
                    doNavigation(
                        navigation = it,
                        flowSucceeded = false
                    )
                }
            }

            is Event.OnErrorDismiss -> setState {
                copy(error = null)
            }

            is Event.OnQuickPinEntered -> {
                if (viewState.value.isLockedOut) {
                    event.quickPin.close()
                    return
                }
                setState {
                    copy(
                        quickPinError = null
                    )
                }
                authorizeWithPin(event.quickPin)
            }

            is Event.OnQuickPinLengthChanged -> {
                if (viewState.value.isLockedOut) {
                    return
                }
                setState {
                    copy(
                        quickPinError = null
                    )
                }
            }
        }
    }

    override fun onCleared() {
        lockoutTickJob?.cancel()
        lockoutTickJob = null
        super.onCleared()
    }

    private fun authorizeWithPin(pin: SecurePin) {

        if (pin.length != viewState.value.quickPinSize) {
            pin.close()
            return
        }

        setState {
            copy(
                isLoading = true
            )
        }

        viewModelScope.launch {
            biometricInteractor.isPinValid(pin)
                .collect {
                    when (it) {
                        is QuickPinInteractorPinValidPartialState.Failed -> {
                            when (val lockoutState = biometricInteractor.recordPinFailure()) {
                                is PinLockoutState.Active -> {
                                    setState { copy(isLoading = false) }
                                    startLockoutTick(lockoutState.remaining.inWholeMilliseconds)
                                }

                                PinLockoutState.Idle -> {
                                    setState {
                                        copy(
                                            quickPinError = it.errorMessage,
                                            isLoading = false
                                        )
                                    }
                                }
                            }
                        }

                        is QuickPinInteractorPinValidPartialState.Success -> {
                            biometricInteractor.resetPinThrottle()
                            stopLockoutTick()
                            authenticationSuccess()
                        }
                    }
                }
        }
    }

    private fun authenticate(context: Context) {
        biometricInteractor.authenticateWithBiometrics(
            context = context,
            notifyOnAuthenticationFailure = viewState.value.notifyOnAuthenticationFailure
        ) {
            when (it) {
                is BiometricsAuthenticate.Success -> {
                    viewModelScope.launch {
                        biometricInteractor.resetPinThrottle()
                        stopLockoutTick()
                        authenticationSuccess()
                    }
                }

                else -> {}
            }
        }
    }

    private fun authenticationSuccess() {
        doNavigation(
            navigation = biometricUiConfig.onSuccessNavigation,
            flowSucceeded = true
        )
    }

    private fun startLockoutTick(initialRemainingMs: Long) {
        lockoutTickJob?.cancel()
        if (initialRemainingMs <= 0L) {
            stopLockoutTick()
            return
        }
        setState {
            copy(
                isLockedOut = true,
                isLoading = false,
                quickPinError = null,
                lockoutMessage = buildLockoutMessage(initialRemainingMs)
            )
        }
        lockoutTickJob = viewModelScope.launch {
            var remaining = initialRemainingMs
            while (remaining > 0L) {
                delay(1_000L)
                remaining -= 1_000L
                if (remaining <= 0L) break
                setState {
                    copy(lockoutMessage = buildLockoutMessage(remaining))
                }
            }
            stopLockoutTick()
        }
    }

    private fun stopLockoutTick() {
        lockoutTickJob?.cancel()
        lockoutTickJob = null
        setState {
            copy(
                isLockedOut = false,
                lockoutMessage = null
            )
        }
    }

    private fun buildLockoutMessage(remainingMs: Long): String {
        val totalSeconds = ((remainingMs + 999L) / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        val mmss = "%02d:%02d".format(minutes, seconds)
        return resourceProvider.getString(
            R.string.quick_pin_locked_out,
            biometricInteractor.maxFailedPinAttempts,
            mmss
        )
    }

    private fun doNavigation(
        navigation: ConfigNavigation,
        screenRoute: String = CommonScreens.Biometric.screenRoute,
        flowSucceeded: Boolean
    ) {
        navigate(navigation, screenRoute, flowSucceeded)
    }

    private fun navigate(
        navigation: ConfigNavigation,
        screenRoute: String = CommonScreens.Biometric.screenRoute,
        flowSucceeded: Boolean
    ) {
        val navigationEffect: Effect.Navigation = when (val nav = navigation.navigationType) {

            is NavigationType.PopTo -> {
                Effect.Navigation.PopBackStackUpTo(
                    screenRoute = nav.screen.screenRoute,
                    inclusive = false,
                    indicateFlowCompletion = when (navigation.indicateFlowCompletion) {
                        FlowCompletion.CANCEL -> if (!flowSucceeded) FlowCompletion.CANCEL else FlowCompletion.NONE
                        FlowCompletion.SUCCESS -> if (flowSucceeded) FlowCompletion.SUCCESS else FlowCompletion.NONE
                        FlowCompletion.NONE -> FlowCompletion.NONE
                    }
                )
            }

            is NavigationType.PushScreen -> {
                Effect.Navigation.SwitchScreen(
                    screen = generateComposableNavigationLink(
                        screen = nav.screen,
                        arguments = generateComposableArguments(nav.arguments)
                    ),
                    screenPopUpTo = screenRoute
                )
            }

            is NavigationType.PushRoute -> {
                Effect.Navigation.SwitchScreen(
                    screen = nav.route,
                    screenPopUpTo = screenRoute
                )
            }

            is NavigationType.Deeplink -> Effect.Navigation.Deeplink(
                link = nav.link.toUri(),
                isPreAuthorization = viewState.value.config.isPreAuthorization,
                routeToPop = nav.routeToPop
            )

            is NavigationType.Pop -> Effect.Navigation.Pop
            is NavigationType.Finish -> Effect.Navigation.Finish
        }

        setEffect {
            navigationEffect
        }
    }
}