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

package eu.europa.ec.businesslogic.controller.security

import android.os.Build
import eu.europa.ec.businesslogic.config.AppBuildType
import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.businesslogic.config.ConfigSecurityLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

interface SecurityController {
    fun isRunningOnEmulator(): Boolean
    fun isDeviceRooted(): Boolean
    fun isSignatureValid(): Boolean
    fun isPackageInstallerValid(): Boolean
    fun isDebugModeEnabled(): Boolean
    fun areRootCloakingAppsInstalled(): Boolean
    fun isHookDetected(): Boolean
    fun blockScreenCapture(): Boolean
    fun isApplicationSecure(): Flow<SecurityValidation>
}

class SecurityControllerImpl(
    private val configLogic: ConfigLogic,
    private val configSecurityLogic: ConfigSecurityLogic,
    private val rootController: RootController,
    private val antiHookController: AntiHookController,
    private val androidPackageController: AndroidPackageController
) : SecurityController {

    override fun isRunningOnEmulator() =
        configLogic.appBuildType == AppBuildType.RELEASE
                && configSecurityLogic.blockEmulator
                && ((Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("sdk_gphone64_arm64")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
                || Build.PRODUCT.contains("robolectric"))

    override fun isDeviceRooted(): Boolean =
        configLogic.appBuildType == AppBuildType.RELEASE
                && configSecurityLogic.blockRootAccess
                && rootController.isRooted()

    override fun isSignatureValid(): Boolean {
        if (configLogic.appBuildType != AppBuildType.RELEASE || configSecurityLogic.packageSignature == null) {
            return true
        }
        for (signature in androidPackageController.getSignatures()) {
            if (signature == configSecurityLogic.packageSignature) {
                return true
            }
        }
        return false
    }

    override fun isPackageInstallerValid(): Boolean {
        if (configLogic.appBuildType != AppBuildType.RELEASE || configSecurityLogic.packageInstallers.isEmpty()) {
            return true
        }
        return when (androidPackageController.getInstaller(configSecurityLogic.packageInstallers)) {
            AndroidInstaller.TRUSTED -> true
            AndroidInstaller.UNKNOWN -> false
        }
    }

    override fun isDebugModeEnabled(): Boolean {
        return configLogic.appBuildType == AppBuildType.RELEASE
                && androidPackageController.isDebugModeEnabled()
                && configSecurityLogic.blockDebugMode
    }

    override fun areRootCloakingAppsInstalled(): Boolean =
        configLogic.appBuildType == AppBuildType.RELEASE
                && configSecurityLogic.blockRootCloakingAppsAccess
                && rootController.areRootCloakingAppsInstalled()

    override fun isHookDetected(): Boolean =
        configLogic.appBuildType == AppBuildType.RELEASE
                && configSecurityLogic.blockHooks
                && (antiHookController.isMemoryHooked() || antiHookController.isStacktraceHooked())

    override fun isApplicationSecure(): Flow<SecurityValidation> = flow {
        val isRunningOnEmulator = isRunningOnEmulator()
        val isDeviceRooted = isDeviceRooted()
        val isSignatureValid = isSignatureValid()
        val isPackageInstallerValid = isPackageInstallerValid()
        val isDebugModeEnabled = isDebugModeEnabled()
        val isHookDetected = isHookDetected()
        val areRootCloakingAppsInstalled = areRootCloakingAppsInstalled()
        emit(
            SecurityValidation(
                isRunningOnEmulator = isRunningOnEmulator,
                isDeviceRooted = isDeviceRooted,
                isSignatureValid = isSignatureValid,
                isPackageInstallerValid = isPackageInstallerValid,
                isDebugModeEnabled = isDebugModeEnabled,
                areRootCloakingAppsInstalled = areRootCloakingAppsInstalled,
                isHookDetected = isHookDetected,
                serviceErrorCode = when {
                    isRunningOnEmulator -> SecurityErrorCode.EMULATOR
                    isDeviceRooted -> SecurityErrorCode.ROOT
                    !isSignatureValid -> SecurityErrorCode.SIGNATURE
                    !isPackageInstallerValid -> SecurityErrorCode.PACKAGE_INSTALLER
                    isDebugModeEnabled -> SecurityErrorCode.DEBUGGABLE
                    areRootCloakingAppsInstalled -> SecurityErrorCode.ROOT_CLOAKING_APPS
                    isHookDetected -> SecurityErrorCode.HOOK
                    else -> SecurityErrorCode.UNKNOWN
                }
            )
        )
    }.flowOn(Dispatchers.IO)

    override fun blockScreenCapture(): Boolean {
        return configSecurityLogic.blockScreenCapture && configLogic.appBuildType == AppBuildType.RELEASE
    }
}

data class SecurityValidation(
    val isRunningOnEmulator: Boolean,
    val isDeviceRooted: Boolean,
    val isSignatureValid: Boolean,
    val isPackageInstallerValid: Boolean,
    val isDebugModeEnabled: Boolean,
    val areRootCloakingAppsInstalled: Boolean,
    val isHookDetected: Boolean,
    val serviceErrorCode: SecurityErrorCode
)

enum class SecurityErrorCode(val code: String) {
    EMULATOR("1001"),
    ROOT("1002"),
    SIGNATURE("1003"),
    PACKAGE_INSTALLER("1004"),
    DEBUGGABLE("1005"),
    ROOT_CLOAKING_APPS("1006"),
    HOOK("1007"),
    UNKNOWN("1000")
}