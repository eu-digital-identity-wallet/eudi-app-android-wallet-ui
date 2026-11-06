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

package eu.europa.ec.proximityfeature.ui.qr

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelStore
import eu.europa.ec.commonfeature.config.PresentationMode
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.proximityfeature.interactor.ProximityQRInteractor
import eu.europa.ec.proximityfeature.interactor.ProximityQRPartialState
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TestProximityQRViewModel {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private val interactor: ProximityQRInteractor = mock()

    private val uiSerializer: UiSerializer = mock()

    private val store = ViewModelStore()

    private val config = RequestUriConfig(
        mode = PresentationMode.Ble(initiatorRoute = "DASHBOARD", sessionId = "session-1")
    )

    private val leftForeground =
        Event.OnStop(isChangingConfigurations = false, isCurrentScreen = true)

    private val stoppedForConfigurationChange =
        Event.OnStop(isChangingConfigurations = true, isCurrentScreen = true)

    @Before
    fun before() {
        whenever(uiSerializer.fromBase64(anyOrNull(), eq(RequestUriConfig::class.java), any()))
            .thenReturn(config)
    }

    @Test
    fun `Given a QR shown and no reader connected, When the screen goes away, Then the session is stopped`() {
        coroutineRule.runTest {
            whenever(interactor.startQrEngagement()).thenReturn(
                flowOf(
                    ProximityQRPartialState.QrReady(
                        "qr"
                    )
                )
            )
            val viewModel = ProximityQRViewModel(interactor, uiSerializer, "config")

            viewModel.setEvent(Event.Init)
            coroutineRule.testScope.runCurrent()
            store.put("qr", viewModel)
            store.clear()

            verify(interactor).cancelTransfer()
        }
    }

    @Test
    fun `Given a reader connected, When the QR screen goes away, Then the session is left to the request screen`() {
        coroutineRule.runTest {
            whenever(interactor.startQrEngagement()).thenReturn(flowOf(ProximityQRPartialState.Connected))
            val viewModel = ProximityQRViewModel(interactor, uiSerializer, "config")

            viewModel.setEvent(Event.Init)
            coroutineRule.testScope.runCurrent()
            store.put("qr", viewModel)
            store.clear()

            verify(interactor, never()).cancelTransfer()
        }
    }

    @Test
    fun `Given the user went back, When the screen is then removed, Then the session is stopped once`() {
        coroutineRule.runTest {
            whenever(interactor.startQrEngagement()).thenReturn(
                flowOf(
                    ProximityQRPartialState.QrReady(
                        "qr"
                    )
                )
            )
            val viewModel = ProximityQRViewModel(interactor, uiSerializer, "config")

            viewModel.setEvent(Event.Init)
            coroutineRule.testScope.runCurrent()
            viewModel.setEvent(Event.GoBack)
            coroutineRule.testScope.runCurrent()
            store.put("qr", viewModel)
            store.clear()

            verify(interactor, times(1)).cancelTransfer()
        }
    }

    @Test
    fun `Given a screen removed before its session started, When it is cleared, Then nothing is stopped`() {
        val viewModel = ProximityQRViewModel(interactor, uiSerializer, "config")

        store.put("qr", viewModel)
        store.clear()

        verify(interactor, never()).cancelTransfer()
    }

    @Test
    fun `Given a QR shown, When the app leaves the foreground, Then the session is stopped and the screen closes`() {
        coroutineRule.runTest {
            val viewModel = createViewModelShowingQr()

            viewModel.effect.runFlowTest {
                viewModel.setEvent(leftForeground)
                coroutineRule.testScope.runCurrent()

                assertEquals(Effect.Navigation.Pop, awaitItem())
            }
            verify(interactor).cancelTransfer()
        }
    }

    @Test
    fun `Given a QR shown, When the activity stops for a configuration change, Then the session keeps running`() {
        coroutineRule.runTest {
            val viewModel = createViewModelShowingQr()

            viewModel.effect.runFlowTest {
                viewModel.setEvent(stoppedForConfigurationChange)
                coroutineRule.testScope.runCurrent()

                expectNoEvents()
            }
            verify(interactor, never()).cancelTransfer()
        }
    }

    @Test
    fun `Given a reader connected, When the QR screen stops, Then the session is left to the request screen`() {
        coroutineRule.runTest {
            whenever(interactor.startQrEngagement())
                .thenReturn(flowOf(ProximityQRPartialState.Connected))
            val viewModel = ProximityQRViewModel(interactor, uiSerializer, "config")

            viewModel.setEvent(Event.Init)
            coroutineRule.testScope.runCurrent()
            viewModel.setEvent(leftForeground)
            coroutineRule.testScope.runCurrent()

            verify(interactor, never()).cancelTransfer()
        }
    }

    @Test
    fun `Given another screen taking the QR screen's place, When the activity stops, Then the session stops and nothing is closed`() {
        coroutineRule.runTest {
            val viewModel = createViewModelShowingQr()

            viewModel.effect.runFlowTest {
                viewModel.setEvent(
                    Event.OnStop(isChangingConfigurations = false, isCurrentScreen = false)
                )
                coroutineRule.testScope.runCurrent()

                expectNoEvents()
            }
            verify(interactor).cancelTransfer()
        }
    }

    @Test
    fun `Given the session stopped on leaving the foreground, When the screen resumes, Then NFC engagement stays off`() {
        coroutineRule.runTest {
            val activity: ComponentActivity = mock()
            val viewModel = createViewModelShowingQr()

            viewModel.setEvent(leftForeground)
            coroutineRule.testScope.runCurrent()
            viewModel.setEvent(Event.NfcEngagement(componentActivity = activity, enable = true))
            coroutineRule.testScope.runCurrent()

            verify(interactor, never()).toggleNfcEngagement(any(), eq(true))
        }
    }

    @Test
    fun `Given a QR shown, When the screen resumes, Then NFC engagement is switched on`() {
        coroutineRule.runTest {
            val activity: ComponentActivity = mock()
            val viewModel = createViewModelShowingQr()

            viewModel.setEvent(Event.NfcEngagement(componentActivity = activity, enable = true))
            coroutineRule.testScope.runCurrent()

            verify(interactor).toggleNfcEngagement(activity, true)
        }
    }

    @Test
    fun `Given the session stopped on leaving the foreground, When the screen is then removed, Then the session is stopped once`() {
        coroutineRule.runTest {
            val viewModel = createViewModelShowingQr()

            viewModel.setEvent(leftForeground)
            coroutineRule.testScope.runCurrent()
            store.put("qr", viewModel)
            store.clear()

            verify(interactor, times(1)).cancelTransfer()
        }
    }

    @Test
    fun `Given the user went back, When the activity then stops, Then the session is stopped once and nothing else closes`() {
        coroutineRule.runTest {
            val viewModel = createViewModelShowingQr()

            viewModel.effect.runFlowTest {
                viewModel.setEvent(Event.GoBack)
                coroutineRule.testScope.runCurrent()
                assertEquals(Effect.Navigation.Pop, awaitItem())

                viewModel.setEvent(leftForeground)
                coroutineRule.testScope.runCurrent()

                expectNoEvents()
            }
            verify(interactor, times(1)).cancelTransfer()
        }
    }

    @Test
    fun `Given a session not started yet, When the activity stops, Then nothing is stopped or closed`() {
        coroutineRule.runTest {
            val viewModel = ProximityQRViewModel(interactor, uiSerializer, "config")

            viewModel.effect.runFlowTest {
                viewModel.setEvent(leftForeground)
                coroutineRule.testScope.runCurrent()

                expectNoEvents()
            }
            verify(interactor, never()).cancelTransfer()
        }
    }

    /**
     * Creates the view model and starts its session, with the QR code shown and no reader
     * connected.
     *
     * @return The view model, initialised.
     */
    private fun createViewModelShowingQr(): ProximityQRViewModel {
        whenever(interactor.startQrEngagement()).thenReturn(
            flowOf(
                ProximityQRPartialState.QrReady(
                    "qr"
                )
            )
        )
        val viewModel = ProximityQRViewModel(interactor, uiSerializer, "config")
        viewModel.setEvent(Event.Init)
        coroutineRule.testScope.runCurrent()
        return viewModel
    }
}