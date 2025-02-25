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
import eu.europa.ec.businesslogic.validator.model.FilterableItem
import eu.europa.ec.businesslogic.validator.model.FilterableList
import eu.europa.ec.corelogic.model.TransactionCategory
import eu.europa.ec.dashboardfeature.model.Transaction
import eu.europa.ec.dashboardfeature.model.TransactionUi
import eu.europa.ec.dashboardfeature.model.TransactionsFilterableAttributes
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.resourceslogic.theme.values.ThemeColors
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemOverlineTextData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime

sealed class TransactionInteractorGetTransactionsPartialState {
    data class Success(
        val allTransactions: FilterableList,
        val shouldAllowUserInteraction: Boolean,
    ) : TransactionInteractorGetTransactionsPartialState()

    data class Failure(val error: String) : TransactionInteractorGetTransactionsPartialState()
}

sealed class TransactionInteractorDateCategoryPartialState {
    data class WithinLastHour(val minutes: Long) : TransactionInteractorDateCategoryPartialState()
    data class Today(val time: String) : TransactionInteractorDateCategoryPartialState()
    data class WithinMonth(val date: String) : TransactionInteractorDateCategoryPartialState()
}

interface TransactionsInteractor {
    fun getTransactions(): Flow<TransactionInteractorGetTransactionsPartialState>

    fun getTestTransactions(): List<Transaction>

    fun getTransactionCategory(dateTime: LocalDateTime): TransactionCategory
}

class TransactionsInteractorImpl(
    private val resourceProvider: ResourceProvider
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
                            overlineTextData = ListItemOverlineTextData(
                                transaction.status,
                                ThemeColors.success.takeIf {
                                    transaction.status.equals(
                                        resourceProvider.getString(R.string.transaction_status_completed),
                                        ignoreCase = true
                                    )
                                } ?: ThemeColors.error
                            ),
                            supportingText = transaction.creationDate.toFormattedDisplayableDate(),
                            trailingContentData = ListItemTrailingContentData.Icon(
                                iconData = AppIcons.KeyboardArrowRight
                            )
                        ),
                        transactionCategory = getTransactionCategory(dateTime)
                    ),
                    attributes = TransactionsFilterableAttributes(
                        searchTags = listOf(transaction.name),
                        name = transaction.name
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
            else -> TransactionCategory.MonthCategory(dateTime)
        }
        return transactionCategory
    }

    private fun LocalDateTime.toDateTimeState(): TransactionInteractorDateCategoryPartialState =
        when {
            isWithinLastHour() -> TransactionInteractorDateCategoryPartialState.WithinLastHour(
                minutes = minutesToNow()
            )

            isToday() -> TransactionInteractorDateCategoryPartialState.Today(
                time = format(
                    hoursMinutesFormatter
                )
            )

            else -> TransactionInteractorDateCategoryPartialState.WithinMonth(
                date = format(
                    shortDateTimeFormatter
                )
            )
        }

    private fun String.toFormattedDisplayableDate(): String {
        return runCatching {
            val parsedDate = LocalDateTime.parse(this, fullDateTimeFormatter)

            when (val dateTimeState = parsedDate.toDateTimeState()) {
                is TransactionInteractorDateCategoryPartialState.WithinLastHour -> resourceProvider.getString(
                    R.string.transactions_screen_minutes_ago_message,
                    dateTimeState.minutes
                )

                is TransactionInteractorDateCategoryPartialState.Today -> dateTimeState.time
                is TransactionInteractorDateCategoryPartialState.WithinMonth -> dateTimeState.date
            }
        }.getOrDefault(this)
    }
}