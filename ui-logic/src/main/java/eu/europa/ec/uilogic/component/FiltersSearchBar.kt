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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapIconButton

@Composable
fun FiltersSearchBar(
    text: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onClearClick: () -> Unit,
    isFilteringActive: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = text,
            maxLines = 1,
            singleLine = true,
            onValueChange = {
                onValueChange(it)
            },
            placeholder = { Text(placeholder) },
            leadingIcon = {
                WrapIcon(
                    iconData = AppIcons.Search,
                    customTint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = if (text.isNotEmpty()) {
                {
                    WrapIconButton(
                        iconData = AppIcons.Close,
                        onClick = {
                            onClearClick()
                            focusManager.clearFocus()
                        }
                    )
                }
            } else null,
            shape = MaterialTheme.shapes.extraLarge,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            )
        )

        Box {
            WrapIconButton(
                iconData = AppIcons.Filters,
                customTint = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                onFilterClick()
            }
            if (isFilteringActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = SPACING_SMALL.dp, end = SPACING_SMALL.dp)
                        .size((SIZE_SMALL * 1.5).dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
    }
}

@Composable
@ThemeModePreviews
private fun FiltersSearchBarPreview() {
    PreviewTheme {
        FiltersSearchBar(
            text = "",
            placeholder = "Search documents",
            onValueChange = { },
            onFilterClick = {},
            onClearClick = {},
            isFilteringActive = true,
        )
    }
}