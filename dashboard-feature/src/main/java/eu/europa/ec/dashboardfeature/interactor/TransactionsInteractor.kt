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
import eu.europa.ec.businesslogic.util.fullDateTimeFormatter
import eu.europa.ec.businesslogic.util.hoursMinutesFormatter
import eu.europa.ec.businesslogic.util.isJustNow
import eu.europa.ec.businesslogic.util.isToday
import eu.europa.ec.businesslogic.util.isWithinLastHour
import eu.europa.ec.businesslogic.util.isWithinThisWeek
import eu.europa.ec.businesslogic.util.minutesToNow
import eu.europa.ec.businesslogic.util.plusOneDay
import eu.europa.ec.businesslogic.validator.FilterValidator
import eu.europa.ec.businesslogic.validator.FilterValidatorPartialState
import eu.europa.ec.businesslogic.validator.model.FilterAction
import eu.europa.ec.businesslogic.validator.model.FilterElement
import eu.europa.ec.businesslogic.validator.model.FilterElement.FilterItem
import eu.europa.ec.businesslogic.validator.model.FilterGroup
import eu.europa.ec.businesslogic.validator.model.FilterMultipleAction
import eu.europa.ec.businesslogic.validator.model.FilterableItem
import eu.europa.ec.businesslogic.validator.model.FilterableList
import eu.europa.ec.businesslogic.validator.model.Filters
import eu.europa.ec.businesslogic.validator.model.SortOrder
import eu.europa.ec.commonfeature.model.TransactionUiStatus
import eu.europa.ec.commonfeature.model.TransactionUiStatus.Companion.toUiText
import eu.europa.ec.commonfeature.model.TransactionUiType
import eu.europa.ec.commonfeature.model.toTransactionUiStatus
import eu.europa.ec.commonfeature.model.toTransactionUiType
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.model.TransactionCategory
import eu.europa.ec.corelogic.model.TransactionLogData
import eu.europa.ec.corelogic.model.TransactionLogData.Companion.getTransactionTypeLabel
import eu.europa.ec.dashboardfeature.model.TransactionFilterIds
import eu.europa.ec.dashboardfeature.model.TransactionUi
import eu.europa.ec.dashboardfeature.model.TransactionsFilterableAttributes
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.wrap.CheckboxData
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem
import eu.europa.ec.uilogic.component.wrap.RadioButtonData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

sealed class TransactionInteractorFilterPartialState {
    data class FilterApplyResult(
        val transactions: List<Pair<TransactionCategory, List<TransactionUi>>>,
        val filters: List<ExpandableListItem.NestedListItemData>,
        val sortOrder: SortOrder,
        val allDefaultFiltersAreSelected: Boolean,
    ) : TransactionInteractorFilterPartialState()

    data class FilterUpdateResult(
        val filters: List<ExpandableListItem.NestedListItemData>,
        val sortOrder: SortOrder,
    ) : TransactionInteractorFilterPartialState()
}

sealed class TransactionInteractorGetTransactionsPartialState {
    data class Success(
        val allTransactions: FilterableList,
    ) : TransactionInteractorGetTransactionsPartialState()

    data class Failure(val error: String) : TransactionInteractorGetTransactionsPartialState()
}

sealed class TransactionInteractorDateTimeCategoryPartialState {
    data object JustNow : TransactionInteractorDateTimeCategoryPartialState()
    data class WithinLastHour(val minutes: Long) :
        TransactionInteractorDateTimeCategoryPartialState()

    data class Today(val time: String) : TransactionInteractorDateTimeCategoryPartialState()
    data class WithinMonth(val date: String) : TransactionInteractorDateTimeCategoryPartialState()
}

interface TransactionsInteractor {

    fun getTransactions(): Flow<TransactionInteractorGetTransactionsPartialState>

