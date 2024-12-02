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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.theme.values.divider

sealed interface StickyBottomType {
    data class OneButton(
        val config: ButtonConfig
    ) : StickyBottomType

    data class TwoButtons(
        val primaryButtonConfig: ButtonConfig,
        val secondaryButtonConfig: ButtonConfig,
    ) : StickyBottomType

    data object Generic : StickyBottomType
}

data class StickyBottomConfig(
    val type: StickyBottomType,
    val showDivider: Boolean = true,
)

@Composable
fun WrapStickyBottomContent(
    stickyBottomModifier: Modifier = Modifier,
    stickyBottomConfig: StickyBottomConfig,
    content: @Composable () -> Unit,
) {
    when (val stickyBottomType = stickyBottomConfig.type) {
        is StickyBottomType.OneButton -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (stickyBottomConfig.showDivider) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.divider
                    )
                }

                Row(
                    modifier = stickyBottomModifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    WrapButton(
                        modifier = Modifier.fillMaxWidth(),
                        buttonConfig = stickyBottomType.config,
                    ) {
                        content()
                    }
                }
            }
        }

        is StickyBottomType.TwoButtons -> {
            Row(
                modifier = stickyBottomModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WrapButton(
                    modifier = Modifier.weight(1f),
                    buttonConfig = stickyBottomType.primaryButtonConfig
                ) {
                    content()
                }
                WrapButton(
                    modifier = Modifier.weight(1f),
                    buttonConfig = stickyBottomType.secondaryButtonConfig
                ) {
                    content()
                }
            }
        }

        is StickyBottomType.Generic -> {
            content()
        }
    }
}