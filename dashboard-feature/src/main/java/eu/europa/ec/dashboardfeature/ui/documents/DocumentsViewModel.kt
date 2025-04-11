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
import eu.europa.ec.businesslogic.validator.model.FilterableList
import eu.europa.ec.businesslogic.validator.model.SortOrder
import eu.europa.ec.commonfeature.config.IssuanceFlowUiConfig
import eu.europa.ec.commonfeature.config.QrScanFlow
import eu.europa.ec.commonfeature.config.QrScanUiConfig
import eu.europa.ec.commonfeature.model.DocumentUiIssuanceState
import eu.europa.ec.corelogic.model.DeferredDocumentData
import eu.europa.ec.corelogic.model.DocumentCategory
import eu.europa.ec.corelogic.model.FormatType
import eu.europa.ec.dashboardfeature.interactor.DocumentInteractorDeleteDocumentPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentInteractorFilterPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentInteractorGetDocumentsPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentInteractorRetryIssuingDeferredDocumentsPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentsInteractor
import eu.europa.ec.dashboardfeature.model.DocumentUi
import eu.europa.ec.dashboardfeature.ui.documents.DocumentsBottomSheetContent.DeferredDocumentPressed
import eu.europa.ec.dashboardfeature.ui.documents.DocumentsBottomSheetContent.Filters
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
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem
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
    val sheetContent: DocumentsBottomSheetContent = Filters(filters = emptyList()),

    val documentsUi: List<Pair<DocumentCategory, List<DocumentUi>>> = emptyList(),
    val showNoResultsFound: Boolean = false,
    val deferredFailedDocIds: List<DocumentId> = emptyList(),
    val searchText: String = "",
    val allowUserInteraction: Boolean = true,
    val isFromOnPause: Boolean = true,
    val shouldRevertFilterChanges: Boolean = true,

    val filtersUi: List<ExpandableListItem.NestedListItemData> = emptyList(),
    val sortOrder: DualSelectorButtonData,
    val isFilteringActive: Boolean,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
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
    data class Filters(val filters: List<ExpandableListItem.SingleListItemData>) :
        DocumentsBottomSheetContent()

    data object AddDocument : DocumentsBottomSheetContent()
    data class DeferredDocumentPressed(val documentId: DocumentId) : DocumentsBottomSheetContent()
    data class DeferredDocumentsReady(
        val successfullyIssuedDeferredDocuments: List<DeferredDocumentData>,
        val options: List<ModalOptionUi<Event>>,
    ) : DocumentsBottomSheetContent()
}

