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

package eu.europa.ec.analyticslogic.controller

import android.app.Application
import eu.europa.ec.analyticslogic.config.AnalyticsConfig

interface AnalyticsController {
    fun initialize(context: Application)
    fun logScreen(name: String, arguments: Map<String, String> = emptyMap())
    fun logEvent(eventName: String, arguments: Map<String, String> = emptyMap())
}

class AnalyticsControllerImpl(
    private val analyticsConfig: AnalyticsConfig,
) : AnalyticsController {

    override fun initialize(context: Application) {
        analyticsConfig.analyticsProviders.forEach { (key, analyticProvider) ->
            analyticProvider.initialize(context, key)
        }
    }

    override fun logScreen(name: String, arguments: Map<String, String>) {
        analyticsConfig.analyticsProviders.values.forEach {
            it.logScreen(name, arguments)
        }
    }

    override fun logEvent(eventName: String, arguments: Map<String, String>) {
        analyticsConfig.analyticsProviders.values.forEach {
            it.logEvent(eventName, arguments)
        }
    }
}
