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

import eu.europa.ec.businesslogic.util.fullDateTimeFormatter
import eu.europa.ec.businesslogic.util.hoursMinutesFormatter
import eu.europa.ec.businesslogic.util.isToday
import eu.europa.ec.businesslogic.util.isWithinLastHour
import eu.europa.ec.businesslogic.util.isWithinThisWeek
import eu.europa.ec.businesslogic.util.minutesToNow
import eu.europa.ec.businesslogic.util.shortDateTimeFormatter
import eu.europa.ec.businesslogic.validator.FilterValidator
import eu.europa.ec.businesslogic.validator.FilterValidatorPartialState
import eu.europa.ec.businesslogic.validator.model.FilterGroup
import eu.europa.ec.businesslogic.validator.model.FilterItem
import eu.europa.ec.businesslogic.validator.model.FilterMultipleAction
import eu.europa.ec.businesslogic.validator.model.FilterableItem
import eu.europa.ec.businesslogic.validator.model.FilterableList
import eu.europa.ec.businesslogic.validator.model.Filters
import eu.europa.ec.businesslogic.validator.model.SortOrder
import eu.europa.ec.corelogic.model.TransactionCategory
import eu.europa.ec.dashboardfeature.model.FilterIds
import eu.europa.ec.dashboardfeature.model.Transaction
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
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

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

    fun initializeFilters(
        filterableList: FilterableList,
    )

    fun applySearch(query: String)
    fun applyFilters()
    fun addDynamicFilters(documents: FilterableList, filters: Filters): Filters
    fun getFilters(): Filters
    fun onFilterStateChange(): Flow<TransactionInteractorFilterPartialState>
}

class TransactionsInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val filterValidator: FilterValidator,
) : TransactionsInteractor {
    override fun getTestTransactions(): List<Transaction> {
        val now = LocalDateTime.now()
        val someMinutesAgo = now.minusMinutes(20)
        val transactions = listOf(
            Transaction(
                id = "recent",
                name = "Document Signing",
                status = "Completed",
                creationDate = someMinutesAgo.format(fullDateTimeFormatter)
            ),
            Transaction(
                id = "t000",
                name = "Document Signing",
                status = "Completed",
                creationDate = "24 February 2025 09:20 AM"
            ),
            Transaction(
                id = "t000",
                name = "Document Signing",
                status = "Completed",
                creationDate = "23 February 2025 9:20 AM"
            ),
            Transaction(
                id = "t001",
                name = "Document Signing",
                status = "Completed",
                creationDate = "20 February 2025 9:20 AM"
            ),
            Transaction(
                id = "t002",
                name = "PID Presentation",
                status = "Failed",
                creationDate = "19 February 2025 5:40 PM"
            ),
            Transaction(
                id = "t003",
                name = "Identity Verification",
                status = "Completed",
                creationDate = "17 February 2025 11:55 AM"
            ),
            Transaction(
                id = "t004",
                name = "Document Signing",
                status = "Completed",
                creationDate = "10 February 2025 1:15 PM"
            ),
            Transaction(
                id = "t005",
                name = "Data Sharing Request",
                status = "Failed",
                creationDate = "20 January 2025 4:30 PM"
            ),
            Transaction(
                id = "t006",
                name = "Document Signing",
                status = "Completed",
                creationDate = "20 December 2024 10:05 AM"
            ),
            Transaction(
                id = "t007",
                name = "PID Presentation",
                status = "Completed",
                creationDate = "1 March 2024 2:20 PM"
            ),
            Transaction(
                id = "t008",
                name = "Document Signing",
                status = "Failed",
                creationDate = "22 February 2024 9:45 AM"
            ),
            Transaction(
                id = "t009",
                name = "Identity Verification",
                status = "Completed",
                creationDate = "17 February 2024 11:30 AM"
            ),
            Transaction(
                id = "t010",
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
            }.toList().sortedBy { it.first.order }

            val sortOrderUi = when (result.updatedFilters.sortOrder) {
                is SortOrder.Ascending -> DualSelectorButton.FIRST
                is SortOrder.Descending -> DualSelectorButton.SECOND
            }

            when (result) {
                is FilterValidatorPartialState.FilterListResult -> {
                    TransactionInteractorFilterPartialState.FilterApplyResult(
                        transactions = transactionsUi,
                        filters = listOf(),
                        sortOrder = sortOrderUi,
                        allDefaultFiltersAreSelected = result.allDefaultFiltersAreSelected
                    )
                }

                is FilterValidatorPartialState.FilterUpdateResult -> {
                    TransactionInteractorFilterPartialState.FilterUpdateResult(
                        filters = listOf(),
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

                FilterableItem(
                    payload = TransactionUi(
                        uiData = ListItemData(
                            itemId = transaction.id,
                            mainContentData = ListItemMainContentData.Text(text = transaction.name),
                            overlineText = transaction.status,
                            supportingText = transaction.creationDate.toFormattedDisplayableDate(),
                            trailingContentData = ListItemTrailingContentData.Icon(
                                iconData = AppIcons.KeyboardArrowRight
                            )
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
                        )
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

    override fun addDynamicFilters(documents: FilterableList, filters: Filters): Filters {
        return filters.copy(
            filterGroups = filters.filterGroups,
            sortOrder = filters.sortOrder
        )
    }

    override fun getFilters(): Filters = Filters(
        filterGroups = listOf(
            // Filter by Status
            FilterGroup.MultipleSelectionFilterGroup(
                id = FilterIds.FILTER_BY_STATUS_GROUP_ID,
                name = "Filter by Transaction Status",
                filters = listOf(
                    FilterItem(
                        id = FilterIds.FILTER_BY_STATUS_COMPLETE,
                        name = resourceProvider.getString(R.string.transaction_status_completed),
                        selected = true,
                        isDefault = false,
                    ),
                    FilterItem(
                        id = FilterIds.FILTER_BY_STATUS_FAILED,
                        name = resourceProvider.getString(R.string.transaction_status_failed),
                        selected = true,
                        isDefault = false,
                    )
                ),
                filterableAction = FilterMultipleAction<TransactionsFilterableAttributes> { attributes, filter ->
                    when (filter.id) {
                        FilterIds.FILTER_BY_STATUS_COMPLETE -> {
                            attributes.transactionStatus == TransactionUiStatus.Completed
                        }

                        FilterIds.FILTER_BY_STATUS_FAILED -> attributes.transactionStatus == TransactionUiStatus.Failed

                        else -> true
                    }
                }
            )
        ),
        sortOrder = SortOrder.Ascending(isDefault = true)
    )

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
                    shortDateTimeFormatter
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
}