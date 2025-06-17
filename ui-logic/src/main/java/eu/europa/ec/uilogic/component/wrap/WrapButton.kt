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
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_100
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE

enum class ButtonType {
    PRIMARY,
    SECONDARY,
}

private val buttonsShape: RoundedCornerShape = RoundedCornerShape(SIZE_100.dp)

private val buttonsContentPadding: PaddingValues = PaddingValues(
    vertical = 10.dp,
    horizontal = SPACING_LARGE.dp
)

data class ButtonConfig(
    val type: ButtonType,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
    val isWarning: Boolean = false,
    val shape: Shape = buttonsShape,
    val contentPadding: PaddingValues = buttonsContentPadding,
    val buttonColors: ButtonColors? = null,
)

@Composable
fun WrapButton(
    modifier: Modifier = Modifier,
    buttonConfig: ButtonConfig,
    content: @Composable RowScope.() -> Unit,
) {
    when (buttonConfig.type) {
        ButtonType.PRIMARY -> WrapPrimaryButton(
            modifier = modifier,
            buttonConfig = buttonConfig,
            content = content,
        )

        ButtonType.SECONDARY -> WrapSecondaryButton(
            modifier = modifier,
            buttonConfig = buttonConfig,
            content = content,
        )
    }
}

@Composable
private fun WrapPrimaryButton(
    modifier: Modifier = Modifier,
    buttonConfig: ButtonConfig,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = if (buttonConfig.isWarning) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        )
    } else {
        buttonConfig.buttonColors ?: ButtonDefaults.buttonColors()
    }

    Button(
        modifier = modifier,
        enabled = buttonConfig.enabled,
        onClick = buttonConfig.onClick,
        shape = buttonConfig.shape,
        colors = colors,
        contentPadding = buttonConfig.contentPadding,
        content = content
    )
}

@Composable
private fun WrapSecondaryButton(
    modifier: Modifier = Modifier,
    buttonConfig: ButtonConfig,
    content: @Composable RowScope.() -> Unit,
) {
    val borderColor = if (!buttonConfig.enabled) {
        MaterialTheme.colorScheme.onSurface.copy(
            alpha = 0.12f
        )
    } else {
        if (buttonConfig.isWarning) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }
    }

    val colors = if (buttonConfig.isWarning) {
        ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        )
    } else {
        buttonConfig.buttonColors ?: ButtonDefaults.outlinedButtonColors()
    }

    OutlinedButton(
        modifier = modifier,
        enabled = buttonConfig.enabled,
        onClick = buttonConfig.onClick,
        shape = buttonConfig.shape,
        colors = colors,
        border = BorderStroke(
            width = 1.dp,
            color = borderColor,
        ),
        contentPadding = buttonConfig.contentPadding,
        content = content
    )
}

@ThemeModePreviews
@Composable
private fun WrapPrimaryButtonEnabledPreview() {
    PreviewTheme {
        WrapButton(
            buttonConfig = ButtonConfig(
                type = ButtonType.PRIMARY,
                enabled = true,
                onClick = { }
            ),
        ) {
            Text("Enabled Primary Button")
        }
    }
}

@ThemeModePreviews
@Composable
private fun WrapPrimaryButtonDisabledPreview() {
    PreviewTheme {
        WrapButton(
            buttonConfig = ButtonConfig(
                type = ButtonType.PRIMARY,
                enabled = false,
                onClick = { }
            ),
        ) {
            Text("Disabled Primary Button")
        }
    }
}

@ThemeModePreviews
@Composable
private fun WrapPrimaryButtonEnabledWarningPreview() {
    PreviewTheme {
        WrapButton(
            buttonConfig = ButtonConfig(
                type = ButtonType.PRIMARY,
                enabled = true,
                isWarning = true,
                onClick = { }
            )
        ) {
            Text("Enabled Warning Primary Button")
        }
    }
}

@ThemeModePreviews
@Composable
private fun WrapPrimaryButtonDisabledWarningPreview() {
    PreviewTheme {
        WrapButton(
            buttonConfig = ButtonConfig(
                type = ButtonType.PRIMARY,
                enabled = false,
                isWarning = true,
                onClick = { }
            )
        ) {
            Text("Disabled Warning Primary Button")
        }
    }
}

@ThemeModePreviews
@Composable
private fun WrapSecondaryButtonEnabledPreview() {
    PreviewTheme {
        WrapButton(
            buttonConfig = ButtonConfig(
                type = ButtonType.SECONDARY,
                enabled = true,
                onClick = { }
            )
        ) {
            Text("Enabled Secondary Button")
        }
    }
}

@ThemeModePreviews
@Composable
private fun WrapSecondaryButtonDisabledPreview() {
    PreviewTheme {
        WrapButton(
            buttonConfig = ButtonConfig(
                type = ButtonType.SECONDARY,
                enabled = false,
                onClick = { }
            )
        ) {
            Text("Disabled Secondary Button")
        }
    }
}

@ThemeModePreviews
@Composable
private fun WrapSecondaryButtonEnabledWarningPreview() {
    PreviewTheme {
        WrapButton(
            buttonConfig = ButtonConfig(
                type = ButtonType.SECONDARY,
                enabled = true,
                isWarning = true,
                onClick = { }
            )
        ) {
            Text("Enabled Warning Secondary Button")
        }
    }
}

@ThemeModePreviews
@Composable
private fun WrapSecondaryButtonDisabledWarningPreview() {
    PreviewTheme {
        WrapButton(
            buttonConfig = ButtonConfig(
                type = ButtonType.SECONDARY,
                enabled = false,
                isWarning = true,
                onClick = { }
            )
        ) {
            Text("Disabled Warning Secondary Button")
        }
    }
}