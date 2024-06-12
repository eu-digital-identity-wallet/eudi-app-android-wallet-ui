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

package eu.europa.ec.corelogic.controller

import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.corelogic.model.DocType
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.document.DeleteDocumentResult
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.sample.LoadSampleResult
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer
import eu.europa.ec.eudi.wallet.issue.openid4vci.OfferResult
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.util.Base64

enum class IssuanceMethod {
    OPENID4VCI
}

sealed class IssueDocumentPartialState {
    data class Success(val documentId: String) : IssueDocumentPartialState()

    data class Failure(val errorMessage: String) : IssueDocumentPartialState()
    data class UserAuthRequired(
        val crypto: BiometricCrypto,
        val resultHandler: DeviceAuthenticationResult
    ) : IssueDocumentPartialState()

    data object Start : IssueDocumentPartialState()
}

sealed class IssueDocumentsPartialState {
    data class Success(val documentIds: List<String>) : IssueDocumentsPartialState()
    data class PartialSuccess(
        val documentIds: List<String>,
        val nonIssuedDocuments: Map<String, String>
    ) : IssueDocumentsPartialState()

    data class Failure(val errorMessage: String) : IssueDocumentsPartialState()
    data class UserAuthRequired(
        val crypto: BiometricCrypto,
        val resultHandler: DeviceAuthenticationResult
    ) : IssueDocumentsPartialState()

    data object Start : IssueDocumentsPartialState()
}

sealed class AddSampleDataPartialState {
    data object Success : AddSampleDataPartialState()
    data class Failure(val error: String) : AddSampleDataPartialState()
}

sealed class DeleteDocumentPartialState {
    data object Success : DeleteDocumentPartialState()
    data class Failure(val errorMessage: String) : DeleteDocumentPartialState()
}

sealed class DeleteAllDocumentsPartialState {
    data object Success : DeleteAllDocumentsPartialState()
    data class Failure(val errorMessage: String) : DeleteAllDocumentsPartialState()
}

sealed class ResolveDocumentOfferPartialState {
    data class Success(val offer: Offer) : ResolveDocumentOfferPartialState()
    data class Failure(val errorMessage: String) : ResolveDocumentOfferPartialState()
}

/**
 * Controller for interacting with internal local storage of Core for CRUD operations on documents
 * */
interface WalletCoreDocumentsController {
    /**
     * Load sample document data taken from raw.xml
     * */
    fun loadSampleData(sampleDataByteArray: ByteArray): Flow<LoadSampleDataPartialState>

    /**
     * Adds the sample data into the Database.
     * */
    fun addSampleData(): Flow<AddSampleDataPartialState>

    /**
     * @return All the documents from the Database.
     * */
    fun getAllDocuments(): List<Document>

    fun getAllDocumentsByType(documentIdentifier: DocumentIdentifier): List<Document>

    fun getDocumentById(id: String): Document?

    fun getMainPidDocument(): Document?

    fun issueDocument(
        issuanceMethod: IssuanceMethod,
        documentType: DocType
    ): Flow<IssueDocumentPartialState>

    fun issueDocumentsByOfferUri(
        offerUri: String,
    ): Flow<IssueDocumentsPartialState>

    fun deleteDocument(
        documentId: String
    ): Flow<DeleteDocumentPartialState>

    fun deleteAllDocuments(mainPidDocumentId: String): Flow<DeleteAllDocumentsPartialState>

    fun resolveDocumentOffer(offerUri: String): Flow<ResolveDocumentOfferPartialState>
}

