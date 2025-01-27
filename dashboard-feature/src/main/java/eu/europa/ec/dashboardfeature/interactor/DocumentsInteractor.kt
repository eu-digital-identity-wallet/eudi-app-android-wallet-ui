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

package eu.europa.ec.dashboardfeature.interactor

import eu.europa.ec.businesslogic.controller.filters.FiltersController
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.businesslogic.model.FilterableItem
import eu.europa.ec.businesslogic.model.FilterableList
import eu.europa.ec.businesslogic.model.Filters
import eu.europa.ec.businesslogic.model.SortOrder
import eu.europa.ec.businesslogic.util.formatInstant
import eu.europa.ec.commonfeature.model.DocumentUiIssuanceState
import eu.europa.ec.corelogic.controller.DeleteDocumentPartialState
import eu.europa.ec.corelogic.controller.IssueDeferredDocumentPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.extension.localizedIssuerMetadata
import eu.europa.ec.corelogic.model.DeferredDocumentData
import eu.europa.ec.corelogic.model.FormatType
import eu.europa.ec.corelogic.model.toDocumentIdentifier
import eu.europa.ec.dashboardfeature.model.DocumentItemUi
import eu.europa.ec.dashboardfeature.model.DocumentsFilterableAttributes
import eu.europa.ec.dashboardfeature.model.FilterableDocumentItem
import eu.europa.ec.dashboardfeature.model.FilterableDocuments
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.resourceslogic.theme.values.ThemeColors
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemLeadingContentData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

sealed class DocumentInteractorFilterPartialState {
    data class FilterResult(val filterableList: FilterableList)
//    data class ResetFilters(
//        val documents: FilterableDocuments,
//        val filters: List<ExpandableListItemData>,
//    ) : DocumentInteractorFilterPartialState()
//
//    data class ApplyFilters(
//        val documents: FilterableDocuments,
//        val filters: List<ExpandableListItemData>,
//    )
//
//    data class SearchResult(
//        val documents: FilterableDocuments,
//        val filters: List<ExpandableListItemData>,
//    )
//
//    data class UpdateFilters(
//        val filters: List<ExpandableListItemData>,
//    )
//
//    data class GetInitialFilters(
//        val filters: List<ExpandableListItemData>,
//    )
}

sealed class DocumentInteractorGetDocumentsPartialState {
    data class Success(
        val allDocuments: FilterableList,
        val shouldAllowUserInteraction: Boolean,
    ) : DocumentInteractorGetDocumentsPartialState()

    data class Failure(val error: String) : DocumentInteractorGetDocumentsPartialState()
}

sealed class DocumentInteractorDeleteDocumentPartialState {
    data object SingleDocumentDeleted : DocumentInteractorDeleteDocumentPartialState()
    data object AllDocumentsDeleted : DocumentInteractorDeleteDocumentPartialState()
    data class Failure(val errorMessage: String) :
        DocumentInteractorDeleteDocumentPartialState()
}

sealed class DocumentInteractorRetryIssuingDeferredDocumentPartialState {
    data class Success(
        val deferredDocumentData: DeferredDocumentData,
    ) : DocumentInteractorRetryIssuingDeferredDocumentPartialState()

    data class NotReady(
        val deferredDocumentData: DeferredDocumentData,
    ) : DocumentInteractorRetryIssuingDeferredDocumentPartialState()

    data class Failure(
        val documentId: DocumentId,
        val errorMessage: String,
    ) : DocumentInteractorRetryIssuingDeferredDocumentPartialState()

    data class Expired(
        val documentId: DocumentId,
    ) : DocumentInteractorRetryIssuingDeferredDocumentPartialState()
}

sealed class DocumentInteractorRetryIssuingDeferredDocumentsPartialState {
    data class Result(
        val successfullyIssuedDeferredDocuments: List<DeferredDocumentData>,
        val failedIssuedDeferredDocuments: List<DocumentId>,
    ) : DocumentInteractorRetryIssuingDeferredDocumentsPartialState()

    data class Failure(
        val errorMessage: String,
    ) : DocumentInteractorRetryIssuingDeferredDocumentsPartialState()
}

interface DocumentsInteractor {
    fun getDocuments(): Flow<DocumentInteractorGetDocumentsPartialState>

    fun tryIssuingDeferredDocumentsFlow(
        deferredDocuments: Map<DocumentId, FormatType>,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): Flow<DocumentInteractorRetryIssuingDeferredDocumentsPartialState>

