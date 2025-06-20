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

package eu.europa.ec.businesslogic.validator

import eu.europa.ec.businesslogic.extension.applySort
import eu.europa.ec.businesslogic.extension.filterByQuery
import eu.europa.ec.businesslogic.validator.model.FilterAction
import eu.europa.ec.businesslogic.validator.model.FilterElement
import eu.europa.ec.businesslogic.validator.model.FilterElement.FilterItem
import eu.europa.ec.businesslogic.validator.model.FilterGroup
import eu.europa.ec.businesslogic.validator.model.FilterableList
import eu.europa.ec.businesslogic.validator.model.Filters
import eu.europa.ec.businesslogic.validator.model.SortOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

sealed interface FilterValidatorPartialState {
    val updatedFilters: Filters

    sealed interface FilterListResult : FilterValidatorPartialState {
        val allDefaultFiltersAreSelected: Boolean

        data class FilterListEmptyResult(
            override val updatedFilters: Filters,
            override val allDefaultFiltersAreSelected: Boolean,
        ) : FilterListResult

        data class FilterApplyResult(
            val filteredList: FilterableList,
            override val allDefaultFiltersAreSelected: Boolean,
            override val updatedFilters: Filters,
        ) : FilterListResult
    }

    data class FilterUpdateResult(
        override val updatedFilters: Filters,
    ) : FilterValidatorPartialState
}

interface FilterValidator {
    fun onFilterStateChange(): Flow<FilterValidatorPartialState>
    fun initializeValidator(
        filters: Filters,
        filterableList: FilterableList,
    )

    fun updateLists(filterableList: FilterableList)
    fun applyFilters()
    fun applySearch(query: String)
    fun resetFilters()
    fun revertFilters()
    fun updateFilter(filterGroupId: String, filterId: String)
    fun updateDateFilter(
        filterGroupId: String,
        filterId: String,
        lowerLimit: LocalDateTime,
        upperLimit: LocalDateTime
    )

    fun updateSortOrder(sortOrder: SortOrder)
}

