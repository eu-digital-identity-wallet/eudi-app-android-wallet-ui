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

package eu.europa.ec.dashboardfeature.ui.documents.detail

import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.model.DocumentDetailsUi
import eu.europa.ec.commonfeature.ui.document_details.transformer.DocumentDetailsTransformer.transformToDocumentDetailsUi
import eu.europa.ec.dashboardfeature.interactor.DocumentDetailsInteractor
import eu.europa.ec.dashboardfeature.interactor.DocumentDetailsInteractorDeleteBookmarkPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentDetailsInteractorDeleteDocumentPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentDetailsInteractorPartialState
import eu.europa.ec.dashboardfeature.interactor.DocumentDetailsInteractorStoreBookmarkPartialState
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.wrap.BottomSheetTextData
import eu.europa.ec.uilogic.extension.toggleExpansionState
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.StartupScreens
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.net.URI

data class State(
    val isLoading: Boolean = true,
    val error: ContentErrorConfig? = null,
    val isBottomSheetOpen: Boolean = false,
    val isRevoked: Boolean = false,

    val documentDetailsUi: DocumentDetailsUi? = null,
    val title: String? = null,
    val issuerName: String? = null,
    val issuerLogo: URI? = null,
    val documentDetailsSectionTitle: String,
    val documentIssuerSectionTitle: String,

    val isDocumentBookmarked: Boolean = false,
    val hideSensitiveContent: Boolean = true,

    val sheetContent: DocumentDetailsBottomSheetContent = DocumentDetailsBottomSheetContent.DeleteDocumentConfirmation,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
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

    data object BookmarkStored : Effect()
    data object BookmarkRemoved : Effect()
}

sealed class DocumentDetailsBottomSheetContent {
    data object DeleteDocumentConfirmation : DocumentDetailsBottomSheetContent()

    data class BookmarkStoredInfo(
        val bottomSheetTextData: BottomSheetTextData
    ) : DocumentDetailsBottomSheetContent()

    data class BookmarkRemovedInfo(
        val bottomSheetTextData: BottomSheetTextData
    ) : DocumentDetailsBottomSheetContent()

    data class TrustedRelyingPartyInfo(
        val bottomSheetTextData: BottomSheetTextData
    ) : DocumentDetailsBottomSheetContent()
}

@KoinViewModel
class DocumentDetailsViewModel(
    private val documentDetailsInteractor: DocumentDetailsInteractor,
    private val resourceProvider: ResourceProvider,
    @InjectedParam private val documentId: DocumentId,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State = State(
        documentDetailsSectionTitle = resourceProvider.getString(R.string.document_details_main_section_text),
        documentIssuerSectionTitle = resourceProvider.getString(R.string.document_details_issuer_section_text),
    )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> getDocumentDetails(event)

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
                    sheetContent = DocumentDetailsBottomSheetContent.BookmarkStoredInfo(
                        bottomSheetTextData = getBookmarkStoredBottomSheetTextData()
                    )
                )
            }

            is Event.OnBookmarkRemoved -> {
                showBottomSheet(
                    sheetContent = DocumentDetailsBottomSheetContent.BookmarkRemovedInfo(
                        bottomSheetTextData = getBookmarkRemovedBottomSheetTextData()
                    )
                )
            }

            is Event.IssuerCardPressed -> {
                showBottomSheet(
                    sheetContent = DocumentDetailsBottomSheetContent.TrustedRelyingPartyInfo(
                        bottomSheetTextData = getTrustedRelyingPartyBottomSheetTextData()
                    )
                )
            }

            is Event.OnRevocationStatusChanged -> {
                setState {
                    copy(
                        isRevoked = event.revokedIds.contains(documentId)
                    )
                }
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
                                title = documentDetailsUi.documentName,
                                isDocumentBookmarked = response.documentIsBookmarked,
                                isRevoked = response.isRevoked,
                                issuerName = response.issuerName,
                                issuerLogo = response.issuerLogo
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

    private fun getBookmarkStoredBottomSheetTextData(): BottomSheetTextData {
        return BottomSheetTextData(
            title = resourceProvider.getString(R.string.document_details_bottom_sheet_bookmark_info_title),
            message = resourceProvider.getString(R.string.document_details_bottom_sheet_bookmark_info_message)
        )
    }

    private fun getBookmarkRemovedBottomSheetTextData(): BottomSheetTextData {
        return BottomSheetTextData(
            title = resourceProvider.getString(R.string.document_details_bottom_sheet_bookmark_removed_info_title),
            message = resourceProvider.getString(R.string.document_details_bottom_sheet_bookmark_removed_info_message)
        )
    }

    private fun getTrustedRelyingPartyBottomSheetTextData(): BottomSheetTextData {
        return BottomSheetTextData(
            title = resourceProvider.getString(R.string.document_details_bottom_sheet_badge_title),
            message = resourceProvider.getString(R.string.document_details_bottom_sheet_badge_subtitle)
        )
    }
}