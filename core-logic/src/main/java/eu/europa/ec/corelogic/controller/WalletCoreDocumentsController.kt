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

package eu.europa.ec.corelogic.controller

import androidx.core.net.toUri
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.corelogic.config.VciConfig
import eu.europa.ec.corelogic.config.WalletCoreConfig
import eu.europa.ec.corelogic.di.WalletCoreScope
import eu.europa.ec.corelogic.di.getOrCreateKoinScope
import eu.europa.ec.corelogic.extension.documentIdentifier
import eu.europa.ec.corelogic.extension.getLocalizedDisplayName
import eu.europa.ec.corelogic.extension.parseTransactionLog
import eu.europa.ec.corelogic.extension.toCoreTransactionLog
import eu.europa.ec.corelogic.extension.toIssuerRegistrationDomain
import eu.europa.ec.corelogic.extension.toTransactionLogData
import eu.europa.ec.corelogic.extension.toUntrustedIssuerReasonOrNull
import eu.europa.ec.corelogic.model.DeferredDocumentDataDomain
import eu.europa.ec.corelogic.model.DocumentCategories
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.model.FormatType
import eu.europa.ec.corelogic.model.IssuerRegistrationDomain
import eu.europa.ec.corelogic.model.ScopedDocumentDomain
import eu.europa.ec.corelogic.model.TransactionLogDataDomain
import eu.europa.ec.corelogic.model.UntrustedIssuerReasonDomain
import eu.europa.ec.corelogic.model.isBlockedForIssuance
import eu.europa.ec.corelogic.model.toDocumentIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import eu.europa.ec.eudi.statium.Status
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultCreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultCreateKeySettings
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultKeyUnlockData
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.issue.openid4vci.DeferredIssueResult
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer
import eu.europa.ec.eudi.wallet.issue.openid4vci.OfferResult
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.storagelogic.dao.BookmarkDao
import eu.europa.ec.storagelogic.dao.FailedReIssuedDocumentDao
import eu.europa.ec.storagelogic.dao.RevokedDocumentDao
import eu.europa.ec.storagelogic.dao.TransactionLogDao
import eu.europa.ec.storagelogic.model.Bookmark
import eu.europa.ec.storagelogic.model.FailedReIssuedDocument
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLDecoder
import java.util.Locale

enum class IssuanceMethod {
    OPENID4VCI
}

sealed class IssueDocumentsPartialState {
    data class Success(
        val documentIds: List<DocumentId>,
    ) : IssueDocumentsPartialState()

    data class DeferredSuccess(
        val deferredDocuments: Map<DocumentId, FormatType>,
    ) : IssueDocumentsPartialState()

    data class PartialSuccess(
        val documentIds: List<DocumentId>,
        val nonIssuedDocuments: Map<String, String>,
    ) : IssueDocumentsPartialState()

    data class PartialSuccessWithUntrustedIssuer(
        val issuedDocumentIds: List<DocumentId>,
        val untrustedDocuments: Map<FormatType, String>,
    ) : IssueDocumentsPartialState()

    data class Failure(val errorMessage: String) : IssueDocumentsPartialState()

    data class IssuerNotTrusted(
        val reason: UntrustedIssuerReasonDomain,
    ) : IssueDocumentsPartialState()

    data class UserAuthRequired(
        val crypto: BiometricCrypto,
        val resultHandler: DeviceAuthenticationResult,
    ) : IssueDocumentsPartialState()
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
    data class Success(
        val offer: Offer,
        val issuerRegistration: IssuerRegistrationDomain,
    ) : ResolveDocumentOfferPartialState()

    data class Failure(val errorMessage: String) : ResolveDocumentOfferPartialState()

    data class IssuerNotTrusted(
        val reason: UntrustedIssuerReasonDomain,
    ) : ResolveDocumentOfferPartialState()
}

sealed class FetchScopedDocumentsPartialState {
    data class Success(val documents: List<ScopedDocumentDomain>) :
        FetchScopedDocumentsPartialState()

    data class Failure(val errorMessage: String) : FetchScopedDocumentsPartialState()

    data object IssuerNotTrusted : FetchScopedDocumentsPartialState()
}

private sealed class GetIssuerMetadataPartialState {
    data class Success(val metadata: CredentialIssuerMetadata) : GetIssuerMetadataPartialState()
    data object IssuerNotTrusted : GetIssuerMetadataPartialState()
    data class Failure(val errorMessage: String) : GetIssuerMetadataPartialState()
}

