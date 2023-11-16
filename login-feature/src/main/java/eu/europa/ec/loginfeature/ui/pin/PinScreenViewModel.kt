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

import androidx.lifecycle.viewModelScope
import eu.europa.ec.businesslogic.validator.Form
import eu.europa.ec.businesslogic.validator.FormsValidationResult
import eu.europa.ec.businesslogic.validator.Rule
import eu.europa.ec.commonfeature.interactor.QuickPinInteractor
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.ModuleRoute
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class PinScreenViewError(
    val event: Event,
    val errorMsg: String
)


sealed class Event : ViewEvent {

    data class NextButtonPressed(
        val pin: String
    ) : Event()

    data class EnableButton(val enable: Boolean) : Event()
    data class ValidateForm(val pin: String, val rules: List<Rule>) : Event()
    data class CancelButtonPressed(val payload: String) : Event()
    data class SetActionsValue(val payload: String) : Event()
    data object OnErrorDismiss : Event()
    data class OnQuickPinEntered(val quickPin: String) : Event()
    data object Init : Event()
}

data class State(
    val isLoading: Boolean = false,
    val isButtonEnabled: Boolean = false,
    val isBackable: Boolean = true,
    val showTextFieldError: Boolean = false,
    val quickPinError: String? = null,
    val validationResult: FormsValidationResult = FormsValidationResult(false),
    val actions: List<String> = listOf(),
    val error: PinScreenViewError? = null,
    val pin: String = ""
) : ViewState

sealed class Effect : ViewSideEffect {

    data object InitializeQuickPinOnCreate : Effect()

    sealed class Navigation : Effect() {
        data class SwitchModule(val moduleRoute: ModuleRoute) : Navigation()
        data class SwitchScreen(val screen: String) : Navigation()
    }
}


@KoinViewModel
class PinViewModel(private val interactor: QuickPinInteractor) :
    MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State = State(isLoading = false, error = null)

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                //if (biometricUiConfig.shouldInitializeBiometricAuthOnCreate && viewState.value.userBiometricsAreEnabled) {
                setEffect {
                    Effect.InitializeQuickPinOnCreate
                    //    }
                }
            }

            is Event.EnableButton -> {
                setState {
                    copy(
                        isButtonEnabled = event.enable
                    )
                }
            }

            is Event.NextButtonPressed -> {
                setState {
                    copy(isLoading = true, error = null)
                }
//                nextStep(
//                    interactor,
//                    event.payload,
//                    RequestWizardNextStep(
//                        parameters = getWizardNextParameters(event = event)
//                    ),
//                    onFailed = {
//                        setState {
//                            copy(
//                                error = PinScreenViewError(
//                                    event = event,
//                                    errorMsg = it
//                                )
//                            )
//                        }
//                    },
//                    onFinished = {
//                        setState {
//                            copy(isLoading = false)
//                        }
//                    },
//                    onFlowCompleted = {
//                        setEffect {
//                            Effect.Navigation.SwitchModule(ModuleRoute.DashboardModule)
//                        }
//                    },
//                    onScreenReturned = { setEffect { Effect.Navigation.SwitchScreen(it) } }
//                )

            }

            is Event.ValidateForm -> {
                setState {
                    copy(
                        pin = event.pin
                    )
                }
                validateForm(getListOfRules(event), event.pin)
            }

            is Event.CancelButtonPressed -> {
                setState {
                    copy(isLoading = true, error = null)
                }
//                cancelWizard(
//                    interactor,
//                    event.payload,
//                    onFinished = {
//                        setState {
//                            copy(isLoading = false)
//                        }
//                    },
//                    onFlowCanceled = {
//                        setEffect {
//                            Effect.Navigation.SwitchModule(ModuleRoute.StartupModule)
//                        }
//                    }
//                )
            }


            is Event.SetActionsValue -> {
//                parseActions(interactor, event.payload) {
//                    setState {
//                        copy(
//                            actions = it
//                        )
//                    }
//                }
            }


            is Event.OnErrorDismiss -> setState { copy(error = null) }
            else -> {}
        }

    }

    private fun getListOfRules(event: Event.ValidateForm): List<Form> =
        listOf(Form(mapOf(event.rules to event.pin)))

    private fun validateForm(forms: List<Form>, pin: String) {
        viewModelScope.launch {
            interactor.validateForms(forms).collect {
                setState {
                    copy(validationResult = it, isButtonEnabled = it.isValid && pin.length == 4)
                }
            }
        }
    }

}