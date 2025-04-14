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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.IconData
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_MEDIUM
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL

@Composable
fun WrapChip(
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    leadingIcon: IconData? = null,
    trailingIcon: IconData? = null,
    shape: Shape = RoundedCornerShape(SIZE_SMALL.dp),
    colors: SelectableChipColors? = null,
    border: BorderStroke? = InputChipDefaults.inputChipBorder(
        enabled = enabled,
        selected = selected,
        borderColor = MaterialTheme.colorScheme.primary,
        borderWidth = 1.dp
    ),
) {
    val iconsSize = SIZE_MEDIUM.dp

    InputChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = if (leadingIcon != null) {
            @Composable {
                WrapIcon(
                    iconData = leadingIcon,
                    modifier = Modifier.size(iconsSize),
                )
            }
        } else {
            null
        },
        trailingIcon = if (trailingIcon != null) {
            @Composable {
                WrapIcon(
                    iconData = trailingIcon,
                    modifier = Modifier.size(iconsSize),
                )
            }
        } else {
            null
        },
        shape = shape,
        colors = colors ?: InputChipDefaults.inputChipColors(
            labelColor = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            trailingIconColor = MaterialTheme.colorScheme.primary,
            leadingIconColor = MaterialTheme.colorScheme.primary
        ),
        border = border
    )
}

@ThemeModePreviews
@Composable
private fun WrapChipPlainPreview() {
    PreviewTheme {
        WrapChip(
            modifier = Modifier.wrapContentWidth(),
            label = {
                Text(text = "Label text")
            },
        )
    }
}

@ThemeModePreviews
@Composable
private fun WrapChipWithTrailingIconPreview() {
    PreviewTheme {
        WrapChip(
            modifier = Modifier.wrapContentWidth(),
            label = {
                Text(text = "Label text")
            },
            trailingIcon = AppIcons.Close
        )
    }
}

@ThemeModePreviews
@Composable
private fun WrapChipWithLeadingIconPreview() {
    PreviewTheme {
        WrapChip(
            modifier = Modifier.wrapContentWidth(),
            label = {
                Text(text = "Label text")
            },
            leadingIcon = AppIcons.Close
        )
    }
}

@ThemeModePreviews
@Composable
private fun WrapChipFullPreview() {
    PreviewTheme {
        WrapChip(
            modifier = Modifier.wrapContentWidth(),
            label = {
                Text(text = "Label text")
            },
            leadingIcon = AppIcons.Close,
            trailingIcon = AppIcons.Close,
        )
    }
}