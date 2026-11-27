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

package eu.europa.ec.commonfeature.ui.biometric

import android.content.Context
import androidx.lifecycle.ViewModelStore
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAuthenticate
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAvailability
import eu.europa.ec.authenticationlogic.provider.PinLockoutState
import eu.europa.ec.authenticationlogic.secure.SecurePinImpl
import eu.europa.ec.businesslogic.extension.toUri
import eu.europa.ec.commonfeature.config.BiometricMode
import eu.europa.ec.commonfeature.config.BiometricUiConfig
import eu.europa.ec.commonfeature.config.OnBackNavigationConfig
import eu.europa.ec.commonfeature.interactor.BiometricInteractor
import eu.europa.ec.commonfeature.interactor.DeviceLockNotice
import eu.europa.ec.commonfeature.interactor.QuickPinInteractorPinValidPartialState
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.FlowCompletion
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
class TestBiometricViewModel {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var interactor: BiometricInteractor

    @Mock
    private lateinit var resources: ResourceProvider

    @Mock
    private lateinit var serializer: UiSerializer

    @Mock
    private lateinit var context: Context

    private val store = ViewModelStore()
    private val callbacks = mutableListOf<(BiometricsAuthenticate) -> Unit>()

    private lateinit var viewModel: BiometricViewModel
    private lateinit var click: Event.OnBiometricsClicked

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        click = Event.OnBiometricsClicked(context, shouldThrowErrorIfNotAvailable = true)

        val config = BiometricUiConfig(
            mode = BiometricMode.Login("Welcome", "Biometrics", "PIN"),
            onSuccessNavigation = ConfigNavigation(NavigationType.Pop),
            onBackNavigationConfig = OnBackNavigationConfig(null, false)
        )
        whenever(
            serializer.fromBase64(
                "config",
                BiometricUiConfig::class.java,
                BiometricUiConfig.Parser
            )
        ).thenReturn(config)
        whenever(interactor.getBiometricsAvailability())
            .thenReturn(BiometricsAvailability.CanAuthenticate)
        whenever(interactor.maxFailedPinAttempts).thenReturn(MAX_ATTEMPTS)
        whenever(
            resources.getQuantityString(
                eq(R.plurals.quick_pin_locked_out),
                eq(MAX_ATTEMPTS),
                any(),
                any()
            )
        )
            .thenAnswer { invocation ->
                val args = invocation.arguments.drop(2).flatMap {
                    if (it is Array<*>) it.toList() else listOf(it)
                }
                args.joinToString(separator = ":", prefix = "locked:")
            }

        doAnswer {
            callbacks += it.getArgument<(BiometricsAuthenticate) -> Unit>(2)
        }.whenever(interactor).authenticateWithBiometrics(any(), any(), any())

        viewModel = BiometricViewModel(
            biometricInteractor = interactor,
            resourceProvider = resources,
            uiSerializer = serializer,
            biometricConfig = "config"
        )
        store.put("biometric", viewModel)
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
    fun `Given a biometric prompt, When the user taps rapidly and then cancels, Then one prompt opens and a retry is permitted`() {
        // When
        repeat(30) { viewModel.handleEvents(click) }

        // Then
        assertEquals(1, callbacks.size)
        verify(interactor).authenticateWithBiometrics(eq(context), eq(false), any())

        // When
        completeAuthentication(BiometricsAuthenticate.Cancelled)

        // Then
        assertNull(viewModel.viewState.value.error)

        // When
        viewModel.handleEvents(click)

        // Then
        assertEquals(2, callbacks.size)
    }

    @Test
    fun `Given a prompt that fails, When the user taps, dismisses the failure and taps again, Then the failure is shown and dismissed and a retry opens a new prompt`() {
        // When
        viewModel.handleEvents(click)
        completeAuthentication(BiometricsAuthenticate.Failed("Locked sensor"))

        // Then
        assertEquals("Locked sensor", viewModel.viewState.value.error?.errorSubTitle)

        // When
        viewModel.handleEvents(Event.OnErrorDismiss)

        // Then
        assertNull(viewModel.viewState.value.error)

        // When
        viewModel.handleEvents(click)

        // Then
        assertEquals(2, callbacks.size)
    }

