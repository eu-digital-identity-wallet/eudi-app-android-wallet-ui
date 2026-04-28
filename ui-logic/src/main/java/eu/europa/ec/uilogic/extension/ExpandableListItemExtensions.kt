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
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi

/**
 * Recursively traverses this [ExpandableListItemUi] tree and toggles the `isChecked`
 * state of eligible checkbox items.
 *
 * An item will have its checkbox toggled if:
 * - its `header.itemId` matches the provided [id], **or**
 * - its `header.itemId` is contained in [coToggleIds],
 * - and its `trailingContentData` is a [ListItemTrailingContentDataUi.Checkbox]
 *   whose `checkboxData.enabled` flag is `true`.
 *
 * This allows a clicked item to toggle its own checkbox as well as the checkboxes
 * of related sibling items in a single traversal.
 *
 * Behavior:
 * - If the current item is a [ExpandableListItemUi.NestedListItem], the function
 *   recursively processes all of its `nestedItems`.
 * - If the current item is a [ExpandableListItemUi.SingleListItem], its checkbox
 *   state is toggled only if it satisfies the conditions above.
 * - Items without a checkbox, or with a disabled checkbox, remain unchanged.
 *
 * The function preserves immutability by returning a new tree structure where only
 * the affected items are copied and updated.
 *
 * @param id The ID of the primary item whose checkbox should be toggled.
 * @param coToggleIds Additional item IDs whose checkboxes should also be toggled
 * together with the primary item.
 * @return A new [ExpandableListItemUi] reflecting the updated checkbox state.
 * Items that do not match the conditions are returned unchanged.
 */
fun ExpandableListItemUi.toggleCheckboxState(
    id: String,
    coToggleIds: List<String>
): ExpandableListItemUi {
    return when (this) {
        is ExpandableListItemUi.NestedListItem -> {
            this.copy(
                nestedItems = nestedItems.map {
                    it.toggleCheckboxState(id, coToggleIds)
                }
            )
        }

        is ExpandableListItemUi.SingleListItem -> {
            val trailingContent = header.trailingContentData
            val isTarget = header.itemId == id || header.itemId in coToggleIds

            if (
                isTarget &&
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
 * Recursively traverses the [ExpandableListItemUi] and its children to collect all item IDs.
 *
 * If the current item is a [ExpandableListItemUi.NestedListItem], it flattens the IDs collected
 * from all its `nestedItems`. If it is a [ExpandableListItemUi.SingleListItem], it returns
 * a list containing its own header ID.
 *
 * @return A list of all item IDs contained within this item and its nested hierarchy.
 */
fun ExpandableListItemUi.collectAllNestedIds(): List<String> {
    return when (this) {
        is ExpandableListItemUi.NestedListItem -> {
            this.nestedItems.flatMap {
                it.collectAllNestedIds()
            }
        }

        is ExpandableListItemUi.SingleListItem -> {
            listOf(this.header.itemId)
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
                    val currentTrailingContent =
                        nestedItem.header.trailingContentData

                    val newIsExpanded = !nestedItem.isExpanded
                    val newCollapsed = nestedItem.header.copy(
                        trailingContentData = currentTrailingContent.copy(
                            iconData = if (newIsExpanded) {
                                AppIcons.KeyboardArrowUp
                            } else {
                                AppIcons.KeyboardArrowDown
                            }
                        )
                    )

                    nestedItem.copy(
                        header = newCollapsed,
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