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

package eu.europa.ec.proximityfeature.interactor

import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.commonfeature.config.toDomainConfig
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi
import eu.europa.ec.commonfeature.ui.request.transformer.RequestTransformer
import eu.europa.ec.corelogic.controller.TransferEventPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.controller.WalletCorePresentationController
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

sealed class ProximityRequestInteractorPartialState {
    data class Success(
        val verifierName: String? = null,
        val verifierIsTrusted: Boolean,
        val requestDocuments: List<RequestDocumentItemUi>
    ) : ProximityRequestInteractorPartialState()

    data class NoData(
        val verifierName: String? = null,
        val verifierIsTrusted: Boolean,
    ) : ProximityRequestInteractorPartialState()

    data class Failure(val error: String) : ProximityRequestInteractorPartialState()
    data object Disconnect : ProximityRequestInteractorPartialState()
}

interface ProximityRequestInteractor {
    fun getRequestDocuments(): Flow<ProximityRequestInteractorPartialState>
    fun stopPresentation()
    fun updateRequestedDocuments(items: List<RequestDocumentItemUi>)
    fun setConfig(config: RequestUriConfig)
}

class ProximityRequestInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val walletCorePresentationController: WalletCorePresentationController,
    private val walletCoreDocumentsController: WalletCoreDocumentsController
) : ProximityRequestInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun setConfig(config: RequestUriConfig) {
        walletCorePresentationController.setConfig(config.toDomainConfig())
    }

    override fun getRequestDocuments(): Flow<ProximityRequestInteractorPartialState> =
        walletCorePresentationController.events.mapNotNull { response ->
            when (response) {
                is TransferEventPartialState.RequestReceived -> {
                    if (response.requestData.all { it.requestedItems.isEmpty() }) {
                        ProximityRequestInteractorPartialState.NoData(
                            verifierName = response.verifierName,
                            verifierIsTrusted = response.verifierIsTrusted,
                        )
                    } else {
                        val documentsDomain = RequestTransformer.transformToDomainItems(
                            storageDocuments = walletCoreDocumentsController.getAllIssuedDocuments(),
                            requestDocuments = response.requestData,
                            resourceProvider = resourceProvider
                        ).getOrThrow()
                            .filterNot {
                                walletCoreDocumentsController.isDocumentRevoked(it.docId)
                            }

                        if (documentsDomain.isNotEmpty()) {
                            ProximityRequestInteractorPartialState.Success(
                                verifierName = response.verifierName,
                                verifierIsTrusted = response.verifierIsTrusted,
                                requestDocuments = RequestTransformer.transformToUiItems(
                                    documentsDomain = documentsDomain,
                                    resourceProvider = resourceProvider,
                                )
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

    override fun updateRequestedDocuments(items: List<RequestDocumentItemUi>) {
        val disclosedDocuments = RequestTransformer.createDisclosedDocuments(items)
        walletCorePresentationController.updateRequestedDocuments(disclosedDocuments.toMutableList())
    }
}