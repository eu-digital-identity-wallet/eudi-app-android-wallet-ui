/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.analyticslogic.di

import eu.europa.ec.analyticslogic.config.AnalyticsConfig
import eu.europa.ec.analyticslogic.controller.AnalyticsController
import eu.europa.ec.analyticslogic.controller.AnalyticsControllerImpl
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("eu.europa.ec.analyticslogic")
class LogicAnalyticsModule

@Single
fun provideAnalyticsConfig(): AnalyticsConfig {
    return try {
        val impl = Class.forName("eu.europa.ec.analyticslogic.config.AnalyticsConfigImpl")
        return impl.getDeclaredConstructor().newInstance() as AnalyticsConfig
    } catch (_: Exception) {
        val impl = object : AnalyticsConfig {}
        impl
    }
}

@Single
fun provideAnalyticsController(analyticsConfig: AnalyticsConfig): AnalyticsController =
    AnalyticsControllerImpl(analyticsConfig)