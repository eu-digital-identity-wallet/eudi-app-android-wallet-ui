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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.TextConfig
import eu.europa.ec.uilogic.component.wrap.WrapIconButton
import eu.europa.ec.uilogic.component.wrap.WrapText

/**
 * A composable function that displays a section title with customizable styling.
 *
 * @param modifier Modifier used to adjust the layout or appearance of the title.
 * @param text The text to display as the section title.
 * @param textConfig Optional [TextConfig] for customizing the typography
 * and color of the section title. Defaults to a style labelSmall with an
 * onSurfaceVariant color.
 */
@Composable
fun SectionTitle(
    modifier: Modifier,
    text: String,
    textConfig: TextConfig = TextConfig(
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    ),
    icon: IconDataUi? = null,
    throttleIconClicks: Boolean = true,
    iconEnabled: Boolean = true,
    onIconClick: () -> Unit = {},
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WrapText(
            modifier = Modifier.padding(end = SPACING_SMALL.dp),
            text = text,
            textConfig = textConfig,
        )

        icon?.let { safeIcon ->
            WrapIconButton(
                iconData = safeIcon,
                enabled = iconEnabled,
                throttleClicks = throttleIconClicks,
                onClick = onIconClick
            )
        }
    }
}

@ThemeModePreviews
@Composable
private fun SectionTitlePreview() {
    PreviewTheme {
        SectionTitle(
            modifier = Modifier,
            text = "DOCUMENT DETAILS"
        )
    }
}

@ThemeModePreviews
@Composable
private fun SectionTitleWithIconPreview() {
    PreviewTheme {
        SectionTitle(
            modifier = Modifier,
            text = "DOCUMENT DETAILS",
            icon = AppIcons.Visibility,
        )
    }
}