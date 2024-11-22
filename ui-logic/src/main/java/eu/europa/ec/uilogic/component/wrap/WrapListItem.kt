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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItem
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.TextLengthPreviewProvider
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.extension.throttledClickable

@Composable
fun WrapListItem(
    modifier: Modifier = Modifier,
    item: ListItemData,
    onItemClick: ((ListItemData) -> Unit)? = null,
) {
    WrapCard(
        modifier = modifier,
        onClick = { onItemClick?.invoke(item) },
    ) {
        ListItem(
            item = item,
        )
    }
}

@Composable
fun WrapListItems(
    modifier: Modifier = Modifier,
    items: List<ListItemData>,
    clickable: Boolean = false,
    shape: Shape? = null,
    throttleClicks: Boolean = true,
    onItemClick: ((ListItemData) -> Unit)? = null,
) {
    WrapCard(shape = shape) {
        LazyColumn(
            modifier = modifier,
        ) {
            itemsIndexed(items) { index, item ->
                val itemModifier = Modifier
                    .then(
                        if (clickable) {
                            if (throttleClicks) {
                                Modifier.throttledClickable { onItemClick?.invoke(item) }
                            } else {
                                Modifier.clickable { onItemClick?.invoke(item) }
                            }
                        } else {
                            Modifier
                        }
                    )
                    //.throttledClickable { onItemClick?.invoke(item) }
                    .padding(
                        top = if (index == 0) SPACING_SMALL.dp else 0.dp,
                        bottom = if (index == items.lastIndex) SPACING_SMALL.dp else 0.dp,
                    )

                ListItem(
                    item = item,
                    modifier = itemModifier,
                )

                if (index < items.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@ThemeModePreviews
@Composable
private fun WrapListItemPreview(
    @PreviewParameter(TextLengthPreviewProvider::class) text: String
) {
    PreviewTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
        ) {
            WrapListItem(
                modifier = Modifier.fillMaxWidth(),
                item = ListItemData(
                    mainText = "Main text $text",
                )
            )
            WrapListItem(
                modifier = Modifier.fillMaxWidth(),
                item = ListItemData(
                    mainText = "Main text $text",
                    overlineText = "",
                    supportingText = "",
                )
            )
            WrapListItem(
                modifier = Modifier.fillMaxWidth(),
                item = ListItemData(
                    mainText = "Main text $text",
                    overlineText = "Overline text $text",
                    supportingText = "Supporting text $text",
                    leadingIcon = AppIcons.Sign,
                    trailingContentData = ListItemTrailingContentData.Icon(
                        iconData = AppIcons.KeyboardArrowRight,
                    ),
                )
            )
            WrapListItem(
                modifier = Modifier.fillMaxWidth(),
                item = ListItemData(
                    mainText = "Main text $text",
                    supportingText = "Supporting text $text",
                    trailingContentData = ListItemTrailingContentData.Icon(
                        iconData = AppIcons.KeyboardArrowRight,
                    ),
                )
            )
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
}