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

package eu.europa.ec.corelogic.config

/**
 * How presentation requests are held to reader authentication. Wallet Core is configured with it,
 * and the presentation flow reads the same value to refuse, before the consent screen, a request
 * that Wallet Core would answer with no data.
 */
enum class ReaderAuthenticationEnforcement {

    /**
     * Wallet Core evaluates no reader trust, so no requester counts as trusted: every mdoc reader
     * is shown as unverified and receives data once the user accepts the warning, and a signed
     * OpenID4VP request is rejected when it is resolved, as nothing can vouch for it.
     */
    DO_NOT_ENFORCE,

    /**
     * A request that carries reader authentication must be trusted; a request without it may
     * receive data once the user accepts the warning on the consent screen.
     */
    ENFORCE_IF_PRESENT,

    /** Only a request with trusted reader authentication may receive data. */
    ALWAYS_REQUIRE,
}
