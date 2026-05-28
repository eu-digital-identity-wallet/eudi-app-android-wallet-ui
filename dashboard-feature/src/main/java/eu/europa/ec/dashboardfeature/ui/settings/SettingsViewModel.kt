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
import eu.europa.ec.dashboardfeature.interactor.SettingsInteractor
import eu.europa.ec.dashboardfeature.ui.settings.model.SettingsItemUi
import eu.europa.ec.dashboardfeature.ui.settings.model.SettingsMenuItemType
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

data class State(
    val isLoading: Boolean = false,
    val screenTitle: String,

    val settingsItems: List<SettingsItemUi> = emptyList(),

    val appVersion: String = "",
    val changelogUrl: String?,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object Pop : Event()
    data object LaunchBiometricSystemScreen : Event()
    data class ItemClicked(
        val itemType: SettingsMenuItemType,
        val context: Context
    ) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
        data object LaunchBiometricsSystemScreen : Navigation()

        data class OpenUrlExternally(val url: Uri) : Navigation()
    }

    data class ShareLogFile(val intent: Intent, val chooserTitle: String) : Effect()
    data class ShowSnackbar(val message: String) : Effect()
}

@KoinViewModel
class SettingsViewModel(
    private val settingsInteractor: SettingsInteractor,
    private val resourceProvider: ResourceProvider,
) : MviViewModel<Event, State, Effect>() {
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

            is Event.ItemClicked -> handleSettingsMenuItemClicked(
                itemType = event.itemType,
                context = event.context
            )
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

    private fun handleSettingsMenuItemClicked(
        itemType: SettingsMenuItemType,
        context: Context,
    ) {
        when (itemType) {
            SettingsMenuItemType.BIOMETRICS_AUTHENTICATION -> {
                when (val availability = settingsInteractor.getBiometricsAvailability()) {
                    is BiometricsAvailability.CanAuthenticate -> authenticate(context)

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
        }
    }

    private fun authenticate(context: Context) {
        settingsInteractor.authenticateWithBiometrics(
            context = context,
            notifyOnAuthenticationFailure = true,
        ) { result ->
            when (result) {
                is BiometricsAuthenticate.Success -> {
                    viewModelScope.launch {
                        settingsInteractor.toggleBiometricsAuthentication()
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

                else -> {}
            }
        }
    }
}