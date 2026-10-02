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

package eu.europa.ec.dashboardfeature.ui.transactions.list.model

import eu.europa.ec.businesslogic.util.localDateToUtcMillis
import eu.europa.ec.uilogic.component.DatePickerDialogType
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.time.LocalDate

class TestFilterDateRangeSelectionUi {

    //region toDatePickerConfig

    // Case 1:
    // 1. There is no selected range and the list has date bounds.
    //
    // Case 1 Expected Result:
    // Each picker selects the corresponding available boundary.
    @Test
    fun `Given Case 1, When toDatePickerConfig is called, Then Case 1 Expected Result is returned`() {
        // Given
        val selection = FilterDateRangeSelectionUi()
        val limits =
            FilterDateRangeSelectionUi(startDate = mockedStartDate, endDate = mockedEndDate)

        // When
        val start = selection.toDatePickerConfig(DatePickerDialogType.SelectStartDate, limits)
        val end = selection.toDatePickerConfig(DatePickerDialogType.SelectEndDate, limits)

        // Then
        assertEquals(mockedStartDate, start.lowerLimit)
        assertEquals(mockedEndDate, start.upperLimit)
        assertEquals(localDateToUtcMillis(mockedStartDate), start.selectedUtcDateMillis)
        assertEquals(mockedStartDate, end.lowerLimit)
        assertEquals(mockedEndDate, end.upperLimit)
        assertEquals(localDateToUtcMillis(mockedEndDate), end.selectedUtcDateMillis)
    }

    // Case 2:
    // 1. Both boundary transactions have been removed while their dates remain selected.
    //
    // Case 2 Expected Result:
    // The saved dates remain selected and selectable in both pickers.
    @Test
    fun `Given Case 2, When toDatePickerConfig is called, Then Case 2 Expected Result is returned`() {
        // Given
        val selection =
            FilterDateRangeSelectionUi(startDate = mockedStartDate, endDate = mockedEndDate)
        val limits = FilterDateRangeSelectionUi(
            startDate = mockedStartDate.plusDays(1),
            endDate = mockedEndDate.minusDays(1),
        )

        // When
        val start = selection.toDatePickerConfig(DatePickerDialogType.SelectStartDate, limits)
        val end = selection.toDatePickerConfig(DatePickerDialogType.SelectEndDate, limits)

        // Then
        assertEquals(mockedStartDate, start.lowerLimit)
        assertEquals(mockedEndDate, start.upperLimit)
        assertEquals(localDateToUtcMillis(mockedStartDate), start.selectedUtcDateMillis)
        assertEquals(mockedStartDate, end.lowerLimit)
        assertEquals(mockedEndDate, end.upperLimit)
        assertEquals(localDateToUtcMillis(mockedEndDate), end.selectedUtcDateMillis)
    }

    // Case 3:
    // 1. The selected range is entirely before or after the surviving transaction dates.
    //
    // Case 3 Expected Result:
    // Picker bounds remain ordered, respect the other endpoint and preserve the selection.
    @Test
    fun `Given Case 3, When toDatePickerConfig is called, Then Case 3 Expected Result is returned`() {
        // Given
        val selection =
            FilterDateRangeSelectionUi(startDate = mockedStartDate, endDate = mockedEndDate)
        val remainingDates = listOf(mockedStartDate.minusMonths(1), mockedEndDate.plusMonths(1))
        remainingDates.forEach { date ->
            val limits = FilterDateRangeSelectionUi(startDate = date, endDate = date)

            // When
            val start = selection.toDatePickerConfig(DatePickerDialogType.SelectStartDate, limits)
            val end = selection.toDatePickerConfig(DatePickerDialogType.SelectEndDate, limits)

            // Then
            assertTrue(start.lowerLimit!! <= start.upperLimit!!)
            assertTrue(end.lowerLimit!! <= end.upperLimit!!)
            assertEquals(mockedEndDate, start.upperLimit)
            assertEquals(mockedStartDate, end.lowerLimit)
            assertEquals(localDateToUtcMillis(mockedStartDate), start.selectedUtcDateMillis)
            assertEquals(localDateToUtcMillis(mockedEndDate), end.selectedUtcDateMillis)
        }
    }

    // Case 4:
    // 1. No transactions remain, but a date range is still applied.
    //
    // Case 4 Expected Result:
    // The range remains editable without inventing new selected dates.
    @Test
    fun `Given Case 4, When toDatePickerConfig is called, Then Case 4 Expected Result is returned`() {
        // Given
        val selection =
            FilterDateRangeSelectionUi(startDate = mockedStartDate, endDate = mockedEndDate)
        val limits = FilterDateRangeSelectionUi()

        // When
        val start = selection.toDatePickerConfig(DatePickerDialogType.SelectStartDate, limits)
        val end = selection.toDatePickerConfig(DatePickerDialogType.SelectEndDate, limits)

        // Then
        assertEquals(null, start.lowerLimit)
        assertEquals(mockedEndDate, start.upperLimit)
        assertEquals(mockedStartDate, end.lowerLimit)
        assertEquals(null, end.upperLimit)
        assertEquals(localDateToUtcMillis(mockedStartDate), start.selectedUtcDateMillis)
        assertEquals(localDateToUtcMillis(mockedEndDate), end.selectedUtcDateMillis)
    }

    // Case 5:
    // 1. There are no stored transactions or selected dates.
    //
    // Case 5 Expected Result:
    // Neither picker invents a selection or bounds.
    @Test
    fun `Given Case 5, When toDatePickerConfig is called, Then Case 5 Expected Result is returned`() {
        // Given
        val selection = FilterDateRangeSelectionUi()
        val limits = FilterDateRangeSelectionUi()

        // When
        DatePickerDialogType.entries.forEach { type ->
            val config = selection.toDatePickerConfig(type, limits)

            // Then
            assertEquals(type, config.type)
            assertEquals(null, config.lowerLimit)
            assertEquals(null, config.upperLimit)
            assertEquals(null, config.selectedUtcDateMillis)
        }
    }

    // Case 6:
    // 1. Only one endpoint is selected and the remaining list lies beyond it.
    //
    // Case 6 Expected Result:
    // The selected endpoint stays valid and the opposite picker spans the remaining dates.
    @Test
    fun `Given Case 6, When toDatePickerConfig is called, Then Case 6 Expected Result is returned`() {
        // Given
        val startOnly = FilterDateRangeSelectionUi(startDate = mockedStartDate, endDate = null)
        val endOnly = FilterDateRangeSelectionUi(startDate = null, endDate = mockedEndDate)

        // When
        val end = startOnly.toDatePickerConfig(
            type = DatePickerDialogType.SelectEndDate,
            availableDates = FilterDateRangeSelectionUi(mockedEndDate, mockedEndDate),
        )
        val start = endOnly.toDatePickerConfig(
            type = DatePickerDialogType.SelectStartDate,
            availableDates = FilterDateRangeSelectionUi(mockedStartDate, mockedStartDate),
        )

        // Then
        assertEquals(mockedStartDate, end.lowerLimit)
        assertEquals(mockedEndDate, end.upperLimit)
        assertEquals(localDateToUtcMillis(mockedEndDate), end.selectedUtcDateMillis)
        assertEquals(mockedStartDate, start.lowerLimit)
        assertEquals(mockedEndDate, start.upperLimit)
        assertEquals(localDateToUtcMillis(mockedStartDate), start.selectedUtcDateMillis)
    }
    //endregion

    //region mocked objects
    private val mockedStartDate = LocalDate.of(2026, 3, 10)
    private val mockedEndDate = LocalDate.of(2026, 3, 20)
    //endregion
}