/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.assemblylogic.di

import android.app.Application
import eu.europa.ec.authenticationfeature.di.FeatureAuthenticationModule
import eu.europa.ec.businesslogic.di.LogicBusinessModule
import eu.europa.ec.commonfeature.di.FeatureCommonModule
import eu.europa.ec.loginfeature.di.LoginModule
import eu.europa.ec.dashboardfeature.di.FeatureDashboardModule
import eu.europa.ec.networklogic.di.LogicNetworkModule
import eu.europa.ec.resourceslogic.di.LogicResourceModule
import eu.europa.ec.startupfeature.di.FeatureStartupModule
import eu.europa.ec.uilogic.di.LogicUiModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.ksp.generated.module

private val assembledModules = listOf(

    // Logic Modules
    LogicNetworkModule().module,
    LogicUiModule().module,
    LogicResourceModule().module,
    LogicBusinessModule().module,

    // Feature Modules
    FeatureCommonModule().module,
    FeatureStartupModule().module,
    LoginModule().module,
    FeatureDashboardModule().module,
    FeatureAuthenticationModule().module
)

internal fun Application.setupKoin() {
    startKoin {
        androidContext(this@setupKoin)
        androidLogger()
        modules(assembledModules)
    }
}