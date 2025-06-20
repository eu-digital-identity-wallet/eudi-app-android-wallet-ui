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

package eu.europa.ec.corelogic.extension

import eu.europa.ec.corelogic.model.ClaimDomain

/**
 * Recursively removes empty groups from a list of [ClaimDomain].
 *
 * This function traverses the list of [ClaimDomain] and filters out any [ClaimDomain.Group]
 * that, after recursively filtering its items, becomes empty.
 *
 * @receiver The list of [ClaimDomain] to filter.
 * @return A new list of [ClaimDomain] with empty groups removed.
 *         Groups are considered empty if, after recursively filtering their items,
 *         they contain no items. Non-group claims are always kept.
 */
fun List<ClaimDomain>.removeEmptyGroups(): List<ClaimDomain> {
    return this.mapNotNull { claim ->
        when (claim) {
            is ClaimDomain.Group -> {
                val filteredItems =
                    claim.items.removeEmptyGroups() // Recursively filter child groups
                if (filteredItems.isNotEmpty()) {
                    claim.copy(items = filteredItems) // Keep group if it has valid items
                } else {
                    null // Remove empty groups
                }
            }

            is ClaimDomain.Primitive -> claim // Keep non-group claims (Primitive)
        }
    }
}

/**
 * Recursively sorts a list of [ClaimDomain] based on the provided [selector].
 *
 * This function sorts the list of [ClaimDomain] by applying the [selector] to each element.
 * For [ClaimDomain.Group] elements, it recursively sorts the `items` within the group
 * before sorting the list at the current level. [ClaimDomain.Primitive] elements are left unchanged.
 *
 * @param selector A function that extracts a [Comparable] value from a [ClaimDomain] for sorting purposes.
 * @return A new list of [ClaimDomain] sorted recursively according to the [selector].
 */
fun <T : Comparable<T>> List<ClaimDomain>.sortRecursivelyBy(
    selector: (ClaimDomain) -> T
): List<ClaimDomain> {
    return this.map { claim ->
        when (claim) {
            is ClaimDomain.Group -> claim.copy(
                items = claim.items.sortRecursivelyBy(selector) // Recursively sort children
            )

            is ClaimDomain.Primitive -> claim // Primitives stay unchanged
        }
    }.sortedBy(selector) // Apply sorting at the current level
}