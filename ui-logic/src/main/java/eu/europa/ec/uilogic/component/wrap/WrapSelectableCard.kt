/*
 * Copyright (c) 2026 European Commission
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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemLeadingContentDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL

@Stable
object WrapSelectableCardDefaults {

    val CORNER_RADIUS = 12.dp

    val SELECTED_BORDER_WIDTH = 2.dp

    val MAIN_CONTENT_VERTICAL_PADDING = 12.dp
}

/**
 * A selectable card: one option in a single-choice (radio) group. Renders a leading radio and
 * [title] above the caller's [content].
 *
 * Click model: while **unselected** a full-card layer above the content intercepts every tap and
 * invokes [onSelected], so the content's children aren't individually tappable; once **selected**
 * that layer drops its click handler and taps fall through to the content. The layer is always
 * composed (only its click handler is gated) so the selecting tap's ripple animates to completion
 * instead of being disposed mid-frame.
 */
@Composable
fun WrapSelectableCard(
    modifier: Modifier = Modifier,
    title: String,
    isSelected: Boolean,
    onSelected: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) WrapSelectableCardDefaults.SELECTED_BORDER_WIDTH else 0.dp,
        label = "wrapSelectableCardBorderWidth",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        label = "wrapSelectableCardBorderColor",
    )

    val selectInteractionSource = remember { MutableInteractionSource() }

    WrapCard(
        modifier = modifier,
        shape = RoundedCornerShape(WrapSelectableCardDefaults.CORNER_RADIUS),
        border = BorderStroke(
            width = borderWidth,
            color = borderColor,
        ),
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SPACING_SMALL.dp),
                verticalArrangement = Arrangement.spacedBy(SPACING_EXTRA_SMALL.dp),
            ) {
                WrapListItem(
                    item = ListItemDataUi(
                        itemId = title,
                        mainContentData = ListItemMainContentDataUi.Text(text = title),
                        leadingContentData = ListItemLeadingContentDataUi.RadioButton(
                            radioButtonData = RadioButtonDataUi(
                                isSelected = isSelected,
                                onCheckedChange = null,
                            ),
                        ),
                    ),
                    onItemClick = null,
                    mainContentVerticalPadding = WrapSelectableCardDefaults.MAIN_CONTENT_VERTICAL_PADDING
                )
                content()
            }

            // the full-card select layer
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(WrapSelectableCardDefaults.CORNER_RADIUS))
                    .indication(
                        interactionSource = selectInteractionSource,
                        indication = ripple(),
                    )
                    .then(
                        other = if (!isSelected) {
                            Modifier.clickable(
                                interactionSource = selectInteractionSource,
                                indication = null,
                                onClick = onSelected,
                            )
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}

@ThemeModePreviews
@Composable
private fun WrapSelectableCardPreview() {
    PreviewTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp),
        ) {
            WrapSelectableCard(
                title = "Option 1 of 2",
                isSelected = true,
                onSelected = {},
            ) {
                WrapListItem(
                    item = ListItemDataUi(
                        itemId = "0",
                        mainContentData = ListItemMainContentDataUi.Text(
                            text = "mDL (MSO mDoc)"
                        ),
                        supportingText = "View details"
                    ),
                    onItemClick = null,
                )
            }
            WrapSelectableCard(
                title = "Option 2 of 2",
                isSelected = false,
                onSelected = {},
            ) {
                WrapListItem(
                    item = ListItemDataUi(
                        itemId = "1",
                        mainContentData = ListItemMainContentDataUi.Text(
                            text = "PhotoID (MSO mDoc)"
                        ),
                        supportingText = "View details"
                    ),
                    onItemClick = null,
                )
            }
        }
    }
}