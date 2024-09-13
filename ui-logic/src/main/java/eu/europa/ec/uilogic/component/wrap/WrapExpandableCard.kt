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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.theme.values.backgroundDefault
import eu.europa.ec.resourceslogic.theme.values.textPrimaryDark
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM

@Composable
fun WrapExpandableCard(
    modifier: Modifier = Modifier,
    cardTitleContent: @Composable () -> Unit,
    cardTitlePadding: PaddingValues? = null,
    cardContent: @Composable () -> Unit,
    cardContentPadding: PaddingValues? = null,
    onCardClick: (() -> Unit)? = null,
    throttleClicks: Boolean = true,
    cardColors: CardColors? = null,
    enabled: Boolean = true,
    expandCard: Boolean,
) {
    WrapCard(
        modifier = modifier,
        onClick = onCardClick,
        throttleClicks = throttleClicks,
        colors = cardColors ?: CardDefaults.cardColors(
            contentColor = MaterialTheme.colorScheme.textPrimaryDark,
            containerColor = MaterialTheme.colorScheme.backgroundDefault
        ),
        enabled = enabled
    ) {
        Column(
            modifier = Modifier
                .padding(
                    paddingValues = cardTitlePadding ?: PaddingValues(
                        SPACING_MEDIUM.dp
                    )
                )
        ) {
            cardTitleContent()

            AnimatedVisibility(visible = expandCard) {
                Column(
                    modifier = Modifier.padding(
                        paddingValues = cardContentPadding ?: PaddingValues(
                            SPACING_EXTRA_SMALL.dp
                        )
                    ),
                    verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
                ) {
                    cardContent()
                }
            }
        }
    }
}

@ThemeModePreviews
@Composable
private fun WrapExpandableCardCollapsedPreview() {
    PreviewTheme {
        WrapExpandableCard(
            cardTitleContent = {
                Text(text = "Verification Data")
            },
            cardContent = {
                repeat(5) {
                    Text(text = "Data $it")
                }
            },
            expandCard = false
        )
    }
}

@ThemeModePreviews
@Composable
private fun WrapExpandableCardExpandedPreview() {
    PreviewTheme {
        WrapExpandableCard(
            cardTitleContent = {
                Text(text = "Verification Data")
            },
            cardContent = {
                repeat(5) {
                    Text(text = "Data $it")
                }
            },
            expandCard = true
        )
    }
}