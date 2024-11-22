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

package eu.europa.ec.commonfeature.ui.document_details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.europa.ec.commonfeature.ui.document_details.model.DocumentDetailsUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.InfoTextWithNameAndImage
import eu.europa.ec.uilogic.component.InfoTextWithNameAndImageData
import eu.europa.ec.uilogic.component.InfoTextWithNameAndValue
import eu.europa.ec.uilogic.component.InfoTextWithNameAndValueData
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.VSpacer

@Composable
fun DetailsContent(
    modifier: Modifier = Modifier,
    data: List<DocumentDetailsUi>,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        VSpacer.Large()
        data.mapNotNull { documentDetailsUi ->
            when (documentDetailsUi) {
                is DocumentDetailsUi.DefaultItem -> {
                    documentDetailsUi.itemData.infoValues
                        ?.toTypedArray()
                        ?.let { infoValues ->
                            val itemData = InfoTextWithNameAndValueData.create(
                                title = documentDetailsUi.itemData.title,
                                *infoValues
                            )
                            InfoTextWithNameAndValue(
                                modifier = Modifier.fillMaxWidth(),
                                itemData = itemData
                            )
                        }
                }

                is DocumentDetailsUi.SignatureItem -> {
                    InfoTextWithNameAndImage(
                        modifier = Modifier.fillMaxWidth(),
                        itemData = documentDetailsUi.itemData,
                        contentDescription = stringResource(id = R.string.content_description_user_signature_icon)
                    )
                }

                is DocumentDetailsUi.Unknown -> null
            }
        }
    }
    VSpacer.Large()
}

@ThemeModePreviews
@Composable
private fun DetailsContentPreview() {
    val defaultItemData = InfoTextWithNameAndValueData.create(
        title = "Name",
        "John Smith"
    )
    val signatureItemData = InfoTextWithNameAndImageData(
        title = "Signature",
        base64Image = ""
    )
    val data = listOf(
        DocumentDetailsUi.DefaultItem(
            itemData = defaultItemData
        ),
        DocumentDetailsUi.SignatureItem(
            itemData = signatureItemData
        ),
        DocumentDetailsUi.Unknown
    )
    PreviewTheme {
        DetailsContent(
            data = data
        )
    }
}