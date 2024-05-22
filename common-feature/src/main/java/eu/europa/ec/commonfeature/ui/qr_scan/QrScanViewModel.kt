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

package eu.europa.ec.commonfeature.ui.qr_scan

import eu.europa.ec.commonfeature.config.IssuanceFlowUiConfig
import eu.europa.ec.commonfeature.config.OfferUiConfig
import eu.europa.ec.commonfeature.config.PresentationMode
import eu.europa.ec.commonfeature.config.QrScanFlow
import eu.europa.ec.commonfeature.config.QrScanUiConfig
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.corelogic.di.getOrCreatePresentationScope
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.PresentationScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

data class State(
    val hasCameraPermission: Boolean = false,
    val shouldShowPermissionRational: Boolean = false,
    val finishedScanning: Boolean = false,
    val qrScannedConfig: QrScanUiConfig,
) : ViewState

sealed class Event : ViewEvent {
    data object GoBack : Event()
    data class OnQrScanned(val resultQr: String) : Event()
    data object CameraAccessGranted : Event()
    data object ShowPermissionRational : Event()
    data object GoToAppSettings : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(val screenRoute: String) : Navigation()
        data object Pop : Navigation()
        data object GoToAppSettings : Navigation()
    }
}

@KoinViewModel
class QrScanViewModel(
    private val uiSerializer: UiSerializer,
    private val resourceProvider: ResourceProvider,
    @InjectedParam private val qrScannedConfig: String,
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State = State(
        qrScannedConfig = uiSerializer.fromBase64(
            qrScannedConfig,
            QrScanUiConfig::class.java,
            QrScanUiConfig.Parser
        ) ?: throw RuntimeException("QrScanUiConfig:: is Missing or invalid")
    )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.GoBack -> setEffect { Effect.Navigation.Pop }

            is Event.OnQrScanned -> {
                if (viewState.value.finishedScanning) {
                    return
                }
                setState {
                    copy(finishedScanning = true)
                }

                calculateNextStep(
                    qrScanFlow = viewState.value.qrScannedConfig.qrScanFlow,
                    scanResult = event.resultQr
                )
            }

            is Event.CameraAccessGranted -> {
                setState {
                    copy(hasCameraPermission = true)
                }
            }

            is Event.ShowPermissionRational -> {
                setState {
                    copy(shouldShowPermissionRational = true)
                }
            }

            is Event.GoToAppSettings -> setEffect { Effect.Navigation.GoToAppSettings }
        }
    }

    private fun calculateNextStep(
        qrScanFlow: QrScanFlow,
        scanResult: String,
    ) {
        when (qrScanFlow) {
            QrScanFlow.PRESENTATION -> navigateToPresentationRequest(scanResult)
            QrScanFlow.ISSUANCE -> navigateToDocumentOffer(scanResult)
        }
    }

    private fun navigateToPresentationRequest(scanResult: String) {
        setEffect {
            getOrCreatePresentationScope()
            Effect.Navigation.SwitchScreen(
                screenRoute = generateComposableNavigationLink(
                    screen = PresentationScreens.PresentationRequest,
                    arguments = generateComposableArguments(
                        mapOf(
                            RequestUriConfig.serializedKeyName to uiSerializer.toBase64(
                                RequestUriConfig(PresentationMode.OpenId4Vp(uri = scanResult)),
                                RequestUriConfig.Parser
                            )
                        )
                    )
                )
            )
        }
    }

    private fun navigateToDocumentOffer(scanResult: String) {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = generateComposableNavigationLink(
                    screen = IssuanceScreens.DocumentOffer,
                    arguments = generateComposableArguments(
                        mapOf(
                            OfferUiConfig.serializedKeyName to uiSerializer.toBase64(
                                OfferUiConfig(
                                    offerURI = scanResult,
                                    onSuccessNavigation = calculateOnSuccessNavigation(viewState.value.qrScannedConfig.issuanceFlow),
                                    onCancelNavigation = calculateOnCancelNavigation(viewState.value.qrScannedConfig.issuanceFlow)
                                ),
                                OfferUiConfig.Parser
                            )
                        )
                    )
                )
            )
        }
    }

    private fun calculateOnSuccessNavigation(issuanceFlowUiConfig: IssuanceFlowUiConfig): ConfigNavigation {
        return when (issuanceFlowUiConfig) {
            IssuanceFlowUiConfig.NO_DOCUMENT -> {
                ConfigNavigation(
                    navigationType = NavigationType.PushRoute(
                        route = DashboardScreens.Dashboard.screenRoute
                    )
                )
            }

            IssuanceFlowUiConfig.EXTRA_DOCUMENT -> {
                ConfigNavigation(
                    navigationType = NavigationType.PopTo(
                        screen = DashboardScreens.Dashboard
                    )
                )
            }
        }
    }

    private fun calculateOnCancelNavigation(issuanceFlowUiConfig: IssuanceFlowUiConfig): ConfigNavigation {
        return when (issuanceFlowUiConfig) {
            IssuanceFlowUiConfig.NO_DOCUMENT -> {
                ConfigNavigation(
                    navigationType = NavigationType.Pop
                )
            }

            IssuanceFlowUiConfig.EXTRA_DOCUMENT -> {
                ConfigNavigation(
                    navigationType = NavigationType.PopTo(
                        screen = DashboardScreens.Dashboard
                    )
                )
            }
        }
    }
}