class FilterValidatorImpl(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(5000L),
) : FilterValidator {

    // Filters
    private var appliedFilters: Filters = Filters.emptyFilters()
    private var snapshotFilters: Filters = Filters.emptyFilters()
    private var initialFilters: Filters = Filters.emptyFilters()

    // Search
    private var searchQuery: String = ""

    // Collection
    private lateinit var initialList: FilterableList

    // Flow
    private val emissionMutableFlow = MutableSharedFlow<FilterValidatorPartialState>()
    private val filterResultFlow: SharedFlow<FilterValidatorPartialState> = emissionMutableFlow
        .debounce(200L)
        .shareIn(
            scope,
            sharingStarted,
            replay = 1
        )

    override fun initializeValidator(
        filters: Filters,
        filterableList: FilterableList,
    ) {
        this.initialFilters = filters
        val mergedFilterGroups = mutableListOf<FilterGroup>()
        filters.filterGroups.forEach { newFilterGroup ->
            // Find the corresponding group in appliedFilters
            val existingFilterGroup =
                appliedFilters.filterGroups.find { it.id == newFilterGroup.id }

            if (existingFilterGroup != null) {
                val mergedFilters = mergeFilters(newFilterGroup, existingFilterGroup)
                mergedFilterGroups.add(mergedFilters)
            } else {
                mergedFilterGroups.add(newFilterGroup)
            }
        }

        appliedFilters = filters.copy(
            sortOrder = if (appliedFilters.isEmpty) filters.sortOrder else appliedFilters.sortOrder,
            filterGroups = mergedFilterGroups
        )
        this.initialList = filterableList
    }

    override fun updateLists(filterableList: FilterableList) {
        this.initialList = filterableList
    }

    override fun onFilterStateChange(): Flow<FilterValidatorPartialState> = filterResultFlow

    private fun updateFilterInGroup(group: FilterGroup, filterId: String): FilterGroup {
        return when (group) {
            is FilterGroup.MultipleSelectionFilterGroup<*> -> {
                group.copy(
                    filters = group.filters.map { filter ->
                        if (filter.id == filterId && filter is FilterItem) {
                            filter.copy(selected = !filter.selected)
                        } else {
                            filter
                        }
                    }
                )
            }

            is FilterGroup.SingleSelectionFilterGroup -> {
                group.copy(
                    filters = group.filters.map { filter ->
                        when (filter) {
                            is FilterItem -> {
                                filter.copy(selected = filter.id == filterId)
                            }

                            is FilterElement.DateTimeRangeFilterItem -> {
                                filter.copy(selected = filter.id == filterId)
                            }
                        }
                    }
                )
            }

            is FilterGroup.ReversibleSingleSelectionFilterGroup -> {
                group.copy(
                    filters = toggleFilterSelection(group.filters, filterId)
                )
            }

            is FilterGroup.ReversibleMultipleSelectionFilterGroup<*> -> {
                group.copy(
                    filters = toggleFilterSelection(group.filters, filterId)
                )
            }
        }
    }

    private fun updateFilterInGroup(
        group: FilterGroup,
        filterId: String,
        lowerLimit: LocalDateTime,
        upperLimit: LocalDateTime,
    ): FilterGroup {
        return when (group) {
            is FilterGroup.MultipleSelectionFilterGroup<*> -> {
                group.copy(
                    filters = toggleFilterSelection(group.filters, filterId)
                )
            }

            is FilterGroup.SingleSelectionFilterGroup -> {
                group.copy(
                    filters = group.filters.map { filter ->
                        when (filter) {
                            is FilterItem -> {
                                filter.copy(selected = filter.id == filterId)
                            }

                            is FilterElement.DateTimeRangeFilterItem -> {
                                filter.copy(startDateTime = lowerLimit, endDateTime = upperLimit)
                            }
                        }
                    }
                )
            }

            is FilterGroup.ReversibleSingleSelectionFilterGroup -> {
                group.copy(
                    filters = toggleFilterSelection(group.filters, filterId)
                )
            }

            is FilterGroup.ReversibleMultipleSelectionFilterGroup<*> -> {
                group.copy(
                    filters = toggleFilterSelection(group.filters, filterId)
                )
            }
        }
    }

    override fun updateFilter(filterGroupId: String, filterId: String) {
        scope.launch {
            val filtersToUpdate = if (snapshotFilters.isEmpty) appliedFilters else snapshotFilters
            val updatedFilterGroups = filtersToUpdate.filterGroups.map { group ->
                if (group.id == filterGroupId) {
                    updateFilterInGroup(group, filterId)
                } else {
                    group
                }
            }

            val updatedFilters = filtersToUpdate.copy(filterGroups = updatedFilterGroups)
            snapshotFilters = updatedFilters

            emissionMutableFlow.emit(
                FilterValidatorPartialState.FilterUpdateResult(
                    updatedFilters = snapshotFilters
                )
            )
        }
    }

    override fun updateDateFilter(
        filterGroupId: String,
        filterId: String,
        lowerLimit: LocalDateTime,
        upperLimit: LocalDateTime
    ) {
        scope.launch {
            val filtersToUpdate = if (snapshotFilters.isEmpty) appliedFilters else snapshotFilters
            val updatedFilterGroups = filtersToUpdate.filterGroups.map { group ->
                if (group.id == filterGroupId) {
                    updateFilterInGroup(group, filterId, lowerLimit, upperLimit)
                } else {
                    group
                }
            }

            val updatedFilters = filtersToUpdate.copy(filterGroups = updatedFilterGroups)
            snapshotFilters = updatedFilters

            emissionMutableFlow.emit(
                FilterValidatorPartialState.FilterUpdateResult(
                    updatedFilters = snapshotFilters
                )
            )
        }
    }

    override fun resetFilters() {
        // Return back to default filters
        appliedFilters = initialFilters
        // Remove if any selected filter
        snapshotFilters = Filters.emptyFilters()
        // Apply the default
        applyFilters()
    }

    override fun revertFilters() {
        scope.launch {
            snapshotFilters = Filters.emptyFilters()
            emissionMutableFlow.emit(
                FilterValidatorPartialState.FilterUpdateResult(
                    updatedFilters = appliedFilters
                )
            )
        }
    }

    override fun updateSortOrder(sortOrder: SortOrder) {
        scope.launch {
            val filterToUpdateOrderChange =
                if (snapshotFilters.isEmpty) appliedFilters else snapshotFilters
            snapshotFilters = filterToUpdateOrderChange.copy(sortOrder = sortOrder)
            emissionMutableFlow.emit(
                FilterValidatorPartialState.FilterUpdateResult(
                    updatedFilters = snapshotFilters
                )
            )
        }
    }

    override fun applyFilters() {
        scope.launch {
            if (snapshotFilters.isNotEmpty) {
                appliedFilters = snapshotFilters.copy()
                snapshotFilters = Filters.emptyFilters()
            }

            // Flatten all filters from all filter groups into a single list
            val allFilters = appliedFilters.filterGroups.flatMap { it.filters }

            // Check if all selected filters are default filters
            val allSelectedAreDefault = allFilters
                .filter { it.selected }
                .all { it.isDefault }

            // Check if all unselected filters are NOT default filters
            val allUnselectedAreNotDefault = allFilters
                .filter { !it.selected }
                .all { !it.isDefault }

            // Check if the sort order is the default one
            val isDefaultSortOrder = appliedFilters.sortOrder.isDefault

            // Combine the conditions to determine if exactly the default filters and sort order are applied
            val allDefaultAreSelected =
                allSelectedAreDefault && allUnselectedAreNotDefault && isDefaultSortOrder

            val filteredList = appliedFilters.filterGroups
                .fold(initialList) { currentList, group ->
                    when (group) {
                        is FilterGroup.MultipleSelectionFilterGroup<*> -> applyMultipleSelectionFilter(
                            currentList,
                            group
                        )

                        is FilterGroup.SingleSelectionFilterGroup -> applySingleSelectionFilter(
                            currentList,
                            group
                        )

                        is FilterGroup.ReversibleSingleSelectionFilterGroup -> applyReversibleSingleSelectionFilter(
                            currentList,
                            group
                        )

                        is FilterGroup.ReversibleMultipleSelectionFilterGroup<*> -> applyReversibleMultipleSelectionFilter(
                            currentList,
                            group
                        )
                    }
                }
                .filterByQuery(searchQuery)
                .applySort(appliedFilters) // Apply sort after fold in order to avoid random order

            val resultState = if (filteredList.items.isEmpty()) {
                FilterValidatorPartialState.FilterListResult.FilterListEmptyResult(
                    allDefaultFiltersAreSelected = allDefaultAreSelected,
                    updatedFilters = appliedFilters
                )
            } else {
                FilterValidatorPartialState.FilterListResult.FilterApplyResult(
                    filteredList = filteredList,
                    updatedFilters = appliedFilters,
                    allDefaultFiltersAreSelected = allDefaultAreSelected
                )
            }

            emissionMutableFlow.emit(resultState)
        }
    }

    override fun applySearch(query: String) {
        searchQuery = query
        applyFilters()
    }

    private fun toggleFilterSelection(
        filters: List<FilterElement>,
        filterId: String
    ): List<FilterElement> {
        return filters.map { filter ->
            if (filter.id == filterId && filter is FilterItem) {
                filter.copy(selected = !filter.selected)
            } else {
                filter
            }
        }
    }

    private fun applyMultipleSelectionFilter(
        currentList: FilterableList,
        group: FilterGroup.MultipleSelectionFilterGroup<*>,
    ): FilterableList {
        return if (group.filters.none { it.selected }) {
            FilterableList(emptyList())
        } else {
            group.filterableAction.applyFilter(currentList, group)
        }
    }

    private fun applySingleSelectionFilter(
        currentList: FilterableList,
        group: FilterGroup.SingleSelectionFilterGroup,
    ): FilterableList {
        val selectedFilter = group.filters.firstOrNull { it.selected }
        // Skip sorting because fold will mess the order anyway
        if (selectedFilter?.filterableAction is FilterAction.Sort<*, *>) return currentList
        return selectedFilter?.filterableAction?.applyFilter(
            appliedFilters.sortOrder,
            currentList,
            selectedFilter
        ) ?: currentList
    }

    private fun applyReversibleSingleSelectionFilter(
        currentList: FilterableList,
        group: FilterGroup.ReversibleSingleSelectionFilterGroup,
    ): FilterableList {
        val selectedFilter = group.filters.firstOrNull { it.selected }
        if (selectedFilter?.filterableAction is FilterAction.Sort<*, *>) return currentList
        return selectedFilter?.filterableAction?.applyFilter(
            appliedFilters.sortOrder,
            currentList,
            selectedFilter
        ) ?: currentList
    }

    private fun applyReversibleMultipleSelectionFilter(
        currentList: FilterableList,
        group: FilterGroup.ReversibleMultipleSelectionFilterGroup<*>,
    ): FilterableList {
        return if (group.filters.none { it.selected }) {
            currentList
        } else {
            group.filterableAction.applyFilter(currentList, group)
        }
    }

    /** Create a merged FilterGroup with updated filters **/
    private fun mergeFilters(
        newFilterGroup: FilterGroup,
        existingFilterGroup: FilterGroup,
    ): FilterGroup {
        val newFilters = newFilterGroup.filters
        val existingFilters = existingFilterGroup.filters
        val mergedFilters = mutableListOf<FilterElement>()

        // Iterate over the filters in the new group to merge selection states from existing filters
        newFilters.forEach { newFilter ->
            val existingFilter = existingFilters.find { it.id == newFilter.id }

            if (existingFilter != null) {
                // Filter exists in both new and existing groups, copy selection state
                when (newFilter) {
                    is FilterItem -> {
                        // For simple filters, just copy selection state
                        mergedFilters.add(newFilter.copy(selected = existingFilter.selected))
                    }

                    is FilterElement.DateTimeRangeFilterItem -> {
                        // Attempt to smart cast existing filter to DateTimeRangeFilterItem
                        val existingDateFilter =
                            existingFilter as? FilterElement.DateTimeRangeFilterItem
                        if (existingDateFilter != null) {
                            // Copy selection state and also preserve the selected date range
                            mergedFilters.add(
                                newFilter.copy(
                                    selected = existingDateFilter.selected,
                                    startDateTime = existingDateFilter.startDateTime,
                                    endDateTime = existingDateFilter.endDateTime
                                )
                            )
                        } else {
                            // Fallback: type mismatch, retain new filter as-is
                            mergedFilters.add(newFilter)
                        }
                    }
                }
            } else {
                // Filter is new, add it directly without any changes
                mergedFilters.add(newFilter)
            }
        }

        // Reconstruct and return the appropriate FilterGroup subtype with merged filters
        return when (newFilterGroup) {
            is FilterGroup.MultipleSelectionFilterGroup<*> -> {
                newFilterGroup.copy(filters = mergedFilters)
            }

            is FilterGroup.SingleSelectionFilterGroup -> {
                newFilterGroup.copy(filters = mergedFilters)
            }

            is FilterGroup.ReversibleSingleSelectionFilterGroup -> {
                newFilterGroup.copy(filters = mergedFilters)
            }

            is FilterGroup.ReversibleMultipleSelectionFilterGroup<*> -> {
                newFilterGroup.copy(filters = mergedFilters)
            }
        }
    }

}