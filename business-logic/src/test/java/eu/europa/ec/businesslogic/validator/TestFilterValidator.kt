/*
 * Copyright (c) 2025 European Commission
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

import eu.europa.ec.businesslogic.validator.FilterValidator
import eu.europa.ec.businesslogic.validator.FilterValidatorImpl
import eu.europa.ec.businesslogic.validator.FilterValidatorPartialState
import eu.europa.ec.businesslogic.validator.model.FilterableList
import eu.europa.ec.businesslogic.validator.model.SortOrder
import eu.europa.ec.businesslogic.validator.util.filterItemsMultiple
import eu.europa.ec.businesslogic.validator.util.filterItemsSingle
import eu.europa.ec.businesslogic.validator.util.filterableList
import eu.europa.ec.businesslogic.validator.util.filtersWithMultipleSelection
import eu.europa.ec.businesslogic.validator.util.filtersWithMultipleSelectionAllSelected
import eu.europa.ec.businesslogic.validator.util.filtersWithMultipleSelectionNoSelection
import eu.europa.ec.businesslogic.validator.util.filtersWithMultipleSelectionSize3
import eu.europa.ec.businesslogic.validator.util.filtersWithMultipleSelectionSize4
import eu.europa.ec.businesslogic.validator.util.filtersWithSingleSelection
import eu.europa.ec.businesslogic.validator.util.multipleSelectionGroup
import eu.europa.ec.businesslogic.validator.util.singleSelectionGroup
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class TestFilterValidator {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private val testDispatcher = StandardTestDispatcher(coroutineRule.testScope.testScheduler)
    private val testScope = CoroutineScope(testDispatcher)

    private val filterValidator: FilterValidator = FilterValidatorImpl(
        scope = testScope,
        sharingStarted = SharingStarted.Eagerly
    )

    @Test
    fun `Given initial filters, When initializeValidator is called with applyFilters, Then FilterUpdateResult is emitted`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                val expectedFilters = filtersWithSingleSelection

                // When
                filterValidator.initializeValidator(expectedFilters, filterableList)
                filterValidator.applyFilters()

                // Then
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterListResult.FilterApplyResult)
                assertEquals(emittedState.updatedFilters, expectedFilters)
            }
        }

    @Test
    fun `Given initial filters, When initializeValidator is called twice with same appliedFilters, Then FilterUpdateResult is emitted with merged correct`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                val expectedFilters = filtersWithSingleSelection

                // When
                filterValidator.initializeValidator(expectedFilters, filterableList)
                filterValidator.initializeValidator(expectedFilters, filterableList)
                filterValidator.applyFilters()

                // Then
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterListResult.FilterApplyResult)
                assertEquals(emittedState.updatedFilters, expectedFilters)
            }
        }

    @Test
    fun `Given initial filters, When initializeValidator is called twice with different applyFilters, Then FilterUpdateResult is emitted with merged correct`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                val initialFilters = filtersWithMultipleSelectionSize3
                val updatedFilters = filtersWithMultipleSelectionSize4

                // When
                filterValidator.initializeValidator(initialFilters, filterableList)
                filterValidator.initializeValidator(updatedFilters, filterableList)
                filterValidator.applyFilters()

                // Then
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterListResult.FilterApplyResult)
                assertEquals(
                    emittedState.updatedFilters.filterGroups.first().filters.size,
                    updatedFilters.filterGroups.first().filters.size
                )
                assertEquals(
                    emittedState.updatedFilters.filterGroups.first().filters.first().selected,
                    initialFilters.filterGroups.first().filters.first().selected
                )
            }
        }

    @Test
    fun `Given a selected filter to initialize, When updateList is called with applyFilters, Then FilterUpdateResult is emitted with correct result`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                filterValidator.initializeValidator(filtersWithSingleSelection, filterableList)

                // When
                filterValidator.updateLists(FilterableList(emptyList()))
                filterValidator.applyFilters()

                // Then
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterListResult.FilterListEmptyResult)
                assertEquals(emittedState.updatedFilters, filtersWithSingleSelection)
            }
        }

    @Test
    fun `Given a selected filter, When updateFilters is called, Then FilterUpdateResult is emitted with correct result`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                filterValidator.initializeValidator(filtersWithSingleSelection, filterableList)

                // When
                filterValidator.updateFilter(singleSelectionGroup.id, filterItemsSingle[2].id)

                // Then
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterUpdateResult)
                val updatedFilters = emittedState.updatedFilters.filterGroups.first()
                assertTrue(updatedFilters.filters.first { it.id == filterItemsSingle[2].id }.selected)
            }
        }

    @Test
    fun `Given filters with zero selections, When applyFilters is called, Then FilterApplyResult is emitted`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                filterValidator.initializeValidator(
                    filtersWithMultipleSelectionNoSelection,
                    filterableList
                )

                // When
                filterValidator.applyFilters()

                // Then
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterListResult.FilterListEmptyResult)
            }
        }

    @Test
    fun `Given modified filters, When resetFilters is called, Then filters reset to default state`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                filterValidator.initializeValidator(filtersWithMultipleSelection, filterableList)
                filterValidator.updateFilter(multipleSelectionGroup.id, filterItemsMultiple[2].id)

                // When

                val emittedStates = mutableListOf<FilterValidatorPartialState>()
                var latestState: FilterValidatorPartialState? = null
                val times = 3

                // Then
                repeat(times) { iteration ->
                    val emittedState = awaitItem()
                    emittedStates.add(emittedState)
                    latestState = emittedState
                    when (iteration) {
                        0 -> {
                            filterValidator.applyFilters()
                        }

                        1 -> {
                            filterValidator.resetFilters()
                        }
                    }
                }
                // Confirm we get emission from latest resetFilter execution
                assertEquals(times, emittedStates.size)
                assertTrue(latestState is FilterValidatorPartialState.FilterListResult.FilterApplyResult)
                assertEquals(latestState?.updatedFilters, filtersWithMultipleSelection)
            }
        }

    @Test
    fun `Given modified snapshot filters, When revertFilter is called, Then filters reset to previous state`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                filterValidator.initializeValidator(filtersWithMultipleSelection, filterableList)
                filterValidator.updateFilter(multipleSelectionGroup.id, filterItemsMultiple[2].id)

                // When

                val emittedStates = mutableListOf<FilterValidatorPartialState>()
                var latestState: FilterValidatorPartialState? = null
                val times = 2

                // Then
                repeat(times) {
                    val emittedState = awaitItem()
                    emittedStates.add(emittedState)
                    latestState = emittedState
                    filterValidator.revertFilters()
                }
                // Confirm we get emission from latest resetFilter execution
                assertEquals(times, emittedStates.size)
                assertTrue(latestState is FilterValidatorPartialState.FilterUpdateResult)
                assertEquals(
                    latestState?.updatedFilters?.filterGroups,
                    filtersWithMultipleSelection.filterGroups
                )
            }
        }

    @Test
    fun `Given a different sort order, When updateSortOrder is called, Then FilterUpdateResult is emitted`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                filterValidator.initializeValidator(filtersWithSingleSelection, filterableList)

                // When
                filterValidator.updateSortOrder(SortOrder.Descending())

                // Then
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterUpdateResult)
                assertTrue(emittedState.updatedFilters.sortOrder == SortOrder.Descending())
            }
        }

    @Test
    fun `Given a modified filter, When applySearch is called, Then FilterUpdateResult is emitted respecting both filter and search`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                filterValidator.initializeValidator(filtersWithMultipleSelection, filterableList)
                filterValidator.updateFilter(multipleSelectionGroup.id, filterItemsMultiple[3].id)
                filterValidator.updateFilter(multipleSelectionGroup.id, filterItemsMultiple[4].id)

                // When
                val emittedStates = mutableListOf<FilterValidatorPartialState>()
                var latestState: FilterValidatorPartialState? = null
                val times = 4

                // Then
                repeat(times) { iteration ->
                    val emittedState = awaitItem()
                    emittedStates.add(emittedState)
                    latestState = emittedState
                    when (iteration) {
                        0, 1 -> {
                            filterValidator.applyFilters()
                        }

                        2 -> {
                            filterValidator.applySearch("search text")
                        }
                    }
                }

                // Then
                assertEquals(times, emittedStates.size)
                assertTrue(latestState is FilterValidatorPartialState.FilterListResult.FilterApplyResult)
                assertEquals(
                    (latestState as FilterValidatorPartialState.FilterListResult.FilterApplyResult).filteredList.items.size,
                    1
                )
            }
        }

    @Test
    fun `Given a search query, When applySearch is called, Then FilterApplyResult is emitted`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                filterValidator.initializeValidator(
                    filtersWithMultipleSelectionAllSelected,
                    filterableList
                )

                // When
                filterValidator.applySearch("secondary text")

                // Then
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterListResult.FilterApplyResult)
                emittedState as FilterValidatorPartialState.FilterListResult.FilterApplyResult
                assertEquals(emittedState.filteredList.items.size, 1)
            }
        }

    @Test
    fun `Given an invalid search query, When applySearch is called, Then FilterListEmptyResult is emitted`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                filterValidator.initializeValidator(
                    filtersWithMultipleSelectionAllSelected,
                    filterableList
                )

                // When
                filterValidator.applySearch("invalid_search")

                // Then
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterListResult.FilterListEmptyResult)
            }
        }
}

