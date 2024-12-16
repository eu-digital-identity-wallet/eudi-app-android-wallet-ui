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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.europa.ec.businesslogic.util.safeLet
import eu.europa.ec.uilogic.component.ListItemTrailingContentData.Checkbox
import eu.europa.ec.uilogic.component.ListItemTrailingContentData.Icon
import eu.europa.ec.uilogic.component.utils.DEFAULT_ICON_SIZE
import eu.europa.ec.uilogic.component.utils.ICON_SIZE_40
import eu.europa.ec.uilogic.component.utils.SIZE_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.CheckboxData
import eu.europa.ec.uilogic.component.wrap.WrapCheckbox
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapImage

/**
 * Represents the data displayed within a single item in a list.
 *
 * This class encapsulates all the information needed to render a list item,
 * including its content, optional visual elements like leading/trailing icons or checkboxes,
 * and an associated event (e.g., a click event).
 *
 * @param T The type of event associated with the list item (e.g., a click event).
 * @param event The event triggered when the list item is interacted with. This can be null if no event is associated.
 * @param itemId A unique identifier for this specific list item.
 * @param mainContentData The primary content displayed in the list item. This is typically text but could be other UI elements. See [MainContentData] for details.
 * @param overlineText Optional text displayed above the `mainContentData`, providing context or a brief heading.
 * @param supportingText Optional text displayed below the `mainContentData`, offering additional details or description.
 * @param leadingContentData Optional data for content displayed at the beginning of the list item. This could be an icon, image, or other visual element. See [ListItemLeadingContentData] for details.
 * @param trailingContentData Optional data for content displayed at the end of the list item. This could be an icon. checkbox, or other interactive element. See [ListItemTrailingContentData] for details.
 */
data class ListItemData<T>(
    val event: T?,
    val itemId: String,
    val mainContentData: MainContentData,
    val overlineText: String? = null,
    val supportingText: String? = null,
    val leadingContentData: ListItemLeadingContentData? = null,
    val trailingContentData: ListItemTrailingContentData? = null,
)

/**
 * Represents the data that can be displayed in the main content area.
 * This can be either text or an image in base64 format.
 */
sealed class MainContentData {
    data class Text(val text: String) : MainContentData()
    data class Image(val base64Image: String) : MainContentData()
}

/**
 * Represents the data that can be displayed as leading content in a list item.
 * This can be either an icon or a User image in base64 format.
 */
sealed class ListItemLeadingContentData {
    data class Icon(val iconData: IconData) : ListItemLeadingContentData()
    data class UserImage(val userBase64Image: String) : ListItemLeadingContentData()
}

/**
 * Represents the data that can be displayed in the trailing content of a list item.
 *
 * This sealed class offers two options for the trailing content:
 * - [Icon]: Displays an icon.
 * - [Checkbox]: Displays a checkbox with associated data.
 */
sealed class ListItemTrailingContentData {
    data class Icon(val iconData: IconData) : ListItemTrailingContentData()
    data class Checkbox(val checkboxData: CheckboxData) : ListItemTrailingContentData()
}

/**
 * A composable function that displays a list item with various content options.
 *
 * This function allows for customization of the list item's content, including leading and trailing elements,
 * as well as the main and supporting text. It also supports hiding sensitive content by blurring it.
 *
 * @param T The type of the item data.
 * @param item The [ListItemData] object containing the data to display in the list item.
 * @param onItemClick An optional lambda function that is invoked when the item is clicked. It receives the item data as a parameter.
 * @param modifier A [Modifier] that can be used to customize the appearance of the list item.
 * @param hideSensitiveContent A boolean flag indicating whether to hide sensitive content by blurring it.
 * @param mainContentVerticalPadding An optional value specifying the vertical padding for the main content.
 * @param overlineTextStyle The [TextStyle] to be applied to the overline text.
 */
@Composable
fun <T> ListItem(
    item: ListItemData<T>,
    onItemClick: ((T) -> Unit)?,
    modifier: Modifier = Modifier,
    hideSensitiveContent: Boolean = false,
    mainContentVerticalPadding: Dp? = null,
    overlineTextStyle: TextStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    ),
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

    val leadingContentData: ListItemLeadingContentData? =
        remember(hideSensitiveContent, item.leadingContentData) {
            item.leadingContentData?.let { safeLeadingContentData ->
                if (hideSensitiveContent && !supportsBlur) {
                    null
                } else {
                    safeLeadingContentData
                }
            }
        }

    with(item) {
        Row(
            modifier = modifier
                .padding(horizontal = SPACING_MEDIUM.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading Content
            leadingContentData?.let { safeLeadingContentData ->

                val leadingContentModifier = Modifier
                    .padding(end = SIZE_MEDIUM.dp)
                    .size(ICON_SIZE_40.dp)
                    .then(blurModifier)

                when (safeLeadingContentData) {
                    is ListItemLeadingContentData.Icon -> WrapImage(
                        modifier = leadingContentModifier,
                        iconData = safeLeadingContentData.iconData,
                    )

                    is ListItemLeadingContentData.UserImage -> ImageOrPlaceholder(
                        modifier = leadingContentModifier,
                        base64Image = safeLeadingContentData.userBase64Image,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = mainContentVerticalPadding ?: SPACING_SMALL.dp),
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

                // Main Content
                if (!hideSensitiveContent || supportsBlur) {
                    when (mainContentData) {
                        is MainContentData.Image -> ImageOrPlaceholder(
                            modifier = Modifier
                                .wrapContentWidth()
                                .padding(top = SPACING_SMALL.dp)
                                .then(blurModifier),
                            base64Image = mainContentData.base64Image,
                            contentScale = ContentScale.Fit,
                        )

                        is MainContentData.Text -> Text(
                            modifier = blurModifier,
                            text = mainContentData.text,
                            style = mainTextStyle,
                            overflow = textOverflow,
                        )
                    }
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
                    is ListItemTrailingContentData.Checkbox -> WrapEventCheckbox(
                        checkboxData = safeTrailingContentData.checkboxData,
                        onItemClick = {
                            safeLet(onItemClick, event) { safeOnItemClick, safeEvent ->
                                safeOnItemClick(safeEvent)
                            }
                        },
                        modifier = Modifier.padding(start = SIZE_MEDIUM.dp),
                    )

                    is ListItemTrailingContentData.Icon -> WrapIcon(
                        modifier = Modifier
                            .padding(start = SIZE_MEDIUM.dp)
                            .size(DEFAULT_ICON_SIZE.dp)
                            .then(
                                other = safeLet(onItemClick, event) { safeOnItemClick, safeEvent ->
                                    Modifier.clickable {
                                        safeOnItemClick(safeEvent)
                                    }
                                } ?: Modifier
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
 * @param onItemClick A lambda function that is called when the checkbox state changes.
 * @param modifier Modifier used to decorate the checkbox.
 */
@Composable
fun WrapEventCheckbox(
    checkboxData: CheckboxData,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WrapCheckbox(
        checkboxData = checkboxData.copy(
            onCheckedChange = {
                checkboxData.onCheckedChange?.invoke(it)
                onItemClick()
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
