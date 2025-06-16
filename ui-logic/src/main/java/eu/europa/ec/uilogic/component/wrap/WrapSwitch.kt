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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_SMALL

data class SwitchDataUi(
    val isChecked: Boolean,
    val enabled: Boolean = true,
)

@Composable
fun WrapSwitch(
    switchData: SwitchDataUi,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    colors: SwitchColors = SwitchDefaults.colors(),
) {
    Switch(
        modifier = modifier,
        checked = switchData.isChecked,
        enabled = switchData.enabled,
        onCheckedChange = onCheckedChange,
        colors = colors,
    )
}

@ThemeModePreviews
@Composable
private fun WrapSwitchPreview() {
    PreviewTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(SPACING_EXTRA_SMALL.dp)
        ) {
            WrapSwitch(
                switchData = SwitchDataUi(
                    enabled = true,
                    isChecked = true,
                ),
                onCheckedChange = {},
            )
            WrapSwitch(
                switchData = SwitchDataUi(
                    enabled = true,
                    isChecked = false,
                ),
                onCheckedChange = {},
            )
            WrapSwitch(
                switchData = SwitchDataUi(
                    enabled = false,
                    isChecked = true,
                ),
                onCheckedChange = {},
            )
            WrapSwitch(
                switchData = SwitchDataUi(
                    enabled = false,
                    isChecked = false,
                ),
                onCheckedChange = {},
            )
        }
    }
}