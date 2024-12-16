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

import eu.europa.ec.businesslogic.extension.decodeFromBase64
import eu.europa.ec.businesslogic.util.getStringFromJsonOrEmpty
import eu.europa.ec.businesslogic.util.toDateFormatted
import eu.europa.ec.businesslogic.util.toLocalDate
import eu.europa.ec.commonfeature.ui.document_details.model.DocumentJsonKeys
import eu.europa.ec.corelogic.model.toDocumentIdentifier
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.nameSpacedDataJSONObject
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate

fun extractValueFromDocumentOrEmpty(
    document: IssuedDocument,
    key: String
): String {
    return try {
        val documentIdentifier = document.toDocumentIdentifier()
        val documentJsonObject =
            document.nameSpacedDataJSONObject.get(documentIdentifier.nameSpace) as? JSONObject
        return documentJsonObject?.getStringFromJsonOrEmpty(key) ?: ""
    } catch (e: JSONException) {
        ""
    }
}

fun extractFullNameFromDocumentOrEmpty(document: IssuedDocument): String {
    val firstName = extractValueFromDocumentOrEmpty(
        document = document,
        key = DocumentJsonKeys.FIRST_NAME
    )
    val lastName = extractValueFromDocumentOrEmpty(
        document = document,
        key = DocumentJsonKeys.LAST_NAME
    )
    return if (firstName.isNotBlank() && lastName.isNotBlank()) {
        "$firstName $lastName"
    } else if (firstName.isNotBlank()) {
        firstName
    } else if (lastName.isNotBlank()) {
        lastName
    } else {
        ""
    }
}

fun keyIsBase64(key: String): Boolean {
    val listOfBase64Keys = DocumentJsonKeys.BASE64_IMAGE_KEYS
    return listOfBase64Keys.contains(key)
}

fun keyIsPortrait(key: String): Boolean {
    return key == DocumentJsonKeys.PORTRAIT
}

fun keyIsSignature(key: String): Boolean {
    return key == DocumentJsonKeys.SIGNATURE
}

private fun keyIsUserPseudonym(key: String): Boolean {
    return key == DocumentJsonKeys.USER_PSEUDONYM
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

fun parseKeyValueUi(
    json: Any,
    groupIdentifier: String,
    keyIdentifier: String = "",
    resourceProvider: ResourceProvider,
    allItems: StringBuilder
) {
    when (json) {
        is JSONObject -> {
            val keys = json.keys()
            while (keys.hasNext()) {

                val key = keys.next()
                val value = json[key]

                parseKeyValueUi(
                    json = value,
                    groupIdentifier = groupIdentifier,
                    keyIdentifier = key,
                    resourceProvider = resourceProvider,
                    allItems = allItems
                )
            }
        }

        is JSONArray -> {
            for (i in 0 until json.length()) {
                val value = json[i]
                parseKeyValueUi(
                    json = value,
                    groupIdentifier = groupIdentifier,
                    resourceProvider = resourceProvider,
                    allItems = allItems
                )
            }
        }

        is Boolean -> {
            allItems.append(
                resourceProvider.getString(
                    if (json) {
                        R.string.document_details_boolean_item_true_readable_value
                    } else {
                        R.string.document_details_boolean_item_false_readable_value
                    }
                )
            )
        }

        else -> {
            val date: String? = (json as? String)?.toDateFormatted()
            allItems.append(
                when {

                    keyIsGender(groupIdentifier) -> {
                        getGenderValue(json.toString(), resourceProvider)
                    }

                    keyIsUserPseudonym(groupIdentifier) -> {
                        json.toString().decodeFromBase64()
                    }

                    date != null && keyIdentifier.isEmpty() -> {
                        date
                    }

                    else -> {
                        val jsonString = json.toString()
                        if (keyIdentifier.isEmpty()) {
                            jsonString
                        } else {
                            val lineChange = if (allItems.isNotEmpty()) "\n" else ""
                            val key = resourceProvider.getReadableElementIdentifier(keyIdentifier)
                            val value = jsonString.toDateFormatted() ?: jsonString
                            "$lineChange$key: $value"
                        }
                    }
                }
            )
        }
    }
}

fun documentHasExpired(
    documentExpirationDate: String,
    currentDate: LocalDate = LocalDate.now(),
): Boolean {
    val localDateOfDocumentExpirationDate = documentExpirationDate.toLocalDate()

    return localDateOfDocumentExpirationDate?.let {
        currentDate.isAfter(it)
    } ?: false
}