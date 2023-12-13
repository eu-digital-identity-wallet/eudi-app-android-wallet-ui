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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.textPrimaryDark
import eu.europa.ec.resourceslogic.theme.values.textSecondaryDark
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.WrapImage

private val defaultInfoNameTextStyle: TextStyle
    @Composable get() =
        MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.textSecondaryDark
        )

private val defaultInfoValueTextStyle: TextStyle
    @Composable get() =
        MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.textPrimaryDark
        )

class InfoTextWithNameAndValueData private constructor(
    val title: String,
    val infoValues: List<String>?,
) {
    companion object {
        fun create(
            title: String,
            vararg infoValues: String
        ): InfoTextWithNameAndValueData {
            return InfoTextWithNameAndValueData(
                title = title,
                infoValues = infoValues.toList()
            )
        }
    }
}

data class InfoTextWithNameAndImageData(
    val title: String,
    val base64Image: String
)

@Composable
fun InfoTextWithNameAndValue(
    itemData: InfoTextWithNameAndValueData,
    modifier: Modifier = Modifier,
    infoNameTextStyle: TextStyle = defaultInfoNameTextStyle,
    infoValueTextStyle: TextStyle = defaultInfoValueTextStyle,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = itemData.title,
            style = infoNameTextStyle
        )

        itemData.infoValues?.let { infoValues ->
            Column {
                infoValues.forEach { infoValue ->
                    VSpacer.ExtraSmall()

                    Text(
                        text = infoValue,
                        style = infoValueTextStyle
                    )
                }
            }
        }
    }
}

@Composable
fun InfoTextWithNameAndImage(
    itemData: InfoTextWithNameAndImageData,
    contentDescription: String,
    modifier: Modifier = Modifier,
    infoNameTextStyle: TextStyle = defaultInfoNameTextStyle,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = itemData.title,
            style = infoNameTextStyle
        )
        if (itemData.base64Image.isNotBlank()) {
            WrapImage(
                bitmap = rememberBase64DecodedBitmap(base64Image = itemData.base64Image),
                contentDescription = contentDescription
            )
        }
    }
}

@ThemeModePreviews
@Composable
private fun InfoTextWithNameAndValuePreview() {
    val itemData = InfoTextWithNameAndValueData.create(
        title = "Name",
        "John Smith"
    )

    PreviewTheme {
        InfoTextWithNameAndValue(itemData = itemData)
    }
}

@ThemeModePreviews
@Composable
private fun InfoTextWithNameAndImagePreview() {
    val itemData = InfoTextWithNameAndImageData(
        title = "Signature",
        base64Image = ""
    )

    PreviewTheme {
        InfoTextWithNameAndImage(
            itemData = itemData,
            contentDescription = stringResource(id = R.string.content_description_user_signature)
        )
    }
}