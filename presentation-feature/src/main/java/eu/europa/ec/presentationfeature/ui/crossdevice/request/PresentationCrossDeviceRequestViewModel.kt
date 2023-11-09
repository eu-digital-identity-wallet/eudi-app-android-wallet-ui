/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.presentationfeature.ui.crossdevice.request

import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.config.BiometricUiConfig
import eu.europa.ec.commonfeature.ui.request.Event
import eu.europa.ec.commonfeature.ui.request.RequestViewModel
import eu.europa.ec.commonfeature.ui.request.transformer.RequestTransformer
import eu.europa.ec.presentationfeature.interactor.PresentationCrossDeviceInteractor
import eu.europa.ec.presentationfeature.interactor.PresentationCrossDeviceInteractorPartialState
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.PresentationScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class PresentationCrossDeviceRequestViewModel(
    private val interactor: PresentationCrossDeviceInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
) : RequestViewModel() {

    override fun getScreenTitle(): String {
        return resourceProvider.getString(R.string.request_title)
    }

    override fun getScreenSubtitle(): String {
        return resourceProvider.getString(R.string.request_subtitle_one)
    }

    override fun getScreenClickableSubtitle(): String? {
        return resourceProvider.getString(R.string.request_subtitle_two)
    }

    override fun getWarningText(): String {
        return resourceProvider.getString(R.string.request_warning_text)
    }

    override fun getNextScreen(): String {
        return generateComposableNavigationLink(
            screen = CommonScreens.Biometric,
            arguments = generateComposableArguments(
                mapOf(
                    BiometricUiConfig.serializedKeyName to uiSerializer.toBase64(
                        BiometricUiConfig(
                            title = getScreenTitle(),
                            subTitle = resourceProvider.getString(R.string.presentation_biometry_share_subtitle),
                            quickPinOnlySubTitle = resourceProvider.getString(R.string.presentation_quick_pin_share_subtitle),
                            isPreAuthorization = false,
                            shouldInitializeBiometricAuthOnCreate = true,
                            onSuccessNavigation = ConfigNavigation(
                                navigationType = NavigationType.PUSH,
                                screenToNavigate = PresentationScreens.CrossDeviceLoading
                            ),
                            onBackNavigation = ConfigNavigation(
                                navigationType = NavigationType.POP,
                                screenToNavigate = PresentationScreens.CrossDeviceRequest
                            )
                        ),
                        BiometricUiConfig.Parser
                    ).orEmpty()
                )
            )
        )
    }

    override fun doWork() {
        setState {
            copy(
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            interactor.getUserData().collect { response ->
                when (response) {
                    is PresentationCrossDeviceInteractorPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = ContentErrorConfig(
                                    onRetry = { setEvent(Event.DoWork) },
                                    errorSubTitle = response.error,
                                    onCancel = { setEvent(Event.GoBack) }
                                )
                            )
                        }
                    }

                    is PresentationCrossDeviceInteractorPartialState.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                items = RequestTransformer.transformToUiItems(
                                    userDataDomain = response.userDataDomain
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}