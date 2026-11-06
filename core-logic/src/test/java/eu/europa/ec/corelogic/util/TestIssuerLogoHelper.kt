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

package eu.europa.ec.corelogic.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.URI

class TestIssuerLogoHelper {

    private val issuer = "https://issuer.example"

    @Test
    fun `Given an https logo on the issuer's origin, Then it may be loaded`() {
        listOf(
            "https://issuer.example/logo.png",
            "https://ISSUER.example:443/assets/logo.svg",
        ).forEach { logo ->
            assertEquals(logo, URI(logo), issuerLogoOrNull(logo = URI(logo), issuerId = issuer))
        }
    }

    @Test
    fun `Given a logo elsewhere or on a local scheme, Then it is not loaded`() {
        listOf(
            "https://cdn.example/logo.png",
            "https://issuer.example:8443/logo.png",
            "https://issuer.example.attacker.example/logo.png",
            "http://issuer.example/logo.png",
            "content://eu.europa.ec.euidi.provider/logs/log.txt",
            "file:///data/data/eu.europa.ec.euidi/files/logs/log.txt",
            "android.resource://eu.europa.ec.euidi/drawable/logo",
            "logo.png",
        ).forEach { logo ->
            assertNull(logo, issuerLogoOrNull(logo = URI(logo), issuerId = issuer))
        }
    }

    @Test
    fun `Given an origin configured for the issuer, Then a logo there may be loaded and one elsewhere not`() {
        val origins = listOf("https://cdn.example", "http://plain.example", "not a url")

        assertEquals(
            URI("https://cdn.example/logo.svg"),
            issuerLogoOrNull(
                logo = URI("https://cdn.example/logo.svg"),
                issuerId = issuer,
                additionalOrigins = origins
            )
        )
        listOf(
            "https://other.example/logo.svg",
            "https://plain.example/logo.svg",
            "http://cdn.example/logo.svg",
        ).forEach { logo ->
            assertNull(
                logo,
                issuerLogoOrNull(logo = URI(logo), issuerId = issuer, additionalOrigins = origins)
            )
        }
    }

    @Test
    fun `Given no logo, no issuer or an issuer that is not https, Then nothing is loaded`() {
        val logo = URI("https://issuer.example/logo.png")

        assertNull(issuerLogoOrNull(logo = null, issuerId = issuer))
        assertNull(issuerLogoOrNull(logo = logo, issuerId = null))
        assertNull(issuerLogoOrNull(logo = logo, issuerId = "http://issuer.example"))
        assertNull(issuerLogoOrNull(logo = logo, issuerId = "not a url"))
    }
}