@KoinViewModel
class DocumentsViewModel(
    private val interactor: DocumentsInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
) : MviViewModel<Event, State, Effect>() {

    private var retryDeferredDocsJob: Job? = null
    private var fetchDocumentsJob: Job? = null

    override fun setInitialState(): State {
        return State(
            isLoading = true,
            sortOrder = DualSelectorButtonData(
                first = resourceProvider.getString(R.string.documents_screen_filters_ascending),
                second = resourceProvider.getString(R.string.documents_screen_filters_descending),
                selectedButton = DualSelectorButton.FIRST,
            ),
            isFilteringActive = false
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                filterStateChanged()
            }

            is Event.GetDocuments -> {
                getDocuments(
                    event = event,
                    deferredFailedDocIds = viewState.value.deferredFailedDocIds,
                )
            }

            is Event.OnPause -> {
                stopDeferredIssuing()
                stopFetchDocuments()
                setState { copy(isFromOnPause = true) }
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
                stopDeferredIssuing()
                showBottomSheet(sheetContent = Filters(filters = emptyList()))
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
                sortOrderChanged(event.sortingOrder)
            }

            is Event.BottomSheet.UpdateBottomSheetState -> {
                if (viewState.value.sheetContent is Filters
                    && !event.isOpen
                ) {
                    setEffect { Effect.ResumeOnApplyFilter }
                }
                revertFilters(event.isOpen)
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
                    sheetContent = DeferredDocumentPressed(
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

    private fun filterStateChanged() {
        viewModelScope.launch {
            interactor.onFilterStateChange().collect { result ->
                when (result) {
                    is DocumentInteractorFilterPartialState.FilterApplyResult -> {
                        setState {
                            copy(
                                isFilteringActive = !result.allDefaultFiltersAreSelected,
                                documentsUi = result.documents,
                                showNoResultsFound = result.documents.isEmpty(),
                                filtersUi = result.filters,
                                sortOrder = sortOrder.copy(selectedButton = result.sortOrder)
                            )
                        }
                    }

                    is DocumentInteractorFilterPartialState.FilterUpdateResult -> {
                        setState {
                            copy(
                                filtersUi = result.filters,
                                sortOrder = sortOrder.copy(selectedButton = result.sortOrder)
                            )
                        }
                    }
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
                isLoading = documentsUi.isEmpty(),
                error = null
            )
        }
        fetchDocumentsJob = viewModelScope.launch {
            interactor.getDocuments()
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
                            response.allDocuments.items.filter { document ->
                                with(document.payload as DocumentUi) {
                                    documentIssuanceState == DocumentUiIssuanceState.Pending
                                }
                            }.forEach { documentItem ->
                                with(documentItem.payload as DocumentUi) {
                                    deferredDocs[uiData.itemId] =
                                        documentIdentifier.formatType
                                }
                            }
                            val documentsWithFailed =
                                response.allDocuments
                                    .generateFailedDeferredDocs(deferredFailedDocIds)

                            if (viewState.value.isFromOnPause) {
                                interactor.initializeFilters(
                                    filterableList = documentsWithFailed
                                )
                            } else {
                                interactor.updateLists(
                                    filterableList = documentsWithFailed
                                )
                            }

                            interactor.applyFilters()

                            setState {
                                copy(
                                    isLoading = false,
                                    error = null,
                                    deferredFailedDocIds = deferredFailedDocIds,
                                    allowUserInteraction = response.shouldAllowUserInteraction,
                                    isFromOnPause = false
                                )
                            }
                            setEffect { Effect.DocumentsFetched(deferredDocs) }
                        }
                    }
                }
        }
    }

    private fun FilterableList.generateFailedDeferredDocs(deferredFailedDocIds: List<DocumentId>): FilterableList {
        return copy(items = items.map { filterableItem ->
            val data = filterableItem.payload as DocumentUi
            val failedUiItem = if (data.uiData.itemId in deferredFailedDocIds) {
                data.copy(
                    documentIssuanceState = DocumentUiIssuanceState.Failed,
                    uiData = data.uiData.copy(
                        supportingText = resourceProvider.getString(R.string.dashboard_document_deferred_failed),
                        trailingContentData = ListItemTrailingContentData.Icon(
                            iconData = AppIcons.ErrorFilled,
                            tint = ThemeColors.error
                        )
                    )
                )
            } else {
                data
            }

            filterableItem.copy(payload = failedUiItem)
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

        stopDeferredIssuing()
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

    private fun applySearch(queryText: String) {
        interactor.applySearch(queryText)
        setState {
            copy(searchText = queryText)
        }
    }

    private fun updateFilter(filterId: String, groupId: String) {
        setState { copy(shouldRevertFilterChanges = true) }
        interactor.updateFilter(filterGroupId = groupId, filterId = filterId)
    }

    private fun applySelectedFilters() {
        interactor.applyFilters()
        setState {
            copy(
                shouldRevertFilterChanges = false
            )
        }
        hideBottomSheet()
    }

    private fun resetFilters() {
        interactor.resetFilters()
        hideBottomSheet()
    }

    private fun revertFilters(isOpening: Boolean) {
        if (viewState.value.sheetContent is Filters
            && !isOpening
            && viewState.value.shouldRevertFilterChanges
        ) {
            interactor.revertFilters()
            setState { copy(shouldRevertFilterChanges = true) }
        }

        setState {
            copy(isBottomSheetOpen = isOpening)
        }
    }

    private fun sortOrderChanged(orderButton: DualSelectorButton) {
        val sortOrder = when (orderButton) {
            DualSelectorButton.FIRST -> SortOrder.Ascending(isDefault = true)
            DualSelectorButton.SECOND -> SortOrder.Descending()
        }
        setState { copy(shouldRevertFilterChanges = true) }
        interactor.updateSortOrder(sortOrder)
    }

    private fun stopDeferredIssuing() {
        retryDeferredDocsJob?.cancel()
    }

    private fun stopFetchDocuments() {
        fetchDocumentsJob?.cancel()
    }
}