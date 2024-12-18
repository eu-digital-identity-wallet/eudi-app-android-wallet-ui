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
import eu.europa.ec.commonfeature.ui.document_details.transformer.DocumentDetailsTransformer.transformToDocumentDetailsUi
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.issuancefeature.interactor.document.DocumentDetailsInteractor
import eu.europa.ec.issuancefeature.interactor.document.DocumentDetailsInteractorDeleteBookmarkPartialState
import eu.europa.ec.issuancefeature.interactor.document.DocumentDetailsInteractorDeleteDocumentPartialState
import eu.europa.ec.issuancefeature.interactor.document.DocumentDetailsInteractorPartialState
import eu.europa.ec.issuancefeature.interactor.document.DocumentDetailsInteractorRetrieveBookmarkPartialState
import eu.europa.ec.issuancefeature.interactor.document.DocumentDetailsInteractorStoreBookmarkPartialState
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.IconData
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.wrap.BottomSheetTextData
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.StartupScreens
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

data class State(
    val detailsType: IssuanceFlowUiConfig,
    val navigatableAction: ScreenNavigateAction,
    val onBackAction: (() -> Unit)? = null,
    val shouldShowActionButtons: Boolean,
    val hasCustomTopBar: Boolean,
    val hasBottomPadding: Boolean,
    val detailsHaveBottomGradient: Boolean,

    val isLoading: Boolean = true,
    val error: ContentErrorConfig? = null,
    val isBottomSheetOpen: Boolean = false,

    val document: DocumentUi? = null,
    val headline: String? = null,
    val documentDetailsSectionTitle: String? = null,
    val documentIssuerSectionTitle: String? = null,

    val isDocumentBookmarked: Boolean = false,
    val sensitiveInfoIcon: IconData = AppIcons.VisibilityOff,
    val isShowingFullUserInfo: Boolean = true,

    val sheetContent: DocumentDetailsBottomSheetContent
) : ViewState {
    val bookmarkIcon: IconData
        get() = if (isDocumentBookmarked) AppIcons.BookmarkFilled else AppIcons.Bookmark
}

sealed class Event : ViewEvent {
    data object Init : Event()
    data object Pop : Event()
    data object PrimaryButtonPressed : Event()
    data object SecondaryButtonPressed : Event()

    data object DismissError : Event()

    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(val isOpen: Boolean) : BottomSheet()

        sealed class Delete : BottomSheet() {
            data object PrimaryButtonPressed : Delete()
            data object SecondaryButtonPressed : Delete()
        }
    }

    data object ChangeContentVisibility : Event()
    data class BookmarkPressed(val isBookmarked: Boolean) : Event()
    data object IssuerCardPressed : Event()
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

sealed class DocumentDetailsBottomSheetContent {
    data object DeleteDocumentConfirmation : DocumentDetailsBottomSheetContent()

    data class BookmarkStoredInfo(val bottomSheetTextData: BottomSheetTextData) :
        DocumentDetailsBottomSheetContent()

    data class IssuerInfo(
        val bottomSheetTextData: BottomSheetTextData
    ) : DocumentDetailsBottomSheetContent()
}

