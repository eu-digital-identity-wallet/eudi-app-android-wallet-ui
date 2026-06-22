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

package eu.europa.ec.uilogic.extension

import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi

/**
 * Recursively walks this [ExpandableListItemUi] tree and flips the `isChecked` state of the
 * enabled checkbox whose `header.itemId` matches [id]. Items with no match, no checkbox, or a
 * disabled checkbox are returned unchanged; only the matched item is copied.
 */
fun ExpandableListItemUi.toggleCheckboxState(
    id: String,
): ExpandableListItemUi {
    return when (this) {
        is ExpandableListItemUi.NestedListItem -> {
            this.copy(
                nestedItems = nestedItems.map {
                    it.toggleCheckboxState(id)
                }
            )
        }

        is ExpandableListItemUi.SingleListItem -> {
            val trailingContent = header.trailingContentData

            if (
                header.itemId == id &&
                trailingContent is ListItemTrailingContentDataUi.Checkbox &&
                trailingContent.checkboxData.enabled
            ) {
                this.copy(
                    header = this.header.copy(
                        trailingContentData = trailingContent.copy(
                            checkboxData = trailingContent.checkboxData.copy(
                                isChecked = !trailingContent.checkboxData.isChecked
                            )
                        )
                    )
                )
            } else {
                this
            }
        }
    }
}

/**
 * Recursively traverses a list of [ExpandableListItemUi] and toggles the expanded state of the item with the given [id].
 *
 * If an [ExpandableListItemUi.NestedListItem] has a header with a matching [id], its `isExpanded` property is toggled,
 * and its `trailingContentData` icon is updated to reflect the new expanded state (up arrow for expanded, down arrow for collapsed).
 * If a [ExpandableListItemUi.NestedListItem] does not match, it will call this same method on its `nestedItems` to keep traversing.
 *
 * If an [ExpandableListItemUi.SingleListItem] is encountered, it is returned as-is.
 *
 * @param id The ID of the item whose expanded state should be toggled.
 * @return A new list of [ExpandableListItemUi] with the specified item's expanded state toggled, and the arrow updated.
 *  Returns a new list, where the elements where not modified remain untouched.
 */
fun List<ExpandableListItemUi>.toggleExpansionState(id: String): List<ExpandableListItemUi> {
    return this.map { nestedItem ->
        when (nestedItem) {
            is ExpandableListItemUi.NestedListItem -> {
                if (nestedItem.header.itemId == id && nestedItem.header.trailingContentData is ListItemTrailingContentDataUi.Icon) {
                    val newIsExpanded = !nestedItem.isExpanded

                    nestedItem.copy(
                        header = nestedItem.header.withExpansionIcon(newIsExpanded),
                        isExpanded = newIsExpanded
                    )
                } else {
                    nestedItem.copy(
                        nestedItems = nestedItem.nestedItems.toggleExpansionState(id)
                    )
                }
            }

            is ExpandableListItemUi.SingleListItem -> {
                nestedItem
            }
        }
    }
}

/**
 * Applies the expanded/collapsed state from [currentItems] to this list of nested items.
 *
 * This is useful when fresh [ExpandableListItemUi.NestedListItem] models are rebuilt from a
 * data source, but the UI should keep the user's current expansion choices. Matching is done by
 * each nested item's `header.itemId`.
 *
 * The function also updates each expandable header icon to match the applied state:
 * [AppIcons.KeyboardArrowUp] for expanded items and [AppIcons.KeyboardArrowDown] for collapsed
 * items.
 *
 * @param currentItems The current item tree whose expansion state should be copied.
 * @return A new list with expansion state and icons copied from [currentItems] where item IDs match.
 */
fun List<ExpandableListItemUi.NestedListItem>.withExpansionStateFrom(
    currentItems: List<ExpandableListItemUi.NestedListItem>,
): List<ExpandableListItemUi.NestedListItem> {
    val expansionStateById = currentItems.collectExpansionStateById()

    return map { item ->
        item.withExpansionStateFrom(expansionStateById)
    }
}

/**
 * Collapses all [ExpandableListItemUi.NestedListItem] entries in this list.
 *
 * The function preserves the immutable list-item structure while setting every expandable item to
 * `isExpanded = false` and replacing expandable header icons with [AppIcons.KeyboardArrowDown].
 *
 * @return A new list where all nested items are collapsed.
 */
fun List<ExpandableListItemUi.NestedListItem>.collapsedExpansionState(): List<ExpandableListItemUi.NestedListItem> {
    return map { item ->
        item.collapsedExpansionState()
    }
}

/**
 * Returns a copy of this [ListItemDataUi] with its trailing expansion icon set for [isExpanded].
 *
 * If the list item has no icon trailing content, it is returned unchanged.
 *
 * @param isExpanded Whether the item should show an expanded or collapsed indicator.
 * @return A copy with [AppIcons.KeyboardArrowUp] when expanded, or [AppIcons.KeyboardArrowDown]
 * when collapsed.
 */
fun ListItemDataUi.withExpansionIcon(isExpanded: Boolean): ListItemDataUi {
    val trailingContent = trailingContentData
    return if (trailingContent is ListItemTrailingContentDataUi.Icon) {
        copy(
            trailingContentData = trailingContent.copy(
                iconData = if (isExpanded) {
                    AppIcons.KeyboardArrowUp
                } else {
                    AppIcons.KeyboardArrowDown
                }
            )
        )
    } else {
        this
    }
}

private fun List<ExpandableListItemUi.NestedListItem>.collectExpansionStateById(): Map<String, Boolean> {
    return flatMap { item ->
        item.collectExpansionStateById()
    }.toMap()
}

private fun ExpandableListItemUi.NestedListItem.collectExpansionStateById(): List<Pair<String, Boolean>> {
    return listOf(header.itemId to isExpanded) + nestedItems.flatMap { item ->
        when (item) {
            is ExpandableListItemUi.NestedListItem -> item.collectExpansionStateById()
            is ExpandableListItemUi.SingleListItem -> emptyList()
        }
    }
}

private fun ExpandableListItemUi.NestedListItem.withExpansionStateFrom(
    expansionStateById: Map<String, Boolean>,
): ExpandableListItemUi.NestedListItem {
    val appliedIsExpanded = expansionStateById[header.itemId] ?: isExpanded

    return copy(
        header = header.withExpansionIcon(appliedIsExpanded),
        isExpanded = appliedIsExpanded,
        nestedItems = nestedItems.map { item ->
            when (item) {
                is ExpandableListItemUi.NestedListItem -> {
                    item.withExpansionStateFrom(expansionStateById)
                }

                is ExpandableListItemUi.SingleListItem -> {
                    item
                }
            }
        }
    )
}

private fun ExpandableListItemUi.NestedListItem.collapsedExpansionState(): ExpandableListItemUi.NestedListItem {
    return copy(
        header = header.withExpansionIcon(isExpanded = false),
        isExpanded = false,
        nestedItems = nestedItems.map { item ->
            when (item) {
                is ExpandableListItemUi.NestedListItem -> {
                    item.collapsedExpansionState()
                }

                is ExpandableListItemUi.SingleListItem -> {
                    item
                }
            }
        }
    )
}