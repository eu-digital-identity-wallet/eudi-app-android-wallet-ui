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

package eu.europa.ec.proximityfeature.router

import ProximityRequestScreen
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import eu.europa.ec.businesslogic.BuildConfig
import eu.europa.ec.proximityfeature.ui.loading.ProximityLoadingScreen
import eu.europa.ec.proximityfeature.ui.qr.ProximityQRScreen
import eu.europa.ec.uilogic.navigation.ModuleRoute
import eu.europa.ec.uilogic.navigation.ProximityScreens
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.featureProximityGraph(navController: NavController) {
    navigation(
        startDestination = ProximityScreens.QR.screenRoute,
        route = ModuleRoute.ProximityModule.route
    ) {
        // QR
        composable(
            route = ProximityScreens.QR.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + ProximityScreens.QR.screenRoute
                }
            )
        ) {
            ProximityQRScreen(
                navController,
                koinViewModel()
            )
        }

        // Request
        composable(
            route = ProximityScreens.Request.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + ProximityScreens.Request.screenRoute
                }
            )
        ) {
            ProximityRequestScreen(
                navController,
                koinViewModel()
            )
        }

        // Loading
        composable(
            route = ProximityScreens.Loading.screenRoute,
        ) {
            ProximityLoadingScreen(
                navController,
                koinViewModel()
            )
        }
    }
}