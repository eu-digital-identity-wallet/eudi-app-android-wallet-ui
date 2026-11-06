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
import eu.europa.ec.corelogic.extension.isHttpsWithHost
import java.net.URI

/**
 * [logo] when the wallet may load it for the issuer [issuerId]: an `https` URL on the issuer's own
 * origin, or on one of [additionalOrigins], the origins configured for that issuer in
 * `VciConfig.logoOrigins`. The address comes from the issuer's metadata, so without this rule an
 * issuer could make the wallet contact any host, or read a local `content://`, `file://` or
 * `android.resource://` source. Returns null otherwise, and the app shows its placeholder instead.
 */
fun issuerLogoOrNull(
    logo: URI?,
    issuerId: String?,
    additionalOrigins: Collection<String> = emptyList(),
): URI? {
    if (logo == null || issuerId == null) return null
    val issuer = httpsUriOrNull(issuerId) ?: return null
    val allowed = listOf(issuer) + additionalOrigins.mapNotNull(::httpsUriOrNull)
    return logo.takeIf { it.isHttpsWithHost() && allowed.any { origin -> it.hasSameOriginAs(origin) } }
}