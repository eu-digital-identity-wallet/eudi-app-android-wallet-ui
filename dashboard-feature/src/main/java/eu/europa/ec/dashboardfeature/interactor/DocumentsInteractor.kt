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

import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.businesslogic.util.formatInstant
import eu.europa.ec.commonfeature.model.DocumentUiIssuanceState
import eu.europa.ec.corelogic.controller.DeleteDocumentPartialState
import eu.europa.ec.corelogic.controller.IssueDeferredDocumentPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.extension.localizedIssuerMetadata
import eu.europa.ec.corelogic.model.DeferredDocumentData
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.model.FormatType
import eu.europa.ec.corelogic.model.toDocumentIdentifier
import eu.europa.ec.dashboardfeature.extensions.isBeyondNextDays
import eu.europa.ec.dashboardfeature.extensions.isExpired
import eu.europa.ec.dashboardfeature.extensions.isWithinNextDays
import eu.europa.ec.dashboardfeature.model.DocumentDetailsItemUi
import eu.europa.ec.dashboardfeature.model.FilterableAttributes
import eu.europa.ec.dashboardfeature.model.FilterableDocumentItem
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.resourceslogic.theme.values.ThemeColors
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.DualSelectorButton
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemLeadingContentData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemData
import eu.europa.ec.uilogic.component.wrap.RadioButtonData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val FILTER_BY_STATE_GROUP_ID = "state_group_id"
private const val FILTER_BY_STATE_VALID = "state_valid"
private const val FILTER_BY_STATE_EXPIRED = "state_expired"
private const val FILTER_BY_ISSUER_GROUP_ID = "issuer_group_id"
private const val FILTER_BY_PERIOD_GROUP_ID = "by_period_group_id"
private const val FILTER_BY_PERIOD_NEXT_7 = "by_period_next_7"
private const val FILTER_BY_PERIOD_NEXT_30 = "by_period_next_30"
private const val FILTER_BY_PERIOD_BEYOND_30 = "by_period_beyond_30"
private const val FILTER_BY_PERIOD_EXPIRED = "by_period_expired"
private const val FILTER_SORT_GROUP_ID = "sort_group_id"
private const val FILTER_SORT_DEFAULT = "sort_default"
private const val FILTER_SORT_DATE_ISSUED = "sort_date_issued"
private const val FILTER_SORT_EXPIRY_DATE = "sort_expiry_date"

sealed class DocumentInteractorPartialState {
    data class ResetFilters(
        val documents: List<DocumentDetailsItemUi>,
        val filters: List<ExpandableListItemData>,
    ) : DocumentInteractorPartialState()
}

sealed class DocumentInteractorGetDocumentsPartialState {
    data class Success(
        val documentsUi: List<DocumentDetailsItemUi>,
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
        val deferredDocumentData: DeferredDocumentData
    ) : DocumentInteractorRetryIssuingDeferredDocumentPartialState()

    data class NotReady(
        val deferredDocumentData: DeferredDocumentData
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
        documentId: String
    ): Flow<DocumentInteractorDeleteDocumentPartialState>

    fun searchDocuments(query: String): List<DocumentDetailsItemUi>
    fun getFilters(): List<ExpandableListItemData>
    fun onFilterSelect(
        id: String,
        groupId: String,
        setStateAction: (List<ExpandableListItemData>) -> Unit,
    )

    fun clearFilters(setStateAction: (List<ExpandableListItemData>) -> Unit)
    fun resetFilters(): DocumentInteractorPartialState.ResetFilters
    fun applyFilters(queriedDocuments: List<DocumentDetailsItemUi>): List<DocumentDetailsItemUi>
    fun onSortingOrderChanged(sortingOrder: DualSelectorButton)
}

class DocumentsInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
) : DocumentsInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    private val documents: MutableList<FilterableDocumentItem> = mutableListOf()
    private var filteredDocuments: MutableList<FilterableDocumentItem> = mutableListOf()
    private var sortingOrder: DualSelectorButton = DualSelectorButton.FIRST

    //#region Filters
    private val expandableFilterByExpiryPeriod = ExpandableListItemData(
        collapsed = ListItemData(
            itemId = FILTER_BY_PERIOD_GROUP_ID,
            mainContentData = ListItemMainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_filter_by_expiry_period)),
            trailingContentData = ListItemTrailingContentData.Icon(
                iconData = AppIcons.KeyboardArrowDown
            )
        ),
        expanded = listOf(
            ListItemData(
                itemId = FILTER_BY_PERIOD_NEXT_7,
                mainContentData = ListItemMainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_filter_by_expiry_period_1)),
                trailingContentData = ListItemTrailingContentData.RadioButton(
                    radioButtonData = RadioButtonData(
                        isSelected = false,
                        enabled = true
                    )
                )
            ),
            ListItemData(
                itemId = FILTER_BY_PERIOD_NEXT_30,
                mainContentData = ListItemMainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_filter_by_expiry_period_2)),
                trailingContentData = ListItemTrailingContentData.RadioButton(
                    radioButtonData = RadioButtonData(
                        isSelected = false,
                        enabled = true
                    )
                )
            ),
            ListItemData(
                itemId = FILTER_BY_PERIOD_BEYOND_30,
                mainContentData = ListItemMainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_filter_by_expiry_period_3)),
                trailingContentData = ListItemTrailingContentData.RadioButton(
                    radioButtonData = RadioButtonData(
                        isSelected = false,
                        enabled = true
                    )
                )
            ),
            ListItemData(
                itemId = FILTER_BY_PERIOD_EXPIRED,
                mainContentData = ListItemMainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_filter_by_expiry_period_4)),
                trailingContentData = ListItemTrailingContentData.RadioButton(
                    radioButtonData = RadioButtonData(
                        isSelected = false,
                        enabled = true
                    )
                )
            )
        )
    )

    private val expandableSortFilters = ExpandableListItemData(
        collapsed = ListItemData(
            itemId = FILTER_SORT_GROUP_ID,
            mainContentData = ListItemMainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_sort_by)),
            trailingContentData = ListItemTrailingContentData.Icon(
                iconData = AppIcons.KeyboardArrowDown
            )
        ),
        expanded = listOf(
            ListItemData(
                itemId = FILTER_SORT_DEFAULT,
                mainContentData = ListItemMainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_sort_default)),
                trailingContentData = ListItemTrailingContentData.RadioButton(
                    radioButtonData = RadioButtonData(
                        isSelected = true,
                        enabled = true
                    )
                )
            ),
            ListItemData(
                itemId = FILTER_SORT_DATE_ISSUED,
                mainContentData = ListItemMainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_sort_date_issued)),
                trailingContentData = ListItemTrailingContentData.RadioButton(
                    radioButtonData = RadioButtonData(
                        isSelected = false,
                        enabled = true
                    )
                )
            ),
            ListItemData(
                itemId = FILTER_SORT_EXPIRY_DATE,
                mainContentData = ListItemMainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_sort_expiry_date)),
                trailingContentData = ListItemTrailingContentData.RadioButton(
                    radioButtonData = RadioButtonData(
                        isSelected = false,
                        enabled = true
                    )
                )
            )
        )
    )

    private var issuerFilters = ExpandableListItemData(
        collapsed = ListItemData(
            itemId = FILTER_BY_ISSUER_GROUP_ID,
            mainContentData = ListItemMainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_filter_by_issuer)),
            trailingContentData = ListItemTrailingContentData.Icon(
                iconData = AppIcons.KeyboardArrowDown
            )
        ),
        expanded = listOf()
    )

    private val expandableFilterByState = ExpandableListItemData(
        collapsed = ListItemData(
            itemId = FILTER_BY_STATE_GROUP_ID,
            mainContentData = ListItemMainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_filter_by_state)),
            trailingContentData = ListItemTrailingContentData.Icon(
                iconData = AppIcons.KeyboardArrowDown
            )
        ),
        expanded = listOf(
            ListItemData(
                itemId = FILTER_BY_STATE_VALID,
                mainContentData = ListItemMainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_filter_by_state_valid)),
                trailingContentData = ListItemTrailingContentData.RadioButton(
                    radioButtonData = RadioButtonData(
                        isSelected = false,
                        enabled = true
                    )
                )
            ),
            ListItemData(
                itemId = FILTER_BY_STATE_EXPIRED,
                mainContentData = ListItemMainContentData.Text(resourceProvider.getString(R.string.documents_screen_filters_filter_by_state_expired)),
                trailingContentData = ListItemTrailingContentData.RadioButton(
                    radioButtonData = RadioButtonData(
                        isSelected = false,
                        enabled = true
                    )
                )
            )
        )
    )

    private val filterList =
        mutableListOf(
            expandableSortFilters,
            expandableFilterByExpiryPeriod,
            expandableFilterByState
        )
    private var filterSnapshot = mutableListOf(
        expandableSortFilters,
        expandableFilterByExpiryPeriod,
        expandableFilterByState
    )

    //#endregion

    override fun getDocuments(): Flow<DocumentInteractorGetDocumentsPartialState> =
        flow<DocumentInteractorGetDocumentsPartialState> {
            val shouldAllowUserInteraction =
                walletCoreDocumentsController.getMainPidDocument() != null

            documents.clear()
            documents.addAll(
                walletCoreDocumentsController.getAllDocuments().map { document ->
                    when (document) {
                        is IssuedDocument -> {
                            val localizedIssuerMetadata =
                                document.localizedIssuerMetadata(resourceProvider.getLocale())

                            FilterableDocumentItem(
                                filterableAttributes = FilterableAttributes(
                                    issuedDate = document.issuedAt,
                                    expiryDate = document.validUntil,
                                ),
                                itemUi = DocumentDetailsItemUi(
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
                                )
                            )
                        }

                        is UnsignedDocument -> {
                            val localizedIssuerMetadata =
                                document.localizedIssuerMetadata(resourceProvider.getLocale())

                            FilterableDocumentItem(
                                filterableAttributes = FilterableAttributes(
                                    issuedDate = null,
                                    expiryDate = null,
                                ),
                                itemUi = DocumentDetailsItemUi(
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
                                            tint = ThemeColors.pending,
                                        )
                                    ),
                                    documentIdentifier = document.toDocumentIdentifier(),
                                )
                            )
                        }
                    }
                }
            )

            filteredDocuments.clear().run { filteredDocuments.addAll(documents) }
            createIssuerFilter(filteredDocuments.distinctBy { it.itemUi.uiData.overlineText }
                .map { it.itemUi.uiData.overlineText ?: "" })

            emit(
                DocumentInteractorGetDocumentsPartialState.Success(
                    documentsUi = documents.map { it.itemUi }
                        .sortedBy { (it.uiData.mainContentData as ListItemMainContentData.Text).text },
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

    override fun searchDocuments(query: String): List<DocumentDetailsItemUi> {
        val result = filteredDocuments.map { it.itemUi }.filter {
            (it.uiData.mainContentData as ListItemMainContentData.Text).text.lowercase()
                .contains(query.lowercase())
        }

        createIssuerFilter(result.distinctBy { it.uiData.overlineText }
            .map { it.uiData.overlineText ?: "" })
        return result.ifEmpty {
            listOf(
                DocumentDetailsItemUi(
                    documentIssuanceState = DocumentUiIssuanceState.Issued,
                    uiData = ListItemData(
                        itemId = "",
                        mainContentData = ListItemMainContentData.Text(
                            resourceProvider.getString(
                                R.string.documents_screen_search_no_results
                            )
                        ),
                        overlineText = null,
                        supportingText = null,
                        leadingContentData = null,
                        trailingContentData = null
                    ),
                    documentIdentifier = DocumentIdentifier.OTHER(
                        formatType = ""
                    ),
                )
            )
        }
    }

    override fun getFilters(): List<ExpandableListItemData> {
        return filterList
    }

    override fun onFilterSelect(
        id: String,
        groupId: String,
        setStateAction: (List<ExpandableListItemData>) -> Unit,
    ) {

        filterSnapshot = filterSnapshot.map { checkIfSelected(it, id, groupId) }.toMutableList()

        setStateAction(filterSnapshot)
    }

    override fun clearFilters(setStateAction: (List<ExpandableListItemData>) -> Unit) {
        filterSnapshot = filterList
        setStateAction(filterList)
    }

    override fun resetFilters(): DocumentInteractorPartialState.ResetFilters {
        filterSnapshot =
            mutableListOf(
                expandableSortFilters,
                expandableFilterByExpiryPeriod,
                issuerFilters,
                expandableFilterByState
            )
        sortingOrder = DualSelectorButton.FIRST
        filterList.clear().run { filterList.addAll(filterSnapshot) }
        filteredDocuments.clear().run { filteredDocuments.addAll(documents) }
        return DocumentInteractorPartialState.ResetFilters(
            documents = documents.map { it.itemUi },
            filters = filterSnapshot
        )
    }

    override fun applyFilters(queriedDocuments: List<DocumentDetailsItemUi>): List<DocumentDetailsItemUi> {
        filterList.clear().run { filterList.addAll(filterSnapshot) }
        val selectedFilters = getSelectedItems(filterList)

        selectedFilters.forEach { item ->
            filteredDocuments = when (item.itemId) {
                FILTER_BY_PERIOD_NEXT_7 -> {
                    filteredDocuments.filter { document ->
                        document.filterableAttributes.expiryDate?.isWithinNextDays(7) == true
                    }.toMutableList()
                }

                FILTER_BY_PERIOD_NEXT_30 -> {
                    filteredDocuments.filter { document ->
                        document.filterableAttributes.expiryDate?.isWithinNextDays(30) == true
                    }.toMutableList()
                }

                FILTER_BY_PERIOD_BEYOND_30 -> {
                    filteredDocuments.filter { document ->
                        document.filterableAttributes.expiryDate?.isBeyondNextDays(30) == true
                    }.toMutableList()
                }

                FILTER_BY_PERIOD_EXPIRED -> {
                    filteredDocuments.filter { document ->
                        document.filterableAttributes.expiryDate?.isExpired() == true
                    }.toMutableList()
                }

                FILTER_SORT_DEFAULT -> {
                    filteredDocuments.sortByOrder(sortingOrder) { (it.itemUi.uiData.mainContentData as ListItemMainContentData.Text).text }
                        .toMutableList()
                }

                FILTER_SORT_DATE_ISSUED -> {
                    filteredDocuments.sortByOrder(sortingOrder) { it.filterableAttributes.issuedDate }
                        .toMutableList()
                }

                FILTER_SORT_EXPIRY_DATE -> {
                    filteredDocuments.sortByOrder(sortingOrder) { it.filterableAttributes.expiryDate }
                        .toMutableList()
                }

                "$FILTER_BY_ISSUER_GROUP_ID${(item.mainContentData as ListItemMainContentData.Text).text}" -> {
                    filteredDocuments.filter { document ->
                        document.itemUi.uiData.overlineText == item.itemId.replace(
                            FILTER_BY_ISSUER_GROUP_ID,
                            ""
                        )
                    }.toMutableList()
                }

                FILTER_BY_STATE_VALID -> {
                    filteredDocuments.filter { document ->
                        document.filterableAttributes.expiryDate?.isExpired() == false
                    }.toMutableList()
                }

                FILTER_BY_STATE_EXPIRED -> {
                    filteredDocuments.filter { document ->
                        document.filterableAttributes.expiryDate?.isExpired() == true
                    }.toMutableList()
                }

                else -> filteredDocuments
            }
        }

        return filteredDocuments.map { it.itemUi }.ifEmpty {
            listOf(
                DocumentDetailsItemUi(
                    documentIssuanceState = DocumentUiIssuanceState.Issued,
                    uiData = ListItemData(
                        itemId = "",
                        mainContentData = ListItemMainContentData.Text(
                            resourceProvider.getString(
                                R.string.documents_screen_search_no_results
                            )
                        ),
                        overlineText = null,
                        supportingText = null,
                        leadingContentData = null,
                        trailingContentData = null
                    ),
                    documentIdentifier = DocumentIdentifier.OTHER(
                        formatType = ""
                    ),
                )
            )
        }
    }

    override fun onSortingOrderChanged(sortingOrder: DualSelectorButton) {
        this.sortingOrder = sortingOrder
    }

    private fun createIssuerFilter(issuersList: List<String>) {
        val filterList = issuersList.filter { it.isNotBlank() }

        issuerFilters = issuerFilters.copy(expanded = filterList.map {
            ListItemData(
                itemId = "$FILTER_BY_ISSUER_GROUP_ID$it",
                mainContentData = ListItemMainContentData.Text(it),
                trailingContentData = ListItemTrailingContentData.RadioButton(
                    radioButtonData = RadioButtonData(
                        isSelected = false,
                        enabled = true
                    )
                )
            )
        })
        this.filterList.removeIf { it.collapsed.itemId == FILTER_BY_ISSUER_GROUP_ID }
        this.filterList.add(issuerFilters)

        this.filterSnapshot.removeIf { it.collapsed.itemId == FILTER_BY_ISSUER_GROUP_ID }
        this.filterSnapshot.add(issuerFilters)
    }

    private fun getSelectedItems(filters: List<ExpandableListItemData>): List<ListItemData> {
        return filters.flatMap { expandableItem ->
            expandableItem.expanded.filter { item ->
                val radioButtonData =
                    item.trailingContentData as? ListItemTrailingContentData.RadioButton
                radioButtonData?.radioButtonData?.isSelected == true
            }
        }
    }

    private fun checkIfSelected(
        expandableListItemData: ExpandableListItemData,
        id: String,
        groupId: String,
    ): ExpandableListItemData {
        return expandableListItemData.copy(
            expanded = expandableListItemData.expanded.map { listItemData ->
                if (expandableListItemData.collapsed.itemId
                    != groupId && listItemData.itemId != id
                ) {
                    listItemData
                } else {
                    listItemData.copy(
                        trailingContentData = ListItemTrailingContentData.RadioButton(
                            radioButtonData = RadioButtonData(
                                isSelected = listItemData.itemId == id,
                                enabled = true
                            )
                        )
                    )
                }
            }
        )
    }

    private fun <T, R : Comparable<R>> List<T>.sortByOrder(
        sortOrder: DualSelectorButton,
        selector: (T) -> R?,
    ): List<T> {
        return when (sortOrder) {
            DualSelectorButton.FIRST -> sortedBy(selector)
            DualSelectorButton.SECOND -> sortedByDescending(selector)
        }
    }
}