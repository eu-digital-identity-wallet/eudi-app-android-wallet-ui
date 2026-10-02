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

package eu.europa.ec.businesslogic.extension

import eu.europa.ec.businesslogic.validator.util.filterableList
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class TestFilterableListExtensions {

    //region filterByQuery

    // Case 1:
    // 1. Some items have searchable text and others have no tags.
    //
    // Case 1 Expected Result:
    // An empty query preserves every item and its order.
    @Test
    fun `Given Case 1, When filterByQuery is called, Then Case 1 Expected Result is returned`() {
        // Given
        val source = filterableList
        assertTrue(source.items.any { item -> item.attributes.searchTags.isEmpty() })

        // When
        val result = source.filterByQuery(searchQuery = "")

        // Then
        assertEquals(source, result)
    }

    // Case 2:
    // 1. Only one item has the mDL tag and other items include empty tags.
    //
    // Case 2 Expected Result:
    // Nonempty queries keep case-insensitive matching and exclude items without matching text.
    @Test
    fun `Given Case 2, When filterByQuery is called, Then Case 2 Expected Result is returned`() {
        // Given
        val source = filterableList
        val expectedItem = source.items[1]

        // When
        val matched = source.filterByQuery(searchQuery = "MDL")
        val missing = source.filterByQuery(searchQuery = "no matching tag")

        // Then
        assertEquals(listOf(expectedItem), matched.items)
        assertTrue(missing.items.isEmpty())
    }
    //endregion
}