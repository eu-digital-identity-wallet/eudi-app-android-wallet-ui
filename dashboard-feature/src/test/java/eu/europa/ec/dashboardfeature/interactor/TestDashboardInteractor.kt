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

import android.content.Context
import android.content.SharedPreferences
import eu.europa.ec.dashboardfeature.ui.dashboard.model.SideMenuTypeUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.StringResourceProviderMocker.mockResourceProviderStrings
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemLeadingContentDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TestDashboardInteractor {

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var interactor: DashboardInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = DashboardInteractorImpl(
            resourceProvider = resourceProvider,
        )
    }

    @After
    fun after() {
        closeable.close()
    }

    //region getSideMenuOptions
    @Test
    fun `When getSideMenuOptions is called, Then it returns three items with correct data`() {
        // Arrange
        mockStringsNeededForGetSideMenuOptions(resourceProvider)
        mockCertificatePreference(resourceProvider)

        // When
        val sideMenuItems = interactor.getSideMenuOptions()

        // Then
        assertEquals(3, sideMenuItems.size)

        // 1. First item: CHANGE_PIN
        val firstItem = sideMenuItems[0]
        assertEquals(SideMenuTypeUi.CHANGE_PIN, firstItem.type)
        assertEquals(changePinIdString, firstItem.data.itemId)
        val mainContent1 = firstItem.data.mainContentData as ListItemMainContentDataUi.Text
        assertEquals(changePinText, mainContent1.text)
        val leadingIcon1 = firstItem.data.leadingContentData as ListItemLeadingContentDataUi.Icon
        assertEquals(AppIcons.ChangePin, leadingIcon1.iconData)
        val trailingIcon1 = firstItem.data.trailingContentData as ListItemTrailingContentDataUi.Icon
        assertEquals(AppIcons.KeyboardArrowRight, trailingIcon1.iconData)

        // 2. Second item: SETTINGS
        val secondItem = sideMenuItems[1]
        assertEquals(SideMenuTypeUi.SETTINGS, secondItem.type)
        assertEquals(settingsIdString, secondItem.data.itemId)
        val mainContent2 = secondItem.data.mainContentData as ListItemMainContentDataUi.Text
        assertEquals(settingsText, mainContent2.text)
        val leadingIcon2 = secondItem.data.leadingContentData as ListItemLeadingContentDataUi.Icon
        assertEquals(AppIcons.Settings, leadingIcon2.iconData)
        val trailingIcon2 =
            secondItem.data.trailingContentData as ListItemTrailingContentDataUi.Icon
        assertEquals(AppIcons.KeyboardArrowRight, trailingIcon2.iconData)

        // 3. Third item: ENABLE CERTIFICATE
        val thirdItem = sideMenuItems[2]
        assertEquals(SideMenuTypeUi.ENABLE_CERTIFICATE, thirdItem.type)
        assertEquals(certificateIdString, thirdItem.data.itemId)
        val mainContent3 = thirdItem.data.mainContentData as ListItemMainContentDataUi.Text
        assertEquals(certificateText, mainContent3.text)
        val leadingIcon3 = thirdItem.data.leadingContentData as ListItemLeadingContentDataUi.Icon
        assertEquals(AppIcons.Certified, leadingIcon3.iconData)

        // Verify that getString was called exactly once per resource ID
        verify(resourceProvider, times(1))
            .getString(R.string.dashboard_side_menu_option_change_pin_id)
        verify(resourceProvider, times(1))
            .getString(R.string.dashboard_side_menu_option_change_pin)
        verify(resourceProvider, times(1))
            .getString(R.string.dashboard_side_menu_option_settings_id)
        verify(resourceProvider, times(1))
            .getString(R.string.dashboard_side_menu_option_settings)
        verify(resourceProvider, times(1))
            .getString(R.string.dashboard_side_menu_enable_certificates_id)
        verify(resourceProvider, times(1))
            .getString(R.string.dashboard_side_menu_enable_certificates)
    }
    //endregion

    //region Mock Calls
    private fun mockStringsNeededForGetSideMenuOptions(resourcesProvider: ResourceProvider) {
        mockResourceProviderStrings(
            resourcesProvider,
            listOf(
                R.string.dashboard_side_menu_option_change_pin_id to changePinIdString,
                R.string.dashboard_side_menu_option_change_pin to changePinText,
                R.string.dashboard_side_menu_option_settings_id to settingsIdString,
                R.string.dashboard_side_menu_option_settings to settingsText,
                R.string.dashboard_side_menu_enable_certificates to certificateText,
                R.string.dashboard_side_menu_enable_certificates_id to certificateIdString
            )
        )
    }

    private fun mockCertificatePreference(resourceProvider: ResourceProvider) {
        val mockContext: Context = mock()
        val mockPrefs: SharedPreferences = mock()

        whenever(resourceProvider.provideContext()).thenReturn(mockContext)
        whenever(mockContext.getSharedPreferences(any(), any())).thenReturn(mockPrefs)
        whenever(mockPrefs.getBoolean(any(), any())).thenReturn(true)
    }
    //endregion

    //region Mocked objects needed for tests.
    private val changePinIdString = "changePinId"
    private val changePinText = "Change PIN"
    private val settingsIdString = "settingsId"
    private val settingsText = "Settings"
    private val certificateIdString = "enableCertificatesId"
    private val certificateText = "Enable certificate check"
    //endregion
}