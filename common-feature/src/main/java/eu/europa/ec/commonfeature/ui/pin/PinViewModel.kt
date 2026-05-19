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

package eu.europa.ec.commonfeature.ui.pin

import androidx.lifecycle.viewModelScope
import eu.europa.ec.authenticationlogic.provider.PinLockoutState
import eu.europa.ec.authenticationlogic.secure.SecurePin
import eu.europa.ec.commonfeature.config.IssuanceFlowType
import eu.europa.ec.commonfeature.config.IssuanceUiConfig
import eu.europa.ec.commonfeature.config.SuccessUIConfig
import eu.europa.ec.commonfeature.interactor.QuickPinInteractor
import eu.europa.ec.commonfeature.interactor.QuickPinInteractorPinValidPartialState
import eu.europa.ec.commonfeature.interactor.QuickPinInteractorSetPinPartialState
import eu.europa.ec.commonfeature.model.PinFlow
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.ModuleRoute
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

enum class PinValidationState {
    ENTER,
    REENTER,
    VALIDATE
}

data class State(
    private val pinFlow: PinFlow,
    val isLoading: Boolean = false,
    val isButtonEnabled: Boolean = false,
    val quickPinError: String? = null,
    val subtitle: String = "",
    val title: String = "",
    val buttonText: String = "",
    val resetPin: Boolean = false,
    val pinState: PinValidationState,
    val isBottomSheetOpen: Boolean = false,
    val quickPinSize: Int = 6,
    val isLockedOut: Boolean = false,
    val lockoutMessage: String? = null
) : ViewState {
    val action: ScreenNavigateAction
        get() {
            return when (pinFlow) {
                PinFlow.CREATE_WITH_ACTIVATION, PinFlow.CREATE_WITHOUT_ACTIVATION -> ScreenNavigateAction.NONE
                PinFlow.UPDATE -> ScreenNavigateAction.CANCELABLE
            }
        }

    val onBackEvent: Event
        get() {
            return when (pinFlow) {
                PinFlow.CREATE_WITH_ACTIVATION, PinFlow.CREATE_WITHOUT_ACTIVATION -> Event.Finish
                PinFlow.UPDATE -> Event.CancelPressed
            }
        }
}

sealed class Event : ViewEvent {
    data object Init : Event()
    data class NextButtonPressed(val pin: SecurePin) : Event()
    data class OnQuickPinLengthChanged(val length: Int) : Event()
    data object CancelPressed : Event()
    data object Finish : Event()
    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(val isOpen: Boolean) : BottomSheet()

        sealed class Cancel : BottomSheet() {
            data object PrimaryButtonPressed : Cancel()
            data object SecondaryButtonPressed : Cancel()
        }
    }
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchModule(val moduleRoute: ModuleRoute) : Navigation()
        data class SwitchScreen(val screen: String) : Navigation()

        data object Pop : Navigation()
        data object Finish : Navigation()
    }

    data object ShowBottomSheet : Effect()
    data object CloseBottomSheet : Effect()
}