class WalletCoreDocumentsControllerImpl(
    private val resourceProvider: ResourceProvider,
    private val eudiWallet: EudiWallet
) : WalletCoreDocumentsController {

    private val genericErrorMessage
        get() = resourceProvider.genericErrorMessage()

    override fun loadSampleData(sampleDataByteArray: ByteArray): Flow<LoadSampleDataPartialState> =
        flow {
            when (val result = eudiWallet.loadSampleData(sampleDataByteArray)) {
                is LoadSampleResult.Error -> emit(LoadSampleDataPartialState.Failure(result.message))
                is LoadSampleResult.Success -> emit(LoadSampleDataPartialState.Success)
            }
        }.safeAsync {
            LoadSampleDataPartialState.Failure(it.localizedMessage ?: genericErrorMessage)
        }

    override fun addSampleData(): Flow<AddSampleDataPartialState> = flow {

        val byteArray = Base64.getDecoder().decode(
            JSONObject(
                resourceProvider.getStringFromRaw(R.raw.sample_data)
            ).getString("Data")
        )

        loadSampleData(byteArray).map {
            when (it) {
                is LoadSampleDataPartialState.Failure -> AddSampleDataPartialState.Failure(it.error)
                is LoadSampleDataPartialState.Success -> AddSampleDataPartialState.Success
            }
        }.collect {
            emit(it)
        }
    }.safeAsync {
        AddSampleDataPartialState.Failure(it.localizedMessage ?: genericErrorMessage)
    }

    override fun getAllDocuments(): List<Document> = eudiWallet.getDocuments()

    override fun getAllDocumentsByType(documentIdentifier: DocumentIdentifier): List<Document> =
        getAllDocuments().filter { it.docType == documentIdentifier.docType }

    override fun getDocumentById(id: String): Document? {
        return eudiWallet.getDocumentById(documentId = id)
    }

    override fun getMainPidDocument(): Document? =
        getAllDocumentsByType(documentIdentifier = DocumentIdentifier.PID)
            .minByOrNull { it.createdAt }

    override fun issueDocument(
        issuanceMethod: IssuanceMethod,
        documentType: DocType
    ): Flow<IssueDocumentPartialState> = flow {
        when (issuanceMethod) {

            IssuanceMethod.OPENID4VCI -> {
                issueDocumentWithOpenId4VCI(documentType = documentType).collect { response ->
                    when (response) {
                        is IssueDocumentsPartialState.Failure -> emit(
                            IssueDocumentPartialState.Failure(
                                errorMessage = response.errorMessage
                            )
                        )

                        is IssueDocumentsPartialState.Success -> emit(
                            IssueDocumentPartialState.Success(
                                response.documentIds.first()
                            )
                        )

                        is IssueDocumentsPartialState.UserAuthRequired -> emit(
                            IssueDocumentPartialState.UserAuthRequired(
                                crypto = response.crypto,
                                resultHandler = response.resultHandler
                            )
                        )

                        is IssueDocumentsPartialState.PartialSuccess -> emit(
                            IssueDocumentPartialState.Success(
                                response.documentIds.first()
                            )
                        )

                        is IssueDocumentsPartialState.Start -> emit(
                            IssueDocumentPartialState.Start
                        )
                    }
                }
            }
        }
    }.safeAsync {
        IssueDocumentPartialState.Failure(errorMessage = it.localizedMessage ?: genericErrorMessage)
    }

    override fun issueDocumentsByOfferUri(offerUri: String): Flow<IssueDocumentsPartialState> =
        callbackFlow {
            eudiWallet.issueDocumentByOfferUri(
                offerUri = offerUri,
                onEvent = issuanceCallback()
            )

            awaitClose()
        }.safeAsync {
            IssueDocumentsPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMessage
            )
        }

    override fun deleteDocument(documentId: String): Flow<DeleteDocumentPartialState> = flow {
        when (val deleteResult = eudiWallet.deleteDocumentById(documentId = documentId)) {
            is DeleteDocumentResult.Failure -> {
                emit(
                    DeleteDocumentPartialState.Failure(
                        errorMessage = deleteResult.throwable.localizedMessage
                            ?: genericErrorMessage
                    )
                )
            }

            is DeleteDocumentResult.Success -> {
                emit(DeleteDocumentPartialState.Success)
            }
        }
    }.safeAsync {
        DeleteDocumentPartialState.Failure(
            errorMessage = it.localizedMessage ?: genericErrorMessage
        )
    }

    override fun deleteAllDocuments(mainPidDocumentId: String): Flow<DeleteAllDocumentsPartialState> =
        flow {
            val allDocuments = eudiWallet.getDocuments()
            val mainPidDocument = getMainPidDocument()

            mainPidDocument?.let {
                val restOfDocuments = allDocuments.minusElement(it)

                var restOfAllDocsDeleted = true
                var restOfAllDocsDeletedFailureReason = ""

                restOfDocuments.forEach { document ->

                    deleteDocument(
                        documentId = document.id
                    ).collect { deleteDocumentPartialState ->
                        when (deleteDocumentPartialState) {
                            is DeleteDocumentPartialState.Failure -> {
                                restOfAllDocsDeleted = false
                                restOfAllDocsDeletedFailureReason =
                                    deleteDocumentPartialState.errorMessage
                            }

                            is DeleteDocumentPartialState.Success -> {}
                        }
                    }
                }

                if (restOfAllDocsDeleted) {
                    deleteDocument(
                        documentId = mainPidDocumentId
                    ).collect { deleteMainPidDocumentPartialState ->
                        when (deleteMainPidDocumentPartialState) {
                            is DeleteDocumentPartialState.Failure -> emit(
                                DeleteAllDocumentsPartialState.Failure(
                                    errorMessage = deleteMainPidDocumentPartialState.errorMessage
                                )
                            )

                            is DeleteDocumentPartialState.Success -> emit(
                                DeleteAllDocumentsPartialState.Success
                            )
                        }
                    }
                } else {
                    emit(DeleteAllDocumentsPartialState.Failure(errorMessage = restOfAllDocsDeletedFailureReason))
                }
            } ?: emit(
                DeleteAllDocumentsPartialState.Failure(
                    errorMessage = genericErrorMessage
                )
            )
        }.safeAsync {
            DeleteAllDocumentsPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMessage
            )
        }

    override fun resolveDocumentOffer(offerUri: String): Flow<ResolveDocumentOfferPartialState> =
        callbackFlow {
            eudiWallet.resolveDocumentOffer(
                offerUri = offerUri,
                onResult = { offerResult ->
                    when (offerResult) {
                        is OfferResult.Failure -> {
                            trySendBlocking(
                                ResolveDocumentOfferPartialState.Failure(
                                    errorMessage = offerResult.error.localizedMessage
                                        ?: genericErrorMessage
                                )
                            )
                        }

                        is OfferResult.Success -> {
                            trySendBlocking(
                                ResolveDocumentOfferPartialState.Success(
                                    offer = offerResult.offer
                                )
                            )
                        }
                    }
                }
            )

            awaitClose()
        }.safeAsync {
            ResolveDocumentOfferPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMessage
            )
        }

    private fun issueDocumentWithOpenId4VCI(documentType: DocType): Flow<IssueDocumentsPartialState> =
        callbackFlow {

            eudiWallet.issueDocumentByDocType(
                docType = documentType,
                onEvent = issuanceCallback()
            )

            awaitClose()

        }.safeAsync {
            IssueDocumentsPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMessage
            )
        }

    private fun ProducerScope<IssueDocumentsPartialState>.issuanceCallback(): OpenId4VciManager.OnIssueEvent {

        var totalDocumentsToBeIssued = 0
        val nonIssuedDocuments: MutableMap<String, String> = mutableMapOf()
        val issuedDocuments: MutableMap<String, String> = mutableMapOf()

        val listener = OpenId4VciManager.OnIssueEvent { event ->
            when (event) {
                is IssueEvent.DocumentFailed -> {
                    nonIssuedDocuments[event.docType] = event.name
                }

                is IssueEvent.DocumentRequiresUserAuth -> {
                    trySendBlocking(
                        IssueDocumentsPartialState.UserAuthRequired(
                            BiometricCrypto(event.cryptoObject),
                            DeviceAuthenticationResult(
                                onAuthenticationSuccess = { event.resume() },
                                onAuthenticationError = { event.cancel() },
                                onAuthenticationFailure = { event.cancel() },
                            )
                        )
                    )
                }

                is IssueEvent.Failure -> {
                    val errorMessage = event.cause.localizedMessage ?: genericErrorMessage
                    trySendBlocking(
                        IssueDocumentsPartialState.Failure(
                            errorMessage = errorMessage
                        )
                    )
                }

                is IssueEvent.Finished -> {

                    if (event.issuedDocuments.isEmpty()) {
                        trySendBlocking(
                            IssueDocumentsPartialState.Failure(
                                errorMessage = genericErrorMessage
                            )
                        )
                        return@OnIssueEvent
                    }

                    if (event.issuedDocuments.size == totalDocumentsToBeIssued) {
                        trySendBlocking(
                            IssueDocumentsPartialState.Success(
                                documentIds = event.issuedDocuments
                            )
                        )
                        return@OnIssueEvent
                    }

                    trySendBlocking(
                        IssueDocumentsPartialState.PartialSuccess(
                            documentIds = event.issuedDocuments,
                            nonIssuedDocuments = nonIssuedDocuments
                        )
                    )
                }

                is IssueEvent.DocumentIssued -> {
                    issuedDocuments[event.documentId] = event.docType
                }

                is IssueEvent.Started -> {
                    totalDocumentsToBeIssued = event.total
                    trySendBlocking(
                        IssueDocumentsPartialState.Start
                    )
                }
            }
        }

        return listener
    }
}