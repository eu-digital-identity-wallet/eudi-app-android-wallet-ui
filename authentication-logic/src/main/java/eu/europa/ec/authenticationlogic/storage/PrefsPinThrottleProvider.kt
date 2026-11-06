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

import android.os.SystemClock
import eu.europa.ec.authenticationlogic.config.AuthenticationConfig
import eu.europa.ec.authenticationlogic.provider.PinLockoutState
import eu.europa.ec.authenticationlogic.provider.PinThrottleProvider
import eu.europa.ec.businesslogic.controller.storage.PrefsController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The PIN lockout, kept in the preferences. Its time is the device's elapsed real time and the boot
 * it was read in, not the wall clock, so moving the device's date forward does not end a lockout
 * early, and a restart begins the lockout again in full. A value that cannot be read counts as a
 * lockout of the longest configured duration, so a failed read never gives extra attempts. iOS
 * keeps the same rules.
 *
 * @param elapsedRealtime The time since boot, sleep included, in milliseconds.
 * @param bootCount The number of the current boot (`Settings.Global.BOOT_COUNT`).
 */
class PrefsPinThrottleProvider(
    private val prefsController: PrefsController,
    private val authenticationConfig: AuthenticationConfig,
    private val bootCount: () -> Int,
    private val elapsedRealtime: () -> Long = { SystemClock.elapsedRealtime() },
) : PinThrottleProvider {

    private companion object {
        const val KEY_FAILED_ATTEMPTS = "PinFailedAttempts"
        const val KEY_LOCKOUT_LEVEL = "PinLockoutLevel"
        const val KEY_LOCKOUT_STARTED_AT_ELAPSED = "PinLockoutStartedAtElapsed"
        const val KEY_LOCKOUT_DURATION = "PinLockoutDuration"
        const val KEY_LOCKOUT_BOOT_COUNT = "PinLockoutBootCount"

        /** Wall-clock values of builds before the elapsed-time lockout; removed with a success. */
        const val KEY_LOCKOUT_STARTED_AT = "PinLockoutStartedAt"
        const val KEY_LOCKOUT_ENDS_AT = "PinLockoutEndsAt"
    }

    /**
     * A lockout that began in another boot, or after the current elapsed time, which a restart
     * resets, begins again in full. A lockout that has run its course is cleared, keeping its
     * level, so that a later restart does not begin it again. A write that fails here does not
     * change the state returned.
     */
    override suspend fun getState(): PinLockoutState = withContext(Dispatchers.IO) {
        val (startedAt, duration, lockoutBootCount) = runCatching { readLockout() }.getOrNull()
            ?: return@withContext unreadableStateLockout()

        if (startedAt <= 0L || duration <= 0L) {
            return@withContext PinLockoutState.Idle
        }

        val now = elapsedRealtime()
        if (lockoutBootCount != bootCount() || now < startedAt) {
            runCatching { startLockout(duration) }
            return@withContext PinLockoutState.Active(
                remaining = duration.milliseconds,
                total = duration.milliseconds
            )
        }

        val elapsed = now - startedAt
        if (elapsed >= duration) {
            runCatching { clearLockout() }
            PinLockoutState.Idle
        } else {
            PinLockoutState.Active(
                remaining = (duration - elapsed).milliseconds,
                total = duration.milliseconds
            )
        }
    }

    override suspend fun recordFailure(): PinLockoutState = withContext(Dispatchers.IO) {
        val (currentAttempts, currentLevel) = runCatching {
            prefsController.getInt(KEY_FAILED_ATTEMPTS, 0) to
                    prefsController.getInt(KEY_LOCKOUT_LEVEL, 0)
        }.getOrNull() ?: return@withContext unreadableStateLockout()
        val newAttempts = currentAttempts + 1
        val maxAttempts = authenticationConfig.maxFailedPinAttempts

        if (newAttempts < maxAttempts) {
            prefsController.setInt(KEY_FAILED_ATTEMPTS, newAttempts)
            runCatching { clearLockoutIfEnded() }
            return@withContext PinLockoutState.Idle
        }

        val durations = authenticationConfig.pinLockoutDurations
        val duration: Duration = if (durations.isEmpty()) {
            Duration.ZERO
        } else {
            durations[currentLevel.coerceAtMost(durations.lastIndex)]
        }

        prefsController.setInt(KEY_FAILED_ATTEMPTS, 0)
        prefsController.setInt(KEY_LOCKOUT_LEVEL, currentLevel + 1)
        startLockout(duration.inWholeMilliseconds)

        PinLockoutState.Active(remaining = duration, total = duration)
    }

    override suspend fun recordSuccess() = withContext(Dispatchers.IO) {
        prefsController.setInt(KEY_FAILED_ATTEMPTS, 0)
        prefsController.setInt(KEY_LOCKOUT_LEVEL, 0)
        clearLockout()
        prefsController.clear(KEY_LOCKOUT_STARTED_AT)
        prefsController.clear(KEY_LOCKOUT_ENDS_AT)
    }

    /** The stored lockout: its start in elapsed time, its length in milliseconds, and its boot. */
    private suspend fun readLockout(): Triple<Long, Long, Int> = Triple(
        prefsController.getLong(KEY_LOCKOUT_STARTED_AT_ELAPSED, 0L),
        prefsController.getLong(KEY_LOCKOUT_DURATION, 0L),
        prefsController.getInt(KEY_LOCKOUT_BOOT_COUNT, 0),
    )

    private suspend fun startLockout(durationMillis: Long) {
        prefsController.setLong(KEY_LOCKOUT_STARTED_AT_ELAPSED, elapsedRealtime())
        prefsController.setLong(KEY_LOCKOUT_DURATION, durationMillis)
        prefsController.setInt(KEY_LOCKOUT_BOOT_COUNT, bootCount())
    }

    /**
     * Clears a lockout that has run its course in this boot, so that a later restart does not begin
     * it again.
     */
    private suspend fun clearLockoutIfEnded() {
        val (startedAt, duration, lockoutBootCount) = readLockout()
        val now = elapsedRealtime()
        if (startedAt > 0L && lockoutBootCount == bootCount() && now >= startedAt &&
            now - startedAt >= duration
        ) {
            clearLockout()
        }
    }

    private suspend fun clearLockout() {
        prefsController.setLong(KEY_LOCKOUT_STARTED_AT_ELAPSED, 0L)
        prefsController.setLong(KEY_LOCKOUT_DURATION, 0L)
        prefsController.clear(KEY_LOCKOUT_BOOT_COUNT)
    }

    private fun unreadableStateLockout(): PinLockoutState {
        val longest = authenticationConfig.pinLockoutDurations.maxOrNull() ?: Duration.ZERO
        return PinLockoutState.Active(remaining = longest, total = longest)
    }
}