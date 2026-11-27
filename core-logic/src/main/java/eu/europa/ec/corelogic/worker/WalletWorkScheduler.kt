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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.await
import eu.europa.ec.corelogic.config.WalletCoreConfig
import eu.europa.ec.corelogic.controller.WalletDocumentOperationController
import eu.europa.ec.corelogic.controller.WalletResetStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/** Schedules the wallet's background workers, and stops them for the factory reset. */
interface WalletWorkScheduler {

    /**
     * Resumes periodic checks and allows document operations again unless reset is running.
     * Existing scheduled work is kept; Dashboard and a failed reset may call this repeatedly.
     */
    fun schedule()

    /**
     * Runs the revocation check once, the first time it is called in the process, so that a status
     * change, or revocation records lost when the database is recreated after a schema change, is
     * picked up without waiting for the periodic interval. Call it once the wallet's session id is
     * set: a check that started earlier would open Wallet Core under the previous run's session id,
     * and the wallet's screens would then open a second instance on the same storage. The
     * dashboard calls it once the user has logged in, after the activity has set the session id.
     * The periodic runs are not ordered this way; see the Go-Live guide on revocation checks.
     */
    fun scheduleStartupCheck()

    /**
     * Runs the revocation check once more, after any run already queued or running, so that the
     * documents of a re-issuance that completed while its original gained a revocation record are
     * checked without waiting for the periodic interval.
     */
    fun scheduleRevocationCheck()

    /**
     * Blocks new work and waits for worker cancellation, active workers and tracked document
     * operations within one [WalletCoreConfig.workerStopTimeout]. False means reset must not start
     * deleting data.
     */
    suspend fun stop(): Boolean
}

class WalletWorkSchedulerImpl(
    private val context: Context,
    private val walletCoreConfig: WalletCoreConfig,
    private val resetStatus: WalletResetStatus,
    private val walletDocumentOperationController: WalletDocumentOperationController,
    private val stopTimeout: Duration = walletCoreConfig.workerStopTimeout,
    private val workerLocks: List<Mutex> = listOf(
        RevocationWorkManager.runLock,
        ReIssuanceWorkManager.runLock,
    ),
) : WalletWorkScheduler {

    private companion object {
        val WORK_NAMES = listOf(
            RevocationWorkManager.REVOCATION_ON_START_WORK_NAME,
            RevocationWorkManager.REVOCATION_WORK_NAME,
            ReIssuanceWorkManager.RE_ISSUANCE_WORK_NAME,
        )
    }

    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    private val startupCheckScheduled = AtomicBoolean(false)
    private var isStopped = false

    @Synchronized
    override fun schedule() {
        if (resetStatus.isResetting.value) return
        isStopped = false
        walletDocumentOperationController.resume()
        enqueuePeriodicWorker(
            RevocationWorkManager.REVOCATION_WORK_NAME,
            walletCoreConfig.revocationInterval,
            RevocationWorkManager::class.java
        )
        enqueuePeriodicWorker(
            ReIssuanceWorkManager.RE_ISSUANCE_WORK_NAME,
            walletCoreConfig.documentIssuanceConfig.reissuanceRule.backgroundInterval,
            ReIssuanceWorkManager::class.java
        )
    }

    @Synchronized
    override fun scheduleStartupCheck() {
        if (isStopped) return
        if (!startupCheckScheduled.compareAndSet(false, true)) return
        workManager.enqueueUniqueWork(
            RevocationWorkManager.REVOCATION_ON_START_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequest.from(RevocationWorkManager::class.java)
        )
    }

    @Synchronized
    override fun scheduleRevocationCheck() {
        if (isStopped) return
        workManager.enqueueUniqueWork(
            RevocationWorkManager.REVOCATION_ON_START_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequest.from(RevocationWorkManager::class.java)
        )
    }

    /**
     * WorkManager cancellation does not mean a worker has finished its Core callbacks, so the wait
     * is also on the locks the workers hold while they work.
     */
    override suspend fun stop(): Boolean {
        synchronized(this) {
            isStopped = true
        }
        walletDocumentOperationController.stop()
        return withTimeoutOrNull(stopTimeout) {
            WORK_NAMES.forEach { name -> workManager.cancelUniqueWork(name).await() }
            workerLocks.forEach { lock -> lock.withLock {} }
            walletDocumentOperationController.awaitIdle()
            true
        } ?: false
    }

    private fun enqueuePeriodicWorker(
        uniqueName: String,
        duration: Duration,
        workerClass: Class<out ListenableWorker>
    ) {
        val periodicWorkRequest = PeriodicWorkRequest.Builder(
            workerClass,
            duration.toJavaDuration()
        ).build()

        workManager.enqueueUniquePeriodicWork(
            uniqueName,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }
}