    fun deleteDocument(
        documentId: String,
    ): Flow<DocumentInteractorDeleteDocumentPartialState>

//    fun getFilters(documents: FilterableDocuments): DocumentInteractorFilterPartialState.GetInitialFilters
//    fun updateFilters(
//        filterId: String,
//        groupId: String,
//        appliedFilters: List<ExpandableListItemData>,
//    ): DocumentInteractorFilterPartialState.UpdateFilters
//
//    fun applyFilters(
//        documents: FilterableDocuments,
//        appliedFilters: List<ExpandableListItemData>,
//    ): DocumentInteractorFilterPartialState.ApplyFilters
//
//    fun resetFilters(
//        documents: FilterableDocuments,
//        appliedFilters: List<ExpandableListItemData>,
//    ): DocumentInteractorFilterPartialState.ResetFilters
//
//    fun searchDocuments(
//        query: String,
//        documents: FilterableDocuments,
//        appliedFilters: List<ExpandableListItemData>,
//    ): DocumentInteractorFilterPartialState.SearchResult

    fun onFilterStateChange(): Flow<DocumentInteractorFilterPartialState.FilterResult>
    fun initializeFilters(
        filters: Filters,
        filterableDocuments: List<FilterableDocumentItem>,
        scope: CoroutineScope? = null,
    )
    fun updateLists(filterableList: FilterableList)
    fun applyFilters()
    fun applySearch(query: String)
    fun resetFilters()
    fun revertFilters()
    fun updateFilter(filterGroupId: String, filterId: String)
    fun updateSortOrder(sortOrder: SortOrder)
}

class DocumentsInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val filtersController: FiltersController,
) : DocumentsInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun onFilterStateChange(): Flow<DocumentInteractorFilterPartialState.FilterResult> = filtersController.onFilterStateChange().map { result ->
        val s = result.filteredList.items.map {
            it.data
        }

        DocumentInteractorFilterPartialState.FilterResult(
            filterableList = listOf()
        )
    }

    override fun initializeFilters(
        filters: Filters,
        filterableDocuments: FilterableDocuments,
        scope: CoroutineScope?,
    ) {
        val filterableList
        filtersController.initializeFilters(filters, filterableList, scope)
    }

    override fun updateLists(filterableList: FilterableList) = filtersController.updateLists(filterableList)

    override fun applySearch(query: String) = filtersController.applySearch(query)

    override fun revertFilters() = filtersController.revertFilters()

    override fun updateFilter(filterGroupId: String, filterId: String) = filtersController.updateFilter(filterGroupId, filterId)

    override fun updateSortOrder(sortOrder: SortOrder) = filtersController.updateSortOrder(sortOrder)

    override fun applyFilters() = filtersController.applyFilters()

    override fun resetFilters() = filtersController.resetFilters()

    override fun getDocuments(): Flow<DocumentInteractorGetDocumentsPartialState> =
        flow<DocumentInteractorGetDocumentsPartialState> {
            val shouldAllowUserInteraction =
                walletCoreDocumentsController.getMainPidDocument() != null

            val allDocuments = FilterableList(items = walletCoreDocumentsController.getAllDocuments().map { document ->
                    when (document) {
                        is IssuedDocument -> {
                            val localizedIssuerMetadata =
                                document.localizedIssuerMetadata(resourceProvider.getLocale())
                            FilterableItem(
                                data = DocumentItemUi(
                                    documentIssuanceState = DocumentUiIssuanceState.Issued,
                                    uiData = ListItemData(
                                        itemId = document.id,
                                        mainContentData = ListItemMainContentData.Text(text = document.name),
                                        overlineText = localizedIssuerMetadata?.name,
                                        supportingText = "${resourceProvider.getString(R.string.dashboard_document_has_not_expired)}: " +
                                                document.validUntil.formatInstant(),
                                        leadingContentData = ListItemLeadingContentData.AsyncImage(
                                            imageUrl = localizedIssuerMetadata?.logo?.uri.toString(),
                                            errorImage = AppIcons.Id,
                                        ),
                                        trailingContentData = ListItemTrailingContentData.Icon(
                                            iconData = AppIcons.KeyboardArrowRight
                                        )
                                    ),
                                    documentIdentifier = document.toDocumentIdentifier(),
                                ),
                                attributes = DocumentsFilterableAttributes(
                                    issuedDate = document.issuedAt,
                                    expiryDate = document.validUntil,
                                    issuer = localizedIssuerMetadata?.name,
                                    name = document.name,
                                    searchText = document.name
                                )
                            )
//                            FilterableDocumentItem(
//                                filterableAttributes = DocumentsFilterableAttributes(
//                                    issuedDate = document.issuedAt,
//                                    expiryDate = document.validUntil,
//                                    issuer = localizedIssuerMetadata?.name,
//                                    name = document.name,
//                                    searchText = document.name
//                                ),
//                                itemUi = DocumentItemUi(
//                                    documentIssuanceState = DocumentUiIssuanceState.Issued,
//                                    uiData = ListItemData(
//                                        itemId = document.id,
//                                        mainContentData = ListItemMainContentData.Text(text = document.name),
//                                        overlineText = localizedIssuerMetadata?.name,
//                                        supportingText = "${resourceProvider.getString(R.string.dashboard_document_has_not_expired)}: " +
//                                                document.validUntil.formatInstant(),
//                                        leadingContentData = ListItemLeadingContentData.AsyncImage(
//                                            imageUrl = localizedIssuerMetadata?.logo?.uri.toString(),
//                                            errorImage = AppIcons.Id,
//                                        ),
//                                        trailingContentData = ListItemTrailingContentData.Icon(
//                                            iconData = AppIcons.KeyboardArrowRight
//                                        )
//                                    ),
//                                    documentIdentifier = document.toDocumentIdentifier(),
//                                )
//                            )
                        }

                        is UnsignedDocument -> {
                            val localizedIssuerMetadata =
                                document.localizedIssuerMetadata(resourceProvider.getLocale())

                            FilterableItem(
                                data = DocumentItemUi(
                                    documentIssuanceState = DocumentUiIssuanceState.Pending,
                                    uiData = ListItemData(
                                        itemId = document.id,
                                        mainContentData = ListItemMainContentData.Text(text = document.name),
                                        overlineText = localizedIssuerMetadata?.name,
                                        supportingText = resourceProvider.getString(R.string.dashboard_document_deferred_pending),
                                        leadingContentData = ListItemLeadingContentData.AsyncImage(
                                            imageUrl = localizedIssuerMetadata?.logo?.uri.toString(),
                                            errorImage = AppIcons.Id,
                                        ),
                                        trailingContentData = ListItemTrailingContentData.Icon(
                                            iconData = AppIcons.ClockTimer,
                                            tint = ThemeColors.warning,
                                        )
                                    ),
                                    documentIdentifier = document.toDocumentIdentifier(),
                                ),
                                attributes = DocumentsFilterableAttributes(
                                    issuedDate = null,
                                    expiryDate = null,
                                    issuer = localizedIssuerMetadata?.name,
                                    name = document.name,
                                    searchText = document.name
                                )
                            )
//                            FilterableDocumentItem(
//                                filterableAttributes = DocumentsFilterableAttributes(
//                                    issuedDate = null,
//                                    expiryDate = null,
//                                    issuer = localizedIssuerMetadata?.name,
//                                    name = document.name,
//                                    searchText = document.name
//                                ),
//                                itemUi = DocumentItemUi(
//                                    documentIssuanceState = DocumentUiIssuanceState.Pending,
//                                    uiData = ListItemData(
//                                        itemId = document.id,
//                                        mainContentData = ListItemMainContentData.Text(text = document.name),
//                                        overlineText = localizedIssuerMetadata?.name,
//                                        supportingText = resourceProvider.getString(R.string.dashboard_document_deferred_pending),
//                                        leadingContentData = ListItemLeadingContentData.AsyncImage(
//                                            imageUrl = localizedIssuerMetadata?.logo?.uri.toString(),
//                                            errorImage = AppIcons.Id,
//                                        ),
//                                        trailingContentData = ListItemTrailingContentData.Icon(
//                                            iconData = AppIcons.ClockTimer,
//                                            tint = ThemeColors.warning,
//                                        )
//                                    ),
//                                    documentIdentifier = document.toDocumentIdentifier(),
//                                )
//                            )
                        }
                    }
                }
            )

            emit(
                DocumentInteractorGetDocumentsPartialState.Success(
                    allDocuments = allDocuments,
                    shouldAllowUserInteraction = shouldAllowUserInteraction,
                )
            )
        }.safeAsync {
            DocumentInteractorGetDocumentsPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun tryIssuingDeferredDocumentsFlow(
        deferredDocuments: Map<DocumentId, FormatType>,
        dispatcher: CoroutineDispatcher,
    ): Flow<DocumentInteractorRetryIssuingDeferredDocumentsPartialState> = flow {

        val successResults: MutableList<DeferredDocumentData> = mutableListOf()
        val failedResults: MutableList<DocumentId> = mutableListOf()

        withContext(dispatcher) {
            val allJobs = deferredDocuments.keys.map { deferredDocumentId ->
                async {
                    tryIssuingDeferredDocumentSuspend(deferredDocumentId)
                }
            }

            allJobs.forEach { job ->
                when (val result = job.await()) {
                    is DocumentInteractorRetryIssuingDeferredDocumentPartialState.Failure -> {
                        failedResults.add(result.documentId)
                    }

                    is DocumentInteractorRetryIssuingDeferredDocumentPartialState.Success -> {
                        successResults.add(result.deferredDocumentData)
                    }

                    is DocumentInteractorRetryIssuingDeferredDocumentPartialState.NotReady -> {}

                    is DocumentInteractorRetryIssuingDeferredDocumentPartialState.Expired -> {
                        deleteDocument(result.documentId)
                    }
                }
            }
        }

        emit(
            DocumentInteractorRetryIssuingDeferredDocumentsPartialState.Result(
                successfullyIssuedDeferredDocuments = successResults,
                failedIssuedDeferredDocuments = failedResults
            )
        )

    }.safeAsync {
        DocumentInteractorRetryIssuingDeferredDocumentsPartialState.Failure(
            errorMessage = it.localizedMessage ?: genericErrorMsg
        )
    }

    private suspend fun tryIssuingDeferredDocumentSuspend(
        deferredDocumentId: DocumentId,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): DocumentInteractorRetryIssuingDeferredDocumentPartialState {
        return withContext(dispatcher) {
            walletCoreDocumentsController.issueDeferredDocument(deferredDocumentId)
                .map { result ->
                    when (result) {
                        is IssueDeferredDocumentPartialState.Failed -> {
                            DocumentInteractorRetryIssuingDeferredDocumentPartialState.Failure(
                                documentId = result.documentId,
                                errorMessage = result.errorMessage
                            )
                        }

                        is IssueDeferredDocumentPartialState.Issued -> {
                            DocumentInteractorRetryIssuingDeferredDocumentPartialState.Success(
                                deferredDocumentData = result.deferredDocumentData
                            )
                        }

                        is IssueDeferredDocumentPartialState.NotReady -> {
                            DocumentInteractorRetryIssuingDeferredDocumentPartialState.NotReady(
                                deferredDocumentData = result.deferredDocumentData
                            )
                        }

                        is IssueDeferredDocumentPartialState.Expired -> {
                            DocumentInteractorRetryIssuingDeferredDocumentPartialState.Expired(
                                documentId = result.documentId
                            )
                        }
                    }
                }.firstOrNull()
                ?: DocumentInteractorRetryIssuingDeferredDocumentPartialState.Failure(
                    documentId = deferredDocumentId,
                    errorMessage = genericErrorMsg
                )
        }
    }

    override fun deleteDocument(
        documentId: String,
    ): Flow<DocumentInteractorDeleteDocumentPartialState> =
        flow {
            walletCoreDocumentsController.deleteDocument(documentId).collect { response ->
                when (response) {
                    is DeleteDocumentPartialState.Failure -> {
                        emit(
                            DocumentInteractorDeleteDocumentPartialState.Failure(
                                errorMessage = response.errorMessage
                            )
                        )
                    }

                    is DeleteDocumentPartialState.Success -> {
                        if (walletCoreDocumentsController.getAllDocuments().isEmpty()) {
                            emit(DocumentInteractorDeleteDocumentPartialState.AllDocumentsDeleted)
                        } else
                            emit(DocumentInteractorDeleteDocumentPartialState.SingleDocumentDeleted)
                    }
                }
            }
        }.safeAsync {
            DocumentInteractorDeleteDocumentPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMsg
            )
        }

