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

package eu.europa.ec.loginfeature.ui.welcome

import eu.europa.ec.commonfeature.model.PinFlow
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.LoginScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import org.koin.android.annotation.KoinViewModel

data class State(
    val showContent: Boolean = false,
    val enterAnimationDelay: Int = 350,
    val enterAnimationDuration: Int = 750
) : ViewState

sealed class Event : ViewEvent {
    data object NavigateToLogin : Event()
    data object NavigateToFaq : Event()
}

sealed class Effect : ViewSideEffect {

    sealed class Navigation : Effect() {
        data class SwitchScreen(val screenRoute: String, val currentInclusive: Boolean) :
            Navigation()
    }
}

@KoinViewModel
class WelcomeViewModel : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State = State()

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.NavigateToLogin -> navigateTo(
                route = generateComposableNavigationLink(
                    screen = CommonScreens.QuickPin,
                    arguments = generateComposableArguments(mapOf("pinFlow" to PinFlow.CREATE))
                )
            )

            is Event.NavigateToFaq -> navigateTo(LoginScreens.Faq.screenRoute)
        }
    }

    private fun navigateTo(route: String, currentInclusive: Boolean = false) {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = route,
                currentInclusive = currentInclusive
            )
        }
    }
}