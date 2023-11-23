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

package eu.europa.ec.loginfeature.ui.pin

import androidx.core.text.isDigitsOnly
import androidx.lifecycle.viewModelScope
import eu.europa.ec.businesslogic.validator.Form
import eu.europa.ec.businesslogic.validator.FormValidationResult
import eu.europa.ec.businesslogic.validator.Rule
import eu.europa.ec.commonfeature.config.SuccessUIConfig
import eu.europa.ec.commonfeature.interactor.QuickPinInteractor
import eu.europa.ec.commonfeature.interactor.QuickPinInteractorPinValidPartialState
import eu.europa.ec.commonfeature.interactor.QuickPinInteractorSetPinPartialState
import eu.europa.ec.commonfeature.model.PinFlows
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.ModuleRoute
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam


enum class PinValidationState {
    REENTER,
    VALIDATE,
    COMPLETED
}

sealed class Event : ViewEvent {
    data class NextButtonPressed(
        val pin: String
    ) : Event()

    data class OnQuickPinEntered(val quickPin: String) : Event()
    data object Init : Event()
}

data class State(
    val isLoading: Boolean = false,
    val isButtonEnabled: Boolean = false,
    val isBackable: Boolean = true,
    val quickPinError: String? = null,
    val validationResult: FormValidationResult = FormValidationResult(false),
    val subtitle: String = "",
    val title: String = "",
    val pin: String = "",
    val initialPin: String = "",
    val resetPin: Boolean = false,
    val pinState: PinValidationState = PinValidationState.REENTER,

    ) : ViewState

sealed class Effect : ViewSideEffect {

    data object InitializeQuickPinOnCreate : Effect()

    sealed class Navigation : Effect() {
        data class SwitchModule(val moduleRoute: ModuleRoute) : Navigation()
        data class SwitchScreen(val screen: String) : Navigation()
    }
}


