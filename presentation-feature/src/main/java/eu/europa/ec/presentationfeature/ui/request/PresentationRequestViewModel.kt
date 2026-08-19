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

package eu.europa.ec.presentationfeature.ui.request

import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.config.BiometricMode
import eu.europa.ec.commonfeature.config.BiometricUiConfig
import eu.europa.ec.commonfeature.config.OnBackNavigationConfig
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.commonfeature.ui.request.Event
import eu.europa.ec.commonfeature.ui.request.RequestBottomSheetContent
import eu.europa.ec.commonfeature.ui.request.RequestViewModel
import eu.europa.ec.commonfeature.ui.request.model.RequestDataUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi
import eu.europa.ec.commonfeature.ui.request.model.toRegistrationWarningUi
import eu.europa.ec.commonfeature.ui.request.model.toRelyingPartyHeaderUi
import eu.europa.ec.corelogic.di.getOrNullKoinScope
import eu.europa.ec.presentationfeature.interactor.PresentationRequestInteractor
import eu.europa.ec.presentationfeature.interactor.PresentationRequestInteractorPartialState
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.PresentationScreens
import eu.europa.ec.uilogic.navigation.helper.IntentAction
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class PresentationRequestViewModel(
    private val interactor: PresentationRequestInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
    @InjectedParam private val requestUriConfigRaw: String
) : RequestViewModel() {

    override fun getNextScreen(): String {
        return generateComposableNavigationLink(
            screen = CommonScreens.Biometric,
            arguments = generateComposableArguments(
                mapOf(
                    BiometricUiConfig.serializedKeyName to uiSerializer.toBase64(
                        BiometricUiConfig(
                            mode = BiometricMode.Default(
                                descriptionWhenBiometricsEnabled = resourceProvider.getString(R.string.loading_biometry_biometrics_enabled_description),
                                descriptionWhenBiometricsNotEnabled = resourceProvider.getString(R.string.loading_biometry_biometrics_not_enabled_description),
                                textAbovePin = resourceProvider.getString(R.string.biometric_default_mode_text_above_pin_field),
                            ),
                            isPreAuthorization = false,
                            shouldInitializeBiometricAuthOnCreate = true,
                            onSuccessNavigation = ConfigNavigation(
                                navigationType = NavigationType.PushScreen(
                                    screen = PresentationScreens.PresentationLoading,
                                    arguments = mapOf("scopeId" to viewState.value.presentationScopeId)
                                ),
                            ),
                            onBackNavigationConfig = OnBackNavigationConfig(
                                onBackNavigation = ConfigNavigation(
                                    navigationType = NavigationType.PopTo(PresentationScreens.PresentationRequest),
                                ),
                                hasToolbarBackIcon = true
                            )
                        ),
                        BiometricUiConfig.Parser
                    ).orEmpty()
                )
            )
        )
    }

    override fun init(intentAction: IntentAction?) {
        val requestUriConfig = uiSerializer.fromBase64(
            requestUriConfigRaw,
            RequestUriConfig::class.java,
            RequestUriConfig.Parser
        ) ?: throw RuntimeException("RequestUriConfig:: is Missing or invalid")

        setState {
            copy(
                presentationScopeId = requestUriConfig.presentationScopeId,
                intentAction = intentAction,
            )
        }

        interactor.setConfig(requestUriConfig, intentAction)
    }

    override fun doWork() {

        setState {
            copy(
                isLoading = true,
                error = null
            )
        }

        viewModelJob?.cancel()
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
                                    onCancel = { setEvent(Event.OnBack) }
                                )
                            )
                        }
                    }

                    is PresentationRequestInteractorPartialState.Success -> {
                        val requestData = RequestDataUi.of(combinations = response.combinationsUi)

                        val relyingPartyHeader = response.relyingParty.toRelyingPartyHeaderUi(
                            fallbackName = resourceProvider.getString(R.string.request_relying_party_default_name)
                        )
                        val registrationWarning = response.relyingParty.toRegistrationWarningUi()

                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                relyingPartyHeader = relyingPartyHeader,
                                registrationWarning = registrationWarning,
                                requestDataUi = requestData,
                                claimsAreSelectable = response.claimsAreSelectable,
                            )
                        }

                        updateData(updatedItems = requestData.selectedDocuments)
                    }

                    is PresentationRequestInteractorPartialState.VerifierNotTrusted -> {
                        interactor.stopPresentation()
                        setState {
                            copy(
                                isLoading = false,
                                error = null
                            )
                        }
                        showBottomSheet(sheetContent = RequestBottomSheetContent.VerifierNotTrusted)
                    }

                    is PresentationRequestInteractorPartialState.Disconnect -> {
                        setEvent(Event.OnBack)
                    }

                    is PresentationRequestInteractorPartialState.NoData -> {
                        val relyingPartyHeader = response.relyingParty.toRelyingPartyHeaderUi(
                            fallbackName = resourceProvider.getString(R.string.request_relying_party_default_name)
                        )

                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                relyingPartyHeader = relyingPartyHeader,
                                registrationWarning = null,
                                requestDataUi = RequestDataUi.NoData,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun updateData(updatedItems: List<RequestDocumentItemUi>) {
        super.updateData(updatedItems)
        interactor.updateRequestedDocuments(
            selectedCombination = viewState.value.requestDataUi.selectedCombination,
        )
    }

    override fun cleanUp() {
        super.cleanUp()
        interactor.stopPresentation()
        getOrNullKoinScope(viewState.value.presentationScopeId)?.close()
    }
}