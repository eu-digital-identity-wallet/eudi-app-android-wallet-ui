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

import eu.europa.ec.businesslogic.extension.getLocalizedValue
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import java.time.Instant
import java.util.Locale
import kotlin.time.toJavaInstant

fun Document.localizedIssuerMetadata(locale: Locale): IssuerMetadata.IssuerDisplay? {
    return issuerMetadata?.issuerDisplay.getLocalizedValue(
        userLocale = locale,
        fallback = null,
        localeExtractor = { it.locale },
        valueExtractor = { it }
    )
}

/**
 * The document's expiry instant: the latest `validUntil` across its credentials, or `null` when the
 * document has no such credentials (e.g. an exhausted once-only batch).
 *
 * Unlike [IssuedDocument.getValidUntil] — which resolves through `findCredential` and is therefore
 * limited to a *currently-valid* credential, so it yields a future instant or fails once expired —
 * this reads [IssuedDocument.getCredentials], which is not filtered by temporal validity and still
 * includes expired instances. The value therefore survives past expiry, which is what lets callers
 * actually detect an expired document.
 */
suspend fun IssuedDocument.getExpiryDate(): Instant? =
    getCredentials().maxOfOrNull { it.validUntil }?.toJavaInstant()

/**
 * Whether the document has expired, i.e. its [getExpiryDate] is in the past. A document with no
 * credentials (null expiry) is treated as *not* expired: that is an exhausted/credential-less state
 * rather than an expiry.
 */
suspend fun IssuedDocument.isExpired(): Boolean =
    getExpiryDate()?.isBefore(Instant.now()) ?: false