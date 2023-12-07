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

package eu.europa.ec.dashboardfeature.ui.dashboard

import androidx.lifecycle.viewModelScope
import eu.europa.ec.businesslogic.di.PRESENTATION_SCOPE_ID
import eu.europa.ec.businesslogic.di.WalletPresentationScope
import eu.europa.ec.commonfeature.config.IssuanceFlowUiConfig
import eu.europa.ec.commonfeature.extensions.getKoin
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.commonfeature.model.PinFlow
import eu.europa.ec.dashboardfeature.interactor.DashboardInteractor
import eu.europa.ec.dashboardfeature.interactor.DashboardInteractorPartialState
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.ProximityScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class State(
    val isLoading: Boolean = false,
    val error: ContentErrorConfig? = null,
    val isBottomSheetOpen: Boolean = false,

    val userName: String = "",
    val documents: List<DocumentUi> = emptyList()
) : ViewState

sealed class Event : ViewEvent {
    data class Init(val qrResult: String?) : Event()
    data object Pop : Event()
    data class NavigateToDocument(val documentId: String) : Event()
    data object OptionsPressed : Event()
    sealed class Fab : Event() {
        data object PrimaryFabPressed : Fab()
        data object SecondaryFabPressed : Fab()
    }

    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(val isOpen: Boolean) : BottomSheet()
        data object Close : BottomSheet()
        sealed class Options : BottomSheet() {
            data object OpenChangeQuickPin : Options()
            data object OpenScanQr : Options()
        }
    }
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
        data class SwitchScreen(val screenRoute: String) : Navigation()
        data object OpenDeepLinkAction : Navigation()
    }

    data object ShowBottomSheet : Effect()
    data object CloseBottomSheet : Effect()
}

@KoinViewModel
class DashboardViewModel(
    private val dashboardInteractor: DashboardInteractor,
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State = State()

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                getDocuments(event)
                event.qrResult?.let {
                    print(it)
                    // TODO HANDLE OPENID4VP FROM QR
                }
            }

            is Event.Pop -> setEffect { Effect.Navigation.Pop }

            is Event.NavigateToDocument -> {
                setEffect {
                    Effect.Navigation.SwitchScreen(
                        generateComposableNavigationLink(
                            screen = IssuanceScreens.DocumentDetails,
                            arguments = generateComposableArguments(
                                mapOf(
                                    "detailsType" to IssuanceFlowUiConfig.EXTRA_DOCUMENT,
                                    "documentId" to event.documentId
                                )
                            )
                        )
                    )
                }
            }

            is Event.OptionsPressed -> {
                showBottomSheet()
            }

            is Event.Fab.PrimaryFabPressed -> {
                // Create Koin scope for presentation
                getKoin().getOrCreateScope<WalletPresentationScope>(PRESENTATION_SCOPE_ID)
                setEffect {
                    Effect.Navigation.SwitchScreen(
                        screenRoute = ProximityScreens.QR.screenRoute
                    )
                }
            }

            is Event.Fab.SecondaryFabPressed -> {
                setEffect {
                    Effect.Navigation.SwitchScreen(
                        screenRoute = generateComposableNavigationLink(
                            screen = IssuanceScreens.AddDocument,
                            arguments = generateComposableArguments(
                                mapOf("flowType" to IssuanceFlowUiConfig.EXTRA_DOCUMENT)
                            )
                        )
                    )
                }
            }

            is Event.BottomSheet.UpdateBottomSheetState -> {
                setState {
                    copy(isBottomSheetOpen = event.isOpen)
                }
            }

            is Event.BottomSheet.Close -> {
                hideBottomSheet()
            }

            is Event.BottomSheet.Options.OpenChangeQuickPin -> {
                hideBottomSheet()
                navigateToChangeQuickPin()
            }

            is Event.BottomSheet.Options.OpenScanQr -> {
                hideBottomSheet()
                navigateToScanQr()
            }
        }
    }

    private fun getDocuments(event: Event) {
        setState {
            copy(
                isLoading = documents.isEmpty(),
                error = null
            )
        }

        viewModelScope.launch {
            dashboardInteractor.getDocuments().collect { response ->
                when (response) {
                    is DashboardInteractorPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = ContentErrorConfig(
                                    onRetry = { setEvent(event) },
                                    errorSubTitle = response.error,
                                    onCancel = { setEvent(Event.Pop) }
                                )
                            )
                        }
                    }

                    is DashboardInteractorPartialState.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                documents = response.documents,
                                userName = response.name
                            )
                        }
                        setEffect { Effect.Navigation.OpenDeepLinkAction }
                    }
                }
            }
        }
    }

    private fun navigateToChangeQuickPin() {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = generateComposableNavigationLink(
                    screen = CommonScreens.QuickPin,
                    arguments = generateComposableArguments(
                        mapOf("pinFlow" to PinFlow.UPDATE)
                    )
                )
            )
        }
    }

    private fun navigateToScanQr() {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = DashboardScreens.Scanner.screenRoute,
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