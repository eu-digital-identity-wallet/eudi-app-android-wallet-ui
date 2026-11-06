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

import eu.europa.ec.corelogic.extension.hasSameOriginAs
import eu.europa.ec.corelogic.extension.httpsUriOrNull
import eu.europa.ec.eudi.openid4vci.CredentialOfferRequest
import io.ktor.http.URLBuilder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The issuer that a credential offer link comes from, read from the link alone, before anything is
 * fetched, so that the wallet contacts no host other than a configured issuer's while reading the
 * link and never uses one issuer's client settings for another.
 *
 * The link is parsed with the OpenID4VCI library's own parser, which Wallet Core uses next, so that
 * both read the same offer; a link that gives the offer more than once, in any spelling, is
 * [OfferIssuer.Unreadable].
 *
 * - An offer passed by value (`credential_offer`) must name a [configuredIssuers] entry as its
 *   `credential_issuer`, compared exactly as the wallet compares issuer identifiers elsewhere.
 * - An offer passed by reference (`credential_offer_uri`) is fetched from that address, so the
 *   address must be on the origin of a configured issuer, which is then returned. The offer itself
 *   must name a configured issuer too, which can only be checked once it has been read.
 */
fun offerIssuerOf(offerUri: String, configuredIssuers: Collection<String>): OfferIssuer {
    val parameters = try {
        URLBuilder(offerUri).parameters
    } catch (_: Exception) {
        return OfferIssuer.Unreadable
    }
    val repeated = listOf(CREDENTIAL_OFFER, CREDENTIAL_OFFER_URI).any { name ->
        parameters.getAll(name).orEmpty().size > 1
    }
    if (repeated) return OfferIssuer.Unreadable

    return when (val request = CredentialOfferRequest(offerUri).getOrNull()) {
        is CredentialOfferRequest.PassByValue -> {
            val issuer = credentialIssuerOf(request.value) ?: return OfferIssuer.Unreadable
            if (issuer in configuredIssuers) OfferIssuer.Configured(issuer) else OfferIssuer.NotConfigured
        }

        is CredentialOfferRequest.PassByReference -> {
            val reference = httpsUriOrNull(request.value.value.toString())
                ?: return OfferIssuer.Unreadable
            configuredIssuers
                .firstOrNull { issuer ->
                    httpsUriOrNull(issuer)?.let { reference.hasSameOriginAs(it) } == true
                }
                ?.let { OfferIssuer.Configured(it) }
                ?: OfferIssuer.NotConfigured
        }

        null -> OfferIssuer.Unreadable
    }
}

private const val CREDENTIAL_OFFER = "credential_offer"
private const val CREDENTIAL_OFFER_URI = "credential_offer_uri"

private fun credentialIssuerOf(offer: String): String? = try {
    Json.parseToJsonElement(offer).jsonObject["credential_issuer"]?.jsonPrimitive?.contentOrNull
} catch (_: Exception) {
    null
}