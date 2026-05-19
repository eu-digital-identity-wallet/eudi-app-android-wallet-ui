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

package eu.europa.ec.authenticationlogic.storage

import eu.europa.ec.authenticationlogic.config.AuthenticationConfig
import eu.europa.ec.authenticationlogic.provider.PinLockoutState
import eu.europa.ec.authenticationlogic.provider.PinThrottleProvider
import eu.europa.ec.businesslogic.controller.storage.PrefsController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class PrefsPinThrottleProvider(
    private val prefsController: PrefsController,
    private val authenticationConfig: AuthenticationConfig
) : PinThrottleProvider {

    private companion object {
        const val KEY_FAILED_ATTEMPTS = "PinFailedAttempts"
        const val KEY_LOCKOUT_LEVEL = "PinLockoutLevel"
        const val KEY_LOCKOUT_STARTED_AT = "PinLockoutStartedAt"
        const val KEY_LOCKOUT_ENDS_AT = "PinLockoutEndsAt"
    }

    override suspend fun getState(): PinLockoutState = withContext(Dispatchers.IO) {
        val startedAt = prefsController.getLong(KEY_LOCKOUT_STARTED_AT, 0L)
        val endsAt = prefsController.getLong(KEY_LOCKOUT_ENDS_AT, 0L)

        if (endsAt <= 0L || startedAt <= 0L) {
            return@withContext PinLockoutState.Idle
        }

        val total = (endsAt - startedAt).coerceAtLeast(0L).milliseconds
        val now = System.currentTimeMillis()

        if (now < startedAt) {
            return@withContext PinLockoutState.Active(remaining = total, total = total)
        }

        if (now >= endsAt) {
            return@withContext PinLockoutState.Idle
        }

        val remaining = (endsAt - now).milliseconds
        PinLockoutState.Active(remaining = remaining, total = total)
    }

    override suspend fun recordFailure(): PinLockoutState = withContext(Dispatchers.IO) {
        val currentAttempts = prefsController.getInt(KEY_FAILED_ATTEMPTS, 0)
        val newAttempts = currentAttempts + 1
        val maxAttempts = authenticationConfig.maxFailedPinAttempts

        if (newAttempts < maxAttempts) {
            prefsController.setInt(KEY_FAILED_ATTEMPTS, newAttempts)
            return@withContext PinLockoutState.Idle
        }

        val currentLevel = prefsController.getInt(KEY_LOCKOUT_LEVEL, 0)
        val durations = authenticationConfig.pinLockoutDurations
        val duration: Duration = if (durations.isEmpty()) {
            Duration.ZERO
        } else {
            durations[currentLevel.coerceAtMost(durations.lastIndex)]
        }

        val now = System.currentTimeMillis()
        val endsAt = now + duration.inWholeMilliseconds

        prefsController.setInt(KEY_FAILED_ATTEMPTS, 0)
        prefsController.setInt(KEY_LOCKOUT_LEVEL, currentLevel + 1)
        prefsController.setLong(KEY_LOCKOUT_STARTED_AT, now)
        prefsController.setLong(KEY_LOCKOUT_ENDS_AT, endsAt)

        PinLockoutState.Active(remaining = duration, total = duration)
    }

    override suspend fun recordSuccess() = withContext(Dispatchers.IO) {
        prefsController.setInt(KEY_FAILED_ATTEMPTS, 0)
        prefsController.setInt(KEY_LOCKOUT_LEVEL, 0)
        prefsController.setLong(KEY_LOCKOUT_STARTED_AT, 0L)
        prefsController.setLong(KEY_LOCKOUT_ENDS_AT, 0L)
    }
}
