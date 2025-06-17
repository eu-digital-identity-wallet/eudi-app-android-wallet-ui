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

package eu.europa.ec.issuancefeature.ui.document.code

import android.content.Context
import androidx.lifecycle.viewModelScope
import eu.europa.ec.commonfeature.config.IssuanceSuccessUiConfig
import eu.europa.ec.commonfeature.config.OfferCodeUiConfig
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.issuancefeature.interactor.document.DocumentOfferInteractor
import eu.europa.ec.issuancefeature.interactor.document.IssueDocumentsInteractorPartialState
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

private typealias PinCode = String

data class State(
    val offerCodeUiConfig: OfferCodeUiConfig,

    val isLoading: Boolean = false,
    val error: ContentErrorConfig? = null,
    val notifyOnAuthenticationFailure: Boolean = false,

    val screenTitle: String,
    val screenSubtitle: String
) : ViewState

sealed class Event : ViewEvent {
    data object Pop : Event()
    data object DismissError : Event()
    data class OnPinChange(val code: PinCode, val context: Context) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(
            val screenRoute: String
        ) : Navigation()

        data object Pop : Navigation()
    }
}

@KoinViewModel
class DocumentOfferCodeViewModel(
    private val documentOfferInteractor: DocumentOfferInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
    @InjectedParam private val offerCodeSerializedConfig: String
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State {
        val deserializedOfferCodeUiConfig = uiSerializer.fromBase64(
            offerCodeSerializedConfig,
            OfferCodeUiConfig::class.java,
            OfferCodeUiConfig.Parser
        ) ?: throw RuntimeException("OfferCodeUiConfig:: is Missing or invalid")
        return State(
            offerCodeUiConfig = deserializedOfferCodeUiConfig,
            screenTitle = calculateScreenTitle(issuerName = deserializedOfferCodeUiConfig.issuerName),
            screenSubtitle = calculateScreenCaption(txCodeLength = deserializedOfferCodeUiConfig.txCodeLength)
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Pop -> {
                setState { copy(error = null) }
                setEffect { Effect.Navigation.Pop }
            }

            is Event.DismissError -> {
                setState { copy(error = null) }
            }

            is Event.OnPinChange -> {
                if (event.code.isPinValid()) {
                    issueDocuments(
                        event.context,
                        event.code
                    )
                }
            }
        }
    }

    private fun issueDocuments(context: Context, pinCode: PinCode) {
        viewModelScope.launch {

            setState {
                copy(
                    isLoading = true,
                    error = null
                )
            }

            documentOfferInteractor.issueDocuments(
                offerUri = viewState.value.offerCodeUiConfig.offerURI,
                issuerName = viewState.value.offerCodeUiConfig.issuerName,
                navigation = viewState.value.offerCodeUiConfig.onSuccessNavigation,
                txCode = pinCode
            ).collect { response ->
                when (response) {
                    is IssueDocumentsInteractorPartialState.Failure -> setState {
                        copy(
                            isLoading = false,
                            error = ContentErrorConfig(
                                errorSubTitle = response.errorMessage,
                                onCancel = { setEvent(Event.DismissError) }
                            )
                        )
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
                            onSuccessNavigation = viewState.value.offerCodeUiConfig.onSuccessNavigation,
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

    private fun calculateScreenTitle(issuerName: String): String = resourceProvider.getString(
        R.string.issuance_code_title,
        issuerName
    )

    private fun calculateScreenCaption(txCodeLength: Int): String =
        resourceProvider.getString(R.string.issuance_code_caption, txCodeLength)

    private fun PinCode.isPinValid(): Boolean =
        this.length == viewState.value.offerCodeUiConfig.txCodeLength
}