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

package eu.europa.ec.issuancefeature.ui.document.offer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import eu.europa.ec.businesslogic.extension.toUri
import eu.europa.ec.commonfeature.config.OfferCodeUiConfig
import eu.europa.ec.commonfeature.config.OfferUiConfig
import eu.europa.ec.commonfeature.config.PresentationMode
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.commonfeature.ui.request.model.DocumentItemUi
import eu.europa.ec.corelogic.di.getOrCreatePresentationScope
import eu.europa.ec.issuancefeature.interactor.document.DocumentOfferInteractor
import eu.europa.ec.issuancefeature.interactor.document.IssueDocumentsInteractorPartialState
import eu.europa.ec.issuancefeature.interactor.document.ResolveDocumentOfferInteractorPartialState
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.PresentationScreens
import eu.europa.ec.uilogic.navigation.helper.DeepLinkType
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.navigation.helper.hasDeepLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

data class State(
    val offerUiConfig: OfferUiConfig,

    val isLoading: Boolean = true,
    val error: ContentErrorConfig? = null,
    val isBottomSheetOpen: Boolean = false,
    val isInitialised: Boolean = false,

    val issuerName: String,
    val screenTitle: String,
    val screenSubtitle: String,
    val documents: List<DocumentItemUi> = emptyList(),
    val noDocument: Boolean = false,
    val txCodeLength: Int? = null
) : ViewState

sealed class Event : ViewEvent {
    data class Init(val deepLink: Uri?) : Event()
    data object Pop : Event()
    data object OnPause : Event()
    data object OnResumeIssuance : Event()
    data class OnDynamicPresentation(val uri: String) : Event()
    data object DismissError : Event()

    data class PrimaryButtonPressed(val context: Context) : Event()
    data object SecondaryButtonPressed : Event()

    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(val isOpen: Boolean) : BottomSheet()

        sealed class Cancel : BottomSheet() {
            data object PrimaryButtonPressed : Cancel()
            data object SecondaryButtonPressed : Cancel()
        }
    }
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(
            val screenRoute: String,
            val shouldPopToSelf: Boolean = true
        ) : Navigation()

        data class PopBackStackUpTo(
            val screenRoute: String,
            val inclusive: Boolean
        ) : Navigation()

        data object Pop : Navigation()

        data class DeepLink(
            val link: Uri
        ) : Navigation()
    }

    data object ShowBottomSheet : Effect()
    data object CloseBottomSheet : Effect()
}

