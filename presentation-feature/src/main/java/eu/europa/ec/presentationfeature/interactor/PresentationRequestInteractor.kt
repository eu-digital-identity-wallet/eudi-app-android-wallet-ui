/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.presentationfeature.interactor

import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.commonfeature.config.toDomainConfig
import eu.europa.ec.commonfeature.interactor.ScopedPresentationInteractor
import eu.europa.ec.commonfeature.interactor.ScopedPresentationInteractorDelegate
import eu.europa.ec.commonfeature.ui.request.model.RequestCombinationUi
import eu.europa.ec.commonfeature.ui.request.transformer.RequestTransformer
import eu.europa.ec.corelogic.controller.TransferEventPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.controller.WalletCorePresentationController
import eu.europa.ec.corelogic.model.PresentationCombinationDomain
import eu.europa.ec.corelogic.model.PresentationTransactionDataDomain
import eu.europa.ec.corelogic.model.RelyingPartyDomain
import eu.europa.ec.corelogic.model.overaskedClaimsOrEmpty
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.config.ConfigUILogic
import eu.europa.ec.uilogic.navigation.helper.IntentAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

sealed class PresentationRequestInteractorPartialState {
    data class Success(
        val relyingParty: RelyingPartyDomain,
        val combinationsUi: List<RequestCombinationUi>,
        val claimsAreSelectable: Boolean,
    ) : PresentationRequestInteractorPartialState()

    data class NoData(
        val relyingParty: RelyingPartyDomain,
    ) : PresentationRequestInteractorPartialState()

    /** The wallet declined the request, for [reason]; nothing can be shared. */
    data class Declined(
        val relyingParty: RelyingPartyDomain,
        val reason: String,
    ) : PresentationRequestInteractorPartialState()

    data class Failure(val error: String) : PresentationRequestInteractorPartialState()
    data object VerifierNotTrusted : PresentationRequestInteractorPartialState()
    data object Disconnect : PresentationRequestInteractorPartialState()
}

interface PresentationRequestInteractor : ScopedPresentationInteractor {
    fun getRequestDocuments(): Flow<PresentationRequestInteractorPartialState>
    fun stopPresentation()
    fun updateRequestedDocuments(selectedCombination: RequestCombinationUi?)
    fun setConfig(config: RequestUriConfig, intentAction: IntentAction?)
}

class PresentationRequestInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val uuidProvider: UuidProvider,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val configUILogic: ConfigUILogic,
    walletCorePresentationController: WalletCorePresentationController? = null
) : PresentationRequestInteractor,
    ScopedPresentationInteractorDelegate(walletCorePresentationController) {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    private val claimsAreSelectable: Boolean
        get() = walletCorePresentationController.requestAllowsClaimSelection

    override fun setConfig(config: RequestUriConfig, intentAction: IntentAction?) {
        setScopeId(config.presentationScopeId)

        walletCorePresentationController.setConfig(
            config.toDomainConfig(intentAction = intentAction)
        )
    }

    override fun getRequestDocuments(): Flow<PresentationRequestInteractorPartialState> =
        walletCorePresentationController.events.mapNotNull { response ->
            when (response) {
                is TransferEventPartialState.RequestReceived -> {
                    val requestedClaimsAreEmpty = response.combinationsDomain
                        .flatMap { it.matches }
                        .all { it.requestedClaims.isEmpty() }
                    if (requestedClaimsAreEmpty) {
                        PresentationRequestInteractorPartialState.NoData(
                            relyingParty = response.relyingParty,
                        )
                    } else {
                        val storageDocuments = walletCoreDocumentsController.getAllIssuedDocuments()

                        val revokedDocumentIds = storageDocuments
                            .map { it.id }
                            .filter { walletCoreDocumentsController.isDocumentRevoked(it) }
                            .toSet()

                        val combinationsDomain =
                            response.combinationsDomain.map { combinationDomain ->
                                combinationDomain.copy(
                                    matches = combinationDomain.matches
                                        .filterNot { it.documentId in revokedDocumentIds }
                                )
                            }

                        val presentedCombinations = RequestTransformer.transformToCombinationsUi(
                            storageDocuments = storageDocuments,
                            resourceProvider = resourceProvider,
                            uuidProvider = uuidProvider,
                            combinationsDomain = combinationsDomain,
                            claimsAreSelectable = claimsAreSelectable,
                            overaskedClaims = response.relyingParty.registration.overaskedClaimsOrEmpty(),
                            userImageClaimKeys = configUILogic.userImageClaimKeys,
                        ).getOrThrow()
                            .withIndex()
                            .filter { (_, combinationUi) -> combinationUi.documents.isNotEmpty() }
                        val combinationsUi = presentedCombinations.map { it.value }
                        val canShowAllTransactionData =
                            presentedCombinations.all { (index, combinationUi) ->
                                canShowTransactionData(
                                    requested = response.combinationsDomain[index],
                                    shown = combinationUi,
                                )
                            }

                        if (!canShowAllTransactionData) {
                            PresentationRequestInteractorPartialState.Declined(
                                relyingParty = response.relyingParty,
                                reason = resourceProvider.getString(
                                    R.string.request_transaction_data_not_shown_error
                                ),
                            )
                        } else if (combinationsUi.isNotEmpty()) {
                            PresentationRequestInteractorPartialState.Success(
                                relyingParty = response.relyingParty,
                                combinationsUi = combinationsUi,
                                claimsAreSelectable = claimsAreSelectable,
                            )
                        } else {
                            PresentationRequestInteractorPartialState.NoData(
                                relyingParty = response.relyingParty,
                            )
                        }
                    }
                }

                is TransferEventPartialState.Error -> {
                    PresentationRequestInteractorPartialState.Failure(error = response.error)
                }

                is TransferEventPartialState.VerifierNotTrusted -> {
                    PresentationRequestInteractorPartialState.VerifierNotTrusted
                }

                is TransferEventPartialState.Disconnected -> {
                    PresentationRequestInteractorPartialState.Disconnect
                }

                else -> null
            }
        }.safeAsync {
            PresentationRequestInteractorPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun stopPresentation() {
        walletCorePresentationController.stopPresentation()
    }

    /**
     * True when every transaction-data entry of [requested] can be shown as it will be signed:
     * readable, and bound to a match on the consent screen ([shown]), keyed as the selection is by
     * document and query. A match left out because its document is blocked or cannot be shown, or
     * because it asks for no claims, takes its entries with it, so the request is declined rather
     * than shown in part, as on iOS.
     */
    private fun canShowTransactionData(
        requested: PresentationCombinationDomain,
        shown: RequestCombinationUi,
    ): Boolean {
        val shownMatches = shown.documents
            .map { it.domainPayload.docId to it.domainPayload.queryId }
            .toSet()
        return requested.matches
            .filter { it.transactionData.isNotEmpty() }
            .all { match ->
                (match.documentId to match.queryId) in shownMatches &&
                        PresentationTransactionDataDomain.Unavailable !in match.transactionData
            }
    }

    override fun updateRequestedDocuments(selectedCombination: RequestCombinationUi?) {
        val selections = selectedCombination?.let { safeSelectedCombinationUi ->
            RequestTransformer.createSelectionsDomain(
                documentItemsUi = safeSelectedCombinationUi.documents,
                matchesDomain = safeSelectedCombinationUi.matches,
                claimsAreSelectable = claimsAreSelectable,
            )
        }.orEmpty()

        walletCorePresentationController.updateRequestedDocuments(disclosedDocuments = selections)
    }
}