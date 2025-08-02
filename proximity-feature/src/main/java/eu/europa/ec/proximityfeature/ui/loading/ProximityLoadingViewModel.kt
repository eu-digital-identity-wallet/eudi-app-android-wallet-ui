/*
 * Copyright (c) 2025 European Commission
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
import eu.europa.ec.commonfeature.ui.loading.Effect
import eu.europa.ec.commonfeature.ui.loading.Event
import eu.europa.ec.commonfeature.ui.loading.LoadingViewModel
import eu.europa.ec.corelogic.model.AuthenticationData
import eu.europa.ec.proximityfeature.interactor.ProximityLoadingInteractor
import eu.europa.ec.proximityfeature.interactor.ProximityLoadingObserveResponsePartialState
import eu.europa.ec.proximityfeature.interactor.ProximityLoadingSendRequestedDocumentPartialState
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.content.ContentHeaderConfig
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.navigation.ProximityScreens
import eu.europa.ec.uilogic.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@KoinViewModel
class ProximityLoadingViewModel(
    private val resourceProvider: ResourceProvider,
    private val interactor: ProximityLoadingInteractor,
) : LoadingViewModel() {

    override fun getHeaderConfig(): ContentHeaderConfig {
        return ContentHeaderConfig(
            description = resourceProvider.getString(R.string.loading_header_description),
        )
    }

    override fun getPreviousScreen(): Screen {
        return ProximityScreens.Request
    }

    override fun getCallerScreen(): Screen {
        return ProximityScreens.Loading
    }

    private fun getNextScreen(): String {
        return ProximityScreens.Success.screenRoute
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
                        onSuccess()
                    }

                    is ProximityLoadingObserveResponsePartialState.RequestReadyToBeSent -> {
                        sendRequestedDocuments(event = Event.DoWork(context))
                    }

                    is ProximityLoadingObserveResponsePartialState.UserAuthenticationRequired -> {
                        val popEffect = Effect.Navigation.PopBackStackUpTo(
                            screenRoute = ProximityScreens.Request.screenRoute,
                            inclusive = false
                        )

                        openAuthenticationPrompt(
                            context,
                            popEffect,
                            it.authenticationData,
                            {
                                sendRequestedDocuments(event = Event.DoWork(context))
                            }
                        )
                    }
                }
            }
        }
    }

    private fun sendRequestedDocuments(event: Event) {

        when (val result = interactor.sendRequestedDocuments()) {
            is ProximityLoadingSendRequestedDocumentPartialState.Success -> { /*no op*/
            }

            is ProximityLoadingSendRequestedDocumentPartialState.Failure -> {
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
        sendRequestedDocumentsAction: suspend () -> Unit,
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

    private fun onSuccess() {
        setState {
            copy(
                error = null
            )
        }
        doNavigation(NavigationType.PushRoute(getNextScreen()))
    }
}