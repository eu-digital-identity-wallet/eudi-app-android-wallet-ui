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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.TextLengthPreviewProvider
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews

@Composable
fun WrapSearchBar(
    modifier: Modifier = Modifier,
    searchText: String,
    placeholder: String,
    onSearchTextChanged: (String) -> Unit,
    colors: TextFieldColors? = null,
    maxLines: Int = 1,
) {
    WrapTextField(
        value = searchText,
        onValueChange = onSearchTextChanged,
        modifier = modifier,
        leadingIcon = {
            WrapIcon(
                iconData = AppIcons.Search,
            )
        },
        placeholder = {
            Text(text = placeholder)
        },
        colors = colors ?: OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.secondary,
        ),
        maxLines = maxLines,
    )
}

@ThemeModePreviews
@Composable
private fun WrapSearchBarWithTextPreview(
    @PreviewParameter(TextLengthPreviewProvider::class) text: String
) {
    PreviewTheme {
        WrapSearchBar(
            modifier = Modifier.fillMaxWidth(),
            searchText = text,
            placeholder = "Search Documents",
            onSearchTextChanged = {}
        )
    }
}

@ThemeModePreviews
@Composable
private fun WrapSearchBarWithNoTextPreview() {
    PreviewTheme {
        WrapSearchBar(
            modifier = Modifier.fillMaxWidth(),
            searchText = "",
            placeholder = "Search Documents",
            onSearchTextChanged = {}
        )
    }
}