    @Test
    fun `Given unavailable biometrics, When an automatic attempt is made, Then the PIN stays available without an error overlay`() {
        // Given
        whenever(interactor.getBiometricsAvailability())
            .thenReturn(BiometricsAvailability.Failure("Unsupported biometrics"))

        // When
        viewModel.handleEvents(click.copy(shouldThrowErrorIfNotAvailable = false))

        // Then
        assertNull(viewModel.viewState.value.error)
        assertFalse(viewModel.viewState.value.isLoading)
        assertEquals(0, callbacks.size)
    }

    @Test
    fun `Given unavailable biometrics, When a manual attempt is made, Then the unsupported biometrics are explained`() {
        // Given
        whenever(interactor.getBiometricsAvailability())
            .thenReturn(BiometricsAvailability.Failure("Unsupported biometrics"))

        // When
        viewModel.handleEvents(click)

        // Then
        assertEquals("Unsupported biometrics", viewModel.viewState.value.error?.errorSubTitle)
        assertEquals(0, callbacks.size)
    }

    @Test
    fun `Given a started biometric attempt, When it succeeds despite rapid taps, Then the PIN throttle is reset and navigation happens once`() =
        coroutineRule.runTest {
            viewModel.effect.runFlowTest {
                // When
                viewModel.handleEvents(click)
                completeAuthentication(BiometricsAuthenticate.Success)
                repeat(30) { viewModel.handleEvents(click) }
                coroutineRule.testScope.runCurrent()

                // Then
                assertEquals(1, callbacks.size)
                assertEquals(Effect.Navigation.Pop, awaitItem())
                verify(interactor, times(1)).resetPinThrottle()
                expectNoEvents()
            }
        }

    @Test
    fun `Given a valid PIN, When the biometric prompt is cancelled and the PIN is entered, Then the PIN still succeeds`() =
        coroutineRule.runTest {
            // Given
            val pin = SecurePinImpl("123456")
            whenever(interactor.isPinValid(pin))
                .thenReturn(flowOf(QuickPinInteractorPinValidPartialState.Success))

            viewModel.effect.runFlowTest {
                // When
                viewModel.handleEvents(click)
                completeAuthentication(BiometricsAuthenticate.Cancelled)
                viewModel.handleEvents(Event.OnQuickPinEntered(pin))

                // Then
                assertEquals(Effect.Navigation.Pop, awaitItem())
                verify(interactor).resetPinThrottle()
            }

            pin.close()
        }

    @Test
    fun `Given no enrolled biometrics, When a manual attempt is made, Then the system setup screen opens`() =
        coroutineRule.runTest {
            // Given
            whenever(interactor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.NonEnrolled)

            viewModel.effect.runFlowTest {
                // When
                viewModel.handleEvents(click)

                // Then
                assertEquals(Effect.Navigation.LaunchBiometricsSystemScreen, awaitItem())
                assertEquals(0, callbacks.size)
            }
        }

    @Test
    fun `Given no enrolled biometrics, When an automatic attempt is made, Then nothing is shown`() =
        coroutineRule.runTest {
            // Given
            whenever(interactor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.NonEnrolled)

            viewModel.effect.runFlowTest {
                // When
                viewModel.handleEvents(click.copy(shouldThrowErrorIfNotAvailable = false))
                coroutineRule.testScope.runCurrent()

                // Then
                assertNull(viewModel.viewState.value.error)
                assertEquals(0, callbacks.size)
                expectNoEvents()
            }
        }

    @Test
    fun `Given an unsupported biometrics explanation, When its own action is invoked, Then it is dismissed`() =
        coroutineRule.runTest {
            // Given
            whenever(interactor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.Failure("Unsupported biometrics"))

            viewModel.handleEvents(click)
            assertNotNull(viewModel.viewState.value.error)

            // When
            viewModel.viewState.value.error?.onCancel?.invoke()
            coroutineRule.testScope.runCurrent()

            // Then
            assertNull(viewModel.viewState.value.error)
        }

