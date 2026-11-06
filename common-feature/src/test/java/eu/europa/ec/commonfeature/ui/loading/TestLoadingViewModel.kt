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

package eu.europa.ec.commonfeature.ui.loading

import android.content.Context
import android.net.Uri
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.content.ContentHeaderConfig
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration

@RunWith(RobolectricTestRunner::class)
class TestLoadingViewModel {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    /** The shared loading logic, where [reject] shows a rejection that carries a redirect. */
    private class FakeLoadingViewModel : LoadingViewModel() {
        override fun getHeaderConfig() = ContentHeaderConfig(description = null)

        override fun getPreviousScreen(): Screen = DashboardScreens.Dashboard

        override fun getCallerScreen(): Screen = DashboardScreens.Dashboard

        override fun doWork(context: Context) {}

        override fun getCancellableTimeout(): Duration = Duration.ZERO

        fun reject(redirect: String) {
            showRejection(
                initiatorRoute = INITIATOR_ROUTE,
                redirectUri = Uri.parse(redirect),
                headerConfig = ContentHeaderConfig(description = null),
            )
        }
    }

    @Test
    fun `Given a rejection with an https redirect, When it is closed, Then the redirect is opened`() {
        coroutineRule.runTest {
            val viewModel = FakeLoadingViewModel()
            viewModel.reject(redirect = "https://verifier.example/rejected")

            viewModel.effect.runFlowTest {
                viewModel.setEvent(Event.CloseRejection)

                assertEquals(
                    Effect.Navigation.CloseRejection(
                        initiatorRoute = INITIATOR_ROUTE,
                        redirectUri = Uri.parse("https://verifier.example/rejected"),
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Given a rejection with a redirect into one of the wallet's own flows, When it is closed, Then nothing is opened`() {
        coroutineRule.runTest {
            listOf(
                "openid-credential-offer://?credential_offer_uri=https%3A%2F%2Fattacker.example",
                "eudi-rqes://sign?request_uri=https%3A%2F%2Fattacker.example%2Fdoc",
                "eudi-wallet://dashboard",
            ).forEach { redirect ->
                val viewModel = FakeLoadingViewModel()
                viewModel.reject(redirect = redirect)

                viewModel.effect.runFlowTest {
                    viewModel.setEvent(Event.CloseRejection)

                    assertEquals(
                        Effect.Navigation.CloseRejection(
                            initiatorRoute = INITIATOR_ROUTE,
                            redirectUri = null
                        ),
                        awaitItem()
                    )
                }
            }
        }
    }

    private companion object {
        const val INITIATOR_ROUTE = "PRESENTATION_REQUEST"
    }
}