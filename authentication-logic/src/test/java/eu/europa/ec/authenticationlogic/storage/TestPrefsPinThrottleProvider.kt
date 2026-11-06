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
import eu.europa.ec.businesslogic.controller.storage.PrefsController
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TestPrefsPinThrottleProvider {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private var now: Long = 1_000_000L
    private var boot: Int = 7
    private val prefs = FakePrefsController()

    private val config = object : AuthenticationConfig {
        override val maxFailedPinAttempts: Int = 3
        override val pinLockoutDurations: List<Duration> = listOf(30.seconds, 90.seconds, 5.minutes)
        override val backgroundLockTimeout: Duration = 60.seconds
        override val idleLockTimeout: Duration = 5.minutes
        override val protectedFlowLockTimeout: Duration = 10.minutes
        override val commonPins: Set<String> = emptySet()
        override val requireDeviceSecure: Boolean = false
    }

    private val provider = PrefsPinThrottleProvider(
        prefsController = prefs,
        authenticationConfig = config,
        bootCount = { boot },
        elapsedRealtime = { now },
    )

    @Test
    fun `Given three failed attempts, When the state is read, Then the first lockout runs`() =
        coroutineRule.runTest {
            repeat(2) { assertEquals(PinLockoutState.Idle, provider.recordFailure()) }

            assertEquals(active(30.seconds, 30.seconds), provider.recordFailure())
            now += 10.seconds.inWholeMilliseconds
            assertEquals(active(20.seconds, 30.seconds), provider.getState())
        }

    @Test
    fun `Given a lockout, When its time has passed, Then the state is idle`() =
        coroutineRule.runTest {
            lockOut()

            now += 30.seconds.inWholeMilliseconds

            assertEquals(PinLockoutState.Idle, provider.getState())
        }

    /** A lockout of a build before the elapsed-time one is not read, as on iOS. */
    @Test
    fun `Given only the wall-clock values of an earlier build, When the state is read, Then it is idle`() =
        coroutineRule.runTest {
            prefs.values["PinLockoutStartedAt"] = 1L
            prefs.values["PinLockoutEndsAt"] = Long.MAX_VALUE

            assertEquals(PinLockoutState.Idle, provider.getState())
        }

    /**
     * The elapsed time is kept past the lockout's start, so that only the change of boot can
     * restart it.
     */
    @Test
    fun `Given a lockout, When the device restarts, Then the lockout begins again in full`() =
        coroutineRule.runTest {
            lockOut()
            now += 25.seconds.inWholeMilliseconds

            boot += 1

            assertEquals(active(30.seconds, 30.seconds), provider.getState())
            now += 10.seconds.inWholeMilliseconds
            assertEquals(active(20.seconds, 30.seconds), provider.getState())
        }

    @Test
    fun `Given a lockout that has run its course, When the device restarts, Then no lockout begins`() =
        coroutineRule.runTest {
            lockOut()
            now += 30.seconds.inWholeMilliseconds
            assertEquals(PinLockoutState.Idle, provider.getState())

            boot += 1
            now = 2_000L

            assertEquals(PinLockoutState.Idle, provider.getState())
            assertEquals(1, prefs.values["PinLockoutLevel"])
        }

    /**
     * A failure recorded while a lockout runs, such as one from another screen that read the state
     * before the lockout began, keeps the lockout running.
     */
    @Test
    fun `Given a lockout that runs, When a wrong PIN is recorded, Then the lockout keeps running`() =
        coroutineRule.runTest {
            lockOut()
            now += 10.seconds.inWholeMilliseconds

            provider.recordFailure()

            assertEquals(active(20.seconds, 30.seconds), provider.getState())
        }

    /** The screen did not read the state after the countdown; the next wrong PIN clears it. */
    @Test
    fun `Given a lockout that ended unseen, When a wrong PIN is recorded and the device restarts, Then no lockout begins`() =
        coroutineRule.runTest {
            lockOut()
            now += 30.seconds.inWholeMilliseconds

            assertEquals(PinLockoutState.Idle, provider.recordFailure())
            boot += 1
            now = 2_000L

            assertEquals(PinLockoutState.Idle, provider.getState())
        }

    @Test
    fun `Given an elapsed time earlier than the lockout's start, When the state is read, Then the lockout begins again in full`() =
        coroutineRule.runTest {
            lockOut()

            now -= 1L

            assertEquals(active(30.seconds, 30.seconds), provider.getState())
        }

    @Test
    fun `Given preferences that cannot be read, When the state is read, Then the longest lockout applies`() =
        coroutineRule.runTest {
            prefs.failReads = true

            assertEquals(active(5.minutes, 5.minutes), provider.getState())
        }

    @Test
    fun `Given preferences that cannot be read, When a failure is recorded, Then the longest lockout applies and no attempt is counted`() =
        coroutineRule.runTest {
            prefs.failReads = true

            assertEquals(active(5.minutes, 5.minutes), provider.recordFailure())
            prefs.failReads = false
            assertNull(prefs.values["PinFailedAttempts"])
        }

    @Test
    fun `Given a lockout, When a success is recorded, Then the state is idle and the old wall-clock values are gone`() =
        coroutineRule.runTest {
            prefs.values["PinLockoutStartedAt"] = 1L
            prefs.values["PinLockoutEndsAt"] = 2L
            lockOut()

            provider.recordSuccess()

            assertEquals(PinLockoutState.Idle, provider.getState())
            assertNull(prefs.values["PinLockoutStartedAt"])
            assertNull(prefs.values["PinLockoutEndsAt"])
            assertNull(prefs.values["PinLockoutBootCount"])
        }

    private suspend fun lockOut() {
        repeat(config.maxFailedPinAttempts) { provider.recordFailure() }
    }

    private fun active(remaining: Duration, total: Duration) =
        PinLockoutState.Active(remaining = remaining, total = total)

    /**
     * Preferences in memory, whose reads throw while [failReads] is set, as a failed decryption
     * does.
     */
    private class FakePrefsController : PrefsController {

        val values = mutableMapOf<String, Any>()
        var failReads = false

        override suspend fun contains(key: String): Boolean = read { key in values }

        override suspend fun clear(key: String) {
            values.remove(key)
        }

        override suspend fun clear() {
            values.clear()
        }

        override suspend fun clearExcept(keys: Set<String>) {
            values.keys.retainAll(keys)
        }

        override suspend fun setString(key: String, value: String) {
            values[key] = value
        }

        override suspend fun setLong(key: String, value: Long) {
            values[key] = value
        }

        override suspend fun setBool(key: String, value: Boolean) {
            values[key] = value
        }

        override suspend fun setInt(key: String, value: Int) {
            values[key] = value
        }

        override suspend fun getString(key: String, defaultValue: String): String =
            read { values[key] as? String ?: defaultValue }

        override suspend fun getLong(key: String, defaultValue: Long): Long =
            read { values[key] as? Long ?: defaultValue }

        override suspend fun getBool(key: String, defaultValue: Boolean): Boolean =
            read { values[key] as? Boolean ?: defaultValue }

        override suspend fun getInt(key: String, defaultValue: Int): Int =
            read { values[key] as? Int ?: defaultValue }

        private fun <T> read(value: () -> T): T {
            if (failReads) throw IOException("The preferences cannot be read")
            return value()
        }
    }
}