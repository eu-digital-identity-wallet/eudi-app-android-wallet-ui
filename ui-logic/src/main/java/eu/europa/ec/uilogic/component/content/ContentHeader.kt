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

package eu.europa.ec.uilogic.component.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.AppIconAndText
import eu.europa.ec.uilogic.component.AppIconAndTextDataUi
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.RelyingParty
import eu.europa.ec.uilogic.component.RelyingPartyDataUi
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.TextLengthPreviewProvider
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.TextConfig
import eu.europa.ec.uilogic.component.wrap.WrapText

/**
 * Data class representing the configuration for a content header.
 * This header typically displays information like app icon, name, description,
 * and potentially relying party details.
 *
 * @property appIconAndTextData Data for displaying the app icon and text.
 * @property description A descriptive text for the content.
 * @property descriptionTextConfig Configuration for the appearance of the description text.
 * @property mainText The main title or heading text.
 * @property mainTextConfig Configuration for the appearance of the main text.
 * @property relyingPartyData Data for displaying information about the relying party, if applicable.
 */
data class ContentHeaderConfig(
    val appIconAndTextData: AppIconAndTextDataUi = AppIconAndTextDataUi(),
    val description: String?,
    val descriptionTextConfig: TextConfig? = null,
    val mainText: String? = null,
    val mainTextConfig: TextConfig? = null,
    val relyingPartyData: RelyingPartyDataUi? = null,
)

/**
 * Composable function that displays the content header for the screen.
 *
 * This function displays the app icon and text, description, main text, and relying party information
 * based on the provided [ContentHeaderConfig].
 *
 * @param modifier Modifier used to adjust the layout of the header.
 * @param config Configuration object containing data for the header content.
 */
@Composable
fun ContentHeader(
    modifier: Modifier = Modifier,
    config: ContentHeaderConfig,
) {
    val commonTextAlign = TextAlign.Center

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        with(config) {
            // App icon and text section.
            AppIconAndText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SPACING_LARGE.dp),
                appIconAndTextData = appIconAndTextData,
            )

            // Description section.
            description?.let { safeDescription ->
                WrapText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SPACING_SMALL.dp),
                    text = safeDescription,
                    textConfig = descriptionTextConfig ?: TextConfig(
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = commonTextAlign,
                        maxLines = 3,
                    )
                )
            }

            // Main text section.
            mainText?.let { safeMainText ->
                WrapText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SPACING_MEDIUM.dp),
                    text = safeMainText,
                    textConfig = mainTextConfig ?: TextConfig(
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.W600
                        ),
                        textAlign = commonTextAlign,
                    )
                )
            }

            // Relying party section.
            relyingPartyData?.let { safeRelyingPartyData ->
                RelyingParty(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SPACING_SMALL.dp),
                    relyingPartyData = safeRelyingPartyData,
                )
            }
        }
    }
}

@ThemeModePreviews
@Composable
private fun ContentHeaderPreview(
    @PreviewParameter(TextLengthPreviewProvider::class) text: String
) {
    PreviewTheme {
        ContentHeader(
            config = ContentHeaderConfig(
                appIconAndTextData = AppIconAndTextDataUi(
                    appIcon = AppIcons.LogoPlain,
                    appText = AppIcons.LogoText,
                ),
                description = "Description: $text",
                mainText = "Title: $text",
                relyingPartyData = RelyingPartyDataUi(
                    isVerified = true,
                    name = "Relying Party Name: $text",
                    description = "Relying Party Description: $text",
                )
            )
        )
    }
}