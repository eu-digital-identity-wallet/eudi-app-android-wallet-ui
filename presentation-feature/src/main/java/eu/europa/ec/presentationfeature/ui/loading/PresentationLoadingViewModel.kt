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
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.commonfeature.config.SuccessUIConfig
import eu.europa.ec.commonfeature.ui.loading.Effect
import eu.europa.ec.commonfeature.ui.loading.Event
import eu.europa.ec.commonfeature.ui.loading.LoadingViewModel
import eu.europa.ec.corelogic.di.getOrCreatePresentationScope
import eu.europa.ec.corelogic.model.AuthenticationData
import eu.europa.ec.presentationfeature.interactor.PresentationLoadingInteractor
import eu.europa.ec.presentationfeature.interactor.PresentationLoadingObserveResponsePartialState
import eu.europa.ec.presentationfeature.interactor.PresentationLoadingSendRequestedDocumentPartialState
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.content.ContentHeaderConfig
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.PresentationScreens
import eu.europa.ec.uilogic.navigation.Screen
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import java.net.URI
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@KoinViewModel
class PresentationLoadingViewModel(
    private val uiSerializer: UiSerializer,
    private val resourceProvider: ResourceProvider,
    private val interactor: PresentationLoadingInteractor,
) : LoadingViewModel() {

    override fun getHeaderConfig(): ContentHeaderConfig {
        return ContentHeaderConfig(
            description = resourceProvider.getString(R.string.loading_content_header_description),
        )
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

    override fun getCancellableTimeout(): Duration = 5.toDuration(DurationUnit.SECONDS)

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

                    is PresentationLoadingObserveResponsePartialState.Redirect -> {
                        onSuccess(it.uri)
                    }

                    is PresentationLoadingObserveResponsePartialState.RequestReadyToBeSent -> {
                        sendRequestedDocuments(Event.DoWork(context))
                    }

                    is PresentationLoadingObserveResponsePartialState.UserAuthenticationRequired -> {
                        val popEffect = Effect.Navigation.PopBackStackUpTo(
                            screenRoute = PresentationScreens.PresentationRequest.screenRoute,
                            inclusive = false
                        )

                        openAuthenticationPrompt(
                            context,
                            popEffect,
                            it.authenticationData,
                            {
                                sendRequestedDocuments(Event.DoWork(context))
                            }
                        )
                    }
                }
            }
        }
    }

    private fun sendRequestedDocuments(event: Event) {

        when (val result = interactor.sendRequestedDocuments()) {
            is PresentationLoadingSendRequestedDocumentPartialState.Success -> { /*no op*/
            }

            is PresentationLoadingSendRequestedDocumentPartialState.Failure -> {
                setState {
                    copy(
                        error = ContentErrorConfig(
                            onRetry = { setEvent(event) },
                            errorSubTitle = result.error,
                            onCancel = {
                                setEvent(Event.DismissError)
                                doNavigation(
                                    NavigationType.PopTo(
                                        getPreviousScreen()
                                    )
                                )
                            }
                        )
                    )
                }
            }
        }
    }

    private fun openAuthenticationPrompt(
        context: Context,
        popEffect: Effect,
        authenticationDataList: List<AuthenticationData>,
        sendRequestedDocumentsAction: () -> Unit,
        index: Int = 0,
    ) {
        val authenticationData = authenticationDataList[index]
        val isFinalAuthentication = index == authenticationDataList.lastIndex
        interactor.handleUserAuthentication(
            context = context,
            crypto = authenticationData.crypto,
            notifyOnAuthenticationFailure = viewState.value.notifyOnAuthenticationFailure,
            resultHandler = DeviceAuthenticationResult(
                onAuthenticationSuccess = {
                    authenticationData.onAuthenticationSuccess()
                    if (isFinalAuthentication) {
                        sendRequestedDocumentsAction()
                    } else {
                        delay(500)
                        openAuthenticationPrompt(
                            context,
                            popEffect,
                            authenticationDataList,
                            sendRequestedDocumentsAction,
                            index + 1
                        )
                    }
                },
                onAuthenticationError = { setEffect { popEffect } }
            )
        )
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

    private fun getSuccessConfig(uri: URI?): Map<String, String> {
        val deepLinkWithUriOrPopToDashboard = ConfigNavigation(
            navigationType = uri?.let {
                NavigationType.Deeplink(it.toString(), interactor.initiatorRoute)
            } ?: NavigationType.PopTo(DashboardScreens.Dashboard)
        )

        return mapOf(
            SuccessUIConfig.serializedKeyName to uiSerializer.toBase64(
                SuccessUIConfig(
                    headerConfig = SuccessUIConfig.HeaderConfig(
                        title = ""//resourceProvider.getString(R.string.loading_success_config_title)
                    ),
                    content = resourceProvider.getString(
                        //R.string.presentation_loading_success_config_subtitle,
                        R.string.loading_content_header_description, //TODO Giannis
                        interactor.verifierName
                        //?: resourceProvider.getString(R.string.presentation_loading_success_config_verifier)
                            ?: R.string.loading_content_header_description, //TODO Giannis
                    ),
                    imageConfig = SuccessUIConfig.ImageConfig(
                        type = SuccessUIConfig.ImageConfig.Type.DEFAULT
                    ),
                    buttonConfig = listOf(
                        SuccessUIConfig.ButtonConfig(
                            text = resourceProvider.getString(R.string.loading_success_config_primary_button_text),
                            style = SuccessUIConfig.ButtonConfig.Style.PRIMARY,
                            navigation = deepLinkWithUriOrPopToDashboard,
                        )
                    ),
                    onBackScreenToNavigate = deepLinkWithUriOrPopToDashboard,
                ),
                SuccessUIConfig
            ).orEmpty()
        )
    }
}