    @Test
    fun `Given a running lockout, When a new lockout starts, Then it replaces the tick already running`() =
        coroutineRule.runTest {
            // When
            lockViewModelOut(30.seconds)

            // Then
            assertEquals("locked:$MAX_ATTEMPTS:00:30", viewModel.viewState.value.lockoutMessage)

            // When
            whenever(interactor.getPinLockoutState())
                .thenReturn(lockout(90.seconds), PinLockoutState.Idle)
            viewModel.handleEvents(Event.Init)
            coroutineRule.testScope.runCurrent()

            // Then
            assertEquals("locked:$MAX_ATTEMPTS:01:30", viewModel.viewState.value.lockoutMessage)

            // When
            coroutineRule.testScope.advanceTimeBy(1_500.milliseconds)
            coroutineRule.testScope.runCurrent()

            // Then
            assertEquals("locked:$MAX_ATTEMPTS:01:29", viewModel.viewState.value.lockoutMessage)

            // When
            coroutineRule.testScope.advanceTimeBy(30_000.milliseconds)

            // Then
            assertTrue(viewModel.viewState.value.isLockedOut)
            assertEquals("locked:$MAX_ATTEMPTS:00:59", viewModel.viewState.value.lockoutMessage)
        }

    @Test
    fun `Given biometrics enabled, When init runs, Then the preference is published and the prompt is requested`() =
        coroutineRule.runTest {
            // Given
            whenever(interactor.getBiometricUserSelection()).thenReturn(true)
            whenever(interactor.getPinLockoutState()).thenReturn(PinLockoutState.Idle)

            viewModel.effect.runFlowTest {
                // When
                viewModel.handleEvents(Event.Init)
                coroutineRule.testScope.runCurrent()

                // Then
                assertTrue(viewModel.viewState.value.userBiometricsAreEnabled)
                assertEquals(Effect.InitializeBiometricAuthOnCreate, awaitItem())
            }
        }

    @Test
    fun `Given biometrics disabled, When init runs, Then the prompt stays closed`() =
        coroutineRule.runTest {
            // Given
            whenever(interactor.getBiometricUserSelection()).thenReturn(false)
            whenever(interactor.getPinLockoutState()).thenReturn(PinLockoutState.Idle)

            viewModel.effect.runFlowTest {
                // When
                viewModel.handleEvents(Event.Init)
                coroutineRule.testScope.runCurrent()

                // Then
                assertFalse(viewModel.viewState.value.userBiometricsAreEnabled)
                expectNoEvents()
            }
        }

    @Test
    fun `Given a screen that opts out, When init runs, Then the prompt stays closed`() =
        coroutineRule.runTest {
            // Given
            whenever(interactor.getBiometricUserSelection()).thenReturn(true)
            whenever(interactor.getPinLockoutState()).thenReturn(PinLockoutState.Idle)
            val optedOut = newViewModel(
                "optedOut",
                config(shouldInitializeBiometricAuthOnCreate = false)
            )

            optedOut.effect.runFlowTest {
                // When
                optedOut.handleEvents(Event.Init)
                coroutineRule.testScope.runCurrent()

                // Then
                assertTrue(optedOut.viewState.value.userBiometricsAreEnabled)
                expectNoEvents()
            }
        }

    @Test
    fun `Given an active lockout, When init runs, Then it is restored and counted down to release`() =
        coroutineRule.runTest {
            // Given
            whenever(interactor.getBiometricUserSelection()).thenReturn(false)
            whenever(interactor.getPinLockoutState())
                .thenReturn(lockout(3.seconds), PinLockoutState.Idle)

            // When
            viewModel.handleEvents(Event.Init)
            coroutineRule.testScope.runCurrent()

            // Then
            assertTrue(viewModel.viewState.value.isLockedOut)
            assertEquals("locked:$MAX_ATTEMPTS:00:03", viewModel.viewState.value.lockoutMessage)

            // When
            coroutineRule.testScope.advanceTimeBy(1_500.milliseconds)
            coroutineRule.testScope.runCurrent()

            // Then
            assertEquals("locked:$MAX_ATTEMPTS:00:02", viewModel.viewState.value.lockoutMessage)

            // When
            coroutineRule.testScope.advanceUntilIdle()

            // Then
            assertFalse(viewModel.viewState.value.isLockedOut)
            assertNull(viewModel.viewState.value.lockoutMessage)
        }

