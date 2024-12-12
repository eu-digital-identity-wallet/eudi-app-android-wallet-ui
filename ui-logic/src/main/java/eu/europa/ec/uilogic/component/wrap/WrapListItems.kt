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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.ListItem
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL

@Composable
fun <T> WrapListItems(
    items: List<ListItemData<T>>,
    modifier: Modifier = Modifier,
    hideSensitiveContent: Boolean = false,
    mainTextVerticalPadding: Int? = null,
    clickable: Boolean = false,
    shape: Shape? = null,
    throttleClicks: Boolean = true,
    onEventSend: (T) -> Unit
) {
    WrapCard(shape = shape) {
        Column(
            modifier = modifier,
            //userScrollEnabled = true,
        ) {
            //itemsIndexed(items) { index, item ->
            items.forEachIndexed { index, item ->
                val itemModifier = Modifier
                    //.then(
                    //    if (clickable) {
                    //        if (throttleClicks) {
                    //            Modifier.throttledClickable {
                    //                onEventSend(item.event)
                    //                //onItemClick?.invoke(item)
                    //            }
                    //        } else {
                    //            Modifier.clickable {
                    //                onEventSend(item.event)
                    //                //onItemClick?.invoke(item)
                    //            }
                    //        }
                    //    } else {
                    //        Modifier
                    //    }
                    //)
                    .padding(
                        top = if (index == 0) SPACING_SMALL.dp else 0.dp,
                        bottom = if (index == items.lastIndex) SPACING_SMALL.dp else 0.dp,
                    )
                //.clickable {
                //    onEventSend(item.event) //TODO is this ok? the same event twice?
                //}

                ListItem(
                    item = item,
                    modifier = itemModifier,
                    hideSensitiveContent = hideSensitiveContent,
                    mainTextVerticalPadding = mainTextVerticalPadding,
                    onEventSend = onEventSend
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