sealed class IssueDeferredDocumentPartialState {
    data class Issued(
        val deferredDocumentData: DeferredDocumentDataDomain,
    ) : IssueDeferredDocumentPartialState()

    data class NotReady(
        val deferredDocumentData: DeferredDocumentDataDomain,
    ) : IssueDeferredDocumentPartialState()

    data class Failed(
        val documentId: DocumentId,
        val errorMessage: String,
    ) : IssueDeferredDocumentPartialState()

    data class Expired(
        val documentId: DocumentId,
    ) : IssueDeferredDocumentPartialState()

    data class IssuerNotTrusted(
        val documentId: DocumentId,
    ) : IssueDeferredDocumentPartialState()
}

/**
 * Controller for interacting with internal local storage of Core for CRUD operations on documents
 * */
interface WalletCoreDocumentsController {

    /**
     * @return All the documents from the Database.
     * */
    fun getAllDocuments(): List<Document>

    fun getAllIssuedDocuments(): List<IssuedDocument>

    fun getAllDocumentsByType(documentIdentifiers: List<DocumentIdentifier>): List<IssuedDocument>

    fun getDocumentById(documentId: DocumentId): Document?

    fun getMainPidDocument(): IssuedDocument?

    fun issueDocuments(
        issuanceMethod: IssuanceMethod,
        configIds: List<String>,
        issuerId: String,
        prioritizeDeferred: Boolean = false
    ): Flow<IssueDocumentsPartialState>

    fun issueDocumentsByOffer(
        offer: Offer,
        txCode: String? = null,
        prioritizeDeferred: Boolean = true
    ): Flow<IssueDocumentsPartialState>

    fun reIssueDocument(
        documentId: DocumentId,
        issuerId: String,
        allowAuthorizationFallback: Boolean,
        prioritizeDeferred: Boolean = false
    ): Flow<IssueDocumentsPartialState>

    fun deleteDocument(
        documentId: DocumentId,
    ): Flow<DeleteDocumentPartialState>

    fun deleteAllDocuments(): Flow<DeleteAllDocumentsPartialState>

    fun resolveDocumentOffer(offerUri: String): Flow<ResolveDocumentOfferPartialState>

    fun issueDeferredDocument(docId: DocumentId): Flow<IssueDeferredDocumentPartialState>

    fun resumeOpenId4VciWithAuthorization(uri: String)

    suspend fun getScopedDocuments(locale: Locale): FetchScopedDocumentsPartialState

    fun getAllDocumentCategories(): DocumentCategories

    suspend fun getRevokedDocumentIds(): List<String>

    suspend fun isDocumentRevoked(id: String): Boolean

    suspend fun resolveDocumentStatus(document: IssuedDocument): Result<Status>

    suspend fun getTransactionLogs(): List<TransactionLogDataDomain>

    suspend fun getTransactionLog(id: String): TransactionLogDataDomain?

    suspend fun isDocumentBookmarked(documentId: DocumentId): Boolean

    suspend fun storeBookmark(bookmarkId: String)

    suspend fun deleteBookmark(bookmarkId: String)

    suspend fun isDocumentLowOnCredentials(document: IssuedDocument): Boolean

    suspend fun storeFailedReIssuedDocument(documentId: DocumentId)

    suspend fun deleteFailedReIssuedDocument(documentId: DocumentId)
}

