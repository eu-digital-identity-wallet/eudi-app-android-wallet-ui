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

import eu.europa.ec.corelogic.extension.toClaimPath
import eu.europa.ec.corelogic.model.ClaimPathDomain.Companion.isPrefixOf
import eu.europa.ec.eudi.iso18013.transfer.response.device.MsoMdocItem
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPath
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPathElement
import eu.europa.ec.eudi.wallet.transfer.openId4vp.SdJwtVcItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestClaimPath {

    /** Helper: create a SdJwtVcItem with Claim path elements from strings. */
    private fun sdJwtVc(vararg names: String) =
        SdJwtVcItem(ClaimPath(names.map { ClaimPathElement.Claim(it) })).toClaimPath()

    /** Helper: create a SdJwtVcItem with mixed ClaimPathElements. */
    private fun sdJwtVcPath(vararg elements: ClaimPathElement) =
        SdJwtVcItem(ClaimPath(elements.toList())).toClaimPath()

    @Test
    fun `SdJwt - prefix match with deeper nesting`() {
        val requested1 = sdJwtVc("address")
        val requested2 = sdJwtVc("address", "formatted")

        val available1 = sdJwtVc("address", "country")
        val available2 = sdJwtVc("address", "street")
        val available3 = sdJwtVc("address", "street", "number")
        val available4 = sdJwtVc("addressA", "street")
        val available5 = sdJwtVc("addres", "street")
        val available6 = sdJwtVc("address", "formattedNo")

        assertTrue(requested1.isPrefixOf(available1))
        assertTrue(requested1.isPrefixOf(available2))
        assertTrue(requested1.isPrefixOf(available3))
        assertFalse(requested1.isPrefixOf(available4))
        assertFalse(requested1.isPrefixOf(available5))
        assertFalse(requested2.isPrefixOf(available6))
    }

    @Test
    fun `SdJwt - exact match returns true`() {
        val requested1 = sdJwtVc("family_name")
        val requested2 = sdJwtVc("family_name", "birth")

        val available1 = sdJwtVc("family_name")
        val available2 = sdJwtVc("family_name", "birth")

        assertTrue(requested1.isPrefixOf(available1))
        assertTrue(requested1.isPrefixOf(available2))
        assertFalse(requested2.isPrefixOf(available1))
        assertTrue(requested2.isPrefixOf(available2))
    }

    @Test
    fun `SdJwt - non-prefix should return false`() {
        val requested1 = sdJwtVc("family_name")
        val requested2 = sdJwtVc("family_name_birth")

        val available1 = sdJwtVc("family_name_birth")
        val available2 = sdJwtVc("family_name")

        assertFalse(requested1.isPrefixOf(available1))
        assertFalse(requested2.isPrefixOf(available2))
    }

    @Test
    fun `SdJwt - partial match but diverges deeper`() {
        val requested = sdJwtVc("claim1", "claim1_a", "claim1_b")

        val match1 = sdJwtVc("claim1", "claim1_a", "claim1_b", "claim1_c")
        val match2 = sdJwtVc("claim1", "claim1_a", "claim1_b", "claim2_c")
        val nonMatch = sdJwtVc("claim1", "claim1_a", "claim33_b", "claim2_c")

        assertTrue(requested.isPrefixOf(match1))
        assertTrue(requested.isPrefixOf(match2))
        assertFalse(requested.isPrefixOf(nonMatch))
    }

    @Test
    fun `SdJwt - requested path longer than available returns false`() {
        val requested = sdJwtVc("a", "b", "c")
        val available = sdJwtVc("a", "b")

        assertFalse(requested.isPrefixOf(available))
    }

    @Test
    fun `SdJwt - empty requested path is prefix of any`() {
        val requested = SdJwtVcItem(ClaimPath(emptyList())).toClaimPath()
        val available = sdJwtVc("anything", "deep")

        assertTrue(requested.isPrefixOf(available))
    }

    @Test
    fun `MsoMdoc - exact match`() {
        val requested =
            MsoMdocItem(namespace = "ns", elementIdentifier = "family_name").toClaimPath()
        val available =
            MsoMdocItem(namespace = "ns", elementIdentifier = "family_name").toClaimPath()

        assertTrue(requested.isPrefixOf(available))
    }

    @Test
    fun `MsoMdoc - partial match`() {
        val requested =
            MsoMdocItem(namespace = "ns", elementIdentifier = "family_name").toClaimPath()
        val available =
            MsoMdocItem(namespace = "ns", elementIdentifier = "family_name_birth").toClaimPath()

        assertFalse(requested.isPrefixOf(available))
    }

    @Test
    fun `MsoMdoc - different identifiers`() {
        val requested = MsoMdocItem(namespace = "ns", elementIdentifier = "name").toClaimPath()
        val available = MsoMdocItem(namespace = "ns", elementIdentifier = "surname").toClaimPath()

        assertFalse(requested.isPrefixOf(available))
    }

    @Test
    fun `MsoMdoc - different namespace`() {
        val requested =
            MsoMdocItem(namespace = "ns1", elementIdentifier = "family_name").toClaimPath()
        val available =
            MsoMdocItem(namespace = "ns2", elementIdentifier = "family_name").toClaimPath()

        assertFalse(requested.isPrefixOf(available))
    }

    @Test
    fun `MsoMdoc - namespace as prefix of sdjwt path`() {
        val msoClaimPath =
            MsoMdocItem(namespace = "address", elementIdentifier = "city").toClaimPath()
        val sdjwtClaimPath = sdJwtVc("address", "city")

        assertFalse(msoClaimPath.isPrefixOf(sdjwtClaimPath))
        assertFalse(sdjwtClaimPath.isPrefixOf(msoClaimPath))
    }

    @Test
    fun `SdJwt - trailing null wildcard matches array leaf claim`() {
        val requested = sdJwtVcPath(
            ClaimPathElement.Claim("nationality"),
            ClaimPathElement.AllArrayElements
        )
        val available = sdJwtVc("nationality")

        assertTrue(requested.isPrefixOf(available))
    }

    @Test
    fun `SdJwt - null wildcard in middle matches any array index`() {
        val requested = sdJwtVcPath(
            ClaimPathElement.Claim("addresses"),
            ClaimPathElement.AllArrayElements,
            ClaimPathElement.Claim("city")
        )

        val match1 = sdJwtVcPath(
            ClaimPathElement.Claim("addresses"),
            ClaimPathElement.ArrayElement(0),
            ClaimPathElement.Claim("city")
        )
        val match2 = sdJwtVcPath(
            ClaimPathElement.Claim("addresses"),
            ClaimPathElement.ArrayElement(1),
            ClaimPathElement.Claim("city")
        )
        val nonMatch = sdJwtVcPath(
            ClaimPathElement.Claim("addresses"),
            ClaimPathElement.ArrayElement(0),
            ClaimPathElement.Claim("street")
        )

        assertTrue(requested.isPrefixOf(match1))
        assertTrue(requested.isPrefixOf(match2))
        assertFalse(requested.isPrefixOf(nonMatch))
    }

    @Test
    fun `SdJwt - null wildcard as prefix matches deeper nested paths`() {
        val requested = sdJwtVcPath(
            ClaimPathElement.Claim("addresses"),
            ClaimPathElement.AllArrayElements
        )

        val match1 = sdJwtVcPath(
            ClaimPathElement.Claim("addresses"),
            ClaimPathElement.ArrayElement(0),
            ClaimPathElement.Claim("city")
        )
        val match2 = sdJwtVc("addresses")

        assertTrue(requested.isPrefixOf(match1))
        assertTrue(requested.isPrefixOf(match2))
    }

    @Test
    fun `SdJwt - non-matching claim with null wildcard`() {
        val requested = sdJwtVcPath(
            ClaimPathElement.Claim("nationality"),
            ClaimPathElement.AllArrayElements
        )
        val available = sdJwtVc("family_name")

        assertFalse(requested.isPrefixOf(available))
    }
}
