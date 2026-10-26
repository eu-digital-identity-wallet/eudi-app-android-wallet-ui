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

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import eu.europa.ec.corelogic.config.WalletCoreConfig
import eu.europa.ec.corelogic.controller.WalletResetStatus
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
class TestWalletWorkScheduler {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private val context: Context = RuntimeEnvironment.getApplication()

    private val walletCoreConfig: WalletCoreConfig = mock(defaultAnswer = RETURNS_DEEP_STUBS)
    private val resetStatus = WalletResetStatus()

    @Before
    fun before() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
    }

    @After
    fun after() {
        WorkManagerTestInitHelper.closeWorkDatabase()
    }

    //region stop
    @Test
    fun `Given no worker is working, When the work is stopped, Then it returns true`() {
        coroutineRule.testScope.runTest {
            val scheduler = buildScheduler(workerLocks = listOf(Mutex(), Mutex()))

            assertTrue(scheduler.stop())
        }
    }

    // Case 2:
    // 1. Re-issuance is still finishing after work has been cancelled.
    // 2. It requests another revocation check before releasing its lock.
    //
    // Case 2 Expected Result:
    // Stopping waits for re-issuance to finish without queuing another check.
    @Test
    fun `Given a worker is finishing, When stop waits, Then a requested revocation check is not queued`() {
        coroutineRule.testScope.runTest {
            // Given
            val workerLock = Mutex(locked = true)
            val scheduler = buildScheduler(workerLocks = listOf(Mutex(), workerLock))

            // When
            val stopped = async { scheduler.stop() }
            runCurrent()

            assertFalse(stopped.isCompleted)
            scheduler.scheduleRevocationCheck()
            workerLock.unlock()

            // Then
            assertTrue(stopped.await())
            assertEquals(0, startupCheckCount())
        }
    }

    @Test
    fun `Given a worker that does not finish, When the work is stopped, Then it returns false when the wait ends`() {
        coroutineRule.testScope.runTest {
            val scheduler = buildScheduler(workerLocks = listOf(Mutex(locked = true)))

            assertFalse(scheduler.stop())
        }
    }

    // Case 4:
    // 1. Work has stopped and no reset is running.
    //
    // Case 4 Expected Result:
    // One-time checks stay blocked until scheduling resumes.
    @Test
    fun `Given work has stopped, When one-time checks are requested, Then no work is queued`() {
        coroutineRule.testScope.runTest {
            // Given
            val scheduler = buildScheduler(workerLocks = emptyList())
            assertTrue(scheduler.stop())

            // When
            scheduler.scheduleStartupCheck()
            scheduler.scheduleRevocationCheck()

            // Then
            assertEquals(0, startupCheckCount())
        }
    }
    //endregion

    //region schedule
    // Case 1:
    // 1. Work has stopped but the reset is still running.
    //
    // Case 1 Expected Result:
    // Scheduling cannot restart any checks during deletion.
    @Test
    fun `Given a reset is running, When background work is scheduled, Then no work is queued`() {
        coroutineRule.testScope.runTest {
            // Given
            val scheduler = buildScheduler(workerLocks = emptyList())
            resetStatus.setResetting(true)
            assertTrue(scheduler.stop())

            // When
            scheduler.schedule()
            scheduler.scheduleStartupCheck()
            scheduler.scheduleRevocationCheck()

            // Then
            assertEquals(0, startupCheckCount())
            assertTrue(workIds(RevocationWorkManager.REVOCATION_WORK_NAME).isEmpty())
            assertTrue(workIds(ReIssuanceWorkManager.RE_ISSUANCE_WORK_NAME).isEmpty())
        }
    }

    // Case 2:
    // 1. A reset has finished after stopping work.
    // 2. A start-up check was requested while scheduling was stopped.
    //
    // Case 2 Expected Result:
    // Resuming queues the periodic work and allows the start-up check.
    @Test
    fun `Given a reset has finished, When scheduling resumes, Then periodic and start-up checks are queued`() {
        coroutineRule.testScope.runTest {
            // Given
            val scheduler = buildScheduler(workerLocks = emptyList())
            resetStatus.setResetting(true)
            assertTrue(scheduler.stop())
            scheduler.scheduleStartupCheck()
            resetStatus.setResetting(false)

            // When
            scheduler.schedule()
            scheduler.scheduleStartupCheck()

            // Then
            assertEquals(1, startupCheckCount())
            assertEquals(1, workIds(RevocationWorkManager.REVOCATION_WORK_NAME).size)
            assertEquals(1, workIds(ReIssuanceWorkManager.RE_ISSUANCE_WORK_NAME).size)
        }
    }

    // Case 3:
    // 1. Stopping work timed out and the reset ended.
    //
    // Case 3 Expected Result:
    // A revocation check is blocked until failure recovery resumes scheduling.
    @Test
    fun `Given stopping timed out, When scheduling resumes, Then revocation checks can be queued again`() {
        coroutineRule.testScope.runTest {
            // Given
            val scheduler = buildScheduler(workerLocks = listOf(Mutex(locked = true)))
            resetStatus.setResetting(true)
            assertFalse(scheduler.stop())
            resetStatus.setResetting(false)
            scheduler.scheduleRevocationCheck()
            assertEquals(0, startupCheckCount())

            // When
            scheduler.schedule()
            scheduler.scheduleRevocationCheck()

            // Then
            assertEquals(1, startupCheckCount())
        }
    }
    //endregion

    @Test
    fun `Given the periodic work is scheduled, When nothing else is called, Then no start-up check is queued`() {
        buildScheduler(workerLocks = emptyList()).schedule()

        assertEquals(0, startupCheckCount())
    }

    @Test
    fun `Given the start-up check is asked for twice in a process, When it is scheduled, Then it is queued once`() {
        val scheduler = buildScheduler(workerLocks = emptyList())

        scheduler.scheduleStartupCheck()
        val queued = startupCheckIds()
        scheduler.scheduleStartupCheck()

        assertEquals(1, queued.size)
        assertEquals(queued, startupCheckIds())
    }

    @Test
    fun `Given the periodic work is scheduled, When it is scheduled again, Then the queued work is kept`() {
        val scheduler = buildScheduler(workerLocks = emptyList())

        scheduler.schedule()
        val revocation = workIds(RevocationWorkManager.REVOCATION_WORK_NAME)
        val reIssuance = workIds(ReIssuanceWorkManager.RE_ISSUANCE_WORK_NAME)
        scheduler.schedule()

        assertEquals(1, revocation.size)
        assertEquals(1, reIssuance.size)
        assertEquals(revocation, workIds(RevocationWorkManager.REVOCATION_WORK_NAME))
        assertEquals(reIssuance, workIds(ReIssuanceWorkManager.RE_ISSUANCE_WORK_NAME))
    }

    //region helper functions
    private fun startupCheckCount(): Int = startupCheckIds().size

    private fun startupCheckIds(): List<UUID> =
        workIds(RevocationWorkManager.REVOCATION_ON_START_WORK_NAME)

    private fun workIds(uniqueWorkName: String): List<UUID> = WorkManager.getInstance(context)
        .getWorkInfosForUniqueWork(uniqueWorkName)
        .get()
        .map { it.id }

    private fun buildScheduler(workerLocks: List<Mutex>) = WalletWorkSchedulerImpl(
        context = context,
        walletCoreConfig = walletCoreConfig,
        resetStatus = resetStatus,
        stopTimeout = 30.seconds,
        workerLocks = workerLocks,
    )
    //endregion
}