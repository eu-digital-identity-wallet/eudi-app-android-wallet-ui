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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
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

    private lateinit var interactor: SettingsInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = SettingsInteractorImpl(
            biometricInteractor = biometricInteractor,
            configLogic = configLogic,
            logController = logController,
            resourceProvider = resourceProvider,
            prefKeys = prefKeys,
        )
    }

    @After
    fun after() {
        closeable.close()
    }

    //region getAppVersion
    @Test
    fun `Given an App Version, When getAppVersion is called, Then it returns the Apps Version`() {
        // Given
        val expectedAppVersion = "2024.01.1"
        whenever(configLogic.appVersion)
            .thenReturn(expectedAppVersion)

        // When
        val actualAppVersion = interactor.getAppVersion()

        // Then
        assertEquals(expectedAppVersion, actualAppVersion)
        verify(configLogic, times(1))
            .appVersion
    }
    //endregion

    //region getChangelogUrl
    @Test
    fun `Given a Changelog URL in configLogic, When getChangelogUrl is called, Then it returns the Changelog URL`() {
        // Given
        val expectedUrl = mockedChangeLogUrl
        whenever(configLogic.changelogUrl)
            .thenReturn(expectedUrl)

        // When
        val actualUrl = interactor.getChangelogUrl()

        // Then
        assertEquals(expectedUrl, actualUrl)
        verify(configLogic, times(1))
            .changelogUrl
    }

    @Test
    fun `Given no Changelog URL in configLogic, When getChangelogUrl is called, Then it returns null`() {
        // Given
        val expectedUrl = null
        whenever(configLogic.changelogUrl)
            .thenReturn(expectedUrl)

        // When
        val actualUrl = interactor.getChangelogUrl()

        // Then
        assertEquals(expectedUrl, actualUrl)
        verify(configLogic, times(1))
            .changelogUrl
    }
    //endregion

    //region retrieveLogFileUris
    @Test
    fun `Given a list of logs via logController, When retrieveLogFileUris is called, Then expected logs are returned`() {
        // Given
        val mockedArrayList = arrayListOf(
            Uri.parse(mockedUriPath1),
            Uri.parse(mockedUriPath2)
        )
        whenever(logController.retrieveLogFileUris()).thenReturn(mockedArrayList)

        // When
        val expectedLogFileUris = interactor.retrieveLogFileUris()

        // Then
        assertEquals(mockedArrayList, expectedLogFileUris)
    }
    //endregion

    //region getSettingsItemsUi
    @Test
    fun `Given no Changelog URL and show batch issuance counter preference is true, When getSettingsItemsUi is called, Then two SettingsItemUi entries are returned`() =
        coroutineRule.runTest {
            // Given
            whenever(biometricInteractor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.Failure(biometricsFailureText))
            mockShowBatchIssuanceCounterPreference(response = true)

            mockStringsNeededForGetSettingsItemsUi(
                resourcesProvider = resourceProvider,
                changeLogUrlIsNull = true,
            )

            // When
            val settingsItems = interactor.getSettingsItemsUi(changelogUrl = null)

            // Then
            // 1. Size = 2 (SHOW_BATCH_ISSUANCE_COUNTER + RETRIEVE_LOGS)
            assertEquals(2, settingsItems.size)

            // 2. First item: SHOW_BATCH_ISSUANCE_COUNTER
            val firstItem = settingsItems[0]
            assertShowBatchIssuanceCounterItem(firstItem, isChecked = true)

            // 3. Second item: RETRIEVE_LOGS
            val secondItem = settingsItems[1]
            assertEquals(SettingsMenuItemType.RETRIEVE_LOGS, secondItem.type)
            assertEquals(SettingsMenuItemType.RETRIEVE_LOGS.itemId, secondItem.data.itemId)
            val mainContent2 =
                secondItem.data.mainContentData as ListItemMainContentDataUi.Text
            assertEquals(retrieveLogsText, mainContent2.text)
            val leadingIcon2 =
                secondItem.data.leadingContentData as ListItemLeadingContentDataUi.Icon
            assertEquals(AppIcons.OpenNew, leadingIcon2.iconData)
            val trailingIcon2 =
                secondItem.data.trailingContentData as ListItemTrailingContentDataUi.Icon
            assertEquals(AppIcons.KeyboardArrowRight, trailingIcon2.iconData)

            verify(biometricInteractor, never()).getBiometricUserSelection()
        }

    @Test
    fun `Given a Changelog URL and show batch issuance counter preference is true, When getSettingsItemsUi is called, Then three SettingsItemUi entries are returned`() =
        coroutineRule.runTest {
            // Given
            whenever(biometricInteractor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.Failure(biometricsFailureText))
            mockShowBatchIssuanceCounterPreference(response = true)

            mockStringsNeededForGetSettingsItemsUi(
                resourcesProvider = resourceProvider,
                changeLogUrlIsNull = false,
            )

            val sampleChangelogUrl = mockedChangeLogUrl

            // When
            val settingsItems = interactor.getSettingsItemsUi(changelogUrl = sampleChangelogUrl)

            // Then
            // 1. Size = 3 (SHOW_BATCH_ISSUANCE_COUNTER + RETRIEVE_LOGS + CHANGELOG)
            assertEquals(3, settingsItems.size)

            // 2. First item: SHOW_BATCH_ISSUANCE_COUNTER
            val firstItem = settingsItems[0]
            assertShowBatchIssuanceCounterItem(firstItem, isChecked = true)

            // 3. Second item: RETRIEVE_LOGS
            val secondItem = settingsItems[1]
            assertEquals(SettingsMenuItemType.RETRIEVE_LOGS, secondItem.type)
            assertEquals(SettingsMenuItemType.RETRIEVE_LOGS.itemId, secondItem.data.itemId)
            val mainContent2 =
                secondItem.data.mainContentData as ListItemMainContentDataUi.Text
            assertEquals(retrieveLogsText, mainContent2.text)
            val leadingIcon2 =
                secondItem.data.leadingContentData as ListItemLeadingContentDataUi.Icon
            assertEquals(AppIcons.OpenNew, leadingIcon2.iconData)
            val trailingIcon2 =
                secondItem.data.trailingContentData as ListItemTrailingContentDataUi.Icon
            assertEquals(AppIcons.KeyboardArrowRight, trailingIcon2.iconData)

            // 4. Third item: CHANGELOG
            val thirdItem = settingsItems[2]
            assertEquals(SettingsMenuItemType.CHANGELOG, thirdItem.type)
            assertEquals(SettingsMenuItemType.CHANGELOG.itemId, thirdItem.data.itemId)
            val mainContent3 =
                thirdItem.data.mainContentData as ListItemMainContentDataUi.Text
            assertEquals(changelogText, mainContent3.text)
            val leadingIcon3 =
                thirdItem.data.leadingContentData as ListItemLeadingContentDataUi.Icon
            assertEquals(AppIcons.OpenInBrowser, leadingIcon3.iconData)
            val trailingIcon3 =
                thirdItem.data.trailingContentData as ListItemTrailingContentDataUi.Icon
            assertEquals(AppIcons.KeyboardArrowRight, trailingIcon3.iconData)

            verify(biometricInteractor, never()).getBiometricUserSelection()
        }

    @Test
    fun `Given device supports Biometrics with no Changelog URL and show batch issuance counter preference is true, When getSettingsItemsUi is called, Then Biometrics and Retrieve Logs entries are returned`() =
        coroutineRule.runTest {
            // Given
            whenever(biometricInteractor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.CanAuthenticate)
            whenever(biometricInteractor.getBiometricUserSelection())
                .thenReturn(true)
            mockShowBatchIssuanceCounterPreference(response = true)

            mockStringsNeededForGetSettingsItemsUi(
                resourcesProvider = resourceProvider,
                changeLogUrlIsNull = true,
                deviceSupportsBiometrics = true,
            )

            // When
            val settingsItems = interactor.getSettingsItemsUi(changelogUrl = null)

            // Then
            assertEquals(3, settingsItems.size)

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
        }

    @Test
    fun `Given device has Biometrics with user selection false and show batch issuance counter preference is true, When getSettingsItemsUi is called, Then Biometrics switch is unchecked`() =
        coroutineRule.runTest {
            // Given
            whenever(biometricInteractor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.NonEnrolled)
            whenever(biometricInteractor.getBiometricUserSelection())
                .thenReturn(false)
            mockShowBatchIssuanceCounterPreference(response = true)

            mockStringsNeededForGetSettingsItemsUi(
                resourcesProvider = resourceProvider,
                changeLogUrlIsNull = true,
                deviceSupportsBiometrics = true,
            )

            // When
            val settingsItems = interactor.getSettingsItemsUi(changelogUrl = null)

            // Then
            assertEquals(3, settingsItems.size)

            val biometricsItem = settingsItems[0]
            assertEquals(SettingsMenuItemType.BIOMETRICS_AUTHENTICATION, biometricsItem.type)
            val trailingSwitch =
                biometricsItem.data.trailingContentData as ListItemTrailingContentDataUi.Switch
            assertEquals(false, trailingSwitch.switchData.isChecked)
            assertEquals(true, trailingSwitch.switchData.enabled)
        }

    @Test
    fun `Given device supports Biometrics with a Changelog URL and show batch issuance counter preference is true, When getSettingsItemsUi is called, Then four SettingsItemUi entries are returned`() =
        coroutineRule.runTest {
            // Given
            whenever(biometricInteractor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.CanAuthenticate)
            whenever(biometricInteractor.getBiometricUserSelection())
                .thenReturn(true)
            mockShowBatchIssuanceCounterPreference(response = true)

            mockStringsNeededForGetSettingsItemsUi(
                resourcesProvider = resourceProvider,
                changeLogUrlIsNull = false,
                deviceSupportsBiometrics = true,
            )

            val sampleChangelogUrl = mockedChangeLogUrl

            // When
            val settingsItems = interactor.getSettingsItemsUi(changelogUrl = sampleChangelogUrl)

            // Then
            assertEquals(4, settingsItems.size)
            assertEquals(SettingsMenuItemType.BIOMETRICS_AUTHENTICATION, settingsItems[0].type)
            assertEquals(SettingsMenuItemType.SHOW_BATCH_ISSUANCE_COUNTER, settingsItems[1].type)
            assertEquals(SettingsMenuItemType.RETRIEVE_LOGS, settingsItems[2].type)
            assertEquals(SettingsMenuItemType.CHANGELOG, settingsItems[3].type)
        }

    @Test
    fun `Given show batch issuance counter preference is false, When getSettingsItemsUi is called, Then switch is unchecked`() =
        coroutineRule.runTest {
            // Given
            whenever(biometricInteractor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.Failure(biometricsFailureText))
            mockShowBatchIssuanceCounterPreference(response = false)

            mockStringsNeededForGetSettingsItemsUi(
                resourcesProvider = resourceProvider,
                changeLogUrlIsNull = true,
            )

            // When
            val settingsItems = interactor.getSettingsItemsUi(changelogUrl = null)

            // Then
            val showBatchItem = settingsItems[0]
            assertShowBatchIssuanceCounterItem(showBatchItem, isChecked = false)
        }
    //endregion

    //region toggleBiometricsAuthentication
    @Test
    fun `Given Biometrics user selection is false, When toggleBiometricsAuthentication is called, Then true is stored`() =
        coroutineRule.runTest {
            // Given
            whenever(biometricInteractor.getBiometricUserSelection()).thenReturn(false)

            // When
            interactor.toggleBiometricsAuthentication()

            // Then
            verify(biometricInteractor).storeBiometricsUsageDecision(true)
        }

    @Test
    fun `Given Biometrics user selection is true, When toggleBiometricsAuthentication is called, Then false is stored`() =
        coroutineRule.runTest {
            // Given
            whenever(biometricInteractor.getBiometricUserSelection()).thenReturn(true)

            // When
            interactor.toggleBiometricsAuthentication()

            // Then
            verify(biometricInteractor).storeBiometricsUsageDecision(false)
        }
    //endregion

    //region showBatchIssuanceCounter
    @Test
    fun `Given show batch issuance counter preference is true, When toggleShowBatchIssuanceCounter is called, Then false is stored`() =
        coroutineRule.runTest {
            // Given
            mockShowBatchIssuanceCounterPreference(response = true)

            // When
            interactor.toggleShowBatchIssuanceCounter()

            // Then
            verify(prefKeys).setShowBatchIssuanceCounter(false)
        }

    @Test
    fun `Given show batch issuance counter preference is false, When toggleShowBatchIssuanceCounter is called, Then true is stored`() =
        coroutineRule.runTest {
            // Given
            mockShowBatchIssuanceCounterPreference(response = false)

            // When
            interactor.toggleShowBatchIssuanceCounter()

            // Then
            verify(prefKeys).setShowBatchIssuanceCounter(true)
        }
    //endregion

    //region Mock Calls
    private fun mockShowBatchIssuanceCounterPreference(response: Boolean) {
        whenever(suspend { prefKeys.getShowBatchIssuanceCounter() }).thenReturn(response)
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
    //endregion

    //region Assertions
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
    //endregion

    //region Mocked objects needed for tests.
    private val biometricsAuthenticationText = "Authenticate with biometrics"
    private val retrieveLogsText = "Retrieve logs"
    private val showBatchIssuanceCounterText = "Batch issuance counter"
    private val changelogText = "Changelog"
    private val biometricsFailureText = "Biometrics unavailable"
    //endregion
}