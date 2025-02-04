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

import eu.europa.ec.businesslogic.extension.sortByOrder
import eu.europa.ec.businesslogic.validator.model.FilterGroup
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

sealed interface FiltersValidatorPartialState {
    val updatedFilters: Filters

    sealed interface FilterListResult : FiltersValidatorPartialState {
        val hasMoreThanDefaultFilters: Boolean

        data class FilterListEmptyResult(
            override val updatedFilters: Filters,
            override val hasMoreThanDefaultFilters: Boolean,
        ) : FilterListResult

        data class FilterApplyResult(
            val filteredList: FilterableList,
            override val hasMoreThanDefaultFilters: Boolean,
            override val updatedFilters: Filters,
        ) : FilterListResult
    }

    data class FilterUpdateResult(
        override val updatedFilters: Filters,
    ) : FiltersValidatorPartialState
}

interface FiltersValidator {
    fun onFilterStateChange(): Flow<FiltersValidatorPartialState>
    fun initializeFilters(
        filters: Filters,
        filterableList: FilterableList,
    )
    fun updateLists(filterableList: FilterableList, filters: Filters)
    fun applyFilters()
    fun applySearch(query: String)
    fun resetFilters()
    fun revertFilters()
    fun updateFilter(filterGroupId: String, filterId: String)
    fun updateSortOrder(sortOrder: SortOrder)
}

class FiltersControllerImpl : FiltersValidator {

    // Filters
    private var appliedFilters: Filters = Filters.emptyFilters()
    private var snapshotFilters: Filters = Filters.emptyFilters()
    private var initialFilters: Filters = Filters.emptyFilters()

    // Search
    private var searchQuery: String = ""

    // Collection
    private lateinit var initialList: FilterableList

    // Flow
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private val emissionMutableFlow = MutableSharedFlow<FiltersValidatorPartialState>()
    private val filterResultFlow: SharedFlow<FiltersValidatorPartialState> = emissionMutableFlow
        .debounce(200L)
        .shareIn(
            scope,
            SharingStarted.WhileSubscribed(5000L),
            replay = 1
        )


    private var hasMoreThanDefaultFilterApplied: Boolean = false

    override fun initializeFilters(
        filters: Filters,
        filterableList: FilterableList,
    ) {
        this.initialFilters = filters
        this.appliedFilters = filters
        // We want to sort by name (this is the default)
        // in order to prevent the random order
        this.initialList = filterableList.copy(
            items = filterableList.items
                .sortByOrder(filters.sortOrder) {
                    it.attributes.searchText.lowercase()
                }
        )
    }

    override fun updateLists(filterableList: FilterableList, filters: Filters) {
        this.initialList = filterableList
        this.initialFilters = filters
    }

    override fun onFilterStateChange(): Flow<FiltersValidatorPartialState> = filterResultFlow

    private fun updateFilterInGroup(group: FilterGroup, filterId: String): FilterGroup {
        val defaultFilter = group.filters.find { it.isDefault }
        val isClickedFilterDefault = defaultFilter?.id == filterId
        val updatedFilters = group.filters.map { filter ->
            when {
                isClickedFilterDefault -> filter.copy(selected = filter.isDefault)
                filter.id == filterId -> filter.copy(selected = !filter.selected)
                else -> filter.copy(selected = false)
            }
        }
        val noFilterSelected = updatedFilters.none { it.selected }
        return when {
            defaultFilter != null && noFilterSelected -> group.copy(filters = updatedFilters.map { filter ->
                filter.copy(selected = filter.isDefault)
            })

            !isClickedFilterDefault && updatedFilters.any { it.id == filterId && it.selected } -> group.copy(
                filters = updatedFilters.map { filter ->
                    if (filter.isDefault) filter.copy(selected = false) else filter
                }
            )

            else -> group.copy(filters = updatedFilters)
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
                FiltersValidatorPartialState.FilterUpdateResult(
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
        hasMoreThanDefaultFilterApplied = false
        applyFilters()
    }

    override fun revertFilters() {
        scope.launch {
            snapshotFilters = Filters.emptyFilters()
            emissionMutableFlow.emit(
                FiltersValidatorPartialState.FilterUpdateResult(
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
                FiltersValidatorPartialState.FilterUpdateResult(
                    updatedFilters = snapshotFilters
                )
            )
        }
    }

    override fun applyFilters() {
        scope.launch {
            if (!snapshotFilters.isEmpty) {
                appliedFilters = snapshotFilters.copy()
                snapshotFilters = Filters.emptyFilters()
                hasMoreThanDefaultFilterApplied = true
            }

            val newList = appliedFilters
                .filterGroups
                .flatMap { it.filters }
                .filter { it.selected }
                .fold(initialList) { currentList, filter ->
                    filter.filterableAction.applyFilter(appliedFilters.sortOrder, currentList, filter)
                }.let {
                    it.copy(items = it.items.filter { item ->
                        item.attributes.searchText.contains(
                            searchQuery,
                            ignoreCase = true
                        )
                    })
                }

            if (newList.items.isEmpty()) {
                emissionMutableFlow.emit(
                    FiltersValidatorPartialState.FilterListResult.FilterListEmptyResult(
                        hasMoreThanDefaultFilters = hasMoreThanDefaultFilterApplied,
                        updatedFilters = appliedFilters
                    )
                )
            } else {
                emissionMutableFlow.emit(
                    FiltersValidatorPartialState.FilterListResult.FilterApplyResult(
                        filteredList = newList,
                        updatedFilters = appliedFilters,
                        hasMoreThanDefaultFilters = hasMoreThanDefaultFilterApplied
                    )
                )
            }
        }
    }

    override fun applySearch(query: String) {
        searchQuery = query
        applyFilters()
    }
}