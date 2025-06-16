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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.theme.values.success
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.TextLengthPreviewProvider
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.TextConfig
import eu.europa.ec.uilogic.component.wrap.WrapAsyncImage
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapText
import java.net.URI

/**
 * Data class representing information about a Relying Party.
 *
 * @property logo An optional [IconDataUi] representing the logo of the Relying Party.
 * @property isVerified A boolean indicating whether the Relying Party is verified.
 * @property name The name of the Relying Party.
 * @property nameTextConfig Optional [TextConfig] for styling the name text.
 * @property description An optional description of the Relying Party.
 * @property descriptionTextConfig Optional [TextConfig] for styling the description text.
 */
data class RelyingPartyDataUi(
    val logo: URI? = null,
    val isVerified: Boolean,
    val name: String,
    val nameTextConfig: TextConfig? = null,
    val description: String? = null,
    val descriptionTextConfig: TextConfig? = null,
)

@Composable
fun RelyingParty(
    modifier: Modifier = Modifier,
    relyingPartyData: RelyingPartyDataUi,
) {
    val commonTextAlign = TextAlign.Center

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        with(relyingPartyData) {
            logo?.let { safeLogo ->
                WrapAsyncImage(
                    modifier = Modifier.fillMaxWidth(0.5f),
                    source = safeLogo.toString(),
                    contentScale = ContentScale.FillWidth
                )
                VSpacer.Small()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isVerified) {
                    WrapIcon(
                        modifier = Modifier.size(20.dp),
                        iconData = AppIcons.Verified,
                        customTint = MaterialTheme.colorScheme.success,
                    )
                }
                WrapText(
                    modifier = Modifier.wrapContentWidth(),
                    text = name,
                    textConfig = nameTextConfig ?: TextConfig(
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = commonTextAlign,
                    )
                )
            }

            description?.let { safeDescription ->
                WrapText(
                    modifier = Modifier.fillMaxWidth(),
                    text = safeDescription,
                    textConfig = descriptionTextConfig ?: TextConfig(
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = commonTextAlign,
                    )
                )
            }
        }
    }
}

@ThemeModePreviews
@Composable
private fun RelyingPartyPreview(
    @PreviewParameter(TextLengthPreviewProvider::class) text: String
) {
    PreviewTheme {
        RelyingParty(
            relyingPartyData = RelyingPartyDataUi(
                isVerified = true,
                name = "Relying Party Name: $text",
                description = "Relying Party Description: $text",
            )
        )
    }
}