@KoinViewModel
class PinViewModel(
    private val interactor: QuickPinInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
    @InjectedParam private val pinFlows: String
) :
    MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State {

        var title = ""
        var subtitle = ""
        var pinState = PinValidationState.REENTER

        when (pinFlows) {
            PinFlows.CREATE.type -> {
                title = resourceProvider.getString(R.string.quick_pin_setup_title)
                subtitle = resourceProvider.getString(R.string.quick_pin_setup_subtitle)
                pinState = PinValidationState.REENTER
            }

            PinFlows.UPDATE.type -> {
                title = resourceProvider.getString(R.string.quick_pin_change_title)
                subtitle = resourceProvider.getString(R.string.quick_pin_change_current_subtitle)
                pinState = PinValidationState.VALIDATE
            }
        }
        return State(
            isLoading = false,
            title = title,
            subtitle = subtitle,
            pinState = pinState
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                setEffect {
                    Effect.InitializeQuickPinOnCreate
                }
            }

            is Event.OnQuickPinEntered -> {
                validateForm(event.quickPin)
            }

            is Event.NextButtonPressed -> {
                val state = viewState.value

                when (pinFlows) {
                    PinFlows.CREATE.type -> createPin(state, event)
                    PinFlows.UPDATE.type -> updatePin(state, event)
                }
            }
        }
    }

    private fun getListOfRules(pin: String): Form {
        pin.isDigitsOnly()
        return Form(
            mapOf(
                listOf(
                    Rule.ValidateStringRange(4..4, ""),
                    Rule.ValidateRegex("-?\\d+(\\.\\d+)?".toRegex(), "only numerical")
                ) to pin
            )
        )
    }

    private fun validateForm(pin: String) {
        viewModelScope.launch {
            interactor.validateForm(getListOfRules(pin)).collect {
                setState {
                    copy(
                        validationResult = it,
                        isButtonEnabled = it.isValid,
                        quickPinError = it.message,
                        pin = pin,
                        resetPin = false
                    )
                }
            }
        }
    }

    private fun createPin(state: State, event: Event.NextButtonPressed) {

        if (state.pinState == PinValidationState.COMPLETED) {
            viewModelScope.launch {
                interactor.setPin(state.pin, state.initialPin).collect {
                    when (it) {
                        is QuickPinInteractorSetPinPartialState.Failed -> {
                            setState {
                                copy(
                                    quickPinError = it.errorMessage
                                )
                            }
                        }

                        is QuickPinInteractorSetPinPartialState.Success -> {
                            setEffect {
                                Effect.Navigation.SwitchScreen(getSuccessConfig())

                            }
                        }
                    }
                }
            }
        } else if (state.pinState == PinValidationState.REENTER) {
            setState {
                copy(
                    quickPinError = null,
                    initialPin = event.pin,
                    pinState = PinValidationState.COMPLETED,
                    pin = "",
                    resetPin = true
                )
            }
        }
    }

    private fun updatePin(state: State, event: Event.NextButtonPressed) {

        viewModelScope.launch {
            when (state.pinState) {
                PinValidationState.VALIDATE -> {
                    interactor.isCurrentPinValid(state.pin).collect {
                        when (it) {
                            is QuickPinInteractorPinValidPartialState.Failed -> {
                                setState {
                                    copy(
                                        quickPinError = it.errorMessage
                                    )
                                }
                            }

                            is QuickPinInteractorPinValidPartialState.Success -> {
                                setState {
                                    copy(
                                        quickPinError = null,
                                        pinState = PinValidationState.REENTER,
                                        pin = "",
                                        resetPin = true,
                                        subtitle = resourceProvider.getString(R.string.quick_pin_change_subtitle)
                                    )
                                }
                            }

                        }
                    }
                }

                PinValidationState.REENTER -> {
                    if (state.initialPin.isBlank()) {
                        setState {
                            copy(
                                quickPinError = null,
                                initialPin = event.pin,
                                pinState = PinValidationState.COMPLETED,
                                pin = "",
                                resetPin = true,
                                subtitle = resourceProvider.getString(R.string.quick_pin_reenter_subtitle)
                            )
                        }
                    }
                }

                PinValidationState.COMPLETED -> {
                    interactor.isPinMatched(state.initialPin, state.pin).collect {
                        when (it) {
                            is QuickPinInteractorPinValidPartialState.Failed -> {
                                setState {
                                    copy(
                                        quickPinError = it.errorMessage
                                    )
                                }
                            }

                            QuickPinInteractorPinValidPartialState.Success -> {
                                interactor.changePin(state.pin).collect {
                                    when (it) {
                                        is QuickPinInteractorSetPinPartialState.Failed -> {
                                            setState {
                                                copy(
                                                    quickPinError = it.errorMessage
                                                )
                                            }
                                        }

                                        is QuickPinInteractorSetPinPartialState.Success -> {
                                            setEffect {
                                                Effect.Navigation.SwitchScreen(
                                                    DashboardScreens.Dashboard.screenRoute
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getSuccessConfig(): String {
        return generateComposableNavigationLink(
            screen = CommonScreens.Success,
            arguments = generateComposableArguments(
                mapOf(
                    SuccessUIConfig.serializedKeyName to uiSerializer.toBase64(
                        SuccessUIConfig(
                            header = resourceProvider.getString(R.string.content_description_success_screen_title),
                            content = resourceProvider.getString(R.string.content_description_success_screen_subtitle),
                            imageConfig = SuccessUIConfig.ImageConfig(
                                type = SuccessUIConfig.ImageConfig.Type.DEFAULT
                            ),
                            buttonConfig = listOf(
                                SuccessUIConfig.ButtonConfig(
                                    text = resourceProvider.getString(R.string.quick_pin_continue),
                                    style = SuccessUIConfig.ButtonConfig.Style.PRIMARY,
                                    navigation = ConfigNavigation(
                                        navigationType = NavigationType.PUSH,
                                        screenToNavigate = DashboardScreens.Dashboard
                                    )
                                )
                            ),
                            onBackScreenToNavigate = ConfigNavigation(
                                navigationType = NavigationType.POP,
                                screenToNavigate = DashboardScreens.Dashboard
                            ),
                        ),
                        SuccessUIConfig.Parser
                    ).orEmpty()
                )
            )
        )
    }

}