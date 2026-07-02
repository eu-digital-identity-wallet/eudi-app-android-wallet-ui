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
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.navigation.helper.IntentAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

sealed class PresentationRequestInteractorPartialState {
    data class Success(
        val verifierName: String?,
        val verifierIsTrusted: Boolean,
        val combinationsUi: List<RequestCombinationUi>,
        val claimsAreSelectable: Boolean,
    ) : PresentationRequestInteractorPartialState()

    data class NoData(
        val verifierName: String?,
        val verifierIsTrusted: Boolean,
    ) : PresentationRequestInteractorPartialState()

    data class Failure(val error: String) : PresentationRequestInteractorPartialState()
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
    walletCorePresentationController: WalletCorePresentationController? = null
) : PresentationRequestInteractor,
    ScopedPresentationInteractorDelegate(walletCorePresentationController) {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    // false for OpenID4VP (DCQL is all-or-nothing); true for DC-API.
    private var claimsAreSelectable: Boolean = true

    override fun setConfig(config: RequestUriConfig, intentAction: IntentAction?) {
        setScopeId(config.presentationScopeId)
        claimsAreSelectable = config.mode.allowsClaimSelection

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
                            verifierName = response.verifierName,
                            verifierIsTrusted = response.verifierIsTrusted,
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

                        val combinationsUi = RequestTransformer.transformToCombinationsUi(
                            storageDocuments = storageDocuments,
                            resourceProvider = resourceProvider,
                            uuidProvider = uuidProvider,
                            combinationsDomain = combinationsDomain,
                            claimsAreSelectable = claimsAreSelectable,
                        ).getOrThrow()
                            .filter { it.documents.isNotEmpty() }

                        if (combinationsUi.isNotEmpty()) {
                            PresentationRequestInteractorPartialState.Success(
                                verifierName = response.verifierName,
                                verifierIsTrusted = response.verifierIsTrusted,
                                combinationsUi = combinationsUi,
                                claimsAreSelectable = claimsAreSelectable,
                            )
                        } else {
                            PresentationRequestInteractorPartialState.NoData(
                                verifierName = response.verifierName,
                                verifierIsTrusted = response.verifierIsTrusted,
                            )
                        }
                    }
                }

                is TransferEventPartialState.Error -> {
                    PresentationRequestInteractorPartialState.Failure(error = response.error)
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