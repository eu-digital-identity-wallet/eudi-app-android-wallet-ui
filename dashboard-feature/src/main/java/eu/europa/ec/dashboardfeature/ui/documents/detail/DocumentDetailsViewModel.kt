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

package eu.europa.ec.dashboardfeature.ui.documents.detail

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.commonfeature.config.PresentationMode
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.dashboardfeature.interactor.DocumentDetailsInteractor
import eu.europa.ec.dashboardfeature.interactor.DocumentDetailsInteractorDeleteBookmarkPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentDetailsInteractorDeleteDocumentPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentDetailsInteractorIssuancePartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentDetailsInteractorPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentDetailsInteractorStoreBookmarkPartialState
import eu.europa.ec.dashboardfeature.ui.documents.detail.DocumentDetailsBottomSheetContent.BookmarkRemovedInfo
import eu.europa.ec.dashboardfeature.ui.documents.detail.DocumentDetailsBottomSheetContent.BookmarkStoredInfo
import eu.europa.ec.dashboardfeature.ui.documents.detail.DocumentDetailsBottomSheetContent.TrustedRelyingPartyInfo
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentDetailsUi
import eu.europa.ec.dashboardfeature.ui.documents.detail.transformer.DocumentDetailsTransformer.transformToDocumentDetailsUi
import eu.europa.ec.dashboardfeature.ui.documents.model.DocumentCredentialsInfoUi
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.IssuerDetailsCardDataUi
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.wrap.BottomSheetTextDataUi
import eu.europa.ec.uilogic.extension.toggleExpansionState
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.PresentationScreens
import eu.europa.ec.uilogic.navigation.StartupScreens
import eu.europa.ec.uilogic.navigation.helper.DeepLinkType
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.navigation.helper.hasDeepLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

data class State(
    val isLoading: Boolean = true,
    val error: ContentErrorConfig? = null,
    val isBottomSheetOpen: Boolean = false,

    val documentDetailsUi: DocumentDetailsUi? = null,
    val title: String? = null,
    val issuerDetails: IssuerDetailsCardDataUi? = null,
    val documentCredentialsInfoUi: DocumentCredentialsInfoUi? = null,

    val isDocumentBookmarked: Boolean = false,
    val hideSensitiveContent: Boolean = true,

    val notifyOnAuthenticationFailure: Boolean = false,

    val sheetContent: DocumentDetailsBottomSheetContent = DocumentDetailsBottomSheetContent.DeleteDocumentConfirmation,
) : ViewState

sealed class Event : ViewEvent {
    data class Init(val deepLink: Uri?) : Event()
    data object Pop : Event()
    data class ClaimClicked(val itemId: String) : Event()
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
    data object BookmarkPressed : Event()
    data object OnBookmarkStored : Event()
    data object OnBookmarkRemoved : Event()
    data object IssuerCardPressed : Event()
    data class OnRevocationStatusChanged(val revokedIds: List<String>) : Event()

    sealed class IssuerDetails : Event() {
        data object OnExpandedStateChanged : IssuerDetails()
        data class OnActionButtonClicked(val context: Context) : IssuerDetails()
    }

    data object OnPause : Event()
    data class OnResumeIssuance(val uri: String) : Event()
    data class OnDynamicPresentation(val uri: String) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {

        data object Pop : Navigation()

        data class SwitchScreen(
            val screenRoute: String,
            val popUpToScreenRoute: String?,
            val inclusive: Boolean?
        ) : Navigation()

        data class DeepLink(
            val link: Uri,
            val routeToPop: String? = null
        ) : Navigation()
    }

    data object ShowBottomSheet : Effect()
    data object CloseBottomSheet : Effect()

    data object BookmarkStored : Effect()
    data object BookmarkRemoved : Effect()
}

sealed class DocumentDetailsBottomSheetContent {
    data object DeleteDocumentConfirmation : DocumentDetailsBottomSheetContent()

    data class BookmarkStoredInfo(
        val bottomSheetTextData: BottomSheetTextDataUi
    ) : DocumentDetailsBottomSheetContent()

    data class BookmarkRemovedInfo(
        val bottomSheetTextData: BottomSheetTextDataUi
    ) : DocumentDetailsBottomSheetContent()

