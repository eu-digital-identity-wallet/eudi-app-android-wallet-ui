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
import eu.europa.ec.authenticationlogic.secure.SecurePinImpl
import eu.europa.ec.commonfeature.config.BiometricMode
import eu.europa.ec.commonfeature.config.BiometricUiConfig
import eu.europa.ec.commonfeature.config.OnBackNavigationConfig
import eu.europa.ec.commonfeature.interactor.BiometricInteractor
import eu.europa.ec.commonfeature.interactor.QuickPinInteractorPinValidPartialState
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

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
        whenever(interactor.getBiometricsAvailabilityForCrypto())
            .thenReturn(BiometricsAvailability.CanAuthenticate)

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
        whenever(interactor.getBiometricsAvailabilityForCrypto())
            .thenReturn(BiometricsAvailability.Failure("Unsupported biometrics"))

        viewModel.handleEvents(click.copy(shouldThrowErrorIfNotAvailable = false))

        assertNull(viewModel.viewState.value.error)
        assertFalse(viewModel.viewState.value.isLoading)
        assertEquals(0, callbacks.size)
    }

    @Test
    fun `manual attempt explains unsupported biometrics`() {
        whenever(interactor.getBiometricsAvailabilityForCrypto())
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

    private fun completeAuthentication(result: BiometricsAuthenticate) {
        callbacks.single().invoke(result)
    }
}