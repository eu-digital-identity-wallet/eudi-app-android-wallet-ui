/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.dashboardfeature.ui.documents.list

import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.config.IssuanceFlowType
import eu.europa.ec.commonfeature.config.IssuanceUiConfig
import eu.europa.ec.commonfeature.config.QrScanFlow
import eu.europa.ec.commonfeature.config.QrScanUiConfig
import eu.europa.ec.corelogic.model.DocumentCategory
import eu.europa.ec.dashboardfeature.interactor.DocumentInteractorDeleteDocumentPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentInteractorFilterPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentInteractorGetDocumentsPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentsInteractor
import eu.europa.ec.dashboardfeature.ui.documents.list.DocumentsBottomSheetContent.DeferredDocumentPressed
import eu.europa.ec.dashboardfeature.ui.documents.list.DocumentsBottomSheetContent.Filters
import eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentUi
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import eu.europa.ec.uilogic.extension.collapsedExpansionState
import eu.europa.ec.uilogic.extension.toggleExpansionState
import eu.europa.ec.uilogic.extension.withExpansionStateFrom
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
import kotlinx.coroutines.launch

/**
 * @property isEditingFilters Protects the editable draft from the opening request through actual
 * closure.
 * @property isRefreshPending Coalesces document changes delayed by Filters into one reload.
 * @property isApplyingDocumentFilters Keeps new highlights off the previous search or filter
 * results.
 */
data class State(
    val isLoading: Boolean,
    val error: ContentErrorConfig? = null,
    val isBottomSheetOpen: Boolean = false,
    val sheetContent: DocumentsBottomSheetContent = Filters,

    val documentsUi: List<Pair<DocumentCategory, List<DocumentUi>>> = emptyList(),
    val unseenDeferredIssuanceIds: Set<String> = emptySet(),
    val showNoResultsFound: Boolean = false,
    val searchText: String = "",
    val allowUserInteraction: Boolean = true,
    val shouldRevertFilterChanges: Boolean = true,
    val isEditingFilters: Boolean = false,
    val isRefreshPending: Boolean = false,
    val isApplyingDocumentFilters: Boolean = false,

    val filtersUi: List<ExpandableListItemUi.NestedListItem> = emptyList(),
    val isFilteringActive: Boolean,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object GetDocuments : Event()
    data object OnPause : Event()
    data object OnScreenDisposed : Event()
    data class DeferredDocumentIssued(val documentId: String) : Event()
    data class DeferredIssuanceHighlightShown(val documentId: String) : Event()
    data class DeferredDocumentStatusChanged(val documentId: String) : Event()

    data object Pop : Event()
    data class GoToDocumentDetails(val docId: DocumentId) : Event()
    data class OnSearchQueryChanged(val query: String) : Event()

    data object AddDocumentPressed : Event()
    data object FiltersPressed : Event()

    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(val isOpen: Boolean) : BottomSheet()

        sealed class Filters : BottomSheet() {
            data class SelectionChanged(val filterId: String, val groupId: String) : Filters()
            data class ExpansionChanged(val groupId: String) : Filters()
            data object Reset : Filters()
            data object Apply : Filters()
        }

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

    data object ShowBottomSheet : Effect()
    data object CloseBottomSheet : Effect()
}

sealed class DocumentsBottomSheetContent {
    data object Filters : DocumentsBottomSheetContent()

    data object AddDocument : DocumentsBottomSheetContent()
    data class DeferredDocumentPressed(val documentId: DocumentId) : DocumentsBottomSheetContent()
}

