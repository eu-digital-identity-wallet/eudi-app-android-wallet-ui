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

package eu.europa.ec.uilogic.extension

import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem

/**
 * Recursively traverses an [ExpandableListItem] and toggles the `isChecked` state of a checkbox if the item with the given [id] is found.
 *
 * If the current item is a [ExpandableListItem.NestedListItemData], it recursively calls this method on its `nestedItems`.
 *
 * If the current item is a [ExpandableListItem.SingleListItemData] and its header's `itemId` matches the provided [id],
 * and if the `trailingContentData` is a [ListItemTrailingContentData.Checkbox], then the `isChecked` property of the checkbox data is toggled.
 *
 * If the [id] is not found or if it is found in a `SingleListItemData` that does not have a checkbox as trailing content, the original item is returned.
 *
 * @param id The ID of the item whose checkbox's `isChecked` state should be toggled.
 * @return A new [ExpandableListItem] with the specified item's checkbox state toggled, or the original item if the [id] is not found or if the element is not a checkbox.
 */
fun ExpandableListItem.toggleCheckboxState(id: String): ExpandableListItem {
    return when (this) {
        is ExpandableListItem.NestedListItemData -> {
            this.copy(
                nestedItems = nestedItems.map {
                    it.toggleCheckboxState(id)
                }
            )
        }

        is ExpandableListItem.SingleListItemData -> {
            if (this.header.itemId == id && this.header.trailingContentData is ListItemTrailingContentData.Checkbox) {
                val currentItem =
                    this.header.trailingContentData as ListItemTrailingContentData.Checkbox

                this.copy(
                    header = this.header.copy(
                        trailingContentData = currentItem.copy(
                            checkboxData = currentItem.checkboxData.copy(
                                isChecked = !currentItem.checkboxData.isChecked
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
 * Recursively traverses a list of [ExpandableListItem] and toggles the expanded state of the item with the given [id].
 *
 * If an [ExpandableListItem.NestedListItemData] has a header with a matching [id], its `isExpanded` property is toggled,
 * and its `trailingContentData` icon is updated to reflect the new expanded state (up arrow for expanded, down arrow for collapsed).
 * If a [ExpandableListItem.NestedListItemData] does not match, it will call this same method on its `nestedItems` to keep traversing.
 *
 * If an [ExpandableListItem.SingleListItemData] is encountered, it is returned as-is.
 *
 * @param id The ID of the item whose expanded state should be toggled.
 * @return A new list of [ExpandableListItem] with the specified item's expanded state toggled, and the arrow updated.
 *  Returns a new list, where the elements where not modified remain untouched.
 */
fun List<ExpandableListItem>.toggleExpansionState(id: String): List<ExpandableListItem> {
    return this.map { nestedItem ->
        when (nestedItem) {
            is ExpandableListItem.NestedListItemData -> {
                if (nestedItem.header.itemId == id && nestedItem.header.trailingContentData is ListItemTrailingContentData.Icon) {
                    val currentTrailingContent =
                        nestedItem.header.trailingContentData as ListItemTrailingContentData.Icon

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

            is ExpandableListItem.SingleListItemData -> {
                nestedItem
            }
        }
    }
}