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

package eu.europa.ec.assemblylogic.di

import android.app.Application
import eu.europa.ec.analyticslogic.di.logicAnalyticsModule
import eu.europa.ec.authenticationlogic.di.logicAuthenticationModule
import eu.europa.ec.businesslogic.di.logicBusinessModule
import eu.europa.ec.commonfeature.di.featureCommonModule
import eu.europa.ec.corelogic.di.logicCoreModule
import eu.europa.ec.dashboardfeature.di.featureDashboardModule
import eu.europa.ec.issuancefeature.di.featureIssuanceModule
import eu.europa.ec.networklogic.di.logicNetworkModule
import eu.europa.ec.presentationfeature.di.featurePresentationModule
import eu.europa.ec.proximityfeature.di.featureProximityModule
import eu.europa.ec.resourceslogic.di.logicResourceModule
import eu.europa.ec.startupfeature.di.featureStartupModule
import eu.europa.ec.storagelogic.di.logicStorageModule
import eu.europa.ec.uilogic.di.logicUiModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

internal val eudiWalletModules: List<Module> = listOf(
    logicNetworkModule,
    logicUiModule,
    logicResourceModule,
    logicBusinessModule,
    logicAnalyticsModule,
    logicAuthenticationModule,
    logicCoreModule,
    logicStorageModule,
    featureCommonModule,
    featureDashboardModule,
    featureStartupModule,
    featurePresentationModule,
    featureProximityModule,
    featureIssuanceModule,
)

internal fun Application.setupKoin(): KoinApplication = startKoin {
    androidLogger()
    androidContext(this@setupKoin)
    workManagerFactory()
    modules(eudiWalletModules)
}