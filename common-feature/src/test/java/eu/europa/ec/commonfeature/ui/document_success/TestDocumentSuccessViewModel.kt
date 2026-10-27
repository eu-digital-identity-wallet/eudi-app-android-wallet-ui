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

package eu.europa.ec.commonfeature.ui.document_success

import android.content.Intent
import android.net.Uri
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TestDocumentSuccessViewModel {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    /** A success screen whose next step is a verifier's redirect to [link]. */
    private class FakeDocumentSuccessViewModel(private val link: String) :
        DocumentSuccessViewModel() {
        override fun getNextScreenConfigNavigation() = ConfigNavigation(
            navigationType = NavigationType.Deeplink(link = link, routeToPop = INITIATOR_ROUTE)
        )

        override fun doWork() {}

        override fun getPendingIntent(): Intent? = null
    }

    @Test
    fun `Given an https redirect, When the user continues, Then it opens in the browser and the screen returns to its initiator`() {
        coroutineRule.runTest {
            val viewModel =
                FakeDocumentSuccessViewModel(link = "https://verifier.example/done?state=1")

            viewModel.effect.runFlowTest {
                viewModel.setEvent(Event.StickyButtonPressed)

                assertEquals(
                    Effect.Navigation.OpenRedirect(
                        link = Uri.parse("https://verifier.example/done?state=1"),
                        routeToPop = INITIATOR_ROUTE,
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Given a redirect into one of the wallet's own flows, When the user continues, Then nothing is opened`() {
        coroutineRule.runTest {
            listOf(
                "eudi-rqes://sign?request_uri=https%3A%2F%2Fattacker.example%2Fdoc",
                "openid-credential-offer://?credential_offer_uri=https%3A%2F%2Fattacker.example",
                "openid4vp://?request_uri=https%3A%2F%2Fattacker.example",
                "eudi-wallet://dashboard",
                "http://verifier.example/done",
            ).forEach { link ->
                val viewModel = FakeDocumentSuccessViewModel(link = link)

                viewModel.effect.runFlowTest {
                    viewModel.setEvent(Event.StickyButtonPressed)

                    assertEquals(
                        Effect.Navigation.OpenRedirect(link = null, routeToPop = INITIATOR_ROUTE),
                        awaitItem()
                    )
                }
            }
        }
    }

    private companion object {
        const val INITIATOR_ROUTE = "DASHBOARD"
    }
}