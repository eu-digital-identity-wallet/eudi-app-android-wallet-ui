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

package eu.europa.ec.uilogic.config

import eu.europa.ec.uilogic.navigation.Screen

interface ConfigUILogic {
    /*
       Define the dashboard identifier
    */
    val dashboardScreenIdentifier: Screen

    /*
   Define the issuance identifier
   */
    val issuanceScreenIdentifier: Screen

    /**
     * Screens a locked wallet may show: the startup screen and the steps to log in or create a
     * PIN. A locked wallet restored on any other screen is sent back to login.
     */
    val loginScreens: List<Screen>

    /**
     * Screens on which an issuance, presentation, signing or factory reset is in progress. On them
     * the background lock waits for `AuthenticationConfig.protectedFlowLockTimeout` instead of
     * `backgroundLockTimeout`, because issuance and signing send the user to a website and back,
     * and a reset must not be interrupted. The idle lock applies as on any other screen. The
     * proximity QR screen is not one: leaving the app there stops the session and closes it.
     */
    val protectedFlowScreens: List<Screen>

    /**
     * Claims that hold a picture of the holder, such as the portrait. A key matches a claim of that
     * name in any document and namespace. Matching claims are shown as images on the document
     * details, request and success screens, and a presentation request that includes one shows a
     * biometric-data notice and asks for confirmation before the picture is shared.
     */
    val userImageClaimKeys: Set<String>

    /**
     * Whether the wallet's screens are kept out of screenshots, screen recordings and the
     * recent-apps preview (`FLAG_SECURE` on the host activity's window, which its dialogs and
     * bottom sheets inherit). Off in the reference configuration, so that its screens can be
     * captured in development and testing; a production build should turn it on. Screens of
     * another activity, such as the RQES SDK's, are not covered.
     */
    val shouldPreventScreenCapture: Boolean
}