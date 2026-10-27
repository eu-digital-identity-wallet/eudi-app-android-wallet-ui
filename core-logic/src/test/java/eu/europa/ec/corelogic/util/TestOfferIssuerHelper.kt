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
import org.junit.Test
import java.net.URLEncoder

class TestOfferIssuerHelper {

    private val configured = listOf("https://issuer.example", "https://second.example:8443")

    @Test
    fun `Given an offer by value from a configured issuer, Then that issuer is returned`() {
        assertEquals(
            OfferIssuer.Configured("https://issuer.example"),
            offerIssuerOf(offerByValue("https://issuer.example"), configured)
        )
        assertEquals(
            OfferIssuer.Configured("https://issuer.example"),
            offerIssuerOf(
                offerByValue(
                    "https://issuer.example",
                    prefix = "haip-vci://credential_offer?"
                ), configured
            )
        )
    }

    @Test
    fun `Given an offer by value from an issuer that is not configured, Then it is not configured`() {
        listOf(
            "https://attacker.example",
            "https://issuer.example/",
            "https://issuer.example.attacker.example",
            "http://issuer.example",
        ).forEach { issuer ->
            assertEquals(
                issuer,
                OfferIssuer.NotConfigured,
                offerIssuerOf(offerByValue(issuer), configured)
            )
        }
    }

    @Test
    fun `Given an offer by reference on a configured issuer's origin, Then that issuer is returned`() {
        assertEquals(
            OfferIssuer.Configured("https://issuer.example"),
            offerIssuerOf(offerByReference("https://ISSUER.example/offers/123"), configured)
        )
        assertEquals(
            OfferIssuer.Configured("https://second.example:8443"),
            offerIssuerOf(offerByReference("https://second.example:8443/offers/123"), configured)
        )
    }

    @Test
    fun `Given an offer by reference anywhere else, Then it is not configured`() {
        listOf(
            "https://attacker.example/offers/123",
            "https://second.example/offers/123",
            "https://issuer.example:8443/offers/123",
        ).forEach { reference ->
            assertEquals(
                reference,
                OfferIssuer.NotConfigured,
                offerIssuerOf(offerByReference(reference), configured)
            )
        }
    }

    @Test
    fun `Given the offer given more than once, in any spelling, Then the link is unreadable`() {
        val attacker = encode(offer("https://attacker.example"))
        val configuredOffer = encode(offer("https://issuer.example"))
        listOf(
            "openid-credential-offer://?CREDENTIAL_OFFER=$attacker&credential_offer=$configuredOffer",
            "openid-credential-offer://?credential_offer=$configuredOffer&credential_offer=$attacker",
            "openid-credential-offer://?Credential_Offer_Uri=${encode("https://attacker.example/o")}" +
                    "&credential_offer_uri=${encode("https://issuer.example/o")}",
        ).forEach { link ->
            assertEquals(link, OfferIssuer.Unreadable, offerIssuerOf(link, configured))
        }
    }

    @Test
    fun `Given a parameter name in another case, Then it is read as the library reads it`() {
        assertEquals(
            OfferIssuer.NotConfigured,
            offerIssuerOf(
                "openid-credential-offer://?CREDENTIAL_OFFER=${encode(offer("https://attacker.example"))}",
                configured
            )
        )
    }

    @Test
    fun `Given a link without a usable offer, Then it is unreadable`() {
        listOf(
            "openid-credential-offer://",
            "openid-credential-offer://?other=1",
            "openid-credential-offer://?credential_offer=",
            "openid-credential-offer://?credential_offer=%7Bnot-json",
            "openid-credential-offer://?credential_offer=${encode("{\"credential_issuer\":null}")}",
            "openid-credential-offer://?credential_offer_uri=${encode("http://issuer.example/offers/123")}",
            "openid-credential-offer://?credential_offer_uri=${encode("file:///offers/123")}",
            "openid-credential-offer://?credential_offer=${encode(offer("https://issuer.example"))}" +
                    "&credential_offer_uri=${encode("https://issuer.example/o")}",
        ).forEach { link ->
            assertEquals(link, OfferIssuer.Unreadable, offerIssuerOf(link, configured))
        }
    }

    private fun offer(issuer: String) =
        """{"credential_issuer":"$issuer","credential_configuration_ids":["pid"]}"""

    private fun offerByValue(
        issuer: String,
        prefix: String = "openid-credential-offer://?"
    ): String =
        "${prefix}credential_offer=${encode(offer(issuer))}"

    private fun offerByReference(reference: String): String =
        "openid-credential-offer://?credential_offer_uri=${encode(reference)}"

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}