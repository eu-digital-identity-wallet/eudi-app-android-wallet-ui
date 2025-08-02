/*
 * Copyright (c) 2025 European Commission
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

    private lateinit var interactor: SettingsInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = SettingsInteractorImpl(
            configLogic = configLogic,
            logController = logController,
            resourceProvider = resourceProvider,
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
    fun `Given no Changelog URL, When getSettingsItemsUi is called, Then only one SettingsItemUi entry is returned`() {
        // Given
        mockStringsNeededForGetSettingsItemsUi(
            resourcesProvider = resourceProvider,
            changeLogUrlIsNull = true,
        )

        // When
        val settingsItems = interactor.getSettingsItemsUi(changelogUrl = null)

        // Then
        // 1. Size = 1 (RETRIEVE_LOGS)
        assertEquals(1, settingsItems.size)

        // 2. First item: RETRIEVE_LOGS
        val secondItem = settingsItems[0]
        // 2a. Type
        assertEquals(SettingsMenuItemType.RETRIEVE_LOGS, secondItem.type)
        // 2b. itemId
        assertEquals(retrieveLogsIdString, secondItem.data.itemId)
        // 2c. mainContentData.text
        val mainContent2 =
            secondItem.data.mainContentData as ListItemMainContentDataUi.Text
        assertEquals(retrieveLogsText, mainContent2.text)
        // 2d. leadingContentData is an icon with AppIcons.OpenNew
        val leadingIcon2 =
            secondItem.data.leadingContentData as ListItemLeadingContentDataUi.Icon
        assertEquals(AppIcons.OpenNew, leadingIcon2.iconData)
        // 2e. trailingContentData is an icon with AppIcons.KeyboardArrowRight
        val trailingIcon2 =
            secondItem.data.trailingContentData as ListItemTrailingContentDataUi.Icon
        assertEquals(AppIcons.KeyboardArrowRight, trailingIcon2.iconData)
    }

    @Test
    fun `Given a Changelog URL, When getSettingsItemsUi is called, Then two SettingsItemUi entries are returned`() {
        // Given
        mockStringsNeededForGetSettingsItemsUi(
            resourcesProvider = resourceProvider,
            changeLogUrlIsNull = false,
        )

        val sampleChangelogUrl = mockedChangeLogUrl

        // When
        val settingsItems = interactor.getSettingsItemsUi(changelogUrl = sampleChangelogUrl)

        // Then
        // 1. Size = 2 (RETRIEVE_LOGS + CHANGELOG)
        assertEquals(2, settingsItems.size)

        // 2. First item: RETRIEVE_LOGS
        val firstItem = settingsItems[0]
        assertEquals(SettingsMenuItemType.RETRIEVE_LOGS, firstItem.type)
        assertEquals(retrieveLogsIdString, firstItem.data.itemId)
        val mainContent1 =
            firstItem.data.mainContentData as ListItemMainContentDataUi.Text
        assertEquals(retrieveLogsText, mainContent1.text)
        val leadingIcon1 =
            firstItem.data.leadingContentData as ListItemLeadingContentDataUi.Icon
        assertEquals(AppIcons.OpenNew, leadingIcon1.iconData)
        val trailingIcon1 =
            firstItem.data.trailingContentData as ListItemTrailingContentDataUi.Icon
        assertEquals(AppIcons.KeyboardArrowRight, trailingIcon1.iconData)

        // 3. Second item: CHANGELOG
        val secondItem = settingsItems[1]
        assertEquals(SettingsMenuItemType.CHANGELOG, secondItem.type)
        assertEquals(changelogIdString, secondItem.data.itemId)
        val mainContent2 =
            secondItem.data.mainContentData as ListItemMainContentDataUi.Text
        assertEquals(changelogText, mainContent2.text)
        val leadingIcon2 =
            secondItem.data.leadingContentData as ListItemLeadingContentDataUi.Icon
        assertEquals(AppIcons.OpenInBrowser, leadingIcon2.iconData)
        val trailingIcon2 =
            secondItem.data.trailingContentData as ListItemTrailingContentDataUi.Icon
        assertEquals(AppIcons.KeyboardArrowRight, trailingIcon2.iconData)
    }
    //endregion

    //region Mock Calls
    private fun mockStringsNeededForGetSettingsItemsUi(
        resourcesProvider: ResourceProvider,
        changeLogUrlIsNull: Boolean,
    ) {
        mockResourceProviderStrings(
            resourcesProvider,
            listOf(
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
    private val retrieveLogsIdString = "retrieveLogsId"
    private val retrieveLogsText = "Retrieve logs"
    private val changelogIdString = "changelogId"
    private val changelogText = "Changelog"
    //endregion
}