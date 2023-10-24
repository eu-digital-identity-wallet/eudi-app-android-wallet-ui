/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.uilogic.component.wrap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.theme.values.dividerDark
import eu.europa.ec.resourceslogic.theme.values.textDisabledDark
import eu.europa.ec.resourceslogic.theme.values.textPrimaryDark
import eu.europa.ec.uilogic.component.utils.ALPHA_DISABLED

private val buttonsHeight: Modifier = Modifier.height(48.dp)
private val buttonsShape: RoundedCornerShape = RoundedCornerShape(16.dp)
private val buttonsContentPadding: PaddingValues =
    PaddingValues(horizontal = 16.dp, vertical = 14.dp)

@Composable
fun WrapPrimaryButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        modifier = buttonsHeight.then(modifier),
        enabled = enabled,
        onClick = onClick,
        shape = buttonsShape,
        colors = ButtonDefaults.textButtonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.textPrimaryDark,
            disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = ALPHA_DISABLED),
            disabledContentColor = MaterialTheme.colorScheme.textDisabledDark,
        ),
        contentPadding = buttonsContentPadding,

        content = content
    )
}

@Composable
fun WrapSecondaryButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        modifier = buttonsHeight.then(modifier),
        enabled = enabled,
        onClick = onClick,
        shape = buttonsShape,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.textPrimaryDark,
            disabledContentColor = MaterialTheme.colorScheme.textDisabledDark,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.dividerDark,
        ),
        contentPadding = buttonsContentPadding,
        content = content
    )
}