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

package eu.europa.ec.dashboardfeature.interactor

import android.net.Uri
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAvailability
import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.commonfeature.interactor.BiometricInteractor
import eu.europa.ec.corelogic.controller.WalletCoreDCAPIRegistrationController
import eu.europa.ec.corelogic.provider.DCAPISettingProvider
import eu.europa.ec.dashboardfeature.ui.settings.model.SettingsItemUi
import eu.europa.ec.dashboardfeature.ui.settings.model.SettingsMenuItemType
import eu.europa.ec.dashboardfeature.util.mockedChangeLogUrl
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.StringResourceProviderMocker.mockResourceProviderStrings
import eu.europa.ec.testfeature.util.mockedUriPath1
import eu.europa.ec.testfeature.util.mockedUriPath2
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemLeadingContentDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TestSettingsInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var biometricInteractor: BiometricInteractor

    @Mock
    private lateinit var configLogic: ConfigLogic

    @Mock
    private lateinit var logController: LogController

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var prefKeys: PrefKeys

    @Mock
    private lateinit var dcapiSettingProvider: DCAPISettingProvider

    @Mock
    private lateinit var walletCoreDCAPIRegistrationController: WalletCoreDCAPIRegistrationController

    private lateinit var interactor: SettingsInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        whenever(logController.shouldWriteLogFile).thenReturn(true)

        interactor = SettingsInteractorImpl(
            biometricInteractor = biometricInteractor,
            configLogic = configLogic,
            logController = logController,
            resourceProvider = resourceProvider,
            prefKeys = prefKeys,
            dcapiSettingProvider = dcapiSettingProvider,
            walletCoreDCAPIRegistrationController = walletCoreDCAPIRegistrationController,
        )
    }

    @After
    fun after() {
        closeable.close()
    }

    @Test
    fun `Given an App Version, When getAppVersion is called, Then it returns the Apps Version`() {
        val expectedAppVersion = "2024.01.1"
        whenever(configLogic.appVersion)
            .thenReturn(expectedAppVersion)

        val actualAppVersion = interactor.getAppVersion()

        assertEquals(expectedAppVersion, actualAppVersion)
        verify(configLogic, times(1))
            .appVersion
    }

    @Test
    fun `Given a Changelog URL in configLogic, When getChangelogUrl is called, Then it returns the Changelog URL`() {
        val expectedUrl = mockedChangeLogUrl
        whenever(configLogic.changelogUrl)
            .thenReturn(expectedUrl)

        val actualUrl = interactor.getChangelogUrl()

        assertEquals(expectedUrl, actualUrl)
        verify(configLogic, times(1))
            .changelogUrl
    }

    @Test
    fun `Given no Changelog URL in configLogic, When getChangelogUrl is called, Then it returns null`() {
        val expectedUrl = null
        whenever(configLogic.changelogUrl)
            .thenReturn(expectedUrl)

        val actualUrl = interactor.getChangelogUrl()

        assertEquals(expectedUrl, actualUrl)
        verify(configLogic, times(1))
            .changelogUrl
    }

    @Test
    fun `Given a list of logs via logController, When retrieveLogFileUris is called, Then expected logs are returned`() {
        val mockedArrayList = arrayListOf(
            Uri.parse(mockedUriPath1),
            Uri.parse(mockedUriPath2)
        )
        whenever(logController.retrieveLogFileUris()).thenReturn(mockedArrayList)

        val expectedLogFileUris = interactor.retrieveLogFileUris()

        assertEquals(mockedArrayList, expectedLogFileUris)
    }

    /**
     * Assertions:
     * 1. Size = 4 (SHOW_BATCH_ISSUANCE_COUNTER + REGISTRATION_CHECK + RETRIEVE_LOGS + RESET_WALLET)
     * 2. First item: SHOW_BATCH_ISSUANCE_COUNTER
     * 3. Second item: REGISTRATION_CHECK
     * 4. Third item: RETRIEVE_LOGS
     */
    @Test
    fun `Given no Changelog URL and show batch issuance counter preference is true, When getSettingsItemsUi is called, Then four SettingsItemUi entries are returned`() =
        coroutineRule.runTest {
            whenever(biometricInteractor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.Failure(biometricsFailureText))
            mockShowBatchIssuanceCounterPreference(response = true)
            mockRegistrationCheckPreference(response = mockedRegistrationCheckEnabled)

            mockStringsNeededForGetSettingsItemsUi(
                resourcesProvider = resourceProvider,
                changeLogUrlIsNull = true,
            )

            val settingsItems = interactor.getSettingsItemsUi(changelogUrl = null)

            assertEquals(4, settingsItems.size)

            val firstItem = settingsItems[0]
            assertShowBatchIssuanceCounterItem(firstItem, isChecked = true)

            assertRegistrationCheckItem(
                settingsItems[1],
                isChecked = mockedRegistrationCheckEnabled,
            )

            val thirdItem = settingsItems[2]
            assertEquals(SettingsMenuItemType.RETRIEVE_LOGS, thirdItem.type)
            assertEquals(SettingsMenuItemType.RETRIEVE_LOGS.itemId, thirdItem.data.itemId)
            val mainContent3 =
                thirdItem.data.mainContentData as ListItemMainContentDataUi.Text
            assertEquals(retrieveLogsText, mainContent3.text)
            val leadingIcon3 =
                thirdItem.data.leadingContentData as ListItemLeadingContentDataUi.Icon
            assertEquals(AppIcons.OpenNew, leadingIcon3.iconData)
            val trailingIcon3 =
                thirdItem.data.trailingContentData as ListItemTrailingContentDataUi.Icon
            assertEquals(AppIcons.KeyboardArrowRight, trailingIcon3.iconData)

            assertResetWalletItem(settingsItems[3])

            verify(biometricInteractor, never()).getBiometricUserSelection()
        }

    @Test
    fun `Given a build that writes no log file, When getSettingsItemsUi is called, Then Retrieve logs is not offered`() =
        coroutineRule.runTest {
            whenever(logController.shouldWriteLogFile).thenReturn(false)
            whenever(biometricInteractor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.Failure(biometricsFailureText))
            mockShowBatchIssuanceCounterPreference(response = true)
            mockRegistrationCheckPreference(response = mockedRegistrationCheckEnabled)
            mockStringsNeededForGetSettingsItemsUi(
                resourcesProvider = resourceProvider,
                changeLogUrlIsNull = true,
            )

            val settingsItems = interactor.getSettingsItemsUi(changelogUrl = null)

            assertEquals(3, settingsItems.size)
            assertTrue(settingsItems.none { it.type == SettingsMenuItemType.RETRIEVE_LOGS })
        }

    /**
     * Assertions:
     * 1. Size = 5 (SHOW_BATCH_ISSUANCE_COUNTER + REGISTRATION_CHECK + RETRIEVE_LOGS +
     * CHANGELOG + RESET_WALLET)
     * 2. First item: SHOW_BATCH_ISSUANCE_COUNTER
     * 3. Second item: REGISTRATION_CHECK
     * 4. Third item: RETRIEVE_LOGS
     * 5. Fourth item: CHANGELOG
     */
    @Test
    fun `Given a Changelog URL and show batch issuance counter preference is true, When getSettingsItemsUi is called, Then five SettingsItemUi entries are returned`() =
        coroutineRule.runTest {
            whenever(biometricInteractor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.Failure(biometricsFailureText))
            mockShowBatchIssuanceCounterPreference(response = true)
            mockRegistrationCheckPreference(response = mockedRegistrationCheckEnabled)

            mockStringsNeededForGetSettingsItemsUi(
                resourcesProvider = resourceProvider,
                changeLogUrlIsNull = false,
            )

            val sampleChangelogUrl = mockedChangeLogUrl

            val settingsItems = interactor.getSettingsItemsUi(changelogUrl = sampleChangelogUrl)

            assertEquals(5, settingsItems.size)

            val firstItem = settingsItems[0]
            assertShowBatchIssuanceCounterItem(firstItem, isChecked = true)

            assertRegistrationCheckItem(
                settingsItems[1],
                isChecked = mockedRegistrationCheckEnabled,
            )

            val thirdItem = settingsItems[2]
            assertEquals(SettingsMenuItemType.RETRIEVE_LOGS, thirdItem.type)
            assertEquals(SettingsMenuItemType.RETRIEVE_LOGS.itemId, thirdItem.data.itemId)
            val mainContent3 =
                thirdItem.data.mainContentData as ListItemMainContentDataUi.Text
            assertEquals(retrieveLogsText, mainContent3.text)
            val leadingIcon3 =
                thirdItem.data.leadingContentData as ListItemLeadingContentDataUi.Icon
            assertEquals(AppIcons.OpenNew, leadingIcon3.iconData)
            val trailingIcon3 =
                thirdItem.data.trailingContentData as ListItemTrailingContentDataUi.Icon
            assertEquals(AppIcons.KeyboardArrowRight, trailingIcon3.iconData)

            val fourthItem = settingsItems[3]
            assertEquals(SettingsMenuItemType.CHANGELOG, fourthItem.type)
            assertEquals(SettingsMenuItemType.CHANGELOG.itemId, fourthItem.data.itemId)
            val mainContent4 =
                fourthItem.data.mainContentData as ListItemMainContentDataUi.Text
            assertEquals(changelogText, mainContent4.text)
            val leadingIcon4 =
                fourthItem.data.leadingContentData as ListItemLeadingContentDataUi.Icon
            assertEquals(AppIcons.OpenInBrowser, leadingIcon4.iconData)
            val trailingIcon4 =
                fourthItem.data.trailingContentData as ListItemTrailingContentDataUi.Icon
            assertEquals(AppIcons.KeyboardArrowRight, trailingIcon4.iconData)

            assertResetWalletItem(settingsItems[4])

            verify(biometricInteractor, never()).getBiometricUserSelection()
        }

    @Test
    fun `Given device supports Biometrics with no Changelog URL and show batch issuance counter preference is true, When getSettingsItemsUi is called, Then Biometrics and Retrieve Logs entries are returned`() =
        coroutineRule.runTest {
            whenever(biometricInteractor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.CanAuthenticate)
            whenever(biometricInteractor.getBiometricUserSelection())
                .thenReturn(true)
            mockShowBatchIssuanceCounterPreference(response = true)
            mockRegistrationCheckPreference(response = mockedRegistrationCheckEnabled)

            mockStringsNeededForGetSettingsItemsUi(
                resourcesProvider = resourceProvider,
                changeLogUrlIsNull = true,
                deviceSupportsBiometrics = true,
            )

            val settingsItems = interactor.getSettingsItemsUi(changelogUrl = null)

            assertEquals(5, settingsItems.size)

            val firstItem = settingsItems[0]
            assertEquals(SettingsMenuItemType.BIOMETRICS_AUTHENTICATION, firstItem.type)
            assertEquals(
                SettingsMenuItemType.BIOMETRICS_AUTHENTICATION.itemId,
                firstItem.data.itemId
            )
            val mainContent1 =
                firstItem.data.mainContentData as ListItemMainContentDataUi.Text
            assertEquals(biometricsAuthenticationText, mainContent1.text)
            val leadingIcon1 =
                firstItem.data.leadingContentData as ListItemLeadingContentDataUi.Icon
            assertEquals(AppIcons.TouchId, leadingIcon1.iconData)
            val trailingSwitch1 =
                firstItem.data.trailingContentData as ListItemTrailingContentDataUi.Switch
            assertEquals(true, trailingSwitch1.switchData.isChecked)
            assertEquals(true, trailingSwitch1.switchData.enabled)

            val secondItem = settingsItems[1]
            assertShowBatchIssuanceCounterItem(secondItem, isChecked = true)

            assertRegistrationCheckItem(
                settingsItems[2],
                isChecked = mockedRegistrationCheckEnabled,
            )

            val fourthItem = settingsItems[3]
            assertEquals(SettingsMenuItemType.RETRIEVE_LOGS, fourthItem.type)
            assertEquals(SettingsMenuItemType.RETRIEVE_LOGS.itemId, fourthItem.data.itemId)
            val mainContent4 =
                fourthItem.data.mainContentData as ListItemMainContentDataUi.Text
            assertEquals(retrieveLogsText, mainContent4.text)
            val leadingIcon4 =
                fourthItem.data.leadingContentData as ListItemLeadingContentDataUi.Icon
            assertEquals(AppIcons.OpenNew, leadingIcon4.iconData)
            val trailingIcon4 =
                fourthItem.data.trailingContentData as ListItemTrailingContentDataUi.Icon
            assertEquals(AppIcons.KeyboardArrowRight, trailingIcon4.iconData)

            assertResetWalletItem(settingsItems[4])
        }

    @Test
    fun `Given device has Biometrics with user selection false and show batch issuance counter preference is true, When getSettingsItemsUi is called, Then Biometrics switch is unchecked`() =
        coroutineRule.runTest {
            whenever(biometricInteractor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.NonEnrolled)
            whenever(biometricInteractor.getBiometricUserSelection())
                .thenReturn(false)
            mockShowBatchIssuanceCounterPreference(response = true)
            mockRegistrationCheckPreference(response = mockedRegistrationCheckEnabled)

            mockStringsNeededForGetSettingsItemsUi(
                resourcesProvider = resourceProvider,
                changeLogUrlIsNull = true,
                deviceSupportsBiometrics = true,
            )

            val settingsItems = interactor.getSettingsItemsUi(changelogUrl = null)

            assertEquals(5, settingsItems.size)

            val biometricsItem = settingsItems[0]
            assertEquals(SettingsMenuItemType.BIOMETRICS_AUTHENTICATION, biometricsItem.type)
            val trailingSwitch =
                biometricsItem.data.trailingContentData as ListItemTrailingContentDataUi.Switch
            assertEquals(false, trailingSwitch.switchData.isChecked)
            assertEquals(true, trailingSwitch.switchData.enabled)
        }

    @Test
    fun `Given device supports Biometrics with a Changelog URL and show batch issuance counter preference is true, When getSettingsItemsUi is called, Then six SettingsItemUi entries are returned`() =
        coroutineRule.runTest {
            whenever(biometricInteractor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.CanAuthenticate)
            whenever(biometricInteractor.getBiometricUserSelection())
                .thenReturn(true)
            mockShowBatchIssuanceCounterPreference(response = true)
            mockRegistrationCheckPreference(response = mockedRegistrationCheckEnabled)

            mockStringsNeededForGetSettingsItemsUi(
                resourcesProvider = resourceProvider,
                changeLogUrlIsNull = false,
                deviceSupportsBiometrics = true,
            )

            val sampleChangelogUrl = mockedChangeLogUrl

            val settingsItems = interactor.getSettingsItemsUi(changelogUrl = sampleChangelogUrl)

            assertEquals(6, settingsItems.size)
            assertEquals(SettingsMenuItemType.BIOMETRICS_AUTHENTICATION, settingsItems[0].type)
            assertEquals(SettingsMenuItemType.SHOW_BATCH_ISSUANCE_COUNTER, settingsItems[1].type)
            assertEquals(SettingsMenuItemType.REGISTRATION_CHECK, settingsItems[2].type)
            assertEquals(SettingsMenuItemType.RETRIEVE_LOGS, settingsItems[3].type)
            assertEquals(SettingsMenuItemType.CHANGELOG, settingsItems[4].type)
            assertEquals(SettingsMenuItemType.RESET_WALLET, settingsItems[5].type)
        }

    @Test
    fun `Given show batch issuance counter preference is false, When getSettingsItemsUi is called, Then switch is unchecked`() =
        coroutineRule.runTest {
            whenever(biometricInteractor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.Failure(biometricsFailureText))
            mockShowBatchIssuanceCounterPreference(response = false)
            mockRegistrationCheckPreference(response = mockedRegistrationCheckEnabled)

            mockStringsNeededForGetSettingsItemsUi(
                resourcesProvider = resourceProvider,
                changeLogUrlIsNull = true,
            )

            val settingsItems = interactor.getSettingsItemsUi(changelogUrl = null)

            val showBatchItem = settingsItems[0]
            assertShowBatchIssuanceCounterItem(showBatchItem, isChecked = false)
        }

    @Test
    fun `Given registration check preference is false, When getSettingsItemsUi is called, Then the switch is unchecked`() =
        coroutineRule.runTest {
            whenever(biometricInteractor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.Failure(biometricsFailureText))
            mockShowBatchIssuanceCounterPreference(response = true)
            mockRegistrationCheckPreference(response = false)

            mockStringsNeededForGetSettingsItemsUi(
                resourcesProvider = resourceProvider,
                changeLogUrlIsNull = true,
            )

            val settingsItems = interactor.getSettingsItemsUi(changelogUrl = null)

            assertRegistrationCheckItem(settingsItems[1], isChecked = false)
        }

    @Test
    fun `Given the build enables the Digital Credential API, When getSettingsItemsUi is called, Then its switch follows the setting`() =
        coroutineRule.runTest {
            whenever(biometricInteractor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.Failure(biometricsFailureText))
            mockShowBatchIssuanceCounterPreference(response = true)
            mockRegistrationCheckPreference(response = mockedRegistrationCheckEnabled)
            whenever(dcapiSettingProvider.isAvailable).thenReturn(true)
            whenever(dcapiSettingProvider.isEnabled()).thenReturn(false)

            mockStringsNeededForGetSettingsItemsUi(
                resourcesProvider = resourceProvider,
                changeLogUrlIsNull = true,
            )
            mockResourceProviderStrings(
                resourceProvider,
                listOf(R.string.settings_screen_option_dc_api to dcApiText)
            )

            val settingsItems = interactor.getSettingsItemsUi(changelogUrl = null)

            assertEquals(5, settingsItems.size)
            val dcApiItem = settingsItems[2]
            assertEquals(SettingsMenuItemType.DC_API, dcApiItem.type)
            assertEquals(SettingsMenuItemType.DC_API.itemId, dcApiItem.data.itemId)
            val mainContent = dcApiItem.data.mainContentData as ListItemMainContentDataUi.Text
            assertEquals(dcApiText, mainContent.text)
            val leadingIcon = dcApiItem.data.leadingContentData as ListItemLeadingContentDataUi.Icon
            assertEquals(AppIcons.Id, leadingIcon.iconData)
            val trailingSwitch =
                dcApiItem.data.trailingContentData as ListItemTrailingContentDataUi.Switch
            assertEquals(false, trailingSwitch.switchData.isChecked)
            assertEquals(true, trailingSwitch.switchData.enabled)
            assertEquals(SettingsMenuItemType.RETRIEVE_LOGS, settingsItems[3].type)
        }

    @Test
    fun `Given the build does not enable the Digital Credential API, When getSettingsItemsUi is called, Then no switch is offered`() =
        coroutineRule.runTest {
            whenever(biometricInteractor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.Failure(biometricsFailureText))
            mockShowBatchIssuanceCounterPreference(response = true)
            mockRegistrationCheckPreference(response = mockedRegistrationCheckEnabled)
            whenever(dcapiSettingProvider.isAvailable).thenReturn(false)

            mockStringsNeededForGetSettingsItemsUi(
                resourcesProvider = resourceProvider,
                changeLogUrlIsNull = true,
            )

            val settingsItems = interactor.getSettingsItemsUi(changelogUrl = null)

            assertEquals(false, settingsItems.any { it.type == SettingsMenuItemType.DC_API })
            verify(dcapiSettingProvider, never()).isEnabled()
        }

    @Test
    fun `Given Biometrics user selection is false, When toggleBiometricsAuthentication is called, Then true is stored`() =
        coroutineRule.runTest {
            whenever(biometricInteractor.getBiometricUserSelection()).thenReturn(false)

            interactor.toggleBiometricsAuthentication()

            verify(biometricInteractor).storeBiometricsUsageDecision(true)
        }

    @Test
    fun `Given Biometrics user selection is true, When toggleBiometricsAuthentication is called, Then false is stored`() =
        coroutineRule.runTest {
            whenever(biometricInteractor.getBiometricUserSelection()).thenReturn(true)

            interactor.toggleBiometricsAuthentication()

            verify(biometricInteractor).storeBiometricsUsageDecision(false)
        }

    @Test
    fun `Given show batch issuance counter preference is true, When toggleShowBatchIssuanceCounter is called, Then false is stored`() =
        coroutineRule.runTest {
            mockShowBatchIssuanceCounterPreference(response = true)

            interactor.toggleShowBatchIssuanceCounter()

            verify(prefKeys).setShowBatchIssuanceCounter(false)
        }

    @Test
    fun `Given show batch issuance counter preference is false, When toggleShowBatchIssuanceCounter is called, Then true is stored`() =
        coroutineRule.runTest {
            mockShowBatchIssuanceCounterPreference(response = false)

            interactor.toggleShowBatchIssuanceCounter()

            verify(prefKeys).setShowBatchIssuanceCounter(true)
        }

    @Test
    fun `Given registration check preference is true, When toggleRegistrationCheck is called, Then false is stored`() =
        coroutineRule.runTest {
            mockRegistrationCheckPreference(response = true)

            interactor.toggleRegistrationCheck()

            verify(prefKeys).setRegistrationCheckEnabled(false)
        }

    @Test
    fun `Given registration check preference is false, When toggleRegistrationCheck is called, Then true is stored`() =
        coroutineRule.runTest {
            mockRegistrationCheckPreference(response = false)

            interactor.toggleRegistrationCheck()

            verify(prefKeys).setRegistrationCheckEnabled(true)
        }

    @Test
    fun `Given the Digital Credential API is on, When toggleDCAPI is called, Then it is turned off and the registry is updated`() =
        coroutineRule.runTest {
            whenever(dcapiSettingProvider.isEnabled()).thenReturn(true)

            interactor.toggleDCAPI()

            inOrder(dcapiSettingProvider, walletCoreDCAPIRegistrationController) {
                verify(dcapiSettingProvider).setEnabled(false)
                verify(walletCoreDCAPIRegistrationController).registerCredentials()
            }
        }

    @Test
    fun `Given the Digital Credential API is off, When toggleDCAPI is called, Then it is turned on and the registry is updated`() =
        coroutineRule.runTest {
            whenever(dcapiSettingProvider.isEnabled()).thenReturn(false)

            interactor.toggleDCAPI()

            inOrder(dcapiSettingProvider, walletCoreDCAPIRegistrationController) {
                verify(dcapiSettingProvider).setEnabled(true)
                verify(walletCoreDCAPIRegistrationController).registerCredentials()
            }
        }

    private fun mockShowBatchIssuanceCounterPreference(response: Boolean) {
        whenever(suspend { prefKeys.getShowBatchIssuanceCounter() }).thenReturn(response)
    }

    private fun mockRegistrationCheckPreference(response: Boolean) {
        whenever(suspend { prefKeys.getRegistrationCheckEnabled() }).thenReturn(response)
    }

    private fun mockStringsNeededForGetSettingsItemsUi(
        resourcesProvider: ResourceProvider,
        changeLogUrlIsNull: Boolean,
        deviceSupportsBiometrics: Boolean = false,
    ) {
        mockResourceProviderStrings(
            resourcesProvider,
            listOf(
                R.string.settings_screen_option_retrieve_logs to retrieveLogsText,
                R.string.settings_screen_option_show_batch_issuance_counter to showBatchIssuanceCounterText,
                R.string.settings_screen_option_registration_check to registrationCheckText,
                R.string.settings_screen_option_reset_wallet to resetWalletText,
            )
        )

        if (deviceSupportsBiometrics) {
            mockResourceProviderStrings(
                resourcesProvider,
                listOf(
                    R.string.settings_screen_option_biometrics_authentication to biometricsAuthenticationText,
                )
            )
        }

        if (!changeLogUrlIsNull) {
            mockResourceProviderStrings(
                resourcesProvider,
                listOf(
                    R.string.settings_screen_option_changelog to changelogText,
                )
            )
        }
    }

    private fun assertShowBatchIssuanceCounterItem(
        item: SettingsItemUi,
        isChecked: Boolean,
    ) {
        assertEquals(SettingsMenuItemType.SHOW_BATCH_ISSUANCE_COUNTER, item.type)
        assertEquals(SettingsMenuItemType.SHOW_BATCH_ISSUANCE_COUNTER.itemId, item.data.itemId)
        val mainContent = item.data.mainContentData as ListItemMainContentDataUi.Text
        assertEquals(showBatchIssuanceCounterText, mainContent.text)
        val trailingSwitch =
            item.data.trailingContentData as ListItemTrailingContentDataUi.Switch
        assertEquals(isChecked, trailingSwitch.switchData.isChecked)
        assertEquals(true, trailingSwitch.switchData.enabled)
    }

    private fun assertRegistrationCheckItem(
        item: SettingsItemUi,
        isChecked: Boolean,
    ) {
        assertEquals(SettingsMenuItemType.REGISTRATION_CHECK, item.type)
        assertEquals(SettingsMenuItemType.REGISTRATION_CHECK.itemId, item.data.itemId)
        val mainContent = item.data.mainContentData as ListItemMainContentDataUi.Text
        assertEquals(registrationCheckText, mainContent.text)
        val leadingIcon = item.data.leadingContentData as ListItemLeadingContentDataUi.Icon
        assertEquals(AppIcons.Verified, leadingIcon.iconData)
        val trailingSwitch =
            item.data.trailingContentData as ListItemTrailingContentDataUi.Switch
        assertEquals(isChecked, trailingSwitch.switchData.isChecked)
        assertEquals(true, trailingSwitch.switchData.enabled)
    }

    private fun assertResetWalletItem(item: SettingsItemUi) {
        assertEquals(SettingsMenuItemType.RESET_WALLET, item.type)
        assertEquals(SettingsMenuItemType.RESET_WALLET.itemId, item.data.itemId)
        val mainContent = item.data.mainContentData as ListItemMainContentDataUi.Text
        assertEquals(resetWalletText, mainContent.text)
        val leadingIcon = item.data.leadingContentData as ListItemLeadingContentDataUi.Icon
        assertEquals(AppIcons.Delete, leadingIcon.iconData)
        val trailingIcon = item.data.trailingContentData as ListItemTrailingContentDataUi.Icon
        assertEquals(AppIcons.KeyboardArrowRight, trailingIcon.iconData)
    }

    private val biometricsAuthenticationText = "Authenticate with biometrics"
    private val retrieveLogsText = "Retrieve logs"
    private val showBatchIssuanceCounterText = "Batch issuance counter"
    private val changelogText = "Changelog"
    private val biometricsFailureText = "Biometrics unavailable"
    private val registrationCheckText = "Check Registration Certificates"
    private val resetWalletText = "Reset wallet"
    private val dcApiText = "Allow document requests from apps and websites (DC API)"

    /**
     * the standing value for the cases that are about other rows
     */
    private val mockedRegistrationCheckEnabled = true
}