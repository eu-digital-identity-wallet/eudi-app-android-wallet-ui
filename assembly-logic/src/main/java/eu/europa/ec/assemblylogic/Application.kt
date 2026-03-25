/*
 * Copyright (c) 2023 European Commission
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

package eu.europa.ec.assemblylogic

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import eu.europa.ec.analyticslogic.controller.AnalyticsController
import eu.europa.ec.assemblylogic.di.setupKoin
import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.corelogic.config.WalletCoreConfig
import eu.europa.ec.corelogic.worker.ReIssuanceWorkManager
import eu.europa.ec.corelogic.worker.RevocationWorkManager
import eu.europa.ec.eudi.rqesui.infrastructure.EudiRQESUi
import org.koin.android.ext.android.inject
import org.koin.core.KoinApplication
import java.time.Duration

class Application : Application() {

    private val analyticsController: AnalyticsController by inject()
    private val configLogic: ConfigLogic by inject()
    private val walletCoreConfig: WalletCoreConfig by inject()

    override fun onCreate() {
        super.onCreate()
        initializeKoin().initializeRqes()
        initializeReporting()
        initializeWorkManagers()
    }

    private fun KoinApplication.initializeRqes() {
        EudiRQESUi.setup(
            application = this@Application,
            config = configLogic.rqesConfig,
            koinApplication = this@initializeRqes
        )
    }

    private fun initializeKoin(): KoinApplication {
        return setupKoin()
    }

    private fun initializeReporting() {
        analyticsController.initialize(this)
    }

    private fun initializeWorkManagers() {
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

    private fun enqueuePeriodicWorker(
        uniqueName: String,
        duration: Duration,
        workerClass: Class<out ListenableWorker>
    ) {
        val periodicWorkRequest = PeriodicWorkRequest.Builder(
            workerClass,
            duration
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            uniqueName,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }
}