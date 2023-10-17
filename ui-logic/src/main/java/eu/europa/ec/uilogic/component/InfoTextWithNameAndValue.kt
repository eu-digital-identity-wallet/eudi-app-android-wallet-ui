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

package eu.europa.ec.uilogic.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import eu.europa.ec.resourceslogic.theme.values.textPrimaryDark
import eu.europa.ec.resourceslogic.theme.values.textSecondaryDark
import eu.europa.ec.uilogic.component.utils.VSpacer

data class InfoTextWithNameAndValueData(
    val infoName: String,
    val infoValue: String,
)

@Composable
fun InfoTextWithNameAndValue(
    itemData: InfoTextWithNameAndValueData,
    modifier: Modifier = Modifier,
    infoNameTextStyle: TextStyle = MaterialTheme.typography.bodySmall.copy(
        color = MaterialTheme.colorScheme.textSecondaryDark
    ),
    infoValueTextStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.textPrimaryDark
    )
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = itemData.infoName,
            style = infoNameTextStyle
        )

        VSpacer.ExtraSmall()

        Text(
            text = itemData.infoValue,
            style = infoValueTextStyle
        )
    }
}

@Preview
@Composable
private fun InfoTextWithNameAndValuePreview() {
    val infoItemData = InfoTextWithNameAndValueData(
        infoName = "Name:",
        infoValue = "John Smith"
    )

    InfoTextWithNameAndValue(itemData = infoItemData)
}