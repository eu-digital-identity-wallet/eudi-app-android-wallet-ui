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

package eu.europa.ec.commonfeature.ui.pin

import eu.europa.ec.authenticationlogic.provider.PinLockoutState
import eu.europa.ec.authenticationlogic.secure.SecurePinImpl
import eu.europa.ec.commonfeature.interactor.DeviceLockNotice
import eu.europa.ec.commonfeature.interactor.QuickPinInteractor
import eu.europa.ec.commonfeature.model.PinFlow
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TestPinViewModel {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private val interactor: QuickPinInteractor = mock()

    private val resourceProvider: ResourceProvider = mock()

    private val uiSerializer: UiSerializer = mock()

    @Before
    fun before() {
        whenever(resourceProvider.getString(any())).thenReturn("text")
    }

    @Test
    fun `Given wallet setup, When a PIN that is easy to guess is entered, Then it is refused once the field has cleared`() =
        coroutineRule.runTest {
            whenever(interactor.getWeakPinError(any())).thenReturn(WEAK)
            val viewModel = viewModel(PinFlow.CREATE_WITHOUT_ACTIVATION)
            val pin = SecurePinImpl("123456")

            viewModel.handleEvents(Event.PinEntered(pin))

            assertTrue(pin.isCleared)
            assertTrue(viewModel.viewState.value.isLoading)
            coroutineRule.testScope.advanceUntilIdle()
            assertEquals(WEAK, viewModel.viewState.value.quickPinError)
            assertFalse(viewModel.viewState.value.isLoading)
            assertEquals(PinValidationState.ENTER, viewModel.viewState.value.pinState)
        }

    @Test
    fun `Given wallet setup, When a PIN that is not easy to guess is entered, Then the user is asked to enter it again`() =
        coroutineRule.runTest {
            whenever(interactor.getWeakPinError(any())).thenReturn(null)
            val viewModel = viewModel(PinFlow.CREATE_WITHOUT_ACTIVATION)

            viewModel.handleEvents(Event.PinEntered(SecurePinImpl("135792")))
            coroutineRule.testScope.advanceUntilIdle()

            assertNull(viewModel.viewState.value.quickPinError)
            assertEquals(PinValidationState.REENTER, viewModel.viewState.value.pinState)
        }

    @Test
    fun `Given no screen lock and none required, When the setup screen resumes, Then a warning is shown and entry continues`() =
        coroutineRule.runTest {
            whenever(interactor.getDeviceLockNotice()).thenReturn(DeviceLockNotice.Warning(NO_LOCK))
            val viewModel = viewModel(PinFlow.CREATE_WITHOUT_ACTIVATION)

            viewModel.handleEvents(Event.OnResume)

            assertEquals(NO_LOCK, viewModel.viewState.value.deviceLockNotice)
            assertFalse(viewModel.viewState.value.isBlockedByDeviceLock)
        }

    @Test
    fun `Given no screen lock and one required, When the setup screen resumes, Then entered PINs are dropped`() =
        coroutineRule.runTest {
            whenever(interactor.getDeviceLockNotice()).thenReturn(
                DeviceLockNotice.Blocking(
                    LOCK_REQUIRED
                )
            )
            val viewModel = viewModel(PinFlow.CREATE_WITH_ACTIVATION)
            val pin = SecurePinImpl("135792")

            viewModel.handleEvents(Event.OnResume)
            viewModel.handleEvents(Event.PinEntered(pin))
            coroutineRule.testScope.advanceUntilIdle()

            assertEquals(LOCK_REQUIRED, viewModel.viewState.value.deviceLockNotice)
            assertTrue(viewModel.viewState.value.isBlockedByDeviceLock)
            assertTrue(pin.isCleared)
            assertEquals(PinValidationState.ENTER, viewModel.viewState.value.pinState)
            verify(interactor, never()).getWeakPinError(any())
        }

    @Test
    fun `Given a screen lock is set later, When the setup screen resumes, Then the block is lifted`() =
        coroutineRule.runTest {
            whenever(interactor.getDeviceLockNotice())
                .thenReturn(DeviceLockNotice.Blocking(LOCK_REQUIRED))
                .thenReturn(null)
            val viewModel = viewModel(PinFlow.CREATE_WITH_ACTIVATION)

            viewModel.handleEvents(Event.OnResume)
            viewModel.handleEvents(Event.OnResume)

            assertNull(viewModel.viewState.value.deviceLockNotice)
            assertFalse(viewModel.viewState.value.isBlockedByDeviceLock)
        }

    @Test
    fun `Given the PIN of a set-up wallet is changed, When the screen resumes, Then the screen lock is not checked`() =
        coroutineRule.runTest {
            val viewModel = viewModel(PinFlow.UPDATE)

            viewModel.handleEvents(Event.OnResume)

            assertNull(viewModel.viewState.value.deviceLockNotice)
            verify(interactor, never()).getDeviceLockNotice()
        }

    /**
     * At zero the change-PIN screen reads the state again, as the login screen does: a lockout that
     * is still running goes on, and the read that finds it over clears it in the provider.
     */
    @Test
    fun `Given a lockout on the change-PIN screen, When its countdown reaches zero while it still runs, Then it goes on`() =
        coroutineRule.runTest {
            whenever(interactor.getPinLockoutState()).thenReturn(
                PinLockoutState.Active(remaining = 1.seconds, total = 30.seconds),
                PinLockoutState.Active(remaining = 2.seconds, total = 30.seconds),
                PinLockoutState.Idle,
            )
            val viewModel = viewModel(PinFlow.UPDATE)

            viewModel.handleEvents(Event.Init)
            coroutineRule.testScope.advanceTimeBy(1_500.milliseconds)
            coroutineRule.testScope.runCurrent()

            assertTrue(viewModel.viewState.value.isLockedOut)

            coroutineRule.testScope.advanceUntilIdle()

            assertFalse(viewModel.viewState.value.isLockedOut)
            verify(interactor, times(3)).getPinLockoutState()
        }

    private fun viewModel(pinFlow: PinFlow) = PinViewModel(
        interactor = interactor,
        resourceProvider = resourceProvider,
        uiSerializer = uiSerializer,
        pinFlow = pinFlow,
    )

    private companion object {
        const val WEAK = "This PIN is too easy to guess"
        const val NO_LOCK = "This device has no screen lock"
        const val LOCK_REQUIRED = "Set a screen lock"
    }
}