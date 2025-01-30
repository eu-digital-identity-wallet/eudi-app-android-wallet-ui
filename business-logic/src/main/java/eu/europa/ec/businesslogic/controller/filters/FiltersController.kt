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

package eu.europa.ec.businesslogic.controller.filters

import eu.europa.ec.businesslogic.model.FilterableList
import eu.europa.ec.businesslogic.model.Filters
import eu.europa.ec.businesslogic.model.SortOrder
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

sealed interface FiltersControllerPartialState {
    val updatedFilters: Filters

    data class FilterApplyResult(
        val filteredList: FilterableList,
        override val updatedFilters: Filters,
    ) : FiltersControllerPartialState

    data class FilterUpdateResult(
        override val updatedFilters: Filters,
    ) : FiltersControllerPartialState
}

interface FiltersController {
    fun onFilterStateChange(): Flow<FiltersControllerPartialState>
    fun initializeFilters(
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

class FiltersControllerImpl : FiltersController {

    // Filters
    private var appliedFilters: Filters = Filters.emptyFilters()
    private var snapshotFilters: Filters = Filters.emptyFilters()
    private var initialFilters: Filters = Filters.emptyFilters()

    // Search
    private var searchQuery: String = ""

    // Collection
    private lateinit var initialList: FilterableList
    private lateinit var filteredList: FilterableList

    // Flow
    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private val filterResultMutableFlow = MutableSharedFlow<FiltersControllerPartialState>()
    private val filterResultFlow: SharedFlow<FiltersControllerPartialState> =
        filterResultMutableFlow
            .debounce(300)
            .shareIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(),
                replay = 1
            )

    override fun initializeFilters(
        filters: Filters,
        filterableList: FilterableList,
    ) {
        this.initialFilters = filters
        this.appliedFilters = filters
        this.initialList = filterableList
        this.filteredList = filterableList
    }

    override fun updateLists(filterableList: FilterableList) {
        this.initialList = filterableList
        this.filteredList = filterableList
        applyFilters()
    }

    override fun onFilterStateChange(): Flow<FiltersControllerPartialState> = filterResultFlow

    override fun updateFilter(filterGroupId: String, filterId: String) {
        val filtersToUpdate = if (snapshotFilters.isEmpty) appliedFilters else snapshotFilters
        val updatedFilterGroups = filtersToUpdate.filterGroups.map { group ->
            if (group.id == filterGroupId) {
                group.copy(filters = group.filters.map { filter ->
                    if (filter.id == filterId) {
                        filter.copy(selected = true) // Select the target filter
                    } else {
                        filter.copy(selected = false) // Deselect other filters in the same group
                    }
                })
            } else {
                group // Keep other groups unchanged
            }
        }

        val updatedFilters = appliedFilters.copy(filterGroups = updatedFilterGroups)
        snapshotFilters = updatedFilters

        scope.launch {
            filterResultMutableFlow.emit(
                FiltersControllerPartialState.FilterUpdateResult(
                    updatedFilters = snapshotFilters
                )
            )
        }
    }

    override fun resetFilters() {
        appliedFilters = initialFilters
        scope.launch {
            filterResultMutableFlow.emit(
                FiltersControllerPartialState.FilterApplyResult(
                    filteredList = initialList,
                    updatedFilters = appliedFilters
                )
            )
        }
    }

    override fun revertFilters() {
        snapshotFilters = Filters.emptyFilters()
        scope.launch {
            filterResultMutableFlow.emit(
                FiltersControllerPartialState.FilterUpdateResult(
                    updatedFilters = appliedFilters
                )
            )
        }
    }

    override fun updateSortOrder(sortOrder: SortOrder) {
        snapshotFilters = appliedFilters.copy(sortOrder = sortOrder)
        scope.launch {
            filterResultMutableFlow.emit(
                FiltersControllerPartialState.FilterUpdateResult(
                    updatedFilters = snapshotFilters
                )
            )
        }
    }

    override fun applyFilters() {
        if (!snapshotFilters.isEmpty) {
            appliedFilters = snapshotFilters.copy()
            snapshotFilters = Filters.emptyFilters()
        }
        val newList = appliedFilters
            .filterGroups
            .flatMap { it.filters }
            .filter { it.selected }
            .fold(filteredList) { currentList, filter ->
                filter.filterableAction.applyFilter(appliedFilters.sortOrder, currentList, filter)
            }.let {
                it.copy(items = it.items.filter { item ->
                    item.attributes.searchText.contains(
                        searchQuery,
                        ignoreCase = true
                    )
                })
            }

        scope.launch {
            filterResultMutableFlow.emit(
                FiltersControllerPartialState.FilterApplyResult(
                    filteredList = newList,
                    updatedFilters = appliedFilters
                )
            )
        }
    }

    override fun applySearch(query: String) {
        searchQuery = query
        val newList = filteredList.copy(items = filteredList.items.filter {
            it.attributes.searchText.contains(query, ignoreCase = true)
        })
        scope.launch {
            filterResultMutableFlow.emit(
                FiltersControllerPartialState.FilterApplyResult(
                    filteredList = newList,
                    updatedFilters = appliedFilters
                )
            )
        }
    }
}