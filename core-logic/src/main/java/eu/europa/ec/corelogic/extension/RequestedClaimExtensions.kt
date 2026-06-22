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
import eu.europa.ec.corelogic.model.ClaimPathDomain.Companion.toClaimPathDomain
import eu.europa.ec.corelogic.model.ClaimPathSegment
import eu.europa.ec.corelogic.model.ClaimType
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import org.multipaz.request.JsonRequestedClaim
import org.multipaz.request.MdocRequestedClaim
import org.multipaz.request.RequestedClaim

/** Maps the verifier's [RequestedClaim] to the app's [ClaimPathDomain]; throws on malformed input. */
fun RequestedClaim.toClaimPath(): ClaimPathDomain {
    return when (this) {
        is MdocRequestedClaim -> {
            listOf(ClaimPathSegment.Key(dataElementName))
                .toClaimPathDomain(type = ClaimType.MsoMdoc(namespace = namespaceName))
        }

        is JsonRequestedClaim -> {
            claimPath
                // an empty path would match every stored claim (nothing to compare), so reject it
                .ifEmpty {
                    error("Empty claim path in verifier request: a DCQL claims path must contain at least one element")
                }
                .map { element ->
                    when (element) {
                        is JsonNull -> {
                            ClaimPathSegment.AllElements
                        }

                        is JsonPrimitive if element.isString -> {
                            ClaimPathSegment.Key(name = element.content)
                        }

                        is JsonPrimitive -> {
                            val index = element.intOrNull
                                ?: error("Numeric claim path element is not a valid integer: $element")
                            ClaimPathSegment.Index(index)
                        }

                        else -> {
                            error("Unsupported claim path element: $element")
                        }
                    }
                }
                .toClaimPathDomain(type = ClaimType.SdJwtVc)
        }
    }
}