@KoinViewModel
class DocumentDetailsViewModel(
    private val documentDetailsInteractor: DocumentDetailsInteractor,
    private val resourceProvider: ResourceProvider,
    @InjectedParam private val detailsType: IssuanceFlowUiConfig,
    @InjectedParam private val documentId: DocumentId,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State = State(
        detailsType = detailsType,
        navigatableAction = getNavigatableAction(detailsType),
        onBackAction = getOnBackAction(detailsType),
        shouldShowActionButtons = shouldShowActionButtons(detailsType),
        hasCustomTopBar = hasCustomTopBar(detailsType),
        hasBottomPadding = hasBottomPadding(detailsType),
        detailsHaveBottomGradient = detailsHaveBottomGradient(detailsType),
        documentDetailsSectionTitle = resourceProvider.getString(R.string.document_details_main_section_text),
        documentIssuerSectionTitle = resourceProvider.getString(R.string.document_details_issuer_section_text),
        sheetContent = DocumentDetailsBottomSheetContent.BookmarkStoredInfo(
            bottomSheetTextData = BottomSheetTextData(
                title = resourceProvider.getString(R.string.document_details_bottom_sheet_bookmark_info_title),
                message = resourceProvider.getString(R.string.document_details_bottom_sheet_bookmark_info_message)
            )
        )
    )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                getBookmarkStatus()
                getDocumentDetails(event)
            }

            is Event.Pop -> {
                setState { copy(error = null) }
                setEffect { Effect.Navigation.Pop }
            }

            is Event.PrimaryButtonPressed -> {
                // TODO: will redirect to transactions screen
            }

            is Event.SecondaryButtonPressed -> {
                showBottomSheet(sheetContent = DocumentDetailsBottomSheetContent.DeleteDocumentConfirmation)
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

            is Event.ChangeContentVisibility -> setState {
                copy(
                    isShowingFullUserInfo = isShowingFullUserInfo.not(),
                    sensitiveInfoIcon = AppIcons.VisibilityOff.takeIf { isShowingFullUserInfo.not() }
                        ?: AppIcons.Visibility
                )
            }

            is Event.BookmarkPressed -> {
                when (event.isBookmarked) {
                    false -> {
                        storeBookmark()
                    }

                    true -> {
                        deleteBookmark()
                    }
                }

                setState {
                    copy(
                        isDocumentBookmarked = isDocumentBookmarked.not(),
                    )
                }

                if (viewState.value.isDocumentBookmarked) {
                    val bottomSheetTextData = BottomSheetTextData(
                        title = resourceProvider.getString(R.string.document_details_bottom_sheet_bookmark_info_title),
                        message = resourceProvider.getString(R.string.document_details_bottom_sheet_bookmark_info_message)
                    )
                    showBottomSheet(
                        sheetContent = DocumentDetailsBottomSheetContent.BookmarkStoredInfo(
                            bottomSheetTextData = bottomSheetTextData
                        )
                    )
                }
            }

            is Event.IssuerCardPressed -> {
                val bottomSheetTextData = BottomSheetTextData(
                    title = resourceProvider.getString(R.string.document_details_bottom_sheet_badge_title),
                    message = resourceProvider.getString(R.string.document_details_bottom_sheet_badge_subtitle)
                )
                showBottomSheet(
                    sheetContent = DocumentDetailsBottomSheetContent.IssuerInfo(
                        bottomSheetTextData = bottomSheetTextData
                    )
                )
            }
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
            ).collect { response ->
                when (response) {
                    is DocumentDetailsInteractorPartialState.Success -> {
                        val documentUi =
                            response.documentDetailsDomain.transformToDocumentDetailsUi()
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                document = documentUi,
                                headline = documentUi.documentName,
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
                documentId = documentId
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

    private fun getBookmarkStatus() {
        viewModelScope.launch {
            documentDetailsInteractor.retrieveBookmark(documentId).collect {
                if (it is DocumentDetailsInteractorRetrieveBookmarkPartialState.Success) {
                    setState {
                        copy(
                            isDocumentBookmarked = true
                        )
                    }
                }
            }
        }
    }

    private fun storeBookmark() {
        viewModelScope.launch {
            documentDetailsInteractor.storeBookmark(documentId).collect {
                if (it is DocumentDetailsInteractorStoreBookmarkPartialState.Success) {
                    setState {
                        copy(
                            isDocumentBookmarked = true
                        )
                    }
                }
            }
        }
    }

    private fun deleteBookmark() {
        viewModelScope.launch {
            documentDetailsInteractor.deleteBookmark(documentId).collect {
                if (it is DocumentDetailsInteractorDeleteBookmarkPartialState.Success) {
                    setState {
                        copy(
                            isDocumentBookmarked = false
                        )
                    }
                }
            }
        }
    }

    private fun showBottomSheet(sheetContent: DocumentDetailsBottomSheetContent) {
        setState {
            copy(sheetContent = sheetContent)
        }
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
            IssuanceFlowUiConfig.EXTRA_DOCUMENT -> ScreenNavigateAction.BACKABLE
        }
    }

    private fun shouldShowActionButtons(detailsType: IssuanceFlowUiConfig): Boolean {
        return when (detailsType) {
            IssuanceFlowUiConfig.NO_DOCUMENT, IssuanceFlowUiConfig.EXTRA_DOCUMENT -> true
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