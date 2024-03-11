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

package eu.europa.ec.businesslogic.controller

import eu.europa.ec.businesslogic.config.AppBuildType
import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.businesslogic.config.ConfigSecurityLogic
import eu.europa.ec.businesslogic.controller.security.AndroidInstaller
import eu.europa.ec.businesslogic.controller.security.AndroidPackageController
import eu.europa.ec.businesslogic.controller.security.AntiHookController
import eu.europa.ec.businesslogic.controller.security.RootController
import eu.europa.ec.businesslogic.controller.security.SecurityControllerImpl
import eu.europa.ec.testlogic.base.TestApplication
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.Spy
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class TestSecurityController {

    @Spy
    private lateinit var rootController: RootController

    @Spy
    private lateinit var configLogic: ConfigLogic

    @Spy
    private lateinit var configSecurityLogic: ConfigSecurityLogic

    @Spy
    private lateinit var androidPackageController: AndroidPackageController

    @Spy
    private lateinit var antiHookController: AntiHookController

    private lateinit var securityController: SecurityControllerImpl

    private val signature = "SIGNATURE"
    private val installers = listOf("com.test")

    @Before
    fun before() {
        MockitoAnnotations.openMocks(this)
        securityController = SecurityControllerImpl(
            configLogic,
            configSecurityLogic,
            rootController,
            antiHookController,
            androidPackageController
        )
        MockitoAnnotations.openMocks(this)

        whenever(configLogic.appBuildType).thenReturn(AppBuildType.RELEASE)
        whenever(androidPackageController.getSignatures()).thenReturn(listOf(signature))
    }

    @Test
    fun testIsRunningOnEmulatorWithBlockEmulatorConfigTrue() {
        whenever(configSecurityLogic.blockEmulator).thenReturn(true)
        assertTrue(securityController.isRunningOnEmulator())
    }

    @Test
    fun testIsRunningOnEmulatorWithBlockEmulatorConfigFalse() {
        whenever(configSecurityLogic.blockEmulator).thenReturn(false)
        assertFalse(securityController.isRunningOnEmulator())
    }

    @Test
    fun testIsDeviceRootedWithBlockRootAccessConfigTrue() {
        whenever(configSecurityLogic.blockRootAccess).thenReturn(true)
        whenever(rootController.isRooted()).thenReturn(true)
        assertTrue(securityController.isDeviceRooted())
    }

    @Test
    fun testIsDeviceRootedWithBlockRootAccessConfigFalse() {
        whenever(configSecurityLogic.blockRootAccess).thenReturn(false)
        whenever(rootController.isRooted()).thenReturn(true)
        assertFalse(securityController.isDeviceRooted())
    }

    @Test
    fun testIsSignatureValidWithSignaturePackageNotNullAndValid() {
        whenever(configSecurityLogic.packageSignature).thenReturn(signature)
        assertTrue(securityController.isSignatureValid())
    }

    @Test
    fun testIsSignatureValidWithSignaturePackageNotNullAndNotValid() {
        whenever(configSecurityLogic.packageSignature).thenReturn("")
        assertFalse(securityController.isSignatureValid())
    }

    @Test
    fun testIsSignatureValidWithSignaturePackageConfigIsNull() {
        whenever(configSecurityLogic.packageSignature).thenReturn(null)
        assertTrue(securityController.isSignatureValid())
    }

    @Test
    fun testIsPackageInstallerValidWithConfigPackageInstallersNotEmptyAndValidInstaller() {
        whenever(configSecurityLogic.packageInstallers).thenReturn(installers)
        whenever(androidPackageController.getInstaller(installers))
            .thenReturn(AndroidInstaller.TRUSTED)
        assertTrue(securityController.isPackageInstallerValid())
    }

    @Test
    fun testIsPackageInstallerValidWithConfigPackageInstallersNotEmptyAndInValidInstaller() {
        whenever(configSecurityLogic.packageInstallers).thenReturn(installers)
        whenever(androidPackageController.getInstaller(installers))
            .thenReturn(AndroidInstaller.UNKNOWN)
        assertFalse(securityController.isPackageInstallerValid())
    }

    @Test
    fun testIsPackageInstallerValidWithConfigPackageInstallersEmpty() {
        whenever(configSecurityLogic.packageInstallers).thenReturn(emptyList())
        assertTrue(securityController.isPackageInstallerValid())
    }

    @Test
    fun testIsDebugModeEnabledWithConfigBlockDebugModeEnabledAndFlagDebuggable() {
        whenever(configSecurityLogic.blockDebugMode).thenReturn(true)
        whenever(androidPackageController.isDebugModeEnabled()).thenReturn(true)
        assertTrue(securityController.isDebugModeEnabled())
    }

    @Test
    fun testIsDebugModeEnabledWithConfigBlockDebugModeEnabledAndFlagNotDebuggable() {
        whenever(configSecurityLogic.blockDebugMode).thenReturn(true)
        whenever(androidPackageController.isDebugModeEnabled()).thenReturn(false)
        assertFalse(securityController.isDebugModeEnabled())
    }

    @Test
    fun testBlockScreenCaptureWithConfigBlockScreenCaptureEnabled() {
        whenever(configSecurityLogic.blockScreenCapture).thenReturn(true)
        assertTrue(securityController.blockScreenCapture())
    }

    @Test
    fun testBlockScreenCaptureWithConfigBlockScreenCaptureDisabled() {
        whenever(configSecurityLogic.blockScreenCapture).thenReturn(false)
        assertFalse(securityController.blockScreenCapture())
    }

    @Test
    fun testIsHookDetectedWithConfigBlockHooksEnabledAndReleaseBuildAndIsStackTracedHookDetectedAndIsMemoryHookedDetected() {
        whenever(configSecurityLogic.blockHooks).thenReturn(true)
        whenever(antiHookController.isMemoryHooked()).thenReturn(true)
        whenever(antiHookController.isStacktraceHooked()).thenReturn(true)
        assertTrue(securityController.isHookDetected())
    }

    @Test
    fun testIsHookDetectedWithConfigBlockHooksEnabledAndReleaseBuildAndIsStackTracedHookDetectedAndIsMemoryHookedNotDetected() {
        whenever(configSecurityLogic.blockHooks).thenReturn(true)
        whenever(antiHookController.isMemoryHooked()).thenReturn(false)
        whenever(antiHookController.isStacktraceHooked()).thenReturn(true)
        assertTrue(securityController.isHookDetected())
    }

    @Test
    fun testIsHookDetectedWithConfigBlockHooksEnabledAndReleaseBuildAndIsStackTracedHookNotDetectedAndIsMemoryHookedNotDetected() {
        whenever(configSecurityLogic.blockHooks).thenReturn(true)
        whenever(antiHookController.isMemoryHooked()).thenReturn(false)
        whenever(antiHookController.isStacktraceHooked()).thenReturn(false)
        assertFalse(securityController.isHookDetected())
    }

    @Test
    fun testIsHookDetectedWithConfigBlockHooksDisabledAndReleaseBuildAndIsStackTracedHookDetectedAndIsMemoryHookedDetected() {
        whenever(configSecurityLogic.blockHooks).thenReturn(false)
        whenever(antiHookController.isMemoryHooked()).thenReturn(true)
        whenever(antiHookController.isStacktraceHooked()).thenReturn(true)
        assertFalse(securityController.isHookDetected())
    }
}