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

package eu.europa.ec.corelogic.model

import androidx.annotation.StringRes
import eu.europa.ec.businesslogic.util.endOfDay
import eu.europa.ec.businesslogic.util.endOfMonth
import eu.europa.ec.businesslogic.util.endOfWeek
import eu.europa.ec.businesslogic.util.monthYearFormatter
import eu.europa.ec.businesslogic.util.startOfDay
import eu.europa.ec.businesslogic.util.startOfMonth
import eu.europa.ec.businesslogic.util.startOfWeek
import eu.europa.ec.resourceslogic.R
import java.time.LocalDateTime

sealed class TransactionCategory(
    @StringRes val stringResId: Int,
    val id: Int,
    val order: Int,
    val dateRange: ClosedRange<LocalDateTime>? = null,
    val displayName: String? = null
) {
    data object Today : TransactionCategory(
        stringResId = R.string.transaction_category_today,
        id = 1,
        order = Int.MAX_VALUE,
        dateRange = LocalDateTime.now().startOfDay()..LocalDateTime.now().endOfDay()
    )

    data object ThisWeek : TransactionCategory(
        stringResId = R.string.transaction_category_this_week,
        id = 2,
        order = Int.MAX_VALUE - 1,
        dateRange = LocalDateTime.now().startOfWeek()..LocalDateTime.now().endOfWeek()
    )

    class Month(dateTime: LocalDateTime) : TransactionCategory(
        stringResId = R.string.transaction_category_month_year,
        id = generateMonthId(dateTime),
        order = calculateMonthOrder(dateTime),
        dateRange = dateTime.startOfMonth()..dateTime.endOfMonth(),
        displayName = monthYearFormatter.format(dateTime).uppercase()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Month) return false

            val thisStart = this.dateRange?.start
            val otherStart = other.dateRange?.start

            return thisStart?.year == otherStart?.year &&
                    thisStart?.monthValue == otherStart?.monthValue
        }

        override fun hashCode(): Int {
            return dateRange?.let {
                it.start.year * 100 + it.start.monthValue
            } ?: 0
        }
    }
}

private fun generateMonthId(dateTime: LocalDateTime): Int =
    dateTime.year * 100 + dateTime.monthValue

private fun calculateMonthOrder(dateTime: LocalDateTime): Int {
    return dateTime.year * 100 + dateTime.monthValue
}