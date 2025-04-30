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
import java.time.LocalDateTime

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
            return Filters(filterGroups = emptyList(), SortOrder.Ascending(isDefault = true))
        }
    }
}

sealed class FilterGroup {
    abstract val id: String
    abstract val name: String
    abstract val filters: List<FilterElement>

    data class SingleSelectionFilterGroup(
        override val id: String,
        override val name: String,
        override val filters: List<FilterElement>,
    ) : FilterGroup()

    data class ReversibleSingleSelectionFilterGroup(
        override val id: String,
        override val name: String,
        override val filters: List<FilterElement>,
    ) : FilterGroup()

    data class MultipleSelectionFilterGroup<T : FilterableAttributes>(
        override val id: String,
        override val name: String,
        override val filters: List<FilterElement>,
        val filterableAction: FilterMultipleAction<T>,
    ) : FilterGroup()

    data class ReversibleMultipleSelectionFilterGroup<T : FilterableAttributes>(
        override val id: String,
        override val name: String,
        override val filters: List<FilterElement>,
        val filterableAction: FilterMultipleAction<T>,
    ) : FilterGroup()
}

sealed class FilterElement {
    abstract val id: String
    abstract val name: String
    abstract val selected: Boolean
    abstract val isDefault: Boolean
    abstract val filterableAction: FilterAction

    data class FilterItem(
        override val id: String,
        override val name: String,
        override val selected: Boolean,
        override val isDefault: Boolean = false,
        override val filterableAction: FilterAction = DefaultFilterAction,
    ) : FilterElement() {
        companion object {
            fun emptyFilter(): FilterItem {
                return FilterItem(
                    id = "",
                    name = "",
                    selected = false
                )
            }
        }
    }

    data class DateTimeRangeFilterItem(
        override val id: String,
        override val name: String,
        override val selected: Boolean,
        override val isDefault: Boolean = false,
        val startDateTime: LocalDateTime,
        val endDateTime: LocalDateTime,
        override val filterableAction: FilterAction = DefaultFilterAction,
    ) : FilterElement()
}

data object DefaultFilterAction : FilterAction() {
    override fun applyFilter(
        sortOrder: SortOrder,
        filterableItems: FilterableList,
        filter: FilterElement,
    ): FilterableList {
        return filterableItems
    }
}

sealed class SortOrder {
    abstract val isDefault: Boolean

    data class Ascending(override val isDefault: Boolean = false) : SortOrder()
    data class Descending(override val isDefault: Boolean = false) : SortOrder()
}

@Suppress("UNCHECKED_CAST")
data class FilterMultipleAction<T : FilterableAttributes>(val predicate: (T, FilterElement) -> Boolean) {
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
        sortOrder: SortOrder = SortOrder.Ascending(isDefault = true),
        filterableItems: FilterableList,
        filter: FilterElement,
    ): FilterableList

    @Suppress("UNCHECKED_CAST")
    data class Filter<T : FilterableAttributes>(val predicate: (T, FilterElement) -> Boolean) :
        FilterAction() {
        override fun applyFilter(
            sortOrder: SortOrder,
            filterableItems: FilterableList,
            filter: FilterElement,
        ): FilterableList {
            return filterableItems.copy(
                items = filterableItems.items.filter {
                    predicate(
                        it.attributes as T,
                        filter
                    )
                }
            )
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
            filter: FilterElement,
        ): FilterableList {
            return filterableItems.copy(items = filterableItems.items.sortByOrder(sortOrder) {
                selector(
                    it.attributes as T
                )
            }.toMutableList())
        }
    }
}