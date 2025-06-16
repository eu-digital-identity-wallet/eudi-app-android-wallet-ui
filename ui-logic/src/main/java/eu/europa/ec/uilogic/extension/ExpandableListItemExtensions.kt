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
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi

/**
 * Recursively traverses an [ExpandableListItemUi] and toggles the `isChecked` state of a checkbox if the item with the given [id] is found.
 *
 * If the current item is a [ExpandableListItemUi.NestedListItem], it recursively calls this method on its `nestedItems`.
 *
 * If the current item is a [ExpandableListItemUi.SingleListItem] and its header's `itemId` matches the provided [id],
 * and if the `trailingContentData` is a [ListItemTrailingContentDataUi.Checkbox], then the `isChecked` property of the checkbox data is toggled.
 *
 * If the [id] is not found or if it is found in a `SingleListItemDataUi` that does not have a checkbox as trailing content, the original item is returned.
 *
 * @param id The ID of the item whose checkbox's `isChecked` state should be toggled.
 * @return A new [ExpandableListItemUi] with the specified item's checkbox state toggled, or the original item if the [id] is not found or if the element is not a checkbox.
 */
fun ExpandableListItemUi.toggleCheckboxState(id: String): ExpandableListItemUi {
    return when (this) {
        is ExpandableListItemUi.NestedListItem -> {
            this.copy(
                nestedItems = nestedItems.map {
                    it.toggleCheckboxState(id)
                }
            )
        }

        is ExpandableListItemUi.SingleListItem -> {
            if (this.header.itemId == id && this.header.trailingContentData is ListItemTrailingContentDataUi.Checkbox) {
                val currentItem =
                    this.header.trailingContentData as ListItemTrailingContentDataUi.Checkbox

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
                        nestedItem.header.trailingContentData as ListItemTrailingContentDataUi.Icon

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