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

package eu.europa.ec.presentationfeature.router

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import eu.europa.ec.businesslogic.BuildConfig
import eu.europa.ec.presentationfeature.ui.loading.PresentationLoadingScreen
import eu.europa.ec.presentationfeature.ui.request.crossdevice.PresentationRequestCrossDeviceScreen
import eu.europa.ec.uilogic.navigation.ModuleRoute
import eu.europa.ec.uilogic.navigation.PresentationScreens
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.presentationGraph(navController: NavController) {
    navigation(
        startDestination = PresentationScreens.CrossDevice.screenRoute,
        route = ModuleRoute.PresentationModule.route
    ) {
        composable(
            route = PresentationScreens.CrossDevice.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + PresentationScreens.CrossDevice.screenRoute
                }
            )
        ) {
            PresentationRequestCrossDeviceScreen(
                navController,
                koinViewModel()
            )
        }

        composable(
            route = PresentationScreens.Loading.screenRoute,
        ) {
            PresentationLoadingScreen(
                navController,
                koinViewModel()
            )
        }
    }
}