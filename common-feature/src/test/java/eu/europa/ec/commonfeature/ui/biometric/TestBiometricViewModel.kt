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
        whenever(resources.getString(eq(R.string.quick_pin_locked_out), any(), any()))
            .thenAnswer { invocation ->
                val args = invocation.arguments.drop(1).flatMap {
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
    fun `rapid taps open one prompt and cancellation permits retry`() {
        repeat(30) { viewModel.handleEvents(click) }

        assertEquals(1, callbacks.size)
        verify(interactor).authenticateWithBiometrics(eq(context), eq(false), any())

        completeAuthentication(BiometricsAuthenticate.Cancelled)

        assertNull(viewModel.viewState.value.error)

        viewModel.handleEvents(click)

        assertEquals(2, callbacks.size)
    }

    @Test
    fun `failure is visible and dismissible and retry remains available`() {
        viewModel.handleEvents(click)
        completeAuthentication(BiometricsAuthenticate.Failed("Locked sensor"))

        assertEquals("Locked sensor", viewModel.viewState.value.error?.errorSubTitle)

        viewModel.handleEvents(Event.OnErrorDismiss)

        assertNull(viewModel.viewState.value.error)

        viewModel.handleEvents(click)

        assertEquals(2, callbacks.size)
    }

    @Test
    fun `unavailable automatic biometrics leaves PIN available without an error overlay`() {
        whenever(interactor.getBiometricsAvailability())
            .thenReturn(BiometricsAvailability.Failure("Unsupported biometrics"))

        viewModel.handleEvents(click.copy(shouldThrowErrorIfNotAvailable = false))

        assertNull(viewModel.viewState.value.error)
        assertFalse(viewModel.viewState.value.isLoading)
        assertEquals(0, callbacks.size)
    }

    @Test
    fun `manual attempt explains unsupported biometrics`() {
        whenever(interactor.getBiometricsAvailability())
            .thenReturn(BiometricsAvailability.Failure("Unsupported biometrics"))

        viewModel.handleEvents(click)

        assertEquals("Unsupported biometrics", viewModel.viewState.value.error?.errorSubTitle)
        assertEquals(0, callbacks.size)
    }

    @Test
    fun `success resets PIN throttle and navigates once despite rapid taps`() =
        coroutineRule.runTest {
            viewModel.effect.runFlowTest {
                viewModel.handleEvents(click)
                completeAuthentication(BiometricsAuthenticate.Success)
                repeat(30) { viewModel.handleEvents(click) }
                coroutineRule.testScope.runCurrent()

                assertEquals(1, callbacks.size)
                assertEquals(Effect.Navigation.Pop, awaitItem())
                verify(interactor, times(1)).resetPinThrottle()
                expectNoEvents()
            }
        }

    @Test
    fun `PIN fallback still succeeds after biometric cancellation`() = coroutineRule.runTest {
        val pin = SecurePinImpl("123456")
        whenever(interactor.isPinValid(pin))
            .thenReturn(flowOf(QuickPinInteractorPinValidPartialState.Success))

        viewModel.effect.runFlowTest {
            viewModel.handleEvents(click)
            completeAuthentication(BiometricsAuthenticate.Cancelled)
            viewModel.handleEvents(Event.OnQuickPinEntered(pin))

            assertEquals(Effect.Navigation.Pop, awaitItem())
            verify(interactor).resetPinThrottle()
        }

        pin.close()
    }

    @Test
    fun `a manual attempt without enrollment opens the system setup screen`() =
        coroutineRule.runTest {
            whenever(interactor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.NonEnrolled)

            viewModel.effect.runFlowTest {
                viewModel.handleEvents(click)

                assertEquals(Effect.Navigation.LaunchBiometricsSystemScreen, awaitItem())
                assertEquals(0, callbacks.size)
            }
        }

    @Test
    fun `an automatic attempt without enrollment stays silent`() = coroutineRule.runTest {
        whenever(interactor.getBiometricsAvailability())
            .thenReturn(BiometricsAvailability.NonEnrolled)

        viewModel.effect.runFlowTest {
            viewModel.handleEvents(click.copy(shouldThrowErrorIfNotAvailable = false))
            coroutineRule.testScope.runCurrent()

            assertNull(viewModel.viewState.value.error)
            assertEquals(0, callbacks.size)
            expectNoEvents()
        }
    }

    @Test
    fun `an unsupported biometrics explanation can be dismissed from its own action`() =
        coroutineRule.runTest {
            whenever(interactor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.Failure("Unsupported biometrics"))

            viewModel.handleEvents(click)
            assertNotNull(viewModel.viewState.value.error)

            viewModel.viewState.value.error?.onCancel?.invoke()
            coroutineRule.testScope.runCurrent()

            assertNull(viewModel.viewState.value.error)
        }

    @Test
    fun `a new lockout replaces the tick already running`() = coroutineRule.runTest {
        lockViewModelOut(30.seconds)
        assertEquals("locked:$MAX_ATTEMPTS:00:30", viewModel.viewState.value.lockoutMessage)

        whenever(interactor.getPinLockoutState()).thenReturn(lockout(90.seconds))
        viewModel.handleEvents(Event.Init)
        coroutineRule.testScope.runCurrent()

        assertEquals("locked:$MAX_ATTEMPTS:01:30", viewModel.viewState.value.lockoutMessage)

        coroutineRule.testScope.advanceTimeBy(1_500.milliseconds)
        coroutineRule.testScope.runCurrent()

        assertEquals("locked:$MAX_ATTEMPTS:01:29", viewModel.viewState.value.lockoutMessage)

        coroutineRule.testScope.advanceTimeBy(30_000.milliseconds)

        assertTrue(viewModel.viewState.value.isLockedOut)
        assertEquals("locked:$MAX_ATTEMPTS:00:59", viewModel.viewState.value.lockoutMessage)
    }

    @Test
    fun `init publishes the biometric preference and asks for the prompt`() =
        coroutineRule.runTest {
            whenever(interactor.getBiometricUserSelection()).thenReturn(true)
            whenever(interactor.getPinLockoutState()).thenReturn(PinLockoutState.Idle)

            viewModel.effect.runFlowTest {
                viewModel.handleEvents(Event.Init)
                coroutineRule.testScope.runCurrent()

                assertTrue(viewModel.viewState.value.userBiometricsAreEnabled)
                assertEquals(Effect.InitializeBiometricAuthOnCreate, awaitItem())
            }
        }

    @Test
    fun `init keeps the prompt closed when biometrics are disabled`() = coroutineRule.runTest {
        whenever(interactor.getBiometricUserSelection()).thenReturn(false)
        whenever(interactor.getPinLockoutState()).thenReturn(PinLockoutState.Idle)

        viewModel.effect.runFlowTest {
            viewModel.handleEvents(Event.Init)
            coroutineRule.testScope.runCurrent()

            assertFalse(viewModel.viewState.value.userBiometricsAreEnabled)
            expectNoEvents()
        }
    }

    @Test
    fun `init keeps the prompt closed when the screen opts out`() = coroutineRule.runTest {
        whenever(interactor.getBiometricUserSelection()).thenReturn(true)
        whenever(interactor.getPinLockoutState()).thenReturn(PinLockoutState.Idle)
        val optedOut = newViewModel(
            "optedOut",
            config(shouldInitializeBiometricAuthOnCreate = false)
        )

        optedOut.effect.runFlowTest {
            optedOut.handleEvents(Event.Init)
            coroutineRule.testScope.runCurrent()

            assertTrue(optedOut.viewState.value.userBiometricsAreEnabled)
            expectNoEvents()
        }
    }

    @Test
    fun `init restores an active lockout and counts it down to release`() = coroutineRule.runTest {
        whenever(interactor.getBiometricUserSelection()).thenReturn(false)
        whenever(interactor.getPinLockoutState()).thenReturn(lockout(3.seconds))

        viewModel.handleEvents(Event.Init)
        coroutineRule.testScope.runCurrent()

        assertTrue(viewModel.viewState.value.isLockedOut)
        assertEquals("locked:$MAX_ATTEMPTS:00:03", viewModel.viewState.value.lockoutMessage)

        coroutineRule.testScope.advanceTimeBy(1_500.milliseconds)
        coroutineRule.testScope.runCurrent()

        assertEquals("locked:$MAX_ATTEMPTS:00:02", viewModel.viewState.value.lockoutMessage)

        coroutineRule.testScope.advanceUntilIdle()

        assertFalse(viewModel.viewState.value.isLockedOut)
        assertNull(viewModel.viewState.value.lockoutMessage)
    }

    @Test
    fun `init releases a lockout that has already expired`() = coroutineRule.runTest {
        whenever(interactor.getBiometricUserSelection()).thenReturn(false)
        whenever(interactor.getPinLockoutState()).thenReturn(lockout(Duration.ZERO))

        viewModel.handleEvents(Event.Init)
        coroutineRule.testScope.runCurrent()

        assertFalse(viewModel.viewState.value.isLockedOut)
        assertNull(viewModel.viewState.value.lockoutMessage)
    }

    @Test
    fun `launching the system screen clears the error and delegates to the interactor`() =
        coroutineRule.runTest {
            viewModel.handleEvents(click)
            completeAuthentication(BiometricsAuthenticate.Failed("Locked sensor"))
            assertNotNull(viewModel.viewState.value.error)

            viewModel.handleEvents(Event.LaunchBiometricSystemScreen)

            assertNull(viewModel.viewState.value.error)
            verify(interactor).launchBiometricSystemScreen()
        }

    @Test
    fun `back navigation reports the flow as cancelled`() = coroutineRule.runTest {
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
            backable.handleEvents(Event.OnNavigateBack)

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
    fun `back navigation does not report success completion for a cancelled flow`() =
        coroutineRule.runTest {
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
                backable.handleEvents(Event.OnNavigateBack)

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
    fun `back navigation emits nothing when no route is configured`() = coroutineRule.runTest {
        viewModel.effect.runFlowTest {
            viewModel.handleEvents(Event.OnNavigateBack)
            coroutineRule.testScope.runCurrent()

            assertNull(viewModel.viewState.value.error)
            expectNoEvents()
        }
    }

    @Test
    @Suppress("UnusedFlow")
    fun `pin entry is discarded while locked out`() = coroutineRule.runTest {
        lockViewModelOut()
        val pin = SecurePinImpl("123456")

        viewModel.handleEvents(Event.OnQuickPinEntered(pin))

        assertTrue(pin.isCleared)
        verify(interactor, never()).isPinValid(any())
    }

    @Test
    fun `pin length changes are ignored while locked out`() = coroutineRule.runTest {
        lockViewModelOut()

        viewModel.handleEvents(Event.OnQuickPinLengthChanged(3))

        assertTrue(viewModel.viewState.value.isLockedOut)
        assertEquals("locked:$MAX_ATTEMPTS:00:30", viewModel.viewState.value.lockoutMessage)
    }

    @Test
    fun `changing the pin length clears the previous pin error`() = coroutineRule.runTest {
        whenever(interactor.recordPinFailure()).thenReturn(PinLockoutState.Idle)

        val pin = enterPin("123456", QuickPinInteractorPinValidPartialState.Failed("Wrong PIN"))
        assertEquals("Wrong PIN", viewModel.viewState.value.quickPinError)

        viewModel.handleEvents(Event.OnQuickPinLengthChanged(3))

        assertNull(viewModel.viewState.value.quickPinError)

        pin.close()
    }

    @Test
    @Suppress("UnusedFlow")
    fun `an incomplete pin is discarded without validation`() = coroutineRule.runTest {
        val pin = SecurePinImpl("123")

        viewModel.handleEvents(Event.OnQuickPinEntered(pin))

        assertTrue(pin.isCleared)
        assertFalse(viewModel.viewState.value.isLoading)
        verify(interactor, never()).isPinValid(any())
    }

    @Test
    fun `a wrong pin surfaces the error while attempts remain`() = coroutineRule.runTest {
        whenever(interactor.recordPinFailure()).thenReturn(PinLockoutState.Idle)

        val pin = enterPin("123456", QuickPinInteractorPinValidPartialState.Failed("Wrong PIN"))

        assertEquals("Wrong PIN", viewModel.viewState.value.quickPinError)
        assertFalse(viewModel.viewState.value.isLoading)
        assertFalse(viewModel.viewState.value.isLockedOut)

        pin.close()
    }

    @Test
    fun `a wrong pin that exhausts the attempts starts the lockout`() = coroutineRule.runTest {
        whenever(interactor.recordPinFailure()).thenReturn(lockout(30.seconds))

        val pin = enterPin("123456", QuickPinInteractorPinValidPartialState.Failed("Wrong PIN"))

        assertTrue(viewModel.viewState.value.isLockedOut)
        assertFalse(viewModel.viewState.value.isLoading)
        assertEquals("locked:$MAX_ATTEMPTS:00:30", viewModel.viewState.value.lockoutMessage)

        pin.close()
    }

    @Test
    fun `clearing the view model freezes the lockout state`() = coroutineRule.runTest {
        lockViewModelOut()

        store.clear()
        coroutineRule.testScope.advanceUntilIdle()

        assertTrue(viewModel.viewState.value.isLockedOut)
        assertEquals("locked:$MAX_ATTEMPTS:00:30", viewModel.viewState.value.lockoutMessage)
    }

    @Test
    fun `a successful authentication releases the re-entrancy guard`() = coroutineRule.runTest {
        viewModel.effect.runFlowTest {
            viewModel.handleEvents(click)
            completeAuthentication(BiometricsAuthenticate.Success)
            coroutineRule.testScope.runCurrent()

            assertEquals(Effect.Navigation.Pop, awaitItem())

            viewModel.handleEvents(click)

            assertEquals(2, callbacks.size)
        }
    }

    @Test
    fun `a biometric failure can be dismissed from its own action`() = coroutineRule.runTest {
        viewModel.handleEvents(click)
        completeAuthentication(BiometricsAuthenticate.Failed("Locked sensor"))

        assertNotNull(viewModel.viewState.value.error)

        viewModel.viewState.value.error?.onCancel?.invoke()
        coroutineRule.testScope.runCurrent()

        assertNull(viewModel.viewState.value.error)
    }

    @Test
    fun `an invalid configuration is rejected`() {
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

        assertThrows(RuntimeException::class.java) { broken.viewState.value }
    }

    @Test
    fun `success reports completion for a pop-to route`() = assertSuccessNavigation(
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

    @Test
    fun `success does not report cancellation for a pop-to route`() = assertSuccessNavigation(
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

    @Test
    fun `success reports no completion when none is configured`() = assertSuccessNavigation(
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

    @Test
    fun `success pushes the configured screen with its arguments`() = assertSuccessNavigation(
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

    @Test
    fun `success pushes the configured raw route`() = assertSuccessNavigation(
        key = "pushRoute",
        navigation = ConfigNavigation(navigationType = NavigationType.PushRoute(route = "RAW_ROUTE")),
        expected = Effect.Navigation.SwitchScreen(
            screen = "RAW_ROUTE",
            screenPopUpTo = CommonScreens.Biometric.screenRoute
        )
    )

    @Test
    fun `success finishes the host when configured`() = assertSuccessNavigation(
        key = "finish",
        navigation = ConfigNavigation(navigationType = NavigationType.Finish),
        expected = Effect.Navigation.Finish
    )

    @Test
    fun `success forwards a deeplink together with the pre authorization flag`() =
        coroutineRule.runTest {
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
                vm.handleEvents(Event.OnBiometricsClicked(context, true))
                callbacks.last().invoke(BiometricsAuthenticate.Success)
                coroutineRule.testScope.runCurrent()

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
        whenever(interactor.getPinLockoutState()).thenReturn(lockout(remaining))

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
    }
}