    /**
     * At zero the state is read again: a lockout that is still running goes on, and the read that
     * finds it over clears it in the provider.
     */
    @Test
    fun `Given a countdown that reaches zero, When the lockout still runs, Then it goes on until the state is idle`() =
        coroutineRule.runTest {
            // Given
            whenever(interactor.getBiometricUserSelection()).thenReturn(false)
            whenever(interactor.getPinLockoutState())
                .thenReturn(lockout(1.seconds), lockout(2.seconds), PinLockoutState.Idle)

            // When
            viewModel.handleEvents(Event.Init)
            coroutineRule.testScope.advanceTimeBy(1_500.milliseconds)
            coroutineRule.testScope.runCurrent()

            // Then
            assertTrue(viewModel.viewState.value.isLockedOut)
            assertEquals("locked:$MAX_ATTEMPTS:00:02", viewModel.viewState.value.lockoutMessage)

            // When
            coroutineRule.testScope.advanceUntilIdle()

            // Then
            assertFalse(viewModel.viewState.value.isLockedOut)
            verify(interactor, times(3)).getPinLockoutState()
        }

    @Test
    fun `Given an expired lockout, When init runs, Then it is released`() = coroutineRule.runTest {
        // Given
        whenever(interactor.getBiometricUserSelection()).thenReturn(false)
        whenever(interactor.getPinLockoutState()).thenReturn(lockout(Duration.ZERO))

        // When
        viewModel.handleEvents(Event.Init)
        coroutineRule.testScope.runCurrent()

        // Then
        assertFalse(viewModel.viewState.value.isLockedOut)
        assertNull(viewModel.viewState.value.lockoutMessage)
    }

    @Test
    fun `Given a shown error, When the system screen is launched, Then the error is cleared and the interactor is called`() =
        coroutineRule.runTest {
            // Given
            viewModel.handleEvents(click)
            completeAuthentication(BiometricsAuthenticate.Failed("Locked sensor"))
            assertNotNull(viewModel.viewState.value.error)

            // When
            viewModel.handleEvents(Event.LaunchBiometricSystemScreen)

            // Then
            assertNull(viewModel.viewState.value.error)
            verify(interactor).launchBiometricSystemScreen()
        }

    @Test
    fun `Given a back navigation that indicates cancellation, When back is pressed, Then the flow is reported as cancelled`() =
        coroutineRule.runTest {
            // Given
            val backable = newViewModel(
                "backable",
                config(
                    onBackNavigation = ConfigNavigation(
                        navigationType = NavigationType.PopTo(DashboardScreens.Dashboard),
                        indicateFlowCompletion = FlowCompletion.CANCEL
                    )
                )
            )

            backable.effect.runFlowTest {
                // When
                backable.handleEvents(Event.OnNavigateBack)

                // Then
                assertEquals(
                    Effect.Navigation.PopBackStackUpTo(
                        screenRoute = DashboardScreens.Dashboard.screenRoute,
                        inclusive = false,
                        indicateFlowCompletion = FlowCompletion.CANCEL
                    ),
                    awaitItem()
                )
            }
        }

    @Test
    fun `Given a back navigation that indicates success, When back is pressed, Then no success completion is reported`() =
        coroutineRule.runTest {
            // Given
            val backable = newViewModel(
                "backableSuccess",
                config(
                    onBackNavigation = ConfigNavigation(
                        navigationType = NavigationType.PopTo(DashboardScreens.Dashboard),
                        indicateFlowCompletion = FlowCompletion.SUCCESS
                    )
                )
            )

            backable.effect.runFlowTest {
                // When
                backable.handleEvents(Event.OnNavigateBack)

                // Then
                assertEquals(
                    Effect.Navigation.PopBackStackUpTo(
                        screenRoute = DashboardScreens.Dashboard.screenRoute,
                        inclusive = false,
                        indicateFlowCompletion = FlowCompletion.NONE
                    ),
                    awaitItem()
                )
            }
        }

    @Test
    fun `Given no back route, When back is pressed, Then nothing is emitted`() =
        coroutineRule.runTest {
            viewModel.effect.runFlowTest {
                // When
                viewModel.handleEvents(Event.OnNavigateBack)
                coroutineRule.testScope.runCurrent()

                // Then
                assertNull(viewModel.viewState.value.error)
                expectNoEvents()
            }
        }

    @Test
    @Suppress("UnusedFlow")
    fun `Given a lockout, When a PIN is entered, Then it is discarded`() = coroutineRule.runTest {
        // Given
        lockViewModelOut()
        val pin = SecurePinImpl("123456")

        // When
        viewModel.handleEvents(Event.OnQuickPinEntered(pin))

        // Then
        assertTrue(pin.isCleared)
        verify(interactor, never()).isPinValid(any())
    }

