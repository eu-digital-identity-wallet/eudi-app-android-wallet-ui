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

import eu.europa.ec.businesslogic.util.dayMonthYearFormatter
import eu.europa.ec.businesslogic.util.fullDateTimeFormatter
import eu.europa.ec.businesslogic.util.hoursMinutesFormatter
import eu.europa.ec.businesslogic.util.isToday
import eu.europa.ec.businesslogic.util.isWithinLastHour
import eu.europa.ec.businesslogic.util.isWithinThisWeek
import eu.europa.ec.businesslogic.util.minutesToNow
import eu.europa.ec.businesslogic.util.plusOneDay
import eu.europa.ec.businesslogic.util.toInstantOrNull
import eu.europa.ec.businesslogic.validator.FilterValidator
import eu.europa.ec.businesslogic.validator.FilterValidatorPartialState
import eu.europa.ec.businesslogic.validator.model.FilterAction
import eu.europa.ec.businesslogic.validator.model.FilterElement
import eu.europa.ec.businesslogic.validator.model.FilterGroup
import eu.europa.ec.businesslogic.validator.model.FilterMultipleAction
import eu.europa.ec.businesslogic.validator.model.FilterableItem
import eu.europa.ec.businesslogic.validator.model.FilterableList
import eu.europa.ec.businesslogic.validator.model.Filters
import eu.europa.ec.businesslogic.validator.model.SortOrder
import eu.europa.ec.corelogic.model.TransactionCategory
import eu.europa.ec.dashboardfeature.model.AttestationPresentationTransaction
import eu.europa.ec.dashboardfeature.model.DocumentSigningTransaction
import eu.europa.ec.dashboardfeature.model.OtherTransaction
import eu.europa.ec.dashboardfeature.model.Transaction
import eu.europa.ec.dashboardfeature.model.TransactionFilterIds
import eu.europa.ec.dashboardfeature.model.TransactionUi
import eu.europa.ec.dashboardfeature.model.TransactionUiStatus
import eu.europa.ec.dashboardfeature.model.TransactionsFilterableAttributes
import eu.europa.ec.dashboardfeature.model.toTransactionUiStatus
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.DualSelectorButton
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.wrap.CheckboxData
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemData
import eu.europa.ec.uilogic.component.wrap.RadioButtonData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime

typealias FilterItem = FilterElement.FilterItem

sealed class TransactionInteractorFilterPartialState {
    data class FilterApplyResult(
        val transactions: List<Pair<TransactionCategory, List<TransactionUi>>>,
        val filters: List<ExpandableListItemData>,
        val sortOrder: DualSelectorButton,
        val allDefaultFiltersAreSelected: Boolean,
    ) : TransactionInteractorFilterPartialState()

    data class FilterUpdateResult(
        val filters: List<ExpandableListItemData>,
        val sortOrder: DualSelectorButton,
    ) : TransactionInteractorFilterPartialState()
}

sealed class TransactionInteractorGetTransactionsPartialState {
    data class Success(
        val allTransactions: FilterableList,
        val shouldAllowUserInteraction: Boolean,
    ) : TransactionInteractorGetTransactionsPartialState()

    data class Failure(val error: String) : TransactionInteractorGetTransactionsPartialState()
}

sealed class TransactionInteractorDateTimeCategoryPartialState {
    data class WithinLastHour(val minutes: Long) :
        TransactionInteractorDateTimeCategoryPartialState()

    data class Today(val time: String) : TransactionInteractorDateTimeCategoryPartialState()
    data class WithinMonth(val date: String) : TransactionInteractorDateTimeCategoryPartialState()
}

interface TransactionsInteractor {
    fun getTestTransactions(): List<Transaction>

    fun getTransactions(): Flow<TransactionInteractorGetTransactionsPartialState>
    fun getTransactionCategory(dateTime: LocalDateTime): TransactionCategory
    fun getTransactionType(transaction: Transaction): String?

    fun initializeFilters(
        filterableList: FilterableList,
    )

