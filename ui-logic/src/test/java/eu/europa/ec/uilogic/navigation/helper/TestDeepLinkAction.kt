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

package eu.europa.ec.uilogic.navigation.helper

import eu.europa.ec.uilogic.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestDeepLinkAction {

    @Test
    fun `Given each scheme the wallet registers, When parsed, Then it maps to its flow`() {
        assertEquals(DeepLinkType.OPENID4VP, DeepLinkType.parse(BuildConfig.OPENID4VP_SCHEME))
        assertEquals(DeepLinkType.OPENID4VP, DeepLinkType.parse(BuildConfig.EUDI_OPENID4VP_SCHEME))
        assertEquals(DeepLinkType.OPENID4VP, DeepLinkType.parse(BuildConfig.MDOC_OPENID4VP_SCHEME))
        assertEquals(DeepLinkType.OPENID4VP, DeepLinkType.parse(BuildConfig.HAIP_OPENID4VP_SCHEME))
        assertEquals(
            DeepLinkType.CREDENTIAL_OFFER,
            DeepLinkType.parse(BuildConfig.CREDENTIAL_OFFER_SCHEME)
        )
        assertEquals(
            DeepLinkType.CREDENTIAL_OFFER,
            DeepLinkType.parse(BuildConfig.CREDENTIAL_OFFER_HAIP_SCHEME)
        )
        assertEquals(
            DeepLinkType.ISSUANCE,
            DeepLinkType.parse(
                BuildConfig.ISSUE_AUTHORIZATION_SCHEME,
                BuildConfig.ISSUE_AUTHORIZATION_HOST
            )
        )
        assertEquals(
            DeepLinkType.RQES,
            DeepLinkType.parse(
                BuildConfig.RQES_SCHEME,
                BuildConfig.RQES_HOST,
                BuildConfig.RQES_PATH
            )
        )
    }

    @Test
    fun `Given a known scheme with the wrong host, When parsed, Then it starts no flow`() {
        assertEquals(
            DeepLinkType.EXTERNAL,
            DeepLinkType.parse(BuildConfig.RQES_SCHEME, "attacker.example", BuildConfig.RQES_PATH)
        )
        assertEquals(
            DeepLinkType.EXTERNAL,
            DeepLinkType.parse(BuildConfig.ISSUE_AUTHORIZATION_SCHEME, "attacker.example")
        )
    }

    @Test
    fun `Given a configured document-retrieval host, When parsed, Then only that host starts the signing flow`() {
        val configured = "docs.qtsp.example"

        assertEquals(
            DeepLinkType.RQES_DOC_RETRIEVAL,
            DeepLinkType.parse(
                BuildConfig.RQES_DOC_RETRIEVAL_SCHEME,
                configured,
                rqesDocRetrievalHost = configured
            )
        )
        assertEquals(
            DeepLinkType.EXTERNAL,
            DeepLinkType.parse(
                BuildConfig.RQES_DOC_RETRIEVAL_SCHEME,
                "attacker.example",
                rqesDocRetrievalHost = configured
            )
        )
        assertEquals(
            DeepLinkType.RQES_DOC_RETRIEVAL,
            DeepLinkType.parse(
                BuildConfig.RQES_DOC_RETRIEVAL_SCHEME,
                "attacker.example",
                rqesDocRetrievalHost = DeepLinkType.ANY_HOST,
            )
        )
    }

    @Test
    fun `Given a known host in another case, When parsed, Then it starts its flow`() {
        assertEquals(
            DeepLinkType.ISSUANCE,
            DeepLinkType.parse(
                BuildConfig.ISSUE_AUTHORIZATION_SCHEME,
                BuildConfig.ISSUE_AUTHORIZATION_HOST.uppercase()
            )
        )
        assertEquals(
            DeepLinkType.RQES,
            DeepLinkType.parse(
                BuildConfig.RQES_SCHEME,
                BuildConfig.RQES_HOST.uppercase(),
                BuildConfig.RQES_PATH
            )
        )
    }

    @Test
    fun `Given the signing callback's scheme and host with another path or none, When parsed, Then it starts no flow`() {
        assertEquals(
            DeepLinkType.EXTERNAL,
            DeepLinkType.parse(BuildConfig.RQES_SCHEME, BuildConfig.RQES_HOST, "/other")
        )
        assertEquals(
            DeepLinkType.EXTERNAL,
            DeepLinkType.parse(BuildConfig.RQES_SCHEME, BuildConfig.RQES_HOST)
        )
    }

    @Test
    fun `Given a configured host, When a host is matched, Then only that host matches, in any case`() {
        assertTrue(
            DeepLinkType.hostMatches(
                configured = "docs.qtsp.example",
                host = "docs.qtsp.example"
            )
        )
        assertTrue(
            DeepLinkType.hostMatches(
                configured = "docs.qtsp.example",
                host = "DOCS.QTSP.example"
            )
        )
        assertFalse(
            DeepLinkType.hostMatches(
                configured = "docs.qtsp.example",
                host = "attacker.example"
            )
        )
        assertFalse(DeepLinkType.hostMatches(configured = "docs.qtsp.example", host = null))
        assertFalse(DeepLinkType.hostMatches(configured = null, host = "docs.qtsp.example"))
    }

    @Test
    fun `Given the any-host sentinel, When a host is matched, Then every host matches`() {
        assertTrue(
            DeepLinkType.hostMatches(
                configured = DeepLinkType.ANY_HOST,
                host = "attacker.example"
            )
        )
        assertTrue(DeepLinkType.hostMatches(configured = DeepLinkType.ANY_HOST, host = null))
    }
}