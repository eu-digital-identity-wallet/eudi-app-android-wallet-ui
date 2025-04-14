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

package eu.europa.ec.businesslogic.validator.util

import eu.europa.ec.businesslogic.validator.model.FilterAction
import eu.europa.ec.businesslogic.validator.model.FilterElement
import eu.europa.ec.businesslogic.validator.model.FilterElement.FilterItem
import eu.europa.ec.businesslogic.validator.model.FilterGroup
import eu.europa.ec.businesslogic.validator.model.FilterMultipleAction
import eu.europa.ec.businesslogic.validator.model.FilterableAttributes
import eu.europa.ec.businesslogic.validator.model.FilterableItem
import eu.europa.ec.businesslogic.validator.model.FilterableItemPayload
import eu.europa.ec.businesslogic.validator.model.FilterableList
import eu.europa.ec.businesslogic.validator.model.Filters
import eu.europa.ec.businesslogic.validator.model.SortOrder

val filterItemsSingle = listOf(
    FilterItem(
        id = "1",
        name = "PID",
        selected = true,
        filterableAction = FilterAction.Filter { attributes: TestAttributes, filterItem: FilterElement ->
            attributes.name == filterItem.name
        }
    ),
    FilterItem(
        id = "2",
        name = "mDL", selected = false,
        filterableAction = FilterAction.Filter { attributes: TestAttributes, filterItem: FilterElement ->
            attributes.name == filterItem.name
        }
    ),
    FilterItem(
        id = "3",
        name = "Age Verification", selected = false,
        filterableAction = FilterAction.Filter { attributes: TestAttributes, filterItem: FilterElement ->
            attributes.name == filterItem.name
        }
    )
)

val filterItemsMultipleSize3 = listOf(
    FilterItem(id = "1", name = "PID", selected = false),
    FilterItem(id = "2", name = "mDL", selected = true),
    FilterItem(id = "3", name = "Age Verification", selected = true),
)

val filterItemsMultipleSize4 = listOf(
    FilterItem(id = "1", name = "PID", selected = true),
    FilterItem(id = "2", name = "mDL", selected = true),
    FilterItem(id = "3", name = "Age Verification", selected = true),
    FilterItem(id = "4", name = "New Filter", selected = true),
)

val filterItemsMultiple = listOf(
    FilterItem(id = "1", name = "PID", selected = false),
    FilterItem(id = "2", name = "mDL", selected = true),
    FilterItem(id = "3", name = "Age Verification", selected = true),
    FilterItem(id = "4", name = "Search Test", selected = false),
    FilterItem(id = "5", name = "NO OP", selected = false),
)

val filterItemsMultipleNoSelections = listOf(
    FilterItem(id = "1", name = "PID", selected = false),
    FilterItem(id = "2", name = "mDL", selected = false),
    FilterItem(id = "3", name = "Age Verification", selected = false),
    FilterItem(id = "4", name = "Search Test", selected = false),
    FilterItem(id = "5", name = "NO OP", selected = false)
)

val filterItemsMultipleAllSelected = listOf(
    FilterItem(id = "1", name = "PID", selected = true),
    FilterItem(id = "2", name = "mDL", selected = true),
    FilterItem(id = "3", name = "Age Verification", selected = true),
    FilterItem(id = "4", name = "Search Test", selected = true),
    FilterItem(id = "5", name = "NO OP", selected = true)
)

val singleSelectionGroup = FilterGroup.SingleSelectionFilterGroup(
    id = "single_filter",
    name = "Document Selection",
    filters = filterItemsSingle,
)

val multipleSelectionGroupSize3 = FilterGroup.MultipleSelectionFilterGroup(
    id = "multi_filter",
    name = "Multi Document Selection",
    filters = filterItemsMultipleSize3,
    filterableAction = FilterMultipleAction { attributes: TestAttributes, filterItem: FilterElement ->
        filterItem.name == attributes.name
    }
)

