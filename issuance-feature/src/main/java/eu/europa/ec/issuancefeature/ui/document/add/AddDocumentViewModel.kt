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
import androidx.lifecycle.viewModelScope
import eu.europa.ec.businesslogic.controller.walletcore.AddSampleDataPartialState
import eu.europa.ec.businesslogic.controller.walletcore.IssuanceMethod
import eu.europa.ec.businesslogic.controller.walletcore.IssueDocumentPartialState
import eu.europa.ec.businesslogic.model.DeviceAuthenticationResult
import eu.europa.ec.commonfeature.config.IssuanceFlowUiConfig
import eu.europa.ec.commonfeature.model.DocumentOptionItemUi
import eu.europa.ec.commonfeature.model.DocumentTypeUi
import eu.europa.ec.issuancefeature.interactor.document.AddDocumentInteractor
import eu.europa.ec.issuancefeature.interactor.document.AddDocumentInteractorPartialState
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
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
    data object Init : Event()
    data object Pop : Event()
    data object DismissError : Event()
    data class IssueDocument(
        val issuanceMethod: IssuanceMethod,
        val documentType: String,
        val context: Context
    ) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
        data class SwitchScreen(val screenRoute: String, val inclusive: Boolean) : Navigation()
    }
}

@KoinViewModel
class AddDocumentViewModel(
    private val addDocumentInteractor: AddDocumentInteractor,
    private val resourceProvider: ResourceProvider,
    @InjectedParam private val flowType: IssuanceFlowUiConfig,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State = State(
        navigatableAction = getNavigatableAction(flowType),
        onBackAction = getOnBackAction(flowType),
        title = resourceProvider.getString(R.string.issuance_add_document_title),
        subtitle = resourceProvider.getString(R.string.issuance_add_document_subtitle)
    )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                if (viewState.value.isInitialised) {
                    setState { copy(isLoading = false) }
                }
                if (viewState.value.options.isEmpty()) {
                    getOptions(event)
                }
            }

            is Event.Pop -> setEffect { Effect.Navigation.Pop }

            is Event.DismissError -> {
                setState { copy(error = null) }
            }

            is Event.IssueDocument -> {
                if (event.documentType != DocumentTypeUi.SAMPLE_DOCUMENTS.codeName) {
                    issueDocument(
                        event = event,
                        issuanceMethod = event.issuanceMethod,
                        docType = event.documentType,
                        context = event.context
                    )
                } else {
                    loadSampleData(event)
                }
            }
        }
    }

    private fun getOptions(event: Event) {
        setState {
            copy(
                isLoading = true
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
        event: Event,
        issuanceMethod: IssuanceMethod,
        docType: String,
        context: Context
    ) {
        setState {
            copy(
                isLoading = true
            )
        }

        viewModelScope.launch {
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
                        navigateToSuccessScreen(
                            documentId = response.documentId
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

    private fun navigateToSuccessScreen(documentId: String) {
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

    private fun navigateToDashboardScreen() {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = DashboardScreens.Dashboard.screenRoute,
                inclusive = true
            )
        }
    }

    private fun getNavigatableAction(flowType: IssuanceFlowUiConfig): ScreenNavigateAction {
        return when (flowType) {
            IssuanceFlowUiConfig.NO_DOCUMENT -> ScreenNavigateAction.NONE
            IssuanceFlowUiConfig.EXTRA_DOCUMENT -> ScreenNavigateAction.CANCELABLE
        }
    }

    private fun getOnBackAction(flowType: IssuanceFlowUiConfig): (() -> Unit)? {
        return when (flowType) {
            IssuanceFlowUiConfig.NO_DOCUMENT -> null
            IssuanceFlowUiConfig.EXTRA_DOCUMENT -> {
                { setEvent(Event.Pop) }
            }
        }
    }
}