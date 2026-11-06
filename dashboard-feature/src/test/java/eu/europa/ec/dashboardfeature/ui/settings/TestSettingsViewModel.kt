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
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelStore
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAuthenticate
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAvailability
import eu.europa.ec.commonfeature.config.BiometricUiConfig
import eu.europa.ec.dashboardfeature.interactor.SettingsInteractor
import eu.europa.ec.dashboardfeature.ui.settings.model.SettingsItemUi
import eu.europa.ec.dashboardfeature.ui.settings.model.SettingsMenuItemType
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.config.FlowCompletion
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
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
    private lateinit var uiSerializer: UiSerializer

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
        whenever(resources.getString(R.string.settings_screen_option_registration_check_restart))
            .thenReturn(RESTART_STRING)
        whenever(resources.getString(R.string.settings_intent_chooser_logs_share_title))
            .thenReturn(LOGS_CHOOSER_TITLE)
        whenever(interactor.getAppVersion()).thenReturn("test")
        whenever(interactor.getBiometricsAvailability())
            .thenReturn(BiometricsAvailability.CanAuthenticate)

        doAnswer {
            callbacks += it.getArgument<(BiometricsAuthenticate) -> Unit>(2)
        }.whenever(interactor).authenticateWithBiometrics(any(), any(), any())

        viewModel = SettingsViewModel(
            settingsInteractor = interactor,
            resourceProvider = resources,
            uiSerializer = uiSerializer,
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
    fun `Given biometric unlock off, When the check succeeds 30 times, Then one biometric prompt opens`() =
        coroutineRule.runTest {
            repeat(30) { confirmBiometricSwitch() }

            assertEquals(1, callbacks.size)
            verify(interactor).authenticateWithBiometrics(eq(context), eq(false), any())
            verify(interactor, never()).toggleBiometricsAuthentication()
        }

    @Test
    fun `Given biometric unlock available, When the switch is tapped, Then the PIN or biometric check opens first`() =
        coroutineRule.runTest {
            val config = argumentCaptor<BiometricUiConfig>()
            whenever(uiSerializer.toBase64(config.capture(), any())).thenReturn(SERIALIZED_CONFIG)

            viewModel.effect.runFlowTest {
                viewModel.handleEvents(click)

                assertEquals(
                    Effect.Navigation.SwitchScreen(
                        screenRoute = "${CommonScreens.Biometric.screenName}?" +
                                "${BiometricUiConfig.serializedKeyName}=$SERIALIZED_CONFIG"
                    ),
                    awaitItem()
                )
            }

            assertEquals(0, callbacks.size)
            verify(interactor, never()).toggleBiometricsAuthentication()
            assertEquals(
                NavigationType.PopTo(DashboardScreens.Settings),
                config.firstValue.onSuccessNavigation.navigationType
            )
            assertEquals(
                FlowCompletion.SUCCESS,
                config.firstValue.onSuccessNavigation.indicateFlowCompletion
            )
            assertEquals(
                NavigationType.PopTo(DashboardScreens.Settings),
                config.firstValue.onBackNavigationConfig.onBackNavigation?.navigationType
            )
            assertEquals(
                FlowCompletion.NONE,
                config.firstValue.onBackNavigationConfig.onBackNavigation?.indicateFlowCompletion
            )
        }

    @Test
    fun `Given biometric unlock on, When the check succeeds, Then it is turned off without another prompt`() =
        coroutineRule.runTest {
            val updatedItems = listOf(settingsItem)
            whenever(interactor.getSettingsItemsUi(null)).thenReturn(updatedItems)

            confirmBiometricSwitch(isBiometricUnlockOn = true)

            verify(interactor, times(1)).toggleBiometricsAuthentication()
            assertEquals(0, callbacks.size)
            assertEquals(updatedItems, viewModel.viewState.value.settingsItems)
        }

    @Test
    fun `Given biometric unlock off, When the check succeeds, Then a biometric is asked for before it is turned on`() =
        coroutineRule.runTest {
            whenever(interactor.getSettingsItemsUi(null)).thenReturn(listOf(settingsItem))

            confirmBiometricSwitch(isBiometricUnlockOn = false)

            assertEquals(1, callbacks.size)
            verify(interactor, never()).toggleBiometricsAuthentication()

            completeAuthentication(BiometricsAuthenticate.Success)
            coroutineRule.testScope.runCurrent()

            verify(interactor, times(1)).toggleBiometricsAuthentication()
        }

    /**
     * Further confirmations arrive both before the persistence coroutine starts and while it
     * suspends.
     */
    @Test
    fun `Given biometric unlock off, When the check succeeds and more confirmations follow, Then it is saved and refreshed once`() =
        coroutineRule.runTest {
            val saveFinished = CompletableDeferred<Unit>()
            val updatedItems = listOf(settingsItem)

            whenever(interactor.toggleBiometricsAuthentication()).doSuspendableAnswer {
                saveFinished.await()
            }
            whenever(interactor.getSettingsItemsUi(null)).thenReturn(updatedItems)

            confirmBiometricSwitch()
            completeAuthentication(BiometricsAuthenticate.Success)

            confirmBiometricSwitch()
            coroutineRule.testScope.runCurrent()
            repeat(30) { confirmBiometricSwitch() }

            assertEquals(1, callbacks.size)

            saveFinished.complete(Unit)
            coroutineRule.testScope.runCurrent()

            verify(interactor, times(1)).toggleBiometricsAuthentication()
            assertEquals(updatedItems, viewModel.viewState.value.settingsItems)

            confirmBiometricSwitch()

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
                confirmBiometricSwitch()
                completeAuthentication(BiometricsAuthenticate.Failed("Sensor locked"))

                assertEquals(Effect.ShowSnackbar("Sensor locked"), awaitItem())
                assertEquals(originalItems, viewModel.viewState.value.settingsItems)
                verify(interactor, never()).toggleBiometricsAuthentication()

                confirmBiometricSwitch()

                assertEquals(2, callbacks.size)
                expectNoEvents()
            }
        }

    @Test
    fun `cancellation preserves settings without snackbar and permits retry`() =
        coroutineRule.runTest {
            viewModel.effect.runFlowTest {
                confirmBiometricSwitch()
                completeAuthentication(BiometricsAuthenticate.Cancelled)
                coroutineRule.testScope.runCurrent()

                verify(interactor, never()).toggleBiometricsAuthentication()
                expectNoEvents()

                confirmBiometricSwitch()

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

    @Test
    fun `init shows a loading pass before publishing the settings items`() =
        coroutineRule.runTest {
            val items = listOf(settingsItem)
            val released = CompletableDeferred<Unit>()
            whenever(interactor.getSettingsItemsUi(null)).doSuspendableAnswer {
                released.await()
                items
            }

            viewModel.handleEvents(Event.Init)
            coroutineRule.testScope.runCurrent()

            assertTrue(viewModel.viewState.value.isLoading)
            assertEquals(emptyList<SettingsItemUi>(), viewModel.viewState.value.settingsItems)

            released.complete(Unit)
            coroutineRule.testScope.runCurrent()

            assertFalse(viewModel.viewState.value.isLoading)
            assertEquals(items, viewModel.viewState.value.settingsItems)
        }

    @Test
    fun `pop requests navigation back`() = coroutineRule.runTest {
        viewModel.effect.runFlowTest {
            viewModel.handleEvents(Event.Pop)

            assertEquals(Effect.Navigation.Pop, awaitItem())
        }
    }

    @Test
    fun `launch biometric system screen delegates to the interactor`() = coroutineRule.runTest {
        viewModel.handleEvents(Event.LaunchBiometricSystemScreen)

        verify(interactor).launchBiometricSystemScreen()
        assertEquals(0, callbacks.size)
    }

    @Test
    fun `toggling the batch issuance counter refreshes the settings items`() =
        coroutineRule.runTest {
            val updatedItems = listOf(settingsItem)
            whenever(interactor.getSettingsItemsUi(null)).thenReturn(updatedItems)

            viewModel.handleEvents(
                Event.ItemClicked(SettingsMenuItemType.SHOW_BATCH_ISSUANCE_COUNTER, context)
            )
            coroutineRule.testScope.runCurrent()

            verify(interactor).toggleShowBatchIssuanceCounter()
            assertEquals(updatedItems, viewModel.viewState.value.settingsItems)
            verify(interactor, never()).authenticateWithBiometrics(any(), any(), any())
        }

    @Test
    fun `toggling the Digital Credential API applies the setting and refreshes the items`() =
        coroutineRule.runTest {
            val updatedItems = listOf(settingsItem)
            whenever(interactor.getSettingsItemsUi(null)).thenReturn(updatedItems)

            viewModel.handleEvents(
                Event.ItemClicked(SettingsMenuItemType.DC_API, context)
            )
            coroutineRule.testScope.runCurrent()

            verify(interactor).toggleDCAPI()
            assertEquals(updatedItems, viewModel.viewState.value.settingsItems)
            verify(interactor, never()).authenticateWithBiometrics(any(), any(), any())
        }

    @Test
    fun `toggling the registration check refreshes the items and asks for a restart`() =
        coroutineRule.runTest {
            val updatedItems = listOf(settingsItem)
            whenever(interactor.getSettingsItemsUi(null)).thenReturn(updatedItems)

            viewModel.effect.runFlowTest {
                viewModel.handleEvents(
                    Event.ItemClicked(SettingsMenuItemType.REGISTRATION_CHECK, context)
                )
                coroutineRule.testScope.runCurrent()

                verify(interactor).toggleRegistrationCheck()
                assertEquals(updatedItems, viewModel.viewState.value.settingsItems)
                assertEquals(Effect.ShowSnackbar(RESTART_STRING), awaitItem())
            }
        }

    @Test
    fun `retrieving logs shares every log file`() = coroutineRule.runTest {
        val logs = arrayListOf("content://logs/1".toUri(), "content://logs/2".toUri())
        whenever(interactor.retrieveLogFileUris()).thenReturn(logs)

        viewModel.effect.runFlowTest {
            viewModel.handleEvents(Event.ItemClicked(SettingsMenuItemType.RETRIEVE_LOGS, context))

            val effect = awaitItem()
            assertTrue(effect is Effect.ShareLogFile)
            val shareLogFile = effect as Effect.ShareLogFile
            assertEquals(Intent.ACTION_SEND_MULTIPLE, shareLogFile.intent.action)
            assertEquals("text/*", shareLogFile.intent.type)
            assertEquals(LOGS_CHOOSER_TITLE, shareLogFile.chooserTitle)
            assertEquals(
                logs,
                IntentCompat.getParcelableArrayListExtra(
                    shareLogFile.intent,
                    Intent.EXTRA_STREAM,
                    Uri::class.java
                )
            )
        }
    }

    @Test
    fun `retrieving logs stays silent when there is nothing to share`() = coroutineRule.runTest {
        whenever(interactor.retrieveLogFileUris()).thenReturn(arrayListOf())

        viewModel.effect.runFlowTest {
            viewModel.handleEvents(Event.ItemClicked(SettingsMenuItemType.RETRIEVE_LOGS, context))
            coroutineRule.testScope.runCurrent()

            expectNoEvents()
        }
    }

    @Test
    fun `changelog opens the configured url externally`() = coroutineRule.runTest {
        whenever(interactor.getChangelogUrl()).thenReturn(CHANGELOG_URL)
        val withChangelog = newViewModel("withChangelog")

        withChangelog.effect.runFlowTest {
            withChangelog.handleEvents(Event.ItemClicked(SettingsMenuItemType.CHANGELOG, context))

            assertEquals(
                Effect.Navigation.OpenUrlExternally(CHANGELOG_URL.toUri()),
                awaitItem()
            )
        }
    }

    @Test
    fun `changelog stays silent when no url is configured`() = coroutineRule.runTest {
        whenever(interactor.getChangelogUrl()).thenReturn(null)
        val withoutChangelog = newViewModel("withoutChangelog")

        withoutChangelog.effect.runFlowTest {
            withoutChangelog.handleEvents(
                Event.ItemClicked(SettingsMenuItemType.CHANGELOG, context)
            )
            coroutineRule.testScope.runCurrent()

            expectNoEvents()
        }
    }

    @Test
    fun `reset wallet asks for confirmation first`() = coroutineRule.runTest {
        viewModel.effect.runFlowTest {
            viewModel.handleEvents(Event.ItemClicked(SettingsMenuItemType.RESET_WALLET, context))

            assertEquals(Effect.ShowBottomSheet, awaitItem())
            expectNoEvents()
        }
        verify(uiSerializer, never()).toBase64(any<BiometricUiConfig>(), any())
    }

    @Test
    fun `the confirmation sheet state follows the screen`() = coroutineRule.runTest {
        viewModel.handleEvents(Event.BottomSheet.UpdateBottomSheetState(isOpen = true))
        assertTrue(viewModel.viewState.value.isBottomSheetOpen)

        viewModel.handleEvents(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
        assertFalse(viewModel.viewState.value.isBottomSheetOpen)
    }

    @Test
    fun `confirming the reset asks for the PIN, which leads to the reset screen`() =
        coroutineRule.runTest {
            val config = argumentCaptor<BiometricUiConfig>()
            whenever(uiSerializer.toBase64(config.capture(), any())).thenReturn(SERIALIZED_CONFIG)

            viewModel.effect.runFlowTest {
                viewModel.handleEvents(Event.BottomSheet.ResetWallet.PrimaryButtonPressed)

                assertEquals(Effect.CloseBottomSheet, awaitItem())
                expectNoEvents()

                viewModel.handleEvents(Event.BottomSheet.FinishedClosing)
                assertEquals(
                    Effect.Navigation.SwitchScreen(
                        screenRoute = "${CommonScreens.Biometric.screenName}?${BiometricUiConfig.serializedKeyName}=$SERIALIZED_CONFIG"
                    ),
                    awaitItem()
                )
            }

            assertFalse(viewModel.viewState.value.resetAfterSheetCloses)
            assertFalse(config.firstValue.isPreAuthorization)
            assertEquals(
                NavigationType.PushScreen(screen = DashboardScreens.ResetWallet),
                config.firstValue.onSuccessNavigation.navigationType
            )
            assertEquals(
                NavigationType.PopTo(DashboardScreens.Settings),
                config.firstValue.onBackNavigationConfig.onBackNavigation?.navigationType
            )
        }

    @Test
    fun `cancelling the reset only closes the sheet`() = coroutineRule.runTest {
        viewModel.effect.runFlowTest {
            viewModel.handleEvents(Event.BottomSheet.ResetWallet.SecondaryButtonPressed)

            assertEquals(Effect.CloseBottomSheet, awaitItem())
            viewModel.handleEvents(Event.BottomSheet.FinishedClosing)
            expectNoEvents()
        }
        verify(uiSerializer, never()).toBase64(any<BiometricUiConfig>(), any())
    }

    private fun completeAuthentication(result: BiometricsAuthenticate) {
        callbacks.single().invoke(result)
    }

    /**
     * Sends what Settings receives once the PIN or biometric check before the switch has
     * succeeded, and runs the work it starts.
     *
     * @param isBiometricUnlockOn Whether biometric unlock is on before the switch.
     */
    private suspend fun confirmBiometricSwitch(isBiometricUnlockOn: Boolean = false) {
        whenever(interactor.getBiometricUserSelection()).thenReturn(isBiometricUnlockOn)
        viewModel.handleEvents(Event.OnBiometricSwitchAuthenticated(context))
        coroutineRule.testScope.runCurrent()
    }

    private fun newViewModel(key: String): SettingsViewModel = SettingsViewModel(
        settingsInteractor = interactor,
        resourceProvider = resources,
        uiSerializer = uiSerializer,
    ).also { store.put(key, it) }

    private companion object {
        const val SETTINGS_STRING = "Settings"
        const val RESTART_STRING = "Restart required"
        const val LOGS_CHOOSER_TITLE = "Share logs"
        const val CHANGELOG_URL = "https://example.org/changelog"
        const val SERIALIZED_CONFIG = "serializedConfig"
    }
}