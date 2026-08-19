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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_SMALL

data class CheckboxDataUi(
    val isChecked: Boolean,
    val enabled: Boolean = true,
    val onCheckedChange: ((Boolean) -> Unit)? = null,
)

@Composable
fun WrapCheckbox(
    checkboxData: CheckboxDataUi,
    modifier: Modifier = Modifier,
    checkboxColors: CheckboxColors? = null,
) {
    // This is needed, otherwise M3 adds unwanted space around CheckBoxes.
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides Dp.Unspecified
    ) {
        Checkbox(
            checked = checkboxData.isChecked,
            onCheckedChange = checkboxData.onCheckedChange,
            modifier = modifier,
            enabled = checkboxData.enabled,
            colors = checkboxColors ?: CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@ThemeModePreviews
@Composable
private fun WrapCheckBoxPreview() {
    PreviewTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(SPACING_EXTRA_SMALL.dp)
        ) {
            WrapCheckbox(
                checkboxData = CheckboxDataUi(
                    isChecked = true,
                    enabled = true,
                    onCheckedChange = {},
                )
            )
            WrapCheckbox(
                checkboxData = CheckboxDataUi(
                    isChecked = false,
                    enabled = true,
                    onCheckedChange = {},
                )
            )
            WrapCheckbox(
                checkboxData = CheckboxDataUi(
                    isChecked = true,
                    enabled = false,
                    onCheckedChange = {},
                )
            )
            WrapCheckbox(
                checkboxData = CheckboxDataUi(
                    isChecked = false,
                    enabled = false,
                    onCheckedChange = {},
                )
            )
        }
    }
}