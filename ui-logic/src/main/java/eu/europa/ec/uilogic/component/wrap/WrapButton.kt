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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_100
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE

private val buttonsShape: RoundedCornerShape = RoundedCornerShape(SIZE_100.dp)

private val buttonsContentPadding: PaddingValues = PaddingValues(
    vertical = 10.dp,
    horizontal = SPACING_LARGE.dp
)

@Composable
fun WrapPrimaryButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        shape = buttonsShape,
        colors = ButtonDefaults.textButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.12f
            )
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
    val borderColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(
            alpha = 0.12f
        )
    }

    OutlinedButton(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        shape = buttonsShape,
        colors = ButtonDefaults.outlinedButtonColors(),
        border = BorderStroke(
            width = 1.dp,
            color = borderColor,
        ),
        contentPadding = buttonsContentPadding,
        content = content
    )
}

@ThemeModePreviews
@Composable
private fun WrapPrimaryButtonEnabledPreview() {
    PreviewTheme {
        WrapPrimaryButton(
            enabled = true,
            onClick = { }
        ) {
            Text("Enabled Primary Button")
        }
    }
}

@ThemeModePreviews
@Composable
private fun WrapPrimaryButtonDisabledPreview() {
    PreviewTheme {
        WrapPrimaryButton(
            enabled = false,
            onClick = { }
        ) {
            Text("Disabled Primary Button")
        }
    }
}

@ThemeModePreviews
@Composable
private fun WrapSecondaryButtonEnabledPreview() {
    PreviewTheme {
        WrapSecondaryButton(
            enabled = true,
            onClick = { }
        ) {
            Text("Enabled Secondary Button")
        }
    }
}

@ThemeModePreviews
@Composable
private fun WrapSecondaryButtonDisabledPreview() {
    PreviewTheme {
        WrapSecondaryButton(
            enabled = false,
            onClick = { }
        ) {
            Text("Disabled Secondary Button")
        }
    }
}