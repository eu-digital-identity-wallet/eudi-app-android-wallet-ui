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

    /**
     * Given a stored "nationalities" array with two elements, modelled as the SDK does: a parent
     * object-key claim whose children carry ArrayElement path segments.
     *
     * Then each element is addressable on its own, with a typed Index segment.
     */
    @Test
    fun `Given an SD-JWT array claim, When toClaimPaths is called, Then one typed Index leaf per element is returned`() {
        // Given
        val nationalities = sdJwtClaim(
            pathElement = ClaimPathElement.Claim("nationalities"),
            children = listOf(
                sdJwtClaim(pathElement = ClaimPathElement.ArrayElement(0)),
                sdJwtClaim(pathElement = ClaimPathElement.ArrayElement(1)),
            ),
        )

        // When
        val paths = nationalities.toClaimPaths()

        // Then
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
    fun `Given a flat SD-JWT claim, When toClaimPaths is called, Then a single Key leaf is returned`() {
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

    /**
     * Given an "address" object with a single "country" leaf.
     */
    @Test
    fun `Given a nested SD-JWT object claim, When toClaimPaths is called, Then the full Key path of each leaf is returned`() {
        // Given
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

    /**
     * Then the namespace lives in the ClaimType, not in the path segments.
     */
    @Test
    fun `Given an mso_mdoc claim, When toClaimPaths is called, Then a single Key leaf carries the namespace in its type`() {
        // Given
        val familyName = mdocClaim(nameSpace = "org.iso.18013.5.1", dataElementName = "family_name")

        // When
        val paths = familyName.toClaimPaths()

        // Then
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
    fun `Given an mso_mdoc claim, When identifierString is read, Then it is the dataElementName`() {
        // Given
        val claim = mdocClaim(nameSpace = "org.iso.18013.5.1", dataElementName = "family_name")

        // When
        val identifier = claim.identifierString

        // Then
        assertEquals("family_name", identifier)
    }

    @Test
    fun `Given an SD-JWT object-key claim, When identifierString is read, Then it is the key as-is`() {
        // Given
        val claim = sdJwtClaim(pathElement = ClaimPathElement.Claim("place_of_birth"))

        // When
        val identifier = claim.identifierString

        // Then
        assertEquals("place_of_birth", identifier)
    }

    @Test
    fun `Given an SD-JWT array-element claim, When identifierString is read, Then it is the integer string of its index`() {
        // Given
        val claim = sdJwtClaim(pathElement = ClaimPathElement.ArrayElement(0))

        // When
        val identifier = claim.identifierString

        // Then
        assertEquals("0", identifier)
    }

    // wildcard and a hypothetical Key("null") both stringify to "null"; identifierString does
    // not distinguish them
    @Test
    fun `Given an SD-JWT wildcard claim, When identifierString is read, Then it is the literal null string`() {
        // Given
        val claim = sdJwtClaim(pathElement = ClaimPathElement.AllArrayElements)

        // When
        val identifier = claim.identifierString

        // Then
        assertEquals("null", identifier)
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