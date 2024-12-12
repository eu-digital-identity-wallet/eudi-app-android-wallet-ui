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

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.utils.DEFAULT_ICON_SIZE
import eu.europa.ec.uilogic.component.utils.ICON_SIZE_40
import eu.europa.ec.uilogic.component.utils.SIZE_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.CheckboxData
import eu.europa.ec.uilogic.component.wrap.WrapCheckbox
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapImage

data class ListItemData<T>(
    val event: T, //todo val event: T? = null, is this better?
    val itemId: String? = null,
    val mainText: String,
    val overlineText: String? = null,
    val supportingText: String? = null,
    val leadingIcon: IconData? = null,
    val trailingContentData: ListItemTrailingContentData? = null,
)

sealed class ListItemTrailingContentData {
    data class Icon(val iconData: IconData) : ListItemTrailingContentData()
    data class Checkbox(val checkboxData: CheckboxData) : ListItemTrailingContentData()
}

enum class ListItemClickArea {
    WHOLE_ROW,
    TRAILING_CONTENT
}

@Composable
fun <T> ListItem(
    item: ListItemData<T>,
    onEventSend: ((T) -> Unit)?,
    modifier: Modifier = Modifier,
    hideSensitiveContent: Boolean = false,
    mainTextVerticalPadding: Int? = null,
    overlineTextStyle: TextStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    ),
    //clickAreas: List<ListItemClickArea> = listOf(ListItemClickArea.TRAILING_CONTENT, ListItemClickArea.WHOLE_ROW)
) {
    val maxSecondaryTextLines = 1
    val textOverflow = TextOverflow.Ellipsis
    val mainTextStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface
    )

    // API check
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val blurModifier = remember(hideSensitiveContent) {
        if (supportsBlur && hideSensitiveContent) {
            Modifier.blur(10.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        } else {
            Modifier
        }
    }

    // Replace leading icon with "User" (default) icon if hiding sensitive content on unsupported APIs
    val leadingIconData: IconData? = remember(hideSensitiveContent, item.leadingIcon) {
        item.leadingIcon?.let { safeLeadingIcon ->
            if (hideSensitiveContent && !supportsBlur) {
                AppIcons.User
            } else {
                safeLeadingIcon
            }
        }
    }

    with(item) {
        Row(
            modifier = modifier
                //.then(
                //    if (clickAreas.contains(ListItemClickArea.WHOLE_ROW)) {
                //        Modifier.clickable {
                //            onEventSend?.invoke(item.event)
                //        }
                //    } else {
                //        Modifier
                //    }
                //)
                .padding(horizontal = SPACING_MEDIUM.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading Icon
            leadingIconData?.let { safeLeadingIcon ->
                WrapImage(
                    modifier = Modifier
                        .padding(end = SIZE_MEDIUM.dp)
                        .size(ICON_SIZE_40.dp)
                        .then(blurModifier),
                    iconData = safeLeadingIcon,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = mainTextVerticalPadding?.dp ?: SPACING_SMALL.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                // Overline Text
                overlineText?.let { safeOverlineText ->
                    Text(
                        text = safeOverlineText,
                        style = if (hideSensitiveContent && !supportsBlur) mainTextStyle else overlineTextStyle,
                        maxLines = maxSecondaryTextLines,
                        overflow = textOverflow,
                    )
                }

                // Main Text
                if (!hideSensitiveContent || supportsBlur) {
                    Text(
                        modifier = blurModifier,
                        text = mainText,
                        style = mainTextStyle,
                        maxLines = 2,
                        overflow = textOverflow,
                    )
                }

                // Supporting Text
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

            // Trailing Content
            trailingContentData?.let { safeTrailingContentData ->
                when (safeTrailingContentData) {
                    /*is ListItemTrailingContentData.Checkbox -> WrapCheckbox(
                        checkboxData = safeTrailingContentData.checkboxData.copy(
                            onCheckedChange = {
                                onEventSend(item.event)
                            }
                        ),
                        modifier = Modifier.padding(start = SIZE_MEDIUM.dp),
                    )*/

                    is ListItemTrailingContentData.Checkbox -> WrapEventCheckbox(
                        checkboxData = safeTrailingContentData.checkboxData,
                        onEventSend = {
                            if (onEventSend != null) {
                                onEventSend(item.event)
                            }
                        },
                        modifier = Modifier.padding(start = SIZE_MEDIUM.dp),
                    )

                    is ListItemTrailingContentData.Icon -> WrapIcon(
                        modifier = Modifier
                            .padding(start = SIZE_MEDIUM.dp)
                            .size(DEFAULT_ICON_SIZE.dp)
                            .then(
                                if (onEventSend != null) {
                                    Modifier.clickable {
                                        onEventSend(item.event)
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                        iconData = safeTrailingContentData.iconData,
                        customTint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}


/**
 * A wrapper for [WrapCheckbox] that triggers an event when the checkbox state changes.
 *
 * This composable function wraps the [WrapCheckbox] and adds a layer of event handling.
 * When the checkbox state changes, it invokes the provided `onEventSend` lambda function,
 * allowing you to send an event or perform other actions in response to the change.
 *
 * @param checkboxData The data for the checkbox, including the label, checked state, and
 * optional `onCheckedChange` callback.
 * @param onEventSend A lambda function that is called when the checkbox state changes.
 * @param modifier Modifier used to decorate the checkbox.
 */
@Composable
fun WrapEventCheckbox(
    checkboxData: CheckboxData,
    onEventSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WrapCheckbox(
        checkboxData = checkboxData.copy(
            onCheckedChange = {
                checkboxData.onCheckedChange?.invoke(it)
                onEventSend()
            }
        ),
        modifier = modifier,
    )
}

/*
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
}*/
