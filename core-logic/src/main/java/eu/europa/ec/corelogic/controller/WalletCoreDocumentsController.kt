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

import androidx.annotation.VisibleForTesting
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.corelogic.config.VciConfig
import eu.europa.ec.corelogic.config.WalletCoreConfig
import eu.europa.ec.corelogic.di.WalletCoreScope
import eu.europa.ec.corelogic.di.getOrCreateKoinScope
import eu.europa.ec.corelogic.extension.documentIdentifier
import eu.europa.ec.corelogic.extension.getIssuerLogo
import eu.europa.ec.corelogic.extension.getLocalizedDisplayName
import eu.europa.ec.corelogic.extension.isDeferredTrustVerdict
import eu.europa.ec.corelogic.extension.isUnknownDeferredTransaction
import eu.europa.ec.corelogic.extension.localizedIssuerMetadata
import eu.europa.ec.corelogic.extension.toIssuerRegistrationDomain
import eu.europa.ec.corelogic.extension.toIssuerTrustVerdict
import eu.europa.ec.corelogic.extension.toIssuerTrustVerdictDomain
import eu.europa.ec.corelogic.extension.toUntrustedIssuerReasonOrNull
import eu.europa.ec.corelogic.extension.withAuthenticationObserver
import eu.europa.ec.corelogic.model.DeferredDocumentDataDomain
import eu.europa.ec.corelogic.model.DocumentCategories
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.model.FormatType
import eu.europa.ec.corelogic.model.IssuerRegistrationDomain
import eu.europa.ec.corelogic.model.IssuerTrustVerdictDomain
import eu.europa.ec.corelogic.model.ScopedDocumentDomain
import eu.europa.ec.corelogic.model.UntrustedIssuerReasonDomain
import eu.europa.ec.corelogic.model.isBlockedForIssuance
import eu.europa.ec.corelogic.model.toDocumentIdentifier
import eu.europa.ec.corelogic.util.OfferIssuer
import eu.europa.ec.corelogic.util.issuerLogoOrNull
import eu.europa.ec.corelogic.util.offerIssuerOf
import eu.europa.ec.corelogic.worker.WalletWorkScheduler
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
import eu.europa.ec.eudi.wallet.statium.DefaultStatusReferenceExtractor
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.storagelogic.dao.BookmarkDao
import eu.europa.ec.storagelogic.dao.DeferredDocumentScheduleDao
import eu.europa.ec.storagelogic.dao.FailedReIssuedDocumentDao
import eu.europa.ec.storagelogic.dao.IssuerTrustDao
import eu.europa.ec.storagelogic.dao.RevokedDocumentDao
import eu.europa.ec.storagelogic.dao.StatusRefreshFailureDao
import eu.europa.ec.storagelogic.model.Bookmark
import eu.europa.ec.storagelogic.model.DeferredDocumentSchedule
import eu.europa.ec.storagelogic.model.FailedReIssuedDocument
import eu.europa.ec.storagelogic.model.IssuerTrust
import eu.europa.ec.storagelogic.model.IssuerTrustVerdict
import eu.europa.ec.storagelogic.model.RevocationStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.URI
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

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

    /**
     * The documents in [issuedDocumentIds] were issued. [untrustedDocuments] were refused because
     * their issuer could not be trusted, and [nonIssuedDocuments] were not issued for another
     * reason.
     */
    data class PartialSuccessWithUntrustedIssuer(
        val issuedDocumentIds: List<DocumentId>,
        val untrustedDocuments: Map<FormatType, String>,
        val nonIssuedDocuments: Map<FormatType, String>,
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
    data class Success(
        val documents: List<ScopedDocumentDomain>
    ) : FetchScopedDocumentsPartialState()

    data class Failure(val errorMessage: String) : FetchScopedDocumentsPartialState()

    data object NoTrustedIssuers : FetchScopedDocumentsPartialState()
}

private sealed class GetIssuerMetadataPartialState {
    data class Success(val metadata: CredentialIssuerMetadata) : GetIssuerMetadataPartialState()
    data object IssuerNotTrusted : GetIssuerMetadataPartialState()
    data class Failure(val errorMessage: String) : GetIssuerMetadataPartialState()
}

sealed class IssueDeferredDocumentPartialState {
    /**
     * A lifecycle or removal gate closed before submission. Restore the previous deadline so
     * returning to the foreground can immediately retry a document that was already due.
     */
    data object Paused : IssueDeferredDocumentPartialState()

    data class Issued(
        val deferredDocumentData: DeferredDocumentDataDomain,
    ) : IssueDeferredDocumentPartialState()

    /**
     * @property interval The issuer's requested wait, when supplied by Core. The scheduler owns
     * validation and fallback selection; the controller forwards the value unchanged.
     */
    data class NotReady(
        val deferredDocumentData: DeferredDocumentDataDomain,
        val interval: Duration?,
    ) : IssueDeferredDocumentPartialState()

    /** An ordinary DocumentFailed callback from Core. The transaction remains eligible for another attempt. */
    data object Failed : IssueDeferredDocumentPartialState()

    /** A local lookup or setup failed before a Core result was available. Preserve the visible status. */
    data object Unavailable : IssueDeferredDocumentPartialState()