    fun applySearch(query: String)
    fun applyFilters()
    fun updateFilter(filterGroupId: String, filterId: String)
    fun updateDateFilterById(
        filterGroupId: String,
        filterId: String,
        lowerLimitDate: Instant,
        upperLimitDate: Instant
    )

    fun addDynamicFilters(transactions: FilterableList, filters: Filters): Filters
    fun getFilters(): Filters
    fun resetFilters()
    fun onFilterStateChange(): Flow<TransactionInteractorFilterPartialState>
    fun updateSortOrder(sortOrder: SortOrder)
    fun revertFilters()
    fun updateLists(filterableList: FilterableList)
}

class TransactionsInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val filterValidator: FilterValidator,
) : TransactionsInteractor {

    override fun revertFilters() = filterValidator.revertFilters()
    override fun updateLists(filterableList: FilterableList) =
        filterValidator.updateLists(filterableList)

    override fun getTestTransactions(): List<Transaction> {
        val now = LocalDateTime.now()
        val someMinutesAgo = now.minusMinutes(20)
        val transactions = listOf(
            DocumentSigningTransaction(
                id = "recent",
                name = "Document Signing",
                status = "Completed",
                creationDate = someMinutesAgo.format(fullDateTimeFormatter)
            ),
            DocumentSigningTransaction(
                id = "t001",
                name = "Document Signing",
                status = "Completed",
                creationDate = now.minusDays(1).format(fullDateTimeFormatter)
            ),
            DocumentSigningTransaction(
                id = "t002",
                name = "Another Document Signing",
                status = "Completed",
                creationDate = "23 February 2025 09:20 AM"
            ),
            DocumentSigningTransaction(
                id = "t003",
                name = "Document Signing",
                status = "Completed",
                creationDate = "20 February 2025 09:20 AM"
            ),
            AttestationPresentationTransaction(
                id = "t004",
                name = "PID Presentation",
                status = "Failed",
                creationDate = "19 February 2025 05:40 PM",
                issuerName = "Test issuer",
                relyingPartyName = "Test relying party",
                attestationType = "Presentation (test)"
            ),
            AttestationPresentationTransaction(
                id = "t005",
                name = "Identity Verification",
                status = "Completed",
                creationDate = "17 February 2025 11:55 AM",
                issuerName = "Test issuer",
                relyingPartyName = "Test relying party",
                attestationType = "Issuance (test)"
            ),
            DocumentSigningTransaction(
                id = "t006",
                name = "Document Signing",
                status = "Completed",
                creationDate = "10 February 2025 01:15 PM"
            ),
            AttestationPresentationTransaction(
                id = "t007",
                name = "Data Sharing Request",
                status = "Failed",
                creationDate = "20 January 2025 04:30 PM",
                issuerName = "Test issuer",
                relyingPartyName = "Test relying party, other",
                attestationType = "Request (test)"
            ),
            DocumentSigningTransaction(
                id = "t008",
                name = "Document Signing",
                status = "Completed",
                creationDate = "20 December 2024 10:05 AM"
            ),
            AttestationPresentationTransaction(
                id = "t009",
                name = "PID Presentation",
                status = "Completed",
                creationDate = "01 March 2024 02:20 PM",
                issuerName = "Test issuer",
                relyingPartyName = "Test relying party",
                attestationType = "Presentation (test)"
            ),
            DocumentSigningTransaction(
                id = "t010",
                name = "Document Signing",
                status = "Failed",
                creationDate = "22 February 2024 09:45 AM"
            ),
            AttestationPresentationTransaction(
                id = "t011",
                name = "Identity Verification",
                status = "Completed",
                creationDate = "17 February 2024 11:30 AM",
                issuerName = "Test issuer",
                relyingPartyName = "Test relying party",
                attestationType = "Verification (test)"
            ),
            DocumentSigningTransaction(
                id = "t012",
                name = "Old Document",
                status = "Completed",
                creationDate = "15 May 1999 10:30 AM"
            )
        )
        return transactions
    }

    override fun initializeFilters(
        filterableList: FilterableList,
    ) = filterValidator.initializeValidator(
        addDynamicFilters(filterableList, getFilters()),
        filterableList
    )

    override fun onFilterStateChange(): Flow<TransactionInteractorFilterPartialState> =
        filterValidator.onFilterStateChange().map { result ->
            val transactionsUi = when (result) {
                is FilterValidatorPartialState.FilterListResult.FilterApplyResult -> {
                    result.filteredList.items.mapNotNull { filterableItem ->
                        filterableItem.payload as? TransactionUi
                    }
                }

                is FilterValidatorPartialState.FilterListResult.FilterListEmptyResult -> {
                    emptyList()
                }

                else -> {
                    emptyList()
                }
            }.groupBy {
                it.transactionCategory
            }.toList()
                .sortByOrder(sortOrder = result.updatedFilters.sortOrder) { groupPair ->
                    groupPair.first.order
                }

            val filtersUi = result.updatedFilters.filterGroups.map { filterGroup ->
                ExpandableListItemData(
                    collapsed = ListItemData(
                        itemId = filterGroup.id,
                        mainContentData = ListItemMainContentData.Text(filterGroup.name),
                        trailingContentData = ListItemTrailingContentData.Icon(
                            iconData = AppIcons.KeyboardArrowRight
                        )
                    ),
                    expanded = filterGroup.filters.map { filterItem ->
                        ListItemData(
                            itemId = filterItem.id,
                            mainContentData = ListItemMainContentData.Text(filterItem.name),
                            trailingContentData = when (filterGroup) {
                                is FilterGroup.MultipleSelectionFilterGroup<*> -> {
                                    ListItemTrailingContentData.Checkbox(
                                        checkboxData = CheckboxData(
                                            isChecked = filterItem.selected,
                                            enabled = true
                                        )
                                    )
                                }

                                is FilterGroup.SingleSelectionFilterGroup -> {
                                    ListItemTrailingContentData.RadioButton(
                                        radioButtonData = RadioButtonData(
                                            isSelected = filterItem.selected,
                                            enabled = true
                                        )
                                    )
                                }

                                is FilterGroup.ReversibleSingleSelectionFilterGroup -> ListItemTrailingContentData.Checkbox(
                                    checkboxData = CheckboxData(
                                        isChecked = filterItem.selected,
                                        enabled = true
                                    )
                                )
                            },
                        )
                    }
                )
            }

            val sortOrderUi = when (result.updatedFilters.sortOrder) {
                is SortOrder.Descending -> DualSelectorButton.FIRST
                is SortOrder.Ascending -> DualSelectorButton.SECOND
            }

            when (result) {
                is FilterValidatorPartialState.FilterListResult -> {
                    TransactionInteractorFilterPartialState.FilterApplyResult(
                        transactions = transactionsUi,
                        filters = filtersUi,
                        sortOrder = sortOrderUi,
                        allDefaultFiltersAreSelected = result.allDefaultFiltersAreSelected
                    )
                }

                is FilterValidatorPartialState.FilterUpdateResult -> {
                    TransactionInteractorFilterPartialState.FilterUpdateResult(
                        filters = filtersUi,
                        sortOrder = sortOrderUi
                    )
                }
            }
        }

    override fun getTransactions(): Flow<TransactionInteractorGetTransactionsPartialState> = flow {
        runCatching {
            val transactions = getTestTransactions()
            val filterableItems = transactions.map { transaction ->
                val dateTime = LocalDateTime.parse(transaction.creationDate, fullDateTimeFormatter)
                val trailingContentData = getTransactionType(transaction)?.let {
                    ListItemTrailingContentData.TextWithIcon(
                        text = it,
                        iconData = AppIcons.KeyboardArrowRight
                    )
                } ?: ListItemTrailingContentData.Icon(iconData = AppIcons.KeyboardArrowRight)

                FilterableItem(
                    payload = TransactionUi(
                        uiData = ListItemData(
                            itemId = transaction.id,
                            mainContentData = ListItemMainContentData.Text(text = transaction.name),
                            overlineText = transaction.status,
                            supportingText = transaction.creationDate.toFormattedDisplayableDate(),
                            trailingContentData = trailingContentData
                        ),
                        uiStatus = transaction.status.toTransactionUiStatus(
                            completedStatusString = resourceProvider.getString(
                                R.string.transaction_status_completed
                            )
                        ),
                        transactionCategory = getTransactionCategory(dateTime = dateTime)
                    ),
                    attributes = TransactionsFilterableAttributes(
                        searchTags = buildList {
                            add(transaction.name)
                        },
                        transactionStatus = transaction.status.toTransactionUiStatus(
                            completedStatusString = resourceProvider.getString(R.string.transaction_status_completed)
                        ),
                        creationDate = transaction.creationDate.toInstantOrNull(),
                        relyingParty = (transaction as? AttestationPresentationTransaction)?.relyingPartyName,
                        attestationName = (transaction as? AttestationPresentationTransaction)?.name,
                        documentName = (transaction as? DocumentSigningTransaction)?.name,
                    )
                )
            }

            emit(
                TransactionInteractorGetTransactionsPartialState.Success(
                    allTransactions = FilterableList(items = filterableItems),
                    shouldAllowUserInteraction = true
                )
            )
        }.onFailure {
            emit(
                TransactionInteractorGetTransactionsPartialState.Failure(
                    error = resourceProvider.getString(R.string.generic_error_message)
                )
            )
        }
    }

    override fun getTransactionCategory(dateTime: LocalDateTime): TransactionCategory {
        val transactionCategory = when {
            dateTime.isToday() -> TransactionCategory.Today
            dateTime.isWithinThisWeek() -> TransactionCategory.ThisWeek
            else -> TransactionCategory.Month(dateTime = dateTime)
        }
        return transactionCategory
    }

    override fun applySearch(query: String) = filterValidator.applySearch(query)

    override fun applyFilters() = filterValidator.applyFilters()

    override fun addDynamicFilters(transactions: FilterableList, filters: Filters): Filters {
        return filters.copy(
            filterGroups = filters.filterGroups.map { filterGroup ->
                when (filterGroup.id) {
                    TransactionFilterIds.FILTER_BY_RELYING_PARTY_GROUP_ID -> {
                        filterGroup as FilterGroup.MultipleSelectionFilterGroup<*>
                        filterGroup.copy(
                            filters = addRelyingPartyFilter(transactions)
                        )
                    }

                    TransactionFilterIds.FILTER_BY_ATTESTATION_GROUP_ID -> {
                        filterGroup as FilterGroup.MultipleSelectionFilterGroup<*>
                        filterGroup.copy(
                            filters = addAttestationFilter(transactions)
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
        sortOrder = SortOrder.Descending(isDefault = true),
        filterGroups = listOf(
            // Sort, descending/ascending buttons will be displayed within this group, identified by group id
            FilterGroup.SingleSelectionFilterGroup(
                id = TransactionFilterIds.FILTER_SORT_GROUP_ID,
                name = resourceProvider.getString(R.string.transactions_screen_filters_sort_by),
                filters = listOf()
            ),

            // Filter by Transaction date
            FilterGroup.SingleSelectionFilterGroup(
                id = TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_GROUP_ID,
                name = resourceProvider.getString(R.string.transactions_screen_filter_by_date_period),
                filters = listOf(
                    FilterElement.DateTimeRangeFilterItem(
                        id = TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_RANGE,
                        name = resourceProvider.getString(R.string.transactions_screen_filter_by_date_period),
                        selected = true,
                        isDefault = true,
                        startDateTime = Instant.MIN,
                        endDateTime = Instant.MAX,
                        filterableAction = FilterAction.Filter<TransactionsFilterableAttributes> { attributes, filter ->
                            val creationDate = attributes.creationDate
                            when {
                                filter is FilterElement.DateTimeRangeFilterItem && creationDate != null -> {
                                    creationDate.isAfter(
                                        filter.startDateTime
                                    ) && creationDate.isBefore(
                                        // plus one day to the end date limit to not filter out same day item
                                        filter.endDateTime.plusOneDay()
                                    )
                                }

                                else -> true
                            }
                        }
                    ),
                ),
            ),

            // Filter by Status
            FilterGroup.MultipleSelectionFilterGroup(
                id = TransactionFilterIds.FILTER_BY_STATUS_GROUP_ID,
                name = resourceProvider.getString(R.string.transactions_screen_filter_by_status),
                filters = listOf(
                    FilterItem(
                        id = TransactionFilterIds.FILTER_BY_STATUS_COMPLETE,
                        name = resourceProvider.getString(R.string.transaction_status_completed),
                        selected = true,
                        isDefault = true,
                    ),
                    FilterItem(
                        id = TransactionFilterIds.FILTER_BY_STATUS_FAILED,
                        name = resourceProvider.getString(R.string.transaction_status_failed),
                        selected = true,
                        isDefault = true,
                    )
                ),
                filterableAction = FilterMultipleAction<TransactionsFilterableAttributes> { attributes, filter ->
                    when (filter.id) {
                        TransactionFilterIds.FILTER_BY_STATUS_COMPLETE -> {
                            attributes.transactionStatus == TransactionUiStatus.Completed
                        }

                        TransactionFilterIds.FILTER_BY_STATUS_FAILED -> attributes.transactionStatus == TransactionUiStatus.Failed

                        else -> true
                    }
                }
            ),

            // Filter by Relying Party
            FilterGroup.MultipleSelectionFilterGroup(
                id = TransactionFilterIds.FILTER_BY_RELYING_PARTY_GROUP_ID,
                name = resourceProvider.getString(R.string.transactions_screen_filters_filter_by_relying_party),
                filters = emptyList(),
                filterableAction = FilterMultipleAction<TransactionsFilterableAttributes> { attributes, filter ->
                    // Check if the "no relying party" filter is selected
                    if (filter.id == TransactionFilterIds.FILTER_BY_RELYING_PARTY_WITHOUT_NAME && filter.selected) {
                        // Return true only for transactions with no relying party
                        return@FilterMultipleAction attributes.relyingParty == null
                    }

                    // Check if the transaction has a relying party and matches the filter name
                    if (attributes.relyingParty != null) {
                        return@FilterMultipleAction attributes.relyingParty == filter.name
                    }

                    // Default case: return false if no conditions are met
                    return@FilterMultipleAction false
                }
            ),

            // Filter by Attestation
            FilterGroup.MultipleSelectionFilterGroup(
                id = TransactionFilterIds.FILTER_BY_ATTESTATION_GROUP_ID,
                name = resourceProvider.getString(R.string.transactions_screen_filters_filter_by_attestation),
                filters = emptyList(),
                filterableAction = FilterMultipleAction<TransactionsFilterableAttributes> { attributes, filter ->
                    if (filter.id == TransactionFilterIds.FILTER_BY_ATTESTATION_WITHOUT_NAME && filter.selected) {
                        // Return true only for transactions with no relying party
                        return@FilterMultipleAction attributes.attestationName == null
                    }

                    // Check if the transaction has a relying party and matches the filter name
                    if (attributes.attestationName != null) {
                        return@FilterMultipleAction attributes.attestationName == filter.name
                    }

                    return@FilterMultipleAction false
                }),

            // Filter by Document Signing
            FilterGroup.ReversibleSingleSelectionFilterGroup(
                id = TransactionFilterIds.FILTER_BY_DOCUMENT_SIGNING_GROUP_ID,
                name = "Filter by Document Signing",
                filters = listOf(
                    FilterItem(
                        id = TransactionFilterIds.FILTER_BY_DOCUMENT_SIGNED,
                        name = "Signed Documents",
                        selected = false,
                        isDefault = false,

                        filterableAction = FilterAction.Filter<TransactionsFilterableAttributes> { attributes, filter ->
                            when (filter.id) {
                                TransactionFilterIds.FILTER_BY_DOCUMENT_SIGNED -> {
                                    attributes.documentName != null
                                }

                                else -> true
                            }
                        }
                    )),
            )
        )
    )

    override fun resetFilters() = filterValidator.resetFilters()

    override fun updateFilter(filterGroupId: String, filterId: String) =
        filterValidator.updateFilter(filterGroupId, filterId)

    override fun updateDateFilterById(
        filterGroupId: String,
        filterId: String,
        lowerLimitDate: Instant,
        upperLimitDate: Instant
    ) {
        filterValidator.updateDateFilter(
            filterGroupId,
            filterId,
            lowerLimitDate,
            upperLimitDate
        )
    }

    override fun updateSortOrder(sortOrder: SortOrder) =
        filterValidator.updateSortOrder(sortOrder)

    override fun getTransactionType(transaction: Transaction): String? {
        val type = when (transaction) {
            is AttestationPresentationTransaction -> transaction.attestationType
            is DocumentSigningTransaction -> "Document Signing"
            is OtherTransaction -> null
        }
        return type
    }

    private fun LocalDateTime.toDateTimeState(): TransactionInteractorDateTimeCategoryPartialState =
        when {
            isWithinLastHour() -> TransactionInteractorDateTimeCategoryPartialState.WithinLastHour(
                minutes = minutesToNow()
            )

            isToday() -> TransactionInteractorDateTimeCategoryPartialState.Today(
                time = format(
                    hoursMinutesFormatter
                )
            )

            else -> TransactionInteractorDateTimeCategoryPartialState.WithinMonth(
                date = format(
                    dayMonthYearFormatter
                )
            )
        }

    private fun String.toFormattedDisplayableDate(): String {
        return runCatching {
            val parsedDate = LocalDateTime.parse(this, fullDateTimeFormatter)

            when (val dateTimeState = parsedDate.toDateTimeState()) {
                is TransactionInteractorDateTimeCategoryPartialState.WithinLastHour -> resourceProvider.getString(
                    R.string.transactions_screen_minutes_ago_message,
                    dateTimeState.minutes
                )

                is TransactionInteractorDateTimeCategoryPartialState.Today -> dateTimeState.time
                is TransactionInteractorDateTimeCategoryPartialState.WithinMonth -> dateTimeState.date
            }
        }.getOrDefault(this)
    }

    private fun <T> List<T>.sortByOrder(
        sortOrder: SortOrder,
        selector: (T) -> Int
    ): List<T> {
        return when (sortOrder) {
            is SortOrder.Ascending -> this.sortedBy(selector)
            is SortOrder.Descending -> this.sortedByDescending(selector)
        }
    }

    private fun addRelyingPartyFilter(transactions: FilterableList): List<FilterItem> {
        return transactions.items
            .distinctBy { (it.attributes as TransactionsFilterableAttributes).relyingParty }
            .map { filterableItem ->
                with(filterableItem.attributes as TransactionsFilterableAttributes) {
                    if (relyingParty != null) {
                        FilterItem(
                            id = relyingParty,
                            name = relyingParty,
                            selected = true,
                            isDefault = true,
                        )
                    } else {
                        FilterItem(
                            id = TransactionFilterIds.FILTER_BY_RELYING_PARTY_WITHOUT_NAME,
                            name = "Transactions without relying party",
                            selected = true,
                            isDefault = true,
                        )
                    }
                }
            }
    }

    private fun addAttestationFilter(transactions: FilterableList): List<FilterItem> {
        return transactions.items
            .distinctBy { (it.attributes as TransactionsFilterableAttributes).attestationName }
            .mapIndexed { index, filterableItem ->
                with(filterableItem.attributes as TransactionsFilterableAttributes) {
                    if (attestationName != null) {
                        FilterItem(
                            id = "${attestationName}_$index",
                            name = attestationName,
                            selected = true,
                            isDefault = true,
                        )
                    } else {
                        FilterItem(
                            id = TransactionFilterIds.FILTER_BY_ATTESTATION_WITHOUT_NAME,
                            name = "Transactions without attestation name",
                            selected = true,
                            isDefault = true,
                        )
                    }
                }
            }
    }
}