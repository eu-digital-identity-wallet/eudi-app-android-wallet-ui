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

package eu.europa.ec.dashboardfeature.router

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import eu.europa.ec.businesslogic.BuildConfig
import eu.europa.ec.dashboardfeature.ui.adddocument.AddDocumentScreen
import eu.europa.ec.dashboardfeature.ui.dashboard.DashboardScreen
import eu.europa.ec.dashboardfeature.ui.details.DocumentDetailsScreen
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.ModuleRoute
import org.koin.androidx.compose.getViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

fun NavGraphBuilder.featureDashboardGraph(navController: NavController) {
    navigation(
        startDestination = DashboardScreens.Dashboard.screenRoute,
        route = ModuleRoute.DashboardModule.route
    ) {
        composable(
            route = DashboardScreens.Dashboard.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + DashboardScreens.Dashboard.screenRoute
                }
            )
        ) {
            DashboardScreen(navController, koinViewModel())
        }
    }

    composable(
        route = DashboardScreens.AddDocument.screenRoute,
        deepLinks = listOf(
            navDeepLink {
                uriPattern =
                    BuildConfig.DEEPLINK + DashboardScreens.AddDocument.screenRoute
            }
        )
    ) {
        AddDocumentScreen(navController, koinViewModel())
    }

    composable(
        route = DashboardScreens.DocumentDetails.screenRoute,
        deepLinks = listOf(
            navDeepLink {
                uriPattern =
                    BuildConfig.DEEPLINK + DashboardScreens.DocumentDetails.screenRoute
            }
        ),
        arguments = listOf(
            navArgument("documentId") {
                type = NavType.StringType
            }
        )
    ) {
        DocumentDetailsScreen(
            navController,
            getViewModel(
                parameters = {
                    parametersOf(
                        it.arguments?.getString("documentId").orEmpty()
                    )
                }
            )
        )
    }
}