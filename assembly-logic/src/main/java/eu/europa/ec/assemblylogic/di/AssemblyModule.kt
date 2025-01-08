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

package eu.europa.ec.assemblylogic.di

import android.app.Application
import eu.europa.ec.analyticslogic.di.LogicAnalyticsModule
import eu.europa.ec.authenticationlogic.di.LogicAuthenticationModule
import eu.europa.ec.businesslogic.di.LogicBusinessModule
import eu.europa.ec.commonfeature.di.FeatureCommonModule
import eu.europa.ec.corelogic.di.LogicCoreModule
import eu.europa.ec.dashboardfeature.di.FeatureDashboardModule
import eu.europa.ec.issuancefeature.di.FeatureIssuanceModule
import eu.europa.ec.networklogic.di.LogicNetworkModule
import eu.europa.ec.presentationfeature.di.FeaturePresentationModule
import eu.europa.ec.proximityfeature.di.FeatureProximityModule
import eu.europa.ec.resourceslogic.di.LogicResourceModule
import eu.europa.ec.startupfeature.di.FeatureStartupModule
import eu.europa.ec.storagelogic.di.LogicStorageModule
import eu.europa.ec.uilogic.di.LogicUiModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.KoinApplication
import org.koin.core.context.GlobalContext.startKoin
import org.koin.ksp.generated.module

private val assembledModules = listOf(

    // Logic Modules
    LogicNetworkModule().module,
    LogicUiModule().module,
    LogicResourceModule().module,
    LogicBusinessModule().module,
    LogicAnalyticsModule().module,
    LogicAuthenticationModule().module,
    LogicCoreModule().module,
    LogicStorageModule().module,

    // Feature Modules
    FeatureCommonModule().module,
    FeatureDashboardModule().module,
    FeatureStartupModule().module,
    FeaturePresentationModule().module,
    FeatureProximityModule().module,
    FeatureIssuanceModule().module
)

internal fun Application.setupKoin(): KoinApplication {
    return startKoin {
        androidContext(this@setupKoin)
        androidLogger()
        modules(assembledModules)
    }
}