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

package eu.europa.ec.presentationfeature.ui.request

import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.config.BiometricUiConfig
import eu.europa.ec.commonfeature.config.OnBackNavigationConfig
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.commonfeature.ui.request.Event
import eu.europa.ec.commonfeature.ui.request.RequestViewModel
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi2
import eu.europa.ec.presentationfeature.interactor.PresentationRequestInteractor
import eu.europa.ec.presentationfeature.interactor.PresentationRequestInteractorPartialState
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.RelyingPartyData
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.content.ContentHeaderConfig
import eu.europa.ec.uilogic.component.content.TitleWithBadge
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.PresentationScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class PresentationRequestViewModel(
    private val interactor: PresentationRequestInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
    @InjectedParam private val requestUriConfigRaw: String
) : RequestViewModel() {

    override fun getHeaderConfig(): ContentHeaderConfig {
        return ContentHeaderConfig(
            description = resourceProvider.getString(R.string.request_header_description),
            mainText = resourceProvider.getString(R.string.request_header_main_text),
            //relyingPartyData = interactor.getRelyingPartyData() //TODO()
            relyingPartyData = RelyingPartyData(
                isVerified = true,
                name = "Relying Party",
                description = "requests the following",
            )
        )
    }

    override fun getNextScreen(): String {
        return generateComposableNavigationLink(
            screen = CommonScreens.Biometric,
            arguments = generateComposableArguments(
                mapOf(
                    BiometricUiConfig.serializedKeyName to uiSerializer.toBase64(
                        BiometricUiConfig(
                            //title = viewState.value.screenTitle.plainText,
                            title = "",//TODO
                            subTitle = resourceProvider.getString(R.string.loading_biometry_share_subtitle),
                            quickPinOnlySubTitle = resourceProvider.getString(R.string.loading_quick_pin_share_subtitle),
                            isPreAuthorization = false,
                            shouldInitializeBiometricAuthOnCreate = true,
                            onSuccessNavigation = ConfigNavigation(
                                navigationType = NavigationType.PushScreen(PresentationScreens.PresentationLoading),
                            ),
                            onBackNavigationConfig = OnBackNavigationConfig(
                                onBackNavigation = ConfigNavigation(
                                    navigationType = NavigationType.PopTo(PresentationScreens.PresentationRequest),
                                ),
                                hasToolbarCancelIcon = true
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

        val requestUriConfig = uiSerializer.fromBase64(
            requestUriConfigRaw,
            RequestUriConfig::class.java,
            RequestUriConfig.Parser
        ) ?: throw RuntimeException("RequestUriConfig:: is Missing or invalid")

        interactor.setConfig(requestUriConfig)

        viewModelJob = viewModelScope.launch {
            interactor.getRequestDocuments().collect { response ->
                when (response) {
                    is PresentationRequestInteractorPartialState.Failure -> {
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

                    is PresentationRequestInteractorPartialState.Success -> {
                        updateData(response.requestDocuments)
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                verifierName = response.verifierName,
                                items = response.requestDocuments,
                                //newItems = expandableData
                            )
                        }
                    }

                    is PresentationRequestInteractorPartialState.Disconnect -> {
                        setEvent(Event.GoBack)
                    }

                    is PresentationRequestInteractorPartialState.NoData -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                verifierName = response.verifierName,
                                noItems = true,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun updateData(
        updatedItems: List<RequestDocumentItemUi2<Event>>,
        allowShare: Boolean?
    ) {
        super.updateData(updatedItems, allowShare)
        interactor.updateRequestedDocuments(updatedItems)
    }

    override fun cleanUp() {
        super.cleanUp()
        interactor.stopPresentation()
    }

    private fun constructTitle(
        verifierName: String? = null,
        verifierIsTrusted: Boolean = false
    ): TitleWithBadge {
        val textBeforeBadge = if (verifierName.isNullOrBlank()) {
            resourceProvider.getString(R.string.request_title_before_badge)
        } else {
            verifierName
        }

        val textAfterBadge = resourceProvider.getString(R.string.request_title_after_badge)

        return TitleWithBadge(
            textBeforeBadge = textBeforeBadge,
            textAfterBadge = textAfterBadge,
            isTrusted = verifierIsTrusted
        )
    }
}