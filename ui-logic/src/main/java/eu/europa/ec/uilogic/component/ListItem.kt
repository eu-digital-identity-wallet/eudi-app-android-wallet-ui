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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.ClickableArea.ENTIRE_ROW
import eu.europa.ec.uilogic.component.ClickableArea.TRAILING_CONTENT
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.DEFAULT_ICON_SIZE
import eu.europa.ec.uilogic.component.utils.ICON_SIZE_40
import eu.europa.ec.uilogic.component.utils.SIZE_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.CheckboxDataUi
import eu.europa.ec.uilogic.component.wrap.RadioButtonDataUi
import eu.europa.ec.uilogic.component.wrap.SwitchDataUi
import eu.europa.ec.uilogic.component.wrap.TextConfig
import eu.europa.ec.uilogic.component.wrap.WrapAsyncImage
import eu.europa.ec.uilogic.component.wrap.WrapCheckbox
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapIconButton
import eu.europa.ec.uilogic.component.wrap.WrapRadioButton
import eu.europa.ec.uilogic.component.wrap.WrapSwitch
import eu.europa.ec.uilogic.component.wrap.WrapText

/**
 * Represents the data displayed within a single item in a list.
 *
 * This class encapsulates all the information needed to render a list item,
 * including its content, optional visual elements like leading/trailing icons or checkboxes,
 * and any associated data.
 *
 * @param itemId A unique identifier for this specific list item. This is crucial for identifying
 * the item within the list, especially when handling interactions.
 * @param mainContentData The primary content displayed in the list item. This is typically text
 * but could be other UI elements. See [ListItemMainContentDataUi] for details on how to structure
 * the main content.
 * @param overlineText Optional text displayed above the `mainContentData`, providing context
 * or a brief heading for the item.
 * @param supportingText Optional text displayed below the `mainContentData`, offering
 * additional details or description to supplement the main content.
 * @param leadingContentData Optional data for content displayed at the beginning of the list item.
 * This could be an icon, image, or other visual element. See [ListItemLeadingContentDataUi]
 * for details on supported leading content types.
 * @param trailingContentData Optional data for content displayed at the end of the list item.
 * This could be an icon, checkbox, or other interactive element. See [ListItemTrailingContentDataUi]
 * for details on supported trailing content types.
 */
data class ListItemDataUi(
    val itemId: String,
    val mainContentData: ListItemMainContentDataUi,
    val overlineText: String? = null,
    val supportingText: String? = null,
    val leadingContentData: ListItemLeadingContentDataUi? = null,
    val trailingContentData: ListItemTrailingContentDataUi? = null,
)

/**
 * Represents the main content data for an item in a list.
 * This sealed class provides different types of content that can be displayed:
 * - [Text]: Simple text content.
 * - [Image]: An image represented as a Base64 encoded string.
 */
sealed class ListItemMainContentDataUi {
    data class Text(val text: String) : ListItemMainContentDataUi()
    data class Image(val base64Image: String) : ListItemMainContentDataUi()
}

/**
 * Represents data for the leading content within a list item.
 *
 * This sealed class provides a structured way to define the different types of
 * content that can be displayed at the leading edge of a list item. It supports
 * icons, user images (loaded from base64 strings), and images loaded asynchronously
 * from URLs.
 *
 * Each subclass of `ListItemLeadingContentData` represents a distinct type of
 * leading content, allowing for flexible and varied visual elements in lists.
 *
 * @property size The size (width and height) of the leading content in dp. This determines
 *                 the visual dimensions of the icon, image, etc.
 */
sealed class ListItemLeadingContentDataUi {
    abstract val size: Int

    data class Icon(
        override val size: Int = DEFAULT_ICON_SIZE,
        val iconData: IconDataUi,
        val tint: Color? = null,
    ) : ListItemLeadingContentDataUi()

    data class UserImage(
        override val size: Int = ICON_SIZE_40,
        val userBase64Image: String,
    ) : ListItemLeadingContentDataUi()

