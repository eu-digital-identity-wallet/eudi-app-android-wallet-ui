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
import eu.europa.ec.corelogic.model.DeferredDocumentData
import eu.europa.ec.corelogic.model.FormatType
import eu.europa.ec.dashboardfeature.extensions.getEmptyUIifEmptyList
import eu.europa.ec.dashboardfeature.extensions.search
import eu.europa.ec.dashboardfeature.interactor.DocumentInteractorDeleteDocumentPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentInteractorGetDocumentsPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentInteractorRetryIssuingDeferredDocumentsPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentsInteractor
import eu.europa.ec.dashboardfeature.model.DocumentUi
import eu.europa.ec.dashboardfeature.model.FilterableDocuments
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.resourceslogic.theme.values.ThemeColors
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.DualSelectorButton
import eu.europa.ec.uilogic.component.DualSelectorButtonData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
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
    val isBottomSheetOpen: Boolean = false,
    val sheetContent: DocumentsBottomSheetContent = DocumentsBottomSheetContent.Filters(filters = emptyList()),

    val documents: List<DocumentUi> = emptyList(),
    val deferredFailedDocIds: List<DocumentId> = emptyList(),
    val allowUserInteraction: Boolean = true,
    val isInitialDocumentLoading: Boolean = true,

    var filters: List<ExpandableListItemData> = emptyList(),
    var initialFilters: List<ExpandableListItemData> = emptyList(),
    val appliedFilters: List<ExpandableListItemData> = emptyList(),
    var snapshotFilters: List<ExpandableListItemData> = emptyList(),
    val isFilteringActive: Boolean,
    val sortingOrderButtonDataApplied: DualSelectorButtonData,
    val sortingOrderButtonDataSnapshot: DualSelectorButtonData? = null,
    val sortingOrderButtonDataPrevious: DualSelectorButtonData? = null,

    var allDocuments: FilterableDocuments? = null,
    var filteredDocuments: FilterableDocuments? = null,

    val queryText: String = "",
) : ViewState {
    val groups = documents.groupBy { it.documentCategory }
        .toList()
}

sealed class Event : ViewEvent {
    data object GetDocuments : Event()
    data object OnPause : Event()
    data class TryIssuingDeferredDocuments(val deferredDocs: Map<DocumentId, FormatType>) : Event()
    data object Pop : Event()
    data class GoToDocumentDetails(val docId: DocumentId) : Event()
    data class OnSearchQueryChanged(val query: String) : Event()
    data class OnFilterSelectionChanged(val filterId: String, val groupId: String) : Event()
    data object OnFiltersReset : Event()
    data object OnFiltersApply : Event()
    data class OnSortingOrderChanged(val sortingOrder: DualSelectorButton) : Event()

    data object AddDocumentPressed : Event()
    data object FiltersPressed : Event()

    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(val isOpen: Boolean) : BottomSheet()
        data object Close : BottomSheet()

        sealed class AddDocument : BottomSheet() {
            data object FromList : AddDocument()
            data object ScanQr : AddDocument()
        }

        sealed class DeferredDocument : BottomSheet() {
            sealed class DeferredNotReadyYet(
                open val documentId: DocumentId,
            ) : DeferredDocument() {
                data class DocumentSelected(
                    override val documentId: DocumentId,
                ) : DeferredNotReadyYet(documentId)

                data class PrimaryButtonPressed(
                    override val documentId: DocumentId,
                ) : DeferredNotReadyYet(documentId)

                data class SecondaryButtonPressed(
                    override val documentId: DocumentId,
                ) : DeferredNotReadyYet(documentId)
            }

            data class OptionListItemForSuccessfullyIssuingDeferredDocumentSelected(
                val documentId: DocumentId,
            ) : DeferredDocument()
        }
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

    data object ShowBottomSheet : Effect()
    data object CloseBottomSheet : Effect()

    data object ResumeOnApplyFilter : Effect()
}

