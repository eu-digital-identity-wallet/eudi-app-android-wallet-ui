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

package eu.europa.ec.corelogic.worker

import eu.europa.ec.corelogic.util.mockedDeferredConfig
import eu.europa.ec.corelogic.util.mockedDeferredSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class TestDeferredRetryPolicy {
    private val policy = DeferredRetryPolicy(mockedDeferredConfig)

    //region prepareForAttempt
    // Case 1:
    // 1. Initial polling and failure retries have different configured delays.
    //
    // Case 1 Expected Result:
    // The next deadline uses the initial delay, or the last issuer interval when known.
    @Test
    fun `Given different delays, When preparing an attempt, Then polling determines the next deadline`() {
        // Given
        val policy =
            DeferredRetryPolicy(mockedDeferredConfig.copy(initialPollingDelay = 10.seconds))

        // When
        val initial = policy.prepareForAttempt(mockedDeferredSchedule, now = 1_000)
        val subsequent = policy.prepareForAttempt(
            mockedDeferredSchedule.copy(lastIntervalMillis = 60_000),
            now = 1_000
        )

        // Then
        assertEquals(11_000L, initial.nextAttemptAt)
        assertEquals(61_000L, subsequent.nextAttemptAt)
    }
    //endregion

    //region notReady
    // Case 1:
    // 1. A new issuer interval differs from the one used before the attempt.
    //
    // Case 1 Expected Result:
    // The deadline includes a one-second margin from receipt. The stored issuer interval stays exact.
    @Test
    fun `Given a new interval, When NotReady arrives, Then only the deadline includes a one-second margin`() {
        // Given
        val schedule = mockedDeferredSchedule.copy(lastIntervalMillis = 60_000)

        // When
        val result = policy.notReady(schedule, interval = 12.seconds, now = 5_000)

        // Then
        assertEquals(18_000L, result.nextAttemptAt)
        assertEquals(12_000L, result.lastIntervalMillis)
    }

    // Case 2:
    // 1. The response interval is absent, zero, negative or infinite.
    //
    // Case 2 Expected Result:
    // Keep the last valid interval, or the configured initial delay when absent.
    @Test
    fun `Given an invalid interval, When NotReady arrives, Then the last valid interval is preserved`() {
        // Given
        val intervals = listOf(null, Duration.ZERO, (-1).seconds, Duration.INFINITE)

        intervals.forEach { interval ->
            // When
            val known = policy.notReady(
                mockedDeferredSchedule.copy(lastIntervalMillis = 60_000),
                interval,
                1_000
            )
            val unknown = policy.notReady(mockedDeferredSchedule, interval, 1_000)

            // Then
            assertEquals(61_000L, known.nextAttemptAt)
            assertEquals(60_000L, known.lastIntervalMillis)
            assertEquals(6_000L, unknown.nextAttemptAt)
            assertNull(unknown.lastIntervalMillis)
        }
    }
    //endregion

    //region failed
    // Case 1: The last issuer interval is shorter than the retry delay, so each failure waits the delay.
    @Test
    fun `Given six failed polls, When each retry is scheduled, Then the row stays active and uses the failure delay each time`() {
        // Given
        val schedule = mockedDeferredSchedule.copy(lastIntervalMillis = 10_000)

        // When
        var next = schedule
        repeat(6) { next = policy.failed(next, now = next.nextAttemptAt) }

        // Then
        assertEquals(schedule.copy(nextAttemptAt = 180_000), next)
    }

    // Case 2: The retry waits the configured delay or the last issuer interval, whichever is longer.
    @Test
    fun `Given a configured failure delay, When a poll fails, Then the retry waits that delay or a longer issuer interval`() {
        // Given
        val policy = DeferredRetryPolicy(mockedDeferredConfig.copy(failureRetryDelay = 45.seconds))
        val expectedDeadlines = listOf(
            mockedDeferredSchedule to 46_000L,
            mockedDeferredSchedule.copy(lastIntervalMillis = 10_000) to 46_000L,
            mockedDeferredSchedule.copy(lastIntervalMillis = 1.days.inWholeMilliseconds) to
                    1_000L + 1.days.inWholeMilliseconds,
        )

        expectedDeadlines.forEach { (schedule, deadline) ->
            // When
            val result = policy.failed(schedule, now = 1_000)

            // Then
            assertEquals(schedule.copy(nextAttemptAt = deadline), result)
        }
    }
    //endregion
}