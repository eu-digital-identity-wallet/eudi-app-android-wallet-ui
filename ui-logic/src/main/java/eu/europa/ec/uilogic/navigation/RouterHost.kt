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

package eu.europa.ec.uilogic.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import eu.europa.ec.uilogic.config.ConfigUILogic
import eu.europa.ec.uilogic.controller.AnalyticsController

interface RouterHost {
    fun getNavController(): NavHostController
    fun getNavContext(): Context
    fun currentFlowIsAfterOnBoarding(): Boolean
    fun popToLandingScreen()
    fun getLandingScreen(): String

    @Composable
    fun StartFlow(builder: NavGraphBuilder.(NavController) -> Unit)
}

class RouterHostImpl constructor(
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
        navController.addOnDestinationChangedListener { _, destination, _ ->
            destination.route?.let { route ->
                analyticsController.logScreen(route)
            }
        }
    }

    override fun currentFlowIsAfterOnBoarding(): Boolean {
        val screenRoute = getLandingScreen()
        try {
            if (navController.currentDestination?.route == screenRoute) {
                return true
            }
            navController.getBackStackEntry(screenRoute)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    override fun popToLandingScreen() {
        navController.popBackStack(
            route = getLandingScreen(),
            inclusive = false
        )
    }

    override fun getLandingScreen(): String {
        return configUILogic.landingScreenIdentifier.screenRoute
    }
}