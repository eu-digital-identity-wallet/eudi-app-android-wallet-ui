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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.success
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.TextLengthPreviewProvider
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.ICON_SIZE_40
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.TextConfig
import eu.europa.ec.uilogic.component.wrap.TextStyleKey
import eu.europa.ec.uilogic.component.wrap.WrapAsyncImage
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapText
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.net.URI

/**
 * Data class representing information about a Relying Party.
 *
 * @property logo the party's logo, when a source exists.
 * @property isVerified whether the verified badge is shown next to [name].
 * @property name the party's display name.
 * @property uniqueId the party's registered identifier, rendered as an "(ID: …)" line;
 * hidden when null.
 * @property description an optional supporting line under the identity.
 */
@Serializable
data class RelyingPartyDataUi(
    @Contextual val logo: URI?,
    val isVerified: Boolean,
    val name: String,
    val uniqueId: String?,
    val description: String?,
)

/**
 * How [RelyingParty] arranges the logo against the identity block.
 */
enum class RelyingPartyLayout {

    /** Logo beside the identity block, everything start-aligned. */
    InlineStart,

    /** Logo stacked above the identity block, everything centred. */
    StackedCentered,
}

/**
 * The reusable party-identity block: logo, verified badge + name, "(ID: …)" line, optional
 * description.
 */
@Composable
fun RelyingParty(
    modifier: Modifier = Modifier,
    relyingPartyData: RelyingPartyDataUi,
    layout: RelyingPartyLayout = RelyingPartyLayout.InlineStart,
) {
    when (layout) {
        RelyingPartyLayout.InlineStart -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RelyingPartyIdentity(
                modifier = Modifier.weight(1f),
                relyingPartyData = relyingPartyData,
                horizontalAlignment = Alignment.Start,
                textAlign = TextAlign.Start,
            )

            relyingPartyData.logo?.let { safeLogo ->
                RelyingPartyLogo(
                    modifier = Modifier
                        .padding(all = SPACING_SMALL.dp)
                        .size(ICON_SIZE_40.dp),
                    logo = safeLogo,
                )
            }
        }

        RelyingPartyLayout.StackedCentered -> Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            relyingPartyData.logo?.let { safeLogo ->
                RelyingPartyLogo(
                    modifier = Modifier.fillMaxWidth(0.5f),
                    logo = safeLogo,
                    contentScale = ContentScale.FillWidth,
                )
                VSpacer.Small()
            }

            RelyingPartyIdentity(
                modifier = Modifier.fillMaxWidth(),
                relyingPartyData = relyingPartyData,
                horizontalAlignment = Alignment.CenterHorizontally,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RelyingPartyLogo(
    modifier: Modifier,
    logo: URI,
    contentScale: ContentScale = ContentScale.Fit,
) {
    WrapAsyncImage(
        modifier = modifier,
        source = logo.toString(),
        contentScale = contentScale,
        error = AppIcons.Id,
    )
}

@Composable
private fun RelyingPartyIdentity(
    modifier: Modifier = Modifier,
    relyingPartyData: RelyingPartyDataUi,
    horizontalAlignment: Alignment.Horizontal,
    textAlign: TextAlign,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
    ) {
        with(relyingPartyData) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isVerified) {
                    WrapIcon(
                        modifier = Modifier
                            .padding(end = SPACING_EXTRA_SMALL.dp)
                            .size(18.dp),
                        iconData = AppIcons.Verified,
                        customTint = MaterialTheme.colorScheme.success,
                    )
                }
                WrapText(
                    text = name,
                    textConfig = TextConfig(
                        styleKey = TextStyleKey.BodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = textAlign,
                    ),
                )
            }

            uniqueId?.let { safeUniqueId ->
                WrapText(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(
                        R.string.request_relying_party_id_format,
                        safeUniqueId,
                    ),
                    textConfig = TextConfig(
                        styleKey = TextStyleKey.BodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = textAlign,
                    ),
                )
            }

            description?.let { safeDescription ->
                WrapText(
                    modifier = Modifier.fillMaxWidth(),
                    text = safeDescription,
                    textConfig = TextConfig(
                        styleKey = TextStyleKey.BodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = textAlign,
                    ),
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(SPACING_EXTRA_SMALL.dp),
            relyingPartyData = RelyingPartyDataUi(
                logo = null,
                isVerified = true,
                name = "NordicBank A/S: $text",
                uniqueId = "rp:nordicbank:prod",
                description = null,
            ),
        )
    }
}

@ThemeModePreviews
@Composable
private fun RelyingPartyNotVerifiedWithDescriptionPreview() {
    PreviewTheme {
        RelyingParty(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SPACING_EXTRA_SMALL.dp),
            relyingPartyData = RelyingPartyDataUi(
                logo = null,
                isVerified = false,
                name = "ACME A/S",
                uniqueId = "rp:acme:prod",
                description = "requests the following",
            ),
        )
    }
}

@ThemeModePreviews
@Composable
private fun RelyingPartyStackedCenteredPreview() {
    PreviewTheme {
        RelyingParty(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SPACING_EXTRA_SMALL.dp),
            relyingPartyData = RelyingPartyDataUi(
                logo = null,
                isVerified = true,
                name = "Aegean Airlines",
                uniqueId = null,
                description = null,
            ),
            layout = RelyingPartyLayout.StackedCentered,
        )
    }
}