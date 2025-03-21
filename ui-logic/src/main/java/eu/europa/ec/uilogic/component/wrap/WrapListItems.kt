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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ClickableArea
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemLeadingContentData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.TextLengthPreviewProvider
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM

@Composable
fun WrapListItems(
    modifier: Modifier = Modifier,
    items: List<ExpandableListItem>,
    onItemClick: ((item: ListItemData) -> Unit)?,
    hideSensitiveContent: Boolean = false,
    mainContentVerticalPadding: Dp? = null,
    clickableAreas: List<ClickableArea>? = null,
    throttleClicks: Boolean = true,
    addDivider: Boolean = true,
    shape: Shape? = null,
    colors: CardColors? = null,
) {
    WrapCard(
        modifier = modifier,
        shape = shape,
        colors = colors,
    ) {
        items.forEachIndexed { index, item ->
            val itemModifier = Modifier
                .fillMaxWidth()

            when (item) {
                is ExpandableListItem.NestedListItemData -> {
                    WrapExpandableListItem(
                        modifier = itemModifier,
                        header = item.header,
                        data = item.nestedItems,
                        onItemClick = null,
                        onExpandedChange = onItemClick,
                        isExpanded = item.isExpanded,
                        throttleClicks = throttleClicks,
                        hideSensitiveContent = hideSensitiveContent,
                        collapsedMainContentVerticalPadding = SPACING_MEDIUM.dp,
                        shape = RectangleShape,
                    )
                }

                is ExpandableListItem.SingleListItemData -> {
                    WrapListItem(
                        modifier = itemModifier,
                        item = item.header,
                        onItemClick = null,
                        throttleClicks = throttleClicks,
                        hideSensitiveContent = hideSensitiveContent,
                        mainContentVerticalPadding = mainContentVerticalPadding,
                        clickableAreas = clickableAreas,
                    )
                }
            }

            if (addDivider && index < items.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = SPACING_MEDIUM.dp))
            }
        }
    }
}

@ThemeModePreviews
@Composable
private fun WrapListItemsPreview(
    @PreviewParameter(TextLengthPreviewProvider::class) text: String
) {
    PreviewTheme {
        val items = listOf(
            ListItemData(
                itemId = "1",
                mainContentData = ListItemMainContentData.Text(text = "Main text $text"),
            ),
            ListItemData(
                itemId = "2",
                mainContentData = ListItemMainContentData.Text(text = "Main text $text"),
                overlineText = "",
                supportingText = "",
            ),
            ListItemData(
                itemId = "3",
                mainContentData = ListItemMainContentData.Text(text = "Main text $text"),
                overlineText = "Overline text $text",
                supportingText = "Supporting text $text",
                leadingContentData = ListItemLeadingContentData.Icon(iconData = AppIcons.Sign),
                trailingContentData = ListItemTrailingContentData.Icon(
                    iconData = AppIcons.KeyboardArrowRight,
                ),
            ),
            ListItemData(
                itemId = "4",
                mainContentData = ListItemMainContentData.Text(text = "Main text $text"),
                overlineText = "Overline text $text",
                supportingText = "Supporting text $text",
                leadingContentData = ListItemLeadingContentData.Icon(iconData = AppIcons.Sign),
                trailingContentData = ListItemTrailingContentData.Checkbox(
                    checkboxData = CheckboxData(
                        isChecked = true,
                        enabled = true,
                    ),
                ),
            ),
            ListItemData(
                itemId = "5",
                mainContentData = ListItemMainContentData.Text(text = "Main text $text"),
                supportingText = "Supporting text $text",
                trailingContentData = ListItemTrailingContentData.Icon(
                    iconData = AppIcons.KeyboardArrowRight,
                ),
            ),
            ListItemData(
                itemId = "6",
                mainContentData = ListItemMainContentData.Text(text = "Main text $text"),
                supportingText = "Supporting text $text",
                trailingContentData = ListItemTrailingContentData.Checkbox(
                    checkboxData = CheckboxData(
                        isChecked = true,
                        enabled = true,
                    ),
                ),
            ),
        )

        WrapListItems(
            items = items.map { ExpandableListItem.SingleListItemData(it) },
            onItemClick = {},
        )
    }
}