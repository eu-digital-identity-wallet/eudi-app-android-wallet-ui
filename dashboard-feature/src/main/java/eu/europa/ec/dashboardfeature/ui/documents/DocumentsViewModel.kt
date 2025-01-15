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

package eu.europa.ec.dashboardfeature.ui.documents

import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.config.IssuanceFlowUiConfig
import eu.europa.ec.commonfeature.config.QrScanFlow
import eu.europa.ec.commonfeature.config.QrScanUiConfig
import eu.europa.ec.commonfeature.model.DocumentUiIssuanceState
import eu.europa.ec.corelogic.model.FormatType
import eu.europa.ec.dashboardfeature.interactor.DocumentInteractorDeleteDocumentPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentInteractorGetDocumentsPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentInteractorRetryIssuingDeferredDocumentsPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentsInteractor
import eu.europa.ec.dashboardfeature.model.DocumentDetailsItemUi
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.DualSelectorButton
import eu.europa.ec.uilogic.component.DualSelectorButtonData
import eu.europa.ec.uilogic.component.ModalOptionUi
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemData
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.StartupScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class State(
    val isLoading: Boolean,
    val error: ContentErrorConfig? = null,
    val documents: List<DocumentDetailsItemUi> = emptyList(),
    val deferredFailedDocIds: List<DocumentId> = emptyList(),

    //TODO remove that once BottomSheet Logic is sorted out.
    val successfullyIssuedDeferredDocumentsUi: List<ModalOptionUi<Event>> = emptyList(),
    val deferredSelectedDocumentToBeDeleted: DocumentId? = null,

    val allowUserInteraction: Boolean = true, //TODO
    val filters: List<ExpandableListItemData> = emptyList(),
    val isFilteringActive: Boolean,
    val sortingOrderButtonData: DualSelectorButtonData,
    val showAddDocumentBottomSheet: Boolean = false,
    val showFiltersBottomSheet: Boolean = false,
    val showDeferredDocumentsReadyBottomSheet: Boolean = false,
    val showDeferredDocumentPressedBottomSheet: Boolean = false,
) : ViewState

sealed class Event : ViewEvent {
    data object GetDocuments : Event()
    data object OnPause : Event()
    data class TryIssuingDeferredDocuments(val deferredDocs: Map<DocumentId, FormatType>) : Event()
    data object Pop : Event()
    data object GoToAddDocument : Event()
    data object GoToQrScan : Event()
    data class GoToDocumentDetails(val docId: DocumentId) : Event()
    data class ShowAddDocumentBottomSheet(val isOpen: Boolean) : Event()
    data class ShowFiltersBottomSheet(val isOpen: Boolean) : Event()
    data class ShowDeferredDocumentsReadyBottomSheet(val isOpen: Boolean) : Event()
    data class ShowDeferredDocumentPressedBottomSheet(val isOpen: Boolean) : Event()
    data class OnSearchQueryChanged(val query: String) : Event()
    data class OnFilterSelectionChanged(val filterId: String, val groupId: String) : Event()
    data object OnFiltersReset : Event()
    data object OnFiltersApply : Event()
    data class OnSortingOrderChanged(val sortingOrder: DualSelectorButton) : Event()

    data class OptionListItemForSuccessfullyIssuingDeferredDocumentSelected(
        val documentId: DocumentId
    ) : Event()

    sealed class DeferredNotReadyYet(open val documentId: DocumentId) : Event() {
        data class DocumentSelected(override val documentId: DocumentId) :
            DeferredNotReadyYet(documentId)

        data class PrimaryButtonPressed(override val documentId: DocumentId) :
            DeferredNotReadyYet(documentId)

        data class SecondaryButtonPressed(override val documentId: DocumentId) :
            DeferredNotReadyYet(documentId)
    }
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
        data class SwitchScreen(
            val screenRoute: String,
            val popUpToScreenRoute: String = DashboardScreens.Dashboard.screenRoute,
            val inclusive: Boolean = false,
        ) : Navigation()
    }

    data class DocumentsFetched(val deferredDocs: Map<DocumentId, FormatType>) : Effect()
}

