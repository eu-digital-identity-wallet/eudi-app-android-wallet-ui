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

import java.net.URI

/**
 * Whether the wallet may open [link], an address that a relying party or an issuer supplies, such as
 * a verifier's redirect after a presentation or a rejection, or a privacy-policy link from a
 * registration: an `https` URL with a host. The wallet opens such a link in the browser and never
 * through its own link handling, so the party cannot start an issuance, a presentation or a signing in
 * the wallet this way. Any other scheme, and an address that cannot be parsed, is ignored.
 */
fun isOpenableExternalLink(link: String): Boolean {
    val uri = try {
        URI(link)
    } catch (_: Exception) {
        return false
    }
    return uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
}