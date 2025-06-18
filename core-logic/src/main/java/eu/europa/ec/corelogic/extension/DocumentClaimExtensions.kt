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

package eu.europa.ec.corelogic.extension

import eu.europa.ec.corelogic.model.ClaimPathDomain
import eu.europa.ec.eudi.wallet.document.format.DocumentClaim
import eu.europa.ec.eudi.wallet.document.format.MsoMdocClaim
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcClaim

/**
 * Converts a [DocumentClaim] to a list of [ClaimPathDomain]s.
 *
 * This function recursively traverses the claim structure to generate a list of paths,
 * where each path leads to a specific claim.
 * Each path is represented as a list of strings, with each string being an identifier of a claim in the hierarchy.
 *
 * @param parentPath The path of the parent claim, used for recursive calls to build up the full path. Defaults to an empty list.
 * @return A list of [ClaimPathDomain]s representing the paths to each claim in the [DocumentClaim] structure.
 *
 * For [SdJwtVcClaim]s, the function will:
 *  - Append the current claim's identifier to the `parentPath` to form the `currentPath`.
 *  - If the claim has no children or its value is a collection, a single [ClaimPathDomain] is created with the `currentPath`.
 *  - If the claim has children and its value is not a collection, it recursively calls `toClaimPaths` on each child,
 *    passing the `currentPath`, and flattens the resulting lists of paths.
 *
 * For [MsoMdocClaim]s, the function will:
 * - Create a [ClaimPathDomain] with a list containing only the claim's identifier.
 */
fun DocumentClaim.toClaimPaths(
    parentPath: List<String> = emptyList()
): List<ClaimPathDomain> {
    return when (this) {
        is SdJwtVcClaim -> {
            val currentPath: List<String> = parentPath + this.identifier
            if (children.isEmpty() || this.value is Collection<*>) {
                listOf(ClaimPathDomain(currentPath))
            } else {
                children.flatMap { child ->
                    child.toClaimPaths(currentPath)
                }
            }
        }

        is MsoMdocClaim -> {
            listOf(ClaimPathDomain(listOf(this.identifier)))
        }
    }
}