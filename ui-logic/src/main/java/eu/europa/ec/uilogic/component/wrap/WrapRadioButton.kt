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

import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews

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

data class RadioButtonDataUi(
    val isSelected: Boolean,
    val enabled: Boolean = true,
    val onCheckedChange: (() -> Unit)? = null,
)

@Composable
fun WrapRadioButton(
    radioButtonData: RadioButtonDataUi,
    modifier: Modifier = Modifier,
) {
    // This is needed, otherwise M3 adds unwanted space around CheckBoxes.
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides Dp.Unspecified
    ) {
        RadioButton(
            modifier = modifier,
            selected = radioButtonData.isSelected,
            onClick = radioButtonData.onCheckedChange,
            enabled = radioButtonData.enabled,
            colors = RadioButtonDefaults.colors(
                unselectedColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@ThemeModePreviews
@Composable
private fun WrapRadioButtonPreview() {
    var isChecked by remember {
        mutableStateOf(true)
    }

    val radioButtonData = RadioButtonDataUi(
        isSelected = isChecked,
        enabled = true,
        onCheckedChange = {
            isChecked = !isChecked
        }
    )

    PreviewTheme {
        WrapRadioButton(radioButtonData = radioButtonData)
    }
}