@KoinViewModel
class PinViewModel(
    private val interactor: QuickPinInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
    @InjectedParam private val pinFlow: PinFlow
) : MviViewModel<Event, State, Effect>() {

    private var enteredPin: SecurePin? = null
    private var lockoutTickJob: Job? = null

    override fun setInitialState(): State {
        val title: String
        val subtitle: String
        val pinState: PinValidationState
        val buttonText: String

        when (pinFlow) {
            PinFlow.CREATE_WITH_ACTIVATION, PinFlow.CREATE_WITHOUT_ACTIVATION -> {
                title = resourceProvider.getString(R.string.quick_pin_create_title)
                subtitle = resourceProvider.getString(R.string.quick_pin_create_enter_subtitle)
                pinState = PinValidationState.ENTER
                buttonText = calculateButtonText(pinState)
            }

            PinFlow.UPDATE -> {
                title = resourceProvider.getString(R.string.quick_pin_change_title)
                subtitle =
                    resourceProvider.getString(R.string.quick_pin_change_validate_current_subtitle)
                pinState = PinValidationState.VALIDATE
                buttonText = calculateButtonText(pinState)
            }
        }

        return State(
            isLoading = false,
            title = title,
            subtitle = subtitle,
            pinState = pinState,
            buttonText = buttonText,
            pinFlow = pinFlow
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                if (viewState.value.pinState != PinValidationState.VALIDATE) return
                viewModelScope.launch {
                    when (val lockoutState = interactor.getPinLockoutState()) {
                        is PinLockoutState.Active -> startLockoutTick(lockoutState.remaining.inWholeMilliseconds)
                        PinLockoutState.Idle -> Unit
                    }
                }
            }

            is Event.OnQuickPinLengthChanged -> {
                if (viewState.value.isLockedOut) return
                validatePinLength(event.length)
            }

            is Event.NextButtonPressed -> {
                val state = viewState.value
                if (state.isLockedOut) {
                    event.pin.close()
                    return
                }

                when (state.pinState) {
                    PinValidationState.ENTER -> {
                        // Set state for re-enter phase
                        setupReenterPhase(enteredPin = event.pin)
                    }

                    PinValidationState.REENTER -> {
                        // Save the new pin
                        saveNewPin(newPin = event.pin)
                    }

                    PinValidationState.VALIDATE -> {
                        validatePin(currentPin = event.pin)
                    }
                }
            }

            is Event.CancelPressed -> {
                clearPendingPin()
                showBottomSheet()
            }

            is Event.BottomSheet.UpdateBottomSheetState -> {
                setState {
                    copy(isBottomSheetOpen = event.isOpen)
                }
            }

            is Event.BottomSheet.Cancel.PrimaryButtonPressed -> {
                hideBottomSheet()
            }

            is Event.BottomSheet.Cancel.SecondaryButtonPressed -> {
                viewModelScope.launch {
                    clearPendingPin()
                    hideBottomSheet()
                    delay(200L)
                    setEffect { Effect.Navigation.Pop }
                }
            }

            is Event.Finish -> {
                clearPendingPin()
                setEffect { Effect.Navigation.Finish }
            }
        }
    }

    override fun onCleared() {
        clearPendingPin()
        lockoutTickJob?.cancel()
        lockoutTickJob = null
        super.onCleared()
    }

    private fun validatePin(currentPin: SecurePin) {
        setState { copy(isLoading = true) }
        viewModelScope.launch {
            interactor.isCurrentPinValid(
                pin = currentPin
            ).collect {
                when (it) {
                    is QuickPinInteractorPinValidPartialState.Failed -> {
                        when (val lockoutState = interactor.recordPinFailure()) {
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

                    QuickPinInteractorPinValidPartialState.Success -> {
                        interactor.resetPinThrottle()
                        stopLockoutTick()
                        setupEnterPhase()
                    }
                }
            }
        }
    }

    private fun setupEnterPhase() {
        val newPinState = PinValidationState.ENTER
        setState {
            copy(
                quickPinError = null,
                pinState = newPinState,
                buttonText = calculateButtonText(newPinState),
                isButtonEnabled = false,
                resetPin = true,
                subtitle = calculateSubtitle(newPinState),
                isLoading = false
            )
        }
    }

    private fun setupReenterPhase(enteredPin: SecurePin) {
        val newPinState = PinValidationState.REENTER
        replacePendingPin(enteredPin)
        setState {
            copy(
                quickPinError = null,
                pinState = PinValidationState.REENTER,
                buttonText = calculateButtonText(newPinState),
                isButtonEnabled = false,
                resetPin = true,
                subtitle = calculateSubtitle(newPinState),
                isLoading = false
            )
        }
    }

    private fun saveNewPin(newPin: SecurePin) {

        val initialPin = enteredPin ?: run {
            newPin.close()
            return
        }

        setState { copy(isLoading = true) }

        viewModelScope.launch {
            interactor.setPin(
                newPin = newPin,
                initialPin = initialPin
            ).collect {
                when (it) {
                    is QuickPinInteractorSetPinPartialState.Failed -> {
                        setState {
                            copy(
                                quickPinError = it.errorMessage,
                                isLoading = false
                            )
                        }
                    }

                    is QuickPinInteractorSetPinPartialState.Success -> {
                        clearPendingPin()
                        setEffect {
                            Effect.Navigation.SwitchScreen(getNextScreenRoute())
                        }
                    }
                }
            }
        }
    }

    private fun validatePinLength(length: Int) {
        setState {
            copy(
                isButtonEnabled = length == quickPinSize && !isLockedOut,
                quickPinError = null,
                resetPin = false
            )
        }
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
                isButtonEnabled = false,
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
            interactor.maxFailedPinAttempts,
            mmss
        )
    }

    private fun calculateSubtitle(pinState: PinValidationState): String {
        return when (pinFlow) {
            PinFlow.UPDATE -> {
                when (pinState) {
                    PinValidationState.ENTER -> resourceProvider.getString(R.string.quick_pin_change_enter_new_subtitle)
                    PinValidationState.REENTER -> resourceProvider.getString(R.string.quick_pin_change_reenter_new_subtitle)
                    PinValidationState.VALIDATE -> resourceProvider.getString(R.string.quick_pin_change_validate_current_subtitle)
                }
            }

            PinFlow.CREATE_WITH_ACTIVATION, PinFlow.CREATE_WITHOUT_ACTIVATION -> {
                when (pinState) {
                    PinValidationState.ENTER -> resourceProvider.getString(R.string.quick_pin_create_enter_subtitle)
                    PinValidationState.REENTER -> resourceProvider.getString(R.string.quick_pin_create_reenter_subtitle)
                    PinValidationState.VALIDATE -> viewState.value.subtitle
                }
            }
        }
    }

    private fun calculateButtonText(pinState: PinValidationState): String {
        return when (pinState) {
            PinValidationState.ENTER -> resourceProvider.getString(R.string.generic_next_capitalized)
            PinValidationState.REENTER -> resourceProvider.getString(R.string.generic_confirm_capitalized)
            PinValidationState.VALIDATE -> resourceProvider.getString(R.string.generic_next_capitalized)
        }
    }

    private fun getNextScreenRoute(): String {

        val navigationAfterCreate = ConfigNavigation(
            navigationType = NavigationType.PushScreen(
                screen = IssuanceScreens.AddDocument,
                arguments = mapOf(
                    IssuanceUiConfig.serializedKeyName to uiSerializer.toBase64(
                        model = IssuanceUiConfig(
                            flowType = IssuanceFlowType.NoDocument
                        ),
                        parser = IssuanceUiConfig.Parser
                    )
                ),
                popUpToScreen = CommonScreens.QuickPin
            ),
        )

        val navigationAfterUpdate = ConfigNavigation(
            navigationType = NavigationType.PopTo(DashboardScreens.Dashboard),
        )

        val navigationAfterCreateNoActivation = ConfigNavigation(
            navigationType = NavigationType.PushScreen(
                screen = DashboardScreens.Dashboard,
                popUpToScreen = CommonScreens.QuickPin
            ),
        )

        return generateComposableNavigationLink(
            screen = CommonScreens.Success,
            arguments = generateComposableArguments(
                mapOf(
                    SuccessUIConfig.serializedKeyName to uiSerializer.toBase64(
                        SuccessUIConfig(
                            textElementsConfig = SuccessUIConfig.TextElementsConfig(
                                text = when (pinFlow) {
                                    PinFlow.CREATE_WITH_ACTIVATION, PinFlow.CREATE_WITHOUT_ACTIVATION -> resourceProvider.getString(
                                        R.string.quick_pin_create_success_text
                                    )

                                    PinFlow.UPDATE -> resourceProvider.getString(R.string.quick_pin_change_success_text)
                                },
                                description = when (pinFlow) {
                                    PinFlow.CREATE_WITH_ACTIVATION -> resourceProvider.getString(R.string.quick_pin_create_success_description)
                                    PinFlow.CREATE_WITHOUT_ACTIVATION -> resourceProvider.getString(
                                        R.string.quick_pin_create_success_no_activation_description
                                    )

                                    PinFlow.UPDATE -> resourceProvider.getString(R.string.quick_pin_change_success_description)
                                }
                            ),
                            imageConfig = when (pinFlow) {
                                PinFlow.CREATE_WITH_ACTIVATION, PinFlow.CREATE_WITHOUT_ACTIVATION -> SuccessUIConfig.ImageConfig(
                                    type = SuccessUIConfig.ImageConfig.Type.Drawable(
                                        icon = AppIcons.WalletSecured
                                    ),
                                    tint = null,
                                )

                                PinFlow.UPDATE -> SuccessUIConfig.ImageConfig()
                            },
                            buttonConfig = listOf(
                                SuccessUIConfig.ButtonConfig(
                                    text = when (pinFlow) {
                                        PinFlow.CREATE_WITH_ACTIVATION -> resourceProvider.getString(
                                            R.string.quick_pin_create_success_btn
                                        )

                                        PinFlow.CREATE_WITHOUT_ACTIVATION -> resourceProvider.getString(
                                            R.string.quick_pin_create_success_no_activation_btn
                                        )

                                        PinFlow.UPDATE -> resourceProvider.getString(R.string.quick_pin_change_success_btn)
                                    },
                                    style = SuccessUIConfig.ButtonConfig.Style.PRIMARY,
                                    navigation = when (pinFlow) {
                                        PinFlow.CREATE_WITH_ACTIVATION -> navigationAfterCreate
                                        PinFlow.CREATE_WITHOUT_ACTIVATION -> navigationAfterCreateNoActivation
                                        PinFlow.UPDATE -> navigationAfterUpdate
                                    }
                                )
                            ),
                            onBackScreenToNavigate = when (pinFlow) {
                                PinFlow.CREATE_WITH_ACTIVATION -> navigationAfterCreate
                                PinFlow.CREATE_WITHOUT_ACTIVATION -> navigationAfterCreateNoActivation
                                PinFlow.UPDATE -> navigationAfterUpdate
                            },
                        ),
                        SuccessUIConfig.Parser
                    ).orEmpty()
                )
            )
        )
    }

    private fun showBottomSheet() {
        setEffect {
            Effect.ShowBottomSheet
        }
    }

    private fun hideBottomSheet() {
        setEffect {
            Effect.CloseBottomSheet
        }
    }

    private fun replacePendingPin(pin: SecurePin) {
        clearPendingPin()
        enteredPin = pin
    }

    private fun clearPendingPin() {
        enteredPin?.close()
        enteredPin = null
    }
}