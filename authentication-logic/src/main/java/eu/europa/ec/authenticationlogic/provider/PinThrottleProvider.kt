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

package eu.europa.ec.authenticationlogic.provider

import kotlin.time.Duration

sealed interface PinLockoutState {
    data object Idle : PinLockoutState
    data class Active(
        val remaining: Duration,
        val total: Duration
    ) : PinLockoutState
}

interface PinThrottleProvider {

    /**
     * Returns the current lockout state, evaluating wall-clock time against persisted values.
     */
    suspend fun getState(): PinLockoutState

    /**
     * Records a failed PIN attempt. If the failure count reaches the configured threshold,
     * a new lockout is started using the next duration from the configured list.
     *
     * @return The resulting [PinLockoutState] after recording the failure.
     */
    suspend fun recordFailure(): PinLockoutState

    /**
     * Clears all throttle state (counters, lockout level, timestamps).
     * Called after a successful authentication (PIN or biometric).
     */
    suspend fun recordSuccess()
}
