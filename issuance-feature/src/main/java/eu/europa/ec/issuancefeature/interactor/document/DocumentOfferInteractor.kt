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

package eu.europa.ec.issuancefeature.interactor.document

import android.content.Context
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAvailability
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractor
import eu.europa.ec.commonfeature.model.toUiName
import eu.europa.ec.commonfeature.ui.request.model.DocumentItemUi
import eu.europa.ec.corelogic.controller.IssueDocumentsPartialState
import eu.europa.ec.corelogic.controller.ResolveDocumentOfferPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.model.DocumentType
import eu.europa.ec.corelogic.model.isSupported
import eu.europa.ec.corelogic.model.toDocumentType
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

sealed class ResolveDocumentOfferInteractorPartialState {
    data class Success(
        val documents: List<DocumentItemUi>,
        val issuerName: String,
    ) : ResolveDocumentOfferInteractorPartialState()

    data class NoDocument(val issuerName: String) : ResolveDocumentOfferInteractorPartialState()
    data class Failure(val errorMessage: String) : ResolveDocumentOfferInteractorPartialState()
}

sealed class IssueDocumentsInteractorPartialState {
    data class Success(
        val successScreenSubtitle: String,
    ) : IssueDocumentsInteractorPartialState()

    data class Failure(val errorMessage: String) : IssueDocumentsInteractorPartialState()

    data class UserAuthRequired(
        val crypto: BiometricCrypto,
        val resultHandler: DeviceAuthenticationResult
    ) : IssueDocumentsInteractorPartialState()

    data object Start : IssueDocumentsInteractorPartialState()
}

interface DocumentOfferInteractor {
    fun resolveDocumentOffer(offerUri: String): Flow<ResolveDocumentOfferInteractorPartialState>

    fun issueDocuments(
        offerUri: String,
        issuerName: String,
    ): Flow<IssueDocumentsInteractorPartialState>

    fun handleUserAuthentication(
        context: Context,
        crypto: BiometricCrypto,
        resultHandler: DeviceAuthenticationResult
    )
}

class DocumentOfferInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    private val resourceProvider: ResourceProvider,
) : DocumentOfferInteractor {
    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun resolveDocumentOffer(offerUri: String): Flow<ResolveDocumentOfferInteractorPartialState> =
        flow {
            walletCoreDocumentsController.resolveDocumentOffer(
                offerUri = offerUri
            ).map { response ->
                when (response) {
                    is ResolveDocumentOfferPartialState.Failure -> {
                        ResolveDocumentOfferInteractorPartialState.Failure(errorMessage = response.errorMessage)
                    }

                    is ResolveDocumentOfferPartialState.Success -> {
                        val offerHasNoDocuments = response.offer.offeredDocuments.isEmpty()
                        if (offerHasNoDocuments) {
                            ResolveDocumentOfferInteractorPartialState.NoDocument(issuerName = response.offer.issuerName)
                        } else {

                            val hasMainPid =
                                walletCoreDocumentsController.getMainPidDocument() != null

                            val hasPidInOffer =
                                response.offer.offeredDocuments.any { offeredDocument ->
                                    offeredDocument.docType.toDocumentType() == DocumentType.PID
                                }

                            if (hasMainPid || hasPidInOffer) {
                                val resolvedDocumentsNames =
                                    response.offer.offeredDocuments.map { offeredDocument ->
                                        if (offeredDocument.docType.toDocumentType().isSupported()) {
                                            offeredDocument.docType.toDocumentType()
                                                .toUiName(resourceProvider)
                                        } else {
                                            offeredDocument.name
                                        }
                                    }

                                ResolveDocumentOfferInteractorPartialState.Success(
                                    documents = resolvedDocumentsNames.map { documentName ->
                                        DocumentItemUi(title = documentName)
                                    },
                                    issuerName = response.offer.issuerName
                                )
                            } else {
                                ResolveDocumentOfferInteractorPartialState.Failure(
                                    errorMessage = resourceProvider.getString(
                                        R.string.issuance_document_offer_error_missing_pid_text
                                    )
                                )
                            }
                        }
                    }
                }
            }.collect {
                emit(it)
            }
        }.safeAsync {
            ResolveDocumentOfferInteractorPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun issueDocuments(
        offerUri: String,
        issuerName: String,
    ): Flow<IssueDocumentsInteractorPartialState> =
        flow {
            walletCoreDocumentsController.issueDocumentsByOfferUri(
                offerUri = offerUri
            ).map { response ->
                when (response) {
                    is IssueDocumentsPartialState.Failure -> {
                        IssueDocumentsInteractorPartialState.Failure(errorMessage = response.errorMessage)
                    }

                    is IssueDocumentsPartialState.PartialSuccess -> {

                        val nonIssuedDocsNames: String = response.nonIssuedDocuments.entries.map {
                            if (it.key.toDocumentType().isSupported()) {
                                it.key.toDocumentType().toUiName(resourceProvider)
                            } else {
                                it.value
                            }
                        }.joinToString(
                            separator = ", ",
                            transform = {
                                it
                            }
                        )

                        IssueDocumentsInteractorPartialState.Success(
                            successScreenSubtitle = resourceProvider.getString(
                                R.string.issuance_document_offer_partial_success_subtitle,
                                issuerName,
                                nonIssuedDocsNames
                            )
                        )
                    }

                    is IssueDocumentsPartialState.Success -> {
                        IssueDocumentsInteractorPartialState.Success(
                            successScreenSubtitle = resourceProvider.getString(
                                R.string.issuance_document_offer_success_subtitle,
                                issuerName
                            )
                        )
                    }

                    is IssueDocumentsPartialState.UserAuthRequired -> {
                        IssueDocumentsInteractorPartialState.UserAuthRequired(
                            crypto = response.crypto,
                            resultHandler = response.resultHandler
                        )
                    }

                    is IssueDocumentsPartialState.Start -> IssueDocumentsInteractorPartialState.Start
                }
            }.collect {
                emit(it)
            }
        }.safeAsync {
            IssueDocumentsInteractorPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun handleUserAuthentication(
        context: Context,
        crypto: BiometricCrypto,
        resultHandler: DeviceAuthenticationResult
    ) {
        deviceAuthenticationInteractor.getBiometricsAvailability {
            when (it) {
                is BiometricsAvailability.CanAuthenticate -> {
                    deviceAuthenticationInteractor.authenticateWithBiometrics(
                        context,
                        crypto,
                        resultHandler
                    )
                }

                is BiometricsAvailability.NonEnrolled -> {
                    deviceAuthenticationInteractor.authenticateWithBiometrics(
                        context,
                        crypto,
                        resultHandler
                    )
                }

                is BiometricsAvailability.Failure -> {
                    resultHandler.onAuthenticationFailure()
                }
            }
        }
    }
}