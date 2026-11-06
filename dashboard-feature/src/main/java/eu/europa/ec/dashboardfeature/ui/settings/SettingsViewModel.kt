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
import androidx.lifecycle.viewModelScope
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAuthenticate
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAvailability
import eu.europa.ec.businesslogic.extension.toUri
import eu.europa.ec.commonfeature.config.BiometricMode
import eu.europa.ec.commonfeature.config.BiometricUiConfig
import eu.europa.ec.commonfeature.config.OnBackNavigationConfig
import eu.europa.ec.dashboardfeature.interactor.SettingsInteractor
import eu.europa.ec.dashboardfeature.ui.settings.model.SettingsItemUi
import eu.europa.ec.dashboardfeature.ui.settings.model.SettingsMenuItemType
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.FlowCompletion
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.launch

data class State(
    val isLoading: Boolean = false,
    val screenTitle: String,

    val settingsItems: List<SettingsItemUi> = emptyList(),

    val appVersion: String = "",
    val changelogUrl: String?,

    val isBottomSheetOpen: Boolean = false,
    val resetAfterSheetCloses: Boolean = false,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object Pop : Event()
    data object LaunchBiometricSystemScreen : Event()
    data class OnBiometricSwitchAuthenticated(val context: Context) : Event()
    data class ItemClicked(
        val itemType: SettingsMenuItemType,
        val context: Context
    ) : Event()

    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(val isOpen: Boolean) : BottomSheet()
        data object FinishedClosing : BottomSheet()

        sealed class ResetWallet : BottomSheet() {
            data object PrimaryButtonPressed : ResetWallet()
            data object SecondaryButtonPressed : ResetWallet()
        }
    }
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
        data object LaunchBiometricsSystemScreen : Navigation()

        data class OpenUrlExternally(val url: Uri) : Navigation()

        data class SwitchScreen(val screenRoute: String) : Navigation()
    }

    data object ShowBottomSheet : Effect()
    data object CloseBottomSheet : Effect()

    data class ShareLogFile(val intent: Intent, val chooserTitle: String) : Effect()
    data class ShowSnackbar(val message: String) : Effect()
}

