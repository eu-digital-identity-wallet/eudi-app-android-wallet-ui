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

package eu.europa.ec.issuancefeature.ui.document.offer.transformer

import eu.europa.ec.commonfeature.ui.request.model.DocumentItemUi
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.MainContentData

internal object DocumentOfferTransformer {
    fun List<DocumentItemUi>.transformToDocumentOfferUi(): List<ListItemData> {
        return this.mapIndexed { index, item ->
            ListItemData(
                itemId = generateItemId(documentId = item.documentId, index = index),
                mainContentData = MainContentData.Text(item.title)
            )
        }
    }
}

private fun generateItemId(
    documentId: DocumentId,
    index: Int
): String {
    return "${documentId}_$index"
}
