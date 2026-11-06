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

package eu.europa.ec.uilogic.navigation.helper

import eu.europa.ec.uilogic.config.ConfigUILogicImpl
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.PresentationScreens
import eu.europa.ec.uilogic.navigation.ProximityScreens
import eu.europa.ec.uilogic.navigation.StartupScreens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestNavigationRouteHelper {

    private val config = ConfigUILogicImpl()

    @Test
    fun `Given only login screens on the back stack, When requiresLogin is checked, Then no login is required`() {
        val backStack = listOf(
            StartupScreens.Splash.screenRoute,
            CommonScreens.Biometric.screenRoute,
        )

        assertFalse(requiresLogin(backStack, config.loginScreens))
    }

    @Test
    fun `Given a login screen opened on top of the dashboard, When requiresLogin is checked, Then login is required`() {
        val backStack = listOf(
            DashboardScreens.Dashboard.screenRoute,
            CommonScreens.TrustMark.screenRoute,
        )

        assertTrue(requiresLogin(backStack, config.loginScreens))
    }

    @Test
    fun `Given the PIN confirmation of a presentation in front, When requiresLogin is checked, Then login is required`() {
        val backStack = listOf(
            DashboardScreens.Dashboard.screenRoute,
            PresentationScreens.PresentationRequest.screenRoute,
            CommonScreens.Biometric.screenRoute,
        )

        assertTrue(requiresLogin(backStack, config.loginScreens))
    }

    @Test
    fun `Given an empty back stack, When requiresLogin is checked, Then no login is required`() {
        assertFalse(requiresLogin(emptyList(), config.loginScreens))
    }

    @Test
    fun `Given the reference configuration, When its protected-flow screens are read, Then they are the issuance, presentation, proximity and signing screens`() {
        assertEquals(
            listOf(
                IssuanceScreens.AddDocument,
                IssuanceScreens.DocumentOffer,
                IssuanceScreens.DocumentOfferCode,
                PresentationScreens.PresentationRequest,
                PresentationScreens.PresentationLoading,
                ProximityScreens.Request,
                ProximityScreens.Loading,
                DashboardScreens.DocumentSign,
                DashboardScreens.ResetWallet,
                CommonScreens.Biometric,
            ),
            config.protectedFlowScreens,
        )
        config.protectedFlowScreens.forEach { screen ->
            assertTrue(
                screen.screenRoute,
                isProtectedRoute(screen.screenRoute, config.protectedFlowScreens)
            )
        }
    }

    @Test
    fun `Given the dashboard or document details, When isProtectedRoute is checked, Then neither is protected`() {
        assertFalse(
            isProtectedRoute(
                DashboardScreens.Dashboard.screenRoute,
                config.protectedFlowScreens
            )
        )
        assertFalse(
            isProtectedRoute(
                DashboardScreens.DocumentDetails.screenRoute,
                config.protectedFlowScreens
            )
        )
    }

    @Test
    fun `Given no destination, When isProtectedRoute is checked, Then it is not protected`() {
        assertFalse(isProtectedRoute(null, config.protectedFlowScreens))
    }
}