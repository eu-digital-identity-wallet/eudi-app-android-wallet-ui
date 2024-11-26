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

package eu.europa.ec.uilogic.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import eu.europa.ec.analyticslogic.controller.AnalyticsController
import eu.europa.ec.businesslogic.extension.firstPart
import eu.europa.ec.businesslogic.extension.toMapOrEmpty
import eu.europa.ec.uilogic.config.ConfigUILogic

interface RouterHost {
    fun getNavController(): NavHostController
    fun getNavContext(): Context
    fun userIsLoggedInWithDocuments(): Boolean
    fun userIsLoggedInWithNoDocuments(): Boolean
    fun popToDashboardScreen()
    fun popToIssuanceOnboardingScreen()
    fun isScreenOnBackStackOrForeground(screen: Screen): Boolean

    @Composable
    fun StartFlow(builder: NavGraphBuilder.(NavController) -> Unit)
}

class RouterHostImpl(
    private val configUILogic: ConfigUILogic,
    private val analyticsController: AnalyticsController
) : RouterHost {

    private lateinit var navController: NavHostController
    private lateinit var context: Context

    override fun getNavController(): NavHostController = navController
    override fun getNavContext(): Context = context

    @Composable
    override fun StartFlow(builder: NavGraphBuilder.(NavController) -> Unit) {
        navController = rememberNavController()
        context = LocalContext.current
        NavHost(
            navController = navController,
            startDestination = ModuleRoute.StartupModule.route
        ) {
            builder(navController)
        }
        navController.addOnDestinationChangedListener { _, destination, args ->
            destination.route?.let { route ->
                analyticsController.logScreen(route.firstPart("?"), args.toMapOrEmpty())
            }
        }
    }

    override fun userIsLoggedInWithDocuments(): Boolean =
        isScreenOnBackStackOrForeground(getDashboardScreen())

    override fun userIsLoggedInWithNoDocuments(): Boolean =
        isScreenOnBackStackOrForeground(getIssuanceScreen())

    override fun isScreenOnBackStackOrForeground(screen: Screen): Boolean {
        val screenRoute = screen.screenRoute
        try {
            if (navController.currentDestination?.route == screenRoute) {
                return true
            }
            navController.getBackStackEntry(screenRoute)
            return true
        } catch (_: Exception) {
            return false
        }
    }

    override fun popToDashboardScreen() {
        navController.popBackStack(
            route = getDashboardScreen().screenRoute,
            inclusive = false
        )
    }

    override fun popToIssuanceOnboardingScreen() {
        navController.popBackStack(
            route = getIssuanceScreen().screenRoute,
            inclusive = false
        )
    }

    private fun getDashboardScreen(): Screen {
        return configUILogic.dashboardScreenIdentifier
    }

    private fun getIssuanceScreen(): Screen {
        return configUILogic.issuanceScreenIdentifier
    }
}