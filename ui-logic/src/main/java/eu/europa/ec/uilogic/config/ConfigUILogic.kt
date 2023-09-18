package eu.europa.ec.uilogic.config

import eu.europa.ec.uilogic.analytics.AnalyticsProvider
import eu.europa.ec.uilogic.navigation.Screen

interface ConfigUILogic {
    /*
       Supported Analytics Provider, e.g Firebase
    */
    val analyticsProviders: List<AnalyticsProvider>
        get() = emptyList()

    /*
       Define the dashboard identifier
    */
    val landingScreenIdentifier: Screen
}