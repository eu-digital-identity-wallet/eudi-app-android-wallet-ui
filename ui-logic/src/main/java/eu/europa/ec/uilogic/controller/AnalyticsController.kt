package eu.europa.ec.uilogic.controller

import eu.europa.ec.uilogic.config.ConfigUILogic

interface AnalyticsController {
    fun logScreen(name: String)
    fun logEvent(eventName: String, parameters: Map<String, String> = emptyMap())
}

class AnalyticsControllerImpl constructor(
    private val configUiLogic: ConfigUILogic,
) : AnalyticsController {

    override fun logScreen(name: String) {
        configUiLogic.analyticsProviders.forEach { analyticProvider ->
            analyticProvider.logScreen(name)
        }
    }

    override fun logEvent(eventName: String, parameters: Map<String, String>) {
        configUiLogic.analyticsProviders.forEach { analyticProvider ->
            analyticProvider.logEvent(eventName, parameters)
        }
    }
}
