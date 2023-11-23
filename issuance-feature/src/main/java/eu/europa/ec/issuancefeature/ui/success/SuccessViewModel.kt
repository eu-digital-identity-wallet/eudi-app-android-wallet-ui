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

package eu.europa.ec.issuancefeature.ui.success

import eu.europa.ec.commonfeature.config.issuance.IssuanceDetailsUiConfig
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

data class State(
    val isBottomSheetOpen: Boolean = false,

    val docType: String,
) : ViewState

sealed class Event : ViewEvent {
    data object GoBack : Event()
    data class PrimaryButtonPressed(val documentId: String) : Event()
    data object SecondaryButtonPressed : Event()

    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(val isOpen: Boolean) : BottomSheet()

        sealed class Cancel : BottomSheet() {
            data object PrimaryButtonPressed : Cancel()
            data object SecondaryButtonPressed : Cancel()
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

@KoinViewModel
class SuccessViewModel(
    @InjectedParam private val docType: String,
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State {
        return State(
            docType = docType
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.GoBack -> setEffect { Effect.Navigation.Pop }

            is Event.PrimaryButtonPressed -> {
                setEffect {
                    Effect.Navigation.SwitchScreen(
                        screenRoute = generateComposableNavigationLink(
                            screen = IssuanceScreens.DocumentDetails,
                            arguments = generateComposableArguments(
                                mapOf(
                                    "detailsType" to IssuanceDetailsUiConfig.NO_DOCUMENT,
                                    "documentId" to event.documentId
                                )
                            )
                        )
                    )
                }
            }

            is Event.SecondaryButtonPressed -> {
                showBottomSheet()
            }

            is Event.BottomSheet.Cancel.PrimaryButtonPressed -> {
                hideBottomSheet()
            }

            is Event.BottomSheet.Cancel.SecondaryButtonPressed -> {
                hideBottomSheet()
                setEvent(Event.GoBack)
            }

            is Event.BottomSheet.UpdateBottomSheetState -> {
                setState {
                    copy(isBottomSheetOpen = event.isOpen)
                }
            }
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