    @Test
    fun `Given a lockout, When the PIN length changes, Then the change is ignored`() =
        coroutineRule.runTest {
            // Given
            lockViewModelOut()

            // When
            viewModel.handleEvents(Event.OnQuickPinLengthChanged(3))

            // Then
            assertTrue(viewModel.viewState.value.isLockedOut)
            assertEquals("locked:$MAX_ATTEMPTS:00:30", viewModel.viewState.value.lockoutMessage)
        }

    @Test
    fun `Given a wrong PIN error, When the PIN length changes, Then the error is cleared`() =
        coroutineRule.runTest {
            // Given
            whenever(interactor.recordPinFailure()).thenReturn(PinLockoutState.Idle)

            // When
            val pin = enterPin("123456", QuickPinInteractorPinValidPartialState.Failed("Wrong PIN"))

            // Then
            assertEquals("Wrong PIN", viewModel.viewState.value.quickPinError)

            // When
            viewModel.handleEvents(Event.OnQuickPinLengthChanged(3))

            // Then
            assertNull(viewModel.viewState.value.quickPinError)

            pin.close()
        }

    @Test
    @Suppress("UnusedFlow")
    fun `Given an incomplete PIN, When it is entered, Then it is discarded without validation`() =
        coroutineRule.runTest {
            // Given
            val pin = SecurePinImpl("123")

            // When
            viewModel.handleEvents(Event.OnQuickPinEntered(pin))

            // Then
            assertTrue(pin.isCleared)
            assertFalse(viewModel.viewState.value.isLoading)
            verify(interactor, never()).isPinValid(any())
        }

    @Test
    fun `Given attempts remaining, When a wrong PIN is entered, Then the error is shown`() =
        coroutineRule.runTest {
            // Given
            whenever(interactor.recordPinFailure()).thenReturn(PinLockoutState.Idle)

            // When
            val pin = enterPin("123456", QuickPinInteractorPinValidPartialState.Failed("Wrong PIN"))

            // Then
            assertEquals("Wrong PIN", viewModel.viewState.value.quickPinError)
            assertFalse(viewModel.viewState.value.isLoading)
            assertFalse(viewModel.viewState.value.isLockedOut)

            pin.close()
        }

    @Test
    fun `Given one attempt remaining, When a wrong PIN is entered, Then the lockout starts`() =
        coroutineRule.runTest {
            // Given
            whenever(interactor.recordPinFailure()).thenReturn(lockout(30.seconds))
            whenever(interactor.getPinLockoutState()).thenReturn(PinLockoutState.Idle)

            // When
            val pin = enterPin("123456", QuickPinInteractorPinValidPartialState.Failed("Wrong PIN"))

            // Then
            assertTrue(viewModel.viewState.value.isLockedOut)
            assertFalse(viewModel.viewState.value.isLoading)
            assertEquals("locked:$MAX_ATTEMPTS:00:30", viewModel.viewState.value.lockoutMessage)

            pin.close()
        }

    @Test
    fun `Given a lockout, When the view model is cleared, Then the lockout state is frozen`() =
        coroutineRule.runTest {
            // Given
            lockViewModelOut()

            // When
            store.clear()
            coroutineRule.testScope.advanceUntilIdle()

            // Then
            assertTrue(viewModel.viewState.value.isLockedOut)
            assertEquals("locked:$MAX_ATTEMPTS:00:30", viewModel.viewState.value.lockoutMessage)
        }

    @Test
    fun `Given a successful authentication, When the user taps again, Then the re-entrancy guard is released`() =
        coroutineRule.runTest {
            viewModel.effect.runFlowTest {
                // When
                viewModel.handleEvents(click)
                completeAuthentication(BiometricsAuthenticate.Success)
                coroutineRule.testScope.runCurrent()

                // Then
                assertEquals(Effect.Navigation.Pop, awaitItem())

                // When
                viewModel.handleEvents(click)

                // Then
                assertEquals(2, callbacks.size)
            }
        }

    @Test
    fun `Given a biometric failure, When its own action is invoked, Then it is dismissed`() =
        coroutineRule.runTest {
            // Given
            viewModel.handleEvents(click)
            completeAuthentication(BiometricsAuthenticate.Failed("Locked sensor"))

            assertNotNull(viewModel.viewState.value.error)

            // When
            viewModel.viewState.value.error?.onCancel?.invoke()
            coroutineRule.testScope.runCurrent()

            // Then
            assertNull(viewModel.viewState.value.error)
        }

