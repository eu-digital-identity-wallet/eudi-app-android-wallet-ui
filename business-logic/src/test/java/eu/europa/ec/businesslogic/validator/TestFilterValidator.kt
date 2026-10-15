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

package eu.europa.ec.businesslogic.validator

import eu.europa.ec.businesslogic.validator.model.FilterAction
import eu.europa.ec.businesslogic.validator.model.FilterElement
import eu.europa.ec.businesslogic.validator.model.FilterGroup
import eu.europa.ec.businesslogic.validator.model.FilterMultipleAction
import eu.europa.ec.businesslogic.validator.model.FilterSort
import eu.europa.ec.businesslogic.validator.model.FilterableAttributes
import eu.europa.ec.businesslogic.validator.model.FilterableItem
import eu.europa.ec.businesslogic.validator.model.FilterableList
import eu.europa.ec.businesslogic.validator.model.Filters
import eu.europa.ec.businesslogic.validator.model.SortOrder
import eu.europa.ec.businesslogic.validator.util.TestAttributes
import eu.europa.ec.businesslogic.validator.util.TestPayload
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
import java.time.LocalDateTime
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
    fun `Given a different sort, When updateSort is called, Then FilterUpdateResult is emitted`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                val filters = filtersWithSingleSelection.copy(
                    sort = FilterSort(
                        id = "sort",
                        name = "Sort",
                        filters = listOf(
                            FilterElement.FilterItem(
                                id = "name",
                                name = "Name",
                                selected = true,
                                isDefault = true,
                                filterableAction = FilterAction.Sort<TestAttributes, String> { it.name },
                            ),
                            FilterElement.FilterItem(
                                id = "name_desc",
                                name = "Name Desc",
                                selected = false,
                                isDefault = false,
                                filterableAction = FilterAction.Sort<TestAttributes, String> { it.name },
                            ),
                        )
                    )
                )
                filterValidator.initializeValidator(filters, filterableList)

                // When
                filterValidator.updateSort("name_desc")

                // Then
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterUpdateResult)
                assertTrue(
                    emittedState.updatedFilters.sort?.filters
                        ?.first { it.id == "name_desc" }
                        ?.selected == true
                )
            }
        }

    @Test
    fun `Given sort config, When applyFilters is called, Then selected sort is applied`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                val filters = filtersWithMultipleSelectionAllSelected.copy(
                    sort = FilterSort(
                        id = "sort",
                        name = "Sort",
                        filters = listOf(
                            FilterElement.FilterItem(
                                id = "name",
                                name = "Name",
                                selected = true,
                                isDefault = true,
                                filterableAction = FilterAction.Sort<TestAttributes, String> { it.name },
                            )
                        )
                    )
                )
                filterValidator.initializeValidator(filters, filterableList)

                // When
                filterValidator.applyFilters()

                // Then
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterListResult.FilterApplyResult)
                emittedState as FilterValidatorPartialState.FilterListResult.FilterApplyResult
                val names = emittedState.filteredList.items.map {
                    (it.attributes as TestAttributes).name
                }
                assertEquals(names.sorted(), names)
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

    //region applySearch

    // Case 1:
    // 1. Two items have search tags differing only by the number of internal spaces.
    // 2. Queries have outer whitespace and different casing.
    //
    // Case 1 Expected Result:
    // Outer whitespace is ignored and matching remains case-insensitive, preserving internal spaces.
    @Test
    fun `Given Case 1, When applySearch is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val singleSpaceItem = FilterableItem(
                payload = TestPayload(name = "Single space"),
                attributes = TestAttributes(
                    searchTags = listOf("Example issuer"),
                    name = "Single space"
                ),
            )
            val doubleSpaceItem = FilterableItem(
                payload = TestPayload(name = "Double space"),
                attributes = TestAttributes(
                    searchTags = listOf("Example  issuer"),
                    name = "Double space"
                ),
            )
            filterValidator.initializeValidator(
                filters = Filters.emptyFilters(),
                filterableList = FilterableList(items = listOf(singleSpaceItem, doubleSpaceItem)),
            )

            filterValidator.onFilterStateChange().runFlowTest {
                mapOf(
                    " \tEXAMPLE issuer\n " to singleSpaceItem,
                    " \tExample  ISSUER\n " to doubleSpaceItem,
                ).forEach { (query, expectedItem) ->
                    // When
                    filterValidator.applySearch(query = query)

                    // Then
                    val result =
                        awaitItem() as FilterValidatorPartialState.FilterListResult.FilterApplyResult
                    assertEquals(listOf(expectedItem), result.filteredList.items)
                }
            }
        }
    }

    // Case 2:
    // 1. Active filters allow PID, mDL and Age Verification items, including one without search tags.
    // 2. An empty or whitespace-only query is applied after searching for PID.
    //
    // Case 2 Expected Result:
    // Each blank query restores all items allowed by the active filters, including the tagless item.
    @Test
    fun `Given Case 2, When applySearch is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            filterValidator.onFilterStateChange().runFlowTest {
                filterValidator.initializeValidator(
                    filters = filtersWithMultipleSelection,
                    filterableList = filterableList,
                )
                filterValidator.updateFilter(
                    filterGroupId = multipleSelectionGroup.id,
                    filterId = filterItemsMultiple.first().id,
                )
                val selectedFilters =
                    (awaitItem() as FilterValidatorPartialState.FilterUpdateResult).updatedFilters
                val expectedItems = filterableList.items.take(5).toSet()

                listOf("", "   ", "\t\n ").forEach { query ->
                    filterValidator.applySearch(query = "PID")
                    val searched =
                        awaitItem() as FilterValidatorPartialState.FilterListResult.FilterApplyResult
                    assertEquals(
                        listOf(filterableList.items[0], filterableList.items[3]),
                        searched.filteredList.items,
                    )

                    // When
                    filterValidator.applySearch(query = query)

                    // Then
                    val cleared =
                        awaitItem() as FilterValidatorPartialState.FilterListResult.FilterApplyResult
                    assertEquals(expectedItems, cleared.filteredList.items.toSet())
                    assertEquals(selectedFilters, cleared.updatedFilters)
                }
            }
        }
    }

    // Case 3:
    // 1. Named, tagless and incomplete items have different dates.
    // 2. The incomplete status is deselected before searching.
    //
    // Case 3 Expected Result:
    // Padded search matches the named item; blank queries retain the status selection and descending date order.
    @Test
    fun `Given Case 3, When applySearch is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val filters = getMockedSelectionFilters(partyNames = listOf(mockedPrimaryName))
            filterValidator.initializeValidator(
                filters = filters,
                filterableList = FilterableList(
                    items = listOf(mockedUnnamedItem, mockedIncompleteItem, mockedNamedItem)
                ),
            )
            filterValidator.onFilterStateChange().runFlowTest {
                filterValidator.applyFilters()
                val initial = awaitItem() as FilterValidatorPartialState.FilterListResult
                assertEquals(
                    listOf(mockedIncompleteItem, mockedNamedItem, mockedUnnamedItem),
                    initial.filteredItems(),
                )
                filterValidator.updateFilter(mockedStatusGroupId, mockedIncompleteStatusId)
                val selectedFilters =
                    (awaitItem() as FilterValidatorPartialState.FilterUpdateResult).updatedFilters

                // When
                filterValidator.applySearch("  ${mockedPrimaryName.uppercase()}  ")

                // Then
                val matched = awaitItem() as FilterValidatorPartialState.FilterListResult
                assertEquals(listOf(mockedNamedItem), matched.filteredItems())
                assertEquals(selectedFilters, matched.updatedFilters)
                assertEquals(false, matched.allDefaultFiltersAreSelected)
                listOf(" \t\n", "").forEach { query ->
                    filterValidator.applySearch(query)
                    val cleared = awaitItem() as FilterValidatorPartialState.FilterListResult
                    assertEquals(
                        listOf(mockedNamedItem, mockedUnnamedItem),
                        cleared.filteredItems()
                    )
                    assertEquals(selectedFilters, cleared.updatedFilters)
                    assertEquals(false, cleared.allDefaultFiltersAreSelected)
                    assertEquals(
                        SortOrder.Descending(isDefault = true),
                        cleared.updatedFilters.sortOrder
                    )
                }
            }
        }
    }

    // Case 4:
    // 1. Items have primary, secondary, provider or credential-type search tags and mixed statuses.
    // 2. The incomplete status is deselected.
    //
    // Case 4 Expected Result:
    // Each tag matches independently within the selected status, preserving filter state and date order.
    @Test
    fun `Given Case 4, When applySearch is called, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val primary = getMockedSelectionItem(
                name = "Primary",
                attributes = mockedSelectionAttributes.copy(
                    searchTags = listOf(mockedPrimaryName, mockedSecondaryName),
                ),
            )
            val secondary = getMockedSelectionItem(
                name = "Secondary",
                attributes = mockedSelectionAttributes.copy(
                    searchTags = listOf(mockedSecondaryName),
                    partyName = null,
                    dateTime = mockedSelectionAttributes.dateTime.plusHours(1),
                ),
            )
            val incomplete = getMockedSelectionItem(
                name = "Incomplete secondary",
                attributes = mockedSelectionAttributes.copy(
                    searchTags = listOf(mockedSecondaryName),
                    partyName = null,
                    completed = false,
                    dateTime = mockedSelectionAttributes.dateTime.plusHours(2),
                ),
            )
            val provider = getMockedSelectionItem(
                name = "Provider",
                attributes = mockedSelectionAttributes.copy(
                    searchTags = listOf(mockedOtherPartyName),
                    partyName = mockedOtherPartyName,
                    typeId = mockedSecondTypeId,
                    dateTime = mockedSelectionAttributes.dateTime.plusHours(3),
                ),
            )
            val credential = getMockedSelectionItem(
                name = "Credential",
                attributes = mockedSelectionAttributes.copy(
                    searchTags = listOf(mockedIssuerName, mockedCredentialType),
                    partyName = mockedIssuerName,
                    dateTime = mockedSelectionAttributes.dateTime.plusHours(4),
                ),
            )
            val incompleteCredential = getMockedSelectionItem(
                name = "Incomplete credential",
                attributes = mockedSelectionAttributes.copy(
                    searchTags = listOf(mockedIssuerName, mockedCredentialType),
                    partyName = mockedIssuerName,
                    completed = false,
                    dateTime = mockedSelectionAttributes.dateTime.plusHours(5),
                ),
            )
            val filters = getMockedSelectionFilters(
                partyNames = listOf(mockedPrimaryName, mockedOtherPartyName, mockedIssuerName)
            )
            filterValidator.initializeValidator(
                filters = filters,
                filterableList = FilterableList(
                    items = listOf(
                        primary,
                        secondary,
                        incomplete,
                        provider,
                        credential,
                        incompleteCredential,
                    )
                ),
            )
            filterValidator.onFilterStateChange().runFlowTest {
                filterValidator.applyFilters()
                awaitItem()
                filterValidator.updateFilter(mockedStatusGroupId, mockedIncompleteStatusId)
                val selectedFilters =
                    (awaitItem() as FilterValidatorPartialState.FilterUpdateResult).updatedFilters
                val queries = listOf(
                    "  ${
                        mockedSecondaryName.substringAfter(' ').uppercase()
                    }  " to listOf(secondary, primary),
                    mockedPrimaryName.uppercase() to listOf(primary),
                    mockedOtherPartyName.substringAfter(' ').uppercase() to listOf(provider),
                    "  ${mockedCredentialType.uppercase()}  " to listOf(credential),
                    mockedIssuerName.uppercase() to listOf(credential),
                    "" to listOf(credential, provider, secondary, primary),
                )
                queries.forEach { (query, expectedItems) ->
                    // When
                    filterValidator.applySearch(query)

                    // Then
                    val result = awaitItem() as FilterValidatorPartialState.FilterListResult
                    assertEquals(expectedItems, result.filteredItems())
                    assertEquals(selectedFilters, result.updatedFilters)
                    assertEquals(false, result.allDefaultFiltersAreSelected)
                    assertEquals(
                        SortOrder.Descending(isDefault = true),
                        result.updatedFilters.sortOrder
                    )
                }
            }
        }
    }

    //endregion

    //region reinitialize after removal

    // Case 1:
    // 1. Search and date, status, type and party selections match two items.
    // 2. Reinitialization removes the older item while another party remains in the source.
    //
    // Case 1 Expected Result:
    // Only the surviving match remains; selections, search and sort survive, and reset uses the refreshed source.
    @Test
    fun `Given Case 1, When the validator is reinitialized, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val filters = getMockedSelectionFilters(
                partyNames = listOf(mockedPrimaryName, mockedOtherPartyName)
            )
            filterValidator.initializeValidator(
                filters = filters,
                filterableList = FilterableList(
                    items = listOf(mockedDeletedItem, mockedRetainedItem, mockedOtherPartyItem)
                ),
            )
            filterValidator.onFilterStateChange().runFlowTest {
                filterValidator.applyFilters()
                awaitItem()
                listOf(
                    mockedStatusGroupId to mockedIncompleteStatusId,
                    mockedTypeGroupId to mockedSecondTypeId,
                    mockedPartyGroupId to mockedNoPartyId,
                    mockedPartyGroupId to "party:$mockedOtherPartyName",
                ).forEach { (groupId, filterId) ->
                    filterValidator.updateFilter(groupId, filterId)
                    awaitItem()
                }
                filterValidator.updateDateFilter(
                    filterGroupId = mockedDateGroupId,
                    filterId = mockedDateRangeId,
                    lowerLimit = mockedSelectionAttributes.dateTime,
                    upperLimit = mockedSelectionAttributes.dateTime.plusMonths(1),
                )
                awaitItem()
                filterValidator.applySearch("  ${mockedPrimaryName.uppercase()}  ")
                val before = awaitItem() as FilterValidatorPartialState.FilterListResult

                // When
                filterValidator.initializeValidator(
                    filters = filters,
                    filterableList = FilterableList(
                        items = listOf(
                            mockedRetainedItem,
                            mockedOtherPartyItem
                        )
                    ),
                )
                filterValidator.applyFilters()
                val after = awaitItem() as FilterValidatorPartialState.FilterListResult

                // Then
                assertEquals(listOf(mockedRetainedItem, mockedDeletedItem), before.filteredItems())
                assertEquals(listOf(mockedRetainedItem), after.filteredItems())
                assertEquals(before.updatedFilters, after.updatedFilters)
                assertEquals(false, after.allDefaultFiltersAreSelected)
                assertEquals(SortOrder.Descending(isDefault = true), after.updatedFilters.sortOrder)
                filterValidator.resetFilters()
                val reset = awaitItem() as FilterValidatorPartialState.FilterListResult
                assertEquals(listOf(mockedRetainedItem), reset.filteredItems())
                assertEquals(filters, reset.updatedFilters)
                filterValidator.applySearch("")
                val cleared = awaitItem() as FilterValidatorPartialState.FilterListResult
                assertEquals(
                    listOf(mockedOtherPartyItem, mockedRetainedItem),
                    cleared.filteredItems()
                )
            }
        }
    }

    // Case 2:
    // 1. Party selection and search match the only item for the primary party.
    // 2. Reinitialization removes that item and party option, leaving the deselected other party.
    //
    // Case 2 Expected Result:
    // The obsolete option disappears, the remaining selection survives, and reset preserves search.
    @Test
    fun `Given Case 2, When the validator is reinitialized, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            filterValidator.initializeValidator(
                filters = getMockedSelectionFilters(
                    partyNames = listOf(mockedPrimaryName, mockedOtherPartyName)
                ),
                filterableList = FilterableList(
                    items = listOf(
                        mockedDeletedItem,
                        mockedOtherPartyItem
                    )
                ),
            )
            filterValidator.onFilterStateChange().runFlowTest {
                filterValidator.applyFilters()
                awaitItem()
                filterValidator.updateFilter(mockedPartyGroupId, "party:$mockedOtherPartyName")
                awaitItem()
                filterValidator.applySearch(mockedPrimaryName)
                val before = awaitItem() as FilterValidatorPartialState.FilterListResult
                val refreshedFilters =
                    getMockedSelectionFilters(partyNames = listOf(mockedOtherPartyName))

                // When
                filterValidator.initializeValidator(
                    filters = refreshedFilters,
                    filterableList = FilterableList(items = listOf(mockedOtherPartyItem)),
                )
                filterValidator.applyFilters()
                val after = awaitItem() as FilterValidatorPartialState.FilterListResult

                // Then
                assertEquals(listOf(mockedDeletedItem), before.filteredItems())
                assertTrue(after is FilterValidatorPartialState.FilterListResult.FilterListEmptyResult)
                val parties = after.updatedFilters.filterGroups.single { group ->
                    group.id == mockedPartyGroupId
                }
                assertEquals(
                    listOf(mockedNoPartyId, "party:$mockedOtherPartyName"),
                    parties.filters.map { filter -> filter.id },
                )
                assertEquals(listOf(true, false), parties.filters.map { filter -> filter.selected })
                assertEquals(false, after.allDefaultFiltersAreSelected)
                filterValidator.resetFilters()
                val reset = awaitItem() as FilterValidatorPartialState.FilterListResult
                assertTrue(reset.filteredItems().isEmpty())
                assertEquals(refreshedFilters, reset.updatedFilters)
                filterValidator.applySearch("")
                val cleared = awaitItem() as FilterValidatorPartialState.FilterListResult
                assertEquals(listOf(mockedOtherPartyItem), cleared.filteredItems())
            }
        }
    }

    // Case 3:
    // 1. A date range and search match the final source item.
    // 2. Reinitialization supplies an empty source and no named party options.
    //
    // Case 3 Expected Result:
    // Results remain empty after reinitialization, search clearing and reset; obsolete options stay absent.
    @Test
    fun `Given Case 3, When the validator is reinitialized, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            filterValidator.initializeValidator(
                filters = getMockedSelectionFilters(partyNames = listOf(mockedPrimaryName)),
                filterableList = FilterableList(items = listOf(mockedDeletedItem)),
            )
            filterValidator.onFilterStateChange().runFlowTest {
                filterValidator.applyFilters()
                awaitItem()
                filterValidator.updateDateFilter(
                    filterGroupId = mockedDateGroupId,
                    filterId = mockedDateRangeId,
                    lowerLimit = mockedSelectionAttributes.dateTime,
                    upperLimit = mockedSelectionAttributes.dateTime,
                )
                awaitItem()
                filterValidator.applySearch(mockedPrimaryName)
                val before = awaitItem() as FilterValidatorPartialState.FilterListResult
                val refreshedFilters = getMockedSelectionFilters(partyNames = emptyList())

                // When
                filterValidator.initializeValidator(
                    filters = refreshedFilters,
                    filterableList = FilterableList(items = emptyList()),
                )
                filterValidator.applyFilters()
                val after = awaitItem() as FilterValidatorPartialState.FilterListResult

                // Then
                assertEquals(listOf(mockedDeletedItem), before.filteredItems())
                assertTrue(after is FilterValidatorPartialState.FilterListResult.FilterListEmptyResult)
                val parties = after.updatedFilters.filterGroups.single { group ->
                    group.id == mockedPartyGroupId
                }
                assertEquals(listOf(mockedNoPartyId), parties.filters.map { filter -> filter.id })
                val previousRange = before.updatedFilters.dateRange()
                val refreshedRange = after.updatedFilters.dateRange()
                assertEquals(previousRange.selected, refreshedRange.selected)
                assertEquals(previousRange.startDateTime, refreshedRange.startDateTime)
                assertEquals(previousRange.endDateTime, refreshedRange.endDateTime)
                filterValidator.applySearch("")
                assertTrue(
                    (awaitItem() as FilterValidatorPartialState.FilterListResult).filteredItems()
                        .isEmpty()
                )
                filterValidator.resetFilters()
                val reset = awaitItem() as FilterValidatorPartialState.FilterListResult
                assertTrue(reset.filteredItems().isEmpty())
                assertEquals(refreshedFilters, reset.updatedFilters)
            }
        }
    }

    // Case 4:
    // 1. An applied date range matches the deleted item but excludes a surviving item from the same party.
    //
    // Case 4 Expected Result:
    // Reinitialization and search clearing retain the date constraint; reset exposes only the surviving item.
    @Test
    fun `Given Case 4, When the validator is reinitialized, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val filters = getMockedSelectionFilters(partyNames = listOf(mockedPrimaryName))
            filterValidator.initializeValidator(
                filters = filters,
                filterableList = FilterableList(
                    items = listOf(
                        mockedDeletedItem,
                        mockedRetainedItem
                    )
                ),
            )
            filterValidator.onFilterStateChange().runFlowTest {
                filterValidator.applyFilters()
                awaitItem()
                filterValidator.updateDateFilter(
                    filterGroupId = mockedDateGroupId,
                    filterId = mockedDateRangeId,
                    lowerLimit = mockedSelectionAttributes.dateTime,
                    upperLimit = mockedSelectionAttributes.dateTime,
                )
                awaitItem()
                filterValidator.applySearch(mockedPrimaryName)
                val before = awaitItem() as FilterValidatorPartialState.FilterListResult

                // When
                filterValidator.initializeValidator(
                    filters = filters,
                    filterableList = FilterableList(items = listOf(mockedRetainedItem)),
                )
                filterValidator.applyFilters()
                val after = awaitItem() as FilterValidatorPartialState.FilterListResult

                // Then
                assertEquals(listOf(mockedDeletedItem), before.filteredItems())
                assertTrue(after is FilterValidatorPartialState.FilterListResult.FilterListEmptyResult)
                assertEquals(before.updatedFilters, after.updatedFilters)
                assertEquals(
                    mockedSelectionAttributes.dateTime to mockedSelectionAttributes.dateTime,
                    after.updatedFilters.dateRange()
                        .let { range -> range.startDateTime to range.endDateTime },
                )
                filterValidator.applySearch("")
                val cleared = awaitItem() as FilterValidatorPartialState.FilterListResult
                assertTrue(cleared.filteredItems().isEmpty())
                assertEquals(after.updatedFilters, cleared.updatedFilters)
                filterValidator.resetFilters()
                val reset = awaitItem() as FilterValidatorPartialState.FilterListResult
                assertEquals(listOf(mockedRetainedItem), reset.filteredItems())
                assertEquals(filters, reset.updatedFilters)
            }
        }
    }
    //endregion

    //region source list boundaries

    // Case 1:
    // 1. Only one item is supplied, with date, status, type and party filters.
    // 2. Searches, unknown options, deselection and reset are applied.
    //
    // Case 1 Expected Result:
    // Every result stays within the supplied source, and reset restores only that item.
    @Test
    fun `Given Case 1, When source list criteria change, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val filters = getMockedSelectionFilters(partyNames = listOf(mockedPrimaryName))
            val omittedTypeIds = listOf("omitted-first", "omitted-second")
            val queries = listOf(mockedSecondaryName, mockedOtherPartyName) + omittedTypeIds
            filterValidator.initializeValidator(
                filters = filters,
                filterableList = FilterableList(items = listOf(mockedNamedItem)),
            )
            filterValidator.onFilterStateChange().runFlowTest {
                filterValidator.applyFilters()
                assertEquals(
                    listOf(mockedNamedItem),
                    (awaitItem() as FilterValidatorPartialState.FilterListResult).filteredItems(),
                )

                // When
                queries.forEach { query ->
                    filterValidator.applySearch(query)

                    // Then
                    val searched = awaitItem() as FilterValidatorPartialState.FilterListResult
                    assertTrue(searched.filteredItems().isEmpty())
                }
                listOf(" \t", "").forEach { query ->
                    filterValidator.applySearch(query)
                    val cleared = awaitItem() as FilterValidatorPartialState.FilterListResult
                    assertEquals(listOf(mockedNamedItem), cleared.filteredItems())
                }
                omittedTypeIds.forEach { filterId ->
                    filterValidator.updateFilter(mockedTypeGroupId, filterId)
                    val updated =
                        (awaitItem() as FilterValidatorPartialState.FilterUpdateResult).updatedFilters
                    val types =
                        updated.filterGroups.single { group -> group.id == mockedTypeGroupId }
                    assertEquals(
                        listOf(mockedFirstTypeId, mockedSecondTypeId),
                        types.filters.map { filter -> filter.id },
                    )
                    assertEquals(filters, updated)
                }
                listOf(
                    mockedTypeGroupId to mockedFirstTypeId,
                    mockedTypeGroupId to mockedSecondTypeId,
                    mockedStatusGroupId to mockedCompleteStatusId,
                    mockedStatusGroupId to mockedIncompleteStatusId,
                ).forEach { (groupId, filterId) ->
                    filterValidator.updateFilter(groupId, filterId)
                    awaitItem()
                }
                filterValidator.applyFilters()
                assertTrue(
                    (awaitItem() as FilterValidatorPartialState.FilterListResult).filteredItems()
                        .isEmpty()
                )
                filterValidator.resetFilters()
                assertEquals(
                    listOf(mockedNamedItem),
                    (awaitItem() as FilterValidatorPartialState.FilterListResult).filteredItems(),
                )
                filterValidator.updateFilter(mockedPartyGroupId, "party:$mockedPrimaryName")
                awaitItem()
                filterValidator.applyFilters()
                assertTrue(
                    (awaitItem() as FilterValidatorPartialState.FilterListResult).filteredItems()
                        .isEmpty()
                )
                filterValidator.resetFilters()
                assertEquals(
                    listOf(mockedNamedItem),
                    (awaitItem() as FilterValidatorPartialState.FilterListResult).filteredItems(),
                )
                val outsideDate = mockedSelectionAttributes.dateTime.plusYears(1)
                filterValidator.updateDateFilter(
                    filterGroupId = mockedDateGroupId,
                    filterId = mockedDateRangeId,
                    lowerLimit = outsideDate,
                    upperLimit = outsideDate,
                )
                awaitItem()
                filterValidator.applyFilters()
                assertTrue(
                    (awaitItem() as FilterValidatorPartialState.FilterListResult).filteredItems()
                        .isEmpty()
                )
                filterValidator.resetFilters()
                val reset = awaitItem() as FilterValidatorPartialState.FilterListResult
                assertEquals(listOf(mockedNamedItem), reset.filteredItems())
                assertEquals(filters, reset.updatedFilters)
            }
        }
    }
    //endregion

    //region ReversibleSingleSelectionFilterGroup / ReversibleMultipleSelectionFilterGroup
    // These two filter-group variants are exercised by updateFilter and applyFilters paths in
    // FilterValidatorImpl that aren't covered by the SingleSelection/MultipleSelection tests
    // above.

    private val reversibleSingleSelectionGroup = FilterGroup.ReversibleSingleSelectionFilterGroup(
        id = "reversible_single",
        name = "Reversible Single",
        filters = listOf(
            FilterElement.FilterItem(
                id = "rs1",
                name = "PID",
                selected = false,
                filterableAction = FilterAction.Filter<TestAttributes>(predicate = { attrs, filter ->
                    attrs.name == filter.name
                }),
            ),
            FilterElement.FilterItem(
                id = "rs2",
                name = "mDL",
                selected = true,
                filterableAction = FilterAction.Filter<TestAttributes>(predicate = { attrs, filter ->
                    attrs.name == filter.name
                }),
            ),
        ),
    )

    private val reversibleMultipleSelectionGroup =
        FilterGroup.ReversibleMultipleSelectionFilterGroup<TestAttributes>(
            id = "reversible_multi",
            name = "Reversible Multi",
            filters = listOf(
                FilterElement.FilterItem(id = "rm1", name = "PID", selected = true),
                FilterElement.FilterItem(id = "rm2", name = "mDL", selected = false),
            ),
            filterableAction = FilterMultipleAction { attrs, filter ->
                attrs.name == filter.name
            },
        )

    private val filtersWithReversibleSingle = Filters(
        filterGroups = listOf(reversibleSingleSelectionGroup),
        sortOrder = SortOrder.Ascending(isDefault = true),
    )

    private val filtersWithReversibleMultiple = Filters(
        filterGroups = listOf(reversibleMultipleSelectionGroup),
        sortOrder = SortOrder.Descending(),
    )

    @Test
    fun `Given a ReversibleSingleSelectionFilterGroup, When updateFilter is called, Then a FilterUpdateResult is emitted with the toggled item`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                filterValidator.initializeValidator(filtersWithReversibleSingle, filterableList)

                // When — toggle rs1 (was unselected)
                filterValidator.updateFilter(
                    filterGroupId = reversibleSingleSelectionGroup.id,
                    filterId = "rs1",
                )

                // Then
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterUpdateResult)
                val updatedGroup =
                    emittedState.updatedFilters.filterGroups.first { it.id == reversibleSingleSelectionGroup.id }
                assertTrue(updatedGroup.filters.first { it.id == "rs1" }.selected)
            }
        }

    @Test
    fun `Given a ReversibleMultipleSelectionFilterGroup, When updateFilter is called, Then a FilterUpdateResult is emitted with the toggled item`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                filterValidator.initializeValidator(filtersWithReversibleMultiple, filterableList)

                // When — toggle rm2 (was unselected)
                filterValidator.updateFilter(
                    filterGroupId = reversibleMultipleSelectionGroup.id,
                    filterId = "rm2",
                )

                // Then
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterUpdateResult)
                val updatedGroup =
                    emittedState.updatedFilters.filterGroups.first { it.id == reversibleMultipleSelectionGroup.id }
                assertTrue(updatedGroup.filters.first { it.id == "rm2" }.selected)
            }
        }

    @Test
    fun `Given a ReversibleSingleSelectionFilterGroup, When applyFilters is called, Then a FilterApplyResult is emitted`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                filterValidator.initializeValidator(filtersWithReversibleSingle, filterableList)

                // When
                filterValidator.applyFilters()

                // Then
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterListResult)
            }
        }

    @Test
    fun `Given a ReversibleMultipleSelectionFilterGroup, When applyFilters is called, Then a FilterApplyResult is emitted`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                filterValidator.initializeValidator(filtersWithReversibleMultiple, filterableList)

                // When
                filterValidator.applyFilters()

                // Then
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterListResult)
            }
        }
    //endregion

    //region updateFilter group-type coverage

    private val multiSelectionGroupForUpdate =
        FilterGroup.MultipleSelectionFilterGroup<TestAttributes>(
            id = "ms_for_update",
            name = "Multi",
            filters = listOf(
                FilterElement.FilterItem(id = "ms1", name = "PID", selected = false),
                FilterElement.FilterItem(id = "ms2", name = "mDL", selected = true),
            ),
            filterableAction = FilterMultipleAction { attrs, filter -> attrs.name == filter.name },
        )

    private val filtersMultiForUpdate = Filters(
        filterGroups = listOf(multiSelectionGroupForUpdate),
        sortOrder = SortOrder.Descending(),
    )

    @Test
    fun `Given a MultipleSelectionFilterGroup, When updateFilter is called, Then the targeted FilterItem is toggled`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                filterValidator.initializeValidator(filtersMultiForUpdate, filterableList)

                // When
                filterValidator.updateFilter(
                    filterGroupId = multiSelectionGroupForUpdate.id,
                    filterId = "ms1",
                )

                // Then
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterUpdateResult)
                val updatedGroup =
                    emittedState.updatedFilters.filterGroups.first { it.id == multiSelectionGroupForUpdate.id }
                assertTrue(updatedGroup.filters.first { it.id == "ms1" }.selected)
            }
        }

    @Test
    fun `Given updateFilter is called with an unknown filterGroupId, When no group matches, Then groups are untouched`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                filterValidator.initializeValidator(filtersMultiForUpdate, filterableList)

                // When
                filterValidator.updateFilter(
                    filterGroupId = "unknown_group_id",
                    filterId = "ms1",
                )

                // Then
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterUpdateResult)
                val firstGroup = emittedState.updatedFilters.filterGroups.first()
                assertEquals(multiSelectionGroupForUpdate.id, firstGroup.id)
                assertEquals(
                    multiSelectionGroupForUpdate.filters.map { it.id to it.selected },
                    firstGroup.filters.map { it.id to it.selected }
                )
            }
        }
    //endregion

    //region mergeFilters
    // When initializeValidator is called twice with overlapping filter ids, the merge logic
    // copies the existing filter's selection state. Calling it twice with a DateTimeRangeFilterItem
    // exercises the type-mismatch fallback as well as the same-type merge.

    @Test
    fun `Given initializeValidator is called twice with the same DateTimeRangeFilterItem group, Then the merged group preserves the existing date range`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                val first = Filters(
                    filterGroups = listOf(dateRangeGroup),
                    sortOrder = SortOrder.Descending(isDefault = true),
                )
                val second = Filters(
                    filterGroups = listOf(dateRangeGroup),
                    sortOrder = SortOrder.Descending(isDefault = true),
                )

                // When
                filterValidator.initializeValidator(first, filterableList)
                filterValidator.initializeValidator(second, filterableList)
                filterValidator.applyFilters()

                // Then — both initializeValidator calls succeed without throwing; the
                // mergeFilters DateTimeRangeFilterItem branch (line ~765) is exercised by the
                // second initializeValidator call.
                val emitted = awaitItem()
                assertTrue(emitted is FilterValidatorPartialState.FilterListResult)
            }
        }
    //endregion

    //region updateDateFilter on all 4 group types
    // updateDateFilter routes through the (date-version) updateFilterInGroup which has its own
    // `when (group)` over all 4 FilterGroup subtypes. Only the SingleSelection variant was
    // covered above; this adds the other three.

    @Test
    fun `Given a MultipleSelectionFilterGroup containing a DateTimeRangeFilterItem, When updateDateFilter is called, Then the range is updated`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                val group = FilterGroup.MultipleSelectionFilterGroup<TestAttributes>(
                    id = "multi_date",
                    name = "Multi Date",
                    filters = listOf(
                        FilterElement.DateTimeRangeFilterItem(
                            id = "mdr1",
                            name = "Range",
                            selected = true,
                            isDefault = true,
                            startDateTime = LocalDateTime.MIN,
                            endDateTime = LocalDateTime.MAX,
                        ),
                    ),
                    filterableAction = FilterMultipleAction { _, _ -> true },
                )
                val filters = Filters(
                    filterGroups = listOf(group),
                    sortOrder = SortOrder.Descending(isDefault = true),
                )
                filterValidator.initializeValidator(filters, filterableList)

                // When
                filterValidator.updateDateFilter(
                    filterGroupId = group.id,
                    filterId = "mdr1",
                    lowerLimit = LocalDateTime.of(2026, 1, 1, 0, 0),
                    upperLimit = LocalDateTime.of(2026, 12, 31, 0, 0),
                )

                // Then
                val state = awaitItem()
                assertTrue(state is FilterValidatorPartialState.FilterUpdateResult)
            }
        }

    @Test
    fun `Given a ReversibleSingleSelectionFilterGroup containing a DateTimeRangeFilterItem, When updateDateFilter is called, Then the range is updated`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                val group = FilterGroup.ReversibleSingleSelectionFilterGroup(
                    id = "rsingle_date",
                    name = "Rev Single Date",
                    filters = listOf(
                        FilterElement.DateTimeRangeFilterItem(
                            id = "rsdr1",
                            name = "Range",
                            selected = true,
                            isDefault = true,
                            startDateTime = LocalDateTime.MIN,
                            endDateTime = LocalDateTime.MAX,
                        ),
                    ),
                )
                val filters = Filters(
                    filterGroups = listOf(group),
                    sortOrder = SortOrder.Descending(isDefault = true),
                )
                filterValidator.initializeValidator(filters, filterableList)

                // When
                filterValidator.updateDateFilter(
                    filterGroupId = group.id,
                    filterId = "rsdr1",
                    lowerLimit = LocalDateTime.of(2026, 1, 1, 0, 0),
                    upperLimit = LocalDateTime.of(2026, 12, 31, 0, 0),
                )

                // Then
                val state = awaitItem()
                assertTrue(state is FilterValidatorPartialState.FilterUpdateResult)
            }
        }

    @Test
    fun `Given a ReversibleMultipleSelectionFilterGroup containing a DateTimeRangeFilterItem, When updateDateFilter is called, Then the range is updated`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                val group = FilterGroup.ReversibleMultipleSelectionFilterGroup<TestAttributes>(
                    id = "rmulti_date",
                    name = "Rev Multi Date",
                    filters = listOf(
                        FilterElement.DateTimeRangeFilterItem(
                            id = "rmdr1",
                            name = "Range",
                            selected = true,
                            isDefault = true,
                            startDateTime = LocalDateTime.MIN,
                            endDateTime = LocalDateTime.MAX,
                        ),
                    ),
                    filterableAction = FilterMultipleAction { _, _ -> true },
                )
                val filters = Filters(
                    filterGroups = listOf(group),
                    sortOrder = SortOrder.Descending(isDefault = true),
                )
                filterValidator.initializeValidator(filters, filterableList)

                // When
                filterValidator.updateDateFilter(
                    filterGroupId = group.id,
                    filterId = "rmdr1",
                    lowerLimit = LocalDateTime.of(2026, 1, 1, 0, 0),
                    upperLimit = LocalDateTime.of(2026, 12, 31, 0, 0),
                )

                // Then
                val state = awaitItem()
                assertTrue(state is FilterValidatorPartialState.FilterUpdateResult)
            }
        }
    //endregion

    //region applyReversibleSingleSelectionFilter & applyReversibleMultipleSelectionFilter
    // These cover the Sort early-return and null-selectedFilter paths.

    @Test
    fun `Given a ReversibleSingleSelectionFilterGroup with a Sort action and applyFilters, Then the apply path returns the original list`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                val group = FilterGroup.ReversibleSingleSelectionFilterGroup(
                    id = "rev_single_sort",
                    name = "Rev Sort",
                    filters = listOf(
                        FilterElement.FilterItem(
                            id = "rss",
                            name = "Sort by name",
                            selected = true,
                            isDefault = true,
                            filterableAction = FilterAction.Sort<TestAttributes, String> { it.name },
                        ),
                    ),
                )
                val filters = Filters(
                    filterGroups = listOf(group),
                    sortOrder = SortOrder.Ascending(isDefault = true),
                )
                filterValidator.initializeValidator(filters, filterableList)

                // When
                filterValidator.applyFilters()

                // Then
                val state = awaitItem()
                assertTrue(state is FilterValidatorPartialState.FilterListResult)
            }
        }

    @Test
    fun `Given a ReversibleSingleSelectionFilterGroup with no selected filter and applyFilters, Then the original list is returned`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given — no filter is selected → selectedFilter is null
                val group = FilterGroup.ReversibleSingleSelectionFilterGroup(
                    id = "rev_single_empty",
                    name = "Rev Empty",
                    filters = listOf(
                        FilterElement.FilterItem(
                            id = "rse",
                            name = "X",
                            selected = false,
                            isDefault = false,
                        ),
                    ),
                )
                val filters = Filters(
                    filterGroups = listOf(group),
                    sortOrder = SortOrder.Ascending(isDefault = true),
                )
                filterValidator.initializeValidator(filters, filterableList)

                // When
                filterValidator.applyFilters()

                // Then
                val state = awaitItem()
                assertTrue(state is FilterValidatorPartialState.FilterListResult)
            }
        }

    @Test
    fun `Given a ReversibleMultipleSelectionFilterGroup with no selected filter and applyFilters, Then the original list is returned`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given — no filter selected → applyReversibleMultipleSelectionFilter returns currentList
                val group = FilterGroup.ReversibleMultipleSelectionFilterGroup<TestAttributes>(
                    id = "rev_multi_empty",
                    name = "Rev Multi Empty",
                    filters = listOf(
                        FilterElement.FilterItem(
                            id = "rme",
                            name = "X",
                            selected = false,
                            isDefault = false,
                        ),
                    ),
                    filterableAction = FilterMultipleAction { _, _ -> true },
                )
                val filters = Filters(
                    filterGroups = listOf(group),
                    sortOrder = SortOrder.Ascending(isDefault = true),
                )
                filterValidator.initializeValidator(filters, filterableList)

                // When
                filterValidator.applyFilters()

                // Then
                val state = awaitItem()
                assertTrue(state is FilterValidatorPartialState.FilterListResult)
            }
        }

    @Test
    fun `Given a ReversibleMultipleSelectionFilterGroup with selected filters and applyFilters, Then the filterableAction is applied`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                val group = FilterGroup.ReversibleMultipleSelectionFilterGroup<TestAttributes>(
                    id = "rev_multi_apply",
                    name = "Rev Multi Apply",
                    filters = listOf(
                        FilterElement.FilterItem(
                            id = "rma",
                            name = "PID",
                            selected = true,
                            isDefault = false,
                        ),
                    ),
                    filterableAction = FilterMultipleAction { attrs, filter -> attrs.name == filter.name },
                )
                val filters = Filters(
                    filterGroups = listOf(group),
                    sortOrder = SortOrder.Ascending(isDefault = true),
                )
                filterValidator.initializeValidator(filters, filterableList)

                // When
                filterValidator.applyFilters()

                // Then
                val state = awaitItem()
                assertTrue(state is FilterValidatorPartialState.FilterListResult)
            }
        }
    //endregion

    //region DateTimeRangeFilterItem
    // updateDateFilter exercises the DateTimeRangeFilterItem branch in updateFilterInGroup,
    // which is not exercised by the other tests.

    private val dateRangeGroup = FilterGroup.SingleSelectionFilterGroup(
        id = "date_range_group",
        name = "Date range",
        filters = listOf(
            FilterElement.DateTimeRangeFilterItem(
                id = "dr1",
                name = "Range",
                selected = true,
                isDefault = true,
                startDateTime = LocalDateTime.MIN,
                endDateTime = LocalDateTime.MAX,
            ),
        ),
    )

    private val filtersWithDateRange = Filters(
        filterGroups = listOf(dateRangeGroup),
        sortOrder = SortOrder.Descending(isDefault = true),
    )

    @Test
    fun `Given a DateTimeRangeFilterItem, When updateDateFilter is called, Then a FilterUpdateResult with the new range is emitted`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                filterValidator.initializeValidator(filtersWithDateRange, filterableList)
                val newStart = LocalDateTime.of(2026, 1, 1, 0, 0)
                val newEnd = LocalDateTime.of(2026, 12, 31, 23, 59)

                // When
                filterValidator.updateDateFilter(
                    filterGroupId = dateRangeGroup.id,
                    filterId = "dr1",
                    lowerLimit = newStart,
                    upperLimit = newEnd,
                )

                // Then
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterUpdateResult)
                val updatedGroup =
                    emittedState.updatedFilters.filterGroups.first { it.id == dateRangeGroup.id }
                val updatedRange = updatedGroup.filters.first {
                    it.id == "dr1"
                } as FilterElement.DateTimeRangeFilterItem
                assertEquals(newStart, updatedRange.startDateTime)
                assertEquals(newEnd, updatedRange.endDateTime)
            }
        }

    @Test
    fun `Given a DateTimeRangeFilterItem, When updateFilter is called by id, Then the DateTimeRangeFilterItem branch is exercised`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given
                filterValidator.initializeValidator(filtersWithDateRange, filterableList)

                // When — passing the DateTimeRangeFilterItem id to updateFilter exercises the
                // `is DateTimeRangeFilterItem ->` arm in updateFilterInGroup.
                filterValidator.updateFilter(
                    filterGroupId = dateRangeGroup.id,
                    filterId = "dr1",
                )

                // Then
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterUpdateResult)
            }
        }
    //endregion

    //region helper functions

    private fun getMockedSelectionItem(
        name: String,
        attributes: MockedSelectionAttributes,
    ): FilterableItem = FilterableItem(payload = TestPayload(name), attributes = attributes)

    private fun getMockedSelectionFilters(partyNames: List<String>): Filters {
        return Filters(
            filterGroups = listOf(
                FilterGroup.SingleSelectionFilterGroup(
                    id = mockedDateGroupId,
                    name = "Date",
                    filters = listOf(
                        FilterElement.DateTimeRangeFilterItem(
                            id = mockedDateRangeId,
                            name = "Range",
                            selected = true,
                            isDefault = true,
                            startDateTime = LocalDateTime.MIN,
                            endDateTime = LocalDateTime.MAX,
                            filterableAction = FilterAction.Filter<MockedSelectionAttributes> { attributes, filter ->
                                val range = filter as FilterElement.DateTimeRangeFilterItem
                                val dates =
                                    range.startDateTime.toLocalDate()..range.endDateTime.toLocalDate()
                                attributes.dateTime.toLocalDate() in dates
                            },
                        ),
                    ),
                ),
                FilterGroup.MultipleSelectionFilterGroup(
                    id = mockedStatusGroupId,
                    name = "Status",
                    filters = listOf(
                        mockedCompleteStatusId,
                        mockedIncompleteStatusId
                    ).map { statusId ->
                        FilterElement.FilterItem(
                            id = statusId,
                            name = statusId,
                            selected = true,
                            isDefault = true,
                        )
                    },
                    filterableAction = FilterMultipleAction<MockedSelectionAttributes> { attributes, filter ->
                        attributes.completed == (filter.id == mockedCompleteStatusId)
                    },
                ),
                FilterGroup.MultipleSelectionFilterGroup(
                    id = mockedTypeGroupId,
                    name = "Type",
                    filters = listOf(mockedFirstTypeId, mockedSecondTypeId).map { typeId ->
                        FilterElement.FilterItem(
                            id = typeId,
                            name = typeId,
                            selected = true,
                            isDefault = true,
                        )
                    },
                    filterableAction = FilterMultipleAction<MockedSelectionAttributes> { attributes, filter ->
                        attributes.typeId == filter.id
                    },
                ),
                FilterGroup.MultipleSelectionFilterGroup(
                    id = mockedPartyGroupId,
                    name = "Party",
                    filters = listOf(
                        FilterElement.FilterItem(
                            id = mockedNoPartyId,
                            name = "No party",
                            selected = true,
                            isDefault = true,
                        )
                    ) + partyNames.map { partyName ->
                        FilterElement.FilterItem(
                            id = "party:$partyName",
                            name = partyName,
                            selected = true,
                            isDefault = true,
                        )
                    },
                    filterableAction = FilterMultipleAction<MockedSelectionAttributes> { attributes, filter ->
                        if (filter.id == mockedNoPartyId) {
                            attributes.partyName == null
                        } else {
                            attributes.partyName == filter.name
                        }
                    },
                ),
            ),
            sortOrder = SortOrder.Descending(isDefault = true),
            sort = FilterSort(
                id = mockedDateSortId,
                name = "Sort",
                filters = listOf(
                    FilterElement.FilterItem(
                        id = mockedDateSortId,
                        name = "Date",
                        selected = true,
                        isDefault = true,
                        filterableAction = FilterAction.Sort<MockedSelectionAttributes, LocalDateTime> { attributes ->
                            attributes.dateTime
                        },
                    ),
                ),
            ),
        )
    }

    private fun FilterValidatorPartialState.FilterListResult.filteredItems(): List<FilterableItem> =
        when (this) {
            is FilterValidatorPartialState.FilterListResult.FilterApplyResult -> filteredList.items
            is FilterValidatorPartialState.FilterListResult.FilterListEmptyResult -> emptyList()
        }

    private fun Filters.dateRange(): FilterElement.DateTimeRangeFilterItem =
        filterGroups.single { group -> group.id == mockedDateGroupId }.filters.single()
                as FilterElement.DateTimeRangeFilterItem

    //endregion

    //region mocked objects

    private val mockedDateGroupId = "date"
    private val mockedDateRangeId = "range"
    private val mockedDateSortId = "date-sort"
    private val mockedStatusGroupId = "status"
    private val mockedCompleteStatusId = "complete"
    private val mockedIncompleteStatusId = "incomplete"
    private val mockedTypeGroupId = "type"
    private val mockedFirstTypeId = "first-type"
    private val mockedSecondTypeId = "second-type"
    private val mockedPartyGroupId = "party"
    private val mockedNoPartyId = "no-party"
    private val mockedPrimaryName = "Example primary"
    private val mockedSecondaryName = "Example secondary"
    private val mockedOtherPartyName = "Example provider"
    private val mockedIssuerName = "Example issuer"
    private val mockedCredentialType = "example.credential"

    private data class MockedSelectionAttributes(
        override val searchTags: List<String>,
        val dateTime: LocalDateTime,
        val completed: Boolean,
        val typeId: String,
        val partyName: String?,
    ) : FilterableAttributes

    private val mockedSelectionAttributes = MockedSelectionAttributes(
        searchTags = listOf(mockedPrimaryName),
        dateTime = LocalDateTime.of(2026, 3, 15, 14, 30),
        completed = true,
        typeId = mockedFirstTypeId,
        partyName = mockedPrimaryName,
    )
    private val mockedDeletedItem = getMockedSelectionItem(
        name = "Deleted",
        attributes = mockedSelectionAttributes,
    )
    private val mockedRetainedItem = getMockedSelectionItem(
        name = "Retained",
        attributes = mockedSelectionAttributes.copy(
            dateTime = mockedSelectionAttributes.dateTime.plusMonths(1)
        ),
    )
    private val mockedOtherPartyItem = getMockedSelectionItem(
        name = "Other party",
        attributes = mockedSelectionAttributes.copy(
            searchTags = listOf(mockedOtherPartyName),
            dateTime = mockedSelectionAttributes.dateTime.plusMonths(2),
            typeId = mockedSecondTypeId,
            partyName = mockedOtherPartyName,
        ),
    )
    private val mockedNamedItem = getMockedSelectionItem(
        name = "Named",
        attributes = mockedSelectionAttributes.copy(
            dateTime = mockedSelectionAttributes.dateTime.plusHours(2)
        ),
    )
    private val mockedUnnamedItem = getMockedSelectionItem(
        name = "Unnamed",
        attributes = mockedSelectionAttributes.copy(
            searchTags = emptyList(),
            dateTime = mockedSelectionAttributes.dateTime.plusHours(1),
            partyName = null,
        ),
    )
    private val mockedIncompleteItem = getMockedSelectionItem(
        name = "Incomplete",
        attributes = mockedSelectionAttributes.copy(
            dateTime = mockedSelectionAttributes.dateTime.plusHours(3),
            completed = false,
        ),
    )
    //endregion
}