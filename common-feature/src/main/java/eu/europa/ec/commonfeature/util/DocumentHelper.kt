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
import eu.europa.ec.businesslogic.util.safeLet
import eu.europa.ec.businesslogic.util.toDateFormatted
import eu.europa.ec.commonfeature.ui.document_details.model.DocumentJsonKeys
import eu.europa.ec.commonfeature.ui.request.transformer.DomainClaim
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.ElementIdentifier
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.NameSpace
import eu.europa.ec.eudi.wallet.document.format.DocumentClaim
import eu.europa.ec.eudi.wallet.document.format.MsoMdocClaim
import eu.europa.ec.eudi.wallet.document.format.MsoMdocData
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcClaim
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcData
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun extractValueFromDocumentOrEmpty(
    document: IssuedDocument,
    key: String,
): String {
    return document.data.claims
        .firstOrNull { it.identifier == key }
        ?.value
        ?.toString()
        ?: ""
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
        "0" -> {
            resourceProvider.getString(R.string.request_gender_not_known)
        }

        "1" -> {
            resourceProvider.getString(R.string.request_gender_male)
        }

        "2" -> {
            resourceProvider.getString(R.string.request_gender_female)
        }

        "9" -> {
            resourceProvider.getString(R.string.request_gender_not_applicable)
        }

        else -> {
            value
        }
    }

fun parseClaimsToDomain(
    coreClaim: DocumentClaim?,
    readableName: (String) -> String,
    groupIdentifierKey: String?,
    keyIdentifier: String = "",
    resourceProvider: ResourceProvider,
    path: List<String>,
    isRequired: Boolean,
): DomainClaim {
    return try {
        when (coreClaim!!) {
            is MsoMdocClaim -> {
                val value = buildString {
                    parseKeyValueUi(
                        item = coreClaim.value!!, //TODO
                        groupIdentifierKey = groupIdentifierKey!!,
                        keyIdentifier = keyIdentifier,
                        resourceProvider = resourceProvider,
                        allItems = this
                    )
                }

                DomainClaim.Claim.Primitive(
                    key = groupIdentifierKey!!,
                    value = value,
                    displayTitle = readableName(groupIdentifierKey),
                    path = path,
                    isRequired = isRequired
                )
            }

            is SdJwtVcClaim -> {
                return if (coreClaim.children.isEmpty()) {

                    val value = buildString {
                        parseKeyValueUi(
                            item = coreClaim.value!!,
                            groupIdentifierKey = coreClaim.identifier,
                            keyIdentifier = keyIdentifier,
                            resourceProvider = resourceProvider,
                            allItems = this
                        )
                    }

                    DomainClaim.Claim.Primitive(
                        key = coreClaim.identifier,
                        value = value,
                        displayTitle = readableName(coreClaim.identifier),
                        path = path,
                        isRequired = isRequired
                    )
                } else {
                    val result = coreClaim.children.map {
                        parseClaimsToDomain(
                            coreClaim = it,
                            readableName = readableName,
                            groupIdentifierKey = coreClaim.identifier,
                            resourceProvider = resourceProvider,
                            path = path,
                            isRequired = isRequired
                        )
                    }

                    DomainClaim.Claim.Group(
                        items = result,
                        key = coreClaim.identifier,
                        displayTitle = readableName(coreClaim.identifier),
                        path = path
                    )
                }
            }
        }
    } catch (_: Exception) {
        DomainClaim.NotAvailableClaim(
            key = readableName(groupIdentifierKey ?: path.firstOrNull().toString()), //TODO
            displayTitle = resourceProvider.getString(R.string.request_element_identifier_not_available)
        )
    }
}

fun parseKeyValueUi(
    item: Any,
    groupIdentifierKey: String,
    keyIdentifier: String = "",
    resourceProvider: ResourceProvider,
    allItems: StringBuilder,
) {
    when (item) {

        is Map<*, *> -> {
            item.forEach { (key, value) ->
                safeLet(key as? String, value) { key, value ->
                    parseKeyValueUi(
                        item = value,
                        groupIdentifierKey = groupIdentifierKey,
                        keyIdentifier = key,
                        resourceProvider = resourceProvider,
                        allItems = allItems
                    )
                }
            }
        }

        is Collection<*> -> {
            item.forEach { value ->
                value?.let {
                    parseKeyValueUi(
                        item = it,
                        groupIdentifierKey = groupIdentifierKey,
                        resourceProvider = resourceProvider,
                        allItems = allItems
                    )
                }
            }
        }

        is Boolean -> {
            allItems.append(
                resourceProvider.getString(
                    if (item) {
                        R.string.document_details_boolean_item_true_readable_value
                    } else {
                        R.string.document_details_boolean_item_false_readable_value
                    }
                )
            )
        }

        else -> {
            val date: String? = (item as? String)?.toDateFormatted()
            allItems.append(
                when {

                    keyIsGender(groupIdentifierKey) -> {
                        getGenderValue(item.toString(), resourceProvider)
                    }

                    keyIsUserPseudonym(groupIdentifierKey) -> {
                        item.toString().decodeFromBase64()
                    }

                    date != null && keyIdentifier.isEmpty() -> {
                        date
                    }

                    else -> {
                        val jsonString = item.toString()
                        if (keyIdentifier.isEmpty()) {
                            jsonString
                        } else {
                            val lineChange = if (allItems.isNotEmpty()) "\n" else ""
                            val value = jsonString.toDateFormatted() ?: jsonString
                            "$lineChange$keyIdentifier: $value"
                        }
                    }
                }
            )
        }
    }
}

fun documentHasExpired(
    documentExpirationDate: Instant,
    currentDate: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): Boolean {
    return runCatching {
        // Convert Instant to LocalDate using the provided ZoneId
        val localDateOfDocumentExpiration = documentExpirationDate
            .atZone(zoneId)
            .toLocalDate()

        // Check if the current date is after the document expiration date
        currentDate.isAfter(localDateOfDocumentExpiration)
    }.getOrElse {
        // Default to false in case of any exception
        false
    }
}

val IssuedDocument.docNamespace: NameSpace?
    get() = when (val data = this.data) {
        is MsoMdocData -> data.nameSpaces.keys.first()
        is SdJwtVcData -> null
    }

fun generateUniqueFieldId(
    elementIdentifier: ElementIdentifier,
    documentId: DocumentId,
): String =
    elementIdentifier + documentId