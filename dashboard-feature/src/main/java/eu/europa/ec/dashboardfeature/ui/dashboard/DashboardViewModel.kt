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
import eu.europa.ec.commonfeature.model.DocumentUiIssuanceState
import eu.europa.ec.commonfeature.model.PinFlow
import eu.europa.ec.corelogic.di.getOrCreatePresentationScope
import eu.europa.ec.corelogic.model.DeferredDocumentData
import eu.europa.ec.corelogic.model.DocType
import eu.europa.ec.dashboardfeature.interactor.DashboardInteractor
import eu.europa.ec.dashboardfeature.interactor.DashboardInteractorDeleteDocumentPartialState
import eu.europa.ec.dashboardfeature.interactor.DashboardInteractorGetDocumentsOnlyForTheseIdsPartialState
import eu.europa.ec.dashboardfeature.interactor.DashboardInteractorGetDocumentsPartialState
import eu.europa.ec.dashboardfeature.interactor.DashboardInteractorRetryIssuingDeferredDocumentsPartialState
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
import kotlinx.coroutines.delay
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
    val deferredFailedDocIds: List<DocumentId> = emptyList(),
    val allowUserInteraction: Boolean = false,

    val appVersion: String = "",
) : ViewState

sealed class Event : ViewEvent {
    data class Init(val deepLinkUri: Uri?) : Event()
    data object OnPause : Event()
    data class TryIssuingDeferredDocuments(val deferredDocs: Map<DocumentId, DocType>) : Event()
    data object Pop : Event()
    data class NavigateToDocument(
        val documentId: DocumentId
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
            sealed class DeferredNotReadyYet(open val documentUi: DocumentUi) : DeferredDocument() {
                data class DocumentSelected(override val documentUi: DocumentUi) :
                    DeferredNotReadyYet(documentUi)

                data class PrimaryButtonPressed(override val documentUi: DocumentUi) :
                    DeferredNotReadyYet(documentUi)

                data class SecondaryButtonPressed(override val documentUi: DocumentUi) :
                    DeferredNotReadyYet(documentUi)
            }

            data class OptionListItemForSuccessfullyIssuingDeferredDocumentSelected(
                val documentId: DocumentId
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

    data class DocumentsFetched(val deferredDocs: Map<DocumentId, DocType>) : Effect()

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
                getDocuments(
                    event = event,
                    deepLinkUri = event.deepLinkUri,
                    deferredFailedDocIds = viewState.value.deferredFailedDocIds
                )
            }

            is Event.OnPause ->{
                retryDeferredDocsJob?.cancel()
            }

            is Event.TryIssuingDeferredDocuments -> {
                tryIssuingDeferredDocuments(event, event.deferredDocs)
            }

            is Event.Pop -> setEffect { Effect.Navigation.Pop }

            is Event.NavigateToDocument -> {
                goToDocumentDetails(docId = event.documentId)
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

            is Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.DocumentSelected -> {
                showBottomSheet(
                    sheetContent = DashboardBottomSheetContent.DeferredDocumentPressed(
                        documentUi = event.documentUi
                    )
                )
            }

            is Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.PrimaryButtonPressed -> {
                hideBottomSheet()
                deleteDocument(event = event, documentId = event.documentUi.documentId)
            }

            is Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.SecondaryButtonPressed -> {
                hideBottomSheet()
            }

            is Event.BottomSheet.DeferredDocument.OptionListItemForSuccessfullyIssuingDeferredDocumentSelected -> {
                hideBottomSheet()
                goToDocumentDetails(
                    docId = event.documentId
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

    private fun goToDocumentDetails(docId: DocumentId) {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = generateComposableNavigationLink(
                    screen = IssuanceScreens.DocumentDetails,
                    arguments = generateComposableArguments(
                        mapOf(
                            "detailsType" to IssuanceFlowUiConfig.EXTRA_DOCUMENT,
                            "documentId" to docId
                        )
                    )
                )
            )
        }
    }

    private fun getDocuments(
        event: Event,
        deepLinkUri: Uri?,
        //alreadyTriedIssuingDeferredDocs: Boolean = false,
        deferredFailedDocIds: List<DocumentId> = emptyList()
    ) {
        setState {
            copy(
                isLoading = documents.isEmpty(),
                error = null
            )
        }
        viewModelScope.launch {
            dashboardInteractor.getDocuments().collect { response ->
                when (response) {
                    is DashboardInteractorGetDocumentsPartialState.Failure -> {
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

                    is DashboardInteractorGetDocumentsPartialState.Success -> {
                        val shouldAllowUserInteraction =
                            response.mainPid?.state == Document.State.ISSUED

                        val documents = response.documentsUi
                            .map { documentUi ->
                                if (documentUi.documentId in deferredFailedDocIds) {
                                    documentUi.copy(
                                        documentIssuanceState = DocumentUiIssuanceState.Failed
                                    )
                                } else {
                                    documentUi
                                }
                            }

                        val deferredDocs: MutableMap<DocumentId, DocType> = mutableMapOf()
                        response.documentsUi.filter { documentUi ->
                            documentUi.documentIssuanceState == DocumentUiIssuanceState.Pending
                        }.forEach { documentUi ->
                            deferredDocs[documentUi.documentId] =
                                documentUi.documentIdentifier.docType
                        }

                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                documents = documents,
                                deferredFailedDocIds = deferredFailedDocIds,
                                allowUserInteraction = shouldAllowUserInteraction,
                                userFirstName = response.userFirstName,
                                userBase64Image = response.userBase64Portrait
                            )
                        }

                        setEffect { Effect.DocumentsFetched(deferredDocs) }

                        handleDeepLink(deepLinkUri)
                    }
                }
            }
        }
    }

    private suspend fun getDocumentsOnlyForTheseIds(event: Event, docIds: List<DocumentId>) {
        setState {
            copy(
                isLoading = false,
                error = null
            )
        }

        dashboardInteractor.getDocumentsOnlyForTheseIds(
            documentIds = docIds,
            userFirstName = viewState.value.userFirstName,
            userImage = viewState.value.userBase64Image
        ).collect { response ->
            when (response) {
                is DashboardInteractorGetDocumentsOnlyForTheseIdsPartialState.Failure -> {
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

                is DashboardInteractorGetDocumentsOnlyForTheseIdsPartialState.Success -> {
                    val shouldAllowUserInteraction =
                        response.mainPid?.state == Document.State.ISSUED

                    val currentDocuments = viewState.value.documents
                    val newDocuments = response.newlyIssuedDocumentsUi

                    val newDocumentsIdSet = newDocuments.map { it.documentId }.toSet()

                    val updatedDocuments = currentDocuments
                        .filterNot { it.documentId in newDocumentsIdSet }
                        .plus(newDocuments)
                        .toMutableList()


                    setState {
                        copy(
                            isLoading = false,
                            error = null,
                            documents = updatedDocuments,
                            allowUserInteraction = shouldAllowUserInteraction,
                            userFirstName = response.userFirstName,
                            userBase64Image = response.userBase64Portrait
                        )
                    }
                }
            }
        }
    }

    private fun tryIssuingDeferredDocuments(event: Event, deferredDocs: Map<DocumentId, DocType>) {
        setState {
            copy(
                isLoading = false,
                error = null
            )
        }

        retryDeferredDocsJob?.cancel()
        retryDeferredDocsJob = viewModelScope.launch {
            if (deferredDocs.isEmpty()) {
                return@launch
            }

            println("Giannis VM delaying for 2 sec...")
            delay(2000L)
            println("Giannis VM end of delay.")

            println("Giannis VM will try deferred docs: ${deferredDocs.keys} again.")
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
                        val successDocs = response.successfullyIssuedDeferredDocuments
                        if (successDocs.isNotEmpty()) {
                            /*val getDocumentsJob = async {
                                getDocumentsOnlyForTheseIds(
                                    event = event,
                                    docIds = response.successfullyIssuedDeferredDocuments.map { it.documentId }
                                )
                            }

                            getDocumentsJob.await()*/

                            showBottomSheet(
                                sheetContent = DashboardBottomSheetContent.DeferredDocumentsReady(
                                    successfullyIssuedDeferredDocuments = successDocs,
                                    options = getBottomSheetOptions(
                                        deferredDocumentsData = successDocs
                                    )
                                )
                            )
                        }

                        getDocuments(
                            event = event,
                            deepLinkUri = null,
                            //alreadyTriedIssuingDeferredDocs = true,
                            deferredFailedDocIds = response.failedIssuedDeferredDocuments
                        )
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
                        Event.BottomSheet.DeferredDocument.OptionListItemForSuccessfullyIssuingDeferredDocumentSelected(
                            documentId = it.documentId
                        )
                    )
                }
            )
        }
    }

    private fun deleteDocument(event: Event, documentId: DocumentId) {
        setState {
            copy(
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            dashboardInteractor.deleteDocument(
                documentId = documentId
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
                        getDocuments(
                            event = event,
                            deepLinkUri = null
                        )
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
                                    RequestUriConfig(
                                        PresentationMode.OpenId4Vp(
                                            uri.toString(),
                                            DashboardScreens.Dashboard.screenRoute
                                        )
                                    ),
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
                                RequestUriConfig(PresentationMode.Ble(DashboardScreens.Dashboard.screenRoute)),
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

    override fun onCleared() {
        super.onCleared()
        retryDeferredDocsJob?.cancel()
    }
}