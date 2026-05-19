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

package eu.europa.ec.authenticationlogic.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

interface AuthenticationConfig {

    /**
     * Number of consecutive wrong PIN attempts allowed before the user is locked out.
     */
    val maxFailedPinAttempts: Int

    /**
     * Lockout durations applied each time the user reaches [maxFailedPinAttempts].
     * The list indexes by lockout level (0 = first lockout). Once the user exceeds the
     * size of the list, the last entry is reused for every subsequent lockout.
     */
    val pinLockoutDurations: List<Duration>
}

class AuthenticationConfigImpl : AuthenticationConfig {
    override val maxFailedPinAttempts: Int = 3
    override val pinLockoutDurations: List<Duration> = listOf(
        30.seconds,
        90.seconds,
        5.minutes
    )
}