@KoinViewModel
class DocumentOfferViewModel(
    private val documentOfferInteractor: DocumentOfferInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
    @InjectedParam private val offerSerializedConfig: String,
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State {
        val deserializedOfferUiConfig = uiSerializer.fromBase64(
            offerSerializedConfig,
            OfferUiConfig::class.java,
            OfferUiConfig.Parser
        ) ?: throw RuntimeException("OfferUiConfig:: is Missing or invalid")

        val issuerName = resourceProvider.getString(
            R.string.issuance_document_offer_default_issuer_name
        )

        return State(
            offerUiConfig = deserializedOfferUiConfig,
            issuerName = issuerName,
            screenTitle = calculateScreenTitle(issuerName = issuerName),
            screenSubtitle = resourceProvider.getString(R.string.issuance_document_offer_subtitle),
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                if (viewState.value.documents.isEmpty()) {
                    resolveDocumentOffer(
                        offerUri = viewState.value.offerUiConfig.offerURI,
                        deepLink = event.deepLink
                    )
                } else {
                    handleDeepLink(event.deepLink)
                }
            }

            is Event.Pop -> {
                setState { copy(error = null) }
                setEffect { Effect.Navigation.Pop }
            }

            is Event.DismissError -> {
                setState { copy(error = null) }
            }

            is Event.PrimaryButtonPressed -> {
                issueDocuments(
                    context = event.context,
                    offerUri = viewState.value.offerUiConfig.offerURI,
                    issuerName = viewState.value.issuerName,
                    onSuccessNavigation = viewState.value.offerUiConfig.onSuccessNavigation,
                    txCodeLength = viewState.value.txCodeLength
                )
            }

            is Event.SecondaryButtonPressed -> {
                showBottomSheet()
            }

            is Event.BottomSheet.UpdateBottomSheetState -> {
                setState {
                    copy(isBottomSheetOpen = event.isOpen)
                }
            }

            is Event.BottomSheet.Cancel.PrimaryButtonPressed -> {
                hideBottomSheet()
            }

            is Event.BottomSheet.Cancel.SecondaryButtonPressed -> {
                hideBottomSheet()
                doNavigation(viewState.value.offerUiConfig.onCancelNavigation)
            }

            is Event.OnPause -> {
                if (viewState.value.isInitialised) {
                    setState { copy(isLoading = false) }
                }
            }

            is Event.OnResumeIssuance -> setState {
                copy(isLoading = true)
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
                                                IssuanceScreens.DocumentOffer.screenRoute
                                            )
                                        ),
                                        RequestUriConfig
                                    )
                                )
                            )
                        ),
                        shouldPopToSelf = false
                    )
                }
            }
        }
    }

    private fun resolveDocumentOffer(offerUri: String, deepLink: Uri? = null) {
        setState {
            copy(
                isLoading = documents.isEmpty(),
                error = null
            )
        }
        viewModelScope.launch {
            documentOfferInteractor.resolveDocumentOffer(
                offerUri = offerUri
            ).collect { response ->
                when (response) {
                    is ResolveDocumentOfferInteractorPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                isInitialised = false,
                                error = ContentErrorConfig(
                                    errorSubTitle = response.errorMessage,
                                    onCancel = {
                                        setEvent(Event.DismissError)
                                        doNavigation(viewState.value.offerUiConfig.onCancelNavigation)
                                    }
                                )
                            )
                        }
                    }

                    is ResolveDocumentOfferInteractorPartialState.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                documents = response.documents,
                                isInitialised = true,
                                noDocument = false,
                                issuerName = response.issuerName,
                                screenTitle = calculateScreenTitle(issuerName = response.issuerName),
                                txCodeLength = response.txCodeLength
                            )
                        }

                        handleDeepLink(deepLink)
                    }

                    is ResolveDocumentOfferInteractorPartialState.NoDocument -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                documents = emptyList(),
                                isInitialised = true,
                                noDocument = true,
                                issuerName = response.issuerName,
                                screenTitle = calculateScreenTitle(issuerName = response.issuerName)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun issueDocuments(
        context: Context,
        offerUri: String,
        issuerName: String,
        onSuccessNavigation: ConfigNavigation,
        txCodeLength: Int?
    ) {
        viewModelScope.launch {

            txCodeLength?.let {
                navigateToOfferCodeScreen(
                    offerUri,
                    issuerName,
                    txCodeLength,
                    onSuccessNavigation
                )
                return@launch
            }

            setState {
                copy(
                    isLoading = true,
                    error = null
                )
            }

            documentOfferInteractor.issueDocuments(
                offerUri = offerUri,
                issuerName = issuerName,
                navigation = onSuccessNavigation
            ).collect { response ->
                when (response) {
                    is IssueDocumentsInteractorPartialState.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = ContentErrorConfig(
                                    errorSubTitle = response.errorMessage,
                                    onCancel = { setEvent(Event.DismissError) }
                                )
                            )
                        }
                    }

                    is IssueDocumentsInteractorPartialState.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                            )
                        }

                        goToSuccessScreen(route = response.successRoute)
                    }

                    is IssueDocumentsInteractorPartialState.UserAuthRequired -> {
                        documentOfferInteractor.handleUserAuthentication(
                            context = context,
                            crypto = response.crypto,
                            resultHandler = response.resultHandler
                        )
                    }
                }
            }
        }
    }

    private fun goToSuccessScreen(route: String) {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = route
            )
        }
    }

    private fun doNavigation(navigation: ConfigNavigation) {
        val navigationEffect: Effect.Navigation = when (val nav = navigation.navigationType) {
            is NavigationType.PopTo -> {
                Effect.Navigation.PopBackStackUpTo(
                    screenRoute = nav.screen.screenRoute,
                    inclusive = false
                )
            }

            is NavigationType.PushScreen -> {
                Effect.Navigation.SwitchScreen(
                    generateComposableNavigationLink(
                        screen = nav.screen,
                        arguments = generateComposableArguments(nav.arguments),
                    )
                )
            }

            is NavigationType.Deeplink -> Effect.Navigation.DeepLink(
                nav.link.toUri()
            )

            is NavigationType.Pop, NavigationType.Finish -> Effect.Navigation.Pop

            is NavigationType.PushRoute -> Effect.Navigation.SwitchScreen(nav.route)
        }

        setEffect {
            navigationEffect
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

    private fun calculateScreenTitle(issuerName: String): String {
        return resourceProvider.getString(
            R.string.issuance_document_offer_title,
            issuerName
        )
    }

    private fun navigateToOfferCodeScreen(
        offerUri: String,
        issuerName: String,
        txCodeLength: Int,
        onSuccessNavigation: ConfigNavigation
    ) {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = generateComposableNavigationLink(
                    IssuanceScreens.DocumentOfferCode,
                    getNavigateOfferCodeScreenArguments(
                        offerUri,
                        issuerName,
                        txCodeLength,
                        onSuccessNavigation
                    )
                ),
                shouldPopToSelf = false
            )
        }
    }

    private fun getNavigateOfferCodeScreenArguments(
        offerUri: String,
        issuerName: String,
        txCodeLength: Int,
        onSuccessNavigation: ConfigNavigation
    ): String {
        return generateComposableArguments(
            mapOf(
                OfferCodeUiConfig.serializedKeyName to uiSerializer.toBase64(
                    OfferCodeUiConfig(
                        offerURI = offerUri,
                        txCodeLength = txCodeLength,
                        issuerName = issuerName,
                        onSuccessNavigation = onSuccessNavigation
                    ),
                    OfferCodeUiConfig.Parser
                ).orEmpty()
            )
        )
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