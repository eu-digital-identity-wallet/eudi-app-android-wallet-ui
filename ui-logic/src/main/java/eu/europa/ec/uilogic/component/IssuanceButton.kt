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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.TextLengthPreviewProvider
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.HSpacer
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.wrap.WrapCard
import eu.europa.ec.uilogic.component.wrap.WrapIcon

data class IssuanceButtonData(
    val text: String,
    val icon: IconData
)

@Composable
fun IssuanceButton(
    modifier: Modifier = Modifier,
    data: IssuanceButtonData,
    enabled: Boolean,
    onClick: (() -> Unit),
) {
    WrapCard(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        throttleClicks = true,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    ) {
        Row(
            modifier = Modifier.padding(SPACING_MEDIUM.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            val iconsColor = MaterialTheme.colorScheme.primary

            val textColor = MaterialTheme.colorScheme.onSurface.also {
                if (!enabled) {
                    it.copy(alpha = ALPHA_DISABLED)
                }
            }

            WrapIcon(
                modifier = Modifier
                    .width(40.dp)
                    .height(30.dp),
                iconData = data.icon,
                customTint = iconsColor,
                enabled = enabled,
            )

            HSpacer.Medium()

            Text(
                modifier = Modifier.weight(1f),
                text = data.text,
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )

            WrapIcon(
                iconData = AppIcons.Add,
                customTint = iconsColor,
                enabled = enabled,
            )
        }
    }
}

@ThemeModePreviews
@Composable
private fun IssuanceButtonEnabledPreview(
    @PreviewParameter(TextLengthPreviewProvider::class) text: String
) {
    PreviewTheme {
        IssuanceButton(
            modifier = Modifier.fillMaxWidth(),
            data = IssuanceButtonData(
                text = text,
                icon = AppIcons.Id
            ),
            enabled = true,
            onClick = {}
        )
    }
}

@ThemeModePreviews
@Composable
private fun IssuanceButtonDisabledPreview(
    @PreviewParameter(TextLengthPreviewProvider::class) text: String
) {
    PreviewTheme {
        IssuanceButton(
            modifier = Modifier.fillMaxWidth(),
            data = IssuanceButtonData(
                text = text,
                icon = AppIcons.Id
            ),
            enabled = false,
            onClick = {}
        )
    }
}