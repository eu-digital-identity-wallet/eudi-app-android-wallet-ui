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

import android.util.Base64
import eu.europa.ec.businesslogic.extension.decodeFromBase64
import eu.europa.ec.businesslogic.extension.encodeToBase64String
import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.businesslogic.util.safeLet
import eu.europa.ec.businesslogic.util.toDateFormatted
import eu.europa.ec.corelogic.extension.getLocalizedClaimName
import eu.europa.ec.corelogic.extension.removeEmptyGroups
import eu.europa.ec.corelogic.extension.sortRecursivelyBy
import eu.europa.ec.corelogic.model.ClaimDomain
import eu.europa.ec.corelogic.model.ClaimPathDomain
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.NameSpace
import eu.europa.ec.eudi.wallet.document.format.DocumentClaim
import eu.europa.ec.eudi.wallet.document.format.MsoMdocData
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcClaim
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcData
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.uuid.ExperimentalUuidApi

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
    claimMetaData: IssuerMetadata.Claim?,
    userLocale: Locale,
    fallback: String,
): String {
    return claimMetaData
        ?.display.getLocalizedClaimName(
            userLocale = userLocale,
            fallback = fallback
        )
}

fun createKeyValue(
    item: Any,
    groupKey: String,
    childKey: String = "",
    disclosurePath: ClaimPathDomain,
    resourceProvider: ResourceProvider,
    uuidProvider: UuidProvider,
    claimMetaData: IssuerMetadata.Claim?,
    allItems: MutableList<ClaimDomain>,
) {

    @OptIn(ExperimentalUuidApi::class)
    fun addFlatOrGroupedChildren(
        allItems: MutableList<ClaimDomain>,
        children: List<ClaimDomain>,
        groupKey: String,
        displayTitle: String,
        predicate: () -> Boolean
    ) {

        val groupIsAlreadyPresent = children
            .filterIsInstance<ClaimDomain.Group>()
            .any { it.key == groupKey }

        if (predicate() && !groupIsAlreadyPresent) {
            allItems.add(
                ClaimDomain.Group(
                    key = groupKey,
                    displayTitle = displayTitle,
                    path = ClaimPathDomain(listOf(uuidProvider.provideUuid())),
                    items = children
                )
            )
        } else {
            allItems.addAll(children)
        }
    }

    when (item) {

        is Map<*, *> -> {

            val children: MutableList<ClaimDomain> = mutableListOf()
            val childKeys: MutableList<String> = mutableListOf()

            item.forEach { (key, value) ->
                safeLet(key as? String, value) { key, value ->

                    val newGroupKey = if (value is Collection<*>) key else groupKey
                    val newChildKey = if (value is Collection<*>) "" else key

                    childKeys.add(newChildKey)

                    createKeyValue(
                        item = value,
                        groupKey = newGroupKey,
                        childKey = newChildKey,
                        disclosurePath = disclosurePath,
                        resourceProvider = resourceProvider,
                        uuidProvider = uuidProvider,
                        claimMetaData = null,
                        allItems = children
                    )
                }
            }

            addFlatOrGroupedChildren(
                allItems = allItems,
                children = children,
                groupKey = groupKey,
                displayTitle = getReadableNameFromIdentifier(
                    claimMetaData = claimMetaData,
                    userLocale = resourceProvider.getLocale(),
                    fallback = groupKey
                )
            ) {
                childKeys.none { it.isEmpty() }
            }
        }

        is Collection<*> -> {

            val children: MutableList<ClaimDomain> = mutableListOf()

            item.forEach { value ->
                value?.let {
                    createKeyValue(
                        item = it,
                        groupKey = groupKey,
                        disclosurePath = disclosurePath,
                        resourceProvider = resourceProvider,
                        uuidProvider = uuidProvider,
                        claimMetaData = claimMetaData,
                        allItems = children
                    )
                }
            }

            addFlatOrGroupedChildren(
                allItems = allItems,
                children = children,
                groupKey = groupKey,
                displayTitle = getReadableNameFromIdentifier(
                    claimMetaData = claimMetaData,
                    userLocale = resourceProvider.getLocale(),
                    fallback = groupKey
                )
            ) {
                childKey.isEmpty()
            }
        }

        else -> {

            val base64Image = (item as? ByteArray)?.encodeToBase64String(Base64.URL_SAFE)

            val date: String? = (item as? String)?.toDateFormatted()
                ?: (item as? LocalDate)?.toDateFormatted()

            val formattedValue = when {
                base64Image != null -> base64Image
                keyIsGender(groupKey) -> getGenderValue(item.toString(), resourceProvider)
                keyIsUserPseudonym(groupKey) -> item.toString().decodeFromBase64()
                date != null -> date
                item is Boolean -> resourceProvider.getString(
                    if (item) R.string.document_details_boolean_item_true_readable_value
                    else R.string.document_details_boolean_item_false_readable_value
                )

                else -> item.toString()
            }

            allItems.add(
                ClaimDomain.Primitive(
                    key = childKey.ifEmpty { groupKey },
                    displayTitle = childKey.ifEmpty {
                        getReadableNameFromIdentifier(
                            claimMetaData = claimMetaData,
                            userLocale = resourceProvider.getLocale(),
                            fallback = groupKey
                        )
                    },
                    path = disclosurePath,
                    isRequired = false,
                    value = formattedValue
                )
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
    tree: List<ClaimDomain>,
    path: ClaimPathDomain,
    disclosurePath: ClaimPathDomain,
    claims: List<DocumentClaim>,
    resourceProvider: ResourceProvider,
    uuidProvider: UuidProvider,
): List<ClaimDomain> {
    if (path.value.isEmpty()) return tree

    val userLocale = resourceProvider.getLocale()

    val key = path.value.first()

    val existingNode = tree.find { it.key == key }

    val currentClaim: DocumentClaim? = claims.find { it.identifier == key }

    return if (path.value.size == 1) {
        // Leaf node (Primitive or Nested Structure)
        if (existingNode == null && currentClaim != null) {
            val accumulatedClaims: MutableList<ClaimDomain> = mutableListOf()
            createKeyValue(
                item = currentClaim.value!!,
                groupKey = currentClaim.identifier,
                resourceProvider = resourceProvider,
                uuidProvider = uuidProvider,
                claimMetaData = currentClaim.issuerMetadata,
                disclosurePath = disclosurePath,
                allItems = accumulatedClaims,
            )
            tree + accumulatedClaims
        } else {
            tree // Already exists or not available, return unchanged
        }
    } else {
        // Group node (Intermediate)
        val childClaims =
            (claims.find { key == it.identifier } as? SdJwtVcClaim)?.children ?: claims
        val updatedNode = if (existingNode is ClaimDomain.Group) {
            // Update existing group by inserting the next path segment into its items
            existingNode.copy(
                items = insertPath(
                    tree = existingNode.items,
                    path = ClaimPathDomain(path.value.drop(1)),
                    disclosurePath = disclosurePath,
                    claims = childClaims,
                    resourceProvider = resourceProvider,
                    uuidProvider = uuidProvider,
                )
            )
        } else {
            // Create a new group and insert the next path segment
            ClaimDomain.Group(
                key = currentClaim?.identifier ?: key,
                displayTitle = getReadableNameFromIdentifier(
                    claimMetaData = currentClaim?.issuerMetadata,
                    userLocale = userLocale,
                    fallback = currentClaim?.identifier ?: key
                ),
                path = ClaimPathDomain(disclosurePath.value.take((disclosurePath.value.size - path.value.size) + 1)),
                items = insertPath(
                    tree = emptyList(),
                    path = ClaimPathDomain(path.value.drop(1)),
                    disclosurePath = disclosurePath,
                    claims = childClaims,
                    resourceProvider = resourceProvider,
                    uuidProvider = uuidProvider,
                )
            )
        }

        tree.filter { it.key != key } + updatedNode // Replace or add the updated node
    }
}

// Function to build the tree from a list of paths
fun transformPathsToDomainClaims(
    paths: List<ClaimPathDomain>,
    claims: List<DocumentClaim>,
    resourceProvider: ResourceProvider,
    uuidProvider: UuidProvider
): List<ClaimDomain> {
    return paths.fold<ClaimPathDomain, List<ClaimDomain>>(initial = emptyList()) { acc, path ->
        insertPath(
            tree = acc,
            path = path,
            disclosurePath = path,
            claims = claims,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider
        )
    }.removeEmptyGroups()
        .sortRecursivelyBy {
            it.displayTitle.lowercase()
        }
}