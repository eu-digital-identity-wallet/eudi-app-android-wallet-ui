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

package eu.europa.ec.corelogic.model

import eu.europa.ec.eudi.openid4vp.dcql.ClaimPath
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPathElement
import eu.europa.ec.eudi.wallet.document.NameSpace

sealed interface ClaimType {
    data object SdJwtVc : ClaimType
    data class MsoMdoc(val namespace: NameSpace) : ClaimType

    data object Unknown : ClaimType
}

data class ClaimPathDomain(
    val value: List<ClaimPathElement>,
    val type: ClaimType,
) {

    companion object {
        const val PATH_SEPARATOR = ","

        fun toElementIdentifier(itemId: String): String {
            return itemId
                .split(PATH_SEPARATOR)
                .drop(2)
                .first()
        }

        fun toNameSpace(itemId: String): NameSpace {
            return itemId
                .split(PATH_SEPARATOR)
                .drop(1)
                .first()
        }

        fun toSdJwtVcClaimPath(itemId: String): ClaimPath {
            val elements = itemId
                .split(PATH_SEPARATOR)
                .drop(1)
                .map { ClaimPathElement.Claim(it) }
            return ClaimPath(elements)
        }

        /**
         * Converts a list of strings to a [ClaimPathDomain] where all elements
         * are treated as [ClaimPathElement.Claim]s. Use this for paths from stored
         * credentials or transaction logs where no wildcards are expected.
         */
        fun List<String>.toClaimPathDomain(type: ClaimType): ClaimPathDomain {
            return ClaimPathDomain(
                value = this.map { ClaimPathElement.Claim(it) },
                type = type
            )
        }

        /**
         * Checks whether this [ClaimPathDomain] is a prefix of another [ClaimPathDomain].
         *
         * Supports DCQL wildcard matching via [ClaimPathElement.AllArrayElements]:
         * - A wildcard element matches any value at that position.
         * - Trailing wildcard elements are tolerated when the available path ends
         *   at the array claim itself.
         *
         * @param other The [ClaimPathDomain] to compare against.
         * @return `true` if this path is a prefix of [other]; `false` otherwise.
         */
        fun ClaimPathDomain.isPrefixOf(other: ClaimPathDomain): Boolean {
            if (this.type != other.type) return false

            // The effective prefix length ignores trailing wildcard elements,
            // because a trailing wildcard means "any element inside this array"
            // and the credential may store the array as a single leaf claim.
            val effectiveSize =
                this.value.indexOfLast { it !is ClaimPathElement.AllArrayElements } + 1
            if (effectiveSize > other.value.size) return false

            val compareSize = minOf(this.value.size, other.value.size)
            return this.value.take(compareSize).zip(other.value.take(compareSize))
                .all { (a, b) -> a in b || b in a }
        }
    }

    val joined: String
        get() = value.joinToString(PATH_SEPARATOR) { it.toString() }

    fun toId(docId: String): String {
        val namespaceOrNull: String? = when (type) {
            is ClaimType.MsoMdoc -> type.namespace
            is ClaimType.SdJwtVc -> null
            is ClaimType.Unknown -> null
        }

        val finalId: String = buildList {
            add(docId)
            namespaceOrNull?.let { safeNamespace ->
                add(safeNamespace)
            }
            addAll(value.map { it.toString() })
        }.joinToString(separator = PATH_SEPARATOR)

        return finalId
    }
}