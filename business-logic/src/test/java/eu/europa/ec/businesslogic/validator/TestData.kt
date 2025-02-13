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

import eu.europa.ec.businesslogic.validator.model.FilterGroup
import eu.europa.ec.businesslogic.validator.model.FilterItem
import eu.europa.ec.businesslogic.validator.model.FilterMultipleAction
import eu.europa.ec.businesslogic.validator.model.FilterableAttributes
import eu.europa.ec.businesslogic.validator.model.FilterableItem
import eu.europa.ec.businesslogic.validator.model.FilterableItemPayload
import eu.europa.ec.businesslogic.validator.model.FilterableList
import eu.europa.ec.businesslogic.validator.model.Filters
import eu.europa.ec.businesslogic.validator.model.SortOrder

// Sample FilterableAttributes Implementation for Tests
data class TestFilterableAttributes(
    override val searchTags: List<String> = emptyList()
) : FilterableAttributes

// Sample FilterableItemPayload Implementation for Tests
data class TestFilterableItemPayload(val name: String) : FilterableItemPayload

// Sample FilterableItem for Tests
val sampleFilterableItem = FilterableItem(
    payload = TestFilterableItemPayload("Test Item"),
    attributes = TestFilterableAttributes()
)

// Sample FilterableList for Tests
val sampleFilterableList = FilterableList(
    items = listOf(sampleFilterableItem)
)

// Sample FilterItem
val sampleFilterItem = FilterItem(
    id = "filter1",
    name = "Filter 1",
    selected = false
)

// Sample SingleSelectionFilterGroup
val sampleSingleSelectionGroup = FilterGroup.SingleSelectionFilterGroup(
    id = "group1",
    name = "Single Selection",
    filters = listOf(sampleFilterItem)
)

// Sample MultipleSelectionFilterGroup
val sampleMultipleSelectionGroup = FilterGroup.MultipleSelectionFilterGroup(
    id = "group2",
    name = "Multiple Selection",
    filters = listOf(sampleFilterItem),
    filterableAction = FilterMultipleAction<TestFilterableAttributes> { _, _ -> true }
)

// Sample Filters object
val sampleFilters = Filters(
    filterGroups = listOf(sampleSingleSelectionGroup, sampleMultipleSelectionGroup),
    sortOrder = SortOrder.ASCENDING
)
