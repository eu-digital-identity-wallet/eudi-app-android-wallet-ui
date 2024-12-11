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

package eu.europa.ec.commonfeature.ui.document_details.model

import eu.europa.ec.uilogic.component.InfoTextWithNameAndImageData
import eu.europa.ec.uilogic.component.InfoTextWithNameAndValueData
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.MainContentData
import eu.europa.ec.uilogic.component.PortraitWithImageData

// TODO will be removed
sealed interface DocumentDetailsItemData {

    data class DocumentItemFieldWithValue(
        val itemData: InfoTextWithNameAndValueData
    ) : DocumentDetailsItemData

    data class DocumentItemFieldWithImageData(
        val itemData: InfoTextWithNameAndImageData
    ) : DocumentDetailsItemData

    data class DocumentItemPortraitImage(
        val itemData: PortraitWithImageData
    ) : DocumentDetailsItemData

    data object Unknown : DocumentDetailsItemData
}

fun InfoTextWithNameAndValueData.toListItemData(): ListItemData {
    return ListItemData(
        itemId = "",
        overlineText = title,
        mainContentData = MainContentData.Text(text = infoValues.toString())
    )
}

fun InfoTextWithNameAndImageData.toListItemData(): ListItemData {
    return ListItemData(
        itemId = "",
        overlineText = title,
        mainContentData = MainContentData.Image(base64Image = base64Image)
    )
}

fun PortraitWithImageData.toListItemData(): ListItemData {
    return ListItemData(
        itemId = "",
        overlineText = title,
        mainContentData = MainContentData.Image(base64Image = base64Image)
    )
}
