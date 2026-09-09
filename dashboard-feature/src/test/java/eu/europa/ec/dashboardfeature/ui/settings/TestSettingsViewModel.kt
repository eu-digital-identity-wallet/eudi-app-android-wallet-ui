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

package eu.europa.ec.dashboardfeature.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModelStore
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAuthenticate
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAvailability
import eu.europa.ec.dashboardfeature.interactor.SettingsInteractor
import eu.europa.ec.dashboardfeature.ui.settings.model.SettingsItemUi
import eu.europa.ec.dashboardfeature.ui.settings.model.SettingsMenuItemType
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TestSettingsViewModel {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var interactor: SettingsInteractor

    @Mock
    private lateinit var resources: ResourceProvider

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var settingsItem: SettingsItemUi

    private val store = ViewModelStore()
    private val callbacks = mutableListOf<(BiometricsAuthenticate) -> Unit>()

    private lateinit var viewModel: SettingsViewModel
    private lateinit var click: Event.ItemClicked

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        click = Event.ItemClicked(SettingsMenuItemType.BIOMETRICS_AUTHENTICATION, context)

        whenever(resources.getString(any())).thenReturn("Settings")
        whenever(interactor.getAppVersion()).thenReturn("test")
        whenever(interactor.getBiometricsAvailability())
            .thenReturn(BiometricsAvailability.CanAuthenticate)

        doAnswer {
            callbacks += it.getArgument<(BiometricsAuthenticate) -> Unit>(2)
        }.whenever(interactor).authenticateWithBiometrics(any(), any(), any())

        viewModel = SettingsViewModel(
            settingsInteractor = interactor,
            resourceProvider = resources
        )
        store.put("settings", viewModel)
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
    fun `rapid taps open one authentication attempt`() = coroutineRule.runTest {
        repeat(30) { viewModel.handleEvents(click) }

        assertEquals(1, callbacks.size)
        verify(interactor).authenticateWithBiometrics(eq(context), eq(false), any())
        verify(interactor, times(1)).getBiometricsAvailability()
        verify(interactor, never()).toggleBiometricsAuthentication()
    }

    @Test
    fun `success saves and refreshes once while further taps wait for persistence`() =
        coroutineRule.runTest {
            val saveFinished = CompletableDeferred<Unit>()
            val updatedItems = listOf(settingsItem)

            whenever(interactor.toggleBiometricsAuthentication()).doSuspendableAnswer {
                saveFinished.await()
            }
            whenever(interactor.getSettingsItemsUi(null)).thenReturn(updatedItems)

            viewModel.handleEvents(click)
            completeAuthentication(BiometricsAuthenticate.Success)

            // Covers both before the persistence coroutine starts and while it suspends.
            viewModel.handleEvents(click)
            coroutineRule.testScope.runCurrent()
            repeat(30) { viewModel.handleEvents(click) }

            assertEquals(1, callbacks.size)

            saveFinished.complete(Unit)
            coroutineRule.testScope.runCurrent()

            verify(interactor, times(1)).toggleBiometricsAuthentication()
            assertEquals(updatedItems, viewModel.viewState.value.settingsItems)

            viewModel.handleEvents(click)

            assertEquals(2, callbacks.size)
        }

    @Test
    fun `failure shows the reason preserves settings and permits another attempt`() =
        coroutineRule.runTest {
            val originalItems = listOf(settingsItem)
            whenever(interactor.getSettingsItemsUi(null)).thenReturn(originalItems)

            viewModel.handleEvents(Event.Init)
            coroutineRule.testScope.runCurrent()

            viewModel.effect.runFlowTest {
                viewModel.handleEvents(click)
                completeAuthentication(BiometricsAuthenticate.Failed("Sensor locked"))

                assertEquals(Effect.ShowSnackbar("Sensor locked"), awaitItem())
                assertEquals(originalItems, viewModel.viewState.value.settingsItems)
                verify(interactor, never()).toggleBiometricsAuthentication()

                viewModel.handleEvents(click)

                assertEquals(2, callbacks.size)
                expectNoEvents()
            }
        }

    @Test
    fun `cancellation preserves settings without snackbar and permits retry`() =
        coroutineRule.runTest {
            viewModel.effect.runFlowTest {
                viewModel.handleEvents(click)
                completeAuthentication(BiometricsAuthenticate.Cancelled)
                coroutineRule.testScope.runCurrent()

                verify(interactor, never()).toggleBiometricsAuthentication()
                expectNoEvents()

                viewModel.handleEvents(click)

                assertEquals(2, callbacks.size)
            }
        }

    @Test
    fun `unsuitable biometrics show explanation without starting authentication`() =
        coroutineRule.runTest {
            whenever(interactor.getBiometricsAvailability())
                .thenReturn(BiometricsAvailability.Failure("Set up a supported biometric"))

            viewModel.effect.runFlowTest {
                viewModel.handleEvents(click)

                assertEquals(Effect.ShowSnackbar("Set up a supported biometric"), awaitItem())
                assertEquals(0, callbacks.size)
            }
        }

    @Test
    fun `no enrollment opens system setup without authenticating`() = coroutineRule.runTest {
        whenever(interactor.getBiometricsAvailability())
            .thenReturn(BiometricsAvailability.NonEnrolled)

        viewModel.effect.runFlowTest {
            viewModel.handleEvents(click)

            assertEquals(Effect.Navigation.LaunchBiometricsSystemScreen, awaitItem())
            assertEquals(0, callbacks.size)
        }
    }

    private fun completeAuthentication(result: BiometricsAuthenticate) {
        callbacks.single().invoke(result)
    }
}