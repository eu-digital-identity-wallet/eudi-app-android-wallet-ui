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

import eu.europa.ec.businesslogic.extension.isBeyondNextDays
import eu.europa.ec.businesslogic.extension.isExpired
import eu.europa.ec.businesslogic.extension.isValid
import eu.europa.ec.businesslogic.extension.isWithinNextDays
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.businesslogic.util.formatInstant
import eu.europa.ec.businesslogic.validator.FilterValidator
import eu.europa.ec.businesslogic.validator.FilterValidatorPartialState
import eu.europa.ec.businesslogic.validator.model.FilterAction
import eu.europa.ec.businesslogic.validator.model.FilterElement.FilterItem
import eu.europa.ec.businesslogic.validator.model.FilterGroup
import eu.europa.ec.businesslogic.validator.model.FilterMultipleAction
import eu.europa.ec.businesslogic.validator.model.FilterableItem
import eu.europa.ec.businesslogic.validator.model.FilterableList
import eu.europa.ec.businesslogic.validator.model.Filters
import eu.europa.ec.businesslogic.validator.model.SortOrder
import eu.europa.ec.commonfeature.model.DocumentUiIssuanceState
import eu.europa.ec.commonfeature.util.documentHasExpired
import eu.europa.ec.corelogic.controller.DeleteDocumentPartialState
import eu.europa.ec.corelogic.controller.IssueDeferredDocumentPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.extension.localizedIssuerMetadata
import eu.europa.ec.corelogic.model.DeferredDocumentData
import eu.europa.ec.corelogic.model.DocumentCategory
import eu.europa.ec.corelogic.model.FormatType
import eu.europa.ec.corelogic.model.toDocumentCategory
import eu.europa.ec.corelogic.model.toDocumentIdentifier
import eu.europa.ec.dashboardfeature.model.DocumentFilterIds
import eu.europa.ec.dashboardfeature.model.DocumentUi
import eu.europa.ec.dashboardfeature.model.DocumentsFilterableAttributes
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
import eu.europa.ec.uilogic.component.wrap.CheckboxData
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem
import eu.europa.ec.uilogic.component.wrap.RadioButtonData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant

sealed class DocumentInteractorFilterPartialState {
    data class FilterApplyResult(
        val documents: List<Pair<DocumentCategory, List<DocumentUi>>>,
        val filters: List<ExpandableListItem.NestedListItemData>,
        val sortOrder: DualSelectorButton,
        val allDefaultFiltersAreSelected: Boolean,
    ) : DocumentInteractorFilterPartialState()

    data class FilterUpdateResult(
        val filters: List<ExpandableListItem.NestedListItemData>,
        val sortOrder: DualSelectorButton,
    ) : DocumentInteractorFilterPartialState()
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

    fun onFilterStateChange(): Flow<DocumentInteractorFilterPartialState>
    fun initializeFilters(
        filterableList: FilterableList,
    )

    fun updateLists(filterableList: FilterableList)
    fun applyFilters()
    fun applySearch(query: String)
    fun resetFilters()
    fun revertFilters()
    fun updateFilter(filterGroupId: String, filterId: String)
    fun updateSortOrder(sortOrder: SortOrder)
    fun addDynamicFilters(
        documents: FilterableList,
        filters: Filters = Filters.emptyFilters(),
    ): Filters

    fun getFilters(): Filters
}

class DocumentsInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val filterValidator: FilterValidator,
) : DocumentsInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun onFilterStateChange(): Flow<DocumentInteractorFilterPartialState> =
        filterValidator.onFilterStateChange().map { result ->
            val documentsUi = when (result) {
                is FilterValidatorPartialState.FilterListResult.FilterApplyResult -> {
                    result.filteredList.items.mapNotNull { filterableItem ->
                        filterableItem.payload as? DocumentUi
                    }
                }

                is FilterValidatorPartialState.FilterListResult.FilterListEmptyResult -> {
                    emptyList()
                }

                else -> {
                    emptyList()
                }
            }.groupBy {
                it.documentCategory
            }.toList().sortedBy { it.first.order }

            val filtersUi = result.updatedFilters.filterGroups.map { filterGroup ->
                ExpandableListItem.NestedListItemData(
                    isExpanded = true,
                    header = ListItemData(
                        itemId = filterGroup.id,
                        mainContentData = ListItemMainContentData.Text(filterGroup.name),
                        trailingContentData = ListItemTrailingContentData.Icon(
                            iconData = AppIcons.KeyboardArrowRight
                        )
                    ),
                    nestedItems = filterGroup.filters.map { filterItem ->
                        ExpandableListItem.SingleListItemData(
                            header = ListItemData(
                                itemId = filterItem.id,
                                mainContentData = ListItemMainContentData.Text(filterItem.name),
                                trailingContentData = when (filterGroup) {
                                    is FilterGroup.MultipleSelectionFilterGroup<*>,
                                    is FilterGroup.ReversibleMultipleSelectionFilterGroup<*> -> {
                                        ListItemTrailingContentData.Checkbox(
                                            checkboxData = CheckboxData(
                                                isChecked = filterItem.selected,
                                                enabled = true
                                            )
                                        )
                                    }

                                    is FilterGroup.SingleSelectionFilterGroup,
                                    is FilterGroup.ReversibleSingleSelectionFilterGroup -> {
                                        ListItemTrailingContentData.RadioButton(
                                            radioButtonData = RadioButtonData(
                                                isSelected = filterItem.selected,
                                                enabled = true
                                            )
                                        )
                                    }
                                },
                            )
                        )
                    }
                )
            }

            val sortOrderUi = when (result.updatedFilters.sortOrder) {
                is SortOrder.Ascending -> DualSelectorButton.FIRST
                is SortOrder.Descending -> DualSelectorButton.SECOND
            }

            when (result) {
                is FilterValidatorPartialState.FilterListResult -> {
                    DocumentInteractorFilterPartialState.FilterApplyResult(
                        documents = documentsUi,
                        filters = filtersUi,
                        sortOrder = sortOrderUi,
                        allDefaultFiltersAreSelected = result.allDefaultFiltersAreSelected
                    )
                }

                is FilterValidatorPartialState.FilterUpdateResult -> {
                    DocumentInteractorFilterPartialState.FilterUpdateResult(
                        filters = filtersUi,
                        sortOrder = sortOrderUi
                    )
                }
            }
        }

    override fun initializeFilters(
        filterableList: FilterableList,
    ) = filterValidator.initializeValidator(
        addDynamicFilters(filterableList, getFilters()),
        filterableList
    )

    override fun updateLists(filterableList: FilterableList) =
        filterValidator.updateLists(filterableList)

    override fun applySearch(query: String) = filterValidator.applySearch(query)

    override fun revertFilters() = filterValidator.revertFilters()

    override fun updateFilter(filterGroupId: String, filterId: String) =
        filterValidator.updateFilter(filterGroupId, filterId)

    override fun updateSortOrder(sortOrder: SortOrder) =
        filterValidator.updateSortOrder(sortOrder)

    override fun applyFilters() = filterValidator.applyFilters()

    override fun resetFilters() = filterValidator.resetFilters()

    override fun getDocuments(): Flow<DocumentInteractorGetDocumentsPartialState> =
        flow<DocumentInteractorGetDocumentsPartialState> {
            val shouldAllowUserInteraction =
                walletCoreDocumentsController.getMainPidDocument() != null

            val documentCategories = walletCoreDocumentsController.getAllDocumentCategories()

            val userLocale = resourceProvider.getLocale()

            val allDocuments = FilterableList(
                items = walletCoreDocumentsController.getAllDocuments().map { document ->

                    val documentIsRevoked =
                        walletCoreDocumentsController.isDocumentRevoked(document.id)

                    when (document) {
                        is IssuedDocument -> {
                            val localizedIssuerMetadata =
                                document.localizedIssuerMetadata(userLocale)

                            val issuerName = localizedIssuerMetadata?.name

                            val documentIdentifier = document.toDocumentIdentifier()

                            val documentCategory = documentIdentifier.toDocumentCategory(
                                allCategories = documentCategories
                            )

                            val documentSearchTags = buildList {
                                add(document.name)
                                if (!issuerName.isNullOrBlank()) {
                                    add(issuerName)
                                }
                            }

                            val documentHasExpired =
                                documentHasExpired(documentExpirationDate = document.validUntil)

                            val documentIssuanceState = when {
                                documentIsRevoked -> DocumentUiIssuanceState.Revoked
                                documentHasExpired == true -> DocumentUiIssuanceState.Failed
                                else -> DocumentUiIssuanceState.Issued
                            }

                            val supportingText = when {
                                documentIsRevoked -> resourceProvider.getString(R.string.dashboard_document_revoked)
                                documentHasExpired == true -> resourceProvider.getString(R.string.dashboard_document_has_expired)
                                else -> resourceProvider.getString(
                                    R.string.dashboard_document_has_not_expired,
                                    document.validUntil.formatInstant()
                                )
                            }

                            val trailingContentData = if (documentIsRevoked) {
                                ListItemTrailingContentData.Icon(
                                    iconData = AppIcons.ErrorFilled,
                                    tint = ThemeColors.error
                                )
                            } else {
                                ListItemTrailingContentData.Icon(
                                    iconData = AppIcons.KeyboardArrowRight
                                )
                            }

                            FilterableItem(
                                payload = DocumentUi(
                                    documentIssuanceState = documentIssuanceState,
                                    uiData = ListItemData(
                                        itemId = document.id,
                                        mainContentData = ListItemMainContentData.Text(text = document.name),
                                        overlineText = issuerName,
                                        supportingText = supportingText,
                                        leadingContentData = ListItemLeadingContentData.AsyncImage(
                                            imageUrl = localizedIssuerMetadata?.logo?.uri.toString(),
                                            errorImage = AppIcons.Id,
                                        ),
                                        trailingContentData = trailingContentData
                                    ),
                                    documentIdentifier = documentIdentifier,
                                    documentCategory = documentCategory,
                                ),
                                attributes = DocumentsFilterableAttributes(
                                    searchTags = documentSearchTags,
                                    issuedDate = document.issuedAt,
                                    expiryDate = document.validUntil,
                                    issuer = issuerName,
                                    name = document.name,
                                    category = documentCategory,
                                    isRevoked = documentIsRevoked
                                )
                            )
                        }

                        is UnsignedDocument -> {
                            val localizedIssuerMetadata =
                                document.localizedIssuerMetadata(userLocale)

                            val issuerName = localizedIssuerMetadata?.name

                            val documentIdentifier = document.toDocumentIdentifier()

                            val documentCategory = documentIdentifier.toDocumentCategory(
                                allCategories = documentCategories
                            )

                            val documentSearchTags = buildList {
                                add(document.name)
                                if (!issuerName.isNullOrBlank()) {
                                    add(issuerName)
                                }
                            }

                            FilterableItem(
                                payload = DocumentUi(
                                    documentIssuanceState = DocumentUiIssuanceState.Pending,
                                    uiData = ListItemData(
                                        itemId = document.id,
                                        mainContentData = ListItemMainContentData.Text(text = document.name),
                                        overlineText = issuerName,
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
                                    documentIdentifier = documentIdentifier,
                                    documentCategory = documentCategory
                                ),
                                attributes = DocumentsFilterableAttributes(
                                    searchTags = documentSearchTags,
                                    issuedDate = null,
                                    expiryDate = null,
                                    issuer = issuerName,
                                    name = document.name,
                                    category = documentCategory,
                                    isRevoked = documentIsRevoked
                                )
                            )
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

    override fun addDynamicFilters(documents: FilterableList, filters: Filters): Filters {
        return filters.copy(
            filterGroups = filters.filterGroups.map { filterGroup ->
                when (filterGroup.id) {
                    DocumentFilterIds.FILTER_BY_ISSUER_GROUP_ID -> {
                        filterGroup as FilterGroup.MultipleSelectionFilterGroup<*>
                        filterGroup.copy(
                            filters = addIssuerFilter(documents)
                        )
                    }

                    DocumentFilterIds.FILTER_BY_DOCUMENT_CATEGORY_GROUP_ID -> {
                        filterGroup as FilterGroup.MultipleSelectionFilterGroup<*>
                        filterGroup.copy(
                            filters = addDocumentCategoryFilter(documents)
                        )
                    }

                    DocumentFilterIds.FILTER_BY_STATE_GROUP_ID -> {
                        filterGroup as FilterGroup.MultipleSelectionFilterGroup<*>
                        filterGroup.copy(
                            filters = buildList {
                                addAll(filterGroup.filters)
                            }
                        )
                    }

                    else -> {
                        filterGroup
                    }
                }
            },
            sortOrder = filters.sortOrder
        )
    }

    override fun getFilters(): Filters = Filters(
        filterGroups = listOf(
            // Sort
            FilterGroup.SingleSelectionFilterGroup(
                id = DocumentFilterIds.FILTER_SORT_GROUP_ID,
                name = resourceProvider.getString(R.string.documents_screen_filters_sort_by),
                filters = listOf(
                    FilterItem(
                        id = DocumentFilterIds.FILTER_SORT_DEFAULT,
                        name = resourceProvider.getString(R.string.documents_screen_filters_sort_default),
                        selected = true,
                        isDefault = true,
                        filterableAction = FilterAction.Sort<DocumentsFilterableAttributes, String> { attributes ->
                            attributes.name.lowercase()
                        }
                    ),
                    FilterItem(
                        id = DocumentFilterIds.FILTER_SORT_DATE_ISSUED,
                        name = resourceProvider.getString(R.string.documents_screen_filters_sort_date_issued),
                        selected = false,
                        filterableAction = FilterAction.Sort<DocumentsFilterableAttributes, Instant> { attributes ->
                            attributes.issuedDate
                        }
                    ),
                    FilterItem(
                        id = DocumentFilterIds.FILTER_SORT_EXPIRY_DATE,
                        name = resourceProvider.getString(R.string.documents_screen_filters_sort_expiry_date),
                        selected = false,
                        filterableAction = FilterAction.Sort<DocumentsFilterableAttributes, Instant> { attributes ->
                            attributes.expiryDate
                        }
                    )
                )
            ),
            // Filter by expiry period
            FilterGroup.SingleSelectionFilterGroup(
                id = DocumentFilterIds.FILTER_BY_PERIOD_GROUP_ID,
                name = resourceProvider.getString(R.string.documents_screen_filters_filter_by_expiry_period),
                filters = listOf(
                    FilterItem(
                        id = DocumentFilterIds.FILTER_BY_PERIOD_DEFAULT,
                        name = resourceProvider.getString(R.string.documents_screen_filters_sort_default),
                        selected = true,
                        isDefault = true,
                        filterableAction = FilterAction.Filter<DocumentsFilterableAttributes> { _, _ ->
                            true // Get everything
                        }
                    ),
                    FilterItem(
                        id = DocumentFilterIds.FILTER_BY_PERIOD_NEXT_7,
                        name = resourceProvider.getString(R.string.documents_screen_filters_filter_by_expiry_period_1),
                        selected = false,
                        filterableAction = FilterAction.Filter<DocumentsFilterableAttributes> { attributes, _ ->
                            attributes.expiryDate?.isWithinNextDays(7) == true
                        }
                    ),
                    FilterItem(
                        id = DocumentFilterIds.FILTER_BY_PERIOD_NEXT_30,
                        name = resourceProvider.getString(R.string.documents_screen_filters_filter_by_expiry_period_2),
                        selected = false,
                        filterableAction = FilterAction.Filter<DocumentsFilterableAttributes> { attributes, _ ->
                            attributes.expiryDate?.isWithinNextDays(30) == true
                        }
                    ),
                    FilterItem(
                        id = DocumentFilterIds.FILTER_BY_PERIOD_BEYOND_30,
                        name = resourceProvider.getString(R.string.documents_screen_filters_filter_by_expiry_period_3),
                        selected = false,
                        filterableAction = FilterAction.Filter<DocumentsFilterableAttributes> { attributes, _ ->
                            attributes.expiryDate?.isBeyondNextDays(30) == true
                        }
                    ),
                    FilterItem(
                        id = DocumentFilterIds.FILTER_BY_PERIOD_EXPIRED,
                        name = resourceProvider.getString(R.string.documents_screen_filters_filter_by_expiry_period_4),
                        selected = false,
                        filterableAction = FilterAction.Filter<DocumentsFilterableAttributes> { attributes, _ ->
                            attributes.expiryDate?.isExpired() == true
                        }
                    )
                )
            ),
            // Filter by Issuer
            FilterGroup.MultipleSelectionFilterGroup(
                id = DocumentFilterIds.FILTER_BY_ISSUER_GROUP_ID,
                name = resourceProvider.getString(R.string.documents_screen_filters_filter_by_issuer),
                filters = emptyList(),
                filterableAction = FilterMultipleAction<DocumentsFilterableAttributes> { attributes, filter ->
                    attributes.issuer == filter.name
                }
            ),
            // Filter by category
            FilterGroup.MultipleSelectionFilterGroup(
                id = DocumentFilterIds.FILTER_BY_DOCUMENT_CATEGORY_GROUP_ID,
                name = resourceProvider.getString(R.string.documents_screen_filters_filter_by_category),
                filters = emptyList(),
                filterableAction = FilterMultipleAction<DocumentsFilterableAttributes> { attributes, filter ->
                    attributes.category.id.toString() == filter.id
                }
            ),
            // Filter by State
            FilterGroup.MultipleSelectionFilterGroup(
                id = DocumentFilterIds.FILTER_BY_STATE_GROUP_ID,
                name = resourceProvider.getString(R.string.documents_screen_filters_filter_by_state),
                filters = listOf(
                    FilterItem(
                        id = DocumentFilterIds.FILTER_BY_STATE_VALID,
                        name = resourceProvider.getString(R.string.documents_screen_filters_filter_by_state_valid),
                        selected = true,
                        isDefault = true,
                    ),
                    FilterItem(
                        id = DocumentFilterIds.FILTER_BY_STATE_EXPIRED,
                        name = resourceProvider.getString(R.string.documents_screen_filters_filter_by_state_expired),
                        selected = false,
                        isDefault = false,
                    ),
                    FilterItem(
                        id = DocumentFilterIds.FILTER_BY_STATE_REVOKED,
                        name = resourceProvider.getString(R.string.documents_screen_filters_filter_by_state_revoked),
                        selected = false,
                        isDefault = false,
                    ),
                ),
                filterableAction = FilterMultipleAction<DocumentsFilterableAttributes> { attributes, filter ->
                    when (filter.id) {
                        DocumentFilterIds.FILTER_BY_STATE_VALID -> {
                            (attributes.expiryDate?.isValid() == true || attributes.expiryDate == null)
                                    && attributes.isRevoked == false
                        }

                        DocumentFilterIds.FILTER_BY_STATE_EXPIRED -> attributes.expiryDate?.isExpired() == true && attributes.isRevoked == false
                        DocumentFilterIds.FILTER_BY_STATE_REVOKED -> attributes.isRevoked
                        else -> true
                    }
                }
            )
        ),
        sortOrder = SortOrder.Ascending(isDefault = true)
    )

    private fun addDocumentCategoryFilter(documents: FilterableList): List<FilterItem> {
        return documents.items
            .distinctBy { (it.attributes as DocumentsFilterableAttributes).category }
            .map { filterableItem ->
                with(filterableItem.attributes as DocumentsFilterableAttributes) {
                    FilterItem(
                        id = category.id.toString(),
                        name = resourceProvider.getString(category.stringResId),
                        selected = true,
                        isDefault = true
                    )
                }
            }
    }

    private fun addIssuerFilter(documents: FilterableList): List<FilterItem> {
        return documents.items
            .distinctBy { (it.attributes as DocumentsFilterableAttributes).issuer }
            .mapNotNull { filterableItem ->
                with(filterableItem.attributes as DocumentsFilterableAttributes) {
                    if (issuer != null) {
                        FilterItem(
                            id = issuer,
                            name = issuer,
                            selected = true,
                            isDefault = true,
                        )
                    } else {
                        null
                    }
                }
            }
    }

}