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
import eu.europa.ec.corelogic.extension.getLocalizedClaimName
import eu.europa.ec.corelogic.model.ClaimPath
import eu.europa.ec.corelogic.model.DomainClaim
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.NameSpace
import eu.europa.ec.eudi.wallet.document.format.DocumentClaim
import eu.europa.ec.eudi.wallet.document.format.MsoMdocData
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcClaim
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcData
import eu.europa.ec.eudi.wallet.document.metadata.DocumentMetaData
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

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

fun getReadableNameFromIdentifier(
    metadata: DocumentMetaData?,
    userLocale: Locale,
    identifier: String,
): String {
    return metadata?.claims
        ?.find { it.name.name == identifier }
        ?.display.getLocalizedClaimName(
            userLocale = userLocale,
            fallback = identifier
        )
}

fun createKeyValue(
    item: Any,
    groupKey: String,
    childKey: String = "",
    resourceProvider: ResourceProvider,
    allItems: MutableList<Pair<String, String>>,
) {
    when (item) {

        is Map<*, *> -> {
            item.forEach { (key, value) ->
                safeLet(key as? String, value) { key, value ->
                    createKeyValue(
                        item = value,
                        groupKey = groupKey,
                        childKey = key,
                        resourceProvider = resourceProvider,
                        allItems = allItems
                    )
                }
            }
        }

        is Collection<*> -> {
            item.forEach { value ->
                value?.let {
                    createKeyValue(
                        item = it,
                        groupKey = groupKey,
                        resourceProvider = resourceProvider,
                        allItems = allItems
                    )
                }
            }
        }

        is Boolean -> {
            allItems.add(
                childKey
                        to resourceProvider.getString(
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
            allItems.add(
                childKey to
                        when {

                            keyIsGender(groupKey) -> {
                                getGenderValue(item.toString(), resourceProvider)
                            }

                            keyIsUserPseudonym(groupKey) -> {
                                item.toString().decodeFromBase64()
                            }

                            date != null && childKey.isEmpty() -> {
                                date
                            }

                            else -> {
                                val jsonString = item.toString()
                                if (childKey.isEmpty()) {
                                    jsonString
                                } else {
                                    jsonString.toDateFormatted() ?: jsonString
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

private fun insertPath(
    tree: List<DomainClaim>,
    path: ClaimPath,
    disclosurePath: ClaimPath,
    claims: List<DocumentClaim>,
    metadata: DocumentMetaData?,
    resourceProvider: ResourceProvider,
): List<DomainClaim> {
    if (path.value.isEmpty()) return tree

    val userLocale = resourceProvider.getLocale()

    val key = path.value.first()

    val existingNode = tree.find { it.key == key }

    val currentClaim: DocumentClaim? = claims
        .find {
            it.identifier == key
        }

    val isRequired = false

    return if (path.value.size == 1) {
        // Leaf node (Primitive)
        if (existingNode == null && currentClaim != null) {
            val formattedValue = buildList {
                createKeyValue(
                    item = currentClaim.value!!,
                    groupKey = currentClaim.identifier,
                    resourceProvider = resourceProvider,
                    allItems = this
                )
            }

            val newEntry = if (formattedValue.size == 1) {
                DomainClaim.Primitive(
                    key = currentClaim.identifier,
                    displayTitle = getReadableNameFromIdentifier(
                        metadata = metadata,
                        userLocale = userLocale,
                        identifier = currentClaim.identifier
                    ),
                    path = disclosurePath,
                    isRequired = isRequired,
                    value = formattedValue.first().second //TODO
                )
            } else {
                DomainClaim.Group(
                    key = currentClaim.identifier,
                    displayTitle = getReadableNameFromIdentifier(
                        metadata = metadata,
                        userLocale = userLocale,
                        identifier = currentClaim.identifier
                    ),
                    path = disclosurePath,
                    items = formattedValue.map {
                        DomainClaim.Primitive(
                            key = it.first,
                            displayTitle = getReadableNameFromIdentifier(
                                metadata = metadata,
                                userLocale = userLocale,
                                identifier = it.first
                            ),
                            path = disclosurePath,
                            isRequired = isRequired,
                            value = it.second //TODO
                        )
                    }
                )
            }
            tree + newEntry
        } else {
            tree // Already exists or not available, return unchanged
        }
    } else {
        // Group node (Intermediate)
        val childClaims =
            (claims.find { key == it.identifier } as? SdJwtVcClaim)?.children ?: claims
        val updatedNode = if (existingNode is DomainClaim.Group) {
            // Update existing group by inserting the next path segment into its items
            existingNode.copy(
                items = insertPath(
                    tree = existingNode.items.sortedBy { it.displayTitle.lowercase() },
                    path = ClaimPath(path.value.drop(1)),
                    disclosurePath = disclosurePath,
                    claims = childClaims,
                    metadata = metadata,
                    resourceProvider = resourceProvider,
                )
            )
        } else {
            // Create a new group and insert the next path segment
            DomainClaim.Group(
                key = currentClaim?.identifier ?: key,
                displayTitle = getReadableNameFromIdentifier(
                    metadata = metadata,
                    userLocale = userLocale,
                    identifier = currentClaim?.identifier ?: key
                ),
                path = ClaimPath(disclosurePath.value.take((disclosurePath.value.size - path.value.size) + 1)),
                items = insertPath(
                    tree = emptyList(),
                    path = ClaimPath(path.value.drop(1)),
                    disclosurePath = disclosurePath,
                    claims = childClaims,
                    metadata = metadata,
                    resourceProvider = resourceProvider,
                )
            )
        }

        tree.filter { it.key != key } + updatedNode // Replace or add the updated node
    }
}

// Function to build the tree from a list of paths
fun transformPathsToDomainClaims(
    paths: List<ClaimPath>,
    claims: List<DocumentClaim>,
    metadata: DocumentMetaData?,
    resourceProvider: ResourceProvider,
): List<DomainClaim> {
    return paths.fold<ClaimPath, List<DomainClaim>>(emptyList()) { acc, path ->
        insertPath(
            tree = acc,
            path = path,
            disclosurePath = path,
            claims = claims,
            metadata = metadata,
            resourceProvider = resourceProvider,
        )
    }.sortedBy { it.displayTitle.lowercase() }
}