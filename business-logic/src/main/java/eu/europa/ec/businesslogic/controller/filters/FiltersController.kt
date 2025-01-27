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
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class FilterResult(
    val filteredList: FilterableList,
    val updatedFilters: Filters,
)

interface FiltersController {
    fun onFilterStateChange(): Flow<FilterResult>
    fun initializeFilters(
        filters: Filters,
        filterableList: FilterableList,
        scope: CoroutineScope? = null,
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
    private var filters: Filters = Filters.emptyFilters()
    private var snapshotFilters: Filters = Filters.emptyFilters()
    private var initialFilters: Filters = Filters.emptyFilters()

    // Search
    private var searchQuery: String = ""

    // Collection
    private lateinit var initialList: FilterableList
    private lateinit var filteredList: FilterableList

    // Flow
    private var scope = CoroutineScope(Job() + Dispatchers.Main)
    private val filterResultMutableFlow = MutableSharedFlow<FilterResult>()
    private val filterResultFlow: SharedFlow<FilterResult> = filterResultMutableFlow.asSharedFlow()

    override fun initializeFilters(
        filters: Filters,
        filterableList: FilterableList,
        scope: CoroutineScope?,
    ) {
        this.initialFilters = filters
        this.filters = filters
        this.initialList = filterableList
        this.filteredList = filterableList
        scope?.let { this.scope = it }
    }

    override fun updateLists(filterableList: FilterableList) {
        this.initialList = filterableList
        this.filteredList = filterableList
        applyFilters()
    }

    override fun onFilterStateChange(): Flow<FilterResult> = filterResultFlow

    override fun updateFilter(filterGroupId: String, filterId: String) {
        val updatedFilterGroups = filters.filterGroups.map { group ->
            if (group.id == filterGroupId) {
                group.filters.find { it.id == filterId }?.let { targetFilter ->
                    group.copy(filters = group.filters.map {
                        if (it == targetFilter) it.copy(selected = true) else it.copy(selected = false)
                    })
                } ?: group
            } else {
                group
            }
        }

        val updatedFilters = filters.copy(filterGroups = updatedFilterGroups)
        snapshotFilters = updatedFilters

        scope.launch {
            filterResultMutableFlow.emit(FilterResult(filteredList, snapshotFilters))
        }
    }

    override fun resetFilters() {
        filters = initialFilters
        scope.launch {
            filterResultMutableFlow.emit(FilterResult(initialList, filters))
        }
    }

    override fun revertFilters() {
        snapshotFilters = Filters.emptyFilters()
        scope.launch {
            filterResultMutableFlow.emit(FilterResult(filteredList, filters))
        }
    }

    override fun updateSortOrder(sortOrder: SortOrder) {
        snapshotFilters = filters.copy(sortOrder = sortOrder)
        scope.launch {
            filterResultMutableFlow.emit(FilterResult(filteredList, snapshotFilters))
        }
    }

    override fun applyFilters() {
        if (!snapshotFilters.isEmpty) {
            filters = snapshotFilters.copy()
            snapshotFilters = Filters.emptyFilters()
        }
        val newList = filters
            .filterGroups
            .flatMap { it.filters }
            .filter { it.selected }
            .fold(filteredList) { currentList, filter ->
                filter.filterableAction.applyFilter(filters.sortOrder, currentList, filter)
            }.let {
                it.copy(items = filteredList.items.filter { item ->
                    item.attributes.searchText.contains(
                        searchQuery,
                        ignoreCase = true
                    )
                })
            }

        scope.launch {
            filterResultMutableFlow.emit(FilterResult(newList, filters))
        }
    }

    override fun applySearch(query: String) {
        searchQuery = query
        val newList = filteredList.copy(items = filteredList.items.filter {
            it.attributes.searchText.contains(query, ignoreCase = true)
        })
        scope.launch {
            filterResultMutableFlow.emit(FilterResult(newList, filters))
        }
    }
}