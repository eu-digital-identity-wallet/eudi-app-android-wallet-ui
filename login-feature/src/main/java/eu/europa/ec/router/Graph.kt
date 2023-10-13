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

package eu.europa.ec.router

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import eu.europa.ec.businesslogic.BuildConfig
import eu.europa.ec.onlineAuthentication.ui.loading.OnlineAuthenticationLoadingScreen
import eu.europa.ec.onlineAuthentication.ui.userData.UserDataScreen
import eu.europa.ec.uilogic.navigation.ModuleRoute
import eu.europa.ec.uilogic.navigation.OnlineAuthenticationScreens
import org.koin.androidx.compose.getViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

fun NavGraphBuilder.featureOnlineAuthenticationGraph(navController: NavController) {
    navigation(
        startDestination = OnlineAuthenticationScreens.UserData.screenRoute,
        route = ModuleRoute.OnlineAuthenticationModule.route
    ) {
        composable(
            route = OnlineAuthenticationScreens.UserData.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + OnlineAuthenticationScreens.UserData.screenRoute
                }
            ),
            arguments = listOf(
                navArgument("userId") {
                    type = NavType.StringType
                }
            )
        ) {
            UserDataScreen(
                navController,
                getViewModel(
                    parameters = {
                        parametersOf(
                            it.arguments?.getString("userId").orEmpty()
                        )
                    }
                )
            )
        }

        composable(
            route = OnlineAuthenticationScreens.Loading.screenRoute,
        ) {
            OnlineAuthenticationLoadingScreen(
                navController,
                koinViewModel()
            )
        }
    }
}