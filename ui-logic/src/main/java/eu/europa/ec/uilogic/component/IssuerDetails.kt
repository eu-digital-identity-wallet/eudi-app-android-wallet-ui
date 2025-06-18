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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.theme.values.success
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.WrapAsyncImage
import eu.europa.ec.uilogic.component.wrap.WrapCard
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import java.net.URI

/**
 * Data class representing the information displayed on an issuer details card.
 *
 * This class holds the details about the card issuer, such as name, logo, category,
 * location, and verification status.
 *
 * @property issuerName The name of the card issuer.
 * @property issuerLogo The icon representing the issuer's logo.
 * @property issuerIsVerified Indicates whether the issuer is verified.
 */
data class IssuerDetailsCardDataUi(
    val issuerName: String?,
    val issuerLogo: URI?,
    val issuerIsVerified: Boolean,
)

/**
 * A composable function that displays an issuer details card.
 *
 * This card shows information about an issuer, such as their logo, name,
 * verification status, category, and location.
 *
 * @param item The data object containing the issuer details to display.
 * @param modifier Modifier used to adjust the layout or appearance of the card.
 * @param shape The shape of the card. Defaults to a rounded corner shape.
 * @param colors Optional [CardColors] to customize the card's background and content colors.
 * @param onClick An optional callback function that is invoked when the card is clicked.
 */
@Composable
fun IssuerDetailsCard(
    item: IssuerDetailsCardDataUi,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: CardColors? = null,
    onClick: (() -> Unit)? = null,
) {
    WrapCard(
        modifier = modifier,
        shape = shape,
        colors = colors,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = SPACING_MEDIUM.dp,
                    vertical = SPACING_SMALL.dp
                ),
            verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
            horizontalAlignment = Alignment.Start
        ) {
            item.issuerLogo?.let { safeIssuerLogo ->
                WrapAsyncImage(
                    modifier = Modifier.fillMaxWidth(0.5f),
                    source = safeIssuerLogo.toString(),
                    contentScale = ContentScale.FillWidth
                )
            }

            item.issuerName?.let { safeIssuerName ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.issuerIsVerified) {
                        WrapIcon(
                            modifier = Modifier
                                .size(18.dp)
                                .padding(
                                    start = 0.dp,
                                    end = SPACING_EXTRA_SMALL.dp,
                                    top = SPACING_EXTRA_SMALL.dp,
                                    bottom = SPACING_EXTRA_SMALL.dp
                                ),
                            iconData = AppIcons.Verified,
                            customTint = MaterialTheme.colorScheme.success,
                        )
                    }

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = safeIssuerName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                    )
                }
            }

        }
    }
}

@ThemeModePreviews
@Composable
private fun IssuerDetailsCardPreview() {
    PreviewTheme {
        Column(modifier = Modifier.fillMaxWidth()) {
            val issuerDetails = IssuerDetailsCardDataUi(
                issuerName = "Hellenic Government",
                issuerLogo = null,
                issuerIsVerified = false,
            )

            IssuerDetailsCard(
                item = issuerDetails,
            )
        }
    }
}