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
fun List<ExpandableListItem>.changeNestedItems(id: String): List<ExpandableListItem> {
    return this.map { nestedItem ->
        when (nestedItem) {
            is ExpandableListItem.NestedListItemData -> {
                if (nestedItem.header.itemId == id) {
                    val newIsExpanded = !nestedItem.isExpanded
                    val newCollapsed = nestedItem.header.copy(
                        trailingContentData = ListItemTrailingContentData.Icon(
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
                        nestedItems = nestedItem.nestedItems.changeNestedItems(id)
                    )
                }
            }

            is ExpandableListItem.SingleListItemData -> {
                nestedItem
            }
        }
    }
}