    data class Expired(
        val documentId: DocumentId,
    ) : IssueDeferredDocumentPartialState()

    /** A trust rejection ends polling for this document and leaves it available for user removal. */
    data class IssuerNotTrusted(
        val documentId: DocumentId,
    ) : IssueDeferredDocumentPartialState()
}

/**
 * Document storage and issuance boundary to Wallet Core, including app-owned document metadata.
 * Deferred callbacks register persisted schedules here; the scheduler owns deadlines and retries.
 * Issuance and removal operations use shared coordination so reset can await Core calls and their
 * metadata writes independently of the screen collecting their results.
 */
interface WalletCoreDocumentsController {

    /**
     * @return All the documents from the Database.
     * */
    fun getAllDocuments(): List<Document>

    /** Every document Wallet Core stores, including those whose issuance has not finished. */
    fun getAllStoredDocuments(): List<Document>

    fun getAllIssuedDocuments(): List<IssuedDocument>

    fun getAllDocumentsByType(documentIdentifiers: List<DocumentIdentifier>): List<IssuedDocument>

    fun getDocumentById(documentId: DocumentId): Document?

    /**
     * The logo of [document]'s issuer for [locale], when the wallet may load it: see
     * [issuerLogoOrNull] and `VciConfig.logoOrigins`.
     */
    fun getIssuerLogo(document: Document, locale: Locale): URI?

    /** The logo of [offer]'s issuer for [locale], under the same rule as for a document. */
    fun getIssuerLogo(offer: Offer, locale: Locale): URI?

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

    /**
     * Tracks this submission through authentication and its terminal result. Each manual request can
     * create a replacement. The automatic worker checks for active replacements before submitting.
     */
    fun reIssueDocument(
        documentId: DocumentId,
        issuerId: String,
        allowAuthorizationFallback: Boolean,
        prioritizeDeferred: Boolean = false
    ): Flow<IssueDocumentsPartialState>

    /**
     * Removes the selected ID after any active Deferred attempt and its writes finish, even if the
     * document became Issued meanwhile. Removing an original does not remove its replacement.
     */
    fun deleteDocument(
        documentId: DocumentId,
    ): Flow<DeleteDocumentPartialState>

    /**
     * Reset-only deletion, allowed after workers and tracked document operations have drained.
     * New ordinary operations remain blocked while reset uses this explicit entry point.
     */
    fun deleteDocumentForReset(documentId: DocumentId): Flow<DeleteDocumentPartialState>

    /**
     * Removes all documents, including Pending replacements, after active operations finish.
     * Keeps the main PID until the other deletions succeed; reset waits for the whole operation.
     */
    fun deleteAllDocuments(): Flow<DeleteAllDocumentsPartialState>

    fun resolveDocumentOffer(offerUri: String): Flow<ResolveDocumentOfferPartialState>

    /**
     * Performs one Deferred attempt. The scheduler owns the document lock, concurrency slot,
     * deadlines and result persistence. The conditions for starting the request are checked again
     * immediately before the Core call; [IssueDeferredDocumentPartialState.Paused] means no request
     * was started.
     */
    fun issueDeferredDocument(docId: DocumentId): Flow<IssueDeferredDocumentPartialState>

    fun resumeOpenId4VciWithAuthorization(uri: String): Boolean

    suspend fun getScopedDocuments(locale: Locale): FetchScopedDocumentsPartialState

    fun getAllDocumentCategories(): DocumentCategories

    suspend fun isDocumentRevoked(id: String): Boolean

    /**
     * Whether the document is blocked because its issuer suspended it. A suspension can be lifted,
     * unlike a revocation; [isDocumentRevoked] is true for both.
     */
    suspend fun isDocumentSuspended(id: String): Boolean

    /**
     * The issuer-trust outcome recorded when [documentId] was issued. Null means no verdict was
     * recorded, which is distinct from a recorded outcome that says no evaluation took place.
     */
    suspend fun getIssuerTrustVerdict(documentId: DocumentId): IssuerTrustVerdictDomain?

    suspend fun resolveDocumentStatus(document: IssuedDocument): Result<Status>

    /**
     * Whether [document] carries a status-list reference. A document without one has no status to
     * check, so a failed resolution for it is not a failed refresh.
     */
    suspend fun hasStatusReference(document: IssuedDocument): Boolean

    /** Whether the last attempt to refresh the status of [documentId] failed. */
    suspend fun hasStatusRefreshFailed(documentId: DocumentId): Boolean

