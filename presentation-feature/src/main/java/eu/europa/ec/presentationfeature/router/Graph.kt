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

package eu.europa.ec.presentationfeature.router

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import eu.europa.ec.businesslogic.BuildConfig
import eu.europa.ec.presentationfeature.ui.crossdevice.loading.PresentationCrossDeviceLoadingScreen
import eu.europa.ec.presentationfeature.ui.crossdevice.request.PresentationCrossDeviceRequestScreen
import eu.europa.ec.presentationfeature.ui.samedevice.loading.PresentationSameDeviceLoadingScreen
import eu.europa.ec.presentationfeature.ui.samedevice.request.PresentationSameDeviceRequestScreen
import eu.europa.ec.uilogic.navigation.ModuleRoute
import eu.europa.ec.uilogic.navigation.PresentationScreens
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.presentationGraph(navController: NavController) {
    navigation(
        startDestination = PresentationScreens.CrossDeviceRequest.screenRoute,
        route = ModuleRoute.PresentationModule.route
    ) {

        // Cross Device
        composable(
            route = PresentationScreens.CrossDeviceRequest.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + PresentationScreens.CrossDeviceRequest.screenRoute
                }
            )
        ) {
            PresentationCrossDeviceRequestScreen(
                navController,
                koinViewModel()
            )
        }

        composable(
            route = PresentationScreens.CrossDeviceLoading.screenRoute,
        ) {
            PresentationCrossDeviceLoadingScreen(
                navController,
                koinViewModel()
            )
        }

        // Same Device
        composable(
            route = PresentationScreens.SameDeviceRequest.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + PresentationScreens.SameDeviceRequest.screenRoute
                }
            )
        ) {
            PresentationSameDeviceRequestScreen(
                navController,
                koinViewModel()
            )
        }

        composable(
            route = PresentationScreens.SameDeviceLoading.screenRoute,
        ) {
            PresentationSameDeviceLoadingScreen(
                navController,
                koinViewModel()
            )
        }
    }
}