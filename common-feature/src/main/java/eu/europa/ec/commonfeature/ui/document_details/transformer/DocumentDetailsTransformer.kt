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

package eu.europa.ec.commonfeature.ui.document_details.transformer

import eu.europa.ec.businesslogic.util.toDateFormatted
import eu.europa.ec.businesslogic.util.toList
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.commonfeature.model.DocumentUiIssuanceState
import eu.europa.ec.commonfeature.model.toUiName
import eu.europa.ec.commonfeature.ui.document_details.model.DocumentDetailsUi
import eu.europa.ec.commonfeature.ui.document_details.model.DocumentJsonKeys
import eu.europa.ec.commonfeature.util.documentHasExpired
import eu.europa.ec.commonfeature.util.extractFullNameFromDocumentOrEmpty
import eu.europa.ec.commonfeature.util.extractValueFromDocumentOrEmpty
import eu.europa.ec.commonfeature.util.parseKeyValueUi
import eu.europa.ec.corelogic.model.toDocumentIdentifier
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.nameSpacedDataJSONObject
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.InfoTextWithNameAndImageData
import eu.europa.ec.uilogic.component.InfoTextWithNameAndValueData
import org.json.JSONObject

object DocumentDetailsTransformer {

    fun transformToUiItem(
        document: IssuedDocument,
        resourceProvider: ResourceProvider,
    ): DocumentUi? {

        val documentIdentifierUi = document.toDocumentIdentifier()

        // Get the JSON Object from EudiWallerCore.
        val documentJson =
            (document.nameSpacedDataJSONObject[documentIdentifierUi.nameSpace] as JSONObject)

        // Create a JSON Array with all its keys (i.e. given_name, family_name, etc.) keeping their original order.
        val documentKeysJsonArray = documentJson.names() ?: return null

        // Create a JSON Array with all its values (i.e. John, Smith, etc.) keeping their original order.
        val documentValuesJsonArray = documentJson.toJSONArray(documentKeysJsonArray) ?: return null

        val detailsItems = documentValuesJsonArray
            .toList()
            .withIndex()
            // Create a connection between keys and values using their index--original order.
            .associateBy {
                documentKeysJsonArray.get(it.index)
            }
            // Now that we have both the keys and the values, transform them to UI items.
            .map {
                val value = it.value.value
                val key = it.key.toString()
                transformToDocumentDetailsUi(
                    key = key,
                    item = value,
                    resourceProvider = resourceProvider
                )
            }

        val documentImage = extractValueFromDocumentOrEmpty(
            document = document,
            key = DocumentJsonKeys.PORTRAIT
        )

        val documentExpirationDate = extractValueFromDocumentOrEmpty(
            document = document,
            key = DocumentJsonKeys.EXPIRY_DATE
        )

        val docHasExpired = documentHasExpired(documentExpirationDate)

        return DocumentUi(
            documentId = document.id,
            documentName = document.toUiName(resourceProvider),
            documentIdentifier = documentIdentifierUi,
            documentExpirationDateFormatted = documentExpirationDate.toDateFormatted().orEmpty(),
            documentHasExpired = docHasExpired,
            documentImage = documentImage,
            documentDetails = detailsItems,
            userFullName = extractFullNameFromDocumentOrEmpty(document),
            documentIssuanceState = DocumentUiIssuanceState.Issued,
        )
    }

}

private fun transformToDocumentDetailsUi(
    key: String,
    item: Any,
    resourceProvider: ResourceProvider
): DocumentDetailsUi {

    val uiKey = resourceProvider.getReadableElementIdentifier(key)

    val values = StringBuilder()
    parseKeyValueUi(
        json = item,
        groupIdentifier = key,
        resourceProvider = resourceProvider,
        allItems = values
    )
    val groupedValues = values.toString()

    return when (key) {
        DocumentJsonKeys.SIGNATURE -> {
            DocumentDetailsUi.SignatureItem(
                itemData = InfoTextWithNameAndImageData(
                    title = uiKey,
                    base64Image = groupedValues
                )
            )
        }

        DocumentJsonKeys.PORTRAIT -> {
            DocumentDetailsUi.DefaultItem(
                itemData = InfoTextWithNameAndValueData.create(
                    title = uiKey,
                    infoValues = arrayOf(resourceProvider.getString(R.string.document_details_portrait_readable_identifier))
                )
            )
        }

        else -> {
            DocumentDetailsUi.DefaultItem(
                itemData = InfoTextWithNameAndValueData.create(
                    title = uiKey,
                    infoValues = arrayOf(groupedValues)
                )
            )
        }
    }
}