    /** Whether the last automatic re-issuance of [documentId] failed. */
    suspend fun hasReIssuanceFailed(documentId: DocumentId): Boolean

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
    private val revokedDocumentDao: RevokedDocumentDao,
    private val failedReIssuedDocumentDao: FailedReIssuedDocumentDao,
    private val issuerTrustDao: IssuerTrustDao,
    private val statusRefreshFailureDao: StatusRefreshFailureDao,
    private val prefKeys: PrefKeys,
    private val walletWorkScheduler: WalletWorkScheduler,
    private val deferredScheduleDao: DeferredDocumentScheduleDao,
    private val walletDocumentOperationController: WalletDocumentOperationController,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    walletCore: EudiWallet? = null,
) : WalletCoreDocumentsController {

    private var _eudiWallet: EudiWallet? = walletCore

    @Volatile
    private var lastAuthorizationIssuerId: String? = null

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

    private val unconfiguredIssuerMessage
        get() = resourceProvider.getString(
            R.string.issuance_document_offer_error_issuer_not_configured
        )

    private val unreadableOfferMessage
        get() = resourceProvider.getString(R.string.issuance_document_offer_error_no_document)

    private val configuredIssuerIds: List<String>
        get() = openId4VciManagers.keys.map { it.issuerUrl }

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

    override fun getAllStoredDocuments(): List<Document> = eudiWallet.getDocuments()

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
                            is GetIssuerMetadataPartialState.Success ->
                                vciConfig to response.metadata

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
                                is MsoMdocCredential -> credentialConfig.docType
                                    .toDocumentIdentifier() == DocumentIdentifier.MdocPid

                                is SdJwtVcCredential -> credentialConfig.type
                                    .toDocumentIdentifier() == DocumentIdentifier.SdJwtPid

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

                    hasUntrustedIssuer -> FetchScopedDocumentsPartialState.NoTrustedIssuers

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

    override fun getAllDocumentsByType(
        documentIdentifiers: List<DocumentIdentifier>
    ): List<IssuedDocument> =
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

    override fun getIssuerLogo(document: Document, locale: Locale): URI? {
        val issuerId = document.issuerMetadata?.credentialIssuerIdentifier
        return issuerLogoOrNull(
            logo = document.localizedIssuerMetadata(locale)?.logo?.uri,
            issuerId = issuerId,
            additionalOrigins = logoOriginsOf(issuerId),
        )
    }

    override fun getIssuerLogo(offer: Offer, locale: Locale): URI? {
        val issuerId = offer.credentialOffer.credentialIssuerIdentifier.toString()
        return issuerLogoOrNull(
            logo = offer.getIssuerLogo(locale),
            issuerId = issuerId,
            additionalOrigins = logoOriginsOf(issuerId),
        )
    }

    private fun logoOriginsOf(issuerId: String?): List<String> =
        walletCoreConfig.issuersConfig.firstOrNull { it.issuerUrl == issuerId }?.logoOrigins
            .orEmpty()

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
                            IssueDocumentsPartialState.PartialSuccess(
                                documentIds = response.documentIds,
                                nonIssuedDocuments = response.nonIssuedDocuments,
                            )
                        )

                        is IssueDocumentsPartialState.PartialSuccessWithUntrustedIssuer -> emit(
                            IssueDocumentsPartialState.PartialSuccessWithUntrustedIssuer(
                                issuedDocumentIds = response.issuedDocumentIds,
                                untrustedDocuments = response.untrustedDocuments,
                                nonIssuedDocuments = response.nonIssuedDocuments,
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
    }.trackedIssuance()

    override fun reIssueDocument(
        documentId: DocumentId,
        issuerId: String,
        allowAuthorizationFallback: Boolean,
        prioritizeDeferred: Boolean
    ): Flow<IssueDocumentsPartialState> = callbackFlow {

        val manager = getVciManager(
            issuerId = issuerId,
            errorMessage = documentErrorMessage
        ).getOrThrow()

        // a successful re-issue makes Wallet Core delete the original document, so the
        // registration outcome is checked before anything starts
        // in that way, a refusal here leaves the original document intact
        val preflightRefusal = reIssueBlockedRefusalOrNull(documentId)
            ?: preflightRegistrationRefusalOrNull(
                resolveRegistration = {
                    manager.resolveIssuerRegistration(documentId = documentId)
                }
            )

        if (preflightRefusal != null) {
            trySendBlocking(preflightRefusal)
        } else {
            if (allowAuthorizationFallback) {
                lastAuthorizationIssuerId = issuerId
            }

            manager.reissueDocument(
                documentId,
                allowAuthorizationFallback,
                onIssueEvent = issuanceCallback(
                    prioritizeDeferred = prioritizeDeferred,
                    originalDocumentId = documentId,
                )
            )
        }

        awaitClose()

    }.onEach { state ->
        cleanUpAfterReIssuance(originalId = documentId, state = state)
    }.safeAsync {
        IssueDocumentsPartialState.Failure(
            errorMessage = documentErrorMessage
        )
    }.trackedIssuance()

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
                errorMessage = documentErrorMessage
            ).getOrThrow()

            lastAuthorizationIssuerId = issuerId

            manager.issueDocumentByOffer(
                offer = offer,
                onIssueEvent = issuanceCallback(
                    prioritizeDeferred = prioritizeDeferred,
                    originalDocumentId = null
                ),
                txCode = txCode,
            )
            awaitClose()
        }.safeAsync {
            IssueDocumentsPartialState.Failure(
                errorMessage = documentErrorMessage
            )
        }.trackedIssuance()

    override fun deleteDocument(
        documentId: String,
    ): Flow<DeleteDocumentPartialState> = deleteDocumentAsTrackedOperation(
        documentId = documentId,
        duringReset = false,
    )

    override fun deleteDocumentForReset(documentId: String): Flow<DeleteDocumentPartialState> =
        deleteDocumentAsTrackedOperation(
            documentId = documentId,
            duringReset = true,
        )

    /**
     * Tracks Core deletion and metadata cleanup as one operation for reset. The removal guard
     * blocks new pings for this ID before waiting for its document lock and any active attempt.
     */
    private fun deleteDocumentAsTrackedOperation(
        documentId: String,
        duringReset: Boolean,
    ): Flow<DeleteDocumentPartialState> = walletDocumentOperationController.trackedOperationFlow(
        completeWhen = { true },
        isResetOperation = duringReset,
        flowToCollect = {
            deleteDocumentAndCleanUpLocalMetadata(documentId)
        },
    ).safeAsync {
        DeleteDocumentPartialState.Failure(it.localizedMessage ?: genericErrorMessage)
    }

    /**
     * Deletes [documentId] from Wallet Core. The caller tracks the removal for reset.
     * Success requires clearing the document's Room polling schedule. Cleanup of its other
     * local metadata is best effort.
     */
    private fun deleteDocumentAndCleanUpLocalMetadata(
        documentId: String,
    ): Flow<DeleteDocumentPartialState> = flow {
        val result = walletDocumentOperationController.withRemoval(documentId) {
            // A user Remove still targets this ID if an in-flight ping issued it while waiting.
            eudiWallet.deleteDocumentById(documentId).kotlinResult.fold(
                onSuccess = {
                    // Remember the confirmed removal before fallible cleanup so a late callback
                    // cannot recreate the schedule. Reset also waits for the queue deletion.
                    walletDocumentOperationController.recordRemoval(documentId)

                    // Wallet Core has already deleted the document. If deleting the Room schedule fails,
                    // safeAsync reports Failure without undoing the document deletion.
                    deferredScheduleDao.delete(documentId)

                    runBestEffortLocalMetadataUpdate {
                        deleteLocalDocumentMetadata(documentId)
                    }

                    DeleteDocumentPartialState.Success
                },
                onFailure = { cause ->
                    DeleteDocumentPartialState.Failure(
                        cause.localizedMessage ?: genericErrorMessage
                    )
                },
            )
        }
        emit(result)
    }.safeAsync {
        DeleteDocumentPartialState.Failure(it.localizedMessage ?: genericErrorMessage)
    }

    override fun deleteAllDocuments(): Flow<DeleteAllDocumentsPartialState> =
        flow {
            val removalOperation = walletDocumentOperationController.tryStartRemovingAllDocuments {
                deleteAllDocumentsWithMainPidLast()
            }
            emit(
                removalOperation?.await()
                    ?: DeleteAllDocumentsPartialState.Failure(errorMessage = genericErrorMessage)
            )
        }.safeAsync {
            DeleteAllDocumentsPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMessage
            )
        }

    /**
     * Runs within [WalletDocumentOperationController.tryStartRemovingAllDocuments] after earlier
     * operations finish. Reading the documents here includes any replacement issued during the wait.
     */
    private suspend fun deleteAllDocumentsWithMainPidLast(): DeleteAllDocumentsPartialState {
        val allDocuments = getAllDocuments()
        val mainPidDocument = getMainPidDocument()
            ?: return DeleteAllDocumentsPartialState.Failure(errorMessage = genericErrorMessage)
        val documentsExceptMainPid = allDocuments.filterNot { document ->
            document.id == mainPidDocument.id
        }
        var lastDeletionErrorMessage: String? = null

        for (document in documentsExceptMainPid) {
            when (
                val result = deleteDocumentAndCleanUpLocalMetadata(document.id).first()
            ) {
                is DeleteDocumentPartialState.Failure -> {
                    lastDeletionErrorMessage = result.errorMessage
                }

                is DeleteDocumentPartialState.Success -> {
                    // No-op
                }
            }
        }

        lastDeletionErrorMessage?.let { message ->
            return DeleteAllDocumentsPartialState.Failure(errorMessage = message)
        }
        return when (
            val result = deleteDocumentAndCleanUpLocalMetadata(mainPidDocument.id).first()
        ) {
            is DeleteDocumentPartialState.Failure ->
                DeleteAllDocumentsPartialState.Failure(errorMessage = result.errorMessage)

            is DeleteDocumentPartialState.Success -> DeleteAllDocumentsPartialState.Success
        }
    }

    override fun resolveDocumentOffer(offerUri: String): Flow<ResolveDocumentOfferPartialState> =
        callbackFlow {

            val issuerId = when (
                val offerIssuer =
                    offerIssuerOf(offerUri = offerUri, configuredIssuers = configuredIssuerIds)
            ) {
                is OfferIssuer.Configured -> offerIssuer.issuerId
                is OfferIssuer.NotConfigured -> throw IllegalArgumentException(
                    unconfiguredIssuerMessage
                )

                is OfferIssuer.Unreadable -> throw IllegalArgumentException(unreadableOfferMessage)
            }

            val manager = getVciManager(
                issuerId = issuerId,
                errorMessage = unconfiguredIssuerMessage
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

                    is OfferResult.Success if result.offer.credentialOffer
                        .credentialIssuerIdentifier.toString() !in configuredIssuerIds -> {
                        trySendBlocking(
                            ResolveDocumentOfferPartialState.Failure(unconfiguredIssuerMessage)
                        )
                    }

                    is OfferResult.Success -> {
                        val issuerRegistration = result.offer.issuerRegistration
                            .toIssuerRegistrationDomain(locale = resourceProvider.getLocale())
                        // with the registration check off, Core evaluates nothing, so the check's outcome is consulted
                        // only while it is on; otherwise every offer would be refused for lack of an outcome.
                        val registrationRefused = walletCoreConfig.isRegistrationCheckEnabled &&
                                issuerRegistration.isBlockedForIssuance

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

    /**
     * Stores a deferred document's issuer-trust verdict before reporting it as issued. The write
     * starts atomically ([CoroutineStart.ATOMIC]), so a cancellation right after the event cannot
     * skip it.
     */
    @OptIn(DelicateCoroutinesApi::class)
    override fun issueDeferredDocument(docId: DocumentId): Flow<IssueDeferredDocumentPartialState> =
        callbackFlow {
            (getDocumentById(docId) as? DeferredDocument)?.let { deferredDoc ->

                val manager = getVciManager(
                    issuerId = deferredDoc.issuerMetadata?.credentialIssuerIdentifier.orEmpty(),
                    errorMessage = documentErrorMessage
                ).getOrThrow()

                // The Wallet can lock, leave the foreground or start removal after the schedule write.
                // Check again at the point where the request is submitted to Core.
                val started = walletDocumentOperationController.startCorePolling(docId) {
                    manager.issueDeferredDocument(
                        deferredDocument = deferredDoc,
                        executor = null,
                        onIssueResult = { deferredIssuanceResult ->
                            when (deferredIssuanceResult) {
                                is DeferredIssueResult.DocumentFailed -> {
                                    val cause = deferredIssuanceResult.cause
                                    trySendBlocking(
                                        when {
                                            cause.isDeferredTrustVerdict() ->
                                                IssueDeferredDocumentPartialState.IssuerNotTrusted(
                                                    documentId = deferredIssuanceResult.documentId
                                                )

                                            cause.isUnknownDeferredTransaction() ->
                                                IssueDeferredDocumentPartialState.Expired(
                                                    documentId = deferredIssuanceResult.documentId
                                                )

                                            else -> IssueDeferredDocumentPartialState.Failed
                                        }
                                    )
                                }

                                is DeferredIssueResult.DocumentIssued -> {
                                    launch(start = CoroutineStart.ATOMIC) {
                                        storeIssuerTrust(
                                            documentId = deferredIssuanceResult.documentId,
                                            verdict = deferredIssuanceResult.issuerTrustResult
                                                .toIssuerTrustVerdict(),
                                        )

                                        runBestEffortLocalMetadataUpdate {
                                            deleteLocalMetadataForMissingDocuments()
                                        }

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
                                }

                                is DeferredIssueResult.DocumentNotReady -> {
                                    trySendBlocking(
                                        IssueDeferredDocumentPartialState.NotReady(
                                            DeferredDocumentDataDomain(
                                                documentId = deferredIssuanceResult.documentId,
                                                formatType = deferredIssuanceResult.docType,
                                                docName = deferredIssuanceResult.name
                                            ),
                                            interval = deferredIssuanceResult.interval,
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
                }
                if (!started) trySend(IssueDeferredDocumentPartialState.Paused)
            } ?: trySendBlocking(IssueDeferredDocumentPartialState.Unavailable)

            awaitClose()
        }.safeAsync {
            IssueDeferredDocumentPartialState.Unavailable
        }

    override fun resumeOpenId4VciWithAuthorization(uri: String): Boolean {
        val issuerId = lastAuthorizationIssuerId ?: return false

        return runCatching {
            val manager = getVciManager(
                issuerId = issuerId,
                errorMessage = documentErrorMessage
            ).getOrThrow()

            manager.resumeWithAuthorization(uri)
            true
        }.getOrDefault(false)
    }

    override fun getAllDocumentCategories(): DocumentCategories {
        return walletCoreConfig.documentCategories
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

    /**
     * Deletes what the app stores about [documentId] beside Wallet Core: its revocation record,
     * issuer-trust verdict, failed status refresh, bookmark and failed re-issuance record.
     */
    private suspend fun deleteLocalDocumentMetadata(documentId: DocumentId) {
        revokedDocumentDao.delete(documentId)
        issuerTrustDao.delete(documentId)
        statusRefreshFailureDao.delete(documentId)
        bookmarkDao.delete(documentId)
        failedReIssuedDocumentDao.delete(documentId)
    }

    /**
     * Wallet Core deletes the original document once a re-issuance has issued a new credential;
     * when the new credential is deferred, only once it arrives (see
     * [deleteLocalMetadataForMissingDocuments]). Copies the original's bookmark to the new documents,
     * which keep their ids when a deferred credential arrives, and deletes the rest of what the app
     * stored about the original once Wallet Core has deleted it. The history stays. If the original
     * gained a revocation record while the re-issuance was under way, a revocation check is
     * scheduled, so that the new documents are checked without waiting for the periodic interval.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal suspend fun cleanUpAfterReIssuance(
        originalId: DocumentId,
        state: IssueDocumentsPartialState
    ) {
        val newIds = when (state) {
            is IssueDocumentsPartialState.Success -> state.documentIds
            is IssueDocumentsPartialState.PartialSuccess -> state.documentIds
            is IssueDocumentsPartialState.PartialSuccessWithUntrustedIssuer ->
                state.issuedDocumentIds

            is IssueDocumentsPartialState.DeferredSuccess -> state.deferredDocuments.keys.toList()
            else -> return
        }
        if (newIds.isEmpty() || originalId in newIds) return
        if (isDocumentRevoked(originalId)) walletWorkScheduler.scheduleRevocationCheck()

        runBestEffortLocalMetadataUpdate {
            if (bookmarkDao.retrieve(originalId) != null) {
                // Per-document callbacks may have copied it already; BookmarkDao rejects duplicates.
                newIds.forEach { newId ->
                    if (bookmarkDao.retrieve(newId) == null) bookmarkDao.store(Bookmark(newId))
                }
            }
            if (getDocumentById(originalId) == null) {
                deleteLocalDocumentMetadata(originalId)
            }
        }
    }

    /**
     * Deletes local metadata for documents Wallet Core no longer holds. Wallet Core deletes the
     * original of a deferred re-issuance when the new credential arrives, without saying which
     * document that was. Records are read before the documents, so a record written for a document
     * issued in the meantime is kept. A revocation record among those deleted schedules a
     * revocation check, as in [cleanUpAfterReIssuance].
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal suspend fun deleteLocalMetadataForMissingDocuments() {
        val revokedIds = revokedDocumentDao.retrieveAll().map { it.identifier }.toSet()
        val recordIds = buildSet {
            addAll(revokedIds)
            issuerTrustDao.retrieveAll().mapTo(this) { it.identifier }
            statusRefreshFailureDao.retrieveAll().mapTo(this) { it.identifier }
            bookmarkDao.retrieveAll().mapTo(this) { it.identifier }
            failedReIssuedDocumentDao.retrieveAll().mapTo(this) { it.identifier }
        }
        val documentIds = getAllDocuments().map { it.id }.toSet()
        val missingIds = recordIds - documentIds
        if (missingIds.any { it in revokedIds }) walletWorkScheduler.scheduleRevocationCheck()
        missingIds.forEach { documentId -> deleteLocalDocumentMetadata(documentId) }
    }

    /**
     * Runs [action] on local document metadata after Wallet Core has changed its documents.
     * [NonCancellable] lets the update finish if flow collection stops. A [CancellationException]
     * from [action] propagates. Other exceptions leave the wallet operation's result intact.
     * A factory reset clears any remaining metadata.
     */
    private suspend fun runBestEffortLocalMetadataUpdate(action: suspend () -> Unit) {
        withContext(NonCancellable) {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Refuses the re-issuance of a document the issuer has revoked or suspended. The details screen
     * checks this before the issuer's sign-in starts; checking here as well covers the background
     * re-issuance, which filters its documents once and then re-issues them one by one.
     */
    private suspend fun reIssueBlockedRefusalOrNull(
        documentId: DocumentId
    ): IssueDocumentsPartialState? =
        if (isDocumentRevoked(documentId)) {
            IssueDocumentsPartialState.Failure(
                errorMessage = resourceProvider.getString(
                    R.string.document_details_reissue_blocked_text
                )
            )
        } else {
            null
        }

    override suspend fun isDocumentRevoked(id: String): Boolean =
        revokedDocumentDao.retrieve(id) != null

    override suspend fun isDocumentSuspended(id: String): Boolean =
        revokedDocumentDao.retrieve(id)?.status == RevocationStatus.SUSPENDED

    override suspend fun getIssuerTrustVerdict(documentId: DocumentId): IssuerTrustVerdictDomain? =
        issuerTrustDao.retrieve(documentId)?.verdict?.toIssuerTrustVerdictDomain()

    /**
     * Keeps the issuer-trust verdict for [documentId]. Wallet Core has already stored the document,
     * so the write completes even if the issuance flow is no longer collected. A failed write
     * leaves no record, which reads as not trusted, rather than turning a completed issuance into a
     * reported failure.
     */
    private suspend fun storeIssuerTrust(documentId: DocumentId, verdict: IssuerTrustVerdict) {
        withContext(NonCancellable) {
            try {
                issuerTrustDao.store(IssuerTrust(identifier = documentId, verdict = verdict))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    override suspend fun resolveDocumentStatus(document: IssuedDocument): Result<Status> =
        eudiWallet.resolveStatus(document)

    override suspend fun hasStatusReference(document: IssuedDocument): Boolean =
        DefaultStatusReferenceExtractor.extractStatusReference(document).isSuccess

    override suspend fun hasStatusRefreshFailed(documentId: DocumentId): Boolean =
        statusRefreshFailureDao.retrieve(documentId) != null

    override suspend fun hasReIssuanceFailed(documentId: DocumentId): Boolean =
        failedReIssuedDocumentDao.retrieve(documentId) != null

    private fun issueDocumentsWithOpenId4VCI(
        configIds: List<String>,
        issuerId: String,
        prioritizeDeferred: Boolean
    ): Flow<IssueDocumentsPartialState> =
        callbackFlow {

            val manager = getVciManager(
                issuerId = issuerId,
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
                lastAuthorizationIssuerId = issuerId

                manager.issueDocumentByConfigurationIdentifiers(
                    issuerUrl = issuerId,
                    credentialConfigurationIds = configIds,
                    onIssueEvent = issuanceCallback(
                        prioritizeDeferred = prioritizeDeferred,
                        originalDocumentId = null
                    )
                )
            }

            awaitClose()

        }.safeAsync {
            IssueDocumentsPartialState.Failure(
                errorMessage = documentErrorMessage
            )
        }

    /**
     * Turns Wallet Core's issuance events into [IssueDocumentsPartialState]s and persists each
     * document's trust verdict or Deferred schedule as it arrives. A mixed or failed batch may
     * never produce [IssueDocumentsPartialState.DeferredSuccess], so registration cannot depend
     * on that aggregate outcome.
     *
     * Terminal callbacks await these writes before publishing a result: screens read the metadata,
     * and reset must not finish draining while an issuance operation still has writes pending.
     * [CoroutineStart.ATOMIC] protects coroutine entry only; awaiting the writes tracks completion.
     *
     * @param originalDocumentId Known Reissuance origin, or null for initial Issuance. Persisted
     * with replacements for duplicate-request checks and conditional automatic removal.
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun ProducerScope<IssueDocumentsPartialState>.issuanceCallback(
        prioritizeDeferred: Boolean = true,
        originalDocumentId: DocumentId?,
    ): OpenId4VciManager.OnIssueEvent {

        var totalDocumentsToBeIssued = 0
        val issuanceStart = coroutineContext[IssuanceStart]
        val untrustedDocuments: MutableMap<FormatType, String> = mutableMapOf()
        val nonIssuedDocuments: MutableMap<FormatType, String> = mutableMapOf()
        val deferredDocuments: MutableMap<DocumentId, FormatType> = mutableMapOf()
        val issuedDocuments: MutableMap<DocumentId, FormatType> = mutableMapOf()
        val issuerTrustWrites: MutableList<Job> = mutableListOf()
        val scheduleWrites = mutableListOf<Deferred<Result<Unit>>>()

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
                    launch {
                        // A batch can fail after creating Deferred documents; finish their writes too.
                        issuerTrustWrites.joinAll()
                        scheduleWrites.awaitAll()

                        val untrustedReason = event.cause.toUntrustedIssuerReasonOrNull()
                        trySendBlocking(
                            if (untrustedReason != null) {
                                IssueDocumentsPartialState.IssuerNotTrusted(reason = untrustedReason)
                            } else {
                                IssueDocumentsPartialState.Failure(errorMessage = documentErrorMessage)
                            }
                        )
                    }
                }

                is IssueEvent.Finished -> {
                    launch {
                        issuerTrustWrites.joinAll()
                        // Finish schedule writes without replacing Wallet Core's issuance result.
                        scheduleWrites.awaitAll()

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
                            return@launch
                        }

                        if (untrustedDocuments.isNotEmpty()) {
                            trySendBlocking(
                                IssueDocumentsPartialState.PartialSuccessWithUntrustedIssuer(
                                    issuedDocumentIds = issuedDocuments.keys.toList(),
                                    untrustedDocuments = untrustedDocuments,
                                    nonIssuedDocuments = nonIssuedDocuments,
                                )
                            )
                            return@launch
                        }

                        if (deferredDocuments.isNotEmpty() &&
                            (prioritizeDeferred || (issuedDocuments.isEmpty()))
                        ) {
                            trySendBlocking(
                                IssueDocumentsPartialState.DeferredSuccess(
                                    deferredDocuments = deferredDocuments
                                )
                            )
                            return@launch
                        }

                        if (event.issuedDocuments.isEmpty()) {
                            trySendBlocking(
                                IssueDocumentsPartialState.Failure(
                                    errorMessage = documentErrorMessage
                                )
                            )
                            return@launch
                        }

                        if (event.issuedDocuments.size == totalDocumentsToBeIssued) {
                            trySendBlocking(
                                IssueDocumentsPartialState.Success(
                                    documentIds = event.issuedDocuments
                                )
                            )
                            return@launch
                        }

                        trySendBlocking(
                            IssueDocumentsPartialState.PartialSuccess(
                                documentIds = event.issuedDocuments,
                                nonIssuedDocuments = nonIssuedDocuments
                            )
                        )
                    }
                }

                is IssueEvent.DocumentIssued -> {
                    issuedDocuments[event.documentId] = event.docType
                    issuerTrustWrites += launch(start = CoroutineStart.ATOMIC) {
                        storeIssuerTrust(
                            documentId = event.documentId,
                            verdict = event.issuerTrustResult.toIssuerTrustVerdict(),
                        )
                        copyReplacementBookmark(originalDocumentId, event.documentId)
                    }
                }

                is IssueEvent.Started -> {
                    totalDocumentsToBeIssued = event.total
                    issuanceStart?.markStarted()
                }

                is IssueEvent.DocumentDeferred -> {
                    deferredDocuments[event.documentId] = event.docType
                    // This initial Core event exposes no interval. Start the configured fallback
                    // delay at callback receipt, before waiting for the document lock or Room write.
                    val firstAttemptAt = System.currentTimeMillis() +
                            walletCoreConfig.deferredIssuanceConfig.initialPollingDelay.inWholeMilliseconds
                    scheduleWrites += async(start = CoroutineStart.ATOMIC) {
                        // Keep persistence failure as a result: an uncaught async failure would cancel
                        // observation while Core may still be running.
                        runCatching {
                            walletDocumentOperationController.withDocumentLock(event.documentId) {
                                // Remove may have completed before this callback acquired the lock.
                                if (event.documentId in walletDocumentOperationController.state.value.removedDocumentIds) {
                                    return@withDocumentLock
                                }
                                // A duplicate callback can arrive after an earlier poll issued this ID.
                                if (getDocumentById(event.documentId) is IssuedDocument) {
                                    copyReplacementBookmark(originalDocumentId, event.documentId)
                                    return@withDocumentLock
                                }
                                // Insert-ignore preserves an existing deadline or terminal status.
                                deferredScheduleDao.store(
                                    DeferredDocumentSchedule(
                                        identifier = event.documentId,
                                        nextAttemptAt = firstAttemptAt,
                                        lastIntervalMillis = null,
                                        originalDocumentId = originalDocumentId,
                                    )
                                )
                                if (originalDocumentId != null) {
                                    // Recovery may not know the origin. Fill only that missing link,
                                    // leaving any existing schedule and known origin unchanged.
                                    deferredScheduleDao.attachOriginalIfUnknown(
                                        event.documentId,
                                        originalDocumentId
                                    )
                                }
                                copyReplacementBookmark(originalDocumentId, event.documentId)
                            }
                        }
                    }
                }
            }
        }

        return listener
    }

    /**
     * Copies a Reissuance bookmark at each Issued/Deferred callback, before terminal cleanup can
     * remove the original's metadata. Aggregate outcomes may omit Deferred replacement IDs.
     * Bookmark copying keeps the existing best-effort cleanup behavior.
     */
    private suspend fun copyReplacementBookmark(originalId: String?, replacementId: String) {
        if (originalId == null) return
        runBestEffortLocalMetadataUpdate {
            if (bookmarkDao.retrieve(originalId) != null && bookmarkDao.retrieve(replacementId) == null) {
                bookmarkDao.store(Bookmark(replacementId))
            }
        }
    }

    /**
     * Collects initial Issuance and Reissuance in the shared operation scope through their first
     * terminal state and callback writes. Cancelling a screen collector stops result delivery but
     * leaves the registered operation running and tracked, so reset can await its completion.
     * If Core needs a key unlocked, an observer that leaves can no longer answer the prompt.
     * Cancel its unanswered authentication so Core can send the terminal result.
     * Reset and bulk removal wait for every submission, including multiple replacements for one original.
     * Until Wallet Core reports [IssueEvent.Started] (after any browser sign-in), nothing is written,
     * so a screen that leaves before then cancels the operation: an abandoned sign-in, which Wallet Core
     * waits for with no time limit, does not hold back reset or bulk removal.
     */
    private fun Flow<IssueDocumentsPartialState>.trackedIssuance(): Flow<IssueDocumentsPartialState> =
        flow {
            val observerLifetime = Job()
            val issuanceStart = IssuanceStart()
            val issuance = this@trackedIssuance
                .flowOn(issuanceStart)
                .withAuthenticationObserver(observerLifetime)
            val trackedOperation = walletDocumentOperationController.trackedOperationFlow(
                completeWhen = { state ->
                    state !is IssueDocumentsPartialState.UserAuthRequired
                },
                isResetOperation = false,
                isAbandonable = { !issuanceStart.hasStarted },
                flowToCollect = { issuance },
            )
            try {
                emitAll(trackedOperation)
            } finally {
                observerLifetime.cancel()
            }
        }

    /**
     * Registration check for a flow with no approval screen, run before it starts. Returns the
     * terminal state refusing or failing the flow, or null to proceed — always null while the
     * check is off, which also skips the resolve itself.
     */
    private suspend fun preflightRegistrationRefusalOrNull(
        resolveRegistration: suspend () -> Result<RegistrationCertificateResult>,
    ): IssueDocumentsPartialState? {
        if (!walletCoreConfig.isRegistrationCheckEnabled) {
            return null
        }

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

    /**
     * The manager configured for the issuer [issuerId], or a failure with [errorMessage]. There is
     * no fallback: another issuer's manager would send that issuer's client settings to this one.
     */
    private fun getVciManager(
        issuerId: String,
        errorMessage: String
    ): Result<OpenId4VciManager> {

        val manager = openId4VciManagers.entries
            .firstOrNull { (vciConfig, _) -> vciConfig.issuerUrl == issuerId }
            ?.value

        return manager?.let(Result.Companion::success)
            ?: Result.failure(RuntimeException(errorMessage))
    }
}

/**
 * Marks, for one tracked issuance, whether Wallet Core has reported [IssueEvent.Started]. It reaches
 * the issuance callback through the coroutine context of the collected flow.
 */
private class IssuanceStart : AbstractCoroutineContextElement(IssuanceStart) {
    companion object Key : CoroutineContext.Key<IssuanceStart>

    private val started = AtomicBoolean(false)

    val hasStarted: Boolean get() = started.get()

    fun markStarted() = started.set(true)
}
