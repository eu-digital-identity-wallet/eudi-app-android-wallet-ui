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

package eu.europa.ec.proximityfeature.interactor

import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.commonfeature.interactor.ScopedPresentationInteractor
import eu.europa.ec.commonfeature.interactor.ScopedPresentationInteractorDelegate
import eu.europa.ec.commonfeature.ui.request.model.RequestCombinationUi
import eu.europa.ec.commonfeature.ui.request.transformer.RequestTransformer
import eu.europa.ec.corelogic.controller.TransferEventPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.controller.WalletCorePresentationController
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

sealed class ProximityRequestInteractorPartialState {
    data class Success(
        val verifierName: String?,
        val verifierIsTrusted: Boolean,
        val combinationsUi: List<RequestCombinationUi>,
        val claimsAreSelectable: Boolean,
    ) : ProximityRequestInteractorPartialState()

    data class NoData(
        val verifierName: String?,
        val verifierIsTrusted: Boolean,
    ) : ProximityRequestInteractorPartialState()

    data class Failure(val error: String) : ProximityRequestInteractorPartialState()
    data object Disconnect : ProximityRequestInteractorPartialState()
}

interface ProximityRequestInteractor : ScopedPresentationInteractor {
    fun getRequestDocuments(): Flow<ProximityRequestInteractorPartialState>
    fun stopPresentation()
    fun updateRequestedDocuments(selectedCombination: RequestCombinationUi?)
}

class ProximityRequestInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val uuidProvider: UuidProvider,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    walletCorePresentationController: WalletCorePresentationController? = null
) : ProximityRequestInteractor,
    ScopedPresentationInteractorDelegate(walletCorePresentationController) {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    // Proximity always allows selective disclosure.
    private val claimsAreSelectable: Boolean = true

    override fun getRequestDocuments(): Flow<ProximityRequestInteractorPartialState> =
        walletCorePresentationController.events.mapNotNull { response ->
            when (response) {
                is TransferEventPartialState.RequestReceived -> {
                    val requestedClaimsAreEmpty = response.combinationsDomain
                        .flatMap { it.matches }
                        .all { it.requestedClaims.isEmpty() }
                    if (requestedClaimsAreEmpty) {
                        ProximityRequestInteractorPartialState.NoData(
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
                            ProximityRequestInteractorPartialState.Success(
                                verifierName = response.verifierName,
                                verifierIsTrusted = response.verifierIsTrusted,
                                combinationsUi = combinationsUi,
                                claimsAreSelectable = claimsAreSelectable,
                            )
                        } else {
                            ProximityRequestInteractorPartialState.NoData(
                                verifierName = response.verifierName,
                                verifierIsTrusted = response.verifierIsTrusted,
                            )
                        }
                    }
                }

                is TransferEventPartialState.Error -> {
                    ProximityRequestInteractorPartialState.Failure(error = response.error)
                }

                is TransferEventPartialState.Disconnected -> {
                    ProximityRequestInteractorPartialState.Disconnect
                }

                else -> null
            }
        }.safeAsync {
            ProximityRequestInteractorPartialState.Failure(
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