    fun getTransactionCategory(dateTime: LocalDateTime): TransactionCategory

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
    private val walletCoreDocumentsController: WalletCoreDocumentsController
) : TransactionsInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun revertFilters() = filterValidator.revertFilters()
    override fun updateLists(filterableList: FilterableList) =
        filterValidator.updateLists(filterableList)

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

            when (result) {
                is FilterValidatorPartialState.FilterListResult -> {
                    TransactionInteractorFilterPartialState.FilterApplyResult(
                        transactions = transactionsUi,
                        filters = filtersUi,
                        sortOrder = result.updatedFilters.sortOrder,
                        allDefaultFiltersAreSelected = result.allDefaultFiltersAreSelected
                    )
                }

                is FilterValidatorPartialState.FilterUpdateResult -> {
                    TransactionInteractorFilterPartialState.FilterUpdateResult(
                        filters = filtersUi,
                        sortOrder = result.updatedFilters.sortOrder
                    )
                }
            }
        }

    override fun getTransactions(): Flow<TransactionInteractorGetTransactionsPartialState> =
        flow {
            val transactions = walletCoreDocumentsController.getTransactionLogs()
            val filterableItems = transactions.map { transaction ->

                val transactionDateTime = LocalDateTime.ofInstant(
                    transaction.creationDate,
                    ZoneId.systemDefault()
                )

                val trailingContentData = ListItemTrailingContentData.TextWithIcon(
                    text = transaction.getTransactionTypeLabel(resourceProvider),
                    iconData = AppIcons.KeyboardArrowRight
                )

                val transactionName = transaction.name
                val transactionStatus = transaction.status.toTransactionUiStatus()

                FilterableItem(
                    payload = TransactionUi(
                        uiData = ExpandableListItem.SingleListItemData(
                            header = ListItemData(
                                itemId = transaction.id,
                                mainContentData = ListItemMainContentData.Text(text = transactionName),
                                overlineText = transactionStatus.toUiText(resourceProvider),
                                supportingText = transaction.creationDate.toFormattedDisplayableDate(),
                                trailingContentData = trailingContentData
                            )
                        ),
                        uiStatus = transaction.status.toTransactionUiStatus(),
                        transactionCategory = getTransactionCategory(dateTime = transactionDateTime),
                    ),
                    attributes = TransactionsFilterableAttributes(
                        searchTags = buildList {
                            add(transactionName)
                        },
                        transactionStatus = transactionStatus,
                        transactionType = transaction.toTransactionUiType(),
                        creationDate = transaction.creationDate,
                        relyingPartyName = when (transaction) {
                            is TransactionLogData.IssuanceLog -> null // TODO Update this once Core supports Issuance transactions
                            is TransactionLogData.PresentationLog -> transaction.relyingParty.name
                            is TransactionLogData.SigningLog -> null
                        }
                    )
                )
            }

            emit(
                TransactionInteractorGetTransactionsPartialState.Success(
                    allTransactions = FilterableList(items = filterableItems),
                )
            )
        }.safeAsync {
            TransactionInteractorGetTransactionsPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
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
                filters = listOf(
                    FilterItem(
                        id = TransactionFilterIds.FILTER_SORT_TRANSACTION_DATE,
                        name = resourceProvider.getString(R.string.transactions_screen_filters_sort_transaction_date),
                        selected = true,
                        isDefault = true,
                        filterableAction = FilterAction.Sort<TransactionsFilterableAttributes, Instant> { attributes ->
                            attributes.creationDate
                        }
                    ),
                )
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
                            return@Filter isDateAttributeWithinFilterRange(
                                filter = filter,
                                attributes = attributes
                            )
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
                        name = resourceProvider.getString(R.string.transactions_filter_item_status_completed),
                        selected = true,
                        isDefault = true,
                    ),
                    FilterItem(
                        id = TransactionFilterIds.FILTER_BY_STATUS_FAILED,
                        name = resourceProvider.getString(R.string.transactions_filter_item_status_failed),
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
                    // Check if it is the "no relying party" filter
                    if (filter.id == TransactionFilterIds.FILTER_BY_RELYING_PARTY_WITHOUT_NAME) {
                        // Return true only for transactions with no relying party
                        return@FilterMultipleAction attributes.relyingPartyName == null
                    }

                    // Check if the transaction has a relying party and matches the filter name
                    if (attributes.relyingPartyName != null) {
                        return@FilterMultipleAction attributes.relyingPartyName == filter.name
                    }

                    // Default case: return false if no conditions are met
                    return@FilterMultipleAction false
                }
            ),

            // Filter by Transaction Type
            FilterGroup.MultipleSelectionFilterGroup(
                id = TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_GROUP_ID,
                name = resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type),
                filters = listOf(
                    FilterItem(
                        id = TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_PRESENTATION,
                        name = resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_presentation),
                        selected = true,
                        isDefault = true,
                    ),
                    FilterItem(
                        id = TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_ISSUANCE,
                        name = resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_issuance),
                        selected = true,
                        isDefault = true,
                    ),
                    FilterItem(
                        id = TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_SIGNING,
                        name = resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_signing),
                        selected = true,
                        isDefault = true,
                    ),
                ),
                filterableAction = FilterMultipleAction<TransactionsFilterableAttributes> { attributes, filter ->
                    when (filter.id) {
                        TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_PRESENTATION -> {
                            attributes.transactionType == TransactionUiType.PRESENTATION
                        }

                        TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_ISSUANCE -> {
                            attributes.transactionType == TransactionUiType.ISSUANCE
                        }

                        TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_SIGNING -> {
                            attributes.transactionType == TransactionUiType.SIGNING
                        }

                        else -> false
                    }
                }
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

    private fun LocalDateTime.toDateTimeState(): TransactionInteractorDateTimeCategoryPartialState {
        fun String.uppercaseAmPm(): String {
            return this.replace(Regex("\\b(am|pm)\\b")) { it.value.uppercase() }
        }

        return when {
            isJustNow() -> TransactionInteractorDateTimeCategoryPartialState.JustNow

            isWithinLastHour() -> TransactionInteractorDateTimeCategoryPartialState.WithinLastHour(
                minutes = minutesToNow()
            )

            isToday() -> TransactionInteractorDateTimeCategoryPartialState.Today(
                time = format(
                    hoursMinutesFormatter
                ).uppercaseAmPm()
            )

            else -> TransactionInteractorDateTimeCategoryPartialState.WithinMonth(
                date = format(
                    fullDateTimeFormatter
                ).uppercaseAmPm()
            )
        }
    }

    private fun Instant.toFormattedDisplayableDate(zoneId: ZoneId = ZoneId.systemDefault()): String {
        return runCatching {
            val parsedDate = LocalDateTime.ofInstant(this, zoneId)

            when (val dateTimeState = parsedDate.toDateTimeState()) {
                is TransactionInteractorDateTimeCategoryPartialState.JustNow -> resourceProvider.getString(
                    R.string.transactions_screen_0_minutes_ago_message
                )

                is TransactionInteractorDateTimeCategoryPartialState.WithinLastHour -> resourceProvider.getQuantityString(
                    R.plurals.transactions_screen_some_minutes_ago_message,
                    dateTimeState.minutes.toInt(),
                    dateTimeState.minutes
                )

                is TransactionInteractorDateTimeCategoryPartialState.Today -> dateTimeState.time
                is TransactionInteractorDateTimeCategoryPartialState.WithinMonth -> dateTimeState.date
            }
        }.getOrDefault(this.toString())
    }

    private fun addRelyingPartyFilter(transactions: FilterableList): List<FilterItem> {
        val transactionsWithRelyingParty = transactions.items
            .distinctBy { (it.attributes as TransactionsFilterableAttributes).relyingPartyName }
            .mapNotNull { filterableItem ->
                with(filterableItem.attributes as TransactionsFilterableAttributes) {
                    if (relyingPartyName != null) {
                        FilterItem(
                            id = relyingPartyName,
                            name = relyingPartyName,
                            selected = true,
                            isDefault = true,
                        )
                    } else {
                        null
                    }
                }
            }
            .sortedBy { it.name.lowercase() } // Sort by name

        //Put the "Transactions without Relying Party" filter first in the list
        return listOf(
            FilterItem(
                id = TransactionFilterIds.FILTER_BY_RELYING_PARTY_WITHOUT_NAME,
                name = resourceProvider.getString(R.string.transactions_filter_item_no_relying_party_transactions),
                selected = true,
                isDefault = true,
            )
        ) + transactionsWithRelyingParty
    }

    private fun isDateAttributeWithinFilterRange(
        filter: FilterElement,
        attributes: TransactionsFilterableAttributes
    ): Boolean {
        val creationDate = attributes.creationDate
        return when {
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
}