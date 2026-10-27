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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestExternalLinkHelper {

    @Test
    fun `Given an https URL with a host, Then it may be opened`() {
        listOf(
            "https://verifier.example/done",
            "HTTPS://verifier.example:8443/done?response_code=abc#top",
        ).forEach { redirect ->
            assertTrue(redirect, isOpenableExternalLink(redirect))
        }
    }

    @Test
    fun `Given any other scheme or a malformed address, Then it is ignored`() {
        listOf(
            "eudi-rqes://sign?request_uri=https%3A%2F%2Fattacker.example",
            "openid-credential-offer://?credential_offer_uri=https%3A%2F%2Fattacker.example",
            "haip-vci://?credential_offer_uri=https%3A%2F%2Fattacker.example",
            "openid4vp://?request_uri=https%3A%2F%2Fattacker.example",
            "eudi-wallet://dashboard",
            "rqes://oauth/callback?code=forged",
            "eu.europa.ec.euidi://authorization?code=forged",
            "http://verifier.example/done",
            "intent://verifier.example#Intent;scheme=https;end",
            "javascript:alert(1)",
            "file:///data/data/eu.europa.ec.euidi/files/logs",
            "https:///no-host",
            "https://verifier.example/bad path",
            "",
        ).forEach { redirect ->
            assertFalse(redirect, isOpenableExternalLink(redirect))
        }
    }
}