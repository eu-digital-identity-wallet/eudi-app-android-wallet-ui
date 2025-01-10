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

package eu.europa.ec.dashboardfeature.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.wrap.WrapIcon

@Composable
fun FiltersSearchBar(
    placeholder: String,
    onValueChange: (String) -> Unit,
    onFilterClick: () -> Unit,
) {
    var value by remember { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SPACING_MEDIUM.dp, bottom = SPACING_MEDIUM.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = value,
            maxLines = 1,
            onValueChange = {
                value = it
                onValueChange(it)
            },
            placeholder = { Text(placeholder) },
            leadingIcon = {
                WrapIcon(
                    iconData = AppIcons.Search,
                    customTint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            shape = MaterialTheme.shapes.extraLarge,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedBorderColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )

        WrapIcon(
            modifier = Modifier
                .clickable { onFilterClick() }
                .padding(all = SPACING_MEDIUM.dp),
            iconData = AppIcons.Filters,
            customTint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
@ThemeModePreviews
private fun FiltersSearchBarPreview() {
    FiltersSearchBar(
        placeholder = "Search documents", onValueChange = { }, onFilterClick = {}
    )
}