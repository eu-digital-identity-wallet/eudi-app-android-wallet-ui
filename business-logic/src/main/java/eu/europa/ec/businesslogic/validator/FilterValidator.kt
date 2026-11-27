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

import eu.europa.ec.businesslogic.extension.applySort
import eu.europa.ec.businesslogic.extension.filterByQuery
import eu.europa.ec.businesslogic.validator.model.FilterAction
import eu.europa.ec.businesslogic.validator.model.FilterElement
import eu.europa.ec.businesslogic.validator.model.FilterElement.FilterItem
import eu.europa.ec.businesslogic.validator.model.FilterGroup
import eu.europa.ec.businesslogic.validator.model.FilterSort
import eu.europa.ec.businesslogic.validator.model.FilterableList
import eu.europa.ec.businesslogic.validator.model.Filters
import eu.europa.ec.businesslogic.validator.model.SortOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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

/** Mutations are applied asynchronously in caller order, including initialization before Apply. */
interface FilterValidator {
    fun onFilterStateChange(): Flow<FilterValidatorPartialState>
    fun initializeValidator(
        filters: Filters,
        filterableList: FilterableList,
    )

    fun updateLists(filterableList: FilterableList)
    fun applyFilters()

    /** Trims outer whitespace; blank queries clear only the search constraint. */
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

    fun updateSort(filterId: String)
    fun updateSortOrder(sortOrder: SortOrder)
}

