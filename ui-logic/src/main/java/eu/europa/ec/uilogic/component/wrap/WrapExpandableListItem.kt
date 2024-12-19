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

import androidx.compose.foundation.shape.RoundedCornerShape
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
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.MainContentData
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM

data class ExpandableListItemData(
    val collapsed: ListItemData,
    val expanded: List<ListItemData>,
)

@Composable
fun WrapExpandableListItem(
    data: ExpandableListItemData,
    onItemClick: ((item: ListItemData) -> Unit)?,
    modifier: Modifier = Modifier,
    hideSensitiveContent: Boolean = false,
    isExpanded: Boolean,
    onExpandedChange: () -> Unit,
    throttleClicks: Boolean = true,
    collapsedMainContentVerticalPadding: Dp = SPACING_MEDIUM.dp,
    collapsedClickableAreas: List<ClickableArea>? = null,
    expandedMainContentVerticalPadding: Dp = 12.dp,
    expandedClickableAreas: List<ClickableArea>? = null,
    colors: CardColors? = null,
) {
    WrapExpandableCard(
        modifier = modifier,
        cardCollapsedContent = {
            WrapListItem(
                item = data.collapsed,
                onItemClick = {
                    onExpandedChange()
                },
                throttleClicks = throttleClicks,
                hideSensitiveContent = false,
                mainContentVerticalPadding = collapsedMainContentVerticalPadding,
                clickableAreas = collapsedClickableAreas,
                colors = colors,
            )
        },
        cardExpandedContent = {
            WrapListItems(
                items = data.expanded,
                onItemClick = onItemClick,
                hideSensitiveContent = hideSensitiveContent,
                mainContentVerticalPadding = expandedMainContentVerticalPadding,
                clickableAreas = expandedClickableAreas,
                shape = RoundedCornerShape(
                    bottomStart = SIZE_SMALL.dp,
                    bottomEnd = SIZE_SMALL.dp,
                ),
                colors = colors,
            )
        },
        isExpanded = isExpanded,
        throttleClicks = throttleClicks,
        colors = colors,
    )
}

@ThemeModePreviews
@Composable
private fun WrapExpandableListItemPreview() {
    PreviewTheme {
        val data = ExpandableListItemData(
            collapsed = ListItemData(
                itemId = "0",
                mainContentData = MainContentData.Text(text = "Digital ID"),
                supportingText = stringResource(R.string.request_collapsed_supporting_text),
                trailingContentData = ListItemTrailingContentData.Icon(
                    iconData = AppIcons.KeyboardArrowDown
                ),
            ),
            expanded = listOf(
                ListItemData(
                    itemId = "1",
                    overlineText = "Family name",
                    mainContentData = MainContentData.Text(text = "Doe"),
                ),
                ListItemData(
                    itemId = "2",
                    overlineText = "Given name",
                    mainContentData = MainContentData.Text(text = "John"),
                ),
                ListItemData(
                    itemId = "3",
                    overlineText = "Date of birth",
                    mainContentData = MainContentData.Text(text = "21 Oct 2023"),
                ),
            )
        )

        WrapExpandableListItem(
            data = data,
            isExpanded = true,
            onExpandedChange = {},
            onItemClick = {},
        )
    }
}