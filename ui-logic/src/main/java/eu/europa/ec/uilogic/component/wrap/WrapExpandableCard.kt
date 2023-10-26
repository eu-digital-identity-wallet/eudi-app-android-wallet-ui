/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.uilogic.component.wrap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.theme.values.textPrimaryDark
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
    cardColors: CardColors? = null,
    expandCard: Boolean,
) {
    WrapCard(
        modifier = modifier
            .then(
                if (onCardClick != null) {
                    Modifier.clickable {
                        onCardClick()
                    }
                } else {
                    Modifier
                }
            ),
        colors = cardColors ?: CardDefaults.cardColors(
            contentColor = MaterialTheme.colorScheme.textPrimaryDark
        )
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

@Preview
@Composable
private fun WrapExpandableCardCollapsedPreview() {
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

@Preview
@Composable
private fun WrapExpandableCardExpandedPreview() {
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