class FilterValidatorImpl(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(5.seconds),
) : FilterValidator {

    private var lastMutation: Job? = null

    /** Preserve caller order, including Apply/Cancel followed immediately by a document refresh. */
    @Synchronized
    private fun mutate(block: suspend () -> Unit) {
        val previous = lastMutation
        lastMutation = scope.launch {
            previous?.join()
            block()
        }
    }

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
        .debounce(200.milliseconds)
        .shareIn(
            scope,
            sharingStarted,
            replay = 1
        )

    override fun initializeValidator(
        filters: Filters,
        filterableList: FilterableList,
    ) = mutate {
        this.initialFilters = filters
        val mergedFilterGroups = mutableListOf<FilterGroup>()
        filters.filterGroups.forEach { newFilterGroup ->
            // Find the corresponding group in appliedFilters
            val existingFilterGroup =
                appliedFilters.filterGroups.find { group -> group.id == newFilterGroup.id }

            if (existingFilterGroup != null) {
                val mergedFilters = mergeFilters(newFilterGroup, existingFilterGroup)
                mergedFilterGroups.add(mergedFilters)
            } else {
                mergedFilterGroups.add(newFilterGroup)
            }
        }

        appliedFilters = filters.copy(
            sortOrder = if (appliedFilters.isEmpty) filters.sortOrder else appliedFilters.sortOrder,
            filterGroups = mergedFilterGroups,
            sort = if (appliedFilters.isEmpty) {
                filters.sort
            } else {
                mergeSort(
                    newSort = filters.sort,
                    existingSort = appliedFilters.sort
                )
            }
        )
        this.initialList = filterableList
    }

    override fun updateLists(filterableList: FilterableList) = mutate {
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
        mutate {
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
        mutate {
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

    /** Returns to the initial filters and applies them in this same ordered mutation. */
    override fun resetFilters() = mutate {
        // Return back to default filters
        appliedFilters = initialFilters
        // Remove if any selected filter
        snapshotFilters = Filters.emptyFilters()
        // Apply the default
        applyFiltersNow()
    }

    override fun revertFilters() {
        mutate {
            snapshotFilters = Filters.emptyFilters()
            emissionMutableFlow.emit(
                FilterValidatorPartialState.FilterUpdateResult(
                    updatedFilters = appliedFilters
                )
            )
        }
    }

    override fun updateSortOrder(sortOrder: SortOrder) {
        mutate {
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

    override fun updateSort(filterId: String) {
        mutate {
            val filtersToUpdate = if (snapshotFilters.isEmpty) appliedFilters else snapshotFilters
            val updatedSort = filtersToUpdate.sort?.copy(
                filters = filtersToUpdate.sort.filters.map { filter ->
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

            snapshotFilters = filtersToUpdate.copy(sort = updatedSort)

            emissionMutableFlow.emit(
                FilterValidatorPartialState.FilterUpdateResult(
                    updatedFilters = snapshotFilters
                )
            )
        }
    }

    override fun applyFilters() = mutate { applyFiltersNow() }

    private suspend fun applyFiltersNow() {
        if (snapshotFilters.isNotEmpty) {
            appliedFilters = snapshotFilters.copy()
            snapshotFilters = Filters.emptyFilters()
        }

        // Flatten all user filters and sort selectors into a single list
        val allFilters = buildList {
            addAll(appliedFilters.filterGroups.flatMap { group -> group.filters })
            addAll(appliedFilters.sort?.filters.orEmpty())
        }

        // Check if all selected filters are default filters
        val allSelectedAreDefault = allFilters
            .filter { filter -> filter.selected }
            .all { filter -> filter.isDefault }

        // Check if all unselected filters are NOT default filters
        val allUnselectedAreNotDefault = allFilters
            .filter { filter -> !filter.selected }
            .all { filter -> !filter.isDefault }

        // Check if the sort order is the default one
        val isDefaultSortOrder = appliedFilters.sortOrder.isDefault

        // Combine the conditions to determine if exactly the default filters and sort order are
        // applied
        val allDefaultAreSelected =
            allSelectedAreDefault && allUnselectedAreNotDefault && isDefaultSortOrder

        val filteredList = appliedFilters.filterGroups
            .fold(initialList) { currentList, group ->
                when (group) {
                    is FilterGroup.MultipleSelectionFilterGroup<*> ->
                        applyMultipleSelectionFilter(
                            currentList,
                            group
                        )

                    is FilterGroup.SingleSelectionFilterGroup -> applySingleSelectionFilter(
                        currentList,
                        group
                    )

                    is FilterGroup.ReversibleSingleSelectionFilterGroup ->
                        applyReversibleSingleSelectionFilter(
                            currentList,
                            group
                        )

                    is FilterGroup.ReversibleMultipleSelectionFilterGroup<*> ->
                        applyReversibleMultipleSelectionFilter(
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

    override fun applySearch(query: String) = mutate {
        searchQuery = query.trim()
        applyFiltersNow()
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
        return if (group.filters.none { filter -> filter.selected }) {
            FilterableList(emptyList())
        } else {
            group.filterableAction.applyFilter(currentList, group)
        }
    }

    private fun applySingleSelectionFilter(
        currentList: FilterableList,
        group: FilterGroup.SingleSelectionFilterGroup,
    ): FilterableList {
        val selectedFilter = group.filters.firstOrNull { filter -> filter.selected }
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
        val selectedFilter = group.filters.firstOrNull { filter -> filter.selected }
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
        return if (group.filters.none { filter -> filter.selected }) {
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
        val mergedFilters = mergeFilterElements(
            newFilters = newFilterGroup.filters,
            existingFilters = existingFilterGroup.filters
        )

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

    private fun mergeSort(
        newSort: FilterSort?,
        existingSort: FilterSort?,
    ): FilterSort? {
        return when {
            newSort == null -> null
            existingSort == null -> newSort
            else -> newSort.copy(
                filters = mergeFilterElements(
                    newFilters = newSort.filters,
                    existingFilters = existingSort.filters
                )
            )
        }
    }

    private fun mergeFilterElements(
        newFilters: List<FilterElement>,
        existingFilters: List<FilterElement>,
    ): List<FilterElement> {
        val mergedFilters = mutableListOf<FilterElement>()

        newFilters.forEach { newFilter ->
            val existingFilter = existingFilters.find { filter -> filter.id == newFilter.id }

            if (existingFilter != null) {
                when (newFilter) {
                    is FilterItem -> {
                        mergedFilters.add(newFilter.copy(selected = existingFilter.selected))
                    }

                    is FilterElement.DateTimeRangeFilterItem -> {
                        val existingDateFilter =
                            existingFilter as? FilterElement.DateTimeRangeFilterItem
                        if (existingDateFilter != null) {
                            mergedFilters.add(
                                newFilter.copy(
                                    selected = existingDateFilter.selected,
                                    startDateTime = existingDateFilter.startDateTime,
                                    endDateTime = existingDateFilter.endDateTime
                                )
                            )
                        } else {
                            mergedFilters.add(newFilter)
                        }
                    }
                }
            } else {
                mergedFilters.add(newFilter)
            }
        }

        return mergedFilters
    }

}