//    override fun getFilters(documents: FilterableDocuments): DocumentInteractorFilterPartialState.GetInitialFilters {
//        return DocumentInteractorFilterPartialState.GetInitialFilters(
//            filtersController.getAllFilter(documents)
//        )
//    }

//    override fun updateFilters(
//        filterId: String,
//        groupId: String,
//        appliedFilters: List<ExpandableListItemData>,
//    ): DocumentInteractorFilterPartialState.UpdateFilters {
//        return DocumentInteractorFilterPartialState.UpdateFilters(
//            filters = filtersController.updateFilter(filterId, groupId, appliedFilters)
//        )
//    }
//
//    override fun applyFilters(
//        documents: FilterableDocuments,
//        appliedFilters: List<ExpandableListItemData>,
//    ): DocumentInteractorFilterPartialState.ApplyFilters {
//        val (filteredDocuments, filters) = filtersController.applyFilters(documents, appliedFilters)
//        return DocumentInteractorFilterPartialState.ApplyFilters(filteredDocuments, filters)
//    }

//    override fun resetFilters(
//        documents: FilterableDocuments,
//        appliedFilters: List<ExpandableListItemData>,
//    ): DocumentInteractorFilterPartialState.ResetFilters {
//        val (filteredDocuments, filters) = filtersController.resetFilters(documents, appliedFilters)
//        return DocumentInteractorFilterPartialState.ResetFilters(filteredDocuments, filters)
//    }

//    override fun searchDocuments(
//        query: String,
//        documents: FilterableDocuments,
//        appliedFilters: List<ExpandableListItemData>,
//    ): DocumentInteractorFilterPartialState.SearchResult {
//        val (searchedDocuments, filters) = filtersController.applySearch(
//            documents,
//            appliedFilters,
//            query
//        )
//        return DocumentInteractorFilterPartialState.SearchResult(searchedDocuments, filters)
//    }
}