class WalletCoreDocumentsControllerImpl(
    private val resourceProvider: ResourceProvider,
    private val walletCoreConfig: WalletCoreConfig,
    private val bookmarkDao: BookmarkDao,
    private val transactionLogDao: TransactionLogDao,
    private val revokedDocumentDao: RevokedDocumentDao,
    private val failedReIssuedDocumentDao: FailedReIssuedDocumentDao,
    private val prefKeys: PrefKeys,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    walletCore: EudiWallet? = null,
) : WalletCoreDocumentsController {

    private var _eudiWallet: EudiWallet? = walletCore

    private val eudiWallet: EudiWallet
        get() {

            val sessionId = runBlocking(Dispatchers.IO) { prefKeys.getSessionId() }

            if (sessionId.isEmpty()) {
                throw RuntimeException("Missing SessionId")
            }

            return _eudiWallet
                ?: getOrCreateKoinScope<WalletCoreScope>(sessionId).get<EudiWallet>()
                    .also {
                        _eudiWallet = it
                    }
        }

    private val genericErrorMessage
        get() = resourceProvider.genericErrorMessage()

    private val documentErrorMessage
        get() = resourceProvider.getString(R.string.issuance_generic_error)

    /**
     * A map of [OpenId4VciManager] instances, keyed by their [VciConfig].
     * This is initialized lazily, creating a manager for each configuration defined in
     * `[WalletCoreConfig.issuersConfig]`. This allows the controller to interact with multiple
     * credential issuers, each with its own specific configuration.
     */
    private val openId4VciManagers: Map<VciConfig, OpenId4VciManager> by lazy {
        walletCoreConfig.issuersConfig.associateWith { orderConfig ->
            eudiWallet.createOpenId4VciManager(config = orderConfig.config)
        }
    }

    override fun getAllDocuments(): List<Document> =
        eudiWallet.getDocuments { it is IssuedDocument || it is DeferredDocument }

    override fun getAllIssuedDocuments(): List<IssuedDocument> =
        eudiWallet.getDocuments().filterIsInstance<IssuedDocument>()

    private suspend fun getIssuerMetadata(
        manager: OpenId4VciManager,
        issuerId: String
    ): GetIssuerMetadataPartialState {
        return runCatching {
            val metadata = manager.getIssuerMetadata(issuerId)
                .getOrThrow()
            GetIssuerMetadataPartialState.Success(metadata)
        }.getOrElse {
            if (it.toUntrustedIssuerReasonOrNull() != null) {
                GetIssuerMetadataPartialState.IssuerNotTrusted
            } else {
                GetIssuerMetadataPartialState.Failure(it.localizedMessage ?: genericErrorMessage)
            }
        }
    }

    override suspend fun getScopedDocuments(locale: Locale): FetchScopedDocumentsPartialState {
        return withContext(dispatcher) {
            runCatching {

                // every issuer is resolved on its own: one unreachable, malformed or untrusted
                // issuer must not hide the documents offered by the others
                val metadataPerIssuer: Map<VciConfig, GetIssuerMetadataPartialState> =
                    openId4VciManagers.mapValues { (vciConfig, manager) ->
                        getIssuerMetadata(
                            manager = manager,
                            issuerId = vciConfig.issuerUrl
                        )
                    }

                val resolvedMetadata: List<Pair<VciConfig, CredentialIssuerMetadata>> =
                    metadataPerIssuer.mapNotNull { (vciConfig, response) ->
                        when (response) {
                            is GetIssuerMetadataPartialState.Success -> vciConfig to response.metadata
                            is GetIssuerMetadataPartialState.IssuerNotTrusted -> null
                            is GetIssuerMetadataPartialState.Failure -> null
                        }
                    }

                val documents: List<ScopedDocumentDomain> =
                    resolvedMetadata.flatMap { (vciConfig, meta) ->
                        meta.credentialConfigurationsSupported.map { (id, credentialConfig) ->

                            val name: String =
                                credentialConfig.credentialMetadata.getLocalizedDisplayName(
                                    userLocale = locale,
                                    fallback = id.value
                                )

                            val isPid = when (credentialConfig) {
                                is MsoMdocCredential -> credentialConfig.docType.toDocumentIdentifier() == DocumentIdentifier.MdocPid
                                is SdJwtVcCredential -> credentialConfig.type.toDocumentIdentifier() == DocumentIdentifier.SdJwtPid
                                else -> false
                            }

                            val formatType = when (credentialConfig) {
                                is MsoMdocCredential -> credentialConfig.docType
                                is SdJwtVcCredential -> credentialConfig.type
                                else -> null
                            }

                            ScopedDocumentDomain(
                                name = name,
                                configurationId = id.value,
                                credentialIssuerId = vciConfig.issuerUrl,
                                credentialIssuerOrder = vciConfig.order,
                                formatType = formatType,
                                isPid = isPid
                            )
                        }
                    }

                val hasUntrustedIssuer: Boolean = metadataPerIssuer.values.any { response ->
                    response is GetIssuerMetadataPartialState.IssuerNotTrusted
                }

                val firstFailureMessage: String? = metadataPerIssuer.values
                    .filterIsInstance<GetIssuerMetadataPartialState.Failure>()
                    .firstOrNull()
                    ?.errorMessage

                when {
                    documents.isNotEmpty() -> FetchScopedDocumentsPartialState.Success(
                        documents = documents
                    )

                    hasUntrustedIssuer -> FetchScopedDocumentsPartialState.IssuerNotTrusted

                    else -> FetchScopedDocumentsPartialState.Failure(
                        errorMessage = firstFailureMessage ?: genericErrorMessage
                    )
                }
            }
        }.getOrElse {
            FetchScopedDocumentsPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMessage
            )
        }
    }

    override fun getAllDocumentsByType(documentIdentifiers: List<DocumentIdentifier>): List<IssuedDocument> =
        getAllDocuments()
            .filterIsInstance<IssuedDocument>()
            .filter {
                when (it.format) {
                    is MsoMdocFormat -> documentIdentifiers.any { id ->
                        id.formatType == (it.format as MsoMdocFormat).docType
                    }

                    is SdJwtVcFormat -> documentIdentifiers.any { id ->
                        id.formatType == (it.format as SdJwtVcFormat).vct
                    }
                }
            }

    override fun getDocumentById(documentId: DocumentId): Document? {
        return eudiWallet.getDocumentById(documentId = documentId)
    }

    override fun getMainPidDocument(): IssuedDocument? =
        getAllDocumentsByType(
            documentIdentifiers = listOf(
                DocumentIdentifier.MdocPid,
                DocumentIdentifier.SdJwtPid
            )
        ).minByOrNull { it.createdAt }

    override fun issueDocuments(
        issuanceMethod: IssuanceMethod,
        configIds: List<String>,
        issuerId: String,
        prioritizeDeferred: Boolean
    ): Flow<IssueDocumentsPartialState> = flow {
        when (issuanceMethod) {
            IssuanceMethod.OPENID4VCI -> {
                issueDocumentsWithOpenId4VCI(
                    configIds,
                    issuerId,
                    prioritizeDeferred
                ).collect { response ->
                    when (response) {
                        is IssueDocumentsPartialState.Failure -> emit(
                            IssueDocumentsPartialState.Failure(
                                errorMessage = documentErrorMessage
                            )
                        )

                        is IssueDocumentsPartialState.IssuerNotTrusted -> emit(
                            IssueDocumentsPartialState.IssuerNotTrusted(
                                reason = response.reason
                            )
                        )

                        is IssueDocumentsPartialState.Success -> emit(
                            IssueDocumentsPartialState.Success(
                                documentIds = response.documentIds
                            )
                        )

                        is IssueDocumentsPartialState.UserAuthRequired -> emit(
                            IssueDocumentsPartialState.UserAuthRequired(
                                crypto = response.crypto,
                                resultHandler = response.resultHandler
                            )
                        )

                        is IssueDocumentsPartialState.PartialSuccess -> emit(
                            IssueDocumentsPartialState.Success(
                                documentIds = response.documentIds
                            )
                        )

                        is IssueDocumentsPartialState.PartialSuccessWithUntrustedIssuer -> emit(
                            IssueDocumentsPartialState.PartialSuccessWithUntrustedIssuer(
                                issuedDocumentIds = response.issuedDocumentIds,
                                untrustedDocuments = response.untrustedDocuments
                            )
                        )

                        is IssueDocumentsPartialState.DeferredSuccess -> emit(
                            IssueDocumentsPartialState.DeferredSuccess(
                                deferredDocuments = response.deferredDocuments
                            )
                        )
                    }
                }
            }
        }
    }.safeAsync {
        IssueDocumentsPartialState.Failure(errorMessage = documentErrorMessage)
    }

    override fun reIssueDocument(
        documentId: DocumentId,
        issuerId: String,
        allowAuthorizationFallback: Boolean,
        prioritizeDeferred: Boolean
    ): Flow<IssueDocumentsPartialState> = callbackFlow {

        val manager = getVciManager(
            issuerId = issuerId,
            useDefault = true,
            errorMessage = documentErrorMessage
        ).getOrThrow()

        // a successful re-issue makes Wallet Core delete the original document, so the
        // registration outcome is checked before anything starts
        // in that way, a refusal here leaves the original document intact
        val preflightRefusal = preflightRegistrationRefusalOrNull(
            resolveRegistration = {
                manager.resolveIssuerRegistration(documentId = documentId)
            }
        )

        if (preflightRefusal != null) {
            trySendBlocking(preflightRefusal)
        } else {
            manager.reissueDocument(
                documentId,
                allowAuthorizationFallback,
                onIssueEvent = issuanceCallback(prioritizeDeferred = prioritizeDeferred)
            )
        }

        awaitClose()

    }.safeAsync {
        IssueDocumentsPartialState.Failure(
            errorMessage = documentErrorMessage
        )
    }

    override fun issueDocumentsByOffer(
        offer: Offer,
        txCode: String?,
        prioritizeDeferred: Boolean
    ): Flow<IssueDocumentsPartialState> =
        callbackFlow {

            val issuerId = offer
                .credentialOffer
                .credentialIssuerIdentifier
                .toString()

            val manager = getVciManager(
                issuerId = issuerId,
                useDefault = true,
                errorMessage = documentErrorMessage
            ).getOrThrow()

            manager.issueDocumentByOffer(
                offer = offer,
                onIssueEvent = issuanceCallback(prioritizeDeferred = prioritizeDeferred),
                txCode = txCode,
            )
            awaitClose()
        }.safeAsync {
            IssueDocumentsPartialState.Failure(
                errorMessage = documentErrorMessage
            )
        }

    override fun deleteDocument(documentId: String): Flow<DeleteDocumentPartialState> = flow {
        eudiWallet.deleteDocumentById(documentId = documentId)
            .kotlinResult
            .onSuccess {
                revokedDocumentDao.delete(documentId)
                emit(DeleteDocumentPartialState.Success)
            }
            .onFailure {
                emit(
                    DeleteDocumentPartialState.Failure(
                        errorMessage = it.localizedMessage
                            ?: genericErrorMessage
                    )
                )
            }
    }.safeAsync {
        DeleteDocumentPartialState.Failure(
            errorMessage = it.localizedMessage ?: genericErrorMessage
        )
    }

    override fun deleteAllDocuments(): Flow<DeleteAllDocumentsPartialState> =
        flow {

            val allDocuments = getAllDocuments()
            val mainPidDocument = getMainPidDocument()

            mainPidDocument?.let { safeMainPidDocument ->

                val restOfDocuments = allDocuments.filterNot { doc ->
                    doc.id == safeMainPidDocument.id
                }

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
                        documentId = safeMainPidDocument.id
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

            val issuerId = extractCredentialIssuerFromOfferUri(offerUri)
                .getOrDefault("unknown")

            val manager = getVciManager(
                issuerId = issuerId,
                useDefault = true,
                errorMessage = genericErrorMessage
            ).getOrThrow()

            manager.resolveDocumentOffer(offerUri) { result ->
                when (result) {
                    is OfferResult.Failure -> {
                        val untrustedReason = result.cause.toUntrustedIssuerReasonOrNull()

                        trySendBlocking(
                            if (untrustedReason != null) {
                                ResolveDocumentOfferPartialState.IssuerNotTrusted(
                                    reason = untrustedReason
                                )
                            } else {
                                ResolveDocumentOfferPartialState.Failure(
                                    result.cause.localizedMessage ?: genericErrorMessage
                                )
                            }
                        )
                    }

                    is OfferResult.Success -> {
                        val issuerRegistration = result.offer.issuerRegistration
                            .toIssuerRegistrationDomain(locale = resourceProvider.getLocale())
                        val registrationRefused = issuerRegistration.isBlockedForIssuance

                        trySendBlocking(
                            if (registrationRefused) {
                                ResolveDocumentOfferPartialState.IssuerNotTrusted(
                                    reason = UntrustedIssuerReasonDomain.REGISTRATION_CERTIFICATE
                                )
                            } else {
                                ResolveDocumentOfferPartialState.Success(
                                    offer = result.offer,
                                    issuerRegistration = issuerRegistration,
                                )
                            }
                        )
                    }
                }
            }
            awaitClose()
        }.safeAsync {
            ResolveDocumentOfferPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMessage
            )
        }

    override fun issueDeferredDocument(docId: DocumentId): Flow<IssueDeferredDocumentPartialState> =
        callbackFlow {
            (getDocumentById(docId) as? DeferredDocument)?.let { deferredDoc ->

                val manager = deferredDoc.issuerMetadata?.credentialIssuerIdentifier
                    ?.let(openId4VciManagers::get)
                    ?: openId4VciManagers.values.firstOrNull()

                require(manager != null) { documentErrorMessage }

                manager.issueDeferredDocument(
                    deferredDocument = deferredDoc,
                    executor = null,
                    onIssueResult = { deferredIssuanceResult ->
                        when (deferredIssuanceResult) {
                            is DeferredIssueResult.DocumentFailed -> {
                                trySendBlocking(
                                    if (deferredIssuanceResult.cause.toUntrustedIssuerReasonOrNull() != null) {
                                        IssueDeferredDocumentPartialState.IssuerNotTrusted(
                                            documentId = deferredIssuanceResult.documentId
                                        )
                                    } else {
                                        IssueDeferredDocumentPartialState.Failed(
                                            documentId = deferredIssuanceResult.documentId,
                                            errorMessage = deferredIssuanceResult.cause.localizedMessage
                                                ?: documentErrorMessage
                                        )
                                    }
                                )
                            }

                            is DeferredIssueResult.DocumentIssued -> {
                                trySendBlocking(
                                    IssueDeferredDocumentPartialState.Issued(
                                        DeferredDocumentDataDomain(
                                            documentId = deferredIssuanceResult.documentId,
                                            formatType = deferredIssuanceResult.docType,
                                            docName = deferredIssuanceResult.name
                                        )
                                    )
                                )
                            }

                            is DeferredIssueResult.DocumentNotReady -> {
                                trySendBlocking(
                                    IssueDeferredDocumentPartialState.NotReady(
                                        DeferredDocumentDataDomain(
                                            documentId = deferredIssuanceResult.documentId,
                                            formatType = deferredIssuanceResult.docType,
                                            docName = deferredIssuanceResult.name
                                        )
                                    )
                                )
                            }

                            is DeferredIssueResult.DocumentExpired -> {
                                trySendBlocking(
                                    IssueDeferredDocumentPartialState.Expired(
                                        documentId = deferredIssuanceResult.documentId
                                    )
                                )
                            }
                        }
                    }
                )
            } ?: trySendBlocking(
                IssueDeferredDocumentPartialState.Failed(
                    documentId = docId,
                    errorMessage = documentErrorMessage
                )
            )

            awaitClose()
        }.safeAsync {
            IssueDeferredDocumentPartialState.Failed(
                documentId = docId,
                errorMessage = it.localizedMessage ?: genericErrorMessage
            )
        }

    override fun resumeOpenId4VciWithAuthorization(uri: String) {
        for (manager in openId4VciManagers.values) {
            try {
                manager.resumeWithAuthorization(uri)
                break
            } catch (_: Exception) {
            }
        }
    }

    override fun getAllDocumentCategories(): DocumentCategories {
        return walletCoreConfig.documentCategories
    }

    override suspend fun getTransactionLogs(): List<TransactionLogDataDomain> =
        withContext(dispatcher) {
            transactionLogDao.retrieveAll()
                .mapNotNull { transactionLog ->
                    transactionLog
                        .toCoreTransactionLog()
                        ?.parseTransactionLog()
                        ?.toTransactionLogData(transactionLog.identifier)
                }
        }

    override suspend fun getTransactionLog(id: String): TransactionLogDataDomain? =
        withContext(dispatcher) {
            transactionLogDao.retrieve(id)
                ?.toCoreTransactionLog()
                ?.parseTransactionLog()
                ?.toTransactionLogData(id)
        }

    override suspend fun isDocumentBookmarked(documentId: DocumentId): Boolean =
        bookmarkDao.retrieve(documentId) != null

    override suspend fun storeBookmark(bookmarkId: DocumentId) =
        bookmarkDao.store(Bookmark(bookmarkId))

    override suspend fun deleteBookmark(bookmarkId: DocumentId) =
        bookmarkDao.delete(bookmarkId)

    override suspend fun storeFailedReIssuedDocument(documentId: DocumentId) {
        failedReIssuedDocumentDao.store(FailedReIssuedDocument(documentId))
    }

    override suspend fun deleteFailedReIssuedDocument(documentId: DocumentId) {
        failedReIssuedDocumentDao.delete(documentId)
    }

    override suspend fun isDocumentLowOnCredentials(document: IssuedDocument): Boolean {
        val documentRemainingCredentials = document.credentialsCount()

        return document.credentialPolicy is CreateDocumentSettings.CredentialPolicy.OnceOnly
                && documentRemainingCredentials <= 1
    }

    override suspend fun getRevokedDocumentIds(): List<String> =
        revokedDocumentDao.retrieveAll().map { it.identifier }

    override suspend fun isDocumentRevoked(id: String): Boolean =
        revokedDocumentDao.retrieve(id) != null

    override suspend fun resolveDocumentStatus(document: IssuedDocument): Result<Status> =
        eudiWallet.resolveStatus(document)

    private fun issueDocumentsWithOpenId4VCI(
        configIds: List<String>,
        issuerId: String,
        prioritizeDeferred: Boolean
    ): Flow<IssueDocumentsPartialState> =
        callbackFlow {

            val manager = getVciManager(
                issuerId = issuerId,
                useDefault = false,
                errorMessage = documentErrorMessage
            ).getOrThrow()

            // wallet-initiated issuance has no approval screen, so the registration outcome
            // is checked before the flow starts — a refusal here opens no browser and stores
            // nothing
            val preflightRefusal = preflightRegistrationRefusalOrNull(
                resolveRegistration = {
                    manager.resolveIssuerRegistration(
                        issuerUrl = issuerId,
                        credentialConfigurationIds = configIds
                    )
                }
            )

            if (preflightRefusal != null) {
                trySendBlocking(preflightRefusal)
            } else {
                manager.issueDocumentByConfigurationIdentifiers(
                    issuerUrl = issuerId,
                    credentialConfigurationIds = configIds,
                    onIssueEvent = issuanceCallback(prioritizeDeferred = prioritizeDeferred)
                )
            }

            awaitClose()

        }.safeAsync {
            IssueDocumentsPartialState.Failure(
                errorMessage = documentErrorMessage
            )
        }

    private fun ProducerScope<IssueDocumentsPartialState>.issuanceCallback(
        prioritizeDeferred: Boolean = true
    ): OpenId4VciManager.OnIssueEvent {

        var totalDocumentsToBeIssued = 0
        val untrustedDocuments: MutableMap<FormatType, String> = mutableMapOf()
        val nonIssuedDocuments: MutableMap<FormatType, String> = mutableMapOf()
        val deferredDocuments: MutableMap<DocumentId, FormatType> = mutableMapOf()
        val issuedDocuments: MutableMap<DocumentId, FormatType> = mutableMapOf()

        val listener = OpenId4VciManager.OnIssueEvent { event ->
            when (event) {
                is IssueEvent.DocumentFailed -> {
                    if (event.cause.toUntrustedIssuerReasonOrNull() != null) {
                        untrustedDocuments[event.docType] = event.name
                    } else {
                        nonIssuedDocuments[event.docType] = event.name
                    }
                }

                is IssueEvent.DocumentRequiresCreateSettings -> {
                    when (event) {
                        is IssueEvent.DocumentRequiresCreateSettings.MandatoryReusePolicy -> {
                            val (secureAreaId, createKeySettings) = eudiWallet.getDefaultCreateKeySettings()
                            event.resume(secureAreaId, createKeySettings)
                        }

                        is IssueEvent.DocumentRequiresCreateSettings.OptionalReusePolicy -> {
                            val offeredDocIdentifier = event.offeredDocument.documentIdentifier
                            val configuredPolicy = walletCoreConfig
                                .documentIssuanceConfig
                                .getPolicyForDocument(documentIdentifier = offeredDocIdentifier)

                            val maxBatchSize = event.offeredDocument.batchCredentialIssuanceSize
                            val safeMaxNumberOfCredentials = minOf(
                                maxBatchSize,
                                configuredPolicy.numberOfCredentials
                            )

                            val adjustedPolicy = when (configuredPolicy) {
                                is CreateDocumentSettings.CredentialPolicy.LimitedTime -> {
                                    configuredPolicy
                                }

                                is CreateDocumentSettings.CredentialPolicy.OnceOnly -> {
                                    configuredPolicy.copy(
                                        numberOfCredentials = safeMaxNumberOfCredentials
                                    )
                                }

                                is CreateDocumentSettings.CredentialPolicy.RotatingBatch -> {
                                    configuredPolicy.copy(
                                        numberOfCredentials = safeMaxNumberOfCredentials
                                    )
                                }
                            }

                            val createDocumentSettings =
                                eudiWallet.getDefaultCreateDocumentSettings(
                                    offeredDocument = event.offeredDocument,
                                    credentialPolicy = adjustedPolicy
                                )

                            event.resume(createDocumentSettings)
                        }
                    }
                }

                is IssueEvent.DocumentRequiresUserAuth -> {
                    launch {
                        val keyUnlockDataMap =
                            event.keysRequireAuth.mapValues { (keyAlias, secureArea) ->
                                getDefaultKeyUnlockData(secureArea, keyAlias)
                            }

                        val keyUnlockData =
                            keyUnlockDataMap.values.first() //TODO: Revisit this once Core adds support.

                        val cryptoObject = keyUnlockData?.getCryptoObjectForSigning()

                        trySendBlocking(
                            IssueDocumentsPartialState.UserAuthRequired(
                                crypto = BiometricCrypto(cryptoObject),
                                resultHandler = DeviceAuthenticationResult(
                                    onAuthenticationSuccess = { event.resume(keyUnlockDataMap) },
                                    onAuthenticationError = { event.cancel(null) }
                                )
                            )
                        )
                    }
                }

                is IssueEvent.Failure -> {
                    val untrustedReason = event.cause.toUntrustedIssuerReasonOrNull()

                    trySendBlocking(
                        if (untrustedReason != null) {
                            IssueDocumentsPartialState.IssuerNotTrusted(reason = untrustedReason)
                        } else {
                            IssueDocumentsPartialState.Failure(
                                errorMessage = documentErrorMessage
                            )
                        }
                    )
                }

                is IssueEvent.Finished -> {

                    // event.issuedDocuments folds in deferred ids too — the Core emits
                    // Finished(issuedDocumentIds + deferredDocumentIds) — so the untrusted
                    // outcome is decided on the locally tracked *actually issued* documents.
                    // Otherwise a batch of "one untrusted + one deferred from the same issuer"
                    // would be treated as a partial success and navigate the user on the success screen with a deferred
                    // (un-issued) id instead of the "Issuance blocked" sheet.
                    if (untrustedDocuments.isNotEmpty() && issuedDocuments.isEmpty()) {
                        trySendBlocking(
                            IssueDocumentsPartialState.IssuerNotTrusted(
                                reason = UntrustedIssuerReasonDomain.ACCESS_CERTIFICATE
                            )
                        )
                        return@OnIssueEvent
                    }

                    if (untrustedDocuments.isNotEmpty()) {
                        trySendBlocking(
                            IssueDocumentsPartialState.PartialSuccessWithUntrustedIssuer(
                                issuedDocumentIds = issuedDocuments.keys.toList(),
                                untrustedDocuments = untrustedDocuments
                            )
                        )
                        return@OnIssueEvent
                    }

                    if (deferredDocuments.isNotEmpty() && (prioritizeDeferred || (issuedDocuments.isEmpty()))) {
                        trySendBlocking(
                            IssueDocumentsPartialState.DeferredSuccess(
                                deferredDocuments = deferredDocuments
                            )
                        )
                        return@OnIssueEvent
                    }

                    if (event.issuedDocuments.isEmpty()) {
                        trySendBlocking(
                            IssueDocumentsPartialState.Failure(
                                errorMessage = documentErrorMessage
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
                }

                is IssueEvent.DocumentDeferred -> {
                    deferredDocuments[event.documentId] = event.docType
                }
            }
        }

        return listener
    }

    /**
     * Registration check for a flow with no approval screen, run before it starts. Returns the
     * terminal state refusing or failing the flow, or null to proceed.
     */
    private suspend fun preflightRegistrationRefusalOrNull(
        resolveRegistration: suspend () -> Result<RegistrationCertificateResult>,
    ): IssueDocumentsPartialState? {
        val resolution = resolveRegistration()

        return resolution.fold(
            onSuccess = { result ->
                val issuerRegistration = result
                    .toIssuerRegistrationDomain(locale = resourceProvider.getLocale())
                if (issuerRegistration.isBlockedForIssuance) {
                    IssueDocumentsPartialState.IssuerNotTrusted(
                        reason = UntrustedIssuerReasonDomain.REGISTRATION_CERTIFICATE
                    )
                } else {
                    null
                }
            },
            onFailure = { cause ->
                val untrustedReason = cause.toUntrustedIssuerReasonOrNull()
                when {
                    untrustedReason != null -> IssueDocumentsPartialState.IssuerNotTrusted(
                        reason = untrustedReason
                    )

                    // no registration certificate published; refuse like any unverified outcome
                    cause is IllegalStateException -> IssueDocumentsPartialState.IssuerNotTrusted(
                        reason = UntrustedIssuerReasonDomain.REGISTRATION_CERTIFICATE
                    )

                    else -> IssueDocumentsPartialState.Failure(
                        errorMessage = documentErrorMessage
                    )
                }
            },
        )
    }

    private fun extractCredentialIssuerFromOfferUri(offerUri: String): Result<String> =
        runCatching {
            val credentialOffer = offerUri.toUri().getQueryParameter("credential_offer")
            val decoded = URLDecoder.decode(credentialOffer, "UTF-8")
            val json = JSONObject(decoded)
            json.getString("credential_issuer")
        }

    private fun getVciManager(
        issuerId: String,
        useDefault: Boolean,
        errorMessage: String
    ): Result<OpenId4VciManager> {

        val manager = openId4VciManagers.entries
            .firstOrNull { (vciConfig, _) -> vciConfig.issuerUrl == issuerId }
            ?.value
            ?: if (useDefault) openId4VciManagers.values.firstOrNull() else null

        return manager?.let(Result.Companion::success)
            ?: Result.failure(RuntimeException(errorMessage))
    }
}