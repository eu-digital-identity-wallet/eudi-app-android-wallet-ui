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

import eu.europa.ec.businesslogic.util.getStringFromJsonKeyOrEmpty
import eu.europa.ec.businesslogic.util.toDateFormatted
import eu.europa.ec.businesslogic.util.toList
import eu.europa.ec.commonfeature.model.DocumentStatusUi
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.commonfeature.model.toDocumentTypeUi
import eu.europa.ec.commonfeature.ui.document_details.model.DocumentDetailsUi
import eu.europa.ec.commonfeature.ui.document_details.model.DocumentJsonKeys
import eu.europa.ec.commonfeature.ui.request.transformer.RequestTransformer.getGenderValue
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.nameSpacedDataJSONObject
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.InfoTextWithNameAndValueData
import org.json.JSONArray
import org.json.JSONObject

object DocumentDetailsTransformer {

    fun transformToUiItems(
        document: Document,
        resourceProvider: ResourceProvider,
        docType: String,
    ): DocumentUi? {

        // Get the JSON Object from EudiWallerCore.
        val documentJson = (document.nameSpacedDataJSONObject[docType] as JSONObject)

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

        return DocumentUi(
            documentId = document.id,
            documentName = document.name,
            documentType = document.docType.toDocumentTypeUi(),
            documentStatus = DocumentStatusUi.ACTIVE,
            documentImage = documentJson.getStringFromJsonKeyOrEmpty(
                key = DocumentJsonKeys.PORTRAIT
            ),
            documentDetails = detailsItems,
            documentUsername = documentJson.getStringFromJsonKeyOrEmpty(
                key = DocumentJsonKeys.SHORT_NAME
            )
        )
    }

}

private fun transformToDocumentDetailsUi(
    key: String,
    item: Any,
    resourceProvider: ResourceProvider
): DocumentDetailsUi {
    return when (item) {
        // Item is a JSON Array with other JSON Objects within it.
        is JSONArray -> {
            val infoValues = item.toList().flatMap {
                if (it is JSONObject) {
                    val categoryCodeKey = DocumentJsonKeys.VEHICLE_CATEGORY
                    val issueDateKey = DocumentJsonKeys.ISSUE_DATE
                    val expiryDateKey = DocumentJsonKeys.EXPIRY_DATE

                    val categoryCodeValue =
                        it.getStringFromJsonKeyOrEmpty(categoryCodeKey)
                    val issueDateValueFormatted =
                        it.getStringFromJsonKeyOrEmpty(issueDateKey).toDateFormatted()
                    val expiryDateValueFormatted =
                        it.getStringFromJsonKeyOrEmpty(expiryDateKey).toDateFormatted()

                    listOf(
                        "${resourceProvider.getString(R.string.document_details_vehicle_category_code_readable_identifier)}: $categoryCodeValue",
                        "${resourceProvider.getReadableElementIdentifier(issueDateKey)}: $issueDateValueFormatted",
                        "${resourceProvider.getReadableElementIdentifier(expiryDateKey)}: $expiryDateValueFormatted",
                    )
                } else {
                    listOf()
                }
            }.toTypedArray()

            DocumentDetailsUi.DefaultItem(
                infoText = InfoTextWithNameAndValueData.create(
                    title = resourceProvider.getReadableElementIdentifier(key),
                    infoValues = infoValues
                )
            )
        }

        // Item is a primitive Object. (String, Boolean, etc.)
        else -> {
            // Try to parse it as a Date.
            val date: String? = (item as? String)?.toDateFormatted()

            val infoValues = when {
                keyIsBase64(key) -> {
                    resourceProvider.getString(R.string.document_details_base64_item_readable_identifier)
                }

                keyIsGender(key) -> {
                    getGenderValue(item.toString(), resourceProvider)
                }

                date != null -> {
                    date
                }

                else -> {
                    item.toString()
                }
            }

            DocumentDetailsUi.DefaultItem(
                infoText = InfoTextWithNameAndValueData.create(
                    title = resourceProvider.getReadableElementIdentifier(key),
                    infoValues
                )
            )
        }
    }
}

private fun keyIsBase64(key: String): Boolean {
    val listOfBase64Keys = listOf(
        DocumentJsonKeys.PORTRAIT,
        DocumentJsonKeys.SIGNATURE
    )
    return listOfBase64Keys.contains(key)
}

private fun keyIsGender(key: String): Boolean {
    val listOfGenderKeys = DocumentJsonKeys.GENDER
    return listOfGenderKeys.contains(key)
}