class DocumentsViewModel(
    private val interactor: DocumentsInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
) : MviViewModel<Event, State, Effect>() {

    private var filterStateJob: Job? = null
    private var fetchDocumentsJob: Job? = null

    /** A Remove tap can still arrive while the sheet is closing after issuance. */
    private var documentIdToIgnoreRemoveTapsFor: DocumentId? = null

    init {
        viewModelScope.launch {
            interactor.unseenDeferredIssuanceIds.collect { ids ->
                setState { copy(unseenDeferredIssuanceIds = ids) }
            }
        }
    }

    override fun setInitialState(): State {
        return State(
            isLoading = true,
            isFilteringActive = false
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                filterStateChanged()
            }

            is Event.GetDocuments,
            is Event.DeferredDocumentStatusChanged -> getDocuments(Event.GetDocuments)

            is Event.DeferredDocumentIssued -> {
                closeRemovalSheetIfOpenFor(issuedDocumentId = event.documentId)
                getDocuments(Event.GetDocuments)
            }

            is Event.DeferredIssuanceHighlightShown ->
                interactor.markDeferredIssuanceSeen(event.documentId)

            is Event.OnPause -> stopFetchDocuments()

            is Event.OnScreenDisposed -> {
                stopFetchDocuments()
                finishFiltering(refresh = false)
            }

            is Event.Pop -> setEffect { Effect.Navigation.Pop }

            is Event.GoToDocumentDetails -> {
                goToDocumentDetails(event.docId)
            }

            is Event.AddDocumentPressed -> {
                showBottomSheet(sheetContent = DocumentsBottomSheetContent.AddDocument)
            }

            is Event.FiltersPressed -> {
                collapseFilterGroups()
                showBottomSheet(sheetContent = Filters)
            }

            is Event.OnSearchQueryChanged -> {
                applySearch(event.query)
            }

            is Event.BottomSheet.Filters.SelectionChanged -> {
                updateFilter(event.filterId, event.groupId)
            }

            is Event.BottomSheet.Filters.ExpansionChanged -> {
                updateFilterGroupExpansion(event.groupId)
            }

            is Event.BottomSheet.Filters.Apply -> {
                applySelectedFilters()
            }

            is Event.BottomSheet.Filters.Reset -> {
                resetFilters()
            }

            is Event.BottomSheet.UpdateBottomSheetState -> {
                setState { copy(isBottomSheetOpen = event.isOpen) }
                if (!event.isOpen) {
                    documentIdToIgnoreRemoveTapsFor = null
                    finishFiltering(refresh = true)
                }
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
                if (event.documentId in viewState.value.unseenDeferredIssuanceIds) {
                    getDocuments(Event.GetDocuments)
                    return
                }
                showBottomSheet(
                    sheetContent = DeferredDocumentPressed(
                        documentId = event.documentId
                    )
                )
            }

            is Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.PrimaryButtonPressed -> {
                if (event.documentId == documentIdToIgnoreRemoveTapsFor ||
                    event.documentId in viewState.value.unseenDeferredIssuanceIds
                ) return
                hideBottomSheet()
                deleteDocument(event = event, documentId = event.documentId)
            }

            is Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.SecondaryButtonPressed -> {
                hideBottomSheet()
            }

        }
    }

    /** A queued Apply result must not replace an open Filters draft. */
    private fun filterStateChanged() {
        if (filterStateJob?.isActive == true) return
        filterStateJob = viewModelScope.launch {
            interactor.onFilterStateChange().collect { result ->
                when (result) {
                    is DocumentInteractorFilterPartialState.FilterApplyResult -> {
                        if (viewState.value.isEditingFilters) {
                            setState { copy(isRefreshPending = true) }
                            return@collect
                        }
                        setState {
                            copy(
                                isFilteringActive = !result.allDefaultFiltersAreSelected,
                                documentsUi = result.documents,
                                isApplyingDocumentFilters = false,
                                showNoResultsFound = result.documents.isEmpty(),
                                filtersUi = result.filters.withExpansionStateFrom(
                                    currentItems = filtersUi
                                )
                            )
                        }
                    }

                    is DocumentInteractorFilterPartialState.FilterUpdateResult -> {
                        setState {
                            copy(
                                filtersUi = result.filters.withExpansionStateFrom(
                                    currentItems = filtersUi
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * A load started before Filters opened may still deliver a result, which then only marks a
     * refresh as pending. A successful load rebuilds the dynamic filter options while keeping
     * valid applied selections.
     */
    private fun getDocuments(
        event: Event,
    ) {
        if (viewState.value.isEditingFilters) {
            setState { copy(isRefreshPending = true) }
            return
        }
        stopFetchDocuments()
        setState {
            copy(
                isLoading = documentsUi.isEmpty(),
                error = null
            )
        }
        fetchDocumentsJob = viewModelScope.launch {
            interactor.getDocuments()
                .collect { response ->
                    if (viewState.value.isEditingFilters) {
                        setState { copy(isRefreshPending = true) }
                        return@collect
                    }
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
                            setState {
                                copy(
                                    isLoading = false,
                                    error = null,
                                    allowUserInteraction = response.shouldAllowUserInteraction,
                                    isApplyingDocumentFilters = true,
                                )
                            }
                            interactor.initializeFilters(response.allDocuments)
                            interactor.applyFilters()
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
                        getDocuments(
                            event = event,
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
                    screen = DashboardScreens.DocumentDetails,
                    arguments = generateComposableArguments(
                        mapOf(
                            "documentId" to docId
                        )
                    )
                )
            )
        }
    }

    private fun goToAddDocument() {
        val addDocumentScreenRoute = generateComposableNavigationLink(
            screen = IssuanceScreens.AddDocument,
            arguments = generateComposableArguments(
                mapOf(
                    IssuanceUiConfig.serializedKeyName to uiSerializer.toBase64(
                        model = IssuanceUiConfig(
                            flowType = IssuanceFlowType.ExtraDocument(
                                formatType = null
                            )
                        ),
                        parser = IssuanceUiConfig.Parser
                    )
                )
            )
        )
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = addDocumentScreenRoute
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
                                    qrScanFlow = QrScanFlow.Issuance(
                                        issuanceFlowType = IssuanceFlowType.ExtraDocument(
                                            formatType = null
                                        )
                                    )
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

    /**
     * Opening Filters freezes the displayed options first. Issuer pings continue independently.
     */
    private fun showBottomSheet(sheetContent: DocumentsBottomSheetContent) {
        if (sheetContent is Filters) {
            setState {
                copy(
                    isEditingFilters = true,
                    isRefreshPending = isRefreshPending || isApplyingDocumentFilters ||
                            fetchDocumentsJob?.isActive == true,
                    isLoading = false,
                    shouldRevertFilterChanges = true,
                )
            }
            stopFetchDocuments()
        }
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

    private fun closeRemovalSheetIfOpenFor(issuedDocumentId: DocumentId) {
        val state = viewState.value
        val removalSheet = state.sheetContent as? DeferredDocumentPressed
        if (state.isBottomSheetOpen
            && removalSheet?.documentId == issuedDocumentId
            && documentIdToIgnoreRemoveTapsFor == null
        ) {
            documentIdToIgnoreRemoveTapsFor = issuedDocumentId
            hideBottomSheet()
        }
    }

    private fun applySearch(queryText: String) {
        setState { copy(searchText = queryText, isApplyingDocumentFilters = true) }
        interactor.applySearch(queryText)
    }

    private fun updateFilter(filterId: String, groupId: String) {
        setState { copy(shouldRevertFilterChanges = true) }
        interactor.updateFilter(filterGroupId = groupId, filterId = filterId)
    }

    private fun updateFilterGroupExpansion(groupId: String) {
        setState {
            copy(
                filtersUi = filtersUi.toggleExpansionState(groupId)
                    .filterIsInstance<ExpandableListItemUi.NestedListItem>()
            )
        }
    }

    private fun collapseFilterGroups() {
        setState {
            copy(filtersUi = filtersUi.collapsedExpansionState())
        }
    }

    private fun applySelectedFilters() {
        setState { copy(shouldRevertFilterChanges = false, isApplyingDocumentFilters = true) }
        interactor.applyFilters()
        hideBottomSheet()
    }

    private fun resetFilters() {
        setState { copy(shouldRevertFilterChanges = false, isApplyingDocumentFilters = true) }
        interactor.resetFilters()
        hideBottomSheet()
    }

    /**
     * Called after reported sheet closure or real screen disposal. Resolves the draft before a
     * refresh rebuilds the issuer and category options.
     */
    private fun finishFiltering(refresh: Boolean) {
        if (!viewState.value.isEditingFilters) return
        if (viewState.value.shouldRevertFilterChanges) interactor.revertFilters()
        val needsRefresh = viewState.value.isRefreshPending
        setState {
            copy(
                isEditingFilters = false,
                isRefreshPending = false,
                shouldRevertFilterChanges = true,
            )
        }
        if (refresh && needsRefresh) getDocuments(Event.GetDocuments)
    }

    private fun stopFetchDocuments() {
        fetchDocumentsJob?.cancel()
    }
}