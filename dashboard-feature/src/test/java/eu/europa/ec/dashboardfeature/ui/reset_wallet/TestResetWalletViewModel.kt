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

package eu.europa.ec.dashboardfeature.ui.reset_wallet

import androidx.lifecycle.ViewModelStore
import eu.europa.ec.dashboardfeature.interactor.ResetWalletInteractor
import eu.europa.ec.dashboardfeature.interactor.ResetWalletInteractorPartialState
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TestResetWalletViewModel {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var interactor: ResetWalletInteractor

    @Mock
    private lateinit var resources: ResourceProvider

    private val store = ViewModelStore()

    private lateinit var viewModel: ResetWalletViewModel

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        whenever(resources.getString(any())).thenReturn("text")
        whenever(interactor.isSessionUnlocked()).thenReturn(true)
        viewModel = ResetWalletViewModel(
            resetWalletInteractor = interactor,
            resourceProvider = resources,
        )
        store.put("resetWallet", viewModel)
    }

    @After
    fun after() {
        try {
            store.clear()
            coroutineRule.testScope.runCurrent()
        } finally {
            closeable.close()
        }
    }

    @Test
    fun `Given a screen restored with the wallet locked, When it starts, Then the reset does not run`() =
        coroutineRule.runTest {
            whenever(interactor.isSessionUnlocked()).thenReturn(false)

            viewModel.effect.runFlowTest {
                viewModel.handleEvents(Event.Init)
                coroutineRule.testScope.runCurrent()

                expectNoEvents()
            }
            verify(interactor, never()).resetWallet()
        }

    @Test
    fun `Given a successful reset, When the screen starts, Then the wallet starts again`() =
        coroutineRule.runTest {
            whenever(interactor.resetWallet()).thenReturn(ResetWalletInteractorPartialState.Success)

            viewModel.effect.runFlowTest {
                viewModel.handleEvents(Event.Init)

                assertEquals(Effect.Navigation.StartAgain, awaitItem())
            }
        }

    @Test
    fun `Given a reset in progress, When Retry is sent, Then loading shows and the reset runs once`() =
        coroutineRule.runTest {
            val released = CompletableDeferred<Unit>()
            whenever(interactor.resetWallet()).doSuspendableAnswer {
                released.await()
                ResetWalletInteractorPartialState.Success
            }

            viewModel.handleEvents(Event.Init)
            coroutineRule.testScope.runCurrent()
            viewModel.handleEvents(Event.Retry)
            coroutineRule.testScope.runCurrent()

            assertTrue(viewModel.viewState.value.isLoading)
            verify(interactor, times(1)).resetWallet()

            released.complete(Unit)
            coroutineRule.testScope.runCurrent()
        }

    @Test
    fun `Given a failed reset, When the error is shown, Then it offers retry and close`() =
        coroutineRule.runTest {
            whenever(interactor.resetWallet())
                .thenReturn(ResetWalletInteractorPartialState.Failure(errorMessage = "error"))

            viewModel.handleEvents(Event.Init)
            coroutineRule.testScope.runCurrent()

            val error = viewModel.viewState.value.error
            assertFalse(viewModel.viewState.value.isLoading)
            assertNotNull(error)
            assertEquals("error", error?.errorSubTitle)
            assertNotNull(error?.onRetry)

            whenever(interactor.resetWallet()).thenReturn(ResetWalletInteractorPartialState.Success)
            viewModel.effect.runFlowTest {
                error?.onRetry?.invoke()

                assertEquals(Effect.Navigation.StartAgain, awaitItem())
            }
            assertNull(viewModel.viewState.value.error)
            verify(interactor, times(2)).resetWallet()
        }

    @Test
    fun `Given a failed reset, When the user closes the error, Then the wallet starts again`() =
        coroutineRule.runTest {
            whenever(interactor.resetWallet())
                .thenReturn(ResetWalletInteractorPartialState.Failure(errorMessage = "error"))

            viewModel.handleEvents(Event.Init)
            coroutineRule.testScope.runCurrent()

            viewModel.effect.runFlowTest {
                viewModel.viewState.value.error?.onCancel()

                assertEquals(Effect.Navigation.StartAgain, awaitItem())
            }
        }
}