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

/** Which issuer a credential offer link comes from, as [offerIssuerOf] reads it. */
sealed interface OfferIssuer {

    /** The link names the configured issuer [issuerId]. */
    data class Configured(val issuerId: String) : OfferIssuer

    /** The link is a valid offer, but from an issuer that is not configured. */
    data object NotConfigured : OfferIssuer

    /** The link is not a usable credential offer. */
    data object Unreadable : OfferIssuer
}