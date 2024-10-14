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

package eu.europa.ec.issuancefeature.ui.document.add

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.commonfeature.config.IssuanceFlowUiConfig
import eu.europa.ec.commonfeature.config.OfferUiConfig
import eu.europa.ec.commonfeature.config.PresentationMode
import eu.europa.ec.commonfeature.config.QrScanFlow
import eu.europa.ec.commonfeature.config.QrScanUiConfig
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.commonfeature.model.DocumentOptionItemUi
import eu.europa.ec.corelogic.controller.AddSampleDataPartialState
import eu.europa.ec.corelogic.controller.IssuanceMethod
import eu.europa.ec.corelogic.controller.IssueDocumentPartialState
import eu.europa.ec.corelogic.di.getOrCreatePresentationScope
import eu.europa.ec.corelogic.model.DocType
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.issuancefeature.interactor.document.AddDocumentInteractor
import eu.europa.ec.issuancefeature.interactor.document.AddDocumentInteractorPartialState
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.PresentationScreens
import eu.europa.ec.uilogic.navigation.helper.DeepLinkType
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.navigation.helper.hasDeepLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

data class State(
    val navigatableAction: ScreenNavigateAction,
    val onBackAction: (() -> Unit)? = null,

    val isLoading: Boolean = false,
    val error: ContentErrorConfig? = null,
    val isInitialised: Boolean = false,

    val title: String = "",
    val subtitle: String = "",
    val options: List<DocumentOptionItemUi> = emptyList()
) : ViewState

sealed class Event : ViewEvent {
    data class Init(val deepLink: Uri?) : Event()
    data object Pop : Event()
    data object OnPause : Event()
    data class OnResumeIssuance(val uri: String) : Event()
    data class OnDynamicPresentation(val uri: String) : Event()
    data object Finish : Event()
    data object DismissError : Event()
    data class IssueDocument(
        val issuanceMethod: IssuanceMethod,
        val documentType: DocType,
        val context: Context
    ) : Event()

    data object GoToQrScan : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
        data object Finish : Navigation()
        data class SwitchScreen(val screenRoute: String, val inclusive: Boolean) : Navigation()
        data class OpenDeepLinkAction(val deepLinkUri: Uri, val arguments: String?) : Navigation()
    }
}