    @Test
    fun `Given an invalid configuration, When the state is read, Then it is rejected`() {
        // Given
        whenever(
            serializer.fromBase64(
                "broken",
                BiometricUiConfig::class.java,
                BiometricUiConfig.Parser
            )
        ).thenReturn(null)
        val broken = BiometricViewModel(
            biometricInteractor = interactor,
            resourceProvider = resources,
            uiSerializer = serializer,
            biometricConfig = "broken"
        ).also { store.put("broken", it) }

        // Then
        assertThrows(RuntimeException::class.java) { broken.viewState.value }
    }

    @Test
    fun `Given a pop-to route that indicates success, When the authentication succeeds, Then completion is reported`() {
        // Then
        assertSuccessNavigation(
            key = "popToSuccess",
            navigation = ConfigNavigation(
                navigationType = NavigationType.PopTo(DashboardScreens.Dashboard),
                indicateFlowCompletion = FlowCompletion.SUCCESS
            ),
            expected = Effect.Navigation.PopBackStackUpTo(
                screenRoute = DashboardScreens.Dashboard.screenRoute,
                inclusive = false,
                indicateFlowCompletion = FlowCompletion.SUCCESS
            )
        )
    }

    @Test
    fun `Given a pop-to route that indicates cancellation, When the authentication succeeds, Then no cancellation is reported`() {
        // Then
        assertSuccessNavigation(
            key = "popToCancel",
            navigation = ConfigNavigation(
                navigationType = NavigationType.PopTo(DashboardScreens.Dashboard),
                indicateFlowCompletion = FlowCompletion.CANCEL
            ),
            expected = Effect.Navigation.PopBackStackUpTo(
                screenRoute = DashboardScreens.Dashboard.screenRoute,
                inclusive = false,
                indicateFlowCompletion = FlowCompletion.NONE
            )
        )
    }

    @Test
    fun `Given a pop-to route without completion, When the authentication succeeds, Then no completion is reported`() {
        // Then
        assertSuccessNavigation(
            key = "popToNone",
            navigation = ConfigNavigation(
                navigationType = NavigationType.PopTo(DashboardScreens.Dashboard),
                indicateFlowCompletion = FlowCompletion.NONE
            ),
            expected = Effect.Navigation.PopBackStackUpTo(
                screenRoute = DashboardScreens.Dashboard.screenRoute,
                inclusive = false,
                indicateFlowCompletion = FlowCompletion.NONE
            )
        )
    }

    @Test
    fun `Given a push-screen route with arguments, When the authentication succeeds, Then the screen is pushed with its arguments`() {
        // Then
        assertSuccessNavigation(
            key = "pushScreen",
            navigation = ConfigNavigation(
                navigationType = NavigationType.PushScreen(
                    screen = CommonScreens.QuickPin,
                    arguments = mapOf("pinFlow" to "UPDATE")
                )
            ),
            expected = Effect.Navigation.SwitchScreen(
                screen = "${CommonScreens.QuickPin.screenName}?pinFlow=UPDATE",
                screenPopUpTo = CommonScreens.Biometric.screenRoute
            )
        )
    }

    @Test
    fun `Given a push-route navigation, When the authentication succeeds, Then the raw route is pushed`() {
        // Then
        assertSuccessNavigation(
            key = "pushRoute",
            navigation = ConfigNavigation(
                navigationType = NavigationType.PushRoute(route = "RAW_ROUTE")
            ),
            expected = Effect.Navigation.SwitchScreen(
                screen = "RAW_ROUTE",
                screenPopUpTo = CommonScreens.Biometric.screenRoute
            )
        )
    }

    @Test
    fun `Given a finish navigation, When the authentication succeeds, Then the host is finished`() {
        // Then
        assertSuccessNavigation(
            key = "finish",
            navigation = ConfigNavigation(navigationType = NavigationType.Finish),
            expected = Effect.Navigation.Finish
        )
    }

