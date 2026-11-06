/*
 * Copyright (c) 2026 European Commission
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

import androidx.activity.ComponentActivity
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.navigation
import androidx.navigation.testing.TestNavHostController
import eu.europa.ec.authenticationlogic.controller.session.SessionController
import eu.europa.ec.uilogic.config.ConfigUILogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TestRouterHost {

    private val context: ComponentActivity =
        Robolectric.buildActivity(ComponentActivity::class.java).setup().get()

    private val sessionController: SessionController = mock()

    private val configUILogic: ConfigUILogic = mock()

    private val navController = TestNavHostController(context).apply {
        navigatorProvider.addNavigator(ComposeNavigator())
    }

    private val routerHost = RouterHostImpl(
        configUILogic = configUILogic,
        analyticsController = mock(),
        sessionController = sessionController,
    )

    private val addDocument = Screen(name = "ADD_DOCUMENT")

    private val signing = Screen(name = "SIGNING")

    private val login = Screen(name = "LOGIN")

    @Before
    fun before() {
        whenever(configUILogic.loginScreens).thenReturn(listOf(StartupScreens.Splash))
        whenever(configUILogic.protectedFlowScreens).thenReturn(listOf(signing))
        whenever(configUILogic.dashboardScreenIdentifier).thenReturn(DashboardScreens.Dashboard)
        whenever(configUILogic.issuanceScreenIdentifier).thenReturn(addDocument)
        navController.graph =
            navController.createGraph(startDestination = ModuleRoute.StartupModule.route) {
                navigation(
                    startDestination = StartupScreens.Splash.screenRoute,
                    route = ModuleRoute.StartupModule.route,
                ) {
                    composable(StartupScreens.Splash.screenRoute) {}
                }
                composable(DashboardScreens.Dashboard.screenRoute) {}
                composable(addDocument.screenRoute) {}
                composable(signing.screenRoute) {}
                composable(login.screenRoute) {}
            }
        routerHost.attach(navController = navController, context = context)
    }

    @Test
    fun `Given a locked wallet showing the dashboard, When requireLoginIfLocked is called, Then only the login flow is left`() {
        whenever(sessionController.isUnlocked).thenReturn(false)
        navController.navigate(DashboardScreens.Dashboard.screenRoute)

        routerHost.requireLoginIfLocked()

        assertEquals(StartupScreens.Splash.screenRoute, navController.currentDestination?.route)
        assertFalse(
            runCatching { navController.getBackStackEntry(DashboardScreens.Dashboard.screenRoute) }.isSuccess
        )
    }

    @Test
    fun `Given an unlocked wallet showing the dashboard, When requireLoginIfLocked is called, Then the dashboard stays`() {
        whenever(sessionController.isUnlocked).thenReturn(true)
        navController.navigate(DashboardScreens.Dashboard.screenRoute)

        routerHost.requireLoginIfLocked()

        assertEquals(
            DashboardScreens.Dashboard.screenRoute,
            navController.currentDestination?.route
        )
    }

    @Test
    fun `Given a locked wallet showing only login screens, When requireLoginIfLocked is called, Then nothing changes`() {
        whenever(sessionController.isUnlocked).thenReturn(false)
        val entry = navController.currentBackStackEntry

        routerHost.requireLoginIfLocked()

        assertEquals(StartupScreens.Splash.screenRoute, navController.currentDestination?.route)
        assertSame(entry, navController.currentBackStackEntry)
    }

    @Test
    fun `Given the dashboard on the back stack, When the login state is read, Then the wallet counts as logged in only while unlocked`() {
        navController.navigate(DashboardScreens.Dashboard.screenRoute)

        whenever(sessionController.isUnlocked).thenReturn(true)
        assertTrue(routerHost.userIsLoggedInWithDocuments())

        whenever(sessionController.isUnlocked).thenReturn(false)
        assertFalse(routerHost.userIsLoggedInWithDocuments())
    }

    @Test
    fun `Given the add-document screen on the back stack, When the login state is read, Then the wallet counts as logged in only while unlocked`() {
        navController.navigate(addDocument.screenRoute)

        whenever(sessionController.isUnlocked).thenReturn(true)
        assertTrue(routerHost.userIsLoggedInWithNoDocuments())
        assertFalse(routerHost.userIsLoggedInWithDocuments())

        whenever(sessionController.isUnlocked).thenReturn(false)
        assertFalse(routerHost.userIsLoggedInWithNoDocuments())
    }

    @Test
    fun `Given a protected-flow screen in front, When isInProtectedFlow is read, Then it is true only there`() {
        navController.navigate(DashboardScreens.Dashboard.screenRoute)
        assertFalse(routerHost.isInProtectedFlow())

        navController.navigate(signing.screenRoute)
        assertTrue(routerHost.isInProtectedFlow())
    }

    @Test
    fun `Given a protected-flow screen that is also a login screen, When isInProtectedFlow is read, Then it is true only past login`() {
        whenever(configUILogic.loginScreens).thenReturn(listOf(StartupScreens.Splash, login))
        whenever(configUILogic.protectedFlowScreens).thenReturn(listOf(login))

        navController.navigate(login.screenRoute)
        assertFalse(routerHost.isInProtectedFlow())

        navController.navigate(DashboardScreens.Dashboard.screenRoute)
        navController.navigate(login.screenRoute)
        assertTrue(routerHost.isInProtectedFlow())
    }
}