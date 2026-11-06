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

package eu.europa.ec.networklogic.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Limits of the HTTP client the wallet hands to Wallet Core, which uses it for every issuer,
 * verifier, trusted-list and status-list request, including those a link starts. A request that
 * passes a limit fails as a network error.
 */
interface NetworkConfig {

    /** The longest wait for a connection to be set up. */
    val connectTimeout: Duration

    /** The longest wait between two packets of data, sent or received. */
    val socketTimeout: Duration

    /** The longest a whole request may take, from sending it to reading its response. */
    val requestTimeout: Duration

    /**
     * The largest response body the client reads, in bytes. A larger body fails the request: at
     * once when its `Content-Length` says so, otherwise as soon as more has arrived.
     */
    val maxResponseBodySize: Long
}

class NetworkConfigImpl : NetworkConfig {
    override val connectTimeout: Duration = 15.seconds
    override val socketTimeout: Duration = 30.seconds
    override val requestTimeout: Duration = 60.seconds
    override val maxResponseBodySize: Long = 10L * 1024 * 1024
}
