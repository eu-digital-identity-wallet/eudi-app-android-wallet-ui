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

package eu.europa.ec.issuancefeature.ui.document.details

import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.config.IssuanceFlowUiConfig
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.commonfeature.model.toUiName
import eu.europa.ec.issuancefeature.interactor.document.DocumentDetailsInteractor
import eu.europa.ec.issuancefeature.interactor.document.DocumentDetailsInteractorDeleteDocumentPartialState
import eu.europa.ec.issuancefeature.interactor.document.DocumentDetailsInteractorPartialState
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.HeaderData
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.StartupScreens
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

data class State(
    val detailsType: IssuanceFlowUiConfig,
    val navigatableAction: ScreenNavigateAction,
    val onBackAction: (() -> Unit)? = null,
    val shouldShowPrimaryButton: Boolean,
    val hasCustomTopBar: Boolean,
    val hasBottomPadding: Boolean,
    val detailsHaveBottomGradient: Boolean,

    val isLoading: Boolean = true,
    val error: ContentErrorConfig? = null,
    val isBottomSheetOpen: Boolean = false,

    val document: DocumentUi? = null,
    val documentTypeUiName: String = "",
    val headerData: HeaderData? = null
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object Pop : Event()
    data object PrimaryButtonPressed : Event()
    data object DeleteDocumentPressed : Event()

    data object DismissError : Event()

    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(val isOpen: Boolean) : BottomSheet()

        sealed class Delete : BottomSheet() {
            data object PrimaryButtonPressed : Delete()
            data object SecondaryButtonPressed : Delete()
        }
    }
}


sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
        data class SwitchScreen(
            val screenRoute: String,
            val popUpToScreenRoute: String,
            val inclusive: Boolean
        ) : Navigation()
    }

    data object ShowBottomSheet : Effect()
    data object CloseBottomSheet : Effect()
}

@KoinViewModel
class DocumentDetailsViewModel(
    private val documentDetailsInteractor: DocumentDetailsInteractor,
    private val resourceProvider: ResourceProvider,
    @InjectedParam private val detailsType: IssuanceFlowUiConfig,
    @InjectedParam private val documentId: String,
    @InjectedParam private val documentType: String,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State = State(
        detailsType = detailsType,
        navigatableAction = getNavigatableAction(detailsType),
        onBackAction = getOnBackAction(detailsType),
        shouldShowPrimaryButton = shouldShowPrimaryButton(detailsType),
        hasCustomTopBar = hasCustomTopBar(detailsType),
        hasBottomPadding = hasBottomPadding(detailsType),
        detailsHaveBottomGradient = detailsHaveBottomGradient(detailsType),
    )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> getDocumentDetails(event)

            is Event.Pop -> {
                setState { copy(error = null) }
                setEffect { Effect.Navigation.Pop }
            }

            is Event.PrimaryButtonPressed -> {
                setEffect {
                    Effect.Navigation.SwitchScreen(
                        screenRoute = DashboardScreens.Dashboard.screenRoute,
                        popUpToScreenRoute = IssuanceScreens.DocumentDetails.screenRoute,
                        inclusive = true
                    )
                }
            }

            is Event.DeleteDocumentPressed -> {
                showBottomSheet()
            }

            is Event.BottomSheet.UpdateBottomSheetState -> {
                setState {
                    copy(isBottomSheetOpen = event.isOpen)
                }
            }

            is Event.BottomSheet.Delete.PrimaryButtonPressed -> {
                hideBottomSheet()
                deleteDocument(event)
            }

            is Event.BottomSheet.Delete.SecondaryButtonPressed -> {
                hideBottomSheet()
            }

            is Event.DismissError -> setState { copy(error = null) }
        }
    }

    private fun getDocumentDetails(event: Event) {
        setState {
            copy(
                isLoading = document == null,
                error = null
            )
        }

        viewModelScope.launch {
            documentDetailsInteractor.getDocumentDetails(
                documentId = documentId,
                documentType = documentType
            ).collect { response ->
                when (response) {
                    is DocumentDetailsInteractorPartialState.Success -> {
                        val documentUi = response.documentUi
                        val documentTypeUiName = documentUi.documentType.toUiName(resourceProvider)
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                document = documentUi,
                                documentTypeUiName = documentTypeUiName,
                                headerData = HeaderData(
                                    title = documentTypeUiName,
                                    subtitle = documentUi.userFullName.orEmpty(),
                                    base64Image = documentUi.documentImage,
                                    icon = AppIcons.IdStroke
                                )
                            )
                        }
                    }

                    is DocumentDetailsInteractorPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = ContentErrorConfig(
                                    onRetry = { setEvent(event) },
                                    errorSubTitle = response.error,
                                    onCancel = { setEvent(Event.Pop) }
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun deleteDocument(event: Event) {
        setState {
            copy(
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            documentDetailsInteractor.deleteDocument(
                documentId = documentId,
                documentType = documentType
            ).collect { response ->
                when (response) {
                    is DocumentDetailsInteractorDeleteDocumentPartialState.AllDocumentsDeleted -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = null
                            )
                        }

                        setEffect {
                            Effect.Navigation.SwitchScreen(
                                screenRoute = StartupScreens.Splash.screenRoute,
                                popUpToScreenRoute = DashboardScreens.Dashboard.screenRoute,
                                inclusive = true
                            )
                        }
                    }

                    is DocumentDetailsInteractorDeleteDocumentPartialState.SingleDocumentDeleted -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = null
                            )
                        }

                        setEffect {
                            Effect.Navigation.Pop
                        }
                    }

                    is DocumentDetailsInteractorDeleteDocumentPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = ContentErrorConfig(
                                    onRetry = { setEvent(event) },
                                    errorSubTitle = response.errorMessage,
                                    onCancel = { setEvent(Event.DismissError) }
                                )
                            )
                        }
                    }
                }
            }

        }
    }

    private fun showBottomSheet() {
        setEffect {
            Effect.ShowBottomSheet
        }
    }

    private fun hideBottomSheet() {
        setEffect {
            Effect.CloseBottomSheet
        }
    }

    private fun getNavigatableAction(detailsType: IssuanceFlowUiConfig): ScreenNavigateAction {
        return when (detailsType) {
            IssuanceFlowUiConfig.NO_DOCUMENT -> ScreenNavigateAction.NONE
            IssuanceFlowUiConfig.EXTRA_DOCUMENT -> ScreenNavigateAction.CANCELABLE
        }
    }

    private fun shouldShowPrimaryButton(detailsType: IssuanceFlowUiConfig): Boolean {
        return when (detailsType) {
            IssuanceFlowUiConfig.NO_DOCUMENT -> true
            IssuanceFlowUiConfig.EXTRA_DOCUMENT -> false
        }
    }

    private fun hasCustomTopBar(detailsType: IssuanceFlowUiConfig): Boolean {
        return when (detailsType) {
            IssuanceFlowUiConfig.NO_DOCUMENT -> false
            IssuanceFlowUiConfig.EXTRA_DOCUMENT -> true
        }
    }

    private fun hasBottomPadding(detailsType: IssuanceFlowUiConfig): Boolean {
        return when (detailsType) {
            IssuanceFlowUiConfig.NO_DOCUMENT -> true
            IssuanceFlowUiConfig.EXTRA_DOCUMENT -> false
        }
    }

    private fun detailsHaveBottomGradient(detailsType: IssuanceFlowUiConfig): Boolean {
        return when (detailsType) {
            IssuanceFlowUiConfig.NO_DOCUMENT -> true
            IssuanceFlowUiConfig.EXTRA_DOCUMENT -> false
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