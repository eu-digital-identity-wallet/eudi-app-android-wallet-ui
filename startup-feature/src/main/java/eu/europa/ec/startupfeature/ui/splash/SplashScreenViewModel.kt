/*
 * Copyright (c) 2023 European Commission
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

package eu.europa.ec.startupfeature.ui.splash

import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.config.BiometricUiConfig
import eu.europa.ec.commonfeature.model.PinFlows
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.startupfeature.interactor.splash.SplashInteractor
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.ModuleRoute
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class State(
    val logoAnimationDuration: Int = 1500
) : ViewState

sealed class Event : ViewEvent {
    data object Initialize : Event()
}

sealed class Effect : ViewSideEffect {

    sealed class Navigation : Effect() {
        data class SwitchModule(val moduleRoute: ModuleRoute) : Navigation()
        data class SwitchScreen(val route: String) : Navigation()
    }
}

@KoinViewModel
class SplashScreenViewModel(
    private val interactor: SplashInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State = State()

    override fun handleEvents(event: Event) {
        when (event) {
            Event.Initialize -> enterApplication()
        }
    }

    private fun enterApplication() {
        viewModelScope.launch {
            delay((viewState.value.logoAnimationDuration + 500).toLong())
            val hasBiometricsValue = interactor.getDevicePin()

            if (hasBiometricsValue.isNullOrEmpty()) {
                setEffect {
                    Effect.Navigation.SwitchScreen(
                        generateComposableNavigationLink(
                            screen = interactor.getAfterSplashRoute(),
                            arguments = generateComposableArguments(mapOf("payload" to PinFlows.CREATE.type))
                        )
                    )
                }

            } else {
                setEffect { Effect.Navigation.SwitchScreen(getBiometricsNextScreen()) }
            }
        }
    }

    private fun getBiometricsNextScreen(): String {
        return generateComposableNavigationLink(
            screen = CommonScreens.Biometric,
            arguments = generateComposableArguments(
                mapOf(
                    BiometricUiConfig.serializedKeyName to uiSerializer.toBase64(
                        BiometricUiConfig(
                            title = resourceProvider.getString(R.string.biometric_prompt_title),
                            subTitle = resourceProvider.getString(R.string.biometric_prompt_subtitle),
                            quickPinOnlySubTitle = resourceProvider.getString(R.string.loading_quick_pin_share_subtitle),
                            isPreAuthorization = false,
                            shouldInitializeBiometricAuthOnCreate = true,
                            onSuccessNavigation = ConfigNavigation(
                                navigationType = NavigationType.PUSH,
                                screenToNavigate = DashboardScreens.Dashboard
                            ),
                            onBackNavigation = null
                        ),
                        BiometricUiConfig.Parser
                    ).orEmpty()
                )
            )
        )
    }
}