sealed class DocumentsBottomSheetContent {
    data class Filters(val filters: List<ExpandableListItemData>) : DocumentsBottomSheetContent()
    data object AddDocument : DocumentsBottomSheetContent()
    data class DeferredDocumentPressed(val documentId: DocumentId) : DocumentsBottomSheetContent()
    data class DeferredDocumentsReady(
        val successfullyIssuedDeferredDocuments: List<DeferredDocumentData>,
        val options: List<ModalOptionUi<Event>>,
    ) : DocumentsBottomSheetContent()
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
            sortingOrderButtonDataApplied = DualSelectorButtonData(
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
                getDocuments(
                    event = event,
                    deferredFailedDocIds = viewState.value.deferredFailedDocIds,
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

            is Event.AddDocumentPressed -> {
                showBottomSheet(sheetContent = DocumentsBottomSheetContent.AddDocument)
            }

            is Event.FiltersPressed -> {
                setEvent(Event.OnPause)
                showBottomSheet(sheetContent = DocumentsBottomSheetContent.Filters(filters = emptyList()))
            }

            is Event.OnSearchQueryChanged -> {
                applySearch(event.query)
            }

            is Event.OnFilterSelectionChanged -> {
                updateFilter(event.filterId, event.groupId)
            }

            is Event.OnFiltersApply -> {
                applySelectedFilters()
            }

            is Event.OnFiltersReset -> {
                resetFilters()
            }

            is Event.OnSortingOrderChanged -> {
                val selection = viewState.value.sortingOrderButtonDataApplied.copy(
                    selectedButton = event.sortingOrder
                )
                setState {
                    copy(
                        sortingOrderButtonDataSnapshot = selection,
                        sortingOrderButtonDataApplied = selection,
                        sortingOrderButtonDataPrevious = viewState.value.sortingOrderButtonDataApplied,
                    )
                }
            }

            is Event.BottomSheet.UpdateBottomSheetState -> {
                if (viewState.value.sheetContent is DocumentsBottomSheetContent.Filters) {
                    if (!event.isOpen) {
                        if (viewState.value.sheetContent is DocumentsBottomSheetContent.Filters) {
                            // Check if selection should be applied or discarded
                            val appliedSorting =
                                if (viewState.value.sortingOrderButtonDataSnapshot == null) {
                                    // Applying filtering
                                    viewState.value.sortingOrderButtonDataSnapshot
                                        ?: viewState.value.sortingOrderButtonDataApplied
                                } else {
                                    // Closing the bottom sheet without apply action
                                    viewState.value.sortingOrderButtonDataPrevious
                                        ?: viewState.value.sortingOrderButtonDataApplied
                                }

                            setState {
                                copy(
                                    snapshotFilters = emptyList(),
                                    isBottomSheetOpen = false,
                                    filters = viewState.value.appliedFilters,
                                    sortingOrderButtonDataApplied = appliedSorting,
                                    sortingOrderButtonDataSnapshot = null,
                                    sortingOrderButtonDataPrevious = appliedSorting,
                                )
                            }
                            setEffect { Effect.ResumeOnApplyFilter }
                        }
                    } else {
                        setState {
                            copy(isBottomSheetOpen = true)
                        }
                    }
                } else {
                    setState {
                        copy(isBottomSheetOpen = event.isOpen)
                    }
                }
            }

            is Event.BottomSheet.Close -> {
                hideBottomSheet()
            }

            is Event.BottomSheet.AddDocument.FromList -> {
                hideBottomSheet()
                goToAddDocument()
            }

            is Event.BottomSheet.AddDocument.ScanQr -> {
                hideBottomSheet()
                goToQrScan()
            }

            is Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.DocumentSelected -> {
                showBottomSheet(
                    sheetContent = DocumentsBottomSheetContent.DeferredDocumentPressed(
                        documentId = event.documentId
                    )
                )
            }

            is Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.PrimaryButtonPressed -> {
                hideBottomSheet()
                deleteDocument(event = event, documentId = event.documentId)
            }

            is Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.SecondaryButtonPressed -> {
                hideBottomSheet()
            }

            is Event.BottomSheet.DeferredDocument.OptionListItemForSuccessfullyIssuingDeferredDocumentSelected -> {
                hideBottomSheet()
                goToDocumentDetails(docId = event.documentId)
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
            interactor.getDocuments(
                viewState.value.sortingOrderButtonDataApplied.selectedButton,
                viewState.value.filters,
                viewState.value.queryText
            )
                .collect { response ->
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
                            val deferredDocs: MutableMap<DocumentId, FormatType> = mutableMapOf()
                            response.allDocuments.documents.filter { document ->
                                document.itemUi.documentIssuanceState == DocumentUiIssuanceState.Pending
                            }.forEach { documentUi ->
                                deferredDocs[documentUi.itemUi.uiData.itemId] =
                                    documentUi.itemUi.documentIdentifier.formatType
                            }

                            val filteredDocumentsWithFailed =
                                response.filterableDocuments.generateFailedDeferredDocs(
                                    deferredFailedDocIds
                                )
                            val allDocumentsWithFailed =
                                response.allDocuments.generateFailedDeferredDocs(
                                    deferredFailedDocIds
                                )
                            val filters =
                                filteredDocumentsWithFailed.let {
                                    if (shouldApplyFilters()) {
                                        interactor.applyFilters(
                                            it,
                                            viewState.value.filters
                                        ).filters
                                    } else {
                                        interactor.getFilters(allDocumentsWithFailed).filters
                                    }
                                }
                            val initialFilters =
                                interactor.getFilters(allDocumentsWithFailed).filters

                            setState {
                                copy(
                                    isLoading = false,
                                    error = null,
                                    documents = filteredDocumentsWithFailed.search(viewState.value.queryText)
                                        .getEmptyUIifEmptyList(resourceProvider).documents.map { it.itemUi },
                                    filteredDocuments = filteredDocumentsWithFailed,
                                    allDocuments = allDocumentsWithFailed,
                                    filters = filters,
                                    initialFilters = initialFilters,
                                    deferredFailedDocIds = deferredFailedDocIds,
                                    allowUserInteraction = response.shouldAllowUserInteraction,
                                    isInitialDocumentLoading = false
                                )
                            }
                            setEffect { Effect.DocumentsFetched(deferredDocs) }
                        }
                    }
                }
        }
    }

    private fun FilterableDocuments.generateFailedDeferredDocs(deferredFailedDocIds: List<DocumentId>): FilterableDocuments {
        return copy(documents = documents.map { documentUi ->
            if (documentUi.itemUi.uiData.itemId in deferredFailedDocIds) {
                documentUi.copy(
                    itemUi = documentUi.itemUi.copy(
                        documentIssuanceState = DocumentUiIssuanceState.Failed,
                        uiData = documentUi.itemUi.uiData.copy(
                            supportingText = resourceProvider.getString(R.string.dashboard_document_deferred_failed),
                            trailingContentData = ListItemTrailingContentData.Icon(
                                iconData = AppIcons.ErrorFilled,
                                tint = ThemeColors.error
                            )
                        )
                    )
                )
            } else {
                documentUi
            }
        })
    }

    private fun tryIssuingDeferredDocuments(
        event: Event,
        deferredDocs: Map<DocumentId, FormatType>,
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
                        if (successDocs.isNotEmpty()
                            && (!viewState.value.isBottomSheetOpen
                                    || (viewState.value.isBottomSheetOpen
                                    && viewState.value.sheetContent !is DocumentsBottomSheetContent.DeferredDocumentsReady)
                                    )
                        ) {
                            showBottomSheet(
                                sheetContent = DocumentsBottomSheetContent.DeferredDocumentsReady(
                                    successfullyIssuedDeferredDocuments = successDocs,
                                    options = getBottomSheetOptions(
                                        deferredDocumentsData = successDocs
                                    )
                                )
                            )
                        }

                        getDocuments(
                            event = event,
                            deferredFailedDocIds = response.failedIssuedDeferredDocuments,
                        )
                    }
                }
            }
        }
    }

    private fun shouldApplyFilters(): Boolean {
        return with(viewState.value) {
            isFilteringActive && !isInitialDocumentLoading
        }
    }

    private fun getBottomSheetOptions(deferredDocumentsData: List<DeferredDocumentData>): List<ModalOptionUi<Event>> {
        return deferredDocumentsData.map {
            ModalOptionUi(
                title = it.docName,
                trailingIcon = AppIcons.KeyboardArrowRight,
                event = Event.BottomSheet.DeferredDocument.OptionListItemForSuccessfullyIssuingDeferredDocumentSelected(
                    documentId = it.documentId
                )
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
                        getDocuments(
                            event = event,
                            deferredFailedDocIds = viewState.value.deferredFailedDocIds,
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

    private fun applySearch(queryText: String) {
        val searchResult =
            viewState.value.filteredDocuments?.let { documents ->
                interactor.searchDocuments(
                    query = queryText,
                    documents = documents,
                    appliedFilters = viewState.value.filters
                )
            }
        setState {
            copy(
                documents = searchResult?.documents?.documents?.map { it.itemUi }
                    ?: emptyList(),
                filters = searchResult?.filters ?: emptyList(),
                queryText = queryText
            )
        }
    }

    private fun updateFilter(filterId: String, groupId: String) {
        val updatedFilters =
            interactor.updateFilters(
                filterId,
                groupId,
                viewState.value.filters
            )

        setState {
            copy(
                snapshotFilters = updatedFilters.filters,
                filters = updatedFilters.filters
            )
        }
    }

    private fun applySelectedFilters() {
        val filtersToApply = viewState.value.snapshotFilters.ifEmpty {
            viewState.value.filters
        }
        val applied =
            viewState.value.allDocuments?.let {
                interactor.applyFilters(
                    it,
                    filtersToApply
                )
            }
        val appliedFilterDocuments = applied?.documents
        val appliedFilters = applied?.filters ?: emptyList()
        setState {
            copy(
                documents = appliedFilterDocuments?.search(queryText)?.documents?.map { it.itemUi }
                    ?: emptyList(),
                filteredDocuments = appliedFilterDocuments,
                appliedFilters = appliedFilters,
                sortingOrderButtonDataSnapshot = null,
                filters = appliedFilters,
                isFilteringActive = true
            )
        }
        hideBottomSheet()
    }

    private fun resetFilters() {
        val applied = viewState.value.allDocuments?.let { documents ->
            interactor.resetFilters(
                documents,
                viewState.value.initialFilters
            )
        }
        val appliedFilterDocuments = viewState.value.allDocuments
        val appliedFilters = applied?.filters ?: emptyList()
        setState {
            copy(
                filters = appliedFilters,
                filteredDocuments = appliedFilterDocuments,
                sortingOrderButtonDataSnapshot = null,
                sortingOrderButtonDataApplied = sortingOrderButtonDataApplied.copy(selectedButton = DualSelectorButton.FIRST),
                appliedFilters = appliedFilters,
                isFilteringActive = false
            )
        }
        hideBottomSheet()
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

    private fun showBottomSheet(sheetContent: DocumentsBottomSheetContent) {
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