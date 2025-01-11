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

import eu.europa.ec.dashboardfeature.interactor.HomeInteractor
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.wrap.ActionCardConfig
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import org.koin.android.annotation.KoinViewModel

data class State(
    val isBottomSheetOpen: Boolean = false,
    val sheetContent: HomeScreenBottomSheetContent = HomeScreenBottomSheetContent.Authenticate,

    // TODO first name will be needed
    val userFirstName: String = "",
    val welcomeUserMessage: String,
    val authenticateCardConfig: ActionCardConfig,
    val signCardConfig: ActionCardConfig,
) : ViewState

sealed class Event : ViewEvent {
    sealed class AuthenticateCard : Event() {
        data object AuthenticatePressed : Event()
        data object LearnMorePressed : Event()
    }

    sealed class SignDocumentCard : Event() {
        data object SignDocumentPressed : Event()
        data object LearnMorePressed : Event()
    }

    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(val isOpen: Boolean) : BottomSheet()
        data object Close : BottomSheet()

        sealed class Authenticate : BottomSheet() {
            data object OpenAuthenticateInPerson : Authenticate()
            data object OpenAuthenticateOnLine : Authenticate()
        }

        sealed class SignDocument : BottomSheet() {
            data object OpenDocumentFromDevice : SignDocument()
            data object OpenDocumentFromQr : SignDocument()
        }
    }
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(
            val screenRoute: String,
            val popUpToScreenRoute: String = "",
            val inclusive: Boolean = false,
        ) : Navigation()
    }

    data object ShowBottomSheet : Effect()
    data object CloseBottomSheet : Effect()
}

sealed class HomeScreenBottomSheetContent {
    data object Authenticate : HomeScreenBottomSheetContent()
    data object LearnMoreAboutAuthenticate : HomeScreenBottomSheetContent()
    data object Sign : HomeScreenBottomSheetContent()
    data object LearnMoreAboutSignDocument : HomeScreenBottomSheetContent()
}


@KoinViewModel
class HomeViewModel(
    val interactor: HomeInteractor,
    private val resourceProvider: ResourceProvider
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State {
        return State(
            welcomeUserMessage = resourceProvider.getString(R.string.home_screen_welcome),
            authenticateCardConfig = ActionCardConfig(
                title = resourceProvider.getString(R.string.home_screen_authentication_card_title),
                icon = AppIcons.IdCards,
                primaryButtonText = resourceProvider.getString(R.string.home_screen_authenticate),
                secondaryButtonText = resourceProvider.getString(R.string.home_screen_learn_more)
            ),
            signCardConfig = ActionCardConfig(
                title = resourceProvider.getString(R.string.home_screen_sign_card_title),
                icon = AppIcons.Contract,
                primaryButtonText = resourceProvider.getString(R.string.home_screen_sign),
                secondaryButtonText = resourceProvider.getString(R.string.home_screen_learn_more)
            )
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.AuthenticateCard.AuthenticatePressed -> showBottomSheet(
                sheetContent = HomeScreenBottomSheetContent.Authenticate
            )

            is Event.AuthenticateCard.LearnMorePressed -> showBottomSheet(
                sheetContent = HomeScreenBottomSheetContent.LearnMoreAboutAuthenticate
            )

            is Event.SignDocumentCard.SignDocumentPressed -> showBottomSheet(
                sheetContent = HomeScreenBottomSheetContent.Sign
            )

            is Event.SignDocumentCard.LearnMorePressed -> showBottomSheet(
                sheetContent = HomeScreenBottomSheetContent.LearnMoreAboutSignDocument
            )

            is Event.BottomSheet.UpdateBottomSheetState -> {
                setState {
                    copy(isBottomSheetOpen = event.isOpen)
                }
            }

            is Event.BottomSheet.Close -> {
                hideBottomSheet()
            }

            is Event.BottomSheet.Authenticate.OpenAuthenticateInPerson -> TODO()
            is Event.BottomSheet.Authenticate.OpenAuthenticateOnLine -> TODO()

            is Event.BottomSheet.SignDocument.OpenDocumentFromDevice -> navigateToDocumentSign()
            is Event.BottomSheet.SignDocument.OpenDocumentFromQr -> {
                // no action
            }
        }
    }

    private fun navigateToDocumentSign() {
        // navigate to sign document
    }

    private fun showBottomSheet(sheetContent: HomeScreenBottomSheetContent) {
        setState {
            copy(sheetContent = sheetContent)
        }
        setEffect {
            Effect.ShowBottomSheet
        }
    }

    private fun hideBottomSheet() {
        setEffect {
            Effect.CloseBottomSheet
        }
    }
}