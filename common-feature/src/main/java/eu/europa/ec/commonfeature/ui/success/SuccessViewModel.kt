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

package eu.europa.ec.commonfeature.ui.success

import eu.europa.ec.commonfeature.config.SuccessUIConfig
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.Screen
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

data class State(
    val successConfig: SuccessUIConfig
) : ViewState

sealed class Event : ViewEvent {
    data class ButtonClicked(val config: SuccessUIConfig.ButtonConfig) : Event()
    data object BackPressed : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(
            val screenRoute: String
        ) : Navigation()

        data class PopBackStackUpTo(
            val screenRoute: String,
            val inclusive: Boolean
        ) : Navigation()

        data class DeepLink(
            val screen: Screen,
            val arguments: String,
            val flags: Int = 0
        ) : Navigation()
    }
}

@KoinViewModel
class SuccessViewModel constructor(
    private val uiSerializer: UiSerializer,
    @InjectedParam private val successConfig: String
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State =
        State(
            successConfig = uiSerializer.fromBase64(
                successConfig,
                SuccessUIConfig::class.java,
                SuccessUIConfig.Parser
            ) ?: throw RuntimeException("SuccessUIConfig:: is Missing or invalid")
        )

    override fun handleEvents(event: Event) {
        when (event) {

            is Event.ButtonClicked -> {
                doNavigation(event.config.navigation)
            }

            is Event.BackPressed -> {
                doNavigation(
                    viewState.value.successConfig.onBackScreenToNavigate
                )
            }
        }
    }

    private fun doNavigation(navigation: ConfigNavigation) {

        val navigationEffect: Effect.Navigation = when (navigation.navigationType) {
            NavigationType.POP -> {
                Effect.Navigation.PopBackStackUpTo(
                    screenRoute = navigation.screenToNavigate.screenRoute,
                    inclusive = false
                )
            }

            NavigationType.PUSH -> {
                Effect.Navigation.SwitchScreen(
                    generateComposableNavigationLink(
                        screen = navigation.screenToNavigate,
                        arguments = generateComposableArguments(navigation.arguments),
                    )
                )
            }

            NavigationType.DEEPLINK -> Effect.Navigation.DeepLink(
                screen = navigation.screenToNavigate,
                arguments = generateComposableArguments(navigation.arguments),
                flags = navigation.flags,
            )
        }

        setEffect {
            navigationEffect
        }
    }
}