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

package eu.europa.ec.dashboardfeature.ui.dashboard

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import eu.europa.ec.dashboardfeature.ui.component.BottomNavigationBar
import eu.europa.ec.dashboardfeature.ui.component.BottomNavigationItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-w400dp-h800dp")
class TestDashboardScreen {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    /**
     * 1. The Documents tab is open.
     * 2. The user taps Home, and the clock moves on by less than Navigation's default fade.
     *
     * Expected result:
     * Only Home is on screen, so a tap that follows can reach nothing of the Documents tab.
     */
    @Test
    fun `Given the Documents tab, When Home is tapped, Then the Documents tab is gone at once`() {
        // Given
        showTabs()
        openDocumentsTab()

        // When
        composeRule.onNodeWithText(tabLabel(BottomNavigationItem.Home)).performClick()
        composeRule.mainClock.advanceTimeBy(SHORTER_THAN_DEFAULT_FADE_MS)

        // Then
        composeRule.onNodeWithText(DOCUMENTS_CONTENT).assertDoesNotExist()
        composeRule.onNodeWithText(HOME_CONTENT).assertIsDisplayed()
    }

    /**
     * 1. The Documents tab is open, and the user taps Home.
     * 2. Before the next frame is drawn, the user taps the Documents tab's content, which is still
     *    on screen.
     *
     * Expected result:
     * The tap does nothing.
     */
    @Test
    fun `Given a switch to Home under way, When the Documents tab is tapped, Then the tap does nothing`() {
        // Given
        var documentsTaps = 0
        showTabs(
            documentsTab = {
                Text(
                    text = DOCUMENTS_CONTENT,
                    modifier = Modifier.clickable { documentsTaps++ },
                )
            }
        )
        openDocumentsTab()
        composeRule.onNodeWithText(tabLabel(BottomNavigationItem.Home)).performClick()

        // When
        composeRule.onNodeWithText(DOCUMENTS_CONTENT).performClick()

        // Then
        assertEquals(0, documentsTaps)
    }

    private fun showTabs(documentsTab: @Composable () -> Unit = { Text(DOCUMENTS_CONTENT) }) {
        composeRule.setContent {
            val navController = rememberNavController()
            Scaffold(bottomBar = { BottomNavigationBar(navController) }) { padding ->
                DashboardTabs(
                    navController = navController,
                    modifier = Modifier.padding(padding),
                    homeTab = { Text(HOME_CONTENT) },
                    documentsTab = documentsTab,
                    transactionsTab = { Text(TRANSACTIONS_CONTENT) },
                )
            }
        }
    }

    /** Opens the Documents tab and stops the clock, so frames are drawn only when a test asks. */
    private fun openDocumentsTab() {
        composeRule.onNodeWithText(tabLabel(BottomNavigationItem.Documents)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(DOCUMENTS_CONTENT).assertIsDisplayed()
        composeRule.mainClock.autoAdvance = false
    }

    private fun tabLabel(item: BottomNavigationItem) = context.getString(item.titleRes)

    private companion object {
        const val HOME_CONTENT = "home tab content"
        const val DOCUMENTS_CONTENT = "documents tab content"
        const val TRANSACTIONS_CONTENT = "transactions tab content"
        const val SHORTER_THAN_DEFAULT_FADE_MS = 100L
    }
}
