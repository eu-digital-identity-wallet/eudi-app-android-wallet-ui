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

package eu.europa.ec.corelogic.extension

import eu.europa.ec.corelogic.model.ClaimPathDomain
import eu.europa.ec.corelogic.model.ClaimPathSegment
import eu.europa.ec.corelogic.model.ClaimType
import eu.europa.ec.eudi.sdjwt.vc.ClaimPathElement
import eu.europa.ec.eudi.wallet.document.format.DocumentClaim
import eu.europa.ec.eudi.wallet.document.format.MsoMdocClaim
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcClaim

/**
 * Maps an SD-JWT VC library [ClaimPathElement] to [ClaimPathSegment], keeping
 * core-logic decoupled from the library's claim-path representation.
 */
fun ClaimPathElement.toClaimPathSegment(): ClaimPathSegment = when (this) {
    is ClaimPathElement.Claim -> ClaimPathSegment.Key(name)
    is ClaimPathElement.ArrayElement -> ClaimPathSegment.Index(index)
    is ClaimPathElement.AllArrayElements -> ClaimPathSegment.AllElements
}

/**
 * String id for a [DocumentClaim], for UI keys / string look-ups:
 *  - [SdJwtVcClaim] -> the path segment's string form (object keys as-is, array indices as
 *    their integer string, the wildcard as the literal `"null"`).
 *  - [MsoMdocClaim] -> `dataElementName`.
 */
val DocumentClaim.identifierString: String
    get() = when (this) {
        is SdJwtVcClaim -> pathElement.toClaimPathSegment().toString()
        is MsoMdocClaim -> dataElementName
    }

/**
 * Recursively expands a [DocumentClaim] into the leaf paths the wallet stores for it.
 *
 * SD-JWT claims recurse into their children; an array child (e.g. a `nationalities` collection)
 * contributes typed [ClaimPathSegment.Index] entries, so per-element addressing appears in the
 * paths. An mdoc claim is a single [ClaimPathSegment.Key] holding the `dataElementName`, with the
 * namespace carried in [ClaimType.MsoMdoc].
 */
fun DocumentClaim.toClaimPaths(
    parentPath: List<ClaimPathSegment> = emptyList()
): List<ClaimPathDomain> {
    return when (this) {
        is SdJwtVcClaim -> {
            val currentPath: List<ClaimPathSegment> =
                parentPath + this.pathElement.toClaimPathSegment()
            if (children.isEmpty()) {
                listOf(ClaimPathDomain(segments = currentPath, type = ClaimType.SdJwtVc))
            } else {
                children.flatMap { child ->
                    child.toClaimPaths(parentPath = currentPath)
                }
            }
        }

        is MsoMdocClaim -> {
            listOf(
                ClaimPathDomain(
                    segments = listOf(ClaimPathSegment.Key(this.dataElementName)),
                    type = ClaimType.MsoMdoc(this.nameSpace)
                )
            )
        }
    }
}