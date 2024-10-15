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
import eu.europa.ec.analyticslogic.controller.AnalyticsController
import eu.europa.ec.assemblylogic.di.setupKoin
import eu.europa.ec.corelogic.config.WalletCoreConfig
import eu.europa.ec.eudi.wallet.EudiWallet
import org.koin.android.ext.android.inject

class Application : Application() {

    private val configWalletCore: WalletCoreConfig by inject()
    private val analyticsController: AnalyticsController by inject()

    override fun onCreate() {
        super.onCreate()
        setupKoin()
        initializeReporting()
        initializeEudiWallet()
    }

    private fun initializeReporting() {
        analyticsController.initialize(this)
    }

    private fun initializeEudiWallet() {
        EudiWallet.init(
            applicationContext,
            configWalletCore.config
        )
    }
}