@KoinViewModel
class AddDocumentViewModel(
    private val addDocumentInteractor: AddDocumentInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
    @InjectedParam private val flowType: IssuanceFlowUiConfig,
) : MviViewModel<Event, State, Effect>() {

    var issuanceJob: Job? = null

    override fun setInitialState(): State = State(
        navigatableAction = getNavigatableAction(flowType),
        onBackAction = getOnBackAction(flowType),
        title = resourceProvider.getString(R.string.issuance_add_document_title),
        subtitle = resourceProvider.getString(R.string.issuance_add_document_subtitle)
    )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                if (viewState.value.options.isEmpty()) {
                    getOptions(event, event.deepLink)
                } else {
                    handleDeepLink(event.deepLink)
                }
            }

            is Event.Pop -> setEffect { Effect.Navigation.Pop }

            is Event.DismissError -> {
                setState { copy(error = null) }
            }

            is Event.IssueDocument -> {
                if (event.documentType != DocumentIdentifier.SAMPLE.docType) {
                    issueDocument(
                        issuanceMethod = event.issuanceMethod,
                        docType = event.documentType,
                        context = event.context
                    )
                } else {
                    loadSampleData(event)
                }
            }

            is Event.Finish -> setEffect { Effect.Navigation.Finish }

            is Event.GoToQrScan -> navigateToQrScanScreen()

            is Event.OnPause -> {
                if (viewState.value.isInitialised) {
                    setState { copy(isLoading = false) }
                }
            }

            is Event.OnResumeIssuance -> {
                setState {
                    copy(isLoading = true)
                }
                addDocumentInteractor.resumeOpenId4VciWithAuthorization(event.uri)
            }

            is Event.OnDynamicPresentation -> {
                getOrCreatePresentationScope()
                setEffect {
                    Effect.Navigation.SwitchScreen(
                        generateComposableNavigationLink(
                            PresentationScreens.PresentationRequest,
                            generateComposableArguments(
                                mapOf(
                                    RequestUriConfig.serializedKeyName to uiSerializer.toBase64(
                                        RequestUriConfig(
                                            PresentationMode.OpenId4Vp(
                                                event.uri,
                                                IssuanceScreens.AddDocument.screenRoute
                                            )
                                        ),
                                        RequestUriConfig.Parser
                                    )
                                )
                            )
                        ),
                        inclusive = false
                    )
                }
            }
        }
    }

    private fun getOptions(event: Event, deepLinkUri: Uri?) {

        setState {
            copy(
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            addDocumentInteractor.getAddDocumentOption(flowType).collect { response ->
                when (response) {
                    is AddDocumentInteractorPartialState.Success -> {
                        setState {
                            copy(
                                error = null,
                                options = response.options,
                                isInitialised = true,
                                isLoading = false
                            )
                        }
                        handleDeepLink(deepLinkUri)
                    }

                    is AddDocumentInteractorPartialState.Failure -> {
                        setState {
                            copy(
                                error = ContentErrorConfig(
                                    onRetry = { setEvent(event) },
                                    errorSubTitle = response.error,
                                    onCancel = { setEvent(Event.DismissError) }
                                ),
                                options = emptyList(),
                                isInitialised = true,
                                isLoading = false
                            )
                        }
                    }
                }
            }
        }
    }

    private fun issueDocument(
        issuanceMethod: IssuanceMethod,
        docType: DocType,
        context: Context
    ) {
        issuanceJob?.cancel()
        issuanceJob = viewModelScope.launch {

            setState {
                copy(
                    isLoading = true,
                    error = null
                )
            }

            addDocumentInteractor.issueDocument(
                issuanceMethod = issuanceMethod,
                documentType = docType
            ).collect { response ->
                when (response) {
                    is IssueDocumentPartialState.Failure -> {
                        setState {
                            copy(
                                error = ContentErrorConfig(
                                    onRetry = null,
                                    errorSubTitle = response.errorMessage,
                                    onCancel = { setEvent(Event.DismissError) }
                                ),
                                isLoading = false
                            )
                        }
                    }

                    is IssueDocumentPartialState.Success -> {
                        setState {
                            copy(
                                error = null,
                                isLoading = false
                            )
                        }
                        navigateToIssuanceSuccessScreen(
                            documentId = response.documentId
                        )
                    }

                    is IssueDocumentPartialState.DeferredSuccess -> {
                        setState {
                            copy(
                                error = null,
                                isLoading = false
                            )
                        }
                        navigateToGenericSuccessScreen(
                            route = addDocumentInteractor.buildGenericSuccessRouteForDeferred(
                                flowType
                            )
                        )
                    }

                    is IssueDocumentPartialState.UserAuthRequired -> {
                        addDocumentInteractor.handleUserAuth(
                            context = context,
                            crypto = response.crypto,
                            resultHandler = DeviceAuthenticationResult(
                                onAuthenticationSuccess = {
                                    response.resultHandler.onAuthenticationSuccess()
                                },
                                onAuthenticationFailure = {
                                    response.resultHandler.onAuthenticationFailure()
                                },
                                onAuthenticationError = {
                                    response.resultHandler.onAuthenticationError()
                                }
                            )
                        )
                    }
                }
            }
        }
    }

    private fun loadSampleData(event: Event) {
        setState {
            copy(
                isLoading = true
            )
        }

        viewModelScope.launch {
            addDocumentInteractor.addSampleData().collect { response ->
                when (response) {
                    is AddSampleDataPartialState.Failure -> {
                        setState {
                            copy(
                                error = ContentErrorConfig(
                                    onRetry = { setEvent(event) },
                                    errorSubTitle = response.error,
                                    onCancel = { setEvent(Event.DismissError) }
                                ),
                                isLoading = false
                            )
                        }
                    }

                    is AddSampleDataPartialState.Success -> {
                        setState {
                            copy(
                                error = null,
                                isLoading = false
                            )
                        }
                        navigateToDashboardScreen()
                    }
                }
            }
        }
    }

    private fun navigateToIssuanceSuccessScreen(documentId: String) {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = generateComposableNavigationLink(
                    screen = IssuanceScreens.Success,
                    arguments = generateComposableArguments(
                        mapOf(
                            "flowType" to IssuanceFlowUiConfig.fromIssuanceFlowUiConfig(flowType),
                            "documentId" to documentId,
                        )
                    )
                ),
                inclusive = false
            )
        }
    }

    private fun navigateToGenericSuccessScreen(route: String) {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = route,
                inclusive = true
            )
        }
    }

    private fun navigateToDashboardScreen() {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = DashboardScreens.Dashboard.screenRoute,
                inclusive = true
            )
        }
    }

    private fun navigateToQrScanScreen() {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = generateComposableNavigationLink(
                    screen = CommonScreens.QrScan,
                    arguments = generateComposableArguments(
                        mapOf(
                            QrScanUiConfig.serializedKeyName to uiSerializer.toBase64(
                                QrScanUiConfig(
                                    title = resourceProvider.getString(R.string.issuance_qr_scan_title),
                                    subTitle = resourceProvider.getString(R.string.issuance_qr_scan_subtitle),
                                    qrScanFlow = QrScanFlow.Issuance(flowType)
                                ),
                                QrScanUiConfig.Parser
                            )
                        )
                    )
                ),
                inclusive = false
            )
        }
    }

    private fun getNavigatableAction(flowType: IssuanceFlowUiConfig): ScreenNavigateAction {
        return when (flowType) {
            IssuanceFlowUiConfig.NO_DOCUMENT -> ScreenNavigateAction.NONE
            IssuanceFlowUiConfig.EXTRA_DOCUMENT -> ScreenNavigateAction.CANCELABLE
        }
    }

    private fun getOnBackAction(flowType: IssuanceFlowUiConfig): (() -> Unit) {
        return when (flowType) {
            IssuanceFlowUiConfig.NO_DOCUMENT -> {
                { setEvent(Event.Finish) }
            }

            IssuanceFlowUiConfig.EXTRA_DOCUMENT -> {
                { setEvent(Event.Pop) }
            }
        }
    }

    private fun handleDeepLink(deepLinkUri: Uri?) {
        deepLinkUri?.let { uri ->
            hasDeepLink(uri)?.let {
                when (it.type) {
                    DeepLinkType.CREDENTIAL_OFFER -> {
                        setEffect {
                            Effect.Navigation.OpenDeepLinkAction(
                                deepLinkUri = uri,
                                arguments = generateComposableArguments(
                                    mapOf(
                                        OfferUiConfig.serializedKeyName to uiSerializer.toBase64(
                                            OfferUiConfig(
                                                offerURI = it.link.toString(),
                                                onSuccessNavigation = ConfigNavigation(
                                                    navigationType = NavigationType.PushScreen(
                                                        screen = DashboardScreens.Dashboard
                                                    )
                                                ),
                                                onCancelNavigation = ConfigNavigation(
                                                    navigationType = NavigationType.Pop
                                                )
                                            ),
                                            OfferUiConfig.Parser
                                        )
                                    )
                                )
                            )
                        }
                    }

                    DeepLinkType.EXTERNAL -> {
                        setEffect {
                            Effect.Navigation.OpenDeepLinkAction(
                                deepLinkUri = uri,
                                arguments = null
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}