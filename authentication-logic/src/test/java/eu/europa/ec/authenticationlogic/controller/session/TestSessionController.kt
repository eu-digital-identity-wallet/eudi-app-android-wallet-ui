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

package eu.europa.ec.authenticationlogic.controller.session

import eu.europa.ec.authenticationlogic.config.AuthenticationConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TestSessionController {

    private var now: Long = 1_000_000L

    private val config = object : AuthenticationConfig {
        override val maxFailedPinAttempts: Int = 3
        override val pinLockoutDurations: List<Duration> = listOf(30.seconds)
        override val backgroundLockTimeout: Duration = 60.seconds
        override val idleLockTimeout: Duration = 5.minutes
        override val protectedFlowLockTimeout: Duration = 10.minutes
        override val commonPins: Set<String> = emptySet()
        override val requireDeviceSecure: Boolean = false
    }

    private lateinit var controller: SessionController

    @Before
    fun before() {
        controller = SessionControllerImpl(config) { now }
    }

    private fun advance(duration: Duration) {
        now += duration.inWholeMilliseconds
    }

    @Test
    fun `Given a new session, When nothing has happened, Then it is locked`() {
        // When
        val unlocked = controller.isUnlocked

        // Then
        assertFalse(unlocked)
    }

    @Test
    fun `Given a locked session, When it is unlocked and then locked, Then isUnlocked follows each call`() {
        // When
        controller.unlock()
        val unlockedAfterUnlock = controller.isUnlocked
        controller.lock()
        val unlockedAfterLock = controller.isUnlocked

        // Then
        assertTrue(unlockedAfterUnlock)
        assertFalse(unlockedAfterLock)
    }

    /**
     * 1. The session has locked before an action is submitted.
     *
     * Expected result:
     * The action is refused without being called.
     */
    @Test
    fun `Given a locked session, When an action is submitted, Then it is not called`() {
        // Given
        controller.unlock()
        controller.lock()
        var calls = 0

        // When
        val started = controller.runIfUnlocked { calls++ }

        // Then
        assertFalse(started)
        assertEquals(0, calls)
    }

    /**
     * 1. Authentication has unlocked the session.
     *
     * Expected result:
     * The action runs once and admission succeeds.
     */
    @Test
    fun `Given an unlocked session, When an action is submitted, Then it runs once`() {
        // Given
        controller.unlock()
        var calls = 0

        // When
        val started = controller.runIfUnlocked { calls++ }

        // Then
        assertTrue(started)
        assertEquals(1, calls)
    }

    /**
     * 1. An admitted action has started while the session is unlocked.
     * 2. Another thread checks the expired idle timeout before that action returns.
     *
     * Expected result:
     * The session remains unlocked through submission, then the idle lock completes.
     */
    @Test
    fun `Given an admitted action, When the idle timeout expires concurrently, Then locking waits for submission`() {
        // Given
        controller.unlock()
        advance(config.idleLockTimeout)
        val executor = Executors.newSingleThreadExecutor()
        val lockRequested = CountDownLatch(1)
        val lockCompleted = CountDownLatch(1)
        val timeoutSeconds = 5L
        lateinit var idleLock: Future<Boolean>

        try {
            // When
            val started = controller.runIfUnlocked {
                idleLock = executor.submit<Boolean> {
                    lockRequested.countDown()
                    try {
                        controller.lockIfIdle()
                    } finally {
                        lockCompleted.countDown()
                    }
                }
                assertTrue(lockRequested.await(timeoutSeconds, TimeUnit.SECONDS))
                assertFalse(lockCompleted.await(100, TimeUnit.MILLISECONDS))
                assertTrue(controller.isUnlocked)
            }

            // Then
            assertTrue(started)
            assertTrue(idleLock.get(timeoutSeconds, TimeUnit.SECONDS))
            assertFalse(controller.isUnlocked)
        } finally {
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS))
        }
    }

    @Test
    fun `Given an unlocked session in the background, When it returns before the background timeout, Then it stays unlocked`() {
        // Given
        controller.unlock()
        controller.onBackground()
        advance(59.seconds)

        // When
        val locked = controller.onForeground()

        // Then
        assertFalse(locked)
        assertTrue(controller.isUnlocked)
    }

    @Test
    fun `Given an unlocked session in the background, When it returns at the background timeout, Then it locks`() {
        // Given
        controller.unlock()
        controller.onBackground()
        advance(60.seconds)

        // When
        val locked = controller.onForeground()

        // Then
        assertTrue(locked)
        assertFalse(controller.isUnlocked)
    }

    @Test
    fun `Given a protected flow in the background, When it returns after the background timeout, Then it stays unlocked`() {
        // Given
        controller.unlock()
        controller.recordProtectedFlow(inProtectedFlow = true)
        controller.onBackground()
        advance(9.minutes)

        // When
        val locked = controller.onForeground()

        // Then
        assertFalse(locked)
        assertTrue(controller.isUnlocked)
    }

    @Test
    fun `Given a protected flow in the background, When it returns at the protected-flow timeout, Then it locks`() {
        // Given
        controller.unlock()
        controller.recordProtectedFlow(inProtectedFlow = true)
        controller.onBackground()
        advance(10.minutes)

        // When
        val locked = controller.onForeground()

        // Then
        assertTrue(locked)
        assertFalse(controller.isUnlocked)
    }

    @Test
    fun `Given an external flow on any screen, When it returns before the protected-flow timeout, Then it stays unlocked`() {
        // Given
        controller.unlock()
        controller.beginExternalFlow()
        controller.onBackground()
        advance(9.minutes)

        // When
        val locked = controller.onForeground()

        // Then
        assertFalse(locked)
        assertTrue(controller.isUnlocked)
    }

    @Test
    fun `Given an ended external flow, When it returns at the background timeout, Then it locks`() {
        // Given
        controller.unlock()
        controller.beginExternalFlow()
        controller.endExternalFlow()
        controller.onBackground()
        advance(60.seconds)

        // When
        val locked = controller.onForeground()

        // Then
        assertTrue(locked)
    }

    @Test
    fun `Given an external flow, When the session is locked and unlocked again, Then the background timeout applies`() {
        // Given
        controller.unlock()
        controller.beginExternalFlow()
        controller.lock()
        controller.unlock()
        controller.onBackground()
        advance(60.seconds)

        // When
        val locked = controller.onForeground()

        // Then
        assertTrue(locked)
    }

    @Test
    fun `Given an unlocked session never in the background, When it comes to the foreground, Then nothing changes`() {
        // Given
        controller.unlock()
        advance(10.minutes)

        // When
        val locked = controller.onForeground()

        // Then
        assertFalse(locked)
        assertTrue(controller.isUnlocked)
    }

    @Test
    fun `Given an authentication completed after the app left, When the app returns at the background timeout, Then it locks`() {
        // Given
        controller.onBackground()
        controller.unlock()
        advance(60.seconds)

        // When
        val locked = controller.onForeground()

        // Then
        assertTrue(locked)
        assertFalse(controller.isUnlocked)
    }

    @Test
    fun `Given an authentication completed after the app left, When the app returns before the background timeout, Then it stays unlocked`() {
        // Given
        controller.onBackground()
        controller.unlock()
        advance(59.seconds)

        // When
        val locked = controller.onForeground()

        // Then
        assertFalse(locked)
        assertTrue(controller.isUnlocked)
    }

    @Test
    fun `Given a locked session that returned before authentication, When it is unlocked, Then the earlier background is forgotten`() {
        // Given
        controller.onBackground()
        advance(2.minutes)
        assertFalse(controller.onForeground())

        // When
        controller.unlock()
        val locked = controller.onForeground()

        // Then
        assertFalse(locked)
        assertTrue(controller.isUnlocked)
    }

    @Test
    fun `Given a locked session in the background, When it comes to the foreground, Then no new lock is reported`() {
        // Given
        controller.onBackground()
        advance(10.minutes)

        // When
        val locked = controller.onForeground()

        // Then
        assertFalse(locked)
        assertFalse(controller.isUnlocked)
    }

    @Test
    fun `Given time spent in the background, When the idle timeout is checked, Then that time does not count`() {
        // Given
        controller.unlock()
        advance(4.minutes)
        controller.onBackground()
        advance(50.seconds)
        controller.onForeground()
        advance(4.minutes)

        // When
        val locked = controller.lockIfIdle()

        // Then
        assertFalse(locked)
        assertTrue(controller.isUnlocked)
    }

    @Test
    fun `Given an unlocked session, When it is idle for the idle timeout, Then it locks`() {
        // Given
        controller.unlock()
        advance(5.minutes)

        // When
        val locked = controller.lockIfIdle()

        // Then
        assertTrue(locked)
        assertFalse(controller.isUnlocked)
    }

    @Test
    fun `Given an unlocked session, When the user interacts before the idle timeout, Then the idle timeout restarts`() {
        // Given
        controller.unlock()
        advance(4.minutes)

        // When
        controller.onUserInteraction()
        advance(4.minutes)
        val locked = controller.lockIfIdle()

        // Then
        assertFalse(locked)
        assertTrue(controller.isUnlocked)
    }

    @Test
    fun `Given a locked session, When it is idle, Then no new lock is reported`() {
        // Given
        advance(30.minutes)

        // When
        val locked = controller.lockIfIdle()

        // Then
        assertFalse(locked)
    }

    @Test
    fun `Given an unlocked session, When time passes, Then the time until the idle lock counts down and stops at zero`() {
        // Given
        controller.unlock()

        // When
        advance(2.minutes)
        val afterTwoMinutes = controller.timeUntilIdleLock()

        // Then
        assertEquals(3.minutes, afterTwoMinutes)

        // When
        advance(10.minutes)
        val afterTwelveMinutes = controller.timeUntilIdleLock()

        // Then
        assertEquals(Duration.ZERO, afterTwelveMinutes)
    }

    @Test
    fun `Given a locked session, When the time until the idle lock is read, Then it is the full idle timeout`() {
        // When
        val remaining = controller.timeUntilIdleLock()

        // Then
        assertEquals(5.minutes, remaining)
    }

    @Test
    fun `Given another screen covering the wallet, When the wallet is shown again before the protected-flow timeout, Then it stays unlocked`() {
        // Given
        controller.unlock()
        controller.onCoveredByOtherScreen()
        advance(9.minutes)

        // When
        val locked = controller.onWalletScreenShown()

        // Then
        assertFalse(locked)
        assertTrue(controller.isUnlocked)

        // When
        advance(4.minutes)
        val idleLocked = controller.lockIfIdle()

        // Then
        assertFalse(idleLocked)
    }

    @Test
    fun `Given another screen covering the wallet, When the wallet is shown again at the protected-flow timeout, Then it locks`() {
        // Given
        controller.unlock()
        controller.onCoveredByOtherScreen()
        advance(4.minutes)
        controller.onCoveredByOtherScreen()
        advance(6.minutes)

        // When
        val locked = controller.onWalletScreenShown()

        // Then
        assertTrue(locked)
        assertFalse(controller.isUnlocked)
    }

    @Test
    fun `Given another screen covering the wallet, When the app returns from the background before the protected-flow timeout, Then it stays unlocked`() {
        // Given
        controller.unlock()
        controller.onCoveredByOtherScreen()
        controller.onBackground()
        advance(9.minutes)

        // When
        val locked = controller.onForeground()

        // Then
        assertFalse(locked)
        assertTrue(controller.isUnlocked)
    }

    @Test
    fun `Given the wallet shown again after another screen, When the app returns at the background timeout, Then it locks`() {
        // Given
        controller.unlock()
        controller.onCoveredByOtherScreen()
        controller.onWalletScreenShown()
        controller.onBackground()
        advance(60.seconds)

        // When
        val locked = controller.onForeground()

        // Then
        assertTrue(locked)
    }

    @Test
    fun `Given no other screen covered the wallet, When the wallet screen is shown, Then the idle timeout keeps running`() {
        // Given
        controller.unlock()
        advance(5.minutes)

        // When
        val shownLocked = controller.onWalletScreenShown()
        val idleLocked = controller.lockIfIdle()

        // Then
        assertFalse(shownLocked)
        assertTrue(idleLocked)
    }

    @Test
    fun `Given another screen covering the wallet, When the idle timeout passes, Then the session does not lock`() {
        // Given
        controller.unlock()
        controller.onCoveredByOtherScreen()
        advance(6.minutes)

        // When
        val locked = controller.lockIfIdle()
        val remaining = controller.timeUntilIdleLock()

        // Then
        assertFalse(locked)
        assertEquals(5.minutes, remaining)
        assertTrue(controller.isUnlocked)
    }

    @Test
    fun `Given a locked session covered by another screen, When the wallet is shown again, Then no new lock is reported`() {
        // Given
        controller.onCoveredByOtherScreen()
        advance(30.minutes)

        // When
        val locked = controller.onWalletScreenShown()

        // Then
        assertFalse(locked)
        assertFalse(controller.isUnlocked)
    }

    @Test
    fun `Given a protected flow recorded and then cleared, When the app returns at the background timeout, Then it locks`() {
        // Given
        controller.unlock()
        controller.recordProtectedFlow(inProtectedFlow = true)
        controller.recordProtectedFlow(inProtectedFlow = false)
        controller.onBackground()
        advance(60.seconds)

        // When
        val locked = controller.onForeground()

        // Then
        assertTrue(locked)
    }
}