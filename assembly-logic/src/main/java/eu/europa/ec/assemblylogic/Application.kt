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
import android.os.StrictMode
import eu.europa.ec.analyticslogic.controller.AnalyticsController
import eu.europa.ec.assemblylogic.di.setupKoin
import eu.europa.ec.businesslogic.config.ConfigSecurityLogic
import eu.europa.ec.businesslogic.config.WalletCoreConfig
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.resourceslogic.theme.ThemeManager
import eu.europa.ec.resourceslogic.theme.templates.ThemeDimensTemplate
import eu.europa.ec.resourceslogic.theme.values.ThemeColors
import eu.europa.ec.resourceslogic.theme.values.ThemeShapes
import eu.europa.ec.resourceslogic.theme.values.ThemeTypography
import org.koin.android.ext.android.inject

class Application : Application() {

    private val logController: LogController by inject()
    private val configWalletCore: WalletCoreConfig by inject()
    private val configSecurityLogic: ConfigSecurityLogic by inject()
    private val analyticsController: AnalyticsController by inject()

    override fun onCreate() {
        super.onCreate()
        setupKoin()
        initializeReporting()
        initializeEudiWallet()
        initializeLogging()
        initializeTheme()
        handleStrictMode()
    }

    private fun initializeReporting() {
        analyticsController.initialize(this)
    }

    private fun initializeLogging() {
        logController.install()
    }

    private fun initializeTheme() {
        ThemeManager.Builder()
            .withLightColors(ThemeColors.lightColors)
            .withDarkColors(ThemeColors.darkColors)
            .withTypography(ThemeTypography.typo)
            .withShapes(ThemeShapes.shapes)
            .withDimensions(
                ThemeDimensTemplate(
                    screenPadding = 10.0
                )
            )
            .build()
    }

    private fun initializeEudiWallet() {
        EudiWallet.init(
            applicationContext,
            configWalletCore.config
        )
    }

    private fun handleStrictMode() {
        if (configSecurityLogic.enableStrictMode) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
    }
}