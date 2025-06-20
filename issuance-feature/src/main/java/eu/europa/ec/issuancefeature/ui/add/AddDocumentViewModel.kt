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

package eu.europa.ec.issuancefeature.ui.add

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.commonfeature.config.IssuanceFlowUiConfig
import eu.europa.ec.commonfeature.config.IssuanceSuccessUiConfig
import eu.europa.ec.commonfeature.config.OfferUiConfig
import eu.europa.ec.commonfeature.config.PresentationMode
import eu.europa.ec.commonfeature.config.QrScanFlow
import eu.europa.ec.commonfeature.config.QrScanUiConfig
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.commonfeature.model.DocumentOptionItemUi
import eu.europa.ec.corelogic.controller.IssuanceMethod
import eu.europa.ec.corelogic.controller.IssueDocumentPartialState
import eu.europa.ec.corelogic.di.getOrCreatePresentationScope
import eu.europa.ec.issuancefeature.interactor.AddDocumentInteractor
import eu.europa.ec.issuancefeature.interactor.AddDocumentInteractorPartialState
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
import eu.europa.ec.uilogic.navigation.helper.DeepLinkAction
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
    val notifyOnAuthenticationFailure: Boolean = false,

    val title: String = "",
    val subtitle: String = "",
    val options: List<DocumentOptionItemUi> = emptyList(),
    val showFooterScanner: Boolean,
) : ViewState

sealed class Event : ViewEvent {
    data class Init(val deepLink: Uri?) : Event()
    data object GoToQrScan : Event()
    data object Pop : Event()
    data object OnPause : Event()
    data class OnResumeIssuance(val uri: String) : Event()
    data class OnDynamicPresentation(val uri: String) : Event()
    data object Finish : Event()
    data object DismissError : Event()
    data class IssueDocument(
        val issuanceMethod: IssuanceMethod,
        val configId: String,
        val context: Context
    ) : Event()
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

    private var issuanceJob: Job? = null

    override fun setInitialState(): State = State(
        navigatableAction = getNavigatableAction(flowType),
        onBackAction = getOnBackAction(flowType),
        title = resourceProvider.getString(R.string.issuance_add_document_title),
        subtitle = resourceProvider.getString(R.string.issuance_add_document_subtitle),
        showFooterScanner = shouldShowFooterScanner(flowType),
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
                issueDocument(
                    issuanceMethod = event.issuanceMethod,
                    configId = event.configId,
                    context = event.context
                )
            }

            is Event.Finish -> setEffect { Effect.Navigation.Finish }

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

            is Event.GoToQrScan -> {
                navigateToQrScanScreen()
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

                        val deepLinkAction = getDeepLinkAction(deepLinkUri)

                        setState {
                            copy(
                                error = if (deepLinkAction == null) {
                                    ContentErrorConfig(
                                        onRetry = { setEvent(event) },
                                        errorSubTitle = response.error,
                                        onCancel = { setEvent(Event.DismissError) }
                                    )
                                } else {
                                    null
                                },
                                options = emptyList(),
                                isInitialised = true,
                                isLoading = false
                            )
                        }
                        deepLinkAction?.let {
                            handleDeepLink(it.first, it.second)
                        }
                    }
                }
            }
        }
    }

    private fun issueDocument(
        issuanceMethod: IssuanceMethod,
        configId: String,
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
                configId = configId
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
                        navigateToDocumentIssuanceSuccessScreen(
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
                            notifyOnAuthenticationFailure = viewState.value.notifyOnAuthenticationFailure,
                            resultHandler = DeviceAuthenticationResult(
                                onAuthenticationSuccess = {
                                    response.resultHandler.onAuthenticationSuccess()
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

    private fun navigateToDocumentIssuanceSuccessScreen(documentId: String) {
        val onSuccessNavigation = when (flowType) {
            IssuanceFlowUiConfig.NO_DOCUMENT -> ConfigNavigation(
                navigationType = NavigationType.PushScreen(
                    screen = DashboardScreens.Dashboard,
                    popUpToScreen = IssuanceScreens.AddDocument
                )
            )

            IssuanceFlowUiConfig.EXTRA_DOCUMENT -> ConfigNavigation(
                navigationType = NavigationType.PopTo(
                    screen = DashboardScreens.Dashboard
                )
            )
        }

        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = generateComposableNavigationLink(
                    screen = IssuanceScreens.DocumentIssuanceSuccess,
                    arguments = generateComposableArguments(
                        mapOf(
                            IssuanceSuccessUiConfig.serializedKeyName to uiSerializer.toBase64(
                                model = IssuanceSuccessUiConfig(
                                    documentIds = listOf(documentId),
                                    onSuccessNavigation = onSuccessNavigation,
                                ),
                                parser = IssuanceSuccessUiConfig.Parser
                            ).orEmpty()
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

    private fun shouldShowFooterScanner(flowType: IssuanceFlowUiConfig): Boolean {
        return when (flowType) {
            IssuanceFlowUiConfig.NO_DOCUMENT -> true
            IssuanceFlowUiConfig.EXTRA_DOCUMENT -> false
        }
    }

    private fun getNavigatableAction(flowType: IssuanceFlowUiConfig): ScreenNavigateAction {
        return when (flowType) {
            IssuanceFlowUiConfig.NO_DOCUMENT -> ScreenNavigateAction.NONE
            IssuanceFlowUiConfig.EXTRA_DOCUMENT -> ScreenNavigateAction.BACKABLE
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

    private fun getDeepLinkAction(deepLinkUri: Uri?): Pair<Uri, DeepLinkAction>? {
        return deepLinkUri?.let { uri ->
            hasDeepLink(uri)?.let {
                uri to it
            }
        }
    }

    private fun handleDeepLink(deepLinkUri: Uri?) {
        getDeepLinkAction(deepLinkUri)?.let { pair ->
            handleDeepLink(pair.first, pair.second)
        }
    }

    private fun handleDeepLink(uri: Uri, action: DeepLinkAction) {
        when (action.type) {
            DeepLinkType.CREDENTIAL_OFFER -> {
                setEffect {
                    Effect.Navigation.OpenDeepLinkAction(
                        deepLinkUri = uri,
                        arguments = generateComposableArguments(
                            mapOf(
                                OfferUiConfig.serializedKeyName to uiSerializer.toBase64(
                                    OfferUiConfig(
                                        offerURI = action.link.toString(),
                                        onSuccessNavigation = ConfigNavigation(
                                            navigationType = NavigationType.PushScreen(
                                                screen = DashboardScreens.Dashboard,
                                                popUpToScreen = IssuanceScreens.AddDocument
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