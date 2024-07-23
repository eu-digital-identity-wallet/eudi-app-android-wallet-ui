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

package eu.europa.ec.dashboardfeature.interactor

import android.bluetooth.BluetoothManager
import android.content.Context
import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.businesslogic.util.toDateFormatted
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.commonfeature.model.DocumentUiState
import eu.europa.ec.commonfeature.model.toUiName
import eu.europa.ec.commonfeature.ui.document_details.model.DocumentJsonKeys
import eu.europa.ec.commonfeature.util.documentHasExpired
import eu.europa.ec.commonfeature.util.extractValueFromDocumentOrEmpty
import eu.europa.ec.corelogic.config.WalletCoreConfig
import eu.europa.ec.corelogic.controller.DeleteAllDocumentsPartialState
import eu.europa.ec.corelogic.controller.DeleteDocumentPartialState
import eu.europa.ec.corelogic.controller.IssueDeferredDocumentPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.model.DeferredDocumentData
import eu.europa.ec.corelogic.model.DocType
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.model.toDocumentIdentifier
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.withContext

data class DeferredDocNotReadyYetException(override val message: String?) : Exception()

sealed class DashboardInteractorGetDocumentsPartialState {
    data class Success(
        val documentsUi: List<DocumentUi>,
        val mainPid: IssuedDocument?,
        val userFirstName: String,
        val userBase64Portrait: String,
    ) : DashboardInteractorGetDocumentsPartialState()

    data class Failure(val error: String) : DashboardInteractorGetDocumentsPartialState()
}

sealed class DashboardInteractorGetDocumentsOnlyForTheseIdsPartialState {
    data class Success(
        val newlyIssuedDocumentsUi: List<DocumentUi>,
        val mainPid: IssuedDocument?,
        val userFirstName: String,
        val userBase64Portrait: String,
    ) : DashboardInteractorGetDocumentsOnlyForTheseIdsPartialState()

    data class Failure(val error: String) :
        DashboardInteractorGetDocumentsOnlyForTheseIdsPartialState()
}

sealed class DashboardInteractorDeleteDocumentPartialState {
    data object SingleDocumentDeleted : DashboardInteractorDeleteDocumentPartialState()
    data object AllDocumentsDeleted : DashboardInteractorDeleteDocumentPartialState()
    data class Failure(val errorMessage: String) :
        DashboardInteractorDeleteDocumentPartialState()
}

data class UserInfo(
    val userFirstName: String,
    val userBase64Portrait: String
)

sealed class DashboardInteractorRetryIssuingDeferredDocumentPartialState {
    data class Success(
        val deferredDocumentData: DeferredDocumentData
    ) : DashboardInteractorRetryIssuingDeferredDocumentPartialState()

    data class Failure(
        val documentId: DocumentId,
        val errorMessage: String,
    ) : DashboardInteractorRetryIssuingDeferredDocumentPartialState()
}

sealed class DashboardInteractorRetryIssuingDeferredDocumentsPartialState {
    data class Result(
        val successfullyIssuedDeferredDocuments: List<DeferredDocumentData>,
        val failedIssuedDeferredDocuments: List<DocumentId>,
    ) : DashboardInteractorRetryIssuingDeferredDocumentsPartialState()

    data class Failure(
        val errorMessage: String,
    ) : DashboardInteractorRetryIssuingDeferredDocumentsPartialState()
}

interface DashboardInteractor {
    fun getDocuments(): Flow<DashboardInteractorGetDocumentsPartialState>

    fun getDocumentsOnlyForTheseIds(
        documentIds: List<DocumentId>,
        userFirstName: String,
        userImage: String,
    ): Flow<DashboardInteractorGetDocumentsOnlyForTheseIdsPartialState>

    fun getDocumentById(documentId: DocumentId): Document?
    fun isBleAvailable(): Boolean
    fun isBleCentralClientModeEnabled(): Boolean
    fun getAppVersion(): String
    fun deleteDocument(
        documentId: String
    ): Flow<DashboardInteractorDeleteDocumentPartialState>

    suspend fun tryIssuingDeferredDocumentSuspend(deferredDocumentId: DocumentId)
            : DashboardInteractorRetryIssuingDeferredDocumentPartialState

    fun tryIssuingDeferredDocumentsFlow(
        deferredDocuments: Map<DocumentId, DocType>,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): Flow<DashboardInteractorRetryIssuingDeferredDocumentsPartialState>
}

class DashboardInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val walletCoreConfig: WalletCoreConfig,
    private val configLogic: ConfigLogic
) : DashboardInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun isBleAvailable(): Boolean {
        val bluetoothManager: BluetoothManager? = resourceProvider.provideContext()
            .getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager?.adapter?.isEnabled == true
    }

    override fun isBleCentralClientModeEnabled(): Boolean =
        walletCoreConfig.config.bleCentralClientModeEnabled

    override fun getDocuments(): Flow<DashboardInteractorGetDocumentsPartialState> = flow {
        var userFirstName = ""
        var userImage = ""
        val documents = walletCoreDocumentsController.getAllDocuments()
        val mainPid = walletCoreDocumentsController.getMainPidDocument()
        val documentsUi = documents.map { document ->
            val (documentUi, userInfo) = document.toDocumentUiAndUserInfo(mainPid)

            if (userFirstName.isBlank()) {
                userFirstName = userInfo.userFirstName
            }
            if (userImage.isBlank()) {
                userImage = userInfo.userBase64Portrait
            }

            return@map documentUi
        }
        emit(
            DashboardInteractorGetDocumentsPartialState.Success(
                documentsUi = documentsUi,
                mainPid = mainPid,
                userFirstName = userFirstName,
                userBase64Portrait = userImage
            )
        )
    }.safeAsync {
        DashboardInteractorGetDocumentsPartialState.Failure(
            error = it.localizedMessage ?: genericErrorMsg
        )
    }

    override fun getDocumentsOnlyForTheseIds(
        documentIds: List<DocumentId>,
        userFirstName: String,
        userImage: String,
    ): Flow<DashboardInteractorGetDocumentsOnlyForTheseIdsPartialState> = flow {
        var usrFirstName = userFirstName
        var usrImage = userImage
        val documents = walletCoreDocumentsController.getAllDocuments().filter {
            documentIds.contains(it.id)
        }
        val mainPid = walletCoreDocumentsController.getMainPidDocument()
        val documentsUi = documents.map { document ->
            val (documentUi, userInfo) = document.toDocumentUiAndUserInfo(mainPid)

            if (userFirstName.isBlank()) {
                usrFirstName = userInfo.userFirstName
            }
            if (userImage.isBlank()) {
                usrImage = userInfo.userBase64Portrait
            }

            return@map documentUi
        }
        emit(
            DashboardInteractorGetDocumentsOnlyForTheseIdsPartialState.Success(
                newlyIssuedDocumentsUi = documentsUi,
                mainPid = mainPid,
                userFirstName = usrFirstName,
                userBase64Portrait = usrImage
            )
        )
    }.safeAsync {
        DashboardInteractorGetDocumentsOnlyForTheseIdsPartialState.Failure(
            error = it.localizedMessage ?: genericErrorMsg
        )
    }

    private fun Document.toDocumentUiAndUserInfo(mainPid: IssuedDocument?): Pair<DocumentUi, UserInfo> {
        when (this) {
            is IssuedDocument -> {
                var documentExpirationDate = extractValueFromDocumentOrEmpty(
                    document = this,
                    key = DocumentJsonKeys.EXPIRY_DATE
                )

                val docHasExpired = documentHasExpired(documentExpirationDate)

                documentExpirationDate = if (documentExpirationDate.isNotBlank()) {
                    documentExpirationDate.toDateFormatted().toString()
                } else {
                    resourceProvider.getString(R.string.dashboard_document_no_expiration_found)
                }

                val userFirstName = extractValueFromDocumentOrEmpty(
                    document = mainPid ?: this,
                    key = DocumentJsonKeys.FIRST_NAME
                )


                val userImage = extractValueFromDocumentOrEmpty(
                    document = this,
                    key = DocumentJsonKeys.PORTRAIT
                )

                return DocumentUi(
                    documentId = this.id,
                    documentName = this.toUiName(resourceProvider),
                    documentIdentifier = this.toDocumentIdentifier(),
                    documentImage = "",
                    documentExpirationDateFormatted = documentExpirationDate,
                    documentHasExpired = docHasExpired,
                    documentDetails = emptyList(),
                    documentState = DocumentUiState.Issued
                ) to UserInfo(
                    userFirstName = userFirstName,
                    userBase64Portrait = userImage
                )
            }

            else -> {
                return DocumentUi(
                    documentId = this.id,
                    documentName = this.toUiName(resourceProvider),
                    documentIdentifier = this.toDocumentIdentifier(),
                    documentImage = "",
                    documentExpirationDateFormatted = "",
                    documentHasExpired = false,
                    documentDetails = emptyList(),
                    documentState = DocumentUiState.Deferred
                ) to UserInfo(
                    userFirstName = "",
                    userBase64Portrait = ""
                )
            }
        }
    }

    override fun getDocumentById(documentId: DocumentId): Document? =
        walletCoreDocumentsController.getDocumentById(documentId)

    override fun getAppVersion(): String = configLogic.appVersion

    override fun deleteDocument(
        documentId: String,
    ): Flow<DashboardInteractorDeleteDocumentPartialState> =
        flow {
            val document = getDocumentById(documentId = documentId)

            val shouldDeleteAllDocuments: Boolean =
                if (document?.docType?.toDocumentIdentifier() == DocumentIdentifier.PID) {

                    val allPidDocuments =
                        walletCoreDocumentsController.getAllDocumentsByType(documentIdentifier = DocumentIdentifier.PID)

                    if (allPidDocuments.count() > 1) {
                        walletCoreDocumentsController.getMainPidDocument()?.id == documentId
                    } else {
                        true
                    }
                } else {
                    false
                }

            if (shouldDeleteAllDocuments) {
                walletCoreDocumentsController.deleteAllDocuments(mainPidDocumentId = documentId)
                    .map {
                        when (it) {
                            is DeleteAllDocumentsPartialState.Failure -> DashboardInteractorDeleteDocumentPartialState.Failure(
                                errorMessage = it.errorMessage
                            )

                            is DeleteAllDocumentsPartialState.Success -> DashboardInteractorDeleteDocumentPartialState.AllDocumentsDeleted
                        }
                    }
            } else {
                walletCoreDocumentsController.deleteDocument(documentId = documentId).map {
                    when (it) {
                        is DeleteDocumentPartialState.Failure -> DashboardInteractorDeleteDocumentPartialState.Failure(
                            errorMessage = it.errorMessage
                        )

                        is DeleteDocumentPartialState.Success -> DashboardInteractorDeleteDocumentPartialState.SingleDocumentDeleted
                    }
                }
            }.collect {
                emit(it)
            }
        }.safeAsync {
            DashboardInteractorDeleteDocumentPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMsg
            )
        }

    override suspend fun tryIssuingDeferredDocumentSuspend(deferredDocumentId: DocumentId): DashboardInteractorRetryIssuingDeferredDocumentPartialState {
        return withContext(Dispatchers.IO) {
            walletCoreDocumentsController.issueDeferredDocument(deferredDocumentId)
                .map { result ->
                    when (result) {
                        is IssueDeferredDocumentPartialState.Failed -> {
                            DashboardInteractorRetryIssuingDeferredDocumentPartialState.Failure(
                                documentId = result.documentId,
                                errorMessage = result.errorMessage
                            )
                        }

                        is IssueDeferredDocumentPartialState.Issued -> {
                            DashboardInteractorRetryIssuingDeferredDocumentPartialState.Success(
                                deferredDocumentData = result.deferredDocumentData
                            )
                        }

                        is IssueDeferredDocumentPartialState.NotReady -> {
                            throw DeferredDocNotReadyYetException("NotReady")
                        }
                    }
                }.retryWhen { cause, attempt ->
                    println("Giannis Suspend Interactor tried again. Cause: $cause, attempt #$attempt")
                    kotlinx.coroutines.delay(2000) //TODO Giannis Make it 5000
                    cause is DeferredDocNotReadyYetException
                }.firstOrNull()
                ?: DashboardInteractorRetryIssuingDeferredDocumentPartialState.Failure(
                    documentId = deferredDocumentId,
                    errorMessage = genericErrorMsg
                ) // Handle the case where the flow does not emit any items
        }
    }

    override fun tryIssuingDeferredDocumentsFlow(
        deferredDocuments: Map<DocumentId, DocType>,
        dispatcher: CoroutineDispatcher,
    ): Flow<DashboardInteractorRetryIssuingDeferredDocumentsPartialState> = flow {
        println("Giannis tryIssuingDeferredDocuments start.")

        val successResults: MutableList<DeferredDocumentData> = mutableListOf()
        val failedResults: MutableList<DocumentId> = mutableListOf()

        /*coroutineScope { */
        withContext(dispatcher) {
            val allJobs = deferredDocuments.keys.map { deferredDocumentId ->
                async/*(dispatcher)*/ {
                    tryIssuingDeferredDocumentSuspend(deferredDocumentId)
                }
            }

            allJobs.forEach { job ->
                when (val result = job.await()) {
                    is DashboardInteractorRetryIssuingDeferredDocumentPartialState.Failure -> {
                        failedResults.add(result.documentId)
                    }

                    is DashboardInteractorRetryIssuingDeferredDocumentPartialState.Success -> {
                        successResults.add(result.deferredDocumentData)
                    }
                }
            }
        }

        println("Giannis tryIssuingDeferredDocuments end. Success: $successResults, Failed: $failedResults")
        if (successResults.isNotEmpty() || failedResults.isNotEmpty()) {
            emit(
                DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result(
                    successfullyIssuedDeferredDocuments = successResults,
                    failedIssuedDeferredDocuments = failedResults
                )
            )
        }
    }.safeAsync {
        DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Failure(
            errorMessage = it.localizedMessage ?: genericErrorMsg
        )
    }
}