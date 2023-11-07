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

package eu.europa.ec.uilogic.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.theme.values.backgroundPaper
import eu.europa.ec.resourceslogic.theme.values.textPrimaryDark
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
    onClick: (() -> Unit),
) {
    WrapCard(
        modifier = modifier,
        onClick = onClick,
        throttleClicks = true,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.backgroundPaper,
        )
    ) {
        Row(
            modifier = Modifier.padding(SPACING_MEDIUM.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            WrapIcon(
                iconData = data.icon,
                customTint = MaterialTheme.colorScheme.primary
            )

            HSpacer.Medium()

            Text(
                modifier = Modifier.weight(1f),
                text = data.text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.textPrimaryDark
            )

            WrapIcon(
                iconData = AppIcons.Add,
                customTint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@ThemeModePreviews
@Composable
private fun IssuanceButtonsPreview(
    @PreviewParameter(TextLengthPreviewProvider::class) text: String
) {
    PreviewTheme {
        IssuanceButton(
            modifier = Modifier.fillMaxWidth(),
            data = IssuanceButtonData(
                text = text,
                icon = AppIcons.Id
            ), {}
        )
    }
}