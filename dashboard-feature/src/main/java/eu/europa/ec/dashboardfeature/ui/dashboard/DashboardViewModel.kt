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
import eu.europa.ec.commonfeature.config.OfferUiConfig
import eu.europa.ec.commonfeature.config.PresentationMode
import eu.europa.ec.commonfeature.config.QrScanFlow
import eu.europa.ec.commonfeature.config.QrScanUiConfig
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.commonfeature.model.PinFlow
import eu.europa.ec.corelogic.di.getOrCreatePresentationScope
import eu.europa.ec.corelogic.model.DocType
import eu.europa.ec.dashboardfeature.interactor.DashboardInteractor
import eu.europa.ec.dashboardfeature.interactor.DashboardInteractorDeleteDocumentPartialState
import eu.europa.ec.dashboardfeature.interactor.DashboardInteractorPartialState
import eu.europa.ec.dashboardfeature.interactor.DashboardInteractorRetryIssuingDeferredDocumentsPartialState
import eu.europa.ec.dashboardfeature.interactor.DeferredDocumentData
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.wrap.OptionListItemUi
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.ProximityScreens
import eu.europa.ec.uilogic.navigation.StartupScreens
import eu.europa.ec.uilogic.navigation.helper.DeepLinkType
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.navigation.helper.hasDeepLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

enum class BleAvailability {
    AVAILABLE, NO_PERMISSION, DISABLED, UNKNOWN
}

data class State(
    val isLoading: Boolean = true,
    val error: ContentErrorConfig? = null,
    val isBottomSheetOpen: Boolean = false,
    val sheetContent: DashboardBottomSheetContent = DashboardBottomSheetContent.Options,

    val bleAvailability: BleAvailability = BleAvailability.UNKNOWN,
    val isBleCentralClientModeEnabled: Boolean = false,

    val userFirstName: String = "",
    val userBase64Image: String = "",
    val documents: List<DocumentUi> = emptyList(),

    val appVersion: String = "",
    val allowUserInteraction: Boolean = false,
) : ViewState

sealed class Event : ViewEvent {
    data class Init(val deepLinkUri: Uri?) : Event()
    data object TryIssuingDeferredDocuments : Event()
    data object Pop : Event()
    data class NavigateToDocument(
        val documentId: DocumentId,
        val documentType: DocType,
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

        sealed class DeferredDocument : BottomSheet() {
            data class DeferredDocumentPressed(val documentUi: DocumentUi) : DeferredDocument() {
                data class PrimaryButtonPressed(val documentUi: DocumentUi) : DeferredDocument()
                data object SecondaryButtonPressed : DeferredDocument()
            }

            /*data class DeferredDocumentsReady(
                val documentsUi: List<DocumentUi>
            ) : DeferredDocument()*/

            data class DeferredDocumentSelected(
                val documentId: DocumentId,
                val docType: DocType
            ) : DeferredDocument()
        }
    }

    data object OnShowPermissionsRational : Event()
    data class OnPermissionStateChanged(val availability: BleAvailability) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
        data class SwitchScreen(
            val screenRoute: String,
            val popUpToScreenRoute: String = DashboardScreens.Dashboard.screenRoute,
            val inclusive: Boolean = false,
        ) : Navigation()

        data class OpenDeepLinkAction(val deepLinkUri: Uri, val arguments: String?) :
            Navigation()

        data object OnAppSettings : Navigation()
        data object OnSystemSettings : Navigation()
    }

    data object DocumentsFetched : Effect()

    data object ShowBottomSheet : Effect()
    data object CloseBottomSheet : Effect()
}

sealed class DashboardBottomSheetContent {
    data object Options : DashboardBottomSheetContent()

    data class Bluetooth(val availability: BleAvailability) : DashboardBottomSheetContent()
    data class DeferredDocumentPressed(val documentUi: DocumentUi) : DashboardBottomSheetContent()
    data class DeferredDocumentsReady(
        val successfullyIssuedDeferredDocuments: List<DeferredDocumentData>,
        val options: List<OptionListItemUi>,
        //val failedIssuedDeferredDocumentIds: List<DocumentId>,
    ) : DashboardBottomSheetContent()
}

