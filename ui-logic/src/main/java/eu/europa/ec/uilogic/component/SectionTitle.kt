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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.wrap.TextConfig
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
    )
) {
    WrapText(
        modifier = modifier,
        text = text,
        textConfig = textConfig,
    )
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