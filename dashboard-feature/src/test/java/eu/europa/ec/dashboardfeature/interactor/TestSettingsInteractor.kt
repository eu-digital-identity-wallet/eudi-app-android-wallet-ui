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

package eu.europa.ec.dashboardfeature.interactor

import android.net.Uri
import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.dashboardfeature.ui.settings.model.SettingsMenuItemType
import eu.europa.ec.dashboardfeature.util.mockedChangeLogUrl
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.StringResourceProviderMocker.mockResourceProviderStrings
import eu.europa.ec.testfeature.util.mockedUriPath1
import eu.europa.ec.testfeature.util.mockedUriPath2
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemLeadingContentDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TestSettingsInteractor {

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
    fun `Given no Changelog URL, When getSettingsItemsUi is called, Then only two SettingsItemUi entries are returned`() {
        // Given
        mockStringsNeededForGetSettingsItemsUi(
            resourcesProvider = resourceProvider,
            changeLogUrlIsNull = true,
        )

        // Suppose the switch is currently ON:
        mockGetShowBatchIssuanceCounterCall(response = true)

        // When
        val settingsItems = interactor.getSettingsItemsUi(changelogUrl = null)

        // Then
        // 1. Size = 2 (SHOW_BATCH_ISSUANCE_COUNTER + RETRIEVE_LOGS)
        assertEquals(2, settingsItems.size)

        // 2. First item: SHOW_BATCH_ISSUANCE_COUNTER
        val firstItem = settingsItems[0]
        // 2a. Type
        assertEquals(SettingsMenuItemType.SHOW_BATCH_ISSUANCE_COUNTER, firstItem.type)
        // 2b. itemId
        assertEquals(showBatchIssuanceCounterIdString, firstItem.data.itemId)
        // 2c. mainContentData.text
        val mainContent1 =
            firstItem.data.mainContentData as ListItemMainContentDataUi.Text
        assertEquals(showBatchIssuanceCounterText, mainContent1.text)
        // 2d. trailingContentData is a Switch, and its state is driven by prefKeys.getShowBatchIssuanceCounter()
        val trailingSwitch1 =
            firstItem.data.trailingContentData as ListItemTrailingContentDataUi.Switch
        val switchData1 = trailingSwitch1.switchData
        assertEquals(true, switchData1.isChecked)
        assertEquals(true, switchData1.enabled)

        // 3. Second item: RETRIEVE_LOGS
        val secondItem = settingsItems[1]
        // 3a. Type
        assertEquals(SettingsMenuItemType.RETRIEVE_LOGS, secondItem.type)
        // 3b. itemId
        assertEquals(retrieveLogsIdString, secondItem.data.itemId)
        // 3c. mainContentData.text
        val mainContent2 =
            secondItem.data.mainContentData as ListItemMainContentDataUi.Text
        assertEquals(retrieveLogsText, mainContent2.text)
        // 3d. leadingContentData is an icon with AppIcons.OpenNew
        val leadingIcon2 =
            secondItem.data.leadingContentData as ListItemLeadingContentDataUi.Icon
        assertEquals(AppIcons.OpenNew, leadingIcon2.iconData)
        // 3e. trailingContentData is an icon with AppIcons.KeyboardArrowRight
        val trailingIcon2 =
            secondItem.data.trailingContentData as ListItemTrailingContentDataUi.Icon
        assertEquals(AppIcons.KeyboardArrowRight, trailingIcon2.iconData)
    }

    @Test
    fun `Given a Changelog URL, When getSettingsItemsUi is called, Then three SettingsItemUi entries are returned`() {
        // Given
        mockStringsNeededForGetSettingsItemsUi(
            resourcesProvider = resourceProvider,
            changeLogUrlIsNull = false,
        )

        // Suppose the switch is currently OFF:
        mockGetShowBatchIssuanceCounterCall(response = false)

        val sampleChangelogUrl = mockedChangeLogUrl

        // When
        val settingsItems = interactor.getSettingsItemsUi(changelogUrl = sampleChangelogUrl)

        // Then
        // 1. Size = 3 (SHOW_BATCH_ISSUANCE_COUNTER + RETRIEVE_LOGS + CHANGELOG)
        assertEquals(3, settingsItems.size)

        // 2. First item: SHOW_BATCH_ISSUANCE_COUNTER
        val firstItem = settingsItems[0]
        assertEquals(SettingsMenuItemType.SHOW_BATCH_ISSUANCE_COUNTER, firstItem.type)
        assertEquals(showBatchIssuanceCounterIdString, firstItem.data.itemId)
        val mainContent1 =
            firstItem.data.mainContentData as ListItemMainContentDataUi.Text
        assertEquals(showBatchIssuanceCounterText, mainContent1.text)
        val trailingSwitch1 =
            firstItem.data.trailingContentData as ListItemTrailingContentDataUi.Switch
        val switchData1 = trailingSwitch1.switchData
        assertEquals(false, switchData1.isChecked)
        assertEquals(true, switchData1.enabled)

        // 3. Second item: RETRIEVE_LOGS
        val secondItem = settingsItems[1]
        assertEquals(SettingsMenuItemType.RETRIEVE_LOGS, secondItem.type)
        assertEquals(retrieveLogsIdString, secondItem.data.itemId)
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
        assertEquals(changelogIdString, thirdItem.data.itemId)
        val mainContent3 =
            thirdItem.data.mainContentData as ListItemMainContentDataUi.Text
        assertEquals(changelogText, mainContent3.text)
        val leadingIcon3 =
            thirdItem.data.leadingContentData as ListItemLeadingContentDataUi.Icon
        assertEquals(AppIcons.OpenInBrowser, leadingIcon3.iconData)
        val trailingIcon3 =
            thirdItem.data.trailingContentData as ListItemTrailingContentDataUi.Icon
        assertEquals(AppIcons.KeyboardArrowRight, trailingIcon3.iconData)
    }
    //endregion

    //region getShowBatchIssuanceCounter
    @Test
    fun `Given showBatchIssuanceCounter is stored as false, When getShowBatchIssuanceCounter is called, Then it returns false`() {
        // Given
        val expectedShowBatchIssuanceCounter = false
        mockGetShowBatchIssuanceCounterCall(response = expectedShowBatchIssuanceCounter)

        // When
        val actual = interactor.getShowBatchIssuanceCounter()

        // Then
        assertEquals(expectedShowBatchIssuanceCounter, actual)
        verify(prefKeys, times(1))
            .getShowBatchIssuanceCounter()
    }

    @Test
    fun `Given showBatchIssuanceCounter is stored as true, When getShowBatchIssuanceCounter is called, Then it returns true`() {
        // Given
        val expectedShowBatchIssuanceCounter = true
        mockGetShowBatchIssuanceCounterCall(response = expectedShowBatchIssuanceCounter)

        // When
        val actual = interactor.getShowBatchIssuanceCounter()

        // Then
        assertEquals(expectedShowBatchIssuanceCounter, actual)
        verify(prefKeys, times(1))
            .getShowBatchIssuanceCounter()
    }
    //endregion

    //region toggleShowBatchIssuanceCounter
    @Test
    fun `Given showBatchIssuanceCounter is currently false, When toggleShowBatchIssuanceCounter is called, Then it flips to true`() {
        // Given
        mockGetShowBatchIssuanceCounterCall(response = false)

        // When
        interactor.toggleShowBatchIssuanceCounter()

        // Then
        verify(prefKeys, times(1))
            .setShowBatchIssuanceCounter(value = true)
    }

    @Test
    fun `Given showBatchIssuanceCounter is currently true, When toggleShowBatchIssuanceCounter is called, Then it flips to false`() {
        // Given
        mockGetShowBatchIssuanceCounterCall(response = true)

        // When
        interactor.toggleShowBatchIssuanceCounter()

        // Then
        verify(prefKeys, times(1))
            .setShowBatchIssuanceCounter(value = false)
    }
    //endregion

    //region Mock Calls
    private fun mockGetShowBatchIssuanceCounterCall(response: Boolean) {
        whenever(prefKeys.getShowBatchIssuanceCounter())
            .thenReturn(response)
    }

    private fun mockStringsNeededForGetSettingsItemsUi(
        resourcesProvider: ResourceProvider,
        changeLogUrlIsNull: Boolean,
    ) {
        mockResourceProviderStrings(
            resourcesProvider,
            listOf(
                R.string.settings_screen_option_show_batch_issuance_counter_id to showBatchIssuanceCounterIdString,
                R.string.settings_screen_option_show_batch_issuance_counter to showBatchIssuanceCounterText,
                R.string.settings_screen_option_retrieve_logs_id to retrieveLogsIdString,
                R.string.settings_screen_option_retrieve_logs to retrieveLogsText,
            )
        )

        if (!changeLogUrlIsNull) {
            mockResourceProviderStrings(
                resourcesProvider,
                listOf(
                    R.string.settings_screen_option_changelog_id to changelogIdString,
                    R.string.settings_screen_option_changelog to changelogText,
                )
            )
        }
    }
    //endregion

    //region Mocked objects needed for tests.
    private val showBatchIssuanceCounterIdString = "batchIssuanceCounterId"
    private val showBatchIssuanceCounterText = "Batch issuance counter"
    private val retrieveLogsIdString = "retrieveLogsId"
    private val retrieveLogsText = "Retrieve logs"
    private val changelogIdString = "changelogId"
    private val changelogText = "Changelog"
    //endregion
}