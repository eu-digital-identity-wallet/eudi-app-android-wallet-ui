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

package eu.europa.ec.startupfeature.interactor

import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.commonfeature.config.BiometricMode
import eu.europa.ec.commonfeature.config.BiometricUiConfig
import eu.europa.ec.commonfeature.config.IssuanceFlowType
import eu.europa.ec.commonfeature.config.IssuanceUiConfig
import eu.europa.ec.commonfeature.config.OnBackNavigationConfig
import eu.europa.ec.commonfeature.interactor.QuickPinInteractor
import eu.europa.ec.commonfeature.model.PinFlow
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.getMockedFullDocuments
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.serializer.UiSerializer
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class TestSplashInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var quickPinInteractor: QuickPinInteractor

    @Mock
    private lateinit var uiSerializer: UiSerializer

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var walletCoreDocumentsController: WalletCoreDocumentsController

    @Mock
    private lateinit var configLogic: ConfigLogic

    private lateinit var interactor: SplashInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = SplashInteractorImpl(
            quickPinInteractor = quickPinInteractor,
            uiSerializer = uiSerializer,
            resourceProvider = resourceProvider,
            walletCoreDocumentsController = walletCoreDocumentsController,
            configLogic = configLogic
        )
    }

    @After
    fun after() {
        closeable.close()
    }

    //region getAfterSplashRoute

    // Case 1:
    // 1. quickPinInteractor.hasPin() returns false (no PIN set yet).
    // 2. configLogic.forcePidActivation is true.
    // 3. walletCoreDocumentsController.getAllDocuments() returns an empty list,
    //    so shouldActivateWithPid evaluates to true.

    // Case 1 Expected Result:
    // The QUICK_PIN route, with pinFlow = CREATE_WITH_ACTIVATION.
    @Test
    fun `Given Case 1, When getAfterSplashRoute is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(quickPinInteractor.hasPin()).thenReturn(false)
            whenever(configLogic.forcePidActivation).thenReturn(true)
            whenever(walletCoreDocumentsController.getAllDocuments()).thenReturn(emptyList())

            // When
            val result = interactor.getAfterSplashRoute()

            // Then
            val expectedResult =
                "${CommonScreens.QuickPin.screenName}?pinFlow=${PinFlow.CREATE_WITH_ACTIVATION}"
            assertEquals(expectedResult, result)
        }
    }

    // Case 2:
    // 1. quickPinInteractor.hasPin() returns false (no PIN set yet).
    // 2. configLogic.forcePidActivation is true.
    // 3. walletCoreDocumentsController.getAllDocuments() returns a non-empty list,
    //    so shouldActivateWithPid evaluates to false.

    // Case 2 Expected Result:
    // The QUICK_PIN route, with pinFlow = CREATE_WITHOUT_ACTIVATION.
    @Test
    fun `Given Case 2, When getAfterSplashRoute is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(quickPinInteractor.hasPin()).thenReturn(false)
            whenever(configLogic.forcePidActivation).thenReturn(true)
            val mockedFullDocuments = getMockedFullDocuments()
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenReturn(mockedFullDocuments)

            // When
            val result = interactor.getAfterSplashRoute()

            // Then
            val expectedResult =
                "${CommonScreens.QuickPin.screenName}?pinFlow=${PinFlow.CREATE_WITHOUT_ACTIVATION}"
            assertEquals(expectedResult, result)
        }
    }

    // Case 3:
    // 1. quickPinInteractor.hasPin() returns false (no PIN set yet).
    // 2. configLogic.forcePidActivation is false,
    //    so shouldActivateWithPid evaluates to false regardless of stored documents.

    // Case 3 Expected Result:
    // The QUICK_PIN route, with pinFlow = CREATE_WITHOUT_ACTIVATION.
    @Test
    fun `Given Case 3, When getAfterSplashRoute is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(quickPinInteractor.hasPin()).thenReturn(false)
            whenever(configLogic.forcePidActivation).thenReturn(false)
            whenever(walletCoreDocumentsController.getAllDocuments()).thenReturn(emptyList())

            // When
            val result = interactor.getAfterSplashRoute()

            // Then
            val expectedResult =
                "${CommonScreens.QuickPin.screenName}?pinFlow=${PinFlow.CREATE_WITHOUT_ACTIVATION}"
            assertEquals(expectedResult, result)
        }
    }

    // Case 4:
    // 1. quickPinInteractor.hasPin() returns true (PIN already set, biometric login flow).
    // 2. configLogic.forcePidActivation is false,
    //    so shouldActivateWithPid evaluates to false and onSuccessNavigation pushes to Dashboard
    //    with empty arguments (no IssuanceUiConfig payload).

    // Case 4 Expected Result:
    // The BIOMETRIC route, with biometricConfig = the serialized BiometricUiConfig payload.
    @Test
    fun `Given Case 4, When getAfterSplashRoute is called, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(quickPinInteractor.hasPin()).thenReturn(true)
            whenever(configLogic.forcePidActivation).thenReturn(false)
            whenever(walletCoreDocumentsController.getAllDocuments()).thenReturn(emptyList())
            mockBiometricLoginStrings()

            val expectedBiometricConfig = buildBiometricUiConfig(shouldActivateWithPid = false)
            mockBiometricConfigSerialization(expectedBiometricConfig)

            // When
            val result = interactor.getAfterSplashRoute()

            // Then
            val expectedResult =
                "${CommonScreens.Biometric.screenName}?biometricConfig=$mockedBiometricConfigBase64"
            assertEquals(expectedResult, result)
        }
    }

    // Case 5:
    // 1. quickPinInteractor.hasPin() returns true (PIN already set, biometric login flow).
    // 2. configLogic.forcePidActivation is true.
    // 3. walletCoreDocumentsController.getAllDocuments() returns a non-empty list,
    //    so shouldActivateWithPid evaluates to false and onSuccessNavigation pushes to Dashboard
    //    with empty arguments (no IssuanceUiConfig payload).

    // Case 5 Expected Result:
    // The BIOMETRIC route, with biometricConfig = the serialized BiometricUiConfig payload.
    @Test
    fun `Given Case 5, When getAfterSplashRoute is called, Then Case 5 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(quickPinInteractor.hasPin()).thenReturn(true)
            whenever(configLogic.forcePidActivation).thenReturn(true)
            val mockedFullDocuments = getMockedFullDocuments()
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenReturn(mockedFullDocuments)
            mockBiometricLoginStrings()

            val expectedBiometricConfig = buildBiometricUiConfig(shouldActivateWithPid = false)
            mockBiometricConfigSerialization(expectedBiometricConfig)

            // When
            val result = interactor.getAfterSplashRoute()

            // Then
            val expectedResult =
                "${CommonScreens.Biometric.screenName}?biometricConfig=$mockedBiometricConfigBase64"
            assertEquals(expectedResult, result)
        }
    }

    // Case 6:
    // 1. quickPinInteractor.hasPin() returns true (PIN already set, biometric login flow).
    // 2. configLogic.forcePidActivation is true.
    // 3. walletCoreDocumentsController.getAllDocuments() returns an empty list,
    //    so shouldActivateWithPid evaluates to true and onSuccessNavigation pushes to
    //    IssuanceScreens.AddDocument with the serialized IssuanceUiConfig(NoDocument) payload.

    // Case 6 Expected Result:
    // The BIOMETRIC route, with biometricConfig = the serialized BiometricUiConfig payload
    // (whose nested onSuccessNavigation carries the issuanceConfig argument).
    @Test
    fun `Given Case 6, When getAfterSplashRoute is called, Then Case 6 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(quickPinInteractor.hasPin()).thenReturn(true)
            whenever(configLogic.forcePidActivation).thenReturn(true)
            whenever(walletCoreDocumentsController.getAllDocuments()).thenReturn(emptyList())
            mockBiometricLoginStrings()

            whenever(
                uiSerializer.toBase64(
                    model = IssuanceUiConfig(flowType = IssuanceFlowType.NoDocument),
                    parser = IssuanceUiConfig.Parser
                )
            ).thenReturn(mockedIssuanceConfigBase64)

            val expectedBiometricConfig = buildBiometricUiConfig(shouldActivateWithPid = true)
            mockBiometricConfigSerialization(expectedBiometricConfig)

            // When
            val result = interactor.getAfterSplashRoute()

            // Then
            val expectedResult =
                "${CommonScreens.Biometric.screenName}?biometricConfig=$mockedBiometricConfigBase64"
            assertEquals(expectedResult, result)
        }
    }

    // Case 7:
    // 1. quickPinInteractor.hasPin() returns true (biometric login flow).
    // 2. configLogic.forcePidActivation is false (shouldActivateWithPid = false).
    // 3. uiSerializer.toBase64 of the BiometricUiConfig returns null,
    //    so the .orEmpty() fallback inside the route arguments map is exercised.

    // Case 7 Expected Result:
    // The BIOMETRIC route, with an empty biometricConfig value.
    @Test
    fun `Given Case 7, When getAfterSplashRoute is called, Then Case 7 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(quickPinInteractor.hasPin()).thenReturn(true)
            whenever(configLogic.forcePidActivation).thenReturn(false)
            whenever(walletCoreDocumentsController.getAllDocuments()).thenReturn(emptyList())
            mockBiometricLoginStrings()

            val expectedBiometricConfig = buildBiometricUiConfig(shouldActivateWithPid = false)
            whenever(
                uiSerializer.toBase64(
                    model = expectedBiometricConfig,
                    parser = BiometricUiConfig.Parser
                )
            ).thenReturn(null)

            // When
            val result = interactor.getAfterSplashRoute()

            // Then
            val expectedResult = "${CommonScreens.Biometric.screenName}?biometricConfig="
            assertEquals(expectedResult, result)
        }
    }

    //endregion

    //region helper functions
    private fun mockBiometricLoginStrings() {
        whenever(resourceProvider.getString(R.string.biometric_login_title))
            .thenReturn(mockedBiometricLoginTitle)
        whenever(resourceProvider.getString(R.string.biometric_login_biometrics_enabled_subtitle))
            .thenReturn(mockedBiometricLoginSubtitleEnabled)
        whenever(resourceProvider.getString(R.string.biometric_login_biometrics_not_enabled_subtitle))
            .thenReturn(mockedBiometricLoginSubtitleNotEnabled)
    }

    private fun mockBiometricConfigSerialization(config: BiometricUiConfig) {
        whenever(
            uiSerializer.toBase64(
                model = config,
                parser = BiometricUiConfig.Parser
            )
        ).thenReturn(mockedBiometricConfigBase64)
    }

    private fun buildBiometricUiConfig(shouldActivateWithPid: Boolean): BiometricUiConfig {
        return BiometricUiConfig(
            mode = BiometricMode.Login(
                title = mockedBiometricLoginTitle,
                subTitleWhenBiometricsEnabled = mockedBiometricLoginSubtitleEnabled,
                subTitleWhenBiometricsNotEnabled = mockedBiometricLoginSubtitleNotEnabled
            ),
            isPreAuthorization = true,
            shouldInitializeBiometricAuthOnCreate = true,
            onSuccessNavigation = ConfigNavigation(
                navigationType = NavigationType.PushScreen(
                    screen = if (!shouldActivateWithPid) {
                        DashboardScreens.Dashboard
                    } else {
                        IssuanceScreens.AddDocument
                    },
                    arguments = if (shouldActivateWithPid) {
                        mapOf(IssuanceUiConfig.serializedKeyName to mockedIssuanceConfigBase64)
                    } else {
                        emptyMap()
                    }
                )
            ),
            onBackNavigationConfig = OnBackNavigationConfig(
                onBackNavigation = ConfigNavigation(navigationType = NavigationType.Finish),
                hasToolbarBackIcon = false
            )
        )
    }
    //endregion

    //region mocked objects
    private val mockedBiometricLoginTitle = "Biometric login title"
    private val mockedBiometricLoginSubtitleEnabled = "Biometric subtitle when biometrics enabled"
    private val mockedBiometricLoginSubtitleNotEnabled =
        "Biometric subtitle when biometrics not enabled"
    private val mockedIssuanceConfigBase64 = "mockedIssuanceConfigBase64"
    private val mockedBiometricConfigBase64 = "mockedBiometricConfigBase64"
    //endregion
}
