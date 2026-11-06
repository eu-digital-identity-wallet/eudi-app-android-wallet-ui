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
        assertFalse(controller.isUnlocked)
    }

    @Test
    fun `Given a locked session, When it is unlocked and then locked, Then isUnlocked follows each call`() {
        controller.unlock()
        assertTrue(controller.isUnlocked)

        controller.lock()
        assertFalse(controller.isUnlocked)
    }

    @Test
    fun `Given an unlocked session in the background, When it returns before the background timeout, Then it stays unlocked`() {
        controller.unlock()
        controller.onBackground()
        advance(59.seconds)

        assertFalse(controller.onForeground())
        assertTrue(controller.isUnlocked)
    }

    @Test
    fun `Given an unlocked session in the background, When it returns at the background timeout, Then it locks`() {
        controller.unlock()
        controller.onBackground()
        advance(60.seconds)

        assertTrue(controller.onForeground())
        assertFalse(controller.isUnlocked)
    }

    @Test
    fun `Given a protected flow in the background, When it returns after the background timeout, Then it stays unlocked`() {
        controller.unlock()
        controller.recordProtectedFlow(inProtectedFlow = true)
        controller.onBackground()
        advance(9.minutes)

        assertFalse(controller.onForeground())
        assertTrue(controller.isUnlocked)
    }

    @Test
    fun `Given a protected flow in the background, When it returns at the protected-flow timeout, Then it locks`() {
        controller.unlock()
        controller.recordProtectedFlow(inProtectedFlow = true)
        controller.onBackground()
        advance(10.minutes)

        assertTrue(controller.onForeground())
        assertFalse(controller.isUnlocked)
    }

    @Test
    fun `Given an external flow on any screen, When it returns before the protected-flow timeout, Then it stays unlocked`() {
        controller.unlock()
        controller.beginExternalFlow()
        controller.onBackground()
        advance(9.minutes)

        assertFalse(controller.onForeground())
        assertTrue(controller.isUnlocked)
    }

    @Test
    fun `Given an ended external flow, When it returns at the background timeout, Then it locks`() {
        controller.unlock()
        controller.beginExternalFlow()
        controller.endExternalFlow()
        controller.onBackground()
        advance(60.seconds)

        assertTrue(controller.onForeground())
    }

    @Test
    fun `Given an external flow, When the session is locked and unlocked again, Then the background timeout applies`() {
        controller.unlock()
        controller.beginExternalFlow()
        controller.lock()
        controller.unlock()
        controller.onBackground()
        advance(60.seconds)

        assertTrue(controller.onForeground())
    }

    @Test
    fun `Given an unlocked session never in the background, When it comes to the foreground, Then nothing changes`() {
        controller.unlock()
        advance(10.minutes)

        assertFalse(controller.onForeground())
        assertTrue(controller.isUnlocked)
    }

    @Test
    fun `Given an authentication completed after the app left, When the app returns at the background timeout, Then it locks`() {
        controller.onBackground()
        controller.unlock()
        advance(60.seconds)

        assertTrue(controller.onForeground())
        assertFalse(controller.isUnlocked)
    }

    @Test
    fun `Given an authentication completed after the app left, When the app returns before the background timeout, Then it stays unlocked`() {
        controller.onBackground()
        controller.unlock()
        advance(59.seconds)

        assertFalse(controller.onForeground())
        assertTrue(controller.isUnlocked)
    }

    @Test
    fun `Given a locked session that returned before authentication, When it is unlocked, Then the earlier background is forgotten`() {
        controller.onBackground()
        advance(2.minutes)
        assertFalse(controller.onForeground())

        controller.unlock()

        assertFalse(controller.onForeground())
        assertTrue(controller.isUnlocked)
    }

    @Test
    fun `Given a locked session in the background, When it comes to the foreground, Then no new lock is reported`() {
        controller.onBackground()
        advance(10.minutes)

        assertFalse(controller.onForeground())
        assertFalse(controller.isUnlocked)
    }

    @Test
    fun `Given time spent in the background, When the idle timeout is checked, Then that time does not count`() {
        controller.unlock()
        advance(4.minutes)
        controller.onBackground()
        advance(50.seconds)
        controller.onForeground()
        advance(4.minutes)

        assertFalse(controller.lockIfIdle())
        assertTrue(controller.isUnlocked)
    }

    @Test
    fun `Given an unlocked session, When it is idle for the idle timeout, Then it locks`() {
        controller.unlock()
        advance(5.minutes)

        assertTrue(controller.lockIfIdle())
        assertFalse(controller.isUnlocked)
    }

    @Test
    fun `Given an unlocked session, When the user interacts before the idle timeout, Then the idle timeout restarts`() {
        controller.unlock()
        advance(4.minutes)
        controller.onUserInteraction()
        advance(4.minutes)

        assertFalse(controller.lockIfIdle())
        assertTrue(controller.isUnlocked)
    }

    @Test
    fun `Given a locked session, When it is idle, Then no new lock is reported`() {
        advance(30.minutes)

        assertFalse(controller.lockIfIdle())
    }

    @Test
    fun `Given an unlocked session, When time passes, Then the time until the idle lock counts down and stops at zero`() {
        controller.unlock()
        advance(2.minutes)
        assertEquals(3.minutes, controller.timeUntilIdleLock())

        advance(10.minutes)
        assertEquals(Duration.ZERO, controller.timeUntilIdleLock())
    }

    @Test
    fun `Given a locked session, When the time until the idle lock is read, Then it is the full idle timeout`() {
        assertEquals(5.minutes, controller.timeUntilIdleLock())
    }

    @Test
    fun `Given another screen covering the wallet, When the wallet is shown again before the protected-flow timeout, Then it stays unlocked`() {
        controller.unlock()
        controller.onCoveredByOtherScreen()
        advance(9.minutes)

        assertFalse(controller.onWalletScreenShown())
        assertTrue(controller.isUnlocked)
        advance(4.minutes)
        assertFalse(controller.lockIfIdle())
    }

    @Test
    fun `Given another screen covering the wallet, When the wallet is shown again at the protected-flow timeout, Then it locks`() {
        controller.unlock()
        controller.onCoveredByOtherScreen()
        advance(4.minutes)
        controller.onCoveredByOtherScreen()
        advance(6.minutes)

        assertTrue(controller.onWalletScreenShown())
        assertFalse(controller.isUnlocked)
    }

    @Test
    fun `Given another screen covering the wallet, When the app returns from the background before the protected-flow timeout, Then it stays unlocked`() {
        controller.unlock()
        controller.onCoveredByOtherScreen()
        controller.onBackground()
        advance(9.minutes)

        assertFalse(controller.onForeground())
        assertTrue(controller.isUnlocked)
    }

    @Test
    fun `Given the wallet shown again after another screen, When the app returns at the background timeout, Then it locks`() {
        controller.unlock()
        controller.onCoveredByOtherScreen()
        controller.onWalletScreenShown()
        controller.onBackground()
        advance(60.seconds)

        assertTrue(controller.onForeground())
    }

    @Test
    fun `Given no other screen covered the wallet, When the wallet screen is shown, Then the idle timeout keeps running`() {
        controller.unlock()
        advance(5.minutes)

        assertFalse(controller.onWalletScreenShown())
        assertTrue(controller.lockIfIdle())
    }

    @Test
    fun `Given another screen covering the wallet, When the idle timeout passes, Then the session does not lock`() {
        controller.unlock()
        controller.onCoveredByOtherScreen()
        advance(6.minutes)

        assertFalse(controller.lockIfIdle())
        assertEquals(5.minutes, controller.timeUntilIdleLock())
        assertTrue(controller.isUnlocked)
    }

    @Test
    fun `Given a locked session covered by another screen, When the wallet is shown again, Then no new lock is reported`() {
        controller.onCoveredByOtherScreen()
        advance(30.minutes)

        assertFalse(controller.onWalletScreenShown())
        assertFalse(controller.isUnlocked)
    }

    @Test
    fun `Given a protected flow recorded and then cleared, When the app returns at the background timeout, Then it locks`() {
        controller.unlock()
        controller.recordProtectedFlow(inProtectedFlow = true)
        controller.recordProtectedFlow(inProtectedFlow = false)
        controller.onBackground()
        advance(60.seconds)

        assertTrue(controller.onForeground())
    }
}