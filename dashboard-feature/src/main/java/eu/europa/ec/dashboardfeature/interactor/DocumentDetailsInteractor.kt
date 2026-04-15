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

package eu.europa.ec.dashboardfeature.interactor

import android.content.Context
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAvailability
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractor
import eu.europa.ec.corelogic.controller.DeleteAllDocumentsPartialState
import eu.europa.ec.corelogic.controller.DeleteDocumentPartialState
import eu.europa.ec.corelogic.controller.IssueDocumentsPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.extension.localizedIssuerMetadata
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.model.toDocumentIdentifier
import eu.europa.ec.dashboardfeature.ui.documents.detail.model.DocumentDetailsDomain
import eu.europa.ec.dashboardfeature.ui.documents.detail.transformer.DocumentDetailsTransformer
import eu.europa.ec.dashboardfeature.ui.documents.detail.transformer.DocumentDetailsTransformer.createDocumentCredentialsInfoUi
import eu.europa.ec.dashboardfeature.ui.documents.model.DocumentCredentialsInfoUi
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.IssuerDetailsCardDataUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

sealed class DocumentDetailsInteractorIssuancePartialState {
    data object Success : DocumentDetailsInteractorIssuancePartialState()

    data class Failure(val errorMessage: String) : DocumentDetailsInteractorIssuancePartialState()

    data class UserAuthRequired(
        val crypto: BiometricCrypto,
        val resultHandler: DeviceAuthenticationResult,
    ) : DocumentDetailsInteractorIssuancePartialState()
}

sealed class DocumentDetailsInteractorPartialState {
    data class Success(
        val issuerDetails: IssuerDetailsCardDataUi,
        val documentDetailsDomain: DocumentDetailsDomain,
        val documentIsBookmarked: Boolean,
        val documentCredentialsInfoUi: DocumentCredentialsInfoUi,
    ) : DocumentDetailsInteractorPartialState()

    data class Failure(val error: String) : DocumentDetailsInteractorPartialState()
}

sealed class DocumentDetailsInteractorDeleteDocumentPartialState {
    data object SingleDocumentDeleted : DocumentDetailsInteractorDeleteDocumentPartialState()
    data object AllDocumentsDeleted : DocumentDetailsInteractorDeleteDocumentPartialState()
    data class Failure(
        val errorMessage: String
    ) : DocumentDetailsInteractorDeleteDocumentPartialState()
}

sealed class DocumentDetailsInteractorStoreBookmarkPartialState {
    data class Success(
        val bookmarkId: String
    ) : DocumentDetailsInteractorStoreBookmarkPartialState()

    data object Failure : DocumentDetailsInteractorStoreBookmarkPartialState()
}

sealed class DocumentDetailsInteractorDeleteBookmarkPartialState {
    data object Success : DocumentDetailsInteractorDeleteBookmarkPartialState()
    data object Failure : DocumentDetailsInteractorDeleteBookmarkPartialState()
}

interface DocumentDetailsInteractor {
    fun getDocumentDetails(
        documentId: DocumentId,
        wasIssuerDetailsExpanded: Boolean?
    ): Flow<DocumentDetailsInteractorPartialState>

    fun deleteDocument(
        documentId: DocumentId
    ): Flow<DocumentDetailsInteractorDeleteDocumentPartialState>

    fun storeBookmark(
        documentId: String
    ): Flow<DocumentDetailsInteractorStoreBookmarkPartialState>

    fun deleteBookmark(
        documentId: String
    ): Flow<DocumentDetailsInteractorDeleteBookmarkPartialState>

    fun reIssueDocument(
        documentId: String,
        issuerId: String
    ): Flow<DocumentDetailsInteractorIssuancePartialState>

    fun handleUserAuth(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult
    )

    fun resumeOpenId4VciWithAuthorization(uri: String)
}

class DocumentDetailsInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    private val resourceProvider: ResourceProvider,
    private val uuidProvider: UuidProvider,
    private val configLogic: ConfigLogic
) : DocumentDetailsInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getDocumentDetails(
        documentId: DocumentId,
        wasIssuerDetailsExpanded: Boolean?,
    ): Flow<DocumentDetailsInteractorPartialState> =
        flow {
            val issuedDocument =
                walletCoreDocumentsController.getDocumentById(documentId = documentId)
                        as? IssuedDocument

            issuedDocument?.let { safeIssuedDocument ->
                val documentDetailsDomainResult =
                    DocumentDetailsTransformer.transformToDocumentDetailsDomain(
                        document = safeIssuedDocument,
                        resourceProvider = resourceProvider,
                        uuidProvider = uuidProvider
                    )
                val documentDetailsDomain = documentDetailsDomainResult.getOrThrow()

                val documentCredentialsInfo = createDocumentCredentialsInfoUi(
                    document = safeIssuedDocument,
                    resourceProvider = resourceProvider
                )

                val userLocale = resourceProvider.getLocale()
                val issuerName = safeIssuedDocument.localizedIssuerMetadata(userLocale)?.name
                val issuerLogo = safeIssuedDocument.localizedIssuerMetadata(userLocale)?.logo

                val documentIsBookmarked =
                    walletCoreDocumentsController.isDocumentBookmarked(documentId)

                val documentIsRevoked = walletCoreDocumentsController.isDocumentRevoked(documentId)
                val issuerDetails = IssuerDetailsCardDataUi(
                    issuerName = issuerName,
                    issuerLogo = issuerLogo?.uri,
                    documentState = if (documentIsRevoked) {
                        IssuerDetailsCardDataUi.DocumentState.Revoked
                    } else {
                        IssuerDetailsCardDataUi.DocumentState.Issued(
                            issuanceDate = documentDetailsDomain.documentIssuanceDate,
                            expirationDate = documentDetailsDomain.documentExpirationDate
                        )
                    },
                    isExpanded = wasIssuerDetailsExpanded ?: false
                )

                emit(
                    DocumentDetailsInteractorPartialState.Success(
                        issuerDetails = issuerDetails,
                        documentDetailsDomain = documentDetailsDomain,
                        documentIsBookmarked = documentIsBookmarked,
                        documentCredentialsInfoUi = documentCredentialsInfo,
                    )
                )
            } ?: emit(DocumentDetailsInteractorPartialState.Failure(error = genericErrorMsg))
        }.safeAsync {
            DocumentDetailsInteractorPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun deleteDocument(
        documentId: DocumentId
    ): Flow<DocumentDetailsInteractorDeleteDocumentPartialState> =
        flow {
            val document = walletCoreDocumentsController.getDocumentById(documentId = documentId)
            val format = document?.format
            val docType = (format as? MsoMdocFormat)?.docType ?: (format as? SdJwtVcFormat)?.vct
            val docIdentifier = docType?.toDocumentIdentifier()

            val shouldDeleteAllDocuments: Boolean = if (configLogic.forcePidActivation
                && (docIdentifier == DocumentIdentifier.MdocPid || docIdentifier == DocumentIdentifier.SdJwtPid)
            ) {

                val allPidDocuments = walletCoreDocumentsController.getAllDocumentsByType(
                    documentIdentifiers = listOf(
                        DocumentIdentifier.MdocPid,
                        DocumentIdentifier.SdJwtPid
                    )
                )

                if (allPidDocuments.count() > 1) {
                    walletCoreDocumentsController.getMainPidDocument()?.id == documentId
                } else {
                    true
                }
            } else {
                false
            }

            if (shouldDeleteAllDocuments) {
                walletCoreDocumentsController.deleteAllDocuments()
                    .map {
                        when (it) {
                            is DeleteAllDocumentsPartialState.Failure -> DocumentDetailsInteractorDeleteDocumentPartialState.Failure(
                                errorMessage = it.errorMessage
                            )

                            is DeleteAllDocumentsPartialState.Success -> DocumentDetailsInteractorDeleteDocumentPartialState.AllDocumentsDeleted
                        }
                    }
            } else {
                walletCoreDocumentsController.deleteDocument(documentId = documentId).map {
                    when (it) {
                        is DeleteDocumentPartialState.Failure -> DocumentDetailsInteractorDeleteDocumentPartialState.Failure(
                            errorMessage = it.errorMessage
                        )

                        is DeleteDocumentPartialState.Success -> DocumentDetailsInteractorDeleteDocumentPartialState.SingleDocumentDeleted
                    }
                }
            }.collect {
                emit(it)
            }
        }.safeAsync {
            DocumentDetailsInteractorDeleteDocumentPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun storeBookmark(documentId: DocumentId): Flow<DocumentDetailsInteractorStoreBookmarkPartialState> =
        flow {
            walletCoreDocumentsController.storeBookmark(documentId)
            emit(DocumentDetailsInteractorStoreBookmarkPartialState.Success(documentId))
        }.safeAsync {
            DocumentDetailsInteractorStoreBookmarkPartialState.Failure
        }

    override fun deleteBookmark(documentId: DocumentId): Flow<DocumentDetailsInteractorDeleteBookmarkPartialState> =
        flow {
            walletCoreDocumentsController.deleteBookmark(documentId)
            emit(DocumentDetailsInteractorDeleteBookmarkPartialState.Success)
        }.safeAsync {
            DocumentDetailsInteractorDeleteBookmarkPartialState.Failure
        }

    override fun reIssueDocument(
        documentId: String,
        issuerId: String
    ): Flow<DocumentDetailsInteractorIssuancePartialState> = flow {

        walletCoreDocumentsController.reIssueDocument(
            documentId = documentId,
            issuerId = issuerId,
            allowAuthorizationFallback = true
        ).collect { state ->

            val successIds: MutableList<String> = mutableListOf()
            var isDeferred = false
            var error: String? = null
            var authenticationData: Pair<BiometricCrypto, DeviceAuthenticationResult>? = null

            when (state) {
                is IssueDocumentsPartialState.DeferredSuccess -> {
                    isDeferred = true
                }

                is IssueDocumentsPartialState.Failure -> {
                    error = state.errorMessage
                }

                is IssueDocumentsPartialState.PartialSuccess -> {
                    successIds.addAll(state.documentIds)
                }

                is IssueDocumentsPartialState.Success -> {
                    successIds.addAll(state.documentIds)
                }

                is IssueDocumentsPartialState.UserAuthRequired -> {
                    authenticationData = state.crypto to state.resultHandler
                }
            }

            val state = if (successIds.isNotEmpty() || isDeferred) {
                DocumentDetailsInteractorIssuancePartialState.Success
            } else if (error != null) {
                DocumentDetailsInteractorIssuancePartialState.Failure(error)
            } else if (authenticationData != null) {
                DocumentDetailsInteractorIssuancePartialState.UserAuthRequired(
                    authenticationData.first,
                    authenticationData.second
                )
            } else {
                DocumentDetailsInteractorIssuancePartialState.Failure(genericErrorMsg)
            }

            emit(state)
        }
    }.safeAsync {
        DocumentDetailsInteractorIssuancePartialState.Failure(
            errorMessage = it.localizedMessage ?: genericErrorMsg
        )
    }

    override fun handleUserAuth(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult
    ) {
        deviceAuthenticationInteractor.getBiometricsAvailability {
            when (it) {
                is BiometricsAvailability.CanAuthenticate -> {
                    deviceAuthenticationInteractor.authenticateWithBiometrics(
                        context = context,
                        crypto = crypto,
                        notifyOnAuthenticationFailure = notifyOnAuthenticationFailure,
                        resultHandler = resultHandler
                    )
                }

                is BiometricsAvailability.NonEnrolled -> {
                    deviceAuthenticationInteractor.launchBiometricSystemScreen()
                }

                is BiometricsAvailability.Failure -> {
                    resultHandler.onAuthenticationFailure()
                }
            }
        }
    }

    override fun resumeOpenId4VciWithAuthorization(uri: String) {
        walletCoreDocumentsController.resumeOpenId4VciWithAuthorization(uri)
    }
}