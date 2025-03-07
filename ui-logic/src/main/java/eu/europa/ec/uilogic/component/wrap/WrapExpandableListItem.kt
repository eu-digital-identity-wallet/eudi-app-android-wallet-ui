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

package eu.europa.ec.uilogic.component.wrap

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CardColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ClickableArea
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM

const val PATH_SEPARATOR = ","

sealed class ExpandableListItem {
    abstract val collapsed: ListItemData

    data class SingleListItemData( // Leaf
        override val collapsed: ListItemData //add DocItem
    ) : ExpandableListItem()

    data class NestedListItemData( // Group
        override val collapsed: ListItemData,
        val expanded: List<ExpandableListItem>,
        val isExpanded: Boolean
    ) : ExpandableListItem()
}

@Composable
fun WrapExpandableListItem(
    modifier: Modifier = Modifier,
    header: ListItemData,
    data: List<ExpandableListItem>,
    onItemClick: ((item: ListItemData) -> Unit)?,
    hideSensitiveContent: Boolean = false,
    isExpanded: Boolean,
    onExpandedChange: ((item: ListItemData) -> Unit)?,
    throttleClicks: Boolean = true,
    collapsedMainContentVerticalPadding: Dp = SPACING_MEDIUM.dp,
    collapsedClickableAreas: List<ClickableArea>? = null,
    expandedMainContentVerticalPadding: Dp = SPACING_MEDIUM.dp,
    expandedClickableAreas: List<ClickableArea>? = null,
    expandedAddDivider: Boolean = true,
    colors: CardColors? = null,
) {
    WrapExpandableCard(
        modifier = modifier,
        isExpanded = isExpanded,
        throttleClicks = throttleClicks,
        colors = colors,
        cardCollapsedContent = {
            WrapListItem(
                modifier = Modifier.fillMaxWidth(),
                item = header,
                onItemClick = onExpandedChange,
                throttleClicks = throttleClicks,
                hideSensitiveContent = false,
                mainContentVerticalPadding = collapsedMainContentVerticalPadding,
                clickableAreas = collapsedClickableAreas,
                colors = colors,
            )
        },
        cardExpandedContent = {
            data.forEach {
                when (it) {
                    is ExpandableListItem.SingleListItemData -> {
                        WrapListItem(
                            modifier = Modifier.fillMaxWidth(),
                            item = it.collapsed,
                            onItemClick = onItemClick,
                            throttleClicks = throttleClicks,
                            hideSensitiveContent = hideSensitiveContent,
                            mainContentVerticalPadding = expandedMainContentVerticalPadding,
                            clickableAreas = expandedClickableAreas,
                            colors = colors,
                        )
                    }

                    is ExpandableListItem.NestedListItemData -> {
                        WrapExpandableListItem(
                            modifier = Modifier.fillMaxWidth(),
                            header = it.collapsed,
                            data = it.expanded,
                            onItemClick = onItemClick,
                            onExpandedChange = onExpandedChange,
                            throttleClicks = throttleClicks,
                            hideSensitiveContent = hideSensitiveContent,
                            isExpanded = it.isExpanded,
                            collapsedMainContentVerticalPadding = collapsedMainContentVerticalPadding,
                            collapsedClickableAreas = collapsedClickableAreas,
                            expandedMainContentVerticalPadding = expandedMainContentVerticalPadding,
                            expandedClickableAreas = expandedClickableAreas,
                            expandedAddDivider = expandedAddDivider,
                            colors = colors
                        )
                    }
                }
            }
        }
    )
}

@ThemeModePreviews
@Composable
private fun WrapExpandableListItemPreview() {
    PreviewTheme {
        val data = ExpandableListItem.NestedListItemData(
            collapsed = ListItemData(
                itemId = "0",
                mainContentData = ListItemMainContentData.Text(text = "Digital ID"),
                supportingText = stringResource(R.string.request_collapsed_supporting_text),
                trailingContentData = ListItemTrailingContentData.Icon(
                    iconData = AppIcons.KeyboardArrowDown
                ),
            ),
            isExpanded = true,
            expanded = listOf(
                ExpandableListItem.SingleListItemData(
                    ListItemData(
                        itemId = "1",
                        overlineText = "Family name",
                        mainContentData = ListItemMainContentData.Text(text = "Doe"),
                    )
                ),
                ExpandableListItem.SingleListItemData(
                    ListItemData(
                        itemId = "1",
                        overlineText = "Given Name",
                        mainContentData = ListItemMainContentData.Text(text = "Doe"),
                    )
                ),
            )
        )

        /*WrapExpandableListItem(
            data = data,
            //isExpanded = true,
            onExpandedChange = {},
            onItemClick = {},
        )*/
    }
}