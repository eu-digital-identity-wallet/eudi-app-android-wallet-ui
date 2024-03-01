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

package eu.europa.ec.presentationfeature.ui.loading

import android.content.Context
import androidx.lifecycle.viewModelScope
import eu.europa.ec.businesslogic.controller.biometry.UserAuthenticationResult
import eu.europa.ec.businesslogic.di.getOrCreatePresentationScope
import eu.europa.ec.commonfeature.config.SuccessUIConfig
import eu.europa.ec.commonfeature.ui.loading.Effect
import eu.europa.ec.commonfeature.ui.loading.Event
import eu.europa.ec.commonfeature.ui.loading.LoadingViewModel
import eu.europa.ec.presentationfeature.interactor.PresentationLoadingInteractor
import eu.europa.ec.presentationfeature.interactor.PresentationLoadingObserveResponsePartialState
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.PresentationScreens
import eu.europa.ec.uilogic.navigation.Screen
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import java.net.URI

@KoinViewModel
class PresentationLoadingViewModel(
    private val uiSerializer: UiSerializer,
    private val resourceProvider: ResourceProvider,
    private val interactor: PresentationLoadingInteractor,
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
        return PresentationScreens.PresentationRequest
    }

    override fun getCallerScreen(): Screen {
        return PresentationScreens.PresentationLoading
    }

    private fun getNextScreen(uri: URI? = null): String {
        return generateComposableNavigationLink(
            screen = CommonScreens.Success,
            arguments = generateComposableArguments(
                getSuccessConfig(uri)
            )
        )
    }

    override fun doWork(context: Context) {
        viewModelScope.launch {
            interactor.observeResponse().collect {
                when (it) {
                    is PresentationLoadingObserveResponsePartialState.Failure -> {
                        setState {
                            copy(
                                error = ContentErrorConfig(
                                    onRetry = { setEvent(Event.DoWork(context)) },
                                    errorSubTitle = it.error,
                                    onCancel = {
                                        setEvent(Event.DismissError)
                                        doNavigation(NavigationType.Pop)
                                    }
                                )
                            )
                        }
                    }

                    is PresentationLoadingObserveResponsePartialState.Success -> {
                        onSuccess()
                    }

                    is PresentationLoadingObserveResponsePartialState.UserAuthenticationRequired -> {
                        val popEffect = Effect.Navigation.PopBackStackUpTo(
                            screenRoute = PresentationScreens.PresentationRequest.screenRoute,
                            inclusive = false
                        )
                        interactor.handleUserAuthentication(
                            context = context,
                            crypto = it.crypto,
                            resultHandler = UserAuthenticationResult(
                                onAuthenticationError = { setEffect { popEffect } },
                                onAuthenticationFailure = { setEffect { popEffect } }
                            )
                        )
                    }

                    is PresentationLoadingObserveResponsePartialState.Redirect -> {
                        onSuccess(it.uri)
                    }
                }
            }
        }
    }

    private fun onSuccess(uri: URI? = null) {
        setState {
            copy(
                error = null
            )
        }
        interactor.stopPresentation()
        getOrCreatePresentationScope().close()
        doNavigation(NavigationType.PushRoute(getNextScreen(uri)))
    }

    private fun getSuccessConfig(uri: URI?): Map<String, String> =
        mapOf(
            SuccessUIConfig.serializedKeyName to uiSerializer.toBase64(
                SuccessUIConfig(
                    header = resourceProvider.getString(R.string.loading_success_config_title),
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
                            navigation = ConfigNavigation(
                                navigationType = uri?.let {
                                    NavigationType.Deeplink(it.toString())
                                } ?: NavigationType.PopTo(DashboardScreens.Dashboard)
                            )
                        )
                    ),
                    onBackScreenToNavigate = ConfigNavigation(
                        navigationType = NavigationType.PopTo(DashboardScreens.Dashboard)
                    ),
                ),
                SuccessUIConfig.Parser
            ).orEmpty()
        )
}