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

package eu.europa.ec.businesslogic.config

interface ConfigSecurityLogic {
    /**
     * Check root access on startup for release builds.
     */
    val blockRootAccess: Boolean get() = false

    /**
     * Checks if any root cloaking apps are installed on startup for release builds.
     */
    val blockRootCloakingAppsAccess: Boolean get() = false

    /**
     * Checks for any hooks on startup for release builds.
     */
    val blockHooks: Boolean get() = false

    /**
     * Block running on emulator on startup for release builds.
     */
    val blockEmulator: Boolean get() = false

    /**
     * Block debug mode on startup for release builds.
     */
    val blockDebugMode: Boolean get() = false

    /**
     * Block screen capture for release builds.
     */
    val blockScreenCapture: Boolean get() = false

    /**
     * Validate package signature on startup for release builds.
     */
    val packageSignature: String? get() = null

    /**
     * Set trusted installers for release builds.
     */
    val packageInstallers: List<String> get() = emptyList()

    /**
     * Activate Strict mode for Debug Builds
     */
    val enableStrictMode: Boolean get() = false

    /**
     * Use network logger for debug builds.
     */
    val useNetworkLogger: Boolean get() = false
}