    @Test
    fun `Given a deeplink navigation in pre-authorization, When the authentication succeeds, Then the deeplink is forwarded with the pre-authorization flag`() =
        coroutineRule.runTest {
            // Given
            val navigation = ConfigNavigation(
                navigationType = NavigationType.Deeplink(
                    link = DEEPLINK,
                    routeToPop = DashboardScreens.Dashboard.screenRoute
                )
            )
            val vm = newViewModel(
                "deeplink",
                config(onSuccessNavigation = navigation, isPreAuthorization = true)
            )

            vm.effect.runFlowTest {
                // When
                vm.handleEvents(Event.OnBiometricsClicked(context, true))
                callbacks.last().invoke(BiometricsAuthenticate.Success)
                coroutineRule.testScope.runCurrent()

                // Then
                assertEquals(
                    Effect.Navigation.Deeplink(
                        link = DEEPLINK.toUri(),
                        isPreAuthorization = true,
                        routeToPop = DashboardScreens.Dashboard.screenRoute
                    ),
                    awaitItem()
                )
            }
        }

    @Test
    fun `Given a pre-authorization check, When the authentication succeeds, Then the session is unlocked`() =
        coroutineRule.runTest {
            // Given
            val vm = newViewModel("login", config(isPreAuthorization = true))

            vm.effect.runFlowTest {
                // When
                vm.handleEvents(Event.OnBiometricsClicked(context, true))
                callbacks.last().invoke(BiometricsAuthenticate.Success)
                coroutineRule.testScope.runCurrent()

                // Then
                assertEquals(Effect.Navigation.Pop, awaitItem())
                verify(interactor).unlockSession()
            }
        }

    @Test
    fun `Given a check that is not a pre-authorization, When the authentication succeeds, Then the session is not unlocked`() =
        coroutineRule.runTest {
            // Given
            val vm = newViewModel("confirmation", config(isPreAuthorization = false))

            vm.effect.runFlowTest {
                // When
                vm.handleEvents(Event.OnBiometricsClicked(context, true))
                callbacks.last().invoke(BiometricsAuthenticate.Success)
                coroutineRule.testScope.runCurrent()

                // Then
                assertEquals(Effect.Navigation.Pop, awaitItem())
                verify(interactor, never()).unlockSession()
            }
        }

    @Test
    fun `Given a device without a screen lock, When the login screen starts, Then it warns and the PIN still logs in`() =
        coroutineRule.runTest {
            // Given
            whenever(interactor.getPinLockoutState()).thenReturn(PinLockoutState.Idle)
            whenever(interactor.getBiometricUserSelection()).thenReturn(false)
            whenever(interactor.getDeviceLockNotice())
                .thenReturn(DeviceLockNotice.Warning(DEVICE_LOCK_WARNING))

            // When
            viewModel.handleEvents(Event.Init)
            coroutineRule.testScope.runCurrent()

            // Then
            assertEquals(DEVICE_LOCK_WARNING, viewModel.viewState.value.deviceLockNotice)
            assertFalse(viewModel.viewState.value.isBlockedByDeviceLock)

            // When
            val pin = enterPin("123456", QuickPinInteractorPinValidPartialState.Success)

            // Then
            verify(interactor).isPinValid(pin)
        }

    @Test
    fun `Given a screen lock required and none set, When the login screen starts, Then the PIN and biometrics are refused`() =
        coroutineRule.runTest {
            // Given
            whenever(interactor.getPinLockoutState()).thenReturn(PinLockoutState.Idle)
            whenever(interactor.getBiometricUserSelection()).thenReturn(false)
            whenever(interactor.getDeviceLockNotice())
                .thenReturn(DeviceLockNotice.Blocking(DEVICE_LOCK_REQUIRED))

            // When
            viewModel.handleEvents(Event.Init)
            coroutineRule.testScope.runCurrent()
            val pin = SecurePinImpl("123456")
            viewModel.handleEvents(Event.OnQuickPinEntered(pin))
            viewModel.handleEvents(click)
            coroutineRule.testScope.runCurrent()

            // Then
            assertEquals(DEVICE_LOCK_REQUIRED, viewModel.viewState.value.deviceLockNotice)
            assertTrue(viewModel.viewState.value.isBlockedByDeviceLock)
            verify(interactor, never()).isPinValid(any())
            assertEquals(0, callbacks.size)
        }

