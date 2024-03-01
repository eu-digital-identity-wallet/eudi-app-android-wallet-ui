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

package eu.europa.ec.businesslogic.controller.walletcore

import eu.europa.ec.businesslogic.controller.authentication.UserAuthenticationResult
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.businesslogic.model.BiometricCrypto
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.document.DeleteDocumentResult
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.issue.IssueDocumentResult
import eu.europa.ec.eudi.wallet.document.sample.LoadSampleResult
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
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
        val resultHandler: UserAuthenticationResult
    ) : IssueDocumentPartialState()
}

sealed class OpenId4VCIIssueDocumentPartialState {
    data class Success(val documentId: String) : OpenId4VCIIssueDocumentPartialState()
    data class Failure(val errorMessage: String) : OpenId4VCIIssueDocumentPartialState()
    data class UserAuthRequired(
        val crypto: BiometricCrypto,
        val resultHandler: UserAuthenticationResult
    ) : OpenId4VCIIssueDocumentPartialState()
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

    fun getDocumentById(id: String): Document?

    fun issueDocument(
        issuanceMethod: IssuanceMethod,
        documentType: String
    ): Flow<IssueDocumentPartialState>

    fun deleteDocument(
        documentId: String
    ): Flow<DeleteDocumentPartialState>

    fun deleteAllDocuments(PID_documentId: String): Flow<DeleteAllDocumentsPartialState>
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

    override fun getDocumentById(id: String): Document? {
        return eudiWallet.getDocumentById(documentId = id)
    }

    override fun issueDocument(
        issuanceMethod: IssuanceMethod,
        documentType: String
    ): Flow<IssueDocumentPartialState> = flow {
        when (issuanceMethod) {

            IssuanceMethod.OPENID4VCI -> {
                issueDocumentWithOpenId4VCI(documentType = documentType).collect { response ->
                    when (response) {
                        is OpenId4VCIIssueDocumentPartialState.Failure -> emit(
                            IssueDocumentPartialState.Failure(
                                errorMessage = response.errorMessage
                            )
                        )

                        is OpenId4VCIIssueDocumentPartialState.Success -> emit(
                            IssueDocumentPartialState.Success(
                                documentId = response.documentId
                            )
                        )

                        is OpenId4VCIIssueDocumentPartialState.UserAuthRequired -> emit(
                            IssueDocumentPartialState.UserAuthRequired(
                                crypto = response.crypto,
                                resultHandler = response.resultHandler
                            )
                        )
                    }
                }
            }
        }
    }.safeAsync {
        IssueDocumentPartialState.Failure(errorMessage = it.localizedMessage ?: genericErrorMessage)
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

    override fun deleteAllDocuments(PID_documentId: String): Flow<DeleteAllDocumentsPartialState> =
        flow {
            val allDocuments = eudiWallet.getDocuments()
            val PID_document = allDocuments.find { it.id == PID_documentId }

            PID_document?.let {
                val restNonPID_Documents = allDocuments.minusElement(it)

                var allNonPidDocsDeleted = true
                var allNonPidDocsDeletedFailureReason = ""

                restNonPID_Documents.forEach { document ->

                    deleteDocument(
                        documentId = document.id
                    ).collect { deleteNonPID_documentsPartialState ->
                        when (deleteNonPID_documentsPartialState) {
                            is DeleteDocumentPartialState.Failure -> {
                                allNonPidDocsDeleted = false
                                allNonPidDocsDeletedFailureReason =
                                    deleteNonPID_documentsPartialState.errorMessage
                            }

                            is DeleteDocumentPartialState.Success -> {}
                        }
                    }
                }

                if (allNonPidDocsDeleted) {
                    deleteDocument(
                        documentId = PID_documentId
                    ).collect { deletePID_documentPartialState ->
                        when (deletePID_documentPartialState) {
                            is DeleteDocumentPartialState.Failure -> emit(
                                DeleteAllDocumentsPartialState.Failure(
                                    errorMessage = deletePID_documentPartialState.errorMessage
                                )
                            )

                            is DeleteDocumentPartialState.Success -> emit(
                                DeleteAllDocumentsPartialState.Success
                            )
                        }
                    }
                } else {
                    emit(DeleteAllDocumentsPartialState.Failure(errorMessage = allNonPidDocsDeletedFailureReason))
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

    private fun issueDocumentWithOpenId4VCI(documentType: String): Flow<OpenId4VCIIssueDocumentPartialState> =
        callbackFlow {
            eudiWallet.issueDocument(
                docType = documentType,
                callback = { result ->
                    when (result) {
                        is IssueDocumentResult.Failure -> {
                            val errorMessage = result.error.localizedMessage ?: genericErrorMessage
                            trySendBlocking(
                                OpenId4VCIIssueDocumentPartialState.Failure(
                                    errorMessage = errorMessage
                                )
                            )
                        }

                        is IssueDocumentResult.Success -> {
                            val documentId = result.documentId
                            trySendBlocking(OpenId4VCIIssueDocumentPartialState.Success(documentId = documentId))
                        }

                        is IssueDocumentResult.UserAuthRequired -> {
                            trySendBlocking(
                                OpenId4VCIIssueDocumentPartialState.UserAuthRequired(
                                    BiometricCrypto(result.cryptoObject),
                                    UserAuthenticationResult(
                                        onAuthenticationSuccess = { result.resume() },
                                        onAuthenticationError = { result.cancel() },
                                        onAuthenticationFailure = { result.cancel() },
                                    )
                                )
                            )
                        }
                    }
                }
            )

            awaitClose()
        }.safeAsync {
            OpenId4VCIIssueDocumentPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMessage
            )
        }
}