    data class TrustedRelyingPartyInfo(
        val bottomSheetTextData: BottomSheetTextDataUi
    ) : DocumentDetailsBottomSheetContent()
}

@KoinViewModel
class DocumentDetailsViewModel(
    private val documentDetailsInteractor: DocumentDetailsInteractor,
    private val uiSerializer: UiSerializer,
    private val resourceProvider: ResourceProvider,
    @InjectedParam private val documentId: DocumentId,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State = State()

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                if (viewState.value.documentDetailsUi == null) {
                    getDocumentDetails(event)
                } else {
                    handleDeepLink(event.deepLink)
                }
            }

            is Event.Pop -> {
                setState { copy(error = null) }
                setEffect { Effect.Navigation.Pop }
            }

            is Event.ClaimClicked -> onClaimClicked(event.itemId)

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
                    hideSensitiveContent = !hideSensitiveContent,
                )
            }

            is Event.BookmarkPressed -> {
                if (!viewState.value.isDocumentBookmarked) {
                    storeBookmark()
                } else {
                    deleteBookmark()
                }
            }

            is Event.OnBookmarkStored -> {
                showBottomSheet(
                    sheetContent = BookmarkStoredInfo(
                        bottomSheetTextData = getBookmarkStoredBottomSheetTextData()
                    )
                )
            }

            is Event.OnBookmarkRemoved -> {
                showBottomSheet(
                    sheetContent = BookmarkRemovedInfo(
                        bottomSheetTextData = getBookmarkRemovedBottomSheetTextData()
                    )
                )
            }

            is Event.IssuerCardPressed -> {
                showBottomSheet(
                    sheetContent = TrustedRelyingPartyInfo(
                        bottomSheetTextData = getTrustedRelyingPartyBottomSheetTextData()
                    )
                )
            }

            is Event.OnRevocationStatusChanged -> {
                getDocumentDetails(event)
            }

            is Event.IssuerDetails.OnExpandedStateChanged -> toggleIssuerDetailsCardExpansionState()

            is Event.IssuerDetails.OnActionButtonClicked -> {
                viewState.value.issuerDetails?.documentState?.let { safeDocumentState ->
                    handleIssuerDetailsAction(
                        event = event,
                        context = event.context,
                        documentState = safeDocumentState
                    )
                }
            }

            is Event.OnDynamicPresentation -> {
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
                                                DashboardScreens.DocumentDetails.screenRoute
                                            )
                                        ),
                                        RequestUriConfig.Parser
                                    )
                                )
                            )
                        ),
                        popUpToScreenRoute = DashboardScreens.DocumentDetails.screenRoute,
                        inclusive = false
                    )
                }
            }

            is Event.OnPause -> {
                viewState.value.documentDetailsUi?.let {
                    setState { copy(isLoading = false) }
                }
            }

            is Event.OnResumeIssuance -> {
                setState {
                    copy(isLoading = true)
                }
                documentDetailsInteractor.resumeOpenId4VciWithAuthorization(event.uri)
            }
        }
    }

    private fun getDocumentDetails(event: Event) {
        setState {
            copy(
                isLoading = documentDetailsUi == null,
                error = null
            )
        }

        viewModelScope.launch {
            documentDetailsInteractor.getDocumentDetails(
                documentId = documentId,
                wasIssuerDetailsExpanded = viewState.value.issuerDetails?.isExpanded
            ).collect { response ->
                when (response) {
                    is DocumentDetailsInteractorPartialState.Success -> {
                        val documentDetailsUi = response.documentDetailsDomain
                            .transformToDocumentDetailsUi()

                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                documentDetailsUi = documentDetailsUi,
                                documentCredentialsInfoUi = response.documentCredentialsInfoUi,
                                title = documentDetailsUi.documentName,
                                isDocumentBookmarked = response.documentIsBookmarked,
                                issuerDetails = response.issuerDetails,
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

    private fun onClaimClicked(itemId: String) {
        val currentItem = viewState.value.documentDetailsUi
        if (currentItem != null) {
            val updatedDocumentClaims = currentItem.documentClaims.toggleExpansionState(itemId)

            setState {
                copy(
                    documentDetailsUi = currentItem.copy(
                        documentClaims = updatedDocumentClaims
                    )
                )
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

    private fun storeBookmark() {
        viewModelScope.launch {
            documentDetailsInteractor.storeBookmark(documentId).collect {
                if (it is DocumentDetailsInteractorStoreBookmarkPartialState.Success) {
                    setState {
                        copy(
                            isDocumentBookmarked = true
                        )
                    }

                    setEffect {
                        Effect.BookmarkStored
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

                    setEffect {
                        Effect.BookmarkRemoved
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

    private fun getBookmarkStoredBottomSheetTextData(): BottomSheetTextDataUi {
        return BottomSheetTextDataUi(
            title = resourceProvider.getString(R.string.document_details_bottom_sheet_bookmark_info_title),
            message = resourceProvider.getString(R.string.document_details_bottom_sheet_bookmark_info_message)
        )
    }

    private fun getBookmarkRemovedBottomSheetTextData(): BottomSheetTextDataUi {
        return BottomSheetTextDataUi(
            title = resourceProvider.getString(R.string.document_details_bottom_sheet_bookmark_removed_info_title),
            message = resourceProvider.getString(R.string.document_details_bottom_sheet_bookmark_removed_info_message)
        )
    }

    private fun getTrustedRelyingPartyBottomSheetTextData(): BottomSheetTextDataUi {
        return BottomSheetTextDataUi(
            title = resourceProvider.getString(R.string.document_details_bottom_sheet_badge_title),
            message = resourceProvider.getString(R.string.document_details_bottom_sheet_badge_subtitle)
        )
    }

    private fun reIssueDocument(
        event: Event,
        context: Context,
        document: DocumentDetailsUi
    ) {

        setState {
            copy(
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            documentDetailsInteractor.reIssueDocument(
                documentId = document.documentId,
                issuerId = document.issuerId
            ).collect {
                when (it) {
                    is DocumentDetailsInteractorIssuancePartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = ContentErrorConfig(
                                    onRetry = { setEvent(event) },
                                    errorSubTitle = it.errorMessage,
                                    onCancel = { setEvent(Event.DismissError) }
                                )
                            )
                        }
                    }

                    is DocumentDetailsInteractorIssuancePartialState.Success -> {
                        setEffect {
                            Effect.Navigation.Pop
                        }
                    }

                    is DocumentDetailsInteractorIssuancePartialState.UserAuthRequired -> {
                        documentDetailsInteractor.handleUserAuth(
                            context = context,
                            crypto = it.crypto,
                            notifyOnAuthenticationFailure = viewState.value.notifyOnAuthenticationFailure,
                            resultHandler = DeviceAuthenticationResult(
                                onAuthenticationSuccess = {
                                    it.resultHandler.onAuthenticationSuccess()
                                },
                                onAuthenticationError = {
                                    it.resultHandler.onAuthenticationError()
                                }
                            )
                        )
                    }
                }
            }
        }
    }

    private fun toggleIssuerDetailsCardExpansionState() {
        setState {
            copy(
                issuerDetails = issuerDetails?.copy(
                    isExpanded = !issuerDetails.isExpanded
                )
            )
        }
    }

    private fun handleIssuerDetailsAction(
        event: Event,
        context: Context,
        documentState: IssuerDetailsCardDataUi.DocumentState
    ) {
        when (documentState) {
            is IssuerDetailsCardDataUi.DocumentState.Issued -> {
                viewState.value.documentDetailsUi?.let { safeDocumentDetailsUi ->
                    reIssueDocument(
                        event = event,
                        context = context,
                        document = safeDocumentDetailsUi
                    )
                }
            }

            is IssuerDetailsCardDataUi.DocumentState.Revoked -> {
                // No-op
            }
        }
    }

    private fun handleDeepLink(deepLinkUri: Uri?) {
        deepLinkUri?.let { uri ->
            hasDeepLink(uri)?.let {
                when (it.type) {

                    DeepLinkType.EXTERNAL -> {
                        setEffect {
                            Effect.Navigation.DeepLink(uri)
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}