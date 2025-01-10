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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.IconData
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.HSpacer
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL

data class ActionCardConfig(
    val title: String,
    val icon: IconData,
    val primaryButtonText: String,
    val secondaryButtonText: String,
)

@Composable
fun WrapActionCard(
    modifier: Modifier = Modifier,
    config: ActionCardConfig,
    onActionClick: () -> Unit = {},
    onLearnMoreClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SPACING_MEDIUM.dp),
            verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = SPACING_MEDIUM.dp),
                    text = config.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                WrapImage(
                    modifier = Modifier
                        .width(88.dp)
                        .height(97.dp),
                    iconData = config.icon
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp)
            ) {
                WrapButton(
                    modifier = Modifier.fillMaxWidth(),
                    buttonConfig = ButtonConfig(
                        type = ButtonType.PRIMARY,
                        onClick = onActionClick
                    ),
                ) {
                    Text(config.primaryButtonText)
                }

                WrapButton(
                    modifier = Modifier.fillMaxWidth(),
                    buttonConfig = ButtonConfig(
                        type = ButtonType.PRIMARY,
                        isWithoutContainerBackground = true,
                        onClick = onLearnMoreClick
                    )
                ) {
                    WrapIcon(
                        modifier = Modifier.size(SPACING_MEDIUM.dp),
                        iconData = AppIcons.Info,
                        customTint = MaterialTheme.colorScheme.primary
                    )

                    HSpacer.Small()

                    Text(
                        text = config.secondaryButtonText,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@ThemeModePreviews
@Composable
private fun WrapActionCardPreview() {
    PreviewTheme {
        WrapActionCard(
            config = ActionCardConfig(
                title = "Authenticate, authorise transactions and share your digital documents in person or online.",
                icon = AppIcons.WalletActivated,
                primaryButtonText = "Authenticate",
                secondaryButtonText = "Learn more",
            )
        )
    }
}