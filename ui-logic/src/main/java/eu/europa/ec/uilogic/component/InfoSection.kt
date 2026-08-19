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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.TextConfig
import eu.europa.ec.uilogic.component.wrap.TextStyleKey
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapText
import eu.europa.ec.uilogic.extension.clickableNoRipple

/** A titled text section (e.g. "Intended use" + its description). */
@Composable
fun InfoSection(
    modifier: Modifier = Modifier,
    title: String,
    body: String,
) {
    Column(
        modifier = modifier,
    ) {
        InfoSectionTitle(title = title)
        WrapText(
            modifier = Modifier.fillMaxWidth(),
            text = body,
            textConfig = TextConfig(
                styleKey = TextStyleKey.BodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = Int.MAX_VALUE,
            ),
        )
    }
}

/**
 * A titled link section (e.g. "Privacy policy" + a tappable URL with an external-link icon).
 * The tap is only reported through [onLinkClick]; opening the link is the caller's concern.
 */
@Composable
fun InfoLinkSection(
    modifier: Modifier = Modifier,
    title: String,
    linkText: String,
    onLinkClick: () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        InfoSectionTitle(title = title)
        Row(
            modifier = Modifier.clickableNoRipple { onLinkClick() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val contentColor = MaterialTheme.colorScheme.primary
            WrapText(
                text = linkText,
                textConfig = TextConfig(
                    styleKey = TextStyleKey.BodyMedium,
                    color = contentColor,
                ),
            )
            WrapIcon(
                modifier = Modifier
                    .padding(
                        horizontal = SPACING_SMALL.dp,
                        vertical = 2.dp
                    )
                    .size(16.dp),
                iconData = AppIcons.OpenNew,
                customTint = contentColor,
            )
        }
    }
}

@Composable
private fun InfoSectionTitle(
    title: String,
) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = title,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.W600,
        style = MaterialTheme.typography.labelLarge,
    )
}

@ThemeModePreviews
@Composable
private fun InfoSectionPreview() {
    PreviewTheme {
        InfoSection(
            modifier = Modifier.fillMaxWidth(),
            title = "Intended use",
            body = "We will use your identity and age to verify you for a new current account.",
        )
    }
}

@ThemeModePreviews
@Composable
private fun InfoLinkSectionPreview() {
    PreviewTheme {
        InfoLinkSection(
            modifier = Modifier.fillMaxWidth(),
            title = "Privacy policy",
            linkText = "https://nordicbank.example/privacy",
            onLinkClick = {},
        )
    }
}