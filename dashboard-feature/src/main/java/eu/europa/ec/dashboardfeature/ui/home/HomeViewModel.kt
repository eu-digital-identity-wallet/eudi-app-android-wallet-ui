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

package eu.europa.ec.dashboardfeature.ui.home

import eu.europa.ec.resourceslogic.R
import eu.europa.ec.dashboardfeature.interactor.HomeInteractor
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.wrap.ActionCardConfig
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import org.koin.android.annotation.KoinViewModel

data class State(
    val welcomeUserMessage: String,
    val authenticateCardConfig: ActionCardConfig,
    val signCardConfig: ActionCardConfig,
) : ViewState

sealed class Event : ViewEvent {
    data object AuthenticatePressed : Event()
    data object SignPressed : Event()
    data object LearnMorePressed : Event()
}

sealed class Effect : ViewSideEffect

@KoinViewModel
class HomeViewModel(
    val interactor: HomeInteractor,
    private val resourceProvider: ResourceProvider
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State {
        // TODO user first name will be needed
        return State(
            welcomeUserMessage = resourceProvider.getString(
                R.string.home_screen_welcome_user_message,
                "Alex"
            ),
            authenticateCardConfig = ActionCardConfig(
                title = resourceProvider.getString(R.string.home_screen_authentication_card_title),
                icon = AppIcons.WalletActivated,
                primaryButtonText = resourceProvider.getString(R.string.home_screen_authenticate),
                secondaryButtonText = resourceProvider.getString(R.string.home_screen_learn_more)
            ),
            signCardConfig = ActionCardConfig(
                title = resourceProvider.getString(R.string.home_screen_sign_card_title),
                icon = AppIcons.Contract,
                primaryButtonText = resourceProvider.getString(R.string.home_screen_sign),
                secondaryButtonText = resourceProvider.getString(R.string.home_screen_learn_more)
            ),
        )
    }

    override fun handleEvents(event: Event) {
    }
}