class SettingsViewModel(
    private val settingsInteractor: SettingsInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
) : MviViewModel<Event, State, Effect>() {

    private var isBiometricAuthenticating: Boolean = false

    override fun setInitialState(): State {
        return State(
            screenTitle = resourceProvider.getString(R.string.settings_screen_title),
            appVersion = settingsInteractor.getAppVersion(),
            changelogUrl = settingsInteractor.getChangelogUrl(),
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                createSettingsItemsUi(viewState.value.changelogUrl)
            }

            is Event.Pop -> setEffect { Effect.Navigation.Pop }

            is Event.LaunchBiometricSystemScreen -> {
                settingsInteractor.launchBiometricSystemScreen()
            }

            is Event.OnBiometricSwitchAuthenticated -> handleBiometricSwitchAuthenticated(
                event.context
            )

            is Event.ItemClicked -> handleSettingsMenuItemClicked(itemType = event.itemType)

            is Event.BottomSheet.UpdateBottomSheetState -> setState {
                copy(isBottomSheetOpen = event.isOpen)
            }

            is Event.BottomSheet.FinishedClosing -> {
                if (viewState.value.resetAfterSheetCloses) {
                    setState { copy(resetAfterSheetCloses = false) }
                    setEffect { Effect.Navigation.SwitchScreen(screenRoute = getResetWalletAuthenticationRoute()) }
                }
            }

            is Event.BottomSheet.ResetWallet.PrimaryButtonPressed -> {
                setState { copy(resetAfterSheetCloses = true) }
                setEffect { Effect.CloseBottomSheet }
            }

            is Event.BottomSheet.ResetWallet.SecondaryButtonPressed -> {
                setState { copy(resetAfterSheetCloses = false) }
                setEffect { Effect.CloseBottomSheet }
            }
        }
    }

    private fun createSettingsItemsUi(changelogUrl: String?) {
        viewModelScope.launch {
            setState {
                copy(
                    isLoading = true,
                )
            }

            val settingsItems = settingsInteractor.getSettingsItemsUi(changelogUrl = changelogUrl)
            setState {
                copy(
                    isLoading = false,
                    settingsItems = settingsItems,
                )
            }
        }
    }

    private fun handleSettingsMenuItemClicked(itemType: SettingsMenuItemType) {
        when (itemType) {
            SettingsMenuItemType.BIOMETRICS_AUTHENTICATION -> {
                if (isBiometricAuthenticating) return
                when (val availability = settingsInteractor.getBiometricsAvailability()) {
                    is BiometricsAvailability.CanAuthenticate -> setEffect {
                        Effect.Navigation.SwitchScreen(
                            screenRoute = getBiometricSwitchAuthenticationRoute()
                        )
                    }

                    is BiometricsAvailability.NonEnrolled -> {
                        setEffect {
                            Effect.Navigation.LaunchBiometricsSystemScreen
                        }
                    }

                    is BiometricsAvailability.Failure -> {
                        setEffect {
                            Effect.ShowSnackbar(availability.errorMessage)
                        }
                    }
                }
            }

            SettingsMenuItemType.SHOW_BATCH_ISSUANCE_COUNTER -> {
                viewModelScope.launch {
                    settingsInteractor.toggleShowBatchIssuanceCounter()

                    val settingsItems = settingsInteractor.getSettingsItemsUi(
                        changelogUrl = viewState.value.changelogUrl
                    )

                    setState {
                        copy(
                            settingsItems = settingsItems,
                        )
                    }
                }
            }

            SettingsMenuItemType.REGISTRATION_CHECK -> {
                viewModelScope.launch {
                    settingsInteractor.toggleRegistrationCheck()

                    val settingsItems = settingsInteractor.getSettingsItemsUi(
                        changelogUrl = viewState.value.changelogUrl
                    )

                    setState {
                        copy(
                            settingsItems = settingsItems,
                        )
                    }

                    setEffect {
                        Effect.ShowSnackbar(
                            message = resourceProvider.getString(R.string.settings_screen_option_registration_check_restart)
                        )
                    }
                }
            }

            SettingsMenuItemType.DC_API -> {
                viewModelScope.launch {
                    settingsInteractor.toggleDCAPI()

                    val settingsItems = settingsInteractor.getSettingsItemsUi(
                        changelogUrl = viewState.value.changelogUrl
                    )

                    setState {
                        copy(
                            settingsItems = settingsItems,
                        )
                    }
                }
            }

            SettingsMenuItemType.RETRIEVE_LOGS -> {
                val logs = settingsInteractor.retrieveLogFileUris()
                if (logs.isNotEmpty()) {
                    setEffect {
                        Effect.ShareLogFile(
                            intent = Intent().apply {
                                action = Intent.ACTION_SEND_MULTIPLE
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, logs)
                                type = "text/*"
                            },
                            chooserTitle = resourceProvider.getString(R.string.settings_intent_chooser_logs_share_title)
                        )
                    }
                }
            }

            SettingsMenuItemType.CHANGELOG -> {
                val changelogUrl = viewState.value.changelogUrl
                if (changelogUrl != null) {
                    setEffect {
                        Effect.Navigation.OpenUrlExternally(
                            url = changelogUrl.toUri()
                        )
                    }
                }
            }

            SettingsMenuItemType.RESET_WALLET -> setEffect { Effect.ShowBottomSheet }
        }
    }

    /**
     * The PIN or biometric check that must succeed before biometric unlock is switched. With
     * biometric unlock off it asks for the PIN, so that a biometric enrolled by someone holding the
     * unlocked phone cannot turn it on; turning it off also accepts a biometric. On success it
     * returns to Settings, which then makes the change
     * ([handleBiometricSwitchAuthenticated]); going back changes nothing.
     */
    private fun getBiometricSwitchAuthenticationRoute(): String = generateComposableNavigationLink(
        screen = CommonScreens.Biometric,
        arguments = generateComposableArguments(
            mapOf(
                BiometricUiConfig.serializedKeyName to uiSerializer.toBase64(
                    BiometricUiConfig(
                        mode = BiometricMode.Default(
                            descriptionWhenBiometricsEnabled = resourceProvider.getString(
                                R.string.settings_biometrics_switch_enabled_description
                            ),
                            descriptionWhenBiometricsNotEnabled = resourceProvider.getString(
                                R.string.settings_biometrics_switch_not_enabled_description
                            ),
                            textAbovePin = resourceProvider.getString(
                                R.string.biometric_default_mode_text_above_pin_field
                            ),
                        ),
                        isPreAuthorization = false,
                        shouldInitializeBiometricAuthOnCreate = true,
                        onSuccessNavigation = ConfigNavigation(
                            navigationType = NavigationType.PopTo(DashboardScreens.Settings),
                            indicateFlowCompletion = FlowCompletion.SUCCESS,
                        ),
                        onBackNavigationConfig = OnBackNavigationConfig(
                            onBackNavigation = ConfigNavigation(
                                navigationType = NavigationType.PopTo(DashboardScreens.Settings),
                            ),
                            hasToolbarBackIcon = true
                        )
                    ),
                    BiometricUiConfig.Parser
                ).orEmpty()
            )
        )
    )

    /**
     * Switches biometric unlock once the PIN or biometric check has confirmed the user. Turning it
     * off needs nothing more; turning it on then asks for a biometric, so that the one enrolled is
     * known to work before the wallet relies on it.
     *
     * @param context The context the biometric prompt is shown in.
     */
    private fun handleBiometricSwitchAuthenticated(context: Context) {
        viewModelScope.launch {
            if (settingsInteractor.getBiometricUserSelection()) {
                settingsInteractor.toggleBiometricsAuthentication()
                val settingsItems = settingsInteractor.getSettingsItemsUi(
                    changelogUrl = viewState.value.changelogUrl
                )
                setState { copy(settingsItems = settingsItems) }
            } else {
                authenticate(context)
            }
        }
    }

    /**
     * The PIN or biometric check that must succeed before the factory reset runs. On success it
     * replaces itself with the reset screen; going back returns to Settings.
     */
    private fun getResetWalletAuthenticationRoute(): String = generateComposableNavigationLink(
        screen = CommonScreens.Biometric,
        arguments = generateComposableArguments(
            mapOf(
                BiometricUiConfig.serializedKeyName to uiSerializer.toBase64(
                    BiometricUiConfig(
                        mode = BiometricMode.Default(
                            descriptionWhenBiometricsEnabled = resourceProvider.getString(R.string.settings_reset_wallet_biometrics_enabled_description),
                            descriptionWhenBiometricsNotEnabled = resourceProvider.getString(R.string.settings_reset_wallet_biometrics_not_enabled_description),
                            textAbovePin = resourceProvider.getString(R.string.biometric_default_mode_text_above_pin_field),
                        ),
                        isPreAuthorization = false,
                        shouldInitializeBiometricAuthOnCreate = true,
                        onSuccessNavigation = ConfigNavigation(
                            navigationType = NavigationType.PushScreen(
                                screen = DashboardScreens.ResetWallet,
                            ),
                        ),
                        onBackNavigationConfig = OnBackNavigationConfig(
                            onBackNavigation = ConfigNavigation(
                                navigationType = NavigationType.PopTo(DashboardScreens.Settings),
                            ),
                            hasToolbarBackIcon = true
                        )
                    ),
                    BiometricUiConfig.Parser
                ).orEmpty()
            )
        )
    )

    private fun authenticate(context: Context) {

        if (isBiometricAuthenticating) return
        isBiometricAuthenticating = true

        settingsInteractor.authenticateWithBiometrics(
            context = context,
            notifyOnAuthenticationFailure = false,
        ) { result ->
            when (result) {
                is BiometricsAuthenticate.Success -> {
                    viewModelScope.launch {
                        try {
                            settingsInteractor.toggleBiometricsAuthentication()
                            val settingsItems = settingsInteractor.getSettingsItemsUi(
                                changelogUrl = viewState.value.changelogUrl
                            )
                            setState { copy(settingsItems = settingsItems) }
                        } finally {
                            isBiometricAuthenticating = false
                        }
                    }
                }

                is BiometricsAuthenticate.Failed -> {
                    isBiometricAuthenticating = false
                    setEffect {
                        Effect.ShowSnackbar(message = result.errorMessage)
                    }
                }

                is BiometricsAuthenticate.Cancelled -> {
                    isBiometricAuthenticating = false
                }
            }
        }
    }
}