@KoinViewModel
class DashboardViewModel(
    private val dashboardInteractor: DashboardInteractor,
    private val uiSerializer: UiSerializer,
    private val resourceProvider: ResourceProvider,
) : MviViewModel<Event, State, Effect>() {

    private var retryDeferredDocsJob: Job? = null

    override fun setInitialState(): State = State(
        isBleCentralClientModeEnabled = dashboardInteractor.isBleCentralClientModeEnabled(),
        appVersion = dashboardInteractor.getAppVersion()
    )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                getDocuments(event = event, deepLinkUri = event.deepLinkUri)
            }

            is Event.TryIssuingDeferredDocuments -> {
                tryDeferredDocuments(event)
            }

            is Event.Pop -> setEffect { Effect.Navigation.Pop }

            is Event.NavigateToDocument -> {
                goToDocumentDetails(docId = event.documentId, docType = event.documentType)
            }

            is Event.OptionsPressed -> {
                showBottomSheet(sheetContent = DashboardBottomSheetContent.Options)
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
                navigateToQrScan()
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
                showBottomSheet(sheetContent = DashboardBottomSheetContent.Bluetooth(BleAvailability.NO_PERMISSION))
            }

            is Event.OnPermissionStateChanged -> {
                setState { copy(bleAvailability = event.availability) }
            }

            is Event.BottomSheet.DeferredDocument.DeferredDocumentPressed -> {
                showBottomSheet(
                    sheetContent = DashboardBottomSheetContent.DeferredDocumentPressed(
                        documentUi = event.documentUi
                    )
                )
            }

            is Event.BottomSheet.DeferredDocument.DeferredDocumentPressed.PrimaryButtonPressed -> {
                hideBottomSheet()
                deleteDocument(event)
            }

            is Event.BottomSheet.DeferredDocument.DeferredDocumentPressed.SecondaryButtonPressed -> {
                hideBottomSheet()
            }

            /*is Event.BottomSheet.DeferredDocument.DeferredDocumentsReady -> {
                val successDeferredDocs = event.documentsUi.map { documentUi ->
                    DeferredDocumentData(
                        documentId = documentUi.documentId,
                        docType = documentUi.documentIdentifier.docType,
                        docName = documentUi.documentName
                    )
                }

                val options = event.documentsUi.map { documentUi ->
                    OptionListItemUi(
                        text = documentUi.documentName,
                        onClick = {
                            setEvent(
                                Event.BottomSheet.DeferredDocument.DeferredDocumentSelected(
                                    documentId = documentUi.documentId,
                                    docType = documentUi.documentIdentifier.docType
                                )
                            )
                        }
                    )
                }

                showBottomSheet(
                    sheetContent = DashboardBottomSheetContent.DeferredDocumentsReady(
                        successfullyIssuedDeferredDocuments = successDeferredDocs,
                        options = options
                    )
                )
            }*/

            is Event.BottomSheet.DeferredDocument.DeferredDocumentSelected -> {
                //hideBottomSheet()
                goToDocumentDetails(
                    docId = event.documentId,
                    docType = event.docType
                )
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
            showBottomSheet(sheetContent = DashboardBottomSheetContent.Bluetooth(BleAvailability.DISABLED))
        }
    }

    private fun goToDocumentDetails(docId: DocumentId, docType: DocType) {
        setEffect {
            Effect.Navigation.SwitchScreen(
                generateComposableNavigationLink(
                    screen = IssuanceScreens.DocumentDetails,
                    arguments = generateComposableArguments(
                        mapOf(
                            "detailsType" to IssuanceFlowUiConfig.EXTRA_DOCUMENT,
                            "documentId" to docId,
                            "documentType" to docType,
                        )
                    )
                )
            )
        }
    }

    private fun getDocuments(event: Event, deepLinkUri: Uri?) {
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
                        val allDocuments = response.documents
                        val shouldAllowUserInteraction =
                            response.mainPid?.state == Document.State.ISSUED
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                documents = allDocuments,
                                allowUserInteraction = shouldAllowUserInteraction,
                                userFirstName = response.userFirstName,
                                userBase64Image = response.userBase64Portrait
                            )
                        }
                        setEffect { Effect.DocumentsFetched }
                        handleDeepLink(deepLinkUri)
                    }
                }
            }
        }
    }

    private fun tryDeferredDocuments(event: Event) {
        setState {
            copy(
                isLoading = false,
                error = null
            )
        }

        retryDeferredDocsJob?.cancel()
        retryDeferredDocsJob = viewModelScope.launch {
            val deferredDocs: MutableMap<DocumentId, DocType> = mutableMapOf()
            viewState.value.documents.filter { documentUi ->
                documentUi.documentIsDeferred
            }.forEach { documentUi ->
                deferredDocs[documentUi.documentId] = documentUi.documentIdentifier.docType
            }

            if (deferredDocs.isEmpty()) {
                return@launch
            }

            println("Giannis VM will try deferred docs: $deferredDocs again.")
            dashboardInteractor.tryIssuingDeferredDocumentsFlow(deferredDocs).collect { response ->
                when (response) {
                    is DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = ContentErrorConfig(
                                    onRetry = { setEvent(event) },
                                    errorSubTitle = response.errorMessage,
                                    onCancel = {
                                        setState { copy(error = null) }
                                    }
                                )
                            )
                        }
                    }

                    is DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result -> {
                        val options =
                            getBottomSheetOptions(response.successfullyIssuedDeferredDocuments)

                        showBottomSheet(
                            sheetContent = DashboardBottomSheetContent.DeferredDocumentsReady(
                                successfullyIssuedDeferredDocuments = response.successfullyIssuedDeferredDocuments,
                                options = options
                                //failedIssuedDeferredDocumentIds = response.failedIssuedDeferredDocuments
                            )
                        )
                        getDocuments(event = event, deepLinkUri = null)
                    }
                }
            }
        }
    }

    private fun getBottomSheetOptions(deferredDocumentsData: List<DeferredDocumentData>): List<OptionListItemUi> {
        return deferredDocumentsData.map {
            OptionListItemUi(
                text = it.docName,
                onClick = {
                    setEvent(
                        Event.BottomSheet.DeferredDocument.DeferredDocumentSelected(
                            documentId = it.documentId,
                            docType = it.docType
                        )
                    )
                }
            )
        }
    }

    private fun deleteDocument(event: Event.BottomSheet.DeferredDocument.DeferredDocumentPressed.PrimaryButtonPressed) {
        setState {
            copy(
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            dashboardInteractor.deleteDocument(
                documentId = event.documentUi.documentId,
                documentType = event.documentUi.documentIdentifier.docType,
            ).collect { response ->
                when (response) {
                    is DashboardInteractorDeleteDocumentPartialState.AllDocumentsDeleted -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = null
                            )
                        }

                        setEffect {
                            Effect.Navigation.SwitchScreen(
                                screenRoute = StartupScreens.Splash.screenRoute,
                                popUpToScreenRoute = DashboardScreens.Dashboard.screenRoute,
                                inclusive = true
                            )
                        }
                    }

                    is DashboardInteractorDeleteDocumentPartialState.SingleDocumentDeleted -> {
                        getDocuments(event = event, deepLinkUri = null)
                    }

                    is DashboardInteractorDeleteDocumentPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = ContentErrorConfig(
                                    onRetry = { setEvent(event) },
                                    errorSubTitle = response.errorMessage,
                                    onCancel = {
                                        setState {
                                            copy(error = null)
                                        }
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleDeepLink(deepLinkUri: Uri?) {
        deepLinkUri?.let { uri ->
            hasDeepLink(uri)?.let {
                val arguments: String? = when (it.type) {
                    DeepLinkType.OPENID4VP -> {
                        getOrCreatePresentationScope()
                        generateComposableArguments(
                            mapOf(
                                RequestUriConfig.serializedKeyName to uiSerializer.toBase64(
                                    RequestUriConfig(PresentationMode.OpenId4Vp(uri.toString())),
                                    RequestUriConfig.Parser
                                )
                            )
                        )
                    }

                    DeepLinkType.CREDENTIAL_OFFER -> generateComposableArguments(
                        mapOf(
                            OfferUiConfig.serializedKeyName to uiSerializer.toBase64(
                                OfferUiConfig(
                                    offerURI = it.link.toString(),
                                    onSuccessNavigation = ConfigNavigation(
                                        navigationType = NavigationType.PopTo(
                                            screen = DashboardScreens.Dashboard
                                        )
                                    ),
                                    onCancelNavigation = ConfigNavigation(
                                        navigationType = NavigationType.Pop
                                    )
                                ),
                                OfferUiConfig.Parser
                            )
                        )
                    )

                    else -> null
                }
                setEffect {
                    Effect.Navigation.OpenDeepLinkAction(
                        deepLinkUri = uri,
                        arguments = arguments
                    )
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

    private fun navigateToQrScan() {
        val navigationEffect = Effect.Navigation.SwitchScreen(
            screenRoute = generateComposableNavigationLink(
                screen = CommonScreens.QrScan,
                arguments = generateComposableArguments(
                    mapOf(
                        QrScanUiConfig.serializedKeyName to uiSerializer.toBase64(
                            QrScanUiConfig(
                                title = resourceProvider.getString(R.string.presentation_qr_scan_title),
                                subTitle = resourceProvider.getString(R.string.presentation_qr_scan_subtitle),
                                qrScanFlow = QrScanFlow.Presentation
                            ),
                            QrScanUiConfig.Parser
                        )
                    )
                )
            )
        )

        setEffect {
            navigationEffect
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