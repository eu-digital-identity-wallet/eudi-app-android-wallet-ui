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

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import eu.europa.ec.analyticslogic.controller.AnalyticsController
import eu.europa.ec.authenticationlogic.controller.session.SessionController
import eu.europa.ec.businesslogic.extension.firstPart
import eu.europa.ec.businesslogic.extension.toMapOrEmpty
import eu.europa.ec.uilogic.config.ConfigUILogic
import eu.europa.ec.uilogic.extension.destinationRoutes
import eu.europa.ec.uilogic.navigation.helper.isProtectedRoute
import eu.europa.ec.uilogic.navigation.helper.requiresLogin

interface RouterHost {
    fun getNavController(): NavHostController
    fun getNavContext(): Context

    /**
     * True when the session is unlocked and the dashboard is on the back stack. The back stack
     * alone is not proof of login: the dashboard stays on it after the session locks.
     */
    fun userIsLoggedInWithDocuments(): Boolean

    /** As [userIsLoggedInWithDocuments], for a wallet that has reached the add-document screen. */
    fun userIsLoggedInWithNoDocuments(): Boolean
    fun popToDashboardScreen()
    fun popToIssuanceOnboardingScreen()
    fun isScreenOnBackStackOrForeground(screen: Screen): Boolean

    /**
     * True once [StartFlow] has created the navigation controller. A recreated activity gets a
     * new instance, which is not ready until its first composition.
     */
    fun isNavigationReady(): Boolean

    /**
     * True while the screen in front belongs to an issuance, presentation or signing that is in
     * progress ([ConfigUILogic.protectedFlowScreens]). The background lock waits longer on such
     * a screen. The PIN or biometric screen is on that list as a flow's confirmation; it counts
     * only when the back stack also holds a screen past login, so not while it is the login itself.
     */
    fun isInProtectedFlow(): Boolean

    /**
     * Sends the user to login, clearing the back stack, if the wallet is locked while any screen
     * on the back stack is not one of the [ConfigUILogic.loginScreens]. Called after a lock, and
     * when Android restores the app's screens after ending its process in the background. The
     * whole back stack is checked because the login screens are also used after login, for
     * example the PIN screen that confirms a presentation.
     */
    fun requireLoginIfLocked()

    @Composable
    fun StartFlow(builder: NavGraphBuilder.(NavController) -> Unit)
}

class RouterHostImpl(
    private val configUILogic: ConfigUILogic,
    private val analyticsController: AnalyticsController,
    private val sessionController: SessionController
) : RouterHost {

    private lateinit var navController: NavHostController
    private lateinit var context: Context

    override fun getNavController(): NavHostController = navController
    override fun getNavContext(): Context = context

    /** Binds the router to [navController]. [StartFlow] calls it when it composes; tests call it directly. */
    internal fun attach(navController: NavHostController, context: Context) {
        this.navController = navController
        this.context = context
    }

    @Composable
    override fun StartFlow(builder: NavGraphBuilder.(NavController) -> Unit) {

        attach(navController = rememberNavController(), context = LocalContext.current)

        NavHost(
            navController = navController,
            startDestination = ModuleRoute.StartupModule.route
        ) {
            builder(navController)
        }

        DisposableEffect(navController) {
            val listener = NavController.OnDestinationChangedListener { _, destination, args ->
                destination.route?.let { route ->
                    analyticsController.logScreen(
                        route.firstPart("?"),
                        args.toMapOrEmpty()
                    )
                }
            }
            navController.addOnDestinationChangedListener(listener)
            onDispose {
                navController.removeOnDestinationChangedListener(listener)
            }
        }
    }

    override fun userIsLoggedInWithDocuments(): Boolean =
        sessionController.isUnlocked && isScreenOnBackStackOrForeground(getDashboardScreen())

    override fun userIsLoggedInWithNoDocuments(): Boolean =
        sessionController.isUnlocked && isScreenOnBackStackOrForeground(getIssuanceScreen())

    override fun isNavigationReady(): Boolean = ::navController.isInitialized

    override fun isInProtectedFlow(): Boolean {
        if (!isNavigationReady()) return false
        return isProtectedRoute(
            route = navController.currentDestination?.route,
            protectedFlowScreens = configUILogic.protectedFlowScreens,
        ) && requiresLogin(backStackRoutes(), configUILogic.loginScreens)
    }

    override fun requireLoginIfLocked() {
        if (sessionController.isUnlocked || !isNavigationReady()) return
        if (requiresLogin(backStackRoutes(), configUILogic.loginScreens)) {
            lockToLogin()
        }
    }

    private fun backStackRoutes(): List<String> =
        navController.graph.destinationRoutes()
            .filter { route -> runCatching { navController.getBackStackEntry(route) }.isSuccess }

    private fun lockToLogin() {
        navController.navigate(ModuleRoute.StartupModule.route) {
            popUpTo(navController.graph.id) {
                inclusive = true
            }
        }
    }

    override fun isScreenOnBackStackOrForeground(screen: Screen): Boolean {
        val screenRoute = screen.screenRoute
        try {
            if (navController.currentDestination?.route == screenRoute) {
                return true
            }
            navController.getBackStackEntry(screenRoute)
            return true
        } catch (_: Exception) {
            return false
        }
    }

    override fun popToDashboardScreen() {
        navController.popBackStack(
            route = getDashboardScreen().screenRoute,
            inclusive = false
        )
    }

    override fun popToIssuanceOnboardingScreen() {
        navController.popBackStack(
            route = getIssuanceScreen().screenRoute,
            inclusive = false
        )
    }

    private fun getDashboardScreen(): Screen {
        return configUILogic.dashboardScreenIdentifier
    }

    private fun getIssuanceScreen(): Screen {
        return configUILogic.issuanceScreenIdentifier
    }
}