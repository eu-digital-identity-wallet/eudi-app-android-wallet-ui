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

import android.net.Uri
import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.config.IssuanceFlowUiConfig
import eu.europa.ec.commonfeature.config.PresentationMode
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.commonfeature.model.PinFlow
import eu.europa.ec.corelogic.di.getOrCreatePresentationScope
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
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

enum class BleAvailability {
    AVAILABLE, NO_PERMISSION, DISABLED, UNKNOWN
}

data class State(
    val isLoading: Boolean = true,
    val error: ContentErrorConfig? = null,
    val isBottomSheetOpen: Boolean = false,
    val sheetContent: DashboardBottomSheetContent = DashboardBottomSheetContent.OPTIONS,

    val bleAvailability: BleAvailability = BleAvailability.UNKNOWN,
    val isBleCentralClientModeEnabled: Boolean = false,

    val userFirstName: String = "",
    val userBase64Image: String = "",
    val documents: List<DocumentUi> = emptyList(),

    val deepLinkUri: Uri? = null,
    val appVersion: String = ""
) : ViewState

sealed class Event : ViewEvent {
    data class Init(val deepLinkUri: Uri?) : Event()
    data object Pop : Event()
    data class NavigateToDocument(
        val documentId: String,
        val documentType: String,
    ) : Event()

    data object OptionsPressed : Event()
    data object StartProximityFlow : Event()
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

        sealed class Bluetooth : BottomSheet() {
            data class PrimaryButtonPressed(val availability: BleAvailability) : Bluetooth()
            data object SecondaryButtonPressed : Bluetooth()
        }
    }

    data object OnShowPermissionsRational : Event()
    data class OnPermissionStateChanged(val availability: BleAvailability) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
        data class SwitchScreen(val screenRoute: String) : Navigation()
        data class OpenDeepLinkAction(val deepLinkUri: Uri, val arguments: String) :
            Navigation()

        data object OnAppSettings : Navigation()
        data object OnSystemSettings : Navigation()
    }

    data object ShowBottomSheet : Effect()
    data object CloseBottomSheet : Effect()
}

sealed class DashboardBottomSheetContent {
    data object OPTIONS : DashboardBottomSheetContent()

    data class BLUETOOTH(val availability: BleAvailability) : DashboardBottomSheetContent()
}

@KoinViewModel
class DashboardViewModel(
    private val dashboardInteractor: DashboardInteractor,
    private val uiSerializer: UiSerializer
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State = State(
        isBleCentralClientModeEnabled = dashboardInteractor.isBleCentralClientModeEnabled(),
        appVersion = dashboardInteractor.getAppVersion()
    )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                setState {
                    copy(deepLinkUri = event.deepLinkUri)
                }
                getDocuments(event)
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
                                    "documentId" to event.documentId,
                                    "documentType" to event.documentType,
                                )
                            )
                        )
                    )
                }
            }

            is Event.OptionsPressed -> {
                showBottomSheet(sheetContent = DashboardBottomSheetContent.OPTIONS)
            }

            is Event.StartProximityFlow -> {
                startProximityFlow()
            }

            is Event.Fab.PrimaryFabPressed -> {
                checkIfBluetoothIsEnabled()
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

            is Event.BottomSheet.Bluetooth.PrimaryButtonPressed -> {
                hideBottomSheet()
                onBleUserAction(event.availability)
            }

            is Event.BottomSheet.Bluetooth.SecondaryButtonPressed -> {
                hideBottomSheet()
            }

            is Event.OnShowPermissionsRational -> {
                setState { copy(bleAvailability = BleAvailability.UNKNOWN) }
                showBottomSheet(sheetContent = DashboardBottomSheetContent.BLUETOOTH(BleAvailability.NO_PERMISSION))
            }

            is Event.OnPermissionStateChanged -> {
                setState { copy(bleAvailability = event.availability) }
            }
        }
    }

    private fun onBleUserAction(availability: BleAvailability) {
        when (availability) {

            BleAvailability.NO_PERMISSION -> {
                setEffect { Effect.Navigation.OnAppSettings }
            }

            BleAvailability.DISABLED -> {
                setEffect { Effect.Navigation.OnSystemSettings }
            }

            else -> {}
        }
    }

    private fun checkIfBluetoothIsEnabled() {
        if (dashboardInteractor.isBleAvailable()) {
            setState { copy(bleAvailability = BleAvailability.NO_PERMISSION) }
        } else {
            setState { copy(bleAvailability = BleAvailability.DISABLED) }
            showBottomSheet(sheetContent = DashboardBottomSheetContent.BLUETOOTH(BleAvailability.DISABLED))
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
                                    onCancel = {
                                        setState { copy(error = null) }
                                        setEvent(Event.Pop)
                                    }
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
                                userFirstName = response.userFirstName,
                                userBase64Image = response.userBase64Portrait
                            )
                        }
                        viewState.value.deepLinkUri?.let { uri ->
                            getOrCreatePresentationScope()
                            setEffect {
                                Effect.Navigation.OpenDeepLinkAction(
                                    deepLinkUri = uri,
                                    arguments = generateComposableArguments(
                                        mapOf(
                                            RequestUriConfig.serializedKeyName to uiSerializer.toBase64(
                                                RequestUriConfig(PresentationMode.OpenId4Vp(uri.toString())),
                                                RequestUriConfig.Parser
                                            )
                                        )
                                    )
                                )
                            }
                        }
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

    private fun startProximityFlow() {
        setState { copy(bleAvailability = BleAvailability.AVAILABLE) }
        // Create Koin scope for presentation
        getOrCreatePresentationScope()
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = generateComposableNavigationLink(
                    screen = ProximityScreens.QR,
                    arguments = generateComposableArguments(
                        mapOf(
                            RequestUriConfig.serializedKeyName to uiSerializer.toBase64(
                                RequestUriConfig(PresentationMode.Ble),
                                RequestUriConfig.Parser
                            )
                        )
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

    private fun showBottomSheet(sheetContent: DashboardBottomSheetContent) {
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