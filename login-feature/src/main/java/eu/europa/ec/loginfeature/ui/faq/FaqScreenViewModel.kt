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

package eu.europa.ec.loginfeature.ui.faq

import android.util.Log
import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.config.SuccessUIConfig
import eu.europa.ec.loginfeature.interactor.LoginInteractor
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

data class State(val faqItems: List<CollapsableSection>) : ViewState

data class CollapsableSection(val title: String, val description: String)


sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchModule(val moduleRoute: ModuleRoute) : Navigation()
        data class SwitchScreen(val screen: String) : Navigation()
    }
}

@KoinViewModel
class FaqScreenViewModel(
    private val interactor: LoginInteractor,
    private val uiSerializer: UiSerializer
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State = State(
        listOf(
            CollapsableSection(
                title = "Question A goes Here",
                description = "Lorem ipsum dolor sit amet," +
                        " consectetur adipiscing elit,"
            ),
            CollapsableSection(
                title = "Question B goes Here",
                description = "Duis aute irure dolor in reprehenderit in" +
                        " voluptate velit esse cillum dolore eu fugiat nulla pariatur."
            ),
            CollapsableSection(
                title = "Question C goes Here",
                description = "Excepteur sint occaecat cupidatat non proident, " +
                        "sunt in culpa qui officia deserunt mollit anim id est laborum."
            ),
            CollapsableSection(
                title = "Question D goes Here",
                description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
                        "sed  magn laboris nisi ut aliquip ex ea commodo consequat."
            ),
            CollapsableSection(
                title = "Question E goes Here",
                description = "Duis aute irure dolor in reprehenderit" +
                        " in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
            ),
            CollapsableSection(
                title = "Question F goes Here",
                description = "Excepteur sint occaecat cupidatat non proident, " +
                        "sunt in culpa qui officia deserunt mollit anim id est laborum."
            ),
        )
    )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.OnClick -> testNewScreen()
        }
    }

    private fun testNewScreen() {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screen = generateComposableNavigationLink(
                    screen = CommonScreens.Success,
                    arguments = generateComposableArguments(
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
                    )
                )
            )
        }
    }

    private fun testNetworkCall() {
        viewModelScope.launch {
            interactor.test().collect {
                Log.d(FaqScreenViewModel::class.java.name, it.toString())
            }
        }
    }
}