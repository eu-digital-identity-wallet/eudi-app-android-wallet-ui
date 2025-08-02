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

package eu.europa.ec.startupfeature.router

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import eu.europa.ec.startupfeature.BuildConfig
import eu.europa.ec.startupfeature.ui.splash.SplashScreen
import eu.europa.ec.uilogic.navigation.ModuleRoute
import eu.europa.ec.uilogic.navigation.StartupScreens
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.featureStartupGraph(navController: NavController) {
    navigation(
        startDestination = StartupScreens.Splash.screenRoute,
        route = ModuleRoute.StartupModule.route
    ) {
        composable(
            route = StartupScreens.Splash.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + StartupScreens.Splash.screenRoute
                }
            )
        ) {
            SplashScreen(navController, koinViewModel())
        }
    }
}