val multipleSelectionGroupSize4 = FilterGroup.MultipleSelectionFilterGroup(
    id = "multi_filter",
    name = "Multi Document Selection",
    filters = filterItemsMultipleSize4,
    filterableAction = FilterMultipleAction { attributes: TestAttributes, filterItem: FilterElement ->
        filterItem.name == attributes.name
    }
)

val multipleSelectionGroup = FilterGroup.MultipleSelectionFilterGroup(
    id = "multi_filter",
    name = "Multi Document Selection",
    filters = filterItemsMultiple,
    filterableAction = FilterMultipleAction { attributes: TestAttributes, filterItem: FilterElement ->
        filterItem.name == attributes.name
    }
)

val multipleSelectionGroupNoSelection = FilterGroup.MultipleSelectionFilterGroup(
    id = "multi_filter_noselection",
    name = "Multi Document Selection",
    filters = filterItemsMultipleNoSelections,
    filterableAction = FilterMultipleAction { attributes: TestAttributes, filterItem: FilterElement ->
        filterItem.name == attributes.name
    }
)

val multipleSelectionGroupAllSelected = FilterGroup.MultipleSelectionFilterGroup(
    id = "multi_filter_all",
    name = "Multi Document Selection All",
    filters = filterItemsMultipleAllSelected,
    filterableAction = FilterMultipleAction { attributes: TestAttributes, filterItem: FilterElement ->
        filterItem.name == attributes.name
    }
)

val filtersWithSingleSelection = Filters(
    filterGroups = listOf(singleSelectionGroup),
    sortOrder = SortOrder.Ascending(isDefault = true)
)

val filtersWithMultipleSelectionSize3 = Filters(
    filterGroups = listOf(multipleSelectionGroupSize3),
    sortOrder = SortOrder.Descending()
)

val filtersWithMultipleSelectionSize4 = Filters(
    filterGroups = listOf(multipleSelectionGroupSize4),
    sortOrder = SortOrder.Descending()
)

val filtersWithMultipleSelection = Filters(
    filterGroups = listOf(multipleSelectionGroup),
    sortOrder = SortOrder.Descending()
)

val filtersWithMultipleSelectionNoSelection = Filters(
    filterGroups = listOf(multipleSelectionGroupNoSelection),
    sortOrder = SortOrder.Descending()
)

val filtersWithMultipleSelectionAllSelected = Filters(
    filterGroups = listOf(multipleSelectionGroupAllSelected),
    sortOrder = SortOrder.Ascending(isDefault = true)
)


val filterableList = FilterableList(
    items = listOf(
        FilterableItem(
            payload = TestPayload("Item 1"),
            attributes = TestAttributes(searchTags = listOf("PID"), name = "PID")
        ),
        FilterableItem(
            payload = TestPayload("Item 2"),
            attributes = TestAttributes(searchTags = listOf("mDL"), name = "mDL")
        ),
        FilterableItem(
            payload = TestPayload("Item 3"),
            attributes = TestAttributes(
                searchTags = listOf("Age Verification"),
                name = "Age Verification"
            )
        ),
        FilterableItem(
            payload = TestPayload("Item 4"),
            attributes = TestAttributes(searchTags = listOf("PID", "PIDIssuer"), name = "PID")
        ),
        FilterableItem(
            payload = TestPayload("Item 5"),
            attributes = TestAttributes(searchTags = emptyList(), name = "PID")
        ),
        FilterableItem(
            payload = TestPayload("Item 6"),
            attributes = TestAttributes(
                searchTags = listOf("search text", "secondary text"),
                name = "Search Test"
            )
        ),
        FilterableItem(
            payload = TestPayload("Item 7"),
            attributes = TestAttributes(searchTags = listOf("no op"), name = "NO OP")
        )
    )
)

data class TestPayload(val name: String) : FilterableItemPayload

data class TestAttributes(override val searchTags: List<String>, val name: String) :
    FilterableAttributes

