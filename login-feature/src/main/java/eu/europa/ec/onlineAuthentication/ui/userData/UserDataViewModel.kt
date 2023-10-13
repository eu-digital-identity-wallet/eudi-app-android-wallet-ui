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

package eu.europa.ec.onlineAuthentication.ui.userData

import androidx.lifecycle.viewModelScope
import eu.europa.ec.onlineAuthentication.interactor.OnlineAuthenticationInteractor
import eu.europa.ec.onlineAuthentication.interactor.OnlineAuthenticationInteractorPartialState
import eu.europa.ec.onlineAuthentication.model.UserDataDomain
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.OnlineAuthenticationScreens
import eu.europa.ec.uilogic.navigation.StartupScreens
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

data class UserDataError(
    val event: Event,
    val errorMsg: String
)

data class State(
    val isLoading: Boolean = true,
    val error: UserDataError? = null,
    val isBottomSheetOpen: Boolean = false,

    val screenTitle: String,
    val screenSubtitle: String,

    val userId: String,
    val userData: UserDataDomain? = null,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object DismissError : Event()
    data object GoBack : Event()
    data object PrimaryButtonPressed : Event()
    data object SecondaryButtonPressed : Event()
    data class UpdateBottomSheetState(val isOpen: Boolean) : Event()
    data object SheetPrimaryButtonPressed : Event()
    data object SheetSecondaryButtonPressed : Event()
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
    }

    data object ShowBottomSheet : Effect()
    data object CloseBottomSheet : Effect()
}

@KoinViewModel
class UserDataViewModel(
    private val interactor: OnlineAuthenticationInteractor,
    private val resourceProvider: ResourceProvider,
    @InjectedParam private val userId: String
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State {
        val title = resourceProvider.getString(R.string.online_authentication_userData_title)
        val subtitle = resourceProvider.getString(R.string.online_authentication_userData_subtitle)
        return State(
            userId = userId,
            screenTitle = title,
            screenSubtitle = subtitle
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                val userId = viewState.value.userId
                fetchUserData(userId, event)
            }

            is Event.DismissError -> {
                setState {
                    copy(error = null)
                }
            }

            is Event.GoBack -> {
                goBackToSplashScreen()
            }

            is Event.PrimaryButtonPressed -> {
                setEffect {
                    Effect.Navigation.SwitchScreen(
                        screenRoute = OnlineAuthenticationScreens.Loading.screenRoute
                    )
                }
            }

            is Event.SecondaryButtonPressed -> {
                showBottomSheet()
            }

            is Event.UpdateBottomSheetState -> {
                setState {
                    copy(isBottomSheetOpen = event.isOpen)
                }
            }

            is Event.SheetPrimaryButtonPressed -> {
                hideBottomSheet()
            }

            is Event.SheetSecondaryButtonPressed -> {
                hideBottomSheet()
                goBackToSplashScreen()
            }
        }
    }

    private fun fetchUserData(userId: String, event: Event) {
        setState {
            copy(
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            interactor.getUserData(userId = userId).collect { response ->
                when (response) {
                    is OnlineAuthenticationInteractorPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = UserDataError(
                                    event = event,
                                    errorMsg = response.error
                                )
                            )
                        }
                    }

                    is OnlineAuthenticationInteractorPartialState.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                userData = response.userDataDomain
                            )
                        }
                    }
                }
            }
        }
    }

    private fun goBackToSplashScreen() {
        setEffect {
            Effect.Navigation.PopBackStackUpTo(
                screenRoute = StartupScreens.Splash.screenRoute,
                inclusive = false
            )
        }
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

}