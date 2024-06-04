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

package eu.europa.ec.issuancefeature.router

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import eu.europa.ec.commonfeature.config.IssuanceFlowUiConfig
import eu.europa.ec.commonfeature.config.OfferUiConfig
import eu.europa.ec.issuancefeature.BuildConfig
import eu.europa.ec.issuancefeature.ui.document.add.AddDocumentScreen
import eu.europa.ec.issuancefeature.ui.document.details.DocumentDetailsScreen
import eu.europa.ec.issuancefeature.ui.document.offer.DocumentOfferScreen
import eu.europa.ec.issuancefeature.ui.success.SuccessScreen
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.ModuleRoute
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

fun NavGraphBuilder.featureIssuanceGraph(navController: NavController) {
    navigation(
        startDestination = IssuanceScreens.AddDocument.screenRoute,
        route = ModuleRoute.IssuanceModule.route
    ) {
        // Add Document
        composable(
            route = IssuanceScreens.AddDocument.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + IssuanceScreens.AddDocument.screenRoute
                }
            ),
            arguments = listOf(
                navArgument("flowType") {
                    type = NavType.StringType
                },
            )
        ) {
            AddDocumentScreen(
                navController,
                getViewModel(
                    parameters = {
                        parametersOf(
                            IssuanceFlowUiConfig.fromString(
                                it.arguments?.getString("flowType").orEmpty()
                            ),
                        )
                    }
                )
            )
        }

        // Success
        composable(
            route = IssuanceScreens.Success.screenRoute,
            arguments = listOf(
                navArgument("flowType") {
                    type = NavType.StringType
                },
                navArgument("documentId") {
                    type = NavType.StringType
                },
            )
        ) {
            SuccessScreen(
                navController,
                getViewModel(
                    parameters = {
                        parametersOf(
                            IssuanceFlowUiConfig.fromString(
                                it.arguments?.getString("flowType").orEmpty()
                            ),
                            it.arguments?.getString("documentId").orEmpty(),
                        )
                    }
                )
            )
        }

        // Document Details
        composable(
            route = IssuanceScreens.DocumentDetails.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + IssuanceScreens.DocumentDetails.screenRoute
                }
            ),
            arguments = listOf(
                navArgument("detailsType") {
                    type = NavType.StringType
                },
                navArgument("documentId") {
                    type = NavType.StringType
                },
                navArgument("nameSpace") {
                    type = NavType.StringType
                },
            )
        ) {
            DocumentDetailsScreen(
                navController,
                getViewModel(
                    parameters = {
                        parametersOf(
                            IssuanceFlowUiConfig.fromString(
                                it.arguments?.getString("detailsType").orEmpty()
                            ),
                            it.arguments?.getString("documentId").orEmpty(),
                            it.arguments?.getString("nameSpace").orEmpty(),
                        )
                    }
                )
            )
        }

        // Document Offer
        composable(
            route = IssuanceScreens.DocumentOffer.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + IssuanceScreens.DocumentOffer.screenRoute
                }
            ),
            arguments = listOf(
                navArgument(OfferUiConfig.serializedKeyName) {
                    type = NavType.StringType
                },
            )
        ) {
            DocumentOfferScreen(
                navController,
                getViewModel(
                    parameters = {
                        parametersOf(
                            it.arguments?.getString(OfferUiConfig.serializedKeyName).orEmpty()
                        )
                    }
                )
            )
        }
    }
}