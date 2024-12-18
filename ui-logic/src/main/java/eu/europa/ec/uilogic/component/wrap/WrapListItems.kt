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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.ClickableArea
import eu.europa.ec.uilogic.component.ListItem
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL

@Composable
fun WrapListItems(
    items: List<ListItemData>,
    onItemClick: ((item: ListItemData) -> Unit)?,
    modifier: Modifier = Modifier,
    hideSensitiveContent: Boolean = false,
    mainContentVerticalPadding: Dp? = null,
    clickableAreas: List<ClickableArea>? = null,
    shape: Shape? = null,
) {
    WrapCard(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            items.forEachIndexed { index, item ->
                val itemModifier = modifier.padding(
                    top = if (index == 0) SPACING_SMALL.dp else 0.dp,
                    bottom = if (index == items.lastIndex) SPACING_SMALL.dp else 0.dp,
                )

                ListItem(
                    item = item,
                    onItemClick = onItemClick,
                    modifier = itemModifier,
                    hideSensitiveContent = hideSensitiveContent,
                    mainContentVerticalPadding = mainContentVerticalPadding,
                    clickableAreas = clickableAreas ?: listOf(ClickableArea.TRAILING_CONTENT),
                )

                if (index < items.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

/*
@ThemeModePreviews
@Composable
private fun WrapListItemsPreview(
    @PreviewParameter(TextLengthPreviewProvider::class) text: String
) {
    PreviewTheme {
        val items = listOf(
            ListItemData(
                mainText = "Main text $text",
            ),
            ListItemData(
                mainText = "Main text $text",
                overlineText = "",
                supportingText = "",
            ),
            ListItemData(
                mainText = "Main text $text",
                overlineText = "Overline text $text",
                supportingText = "Supporting text $text",
                leadingIcon = AppIcons.Sign,
                trailingContentData = ListItemTrailingContentData.Icon(
                    iconData = AppIcons.KeyboardArrowRight,
                ),
            ),
            ListItemData(
                mainText = "Main text $text",
                overlineText = "Overline text $text",
                supportingText = "Supporting text $text",
                leadingIcon = AppIcons.Sign,
                trailingContentData = ListItemTrailingContentData.Checkbox(
                    checkboxData = CheckboxData(
                        isChecked = true,
                        enabled = true,
                        onCheckedChange = null,
                    ),
                ),
            ),
            ListItemData(
                mainText = "Main text $text",
                supportingText = "Supporting text $text",
                trailingContentData = ListItemTrailingContentData.Icon(
                    iconData = AppIcons.KeyboardArrowRight,
                ),
            ),
            ListItemData(
                mainText = "Main text $text",
                supportingText = "Supporting text $text",
                trailingContentData = ListItemTrailingContentData.Checkbox(
                    checkboxData = CheckboxData(
                        isChecked = true,
                        enabled = true,
                        onCheckedChange = null,
                    ),
                ),
            ),
        )

        WrapListItems(
            items = items,
        )
    }
}*/
