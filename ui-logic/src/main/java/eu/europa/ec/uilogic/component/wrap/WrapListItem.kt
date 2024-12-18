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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import eu.europa.ec.uilogic.component.ClickableArea
import eu.europa.ec.uilogic.component.ListItem
import eu.europa.ec.uilogic.component.ListItemData

@Composable
fun WrapListItem(
    item: ListItemData,
    onItemClick: ((item: ListItemData) -> Unit)?,
    modifier: Modifier = Modifier,
    throttleClicks: Boolean = true,
    hideSensitiveContent: Boolean = false,
    mainContentVerticalPadding: Dp? = null,
    clickableAreas: List<ClickableArea>? = null,
) {
    WrapCard(
        modifier = Modifier.fillMaxWidth(),
        throttleClicks = throttleClicks,
    ) {
        ListItem(
            modifier = modifier,
            item = item,
            onItemClick = onItemClick,
            hideSensitiveContent = hideSensitiveContent,
            mainContentVerticalPadding = mainContentVerticalPadding,
            clickableAreas = clickableAreas ?: listOf(ClickableArea.ENTIRE_ROW),
        )
    }
}

/*
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
}*/
