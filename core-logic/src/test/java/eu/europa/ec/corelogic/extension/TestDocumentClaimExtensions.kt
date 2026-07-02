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
import eu.europa.ec.eudi.wallet.document.format.MsoMdocClaim
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcClaim
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TestDocumentClaimExtensions {

    @Test
    fun `an SD-JWT array claim expands to one typed Index leaf per element`() {
        // Given a stored "nationalities" array with two elements, modelled as the SDK does:
        // a parent object-key claim whose children carry ArrayElement path segments.
        val nationalities = sdJwtClaim(
            pathElement = ClaimPathElement.Claim("nationalities"),
            children = listOf(
                sdJwtClaim(pathElement = ClaimPathElement.ArrayElement(0)),
                sdJwtClaim(pathElement = ClaimPathElement.ArrayElement(1)),
            ),
        )

        // When
        val paths = nationalities.toClaimPaths()

        // Then each element is addressable on its own, with a typed Index segment.
        assertEquals(
            listOf(
                ClaimPathDomain(
                    segments = listOf(
                        ClaimPathSegment.Key("nationalities"),
                        ClaimPathSegment.Index(0),
                    ),
                    type = ClaimType.SdJwtVc,
                ),
                ClaimPathDomain(
                    segments = listOf(
                        ClaimPathSegment.Key("nationalities"),
                        ClaimPathSegment.Index(1),
                    ),
                    type = ClaimType.SdJwtVc,
                ),
            ),
            paths,
        )
    }

    @Test
    fun `a flat SD-JWT claim expands to a single Key leaf`() {
        // Given
        val familyName = sdJwtClaim(pathElement = ClaimPathElement.Claim("family_name"))

        // When
        val paths = familyName.toClaimPaths()

        // Then
        assertEquals(
            listOf(
                ClaimPathDomain(
                    segments = listOf(ClaimPathSegment.Key("family_name")),
                    type = ClaimType.SdJwtVc,
                )
            ),
            paths,
        )
    }

    @Test
    fun `a nested SD-JWT object claim expands to the full Key path of each leaf`() {
        // Given an "address" object with a single "country" leaf.
        val address = sdJwtClaim(
            pathElement = ClaimPathElement.Claim("address"),
            children = listOf(
                sdJwtClaim(pathElement = ClaimPathElement.Claim("country")),
            ),
        )

        // When
        val paths = address.toClaimPaths()

        // Then
        assertEquals(
            listOf(
                ClaimPathDomain(
                    segments = listOf(
                        ClaimPathSegment.Key("address"),
                        ClaimPathSegment.Key("country"),
                    ),
                    type = ClaimType.SdJwtVc,
                )
            ),
            paths,
        )
    }

    @Test
    fun `an mso_mdoc claim expands to a single Key leaf carrying the namespace in its type`() {
        // Given
        val familyName = mdocClaim(nameSpace = "org.iso.18013.5.1", dataElementName = "family_name")

        // When
        val paths = familyName.toClaimPaths()

        // Then the namespace lives in the ClaimType, not in the path segments.
        assertEquals(
            listOf(
                ClaimPathDomain(
                    segments = listOf(ClaimPathSegment.Key("family_name")),
                    type = ClaimType.MsoMdoc(namespace = "org.iso.18013.5.1"),
                )
            ),
            paths,
        )
    }

    //region identifierString

    @Test
    fun `identifierString of an mso_mdoc claim is its dataElementName`() {
        // Given
        val claim = mdocClaim(nameSpace = "org.iso.18013.5.1", dataElementName = "family_name")

        // When / Then
        assertEquals("family_name", claim.identifierString)
    }

    @Test
    fun `identifierString of an SD-JWT object-key claim is the key as-is`() {
        // Given
        val claim = sdJwtClaim(pathElement = ClaimPathElement.Claim("place_of_birth"))

        // When / Then
        assertEquals("place_of_birth", claim.identifierString)
    }

    @Test
    fun `identifierString of an SD-JWT array-element claim is the integer string of its index`() {
        // Given
        val claim = sdJwtClaim(pathElement = ClaimPathElement.ArrayElement(0))

        // When / Then
        assertEquals("0", claim.identifierString)
    }

    // wildcard and a hypothetical Key("null") both stringify to "null"; identifierString does
    // not distinguish them
    @Test
    fun `identifierString of an SD-JWT wildcard claim is the literal null string`() {
        // Given
        val claim = sdJwtClaim(pathElement = ClaimPathElement.AllArrayElements)

        // When / Then
        assertEquals("null", claim.identifierString)
    }

    //endregion

    //region helpers

    private fun sdJwtClaim(
        pathElement: ClaimPathElement,
        children: List<SdJwtVcClaim> = emptyList(),
    ): SdJwtVcClaim {
        val claim = mock<SdJwtVcClaim>()
        whenever(claim.pathElement).thenReturn(pathElement)
        whenever(claim.children).thenReturn(children)
        return claim
    }

    private fun mdocClaim(
        nameSpace: String,
        dataElementName: String,
    ): MsoMdocClaim {
        val claim = mock<MsoMdocClaim>()
        whenever(claim.nameSpace).thenReturn(nameSpace)
        whenever(claim.dataElementName).thenReturn(dataElementName)
        return claim
    }

    //endregion
}