@KoinViewModel
class DocumentsViewModel(
    val interactor: DocumentsInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
) : MviViewModel<Event, State, Effect>() {

    private var retryDeferredDocsJob: Job? = null

    override fun setInitialState(): State {
        return State(
            isLoading = true,
            sortingOrderButtonData = DualSelectorButtonData(
                first = resourceProvider.getString(R.string.documents_screen_filters_ascending),
                second = resourceProvider.getString(R.string.documents_screen_filters_descending),
                selectedButton = DualSelectorButton.FIRST,
            ),
            isFilteringActive = false
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.GetDocuments -> {
                setState {
                    copy(
                        filters = interactor.getFilters()
                    )
                }

                getDocuments(
                    event = event,
                    deferredFailedDocIds = emptyList()
                )
            }

            is Event.OnPause -> {
                retryDeferredDocsJob?.cancel()
            }

            is Event.TryIssuingDeferredDocuments -> {
                tryIssuingDeferredDocuments(event, event.deferredDocs)
            }

            is Event.Pop -> setEffect { Effect.Navigation.Pop }

            is Event.GoToDocumentDetails -> {
                goToDocumentDetails(event.docId)
            }

            is Event.ShowAddDocumentBottomSheet -> {
                setState { copy(showAddDocumentBottomSheet = event.isOpen) }
            }

            is Event.ShowFiltersBottomSheet -> {
                setState { copy(showFiltersBottomSheet = event.isOpen) }
                if (!event.isOpen) {
                    interactor.clearFilters {
                        setState { copy(filters = it) }
                    }
                }
            }

            is Event.GoToAddDocument -> {
                setState { copy(showAddDocumentBottomSheet = false) }
                goToAddDocument()
            }

            is Event.GoToQrScan -> {
                setState { copy(showAddDocumentBottomSheet = false) }
                goToQrScan()
            }

            is Event.OnSearchQueryChanged -> {
                setState { copy(documents = interactor.searchDocuments(event.query)) }
            }

            is Event.OnFilterSelectionChanged -> {
                interactor.onFilterSelect(event.filterId, event.groupId) {
                    setState { copy(filters = it) }
                }
            }

            is Event.OnFiltersApply -> {
                setState {
                    copy(
                        documents = interactor.applyFilters(documents),
                        showFiltersBottomSheet = false,
                        isFilteringActive = true
                    )
                }
            }

            Event.OnFiltersReset -> {
                val (documents, filters) = interactor.resetFilters()
                setState {
                    copy(
                        documents = documents,
                        filters = filters,
                        showFiltersBottomSheet = false,
                        isFilteringActive = false,
                        sortingOrderButtonData = sortingOrderButtonData.copy(selectedButton = DualSelectorButton.FIRST)
                    )
                }
            }

            is Event.OnSortingOrderChanged -> {
                interactor.onSortingOrderChanged(event.sortingOrder)
                setState { copy(sortingOrderButtonData = sortingOrderButtonData.copy(selectedButton = event.sortingOrder)) }
            }

            is Event.ShowDeferredDocumentsReadyBottomSheet -> {
                setState { copy(showDeferredDocumentsReadyBottomSheet = event.isOpen) }
            }

            is Event.ShowDeferredDocumentPressedBottomSheet -> {
                setState { copy(showDeferredDocumentPressedBottomSheet = event.isOpen) }
            }

            is Event.OptionListItemForSuccessfullyIssuingDeferredDocumentSelected -> {
                setState { copy(showDeferredDocumentsReadyBottomSheet = false) }
                goToDocumentDetails(event.documentId)
            }

            is Event.DeferredNotReadyYet.DocumentSelected -> {
                setState {
                    copy(
                        deferredSelectedDocumentToBeDeleted = event.documentId,
                        showDeferredDocumentPressedBottomSheet = true,
                    )
                }
            }

            is Event.DeferredNotReadyYet.PrimaryButtonPressed -> {
                setState {
                    copy(showDeferredDocumentPressedBottomSheet = false)
                }
                deleteDocument(event = event, documentId = event.documentId)
            }

            is Event.DeferredNotReadyYet.SecondaryButtonPressed -> {
                setState {
                    copy(showDeferredDocumentPressedBottomSheet = false)
                }
            }
        }
    }

    private fun getDocuments(
        event: Event,
        deferredFailedDocIds: List<DocumentId>,
    ) {
        setState {
            copy(
                isLoading = documents.isEmpty(),
                error = null
            )
        }
        viewModelScope.launch {
            interactor.getDocuments().collect { response ->
                when (response) {
                    is DocumentInteractorGetDocumentsPartialState.Failure -> {
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

                    is DocumentInteractorGetDocumentsPartialState.Success -> {
                        val documents = response.documentsUi
                            .map { documentUi ->
                                if (documentUi.uiData.itemId in deferredFailedDocIds) {
                                    documentUi.copy(
                                        documentIssuanceState = DocumentUiIssuanceState.Failed
                                    )
                                } else {
                                    documentUi
                                }
                            }

                        val deferredDocs: MutableMap<DocumentId, FormatType> = mutableMapOf()
                        response.documentsUi.filter { documentUi ->
                            documentUi.documentIssuanceState == DocumentUiIssuanceState.Pending
                        }.forEach { documentUi ->
                            deferredDocs[documentUi.uiData.itemId] =
                                documentUi.documentIdentifier.formatType
                        }

                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                documents = documents,
                                deferredFailedDocIds = deferredFailedDocIds,
                                allowUserInteraction = response.shouldAllowUserInteraction,
                            )
                        }

                        setEffect { Effect.DocumentsFetched(deferredDocs) }
                    }
                }
            }
        }
    }

    private fun tryIssuingDeferredDocuments(
        event: Event,
        deferredDocs: Map<DocumentId, FormatType>
    ) {
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

            delay(5000L)

            interactor.tryIssuingDeferredDocumentsFlow(deferredDocs).collect { response ->
                when (response) {
                    is DocumentInteractorRetryIssuingDeferredDocumentsPartialState.Failure -> {
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

                    is DocumentInteractorRetryIssuingDeferredDocumentsPartialState.Result -> {
                        val successDocs = response.successfullyIssuedDeferredDocuments
                        //TODO
                        /*if (successDocs.isNotEmpty()
                            && (!viewState.value.isBottomSheetOpen
                                    || (viewState.value.isBottomSheetOpen
                                    && viewState.value.sheetContent !is DashboardBottomSheetContent.DeferredDocumentsReady)
                                    )
                        ) {
                            showBottomSheet(
                                sheetContent = DashboardBottomSheetContent.DeferredDocumentsReady(
                                    successfullyIssuedDeferredDocuments = successDocs,
                                    options = getBottomSheetOptions(
                                        deferredDocumentsData = successDocs
                                    )
                                )
                            )
                        }*/

                        val isAnyBottomSheetOpen = with(viewState.value) {
                            listOf(
                                showAddDocumentBottomSheet,
                                showFiltersBottomSheet,
                                showDeferredDocumentsReadyBottomSheet,
                                showDeferredDocumentPressedBottomSheet,
                            ).any { it }
                        }

                        if (successDocs.isNotEmpty()
                            && (!isAnyBottomSheetOpen || viewState.value.showDeferredDocumentsReadyBottomSheet)
                        ) {
                            val updatedItems: List<ModalOptionUi<Event>> =
                                viewState.value.successfullyIssuedDeferredDocumentsUi + successDocs.map {
                                    ModalOptionUi(
                                        title = it.docName,
                                        trailingIcon = AppIcons.KeyboardArrowRight,
                                        event = Event.OptionListItemForSuccessfullyIssuingDeferredDocumentSelected(
                                            documentId = it.documentId
                                        )
                                    )
                                }

                            setState {
                                copy(
                                    successfullyIssuedDeferredDocumentsUi = updatedItems
                                )
                            }

                            setEvent(Event.ShowDeferredDocumentsReadyBottomSheet(isOpen = true))
                        }

                        getDocuments(
                            event = event,
                            deferredFailedDocIds = response.failedIssuedDeferredDocuments
                        )
                    }
                }
            }
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
            interactor.deleteDocument(
                documentId = documentId
            ).collect { response ->
                when (response) {
                    is DocumentInteractorDeleteDocumentPartialState.AllDocumentsDeleted -> {
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

                    is DocumentInteractorDeleteDocumentPartialState.SingleDocumentDeleted -> {
                        //TODO remove that once BottomSheet Logic is sorted out.
                        setState {
                            copy(
                                deferredSelectedDocumentToBeDeleted = null,
                            )
                        }

                        getDocuments(
                            event = event,
                            deferredFailedDocIds = viewState.value.deferredFailedDocIds
                        )
                    }

                    is DocumentInteractorDeleteDocumentPartialState.Failure -> {
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

    private fun goToAddDocument() {
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

    private fun goToQrScan() {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = generateComposableNavigationLink(
                    screen = CommonScreens.QrScan,
                    arguments = generateComposableArguments(
                        mapOf(
                            QrScanUiConfig.serializedKeyName to uiSerializer.toBase64(
                                QrScanUiConfig(
                                    title = resourceProvider.getString(R.string.issuance_qr_scan_title),
                                    subTitle = resourceProvider.getString(R.string.issuance_qr_scan_subtitle),
                                    qrScanFlow = QrScanFlow.Issuance(IssuanceFlowUiConfig.EXTRA_DOCUMENT)
                                ),
                                QrScanUiConfig.Parser
                            )
                        )
                    )
                ),
                inclusive = false
            )
        }
    }
}