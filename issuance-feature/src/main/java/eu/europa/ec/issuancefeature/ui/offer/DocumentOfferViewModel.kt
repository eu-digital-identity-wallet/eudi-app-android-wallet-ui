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

package eu.europa.ec.issuancefeature.ui.offer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import eu.europa.ec.businesslogic.extension.ifEmptyOrNull
import eu.europa.ec.businesslogic.extension.toUri
import eu.europa.ec.commonfeature.config.IssuanceSuccessUiConfig
import eu.europa.ec.commonfeature.config.OfferCodeUiConfig
import eu.europa.ec.commonfeature.config.OfferUiConfig
import eu.europa.ec.commonfeature.config.PresentationMode
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.corelogic.di.getOrCreatePresentationScope
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.issuancefeature.interactor.DocumentOfferInteractor
import eu.europa.ec.issuancefeature.interactor.IssueDocumentsInteractorPartialState
import eu.europa.ec.issuancefeature.interactor.ResolveDocumentOfferInteractorPartialState
import eu.europa.ec.issuancefeature.ui.offer.transformer.DocumentOfferTransformer.toListItemDataList
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.RelyingPartyData
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.content.ContentHeaderConfig
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
import java.net.URI

data class State(
    val offerUiConfig: OfferUiConfig,

    val isLoading: Boolean = true,
    val headerConfig: ContentHeaderConfig,
    val error: ContentErrorConfig? = null,
    val isInitialised: Boolean = false,
    val notifyOnAuthenticationFailure: Boolean = false,

    val documents: List<ListItemData> = emptyList(),
    val noDocument: Boolean = false,
    val txCodeLength: Int? = null,
) : ViewState

sealed class Event : ViewEvent {
    data class Init(val deepLink: Uri?) : Event()
    data object BackButtonPressed : Event()
    data object OnPause : Event()
    data class OnResumeIssuance(val uri: String) : Event()
    data class OnDynamicPresentation(val uri: String) : Event()
    data object DismissError : Event()

    data class StickyButtonPressed(val context: Context) : Event()
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
            val link: Uri,
            val routeToPop: String? = null
        ) : Navigation()
    }
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

        return State(
            offerUiConfig = deserializedOfferUiConfig,
            headerConfig = getInitialHeaderConfig()
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

            is Event.BackButtonPressed -> {
                setState { copy(error = null) }
                doNavigation(viewState.value.offerUiConfig.onCancelNavigation)
            }

            is Event.DismissError -> {
                setState { copy(error = null) }
            }

            is Event.StickyButtonPressed -> {
                issueDocuments(
                    context = event.context,
                    offerUri = viewState.value.offerUiConfig.offerURI,
                    issuerName = viewState.value.headerConfig.relyingPartyData?.name.ifEmptyOrNull(
                        default = resourceProvider.getString(R.string.issuance_document_offer_relying_party_default_name)
                    ),
                    onSuccessNavigation = viewState.value.offerUiConfig.onSuccessNavigation,
                    txCodeLength = viewState.value.txCodeLength
                )
            }

            is Event.OnPause -> {
                if (viewState.value.isInitialised) {
                    setState { copy(isLoading = false) }
                }
            }

            is Event.OnResumeIssuance -> {
                setState {
                    copy(isLoading = true)
                }
                documentOfferInteractor.resumeOpenId4VciWithAuthorization(event.uri)
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
                                documents = response.documents.toListItemDataList(),
                                isInitialised = true,
                                noDocument = false,
                                txCodeLength = response.txCodeLength,
                                headerConfig = headerConfig.copy(
                                    relyingPartyData = getHeaderConfigIssuerData(
                                        issuerName = response.issuerName,
                                        issuerLogo = response.issuerLogo,
                                    )
                                ),
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
                                headerConfig = headerConfig.copy(
                                    relyingPartyData = getHeaderConfigIssuerData(
                                        issuerName = response.issuerName,
                                        issuerLogo = response.issuerLogo,
                                    )
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun getInitialHeaderConfig(): ContentHeaderConfig {
        return ContentHeaderConfig(
            description = resourceProvider.getString(R.string.issuance_document_offer_description),
            mainText = resourceProvider.getString(R.string.issuance_document_offer_header_main_text),
            relyingPartyData = RelyingPartyData(
                isVerified = false,
                name = resourceProvider.getString(R.string.issuance_document_offer_relying_party_default_name),
                description = resourceProvider.getString(R.string.issuance_document_offer_relying_party_description)
            )
        )
    }

    private fun getHeaderConfigIssuerData(
        issuerName: String,
        issuerLogo: URI?,
    ): RelyingPartyData {
        return RelyingPartyData(
            logo = issuerLogo,
            isVerified = false,
            name = issuerName,
            description = resourceProvider.getString(R.string.issuance_document_offer_relying_party_description)
        )
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

                        goToDocumentIssuanceSuccessScreen(
                            documentIds = response.documentIds,
                            onSuccessNavigation = onSuccessNavigation,
                        )
                    }

                    is IssueDocumentsInteractorPartialState.DeferredSuccess -> {
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
                            notifyOnAuthenticationFailure = viewState.value.notifyOnAuthenticationFailure,
                            resultHandler = response.resultHandler
                        )
                    }
                }
            }
        }
    }

    private fun goToDocumentIssuanceSuccessScreen(
        documentIds: List<DocumentId>,
        onSuccessNavigation: ConfigNavigation,
    ) {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = generateComposableNavigationLink(
                    screen = IssuanceScreens.DocumentIssuanceSuccess,
                    arguments = generateComposableArguments(
                        mapOf(
                            IssuanceSuccessUiConfig.serializedKeyName to uiSerializer.toBase64(
                                model = IssuanceSuccessUiConfig(
                                    documentIds = documentIds,
                                    onSuccessNavigation = onSuccessNavigation,
                                ),
                                parser = IssuanceSuccessUiConfig.Parser
                            ).orEmpty()
                        )
                    )
                )
            )
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
                nav.link.toUri(),
                nav.routeToPop
            )

            is NavigationType.Pop, NavigationType.Finish -> Effect.Navigation.Pop

            is NavigationType.PushRoute -> Effect.Navigation.SwitchScreen(nav.route)
        }

        setEffect {
            navigationEffect
        }
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