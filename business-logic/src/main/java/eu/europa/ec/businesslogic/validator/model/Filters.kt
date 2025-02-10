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

package eu.europa.ec.businesslogic.validator.model

import eu.europa.ec.businesslogic.extension.sortByOrder

data class Filters(
    val filterGroups: List<FilterGroup>,
    val sortOrder: SortOrder,
) {
    val isEmpty: Boolean
        get() = filterGroups.isEmpty()
    val isNotEmpty: Boolean
        get() = filterGroups.isNotEmpty()

    companion object {
        fun emptyFilters(): Filters {
            return Filters(filterGroups = emptyList(), SortOrder.ASCENDING)
        }
    }
}

sealed class FilterGroup {
    abstract val id: String
    abstract val name: String
    abstract val filters: List<FilterItem>

    data class SingleSelectionFilterGroup(
        override val id: String,
        override val name: String,
        override val filters: List<FilterItem>,
    ) : FilterGroup()

    data class MultipleSelectionFilterGroup<T: FilterableAttributes>(
        override val id: String,
        override val name: String,
        override val filters: List<FilterItem>,
        val filterableAction: FilterMultipleAction<T>
    ) : FilterGroup()
}

data class FilterItem(
    val id: String,
    val name: String,
    val selected: Boolean,
    val filterableAction: FilterAction = DefaultFilterAction,
)

data object DefaultFilterAction : FilterAction() {
    override fun applyFilter(
        sortOrder: SortOrder,
        filterableItems: FilterableList,
        filter: FilterItem,
    ): FilterableList {
        return filterableItems
    }
}

enum class SortOrder {
    ASCENDING,
    DESCENDING
}

@Suppress("UNCHECKED_CAST")
data class FilterMultipleAction<T : FilterableAttributes>(val predicate: (T, FilterItem) -> Boolean) {
    fun applyFilter(
        filterableItems: FilterableList,
        filterGroup: FilterGroup,
    ): FilterableList {
        val selectedFilters = filterGroup.filters.filter { it.selected }
        return if (selectedFilters.isEmpty()) {
            filterableItems
        } else {
            val matchingItems = mutableSetOf<FilterableItem>()
            selectedFilters.forEach { filter ->
                filterableItems.items.filter { item ->
                    predicate(item.attributes as T, filter)
                }.forEach { matchingItems.add(it) }
            }
            filterableItems.copy(items = matchingItems.toList())
        }
    }

}

sealed class FilterAction {
    abstract fun applyFilter(
        sortOrder: SortOrder = SortOrder.ASCENDING,
        filterableItems: FilterableList,
        filter: FilterItem,
    ): FilterableList

    @Suppress("UNCHECKED_CAST")
    data class Filter<T : FilterableAttributes>(val predicate: (T, FilterItem) -> Boolean) :
        FilterAction() {
        override fun applyFilter(
            sortOrder: SortOrder,
            filterableItems: FilterableList,
            filter: FilterItem,
        ): FilterableList {
            return filterableItems.copy(items = filterableItems.items.filter {
                predicate(
                    it.attributes as T,
                    filter
                )
            })
        }

    }

    /**
     * @param T The type of the attribute
     * @param R The type of the attribute to be sorted by
     * */
    @Suppress("UNCHECKED_CAST")
    data class Sort<T : FilterableAttributes, R : Comparable<R>>(val selector: (T) -> R?) :
        FilterAction() {
        override fun applyFilter(
            sortOrder: SortOrder,
            filterableItems: FilterableList,
            filter: FilterItem,
        ): FilterableList {
            return filterableItems.copy(items = filterableItems.items.sortByOrder(sortOrder) {
                selector(
                    it.attributes as T
                )
            }.toMutableList())
        }
    }
}