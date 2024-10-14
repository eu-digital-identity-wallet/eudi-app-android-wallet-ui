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

package eu.europa.ec.proximityfeature.ui.loading

import android.content.Context
import androidx.lifecycle.viewModelScope
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.commonfeature.config.SuccessUIConfig
import eu.europa.ec.commonfeature.ui.loading.Effect
import eu.europa.ec.commonfeature.ui.loading.Event
import eu.europa.ec.commonfeature.ui.loading.LoadingViewModel
import eu.europa.ec.corelogic.di.getOrCreatePresentationScope
import eu.europa.ec.proximityfeature.interactor.ProximityLoadingInteractor
import eu.europa.ec.proximityfeature.interactor.ProximityLoadingObserveResponsePartialState
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.ProximityScreens
import eu.europa.ec.uilogic.navigation.Screen
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@KoinViewModel
class ProximityLoadingViewModel(
    private val uiSerializer: UiSerializer,
    private val resourceProvider: ResourceProvider,
    private val interactor: ProximityLoadingInteractor,
) : LoadingViewModel() {

    override fun getTitle(): String {
        return if (interactor.verifierName.isNullOrBlank()) {
            resourceProvider.getString(R.string.request_title_before_badge) +
                    resourceProvider.getString(R.string.request_title_after_badge)
        } else {
            interactor.verifierName +
                    resourceProvider.getString(R.string.request_title_after_badge)
        }
    }

    override fun getSubtitle(): String {
        return resourceProvider.getString(R.string.loading_subtitle)
    }

    override fun getPreviousScreen(): Screen {
        return ProximityScreens.Request
    }

    override fun getCallerScreen(): Screen {
        return ProximityScreens.Loading
    }

    private fun getNextScreen(): String {
        return generateComposableNavigationLink(
            screen = CommonScreens.Success,
            arguments = generateComposableArguments(
                getSuccessConfig()
            )
        )
    }

    override fun getCancellableTimeout(): Duration = 5.toDuration(DurationUnit.SECONDS)

    override fun doWork(context: Context) {
        viewModelScope.launch {
            interactor.observeResponse().collect {
                when (it) {
                    is ProximityLoadingObserveResponsePartialState.Failure -> {
                        setState {
                            copy(
                                error = ContentErrorConfig(
                                    onRetry = { setEvent(Event.DoWork(context)) },
                                    errorSubTitle = it.error,
                                    onCancel = {
                                        setEvent(Event.DismissError)
                                        doNavigation(NavigationType.PopTo(getPreviousScreen()))
                                    }
                                )
                            )
                        }
                    }

                    is ProximityLoadingObserveResponsePartialState.Success -> {
                        setState {
                            copy(
                                error = null
                            )
                        }
                        interactor.stopPresentation()
                        getOrCreatePresentationScope().close()
                        doNavigation(NavigationType.PushRoute(getNextScreen()))
                    }

                    is ProximityLoadingObserveResponsePartialState.UserAuthenticationRequired -> {
                        val popEffect = Effect.Navigation.PopBackStackUpTo(
                            screenRoute = ProximityScreens.Request.screenRoute,
                            inclusive = false
                        )
                        interactor.handleUserAuthentication(
                            context = context,
                            crypto = it.crypto,
                            resultHandler = DeviceAuthenticationResult(
                                onAuthenticationSuccess = { it.resultHandler.onAuthenticationSuccess() },
                                onAuthenticationFailure = { setEffect { popEffect } },
                                onAuthenticationError = { setEffect { popEffect } }
                            )
                        )
                    }
                }
            }
        }
    }

    private fun getSuccessConfig(): Map<String, String> {
        val popToDashboard = ConfigNavigation(
            navigationType = NavigationType.PopTo(DashboardScreens.Dashboard),
        )

        return mapOf(
            SuccessUIConfig.serializedKeyName to uiSerializer.toBase64(
                SuccessUIConfig(
                    headerConfig = SuccessUIConfig.HeaderConfig(
                        title = resourceProvider.getString(R.string.loading_success_config_title)
                    ),
                    content = resourceProvider.getString(
                        R.string.presentation_loading_success_config_subtitle,
                        interactor.verifierName
                            ?: resourceProvider.getString(R.string.presentation_loading_success_config_verifier)
                    ),
                    imageConfig = SuccessUIConfig.ImageConfig(
                        type = SuccessUIConfig.ImageConfig.Type.DEFAULT
                    ),
                    buttonConfig = listOf(
                        SuccessUIConfig.ButtonConfig(
                            text = resourceProvider.getString(R.string.loading_success_config_primary_button_text),
                            style = SuccessUIConfig.ButtonConfig.Style.PRIMARY,
                            navigation = popToDashboard,
                        )
                    ),
                    onBackScreenToNavigate = popToDashboard,
                ),
                SuccessUIConfig.Parser
            ).orEmpty()
        )
    }
}