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
import eu.europa.ec.corelogic.model.ClaimPath.Companion.isPrefixOf
import eu.europa.ec.eudi.iso18013.transfer.response.device.MsoMdocItem
import eu.europa.ec.eudi.wallet.transfer.openId4vp.SdJwtVcItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestClaimPath {

    @Test
    fun `SdJwt - prefix match with deeper nesting`() {
        val requested1 = SdJwtVcItem(listOf("address")).toClaimPath()
        val requested2 = SdJwtVcItem(listOf("address", "formatted")).toClaimPath()

        val available1 = SdJwtVcItem(listOf("address", "country")).toClaimPath()
        val available2 = SdJwtVcItem(listOf("address", "street")).toClaimPath()
        val available3 = SdJwtVcItem(listOf("address", "street", "number")).toClaimPath()
        val available4 = SdJwtVcItem(listOf("addressA", "street")).toClaimPath()
        val available5 = SdJwtVcItem(listOf("addres", "street")).toClaimPath()
        val available6 = SdJwtVcItem(listOf("address", "formattedNo")).toClaimPath()

        assertTrue(requested1.isPrefixOf(available1))
        assertTrue(requested1.isPrefixOf(available2))
        assertTrue(requested1.isPrefixOf(available3))
        assertFalse(requested1.isPrefixOf(available4))
        assertFalse(requested1.isPrefixOf(available5))
        assertFalse(requested2.isPrefixOf(available6))
    }

    @Test
    fun `SdJwt - exact match returns true`() {
        val requested1 = SdJwtVcItem(listOf("family_name")).toClaimPath()
        val requested2 = SdJwtVcItem(listOf("family_name", "birth")).toClaimPath()

        val available1 = SdJwtVcItem(listOf("family_name")).toClaimPath()
        val available2 = SdJwtVcItem(listOf("family_name", "birth")).toClaimPath()

        assertTrue(requested1.isPrefixOf(available1))
        assertTrue(requested1.isPrefixOf(available2))
        assertFalse(requested2.isPrefixOf(available1))
        assertTrue(requested2.isPrefixOf(available2))
    }

    @Test
    fun `SdJwt - non-prefix should return false`() {
        val requested1 = SdJwtVcItem(listOf("family_name")).toClaimPath()
        val requested2 = SdJwtVcItem(listOf("family_name_birth")).toClaimPath()

        val available1 = SdJwtVcItem(listOf("family_name_birth")).toClaimPath()
        val available2 = SdJwtVcItem(listOf("family_name")).toClaimPath()

        assertFalse(requested1.isPrefixOf(available1))
        assertFalse(requested2.isPrefixOf(available2))
    }

    @Test
    fun `SdJwt - partial match but diverges deeper`() {
        val requested = SdJwtVcItem(listOf("claim1", "claim1_a", "claim1_b")).toClaimPath()

        val match1 = SdJwtVcItem(listOf("claim1", "claim1_a", "claim1_b", "claim1_c")).toClaimPath()
        val match2 = SdJwtVcItem(listOf("claim1", "claim1_a", "claim1_b", "claim2_c")).toClaimPath()
        val nonMatch =
            SdJwtVcItem(listOf("claim1", "claim1_a", "claim33_b", "claim2_c")).toClaimPath()

        assertTrue(requested.isPrefixOf(match1))
        assertTrue(requested.isPrefixOf(match2))
        assertFalse(requested.isPrefixOf(nonMatch))
    }

    @Test
    fun `SdJwt - requested path longer than available returns false`() {
        val requested = SdJwtVcItem(listOf("a", "b", "c")).toClaimPath()
        val available = SdJwtVcItem(listOf("a", "b")).toClaimPath()

        assertFalse(requested.isPrefixOf(available))
    }

    @Test
    fun `SdJwt - empty requested path is prefix of any`() {
        val requested = SdJwtVcItem(emptyList()).toClaimPath()
        val available = SdJwtVcItem(listOf("anything", "deep")).toClaimPath()

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

        assertTrue(requested.isPrefixOf(available))
    }

    @Test
    fun `MsoMdoc - namespace as prefix of sdjwt path`() {
        val msoClaimPath =
            MsoMdocItem(namespace = "address", elementIdentifier = "city").toClaimPath()
        val sdjwtClaimPath = SdJwtVcItem(listOf("address", "city")).toClaimPath()

        assertFalse(msoClaimPath.isPrefixOf(sdjwtClaimPath))
        assertFalse(sdjwtClaimPath.isPrefixOf(msoClaimPath))
    }
}