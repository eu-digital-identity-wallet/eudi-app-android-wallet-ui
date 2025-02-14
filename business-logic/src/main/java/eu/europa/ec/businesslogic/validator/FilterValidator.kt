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
import eu.europa.ec.businesslogic.validator.model.FilterGroup
import eu.europa.ec.businesslogic.validator.model.FilterItem
import eu.europa.ec.businesslogic.validator.model.FilterableList
import eu.europa.ec.businesslogic.validator.model.Filters
import eu.europa.ec.businesslogic.validator.model.SortOrder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

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
    fun updateSortOrder(sortOrder: SortOrder)
}

class FilterValidatorImpl(dispatcher: CoroutineDispatcher = Dispatchers.IO) : FilterValidator {

    // Filters
    private var appliedFilters: Filters = Filters.emptyFilters()
    private var snapshotFilters: Filters = Filters.emptyFilters()
    private var initialFilters: Filters = Filters.emptyFilters()

    // Search
    private var searchQuery: String = ""

    // Collection
    private lateinit var initialList: FilterableList

    // Flow
    private val scope = CoroutineScope(Job() + dispatcher)
    private val emissionMutableFlow = MutableSharedFlow<FilterValidatorPartialState>()
    private val filterResultFlow: SharedFlow<FilterValidatorPartialState> = emissionMutableFlow
        .debounce(200L)
        .shareIn(
            scope,
            SharingStarted.WhileSubscribed(5000L),
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
                        if (filter.id == filterId) {
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
                        filter.copy(selected = filter.id == filterId)
                    }
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

            val allDefaultAreSelected = appliedFilters.filterGroups
                .flatMap { it.filters }
                .filter { it.isDefault }
                .all { it.selected == it.defaultSelected } && appliedFilters.sortOrder.isDefault

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
        )
            ?: currentList
    }

    /** Create a merged FilterGroup with updated filters **/
    private fun mergeFilters(
        newFilterGroup: FilterGroup,
        existingFilterGroup: FilterGroup,
    ): FilterGroup {
        val newFilters = newFilterGroup.filters
        val existingFilters = existingFilterGroup.filters
        val mergedFilters = mutableListOf<FilterItem>()

        newFilters.forEach { newFilter ->
            val existingFilter = existingFilters.find { it.id == newFilter.id }
            if (existingFilter != null) {
                // Filter exists in both, copy selection state
                mergedFilters.add(newFilter.copy(selected = existingFilter.selected))
            } else {
                // Filter is new, add it directly
                mergedFilters.add(newFilter)
            }
        }

        return when (newFilterGroup) {
            is FilterGroup.MultipleSelectionFilterGroup<*> -> {
                newFilterGroup.copy(filters = mergedFilters)
            }

            is FilterGroup.SingleSelectionFilterGroup -> {
                newFilterGroup.copy(filters = mergedFilters)
            }
        }
    }
}