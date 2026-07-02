/*
 * Copyright (c) 2026 European Commission
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

import eu.europa.ec.eudi.wallet.document.NameSpace

/**
 * A path to a claim within a credential — the app's platform-agnostic claim-path pointer,
 * abstracting over the SD-JWT VC and MSO-mdoc representations so the app can identify, match, and
 * address claims uniformly.
 *
 * @property segments the [ClaimPathSegment]s forming the path (keys, indices, or wildcards).
 * @property type the credential format (SD-JWT VC or MSO-mdoc) and namespace context.
 */
data class ClaimPathDomain(
    val segments: List<ClaimPathSegment>,
    val type: ClaimType,
) {

    companion object {
        fun List<ClaimPathSegment>.toClaimPathDomain(type: ClaimType): ClaimPathDomain {
            return ClaimPathDomain(
                segments = this,
                type = type
            )
        }

        /**
         * Builds a [ClaimPathDomain] of plain object-key segments — the common case where a path
         * has only nested keys (flat SD-JWT VC claims, MSO-mdoc elements), no indices or wildcards.
         */
        fun ofPlainKeys(names: List<String>, type: ClaimType): ClaimPathDomain {
            return ClaimPathDomain(
                segments = names.map {
                    ClaimPathSegment.Key(name = it)
                },
                type = type
            )
        }

        /**
         * Builds a [ClaimPathDomain] for a UI-only group node in the rendered claim tree. It is
         * never matched against a real disclosure path; it only gives the group a stable identity,
         * so [groupId] must be unique within the tree.
         */
        fun forUiGroup(groupId: String, type: ClaimType): ClaimPathDomain {
            return ClaimPathDomain(
                segments = listOf(
                    ClaimPathSegment.Key(name = groupId)
                ),
                type = type
            )
        }

        /** Wildcard matches any index; index and key match the same kind by value. */
        private fun segmentMatches(a: ClaimPathSegment, b: ClaimPathSegment): Boolean = when (a) {
            is ClaimPathSegment.AllElements -> b is ClaimPathSegment.Index
            is ClaimPathSegment.Index -> b is ClaimPathSegment.Index && a.index == b.index
            is ClaimPathSegment.Key -> b is ClaimPathSegment.Key && a.name == b.name
        }
    }

    /**
     * Returns `true` when this requested path *matches* the stored path [other], called as
     * `requested.matches(stored)`. Matches on the shared prefix in either direction, a wildcard
     * matches any index, and an empty path matches nothing.
     *
     * Examples (read as `requested matches stored`):
     * ```
     * ["address"]                        matches ["address", "city"]               → true  (descendant: parent asked, leaf stored beneath it)
     * ["nationalities", 0]               matches ["nationalities"]                 → true  (ancestor)
     * ["nationalities", 0]               matches ["nationalities", 0]              → true  (exact)
     * ["nationalities", 0]               matches ["nationalities", 1]              → false (sibling)
     * ["nationalities", null]            matches ["nationalities", 0]              → true  (wildcard)
     * ["nationalities", null]            matches ["nationalities", 1]              → true  (wildcard)
     * ["addresses", null, "city"]        matches ["addresses", 0, "city"]          → true
     * ["addresses", null, "city"]        matches ["addresses", 0, "street"]        → false
     * ```
     */
    fun matches(other: ClaimPathDomain): Boolean {
        if (type != other.type) return false
        // empty path matches nothing: a length-0 shared prefix would match every path of the type
        if (segments.isEmpty() || other.segments.isEmpty()) return false
        val common = minOf(segments.size, other.segments.size)
        for (i in 0 until common) {
            if (!segmentMatches(segments[i], other.segments[i])) return false
        }
        return true
    }

}

sealed interface ClaimType {
    data object SdJwtVc : ClaimType
    data class MsoMdoc(val namespace: NameSpace) : ClaimType
}

/** A single step in a [ClaimPathDomain]. */
sealed interface ClaimPathSegment {

    /** An object key, e.g. `"family_name"`. */
    data class Key(val name: String) : ClaimPathSegment {
        override fun toString(): String = name
    }

    /** A concrete array index, e.g. `0`. */
    data class Index(val index: Int) : ClaimPathSegment {
        override fun toString(): String = index.toString()
    }

    /** The `null` wildcard — matches every index of the targeted array. */
    data object AllElements : ClaimPathSegment {
        override fun toString(): String = "null"
    }
}