    @Test
    fun `Given a login blocked by the missing screen lock, When the user returns with one set, Then the login opens`() =
        coroutineRule.runTest {
            // Given
            whenever(interactor.getPinLockoutState()).thenReturn(PinLockoutState.Idle)
            whenever(interactor.getBiometricUserSelection()).thenReturn(false)
            whenever(interactor.getDeviceLockNotice())
                .thenReturn(DeviceLockNotice.Blocking(DEVICE_LOCK_REQUIRED), null)

            // When
            viewModel.handleEvents(Event.Init)
            coroutineRule.testScope.runCurrent()
            viewModel.handleEvents(Event.OnResume)

            // Then
            assertNull(viewModel.viewState.value.deviceLockNotice)
            assertFalse(viewModel.viewState.value.isBlockedByDeviceLock)

            // When
            val pin = enterPin("123456", QuickPinInteractorPinValidPartialState.Success)

            // Then
            verify(interactor).isPinValid(pin)
        }

    @Test
    fun `Given a confirmation before a presentation, When the screen resumes, Then the screen lock is not checked`() {
        // Given
        val confirmation = newViewModel(
            "confirmation",
            BiometricUiConfig(
                mode = BiometricMode.Default("Biometrics", "PIN", "Enter PIN"),
                onSuccessNavigation = ConfigNavigation(NavigationType.Pop),
                onBackNavigationConfig = OnBackNavigationConfig(null, false)
            )
        )
        whenever(interactor.getDeviceLockNotice())
            .thenReturn(DeviceLockNotice.Blocking(DEVICE_LOCK_REQUIRED))

        // When
        confirmation.handleEvents(Event.OnResume)

        // Then
        verify(interactor, never()).getDeviceLockNotice()
        assertFalse(confirmation.viewState.value.isBlockedByDeviceLock)
    }

    private fun completeAuthentication(result: BiometricsAuthenticate) {
        callbacks.single().invoke(result)
    }

    private fun assertSuccessNavigation(
        key: String,
        navigation: ConfigNavigation,
        expected: Effect,
    ) = coroutineRule.runTest {
        val vm = newViewModel(key, config(onSuccessNavigation = navigation))

        vm.effect.runFlowTest {
            vm.handleEvents(Event.OnBiometricsClicked(context, true))
            callbacks.last().invoke(BiometricsAuthenticate.Success)
            coroutineRule.testScope.runCurrent()

            assertEquals(expected, awaitItem())
        }
    }

    private suspend fun lockViewModelOut(remaining: Duration = 30.seconds) {
        whenever(interactor.getBiometricUserSelection()).thenReturn(false)
        whenever(interactor.getPinLockoutState())
            .thenReturn(lockout(remaining), PinLockoutState.Idle)

        viewModel.handleEvents(Event.Init)
        coroutineRule.testScope.runCurrent()
    }

    private fun enterPin(
        pin: String,
        result: QuickPinInteractorPinValidPartialState
    ): SecurePinImpl {
        val securePin = SecurePinImpl(pin)
        whenever(interactor.isPinValid(securePin)).thenReturn(flowOf(result))

        viewModel.handleEvents(Event.OnQuickPinEntered(securePin))
        coroutineRule.testScope.runCurrent()
        return securePin
    }

    private fun lockout(remaining: Duration) =
        PinLockoutState.Active(remaining = remaining, total = 30.seconds)

    private fun config(
        shouldInitializeBiometricAuthOnCreate: Boolean = true,
        isPreAuthorization: Boolean = false,
        onSuccessNavigation: ConfigNavigation = ConfigNavigation(NavigationType.Pop),
        onBackNavigation: ConfigNavigation? = null,
    ) = BiometricUiConfig(
        mode = BiometricMode.Login("Welcome", "Biometrics", "PIN"),
        isPreAuthorization = isPreAuthorization,
        shouldInitializeBiometricAuthOnCreate = shouldInitializeBiometricAuthOnCreate,
        onSuccessNavigation = onSuccessNavigation,
        onBackNavigationConfig = OnBackNavigationConfig(
            onBackNavigation = onBackNavigation,
            hasToolbarBackIcon = onBackNavigation != null
        )
    )

    private fun newViewModel(key: String, config: BiometricUiConfig): BiometricViewModel {
        whenever(
            serializer.fromBase64(key, BiometricUiConfig::class.java, BiometricUiConfig.Parser)
        ).thenReturn(config)
        return BiometricViewModel(
            biometricInteractor = interactor,
            resourceProvider = resources,
            uiSerializer = serializer,
            biometricConfig = key
        ).also { store.put(key, it) }
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
        const val DEEPLINK = "https://example.org/deeplink"
        const val DEVICE_LOCK_WARNING = "This device has no screen lock."
        const val DEVICE_LOCK_REQUIRED = "Set a screen lock to use the wallet."
    }
}