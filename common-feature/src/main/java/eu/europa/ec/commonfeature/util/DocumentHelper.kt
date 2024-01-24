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

package eu.europa.ec.commonfeature.util

import eu.europa.ec.businesslogic.util.getStringFromJsonOrEmpty
import eu.europa.ec.businesslogic.util.toDateFormatted
import eu.europa.ec.commonfeature.model.toDocumentTypeUi
import eu.europa.ec.commonfeature.ui.document_details.model.DocumentJsonKeys
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.nameSpacedDataJSONObject
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import org.json.JSONArray
import org.json.JSONObject

fun extractValueFromDocumentOrEmpty(
    document: Document,
    key: String
): String {
    val docType = document.docType.toDocumentTypeUi()
    val documentJsonObject =
        document.nameSpacedDataJSONObject.get(docType.codeName) as? JSONObject
    return documentJsonObject?.getStringFromJsonOrEmpty(key) ?: ""
}

fun extractFullNameFromDocumentOrEmpty(document: Document): String {
    val firstName = extractValueFromDocumentOrEmpty(
        document = document,
        key = DocumentJsonKeys.FIRST_NAME
    )
    val lastName = extractValueFromDocumentOrEmpty(
        document = document,
        key = DocumentJsonKeys.LAST_NAME
    )
    return "$firstName $lastName"
}

fun keyIsBase64(key: String): Boolean {
    val listOfBase64Keys = DocumentJsonKeys.BASE64_IMAGE_KEYS
    return listOfBase64Keys.contains(key)
}

private fun keyIsGender(key: String): Boolean {
    val listOfGenderKeys = DocumentJsonKeys.GENDER_KEYS
    return listOfGenderKeys.contains(key)
}

private fun getGenderValue(value: String, resourceProvider: ResourceProvider): String =
    when (value) {
        "1" -> {
            resourceProvider.getString(R.string.request_gender_male)
        }

        "0" -> {
            resourceProvider.getString(R.string.request_gender_female)
        }

        else -> {
            value
        }
    }

fun getKeyValueUi(
    item: Any,
    key: String,
    resourceProvider: ResourceProvider,
): Pair<String, String> {
    val uiKey = resourceProvider.getReadableElementIdentifier(key)
    val uiValue: String =
        when (item) {

            // Item is a JSON Array with other JSON Objects within it.
            is JSONArray -> {

                val allItems: MutableList<String> = mutableListOf()

                for (i in 0 until item.length()) {
                    item.optJSONObject(i)?.let { row ->
                        row.keys().forEach { objKey ->
                            row.opt(objKey)?.toString()?.let { value ->
                                allItems.add(
                                    "${resourceProvider.getReadableElementIdentifier(objKey)}:" +
                                            " ${value.toDateFormatted() ?: value}"
                                )
                            }
                        }
                    }
                }

                var result = ""
                allItems.forEachIndexed { index, s ->
                    result += s
                    if (index != allItems.lastIndex) {
                        result += "\n"
                    }
                }

                result
            }

            // Item is Boolean.
            is Boolean -> {
                val infoValue = resourceProvider.getString(
                    if (item) {
                        R.string.document_details_boolean_item_true_readable_value
                    } else {
                        R.string.document_details_boolean_item_false_readable_value
                    }
                )

                infoValue
            }

            // Item is String, Int, etc.
            else -> {
                // Try to parse it as a Date.
                val date: String? = (item as? String)?.toDateFormatted()

                val infoValue = when {

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

                infoValue
            }
        }

    return Pair(uiKey, uiValue)
}