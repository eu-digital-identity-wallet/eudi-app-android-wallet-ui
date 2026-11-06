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

import java.net.URI

/** [value] as an `https` URI with a host, or null when it is anything else or cannot be parsed. */
internal fun httpsUriOrNull(value: String): URI? = try {
    URI(value).takeIf { it.isHttpsWithHost() }
} catch (_: Exception) {
    null
}

internal fun URI.isHttpsWithHost(): Boolean =
    scheme.equals("https", ignoreCase = true) && !host.isNullOrBlank()

/** Whether this URI and [other] share host and port; hosts are compared without regard to case. */
internal fun URI.hasSameOriginAs(other: URI): Boolean =
    host.equals(other.host, ignoreCase = true) && effectivePort() == other.effectivePort()

private fun URI.effectivePort(): Int = if (port == -1) 443 else port