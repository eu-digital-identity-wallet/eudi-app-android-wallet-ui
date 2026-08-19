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

package eu.europa.ec.uilogic.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.theme.values.pending
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_MEDIUM
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.SwitchDataUi
import eu.europa.ec.uilogic.component.wrap.WrapCard
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapSwitch

/**
 * A warning banner the user must explicitly acknowledge via its switch: warning message on top,
 * the acknowledge row below. The switch state is only reported through [onAcknowledgeChange];
 * owning and resetting it is the caller's concern.
 */
@Composable
fun AcknowledgeWarningCard(
    modifier: Modifier = Modifier,
    warningText: String,
    acknowledgeText: String,
    isAcknowledged: Boolean,
    onAcknowledgeChange: (Boolean) -> Unit,
) {
    val contentColor = MaterialTheme.colorScheme.onPrimary
    val textStyle = MaterialTheme.typography.labelLarge.copy(
        color = contentColor,
    )

    WrapCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.pending,
            contentColor = contentColor,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical = SPACING_SMALL.dp
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SIZE_SMALL.dp)
            ) {
                WrapIcon(
                    iconData = AppIcons.Warning,
                    modifier = Modifier.size(SIZE_MEDIUM.dp),
                    customTint = contentColor,
                )
                Text(
                    text = warningText,
                    modifier = Modifier.fillMaxWidth(),
                    style = textStyle,
                    fontWeight = FontWeight.W600,
                )
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SPACING_SMALL.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp)
            ) {
                Text(
                    text = acknowledgeText,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = SPACING_LARGE.dp),
                    style = textStyle,
                    fontWeight = FontWeight.W500,
                )
                WrapSwitch(
                    switchData = SwitchDataUi(
                        isChecked = isAcknowledged,
                        enabled = true,
                    ),
                    onCheckedChange = onAcknowledgeChange,
                )
            }
        }
    }
}

@ThemeModePreviews
@Composable
private fun AcknowledgeWarningCardNotAcknowledgedPreview() {
    PreviewTheme {
        AcknowledgeWarningCard(
            modifier = Modifier.fillMaxWidth(),
            warningText = "Warning: The information registered about the entity could not " +
                    "be obtained. Review carefully before sharing your data.",
            acknowledgeText = "I understand the risks and agree to share my data",
            isAcknowledged = false,
            onAcknowledgeChange = {},
        )
    }
}

@ThemeModePreviews
@Composable
private fun AcknowledgeWarningCardAcknowledgedPreview() {
    PreviewTheme {
        AcknowledgeWarningCard(
            modifier = Modifier.fillMaxWidth(),
            warningText = "Warning: Some of the requested data are not registered with " +
                    "this interacting party. Review carefully before sharing your data.",
            acknowledgeText = "I understand the risks and agree to share my data",
            isAcknowledged = true,
            onAcknowledgeChange = {},
        )
    }
}