    data class AsyncImage(
        override val size: Int = ICON_SIZE_40,
        val imageUrl: String,
        val contentDescription: String?,
        val errorImage: IconDataUi? = null,
        val placeholderImage: IconDataUi? = null,
    ) : ListItemLeadingContentDataUi()
}

/**
 * Represents the data for the trailing content of a list item.
 *
 * This sealed class defines the possible types of trailing content that can be displayed
 * in a list item, allowing for different visual elements such as icons, checkboxes, and
 * radio buttons.
 *
 * The possible types are:
 *  - [Icon]: Represents an icon to be displayed as trailing content.
 *  - [Checkbox]: Represents a checkbox to be displayed as trailing content.
 *  - [RadioButton]: Represents a radio button to be displayed as trailing content.
 *  - [Switch]: Represents a switch to be displayed as trailing content.
 *  - [TextWithIcon]: Represents text and an icon to be displayed as trailing content.
 */
sealed class ListItemTrailingContentDataUi {
    data class Icon(val iconData: IconDataUi, val tint: Color? = null) :
        ListItemTrailingContentDataUi()

    data class Checkbox(val checkboxData: CheckboxDataUi) : ListItemTrailingContentDataUi()
    data class RadioButton(val radioButtonData: RadioButtonDataUi) : ListItemTrailingContentDataUi()
    data class Switch(val switchData: SwitchDataUi) : ListItemTrailingContentDataUi()
    data class TextWithIcon(
        val text: String,
        val iconData: IconDataUi,
        val tint: Color? = null
    ) : ListItemTrailingContentDataUi()
}

/**
 * Represents the clickable area of a [ListItem].
 *
 * This enum defines the regions within a [ListItem] that respond to user clicks.
 *
 * @property ENTIRE_ROW  The entire row of the [ListItem] is clickable.
 * @property TRAILING_CONTENT The trailing content (e.g., an icon or checkbox) of the [ListItem] is clickable.
 */
enum class ClickableArea {
    ENTIRE_ROW, TRAILING_CONTENT,
}

/**
 * A composable function that displays a list item with various content options.
 *
 * This function provides a flexible way to display list items with customizable content,
 * including leading and trailing elements, main and supporting text, and optional image content.
 * It also supports hiding sensitive content by blurring it on devices with Android S and above.
 *
 * **Content Customization:**
 * - **Leading Content:** Can be an icon or a user image specified by [ListItemDataUi.leadingContentData].
 * - **Main Content:** Can be text or an image specified by [ListItemDataUi.mainContentData].
 * - **Supporting Text:** Provides additional information below the main content, specified by [ListItemDataUi.supportingText].
 * - **Trailing Content:** Can be a checkbox or an icon specified by [ListItemDataUi.trailingContentData].
 * - **Overline Text:**  Displays text above the main content, specified by [ListItemDataUi.overlineText].
 *
 * **Sensitivity Handling:**
 * - If `hideSensitiveContent` is true and the device supports blurring (Android S and above), the content will be blurred.
 * - On devices that don't support blurring, sensitive content is either hidden or displayed as plain text
 *   depending on the content type (e.g., images are hidden, leading content is hidden, text is displayed).
 *
 * **Click Handling:**
 * - `onItemClick` is invoked when a clickable area of the item is clicked. It receives the [ListItemDataUi] object as a parameter.
 *   This allows you to handle item clicks and perform actions based on the selected item.
 * - `clickableAreas` defines which areas of the list item are clickable. By default, only the trailing content is clickable.
 *    You can set it to [ClickableArea.ENTIRE_ROW] to make the entire row clickable, or provide a custom list of clickable areas.
 *
 * @param item The [ListItemDataUi] object containing the data to display in the list item.
 * @param onItemClick An optional lambda function that is invoked when a clickable area of the item is clicked.
 * @param modifier A [Modifier] that can be used to customize the appearance of the list item.
 * @param hideSensitiveContent A boolean flag indicating whether to hide sensitive content by blurring it. Defaults to false.
 * @param mainContentVerticalPadding An optional value specifying the vertical padding */
