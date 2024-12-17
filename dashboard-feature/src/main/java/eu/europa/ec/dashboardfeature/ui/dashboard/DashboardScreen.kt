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

package eu.europa.ec.dashboardfeature.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import eu.europa.ec.dashboardfeature.ui.BottomNavigationBar
import eu.europa.ec.dashboardfeature.ui.BottomNavigationItem
import eu.europa.ec.dashboardfeature.ui.documents.DocumentsScreen
import eu.europa.ec.dashboardfeature.ui.home.HomeScreen
import eu.europa.ec.dashboardfeature.ui.transactions.TransactionsScreen
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews

@Composable
fun DashboardScreen(hostNavController: NavController, viewModel: DashboardViewModel) {
    val bottomNavigationController = rememberNavController()

    ContentScreen(
        isLoading = false,
        navigatableAction = ScreenNavigateAction.NONE,
        onBack = { viewModel.setEvent(Event.Pop) },
        bottomBar = { BottomNavigationBar(bottomNavigationController) }
    ) {
        NavHost(
            navController = bottomNavigationController,
            startDestination = BottomNavigationItem.Home.route
        ) {
            composable(BottomNavigationItem.Home.route) { HomeScreen(hostNavController) }
            composable(BottomNavigationItem.Documents.route) { DocumentsScreen(hostNavController) }
            composable(BottomNavigationItem.Transactions.route) {
                TransactionsScreen(
                    hostNavController
                )
            }
        }
    }
}

@Composable
@ThemeModePreviews
fun DashboardScreenPreview() {
    DashboardScreen(rememberNavController(), DashboardViewModel())
}