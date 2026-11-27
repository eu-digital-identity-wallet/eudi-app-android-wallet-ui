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

import kotlin.time.Duration

/**
 * Deferred polling policy. Delays must be finite and positive in milliseconds; limits must be positive.
 *
 * @property initialPollingDelay Polling fallback until Wallet Core supplies a valid issuer interval.
 * @property failureRetryDelay Time to wait after an ordinary deferred issuance failure before retrying.
 * @property maxConcurrentDeferredRequests Global cap, or null for unlimited; each ID still has one attempt.
 */
data class DeferredIssuanceConfig(
    val initialPollingDelay: Duration,
    val failureRetryDelay: Duration,
    val maxConcurrentDeferredRequests: Int?,
) {
    // A zero delay would poll continuously and a zero concurrency limit would prevent all attempts.
    init {
        require(initialPollingDelay.isFinite() && initialPollingDelay.inWholeMilliseconds > 0)
        require(failureRetryDelay.isFinite() && failureRetryDelay.inWholeMilliseconds > 0)
        require(maxConcurrentDeferredRequests == null || maxConcurrentDeferredRequests > 0)
    }
}