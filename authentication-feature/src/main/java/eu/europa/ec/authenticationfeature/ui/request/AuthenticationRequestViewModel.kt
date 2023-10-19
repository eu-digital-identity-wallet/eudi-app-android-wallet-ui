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

package eu.europa.ec.authenticationfeature.ui.request

import androidx.lifecycle.viewModelScope
import eu.europa.ec.authenticationfeature.interactor.AuthenticationInteractor
import eu.europa.ec.authenticationfeature.interactor.AuthenticationInteractorPartialState
import eu.europa.ec.authenticationfeature.ui.request.model.UserDataUi
import eu.europa.ec.authenticationfeature.ui.request.transformer.toUserDataUi
import eu.europa.ec.commonfeature.config.BiometricUiConfig
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.AuthenticationScreens
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class State(
    val isLoading: Boolean = true,
    val isShowingFullUserInfo: Boolean = false,
    val error: ContentErrorConfig? = null,
    val isBottomSheetOpen: Boolean = false,
    val sheetContent: AuthenticationRequestBottomSheetContent = AuthenticationRequestBottomSheetContent.SUBTITLE,

    val screenTitle: String,
    val screenSubtitle: String,
    val screenClickableSubtitle: String,
    val cardText: String,
    val warningText: String,
    val biometrySubtitle: String,
    val quickPinSubtitle: String,

    val userDataUi: List<UserDataUi> = emptyList(),
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object DismissError : Event()
    data object GoBack : Event()
    data object ChangeContentVisibility : Event()
    data class UserDataItemCheckedStatusChanged(
        val items: List<UserDataUi>,
        val itemId: Int
    ) : Event()

    data object SubtitleClicked : Event()
    data object PrimaryButtonPressed : Event()
    data object SecondaryButtonPressed : Event()

    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(val isOpen: Boolean) : Event()
        sealed class Cancel : BottomSheet() {
            data object PrimaryButtonPressed : Event()
            data object SecondaryButtonPressed : Event()
        }

        sealed class Subtitle : BottomSheet() {
            data object PrimaryButtonPressed : Event()
        }
    }
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(
            val screenRoute: String
        ) : Navigation()

        data object Pop : Navigation()
    }

    data object ShowBottomSheet : Effect()
    data object CloseBottomSheet : Effect()
}

enum class AuthenticationRequestBottomSheetContent {
    SUBTITLE, CANCEL
}

@KoinViewModel
class AuthenticationRequestViewModel(
    private val interactor: AuthenticationInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State {
        return State(
            screenTitle = resourceProvider.getString(R.string.online_authentication_userData_title),
            screenSubtitle = resourceProvider.getString(R.string.online_authentication_userData_subtitle_one),
            screenClickableSubtitle = resourceProvider.getString(R.string.online_authentication_userData_subtitle_two),
            cardText = resourceProvider.getString(R.string.online_authentication_userData_card_text),
            warningText = resourceProvider.getString(R.string.online_authentication_userData_warning_text),
            biometrySubtitle = resourceProvider.getString(R.string.online_authentication_biometry_share_subtitle),
            quickPinSubtitle = resourceProvider.getString(R.string.online_authentication_quick_pin_share_subtitle)
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                fetchUserData(event)
            }

            is Event.DismissError -> {
                setState {
                    copy(error = null)
                }
            }

            is Event.GoBack -> {
                setState {
                    copy(error = null)
                }
                setEffect {
                    Effect.Navigation.Pop
                }
            }

            is Event.ChangeContentVisibility -> {
                setState {
                    copy(isShowingFullUserInfo = !isShowingFullUserInfo)
                }
            }

            is Event.UserDataItemCheckedStatusChanged -> {
                updateUserDataItem(items = event.items, id = event.itemId)
            }

            is Event.SubtitleClicked -> {
                showBottomSheet(sheetContent = AuthenticationRequestBottomSheetContent.SUBTITLE)
            }

            is Event.PrimaryButtonPressed -> {
                setEffect {
                    Effect.Navigation.SwitchScreen(
                        screenRoute = generateComposableNavigationLink(
                            screen = CommonScreens.Biometric,
                            arguments = generateComposableArguments(
                                mapOf(
                                    BiometricUiConfig.serializedKeyName to uiSerializer.toBase64(
                                        BiometricUiConfig(
                                            title = viewState.value.screenTitle,
                                            subTitle = viewState.value.biometrySubtitle,
                                            quickPinOnlySubTitle = viewState.value.quickPinSubtitle,
                                            isPreAuthorization = false,
                                            shouldInitializeBiometricAuthOnCreate = true,
                                            onSuccessNavigation = ConfigNavigation(
                                                navigationType = NavigationType.PUSH,
                                                screenToNavigate = AuthenticationScreens.Loading
                                            ),
                                            onBackNavigation = ConfigNavigation(
                                                navigationType = NavigationType.POP,
                                                screenToNavigate = AuthenticationScreens.Request
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

            is Event.SecondaryButtonPressed -> {
                showBottomSheet(sheetContent = AuthenticationRequestBottomSheetContent.CANCEL)
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
                hideBottomSheet()
                setEffect {
                    Effect.Navigation.Pop
                }
            }

            is Event.BottomSheet.Subtitle.PrimaryButtonPressed -> {
                hideBottomSheet()
            }
        }
    }

    private fun fetchUserData(event: Event) {
        setState {
            copy(
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            interactor.getUserData().collect { response ->
                when (response) {
                    is AuthenticationInteractorPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = ContentErrorConfig(
                                    onRetry = { setEvent(event) },
                                    errorSubTitle = response.error,
                                    onCancel = { setEvent(Event.GoBack) }
                                )
                            )
                        }
                    }

                    is AuthenticationInteractorPartialState.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                userDataUi = response.userDataDomain.toUserDataUi()
                            )
                        }
                    }
                }
            }
        }
    }

    private fun updateUserDataItem(items: List<UserDataUi>, id: Int) {
        val updatedList = items.mapIndexed { index, item ->
            if (id == index) {
                val itemCurrentCheckedState = item.checked
                item.copy(
                    checked = !itemCurrentCheckedState
                )
            } else {
                item
            }
        }
        setState {
            copy(userDataUi = updatedList)
        }
    }

    private fun showBottomSheet(sheetContent: AuthenticationRequestBottomSheetContent) {
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