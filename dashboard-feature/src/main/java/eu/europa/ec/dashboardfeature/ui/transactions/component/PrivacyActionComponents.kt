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

package eu.europa.ec.dashboardfeature.ui.transactions.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.WrapCard

@Composable
internal fun PrivacyActionInfoCard(
    modifier: Modifier,
    label: String?,
    text: String,
) {
    WrapCard(
        modifier = modifier,
        shape = RoundedCornerShape(SIZE_SMALL.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SPACING_MEDIUM.dp),
            verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp),
        ) {
            label?.let { availableLabel ->
                Text(
                    text = availableLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
internal fun PrivacyActionExplanation(
    modifier: Modifier,
    responsibility: String,
    followUp: String,
) {
    val responsibilityText = remember(responsibility) {
        AnnotatedString.fromHtml(responsibility)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_LARGE.dp),
    ) {
        Text(
            text = responsibilityText,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = followUp,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@ThemeModePreviews
@Composable
private fun PrivacyActionInfoCardWithLabelPreview() {
    PreviewTheme {
        PrivacyActionInfoCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SPACING_MEDIUM.dp),
            label = "Responsible data protection authority",
            text = "Example data protection authority",
        )
    }
}

@ThemeModePreviews
@Composable
private fun PrivacyActionInfoCardWithoutLabelPreview() {
    PreviewTheme {
        PrivacyActionInfoCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SPACING_MEDIUM.dp),
            label = null,
            text = "This will open your email application to request data deletion from TravelBook.",
        )
    }
}

@ThemeModePreviews
@Composable
private fun PrivacyActionExplanationPreview() {
    PreviewTheme {
        PrivacyActionExplanation(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SPACING_MEDIUM.dp),
            responsibility = "<b>The wallet doesn't submit or track this report.</b> Contact the authority directly.",
            followUp = "Any updates will come from the authority, not the wallet.",
        )
    }
}