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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

sealed class ExpandableListItem {
    abstract val header: ListItemData

    data class SingleListItemData(
        override val header: ListItemData,
    ) : ExpandableListItem()

    data class NestedListItemData(
        override val header: ListItemData,
        val nestedItems: List<ExpandableListItem>,
        val isExpanded: Boolean,
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
    collapsedMainContentVerticalPadding: Dp? = null,
    collapsedClickableAreas: List<ClickableArea>? = null,
    expandedMainContentVerticalPadding: Dp? = null,
    expandedClickableAreas: List<ClickableArea>? = null,
    addDivider: Boolean = true,
    shape: Shape? = null,
    colors: CardColors? = null
) {
    WrapExpandableCard(
        modifier = modifier,
        isExpanded = isExpanded,
        throttleClicks = throttleClicks,
        shape = shape,
        colors = colors,
        onExpandedChange = { onExpandedChange?.invoke(header) },
        cardCollapsedContent = {
            WrapListItem(
                modifier = Modifier.fillMaxWidth(),
                item = header,
                onItemClick = onExpandedChange,
                throttleClicks = throttleClicks,
                hideSensitiveContent = false,
                mainContentVerticalPadding = collapsedMainContentVerticalPadding,
                clickableAreas = collapsedClickableAreas,
                shape = RectangleShape,
                colors = colors,
                mainContentTextStyle = LocalTextStyle.current.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        cardExpandedContent = {
            data.forEachIndexed { index, listItem ->
                when (listItem) {
                    is ExpandableListItem.SingleListItemData -> {
                        WrapListItem(
                            modifier = Modifier.fillMaxWidth(),
                            item = listItem.header,
                            onItemClick = onItemClick,
                            throttleClicks = throttleClicks,
                            hideSensitiveContent = hideSensitiveContent,
                            mainContentVerticalPadding = expandedMainContentVerticalPadding,
                            clickableAreas = expandedClickableAreas,
                            shape = RectangleShape,
                            colors = colors,
                        )
                    }

                    is ExpandableListItem.NestedListItemData -> {
                        WrapExpandableListItem(
                            modifier = Modifier.fillMaxWidth(),
                            header = listItem.header,
                            data = listItem.nestedItems,
                            onItemClick = onItemClick,
                            onExpandedChange = onExpandedChange,
                            throttleClicks = throttleClicks,
                            hideSensitiveContent = hideSensitiveContent,
                            isExpanded = listItem.isExpanded,
                            collapsedMainContentVerticalPadding = collapsedMainContentVerticalPadding,
                            collapsedClickableAreas = collapsedClickableAreas,
                            expandedMainContentVerticalPadding = expandedMainContentVerticalPadding,
                            expandedClickableAreas = expandedClickableAreas,
                            shape = RectangleShape,
                            colors = colors
                        )
                    }
                }

                if (addDivider && index < data.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = SPACING_MEDIUM.dp))
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
            header = ListItemData(
                itemId = "0",
                mainContentData = ListItemMainContentData.Text(text = "Digital ID"),
                supportingText = stringResource(R.string.request_collapsed_supporting_text),
                trailingContentData = ListItemTrailingContentData.Icon(
                    iconData = AppIcons.KeyboardArrowDown
                ),
            ),
            isExpanded = true,
            nestedItems = listOf(
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

        WrapExpandableListItem(
            header = data.header,
            data = listOf(data),
            isExpanded = true,
            onExpandedChange = {},
            onItemClick = {},
        )
    }
}