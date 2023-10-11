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