@Composable
fun ListItem(
    item: ListItemDataUi,
    onItemClick: ((item: ListItemDataUi) -> Unit)?,
    modifier: Modifier = Modifier,
    hideSensitiveContent: Boolean = false,
    mainContentVerticalPadding: Dp? = null,
    clickableAreas: List<ClickableArea> = listOf(TRAILING_CONTENT),
    overlineTextStyle: TextStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    ),
    supportingTextColor: Color? = null,
    mainContentTextStyle: TextStyle? = null,
) {
    val maxSecondaryTextLines = 1
    val textOverflow = TextOverflow.Ellipsis
    val mainTextStyle = mainContentTextStyle ?: MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface
    )

    val blurModifier = remember(hideSensitiveContent) {
        if (hideSensitiveContent) {
            Modifier.blur(10.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        } else {
            Modifier
        }
    }

    // Determines the appropriate click handling for a list item's row based on its trailing content.
    // - If the trailing content is a radiobutton, checkbox or switch, the handling is only enabled if it is enabled.
    // - If the trailing content is an icon, or there is no trailing content, the handling is always the provided `onItemClick` function.
    val handleRowItemClick = when (val trailingContentData = item.trailingContentData) {
        is ListItemTrailingContentDataUi.RadioButton ->
            if (trailingContentData.radioButtonData.enabled) onItemClick
            else null

        is ListItemTrailingContentDataUi.Checkbox ->
            if (trailingContentData.checkboxData.enabled) onItemClick
            else null

        is ListItemTrailingContentDataUi.Switch ->
            if (trailingContentData.switchData.enabled) onItemClick
            else null

        is ListItemTrailingContentDataUi.Icon -> onItemClick
        is ListItemTrailingContentDataUi.TextWithIcon -> onItemClick
        null -> onItemClick
    }

    with(item) {
        Row(
            modifier = if (clickableAreas.contains(ENTIRE_ROW) && handleRowItemClick != null) {
                Modifier.clickable {
                    handleRowItemClick(item)
                }
            } else {
                Modifier
            }.then(
                other = modifier.padding(horizontal = SPACING_MEDIUM.dp)
            ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading Content
            leadingContentData?.let { safeLeadingContentData ->
                val leadingContentModifier = Modifier
                    .padding(end = SIZE_MEDIUM.dp)
                    .size(safeLeadingContentData.size.dp)
                    .then(blurModifier)

                when (safeLeadingContentData) {
                    is ListItemLeadingContentDataUi.Icon -> WrapIcon(
                        modifier = leadingContentModifier,
                        iconData = safeLeadingContentData.iconData,
                        customTint = safeLeadingContentData.tint
                            ?: MaterialTheme.colorScheme.primary,
                    )

                    is ListItemLeadingContentDataUi.UserImage -> ImageOrPlaceholder(
                        modifier = leadingContentModifier,
                        base64Image = safeLeadingContentData.userBase64Image,
                    )

                    is ListItemLeadingContentDataUi.AsyncImage -> WrapAsyncImage(
                        modifier = leadingContentModifier,
                        source = safeLeadingContentData.imageUrl,
                        error = safeLeadingContentData.errorImage,
                        placeholder = safeLeadingContentData.placeholderImage,
                        contentDescription = safeLeadingContentData.contentDescription
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
                        style = overlineTextStyle,
                    )
                }

                // Main Content
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (mainContentData) {
                        is ListItemMainContentDataUi.Image -> ImageOrPlaceholder(
                            modifier = Modifier
                                .wrapContentWidth()
                                .padding(top = SPACING_SMALL.dp)
                                .then(blurModifier),
                            base64Image = mainContentData.base64Image,
                            contentScale = ContentScale.Fit,
                        )

                        is ListItemMainContentDataUi.Text -> Text(
                            modifier = Modifier
                                .weight(1f)
                                .then(blurModifier),
                            text = mainContentData.text,
                            style = mainTextStyle,
                            overflow = textOverflow,
                        )
                    }

                    if (trailingContentData is ListItemTrailingContentDataUi.TextWithIcon) {
                        WrapText(
                            modifier = Modifier
                                .padding(start = SIZE_MEDIUM.dp),
                            text = trailingContentData.text,
                            textConfig = TextConfig(
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = Int.MAX_VALUE,
                            )
                        )

                        WrapIconButton(
                            modifier = Modifier
                                .padding(start = SPACING_SMALL.dp)
                                .size(DEFAULT_ICON_SIZE.dp),
                            iconData = trailingContentData.iconData,
                            customTint = trailingContentData.tint
                                ?: MaterialTheme.colorScheme.primary,
                            onClick = if (clickableAreas.contains(TRAILING_CONTENT)) {
                                { onItemClick?.invoke(item) }
                            } else null,
                            throttleClicks = false,
                        )
                    }
                }

                // Supporting Text
                supportingText?.let { safeSupportingText ->
                    Text(
                        text = safeSupportingText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = supportingTextColor
                                ?: MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = maxSecondaryTextLines,
                        overflow = textOverflow,
                    )
                }
            }

            // Trailing Content
            trailingContentData?.let { safeTrailingContentData ->
                when (safeTrailingContentData) {
                    is ListItemTrailingContentDataUi.Checkbox -> WrapCheckbox(
                        checkboxData = safeTrailingContentData.checkboxData.copy(
                            onCheckedChange = if (clickableAreas.contains(TRAILING_CONTENT)) {
                                { onItemClick?.invoke(item) }
                            } else null
                        ),
                        modifier = Modifier.padding(start = SIZE_MEDIUM.dp),
                    )

                    is ListItemTrailingContentDataUi.Icon -> WrapIconButton(
                        modifier = Modifier
                            .padding(start = SIZE_MEDIUM.dp)
                            .size(DEFAULT_ICON_SIZE.dp),
                        iconData = safeTrailingContentData.iconData,
                        customTint = safeTrailingContentData.tint
                            ?: MaterialTheme.colorScheme.primary,
                        onClick = if (clickableAreas.contains(TRAILING_CONTENT)) {
                            { onItemClick?.invoke(item) }
                        } else null,
                        throttleClicks = false,
                    )

                    is ListItemTrailingContentDataUi.RadioButton -> WrapRadioButton(
                        radioButtonData = safeTrailingContentData.radioButtonData.copy(
                            onCheckedChange = if (clickableAreas.contains(TRAILING_CONTENT)) {
                                { onItemClick?.invoke(item) }
                            } else null
                        ),
                        modifier = Modifier.padding(start = SIZE_MEDIUM.dp),
                    )

                    is ListItemTrailingContentDataUi.TextWithIcon -> Unit // No-op, it is handled by the main content.

                    is ListItemTrailingContentDataUi.Switch -> WrapSwitch(
                        switchData = safeTrailingContentData.switchData,
                        onCheckedChange = if (clickableAreas.contains(TRAILING_CONTENT)) {
                            { onItemClick?.invoke(item) }
                        } else null,
                        modifier = Modifier.padding(start = SIZE_MEDIUM.dp),
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
                item = ListItemDataUi(
                    itemId = "1",
                    mainContentData = ListItemMainContentDataUi.Text(text = "Basic Item")
                ),
                modifier = modifier,
                onItemClick = {},
            )

            // ListItem with overlineText and supportingText
            ListItem(
                item = ListItemDataUi(
                    itemId = "2",
                    mainContentData = ListItemMainContentDataUi.Text(text = "Item with Overline and Supporting Text"),
                    overlineText = "Overline Text",
                    supportingText = "Supporting Text"
                ),
                modifier = modifier,
                onItemClick = {},
            )

            // ListItem with leadingIcon
            ListItem(
                item = ListItemDataUi(
                    itemId = "3",
                    mainContentData = ListItemMainContentDataUi.Text(text = "Item with Leading Icon"),
                    leadingContentData = ListItemLeadingContentDataUi.Icon(iconData = AppIcons.Add),
                ),
                modifier = modifier,
                onItemClick = {},
            )

            // ListItem with icon for trailing content
            ListItem(
                item = ListItemDataUi(
                    itemId = "4",
                    mainContentData = ListItemMainContentDataUi.Text(text = "Item with Trailing Icon"),
                    trailingContentData = ListItemTrailingContentDataUi.Icon(
                        iconData = AppIcons.KeyboardArrowDown,
                    )
                ),
                modifier = modifier,
                onItemClick = {},
            )

            // ListItem with normal text for main content,
            // normal overline and supporting text,
            // and text with icon with normal text for trailing content
            ListItem(
                item = ListItemDataUi(
                    itemId = "5",
                    mainContentData = ListItemMainContentDataUi.Text(text = "Item with trailing TextWithIcon and Text"),
                    overlineText = "Overline Text",
                    trailingContentData = ListItemTrailingContentDataUi.TextWithIcon(
                        text = "1/2",
                        iconData = AppIcons.KeyboardArrowRight
                    ),
                    supportingText = "Supporting Text"
                ),
                modifier = modifier,
                onItemClick = {},
            )

            // ListItem with normal text for main content,
            // normal overline text,
            // and text with icon with normal text for trailing content
            ListItem(
                item = ListItemDataUi(
                    itemId = "6",
                    mainContentData = ListItemMainContentDataUi.Text(text = "Item with trailing TextWithIcon and Text"),
                    overlineText = "Overline Text",
                    trailingContentData = ListItemTrailingContentDataUi.TextWithIcon(
                        text = "1/2",
                        iconData = AppIcons.KeyboardArrowRight
                    ),
                ),
                modifier = modifier,
                onItemClick = {},
            )

            // ListItem with normal text for main content,
            // and text with icon with normal text for trailing content
            ListItem(
                item = ListItemDataUi(
                    itemId = "7",
                    mainContentData = ListItemMainContentDataUi.Text(text = "Item with TextWithIcon and Text"),
                    trailingContentData = ListItemTrailingContentDataUi.TextWithIcon(
                        text = "1/2",
                        iconData = AppIcons.KeyboardArrowRight
                    ),
                ),
                modifier = modifier,
                onItemClick = {},
            )

            // ListItem with very big text for main content,
            // normal overline text,
            // and text with icon with normal text for trailing content
            ListItem(
                item = ListItemDataUi(
                    itemId = "8",
                    mainContentData = ListItemMainContentDataUi.Text(text = "Item with very very very very very very big text"),
                    overlineText = "Overline Text",
                    trailingContentData = ListItemTrailingContentDataUi.TextWithIcon(
                        text = "1/2",
                        iconData = AppIcons.KeyboardArrowRight
                    )
                ),
                modifier = modifier,
                onItemClick = {},
            )

            // ListItem with normal text for main content,
            // very big overline text,
            // and text with icon with normal text for trailing content
            ListItem(
                item = ListItemDataUi(
                    itemId = "9",
                    overlineText = "Very very very very very very very very very very very very big Overline Text",
                    mainContentData = ListItemMainContentDataUi.Text(text = "Item with trailing Icon and Text"),
                    trailingContentData = ListItemTrailingContentDataUi.TextWithIcon(
                        text = "1/2",
                        iconData = AppIcons.KeyboardArrowRight
                    )
                ),
                modifier = modifier,
                onItemClick = {},
            )

            // ListItem with normal text for main content,
            // very big overline text,
            // and text with icon with very big text for trailing content
            ListItem(
                item = ListItemDataUi(
                    itemId = "10",
                    overlineText = "Very very very very very very very very very very very very big Overline Text",
                    mainContentData = ListItemMainContentDataUi.Text(text = "Item with trailing Icon and Text"),
                    trailingContentData = ListItemTrailingContentDataUi.TextWithIcon(
                        text = "Very very big trailing TextWithIcon",
                        iconData = AppIcons.KeyboardArrowRight
                    )
                ),
                modifier = modifier,
                onItemClick = {},
            )

            // ListItem with trailing enabled checkbox
            ListItem(
                item = ListItemDataUi(
                    itemId = "11",
                    mainContentData = ListItemMainContentDataUi.Text(text = "Item with Trailing Enabled Checkbox"),
                    trailingContentData = ListItemTrailingContentDataUi.Checkbox(
                        checkboxData = CheckboxDataUi(
                            isChecked = true,
                        )
                    )
                ),
                modifier = modifier,
                onItemClick = {},
            )

            // ListItem with trailing disabled checkbox
            ListItem(
                item = ListItemDataUi(
                    itemId = "12",
                    mainContentData = ListItemMainContentDataUi.Text(text = "Item with Trailing Disabled Checkbox"),
                    trailingContentData = ListItemTrailingContentDataUi.Checkbox(
                        checkboxData = CheckboxDataUi(
                            isChecked = true,
                            enabled = false,
                        )
                    )
                ),
                modifier = modifier,
                onItemClick = {},
            )

            // ListItem with trailing enabled radiobutton
            ListItem(
                item = ListItemDataUi(
                    itemId = "13",
                    mainContentData = ListItemMainContentDataUi.Text(text = "Item with Trailing Enabled Radiobutton"),
                    trailingContentData = ListItemTrailingContentDataUi.RadioButton(
                        radioButtonData = RadioButtonDataUi(
                            isSelected = true,
                        )
                    )
                ),
                modifier = modifier,
                onItemClick = {},
            )

            // ListItem with trailing disabled radiobutton
            ListItem(
                item = ListItemDataUi(
                    itemId = "14",
                    mainContentData = ListItemMainContentDataUi.Text(text = "Item with Trailing Disabled Radiobutton"),
                    trailingContentData = ListItemTrailingContentDataUi.RadioButton(
                        radioButtonData = RadioButtonDataUi(
                            isSelected = true,
                            enabled = false,
                        )
                    )
                ),
                modifier = modifier,
                onItemClick = {},
            )

            // ListItem with trailing enabled switch
            ListItem(
                item = ListItemDataUi(
                    itemId = "15",
                    mainContentData = ListItemMainContentDataUi.Text(text = "Item with Trailing Enabled Switch"),
                    trailingContentData = ListItemTrailingContentDataUi.Switch(
                        switchData = SwitchDataUi(
                            isChecked = true,
                        )
                    )
                ),
                modifier = modifier,
                onItemClick = {},
            )

            // ListItem with trailing disabled switch
            ListItem(
                item = ListItemDataUi(
                    itemId = "16",
                    mainContentData = ListItemMainContentDataUi.Text(text = "Item with Trailing Disabled Switch"),
                    trailingContentData = ListItemTrailingContentDataUi.Switch(
                        switchData = SwitchDataUi(
                            isChecked = true,
                            enabled = false,
                        )
                    )
                ),
                modifier = modifier,
                onItemClick = {},
            )

            // ListItem with all elements
            ListItem(
                item = ListItemDataUi(
                    itemId = "17",
                    mainContentData = ListItemMainContentDataUi.Text(text = "Full Item Example"),
                    overlineText = "Overline Text",
                    supportingText = "Supporting Text",
                    leadingContentData = ListItemLeadingContentDataUi.Icon(iconData = AppIcons.Add),
                    trailingContentData = ListItemTrailingContentDataUi.Icon(
                        iconData = AppIcons.KeyboardArrowDown,
                    )
                ),
                modifier = modifier,
                onItemClick = {},
            )
        }
    }
}