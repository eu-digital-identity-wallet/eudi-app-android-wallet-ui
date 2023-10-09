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

package eu.europa.ec.startupfeature.ui

import android.util.Log
import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.config.BiometricUiConfig
import eu.europa.ec.commonfeature.config.SuccessUIConfig
import eu.europa.ec.startupfeature.interactor.StartupInteractor
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.ModuleRoute
import eu.europa.ec.uilogic.navigation.StartupScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

sealed class Event : ViewEvent {
    data object OnClick : Event()
}

object State : ViewState

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchModule(val moduleRoute: ModuleRoute) : Navigation()
        data class SwitchScreen(val screen: String) : Navigation()
    }
}

@KoinViewModel
class StartupViewModel(
    private val interactor: StartupInteractor,
    private val uiSerializer: UiSerializer
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State = State

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.OnClick -> testBiometricScreen()
        }
    }

    private fun testBiometricScreen() {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screen = generateComposableNavigationLink(
                    screen = CommonScreens.Biometric,
                    arguments = generateComposableArguments(
                        mapOf(
                            "biometricConfig" to uiSerializer.toBase64(
                                BiometricUiConfig(
                                    title = "dsfsdfsdf",
                                    subTitle = "dsfsdfsdfsdfsf",
                                    quickPinOnlySubTitle = "dsfsdfsdfsdf",
                                    isPreAuthorization = true,
                                    shouldInitializeBiometricAuthOnCreate = true,
                                    onSuccessNavigation = ConfigNavigation(
                                        navigationType = NavigationType.PUSH,
                                        screenToNavigate = CommonScreens.Success,
                                        arguments = getSuccessConfig()
                                    ),
                                    onBackNavigation = ConfigNavigation(
                                        navigationType = NavigationType.PUSH,
                                        screenToNavigate = StartupScreens.Splash
                                    )
                                ),
                                BiometricUiConfig.Parser
                            ).orEmpty()
                        )
                    )
                )
            )
        }
    }

    private fun testSuccessScreen() {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screen = generateComposableNavigationLink(
                    screen = CommonScreens.Success,
                    arguments = getSuccessScreen()
                )
            )
        }
    }

    private fun getSuccessScreen(): String = generateComposableNavigationLink(
        screen = CommonScreens.Success,
        arguments = generateComposableArguments(
            getSuccessConfig()
        )
    )

    private fun getSuccessConfig(): Map<String, String> =
        mapOf(
            "successConfig" to uiSerializer.toBase64(
                SuccessUIConfig(
                    header = "Success",
                    content = "dsfsdfsdfsdfsdfsdfsdfsdfsdf",
                    imageConfig = SuccessUIConfig.ImageConfig(
                        type = SuccessUIConfig.ImageConfig.Type.DEFAULT
                    ),
                    buttonConfig = listOf(
                        SuccessUIConfig.ButtonConfig(
                            text = "back",
                            style = SuccessUIConfig.ButtonConfig.Style.PRIMARY,
                            navigation = ConfigNavigation(
                                navigationType = NavigationType.POP,
                                screenToNavigate = StartupScreens.Splash
                            )
                        )
                    ),
                    onBackScreenToNavigate = ConfigNavigation(
                        navigationType = NavigationType.POP,
                        screenToNavigate = StartupScreens.Splash
                    ),
                ),
                SuccessUIConfig.Parser
            ).orEmpty()
        )

    private fun testNetworkCall() {
        viewModelScope.launch {
            interactor.test().collect {
                Log.d(StartupViewModel::class.java.name, it.toString())
            }
        }
    }
}