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

package eu.europa.ec.uilogic.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.DEFAULT_ICON_SIZE
import eu.europa.ec.uilogic.component.utils.ICON_SIZE_40
import eu.europa.ec.uilogic.component.utils.SIZE_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.CheckboxData
import eu.europa.ec.uilogic.component.wrap.WrapCheckbox
import eu.europa.ec.uilogic.component.wrap.WrapIcon

data class ListItemData(
    val mainText: String,
    val overlineText: String? = null,
    val supportingText: String? = null,
    val leadingIcon: IconData? = null,
    val trailingContentData: ListItemTrailingContentData? = null,
    //val onClick: (() -> Unit)? = null,
)

sealed class ListItemTrailingContentData {
    data class Icon(val iconData: IconData) : ListItemTrailingContentData()
    data class Checkbox(val checkboxData: CheckboxData) : ListItemTrailingContentData()
}

@Composable
fun ListItem(
    item: ListItemData,
    modifier: Modifier = Modifier,
) {
    val maxSecondaryTextLines = 1
    val textOverflow = TextOverflow.Ellipsis

    with(item) {
        Row(
            modifier = modifier.padding(
                vertical = SPACING_EXTRA_SMALL.dp,
                horizontal = SPACING_MEDIUM.dp
            ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let { safeLeadingIcon ->
                WrapIcon(
                    iconData = safeLeadingIcon,
                    modifier = Modifier
                        .padding(end = SIZE_MEDIUM.dp)
                        .size(ICON_SIZE_40.dp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = SPACING_EXTRA_SMALL.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                overlineText?.let { safeOverlineText ->
                    Text(
                        text = safeOverlineText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = maxSecondaryTextLines,
                        overflow = textOverflow,
                    )
                }

                Text(
                    text = mainText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 2,
                    overflow = textOverflow,
                )

                supportingText?.let { safeSupportingText ->
                    Text(
                        text = safeSupportingText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = maxSecondaryTextLines,
                        overflow = textOverflow,
                    )
                }
            }

            trailingContentData?.let { safeTrailingContentData ->
                when (safeTrailingContentData) {
                    is ListItemTrailingContentData.Checkbox -> WrapCheckbox(
                        checkboxData = safeTrailingContentData.checkboxData,
                        modifier = Modifier.padding(start = SIZE_MEDIUM.dp),
                    )

                    is ListItemTrailingContentData.Icon -> WrapIcon(
                        iconData = safeTrailingContentData.iconData,
                        modifier = Modifier
                            .padding(start = SIZE_MEDIUM.dp)
                            .size(DEFAULT_ICON_SIZE.dp),
                        customTint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@ThemeModePreviews
@Composable
private fun ListItemPreview() {
    PreviewTheme {
        val modifier = Modifier.fillMaxWidth()
        Column(
            modifier = modifier
                .padding(SPACING_MEDIUM.dp),
            verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp)
        ) {
            // Basic ListItem with only mainText
            ListItem(
                item = ListItemData(
                    mainText = "Basic Item"
                ),
                modifier = modifier,
            )

            // ListItem with overlineText and supportingText
            ListItem(
                item = ListItemData(
                    mainText = "Item with Overline and Supporting Text",
                    overlineText = "Overline Text",
                    supportingText = "Supporting Text"
                ),
                modifier = modifier,
            )

            // ListItem with leadingIcon
            ListItem(
                item = ListItemData(
                    mainText = "Item with Leading Icon",
                    leadingIcon = AppIcons.Add,
                ),
                modifier = modifier,
            )

            // ListItem with trailing icon
            ListItem(
                item = ListItemData(
                    mainText = "Item with Trailing Icon",
                    trailingContentData = ListItemTrailingContentData.Icon(
                        iconData = AppIcons.KeyboardArrowDown,
                    )
                ),
                modifier = modifier,
            )

            // ListItem with trailing checkbox
            ListItem(
                item = ListItemData(
                    mainText = "Item with Trailing Checkbox",
                    trailingContentData = ListItemTrailingContentData.Checkbox(
                        checkboxData = CheckboxData(
                            isChecked = true,
                            onCheckedChange = {}
                        )
                    )
                ),
                modifier = modifier,
            )

            // ListItem with all elements
            ListItem(
                item = ListItemData(
                    mainText = "Full Item Example",
                    overlineText = "Overline Text",
                    supportingText = "Supporting Text",
                    leadingIcon = AppIcons.Add,
                    trailingContentData = ListItemTrailingContentData.Icon(
                        iconData = AppIcons.KeyboardArrowDown,
                    )
                ),
                modifier = modifier,
            )
        }
    }
}