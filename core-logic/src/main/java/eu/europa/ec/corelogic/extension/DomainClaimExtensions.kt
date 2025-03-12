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

import eu.europa.ec.corelogic.model.DomainClaim

/**
 * Recursively removes empty groups from a list of [DomainClaim].
 *
 * This function traverses the list of [DomainClaim] and filters out any [DomainClaim.Group]
 * that, after recursively filtering its items, becomes empty.
 *
 * @receiver The list of [DomainClaim] to filter.
 * @return A new list of [DomainClaim] with empty groups removed.
 *         Groups are considered empty if, after recursively filtering their items,
 *         they contain no items. Non-group claims are always kept.
 */
fun List<DomainClaim>.removeEmptyGroups(): List<DomainClaim> {
    return this.mapNotNull { claim ->
        when (claim) {
            is DomainClaim.Group -> {
                val filteredItems =
                    claim.items.removeEmptyGroups() // Recursively filter child groups
                if (filteredItems.isNotEmpty()) {
                    claim.copy(items = filteredItems) // Keep group if it has valid items
                } else {
                    null // Remove empty groups
                }
            }

            is DomainClaim.Primitive -> claim // Keep non-group claims (Primitive)
        }
    }
}