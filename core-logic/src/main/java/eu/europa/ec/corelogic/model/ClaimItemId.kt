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

/**
 * Stable identity of a single request-screen row — a document card header
 * ([DocumentHeader]) or a claim row ([Claim]).
 *
 * Identity is structural: two ids are equal iff their typed fields are equal, so a header and a
 * claim, `Key("0")` and `Index(0)`, or `queryId = null` and `queryId = ""` can never collide.
 *
 * [encode] flattens it to the `String` the UI keys each row by — deterministic (the request flow
 * re-encodes a stored claim path to recover a row's id) and only ever compared for
 * equality and rebuilt, never parsed back.
 */
sealed interface ClaimItemId {

    /** Flatten to the row-key string; see the type doc for the guarantees. */
    fun encode(): String

    /**
     * The document-level card header. Identity is `(docId, queryId)`, so the same physical
     * document matched by two DCQL queries renders as two headers with distinct ids.
     */
    data class DocumentHeader(
        val docId: String,
        val queryId: String?,
    ) : ClaimItemId {
        override fun encode(): String = encodeParts(
            buildList {
                add(KIND_HEADER)
                add(docId)
                addAll(queryIdParts(queryId))
            }
        )
    }

    /**
     * A claim row. Identity is `(docId, queryId, path)` — [path] carries the credential
     * [type][ClaimType] and the typed [segments][ClaimPathSegment], so two queries targeting
     * the same physical document, or the same claim name under different formats, stay distinct.
     */
    data class Claim(
        val docId: String,
        val queryId: String?,
        val path: ClaimPathDomain,
    ) : ClaimItemId {
        override fun encode(): String = encodeParts(
            buildList {
                add(KIND_CLAIM)
                add(docId)
                addAll(queryIdParts(queryId))
                addAll(typeParts(path.type))
                path.segments.forEach { segment -> addAll(segmentParts(segment)) }
            }
        )
    }
}

/**
 * Length-prefixed encoding: each part is emitted as `<length>:<value>`. Because every part
 * declares its own length, no part can be mistaken for another regardless of its content
 * (commas, digits, or the kind tokens themselves), so the concatenation is injective with no
 * escaping. We never decode it — equality of the encoded string is all the UI needs.
 */
private fun encodeParts(parts: List<String>): String =
    buildString {
        parts.forEach { part ->
            append(part.length).append(LENGTH_VALUE_SEPARATOR).append(part)
        }
    }

private fun queryIdParts(queryId: String?): List<String> =
    if (queryId == null) {
        listOf(QUERY_ABSENT)
    } else {
        listOf(QUERY_PRESENT, queryId)
    }

private fun typeParts(type: ClaimType): List<String> =
    when (type) {
        is ClaimType.SdJwtVc -> listOf(TYPE_SD_JWT_VC)
        is ClaimType.MsoMdoc -> listOf(TYPE_MSO_MDOC, type.namespace)
    }

private fun segmentParts(segment: ClaimPathSegment): List<String> =
    when (segment) {
        is ClaimPathSegment.Key -> listOf(SEGMENT_KEY, segment.name)
        is ClaimPathSegment.Index -> listOf(SEGMENT_INDEX, segment.index.toString())
        is ClaimPathSegment.AllElements -> listOf(SEGMENT_WILDCARD)
    }

// Separates each part's decimal length from its value. Must be a non-digit so the boundary
// between the length and the value is unambiguous — that is what keeps the encoding injective.
private const val LENGTH_VALUE_SEPARATOR = ':'

// Structural discriminators — one per variant / type / segment kind. They only need to be
// pairwise distinct; the length prefix keeps them from colliding with arbitrary docId /
// name / namespace values, so a claim named "header" or "index" is still unambiguous.
private const val KIND_HEADER = "header"
private const val KIND_CLAIM = "claim"
private const val QUERY_ABSENT = "query-absent"
private const val QUERY_PRESENT = "query"
private const val TYPE_SD_JWT_VC = "sd-jwt-vc"
private const val TYPE_MSO_MDOC = "mso-mdoc"
private const val SEGMENT_KEY = "key"
private const val SEGMENT_INDEX = "index"
private const val SEGMENT_WILDCARD = "wildcard"