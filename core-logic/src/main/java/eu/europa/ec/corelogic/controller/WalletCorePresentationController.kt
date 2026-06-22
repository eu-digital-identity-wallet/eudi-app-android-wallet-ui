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

import android.content.Intent
import androidx.activity.ComponentActivity
import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.businesslogic.extension.toUri
import eu.europa.ec.corelogic.di.WalletCoreScope
import eu.europa.ec.corelogic.di.getOrCreateKoinScope
import eu.europa.ec.corelogic.extension.toClaimPath
import eu.europa.ec.corelogic.model.AuthenticationData
import eu.europa.ec.corelogic.model.PresentationCombinationDomain
import eu.europa.ec.corelogic.model.PresentationMatchDomain
import eu.europa.ec.corelogic.model.PresentationSelectionDomain
import eu.europa.ec.corelogic.model.identityKey
import eu.europa.ec.corelogic.util.EudiWalletListenerWrapper
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.toKotlinResult
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultKeyUnlockData
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.multipaz.presentment.CredentialPresentmentSelection
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch
import org.multipaz.securearea.KeyUnlockData
import java.net.URI

sealed class PresentationControllerConfig(val initiatorRoute: String) {
    data class OpenId4VP(val uri: String, val initiator: String) :
        PresentationControllerConfig(initiator)

    data class Ble(val initiator: String) : PresentationControllerConfig(initiator)

    data class DcApi(val initiator: String, val startIntent: Intent) :
        PresentationControllerConfig(initiator)
}

sealed class TransferEventPartialState {
    data object Connected : TransferEventPartialState()
    data object Connecting : TransferEventPartialState()
    data object Disconnected : TransferEventPartialState()
    data class Error(val error: String) : TransferEventPartialState()
    data class QrEngagementReady(val qrCode: String) : TransferEventPartialState()
    data class RequestReceived(
        val combinationsDomain: List<PresentationCombinationDomain>,
        val verifierName: String?,
        val verifierIsTrusted: Boolean,
    ) : TransferEventPartialState()

    data object ResponseSent : TransferEventPartialState()
    data class Redirect(val uri: URI) : TransferEventPartialState()
    data class IntentToSend(val intent: Intent) : TransferEventPartialState()
}

sealed class CheckKeyUnlockPartialState {
    data class Failure(val error: String) : CheckKeyUnlockPartialState()
    data class UserAuthenticationRequired(
        val authenticationData: List<AuthenticationData>,
    ) : CheckKeyUnlockPartialState()

    data object RequestIsReadyToBeSent : CheckKeyUnlockPartialState()
}

sealed class SendRequestedDocumentsPartialState {
    data class Failure(val error: String) : SendRequestedDocumentsPartialState()
    data object RequestSent : SendRequestedDocumentsPartialState()
}

sealed class ResponseReceivedPartialState {
    data object Success : ResponseReceivedPartialState()
    data class Redirect(val uri: URI) : ResponseReceivedPartialState()
    data class Failure(val error: String) : ResponseReceivedPartialState()
    data class IntentToSend(val intent: Intent) : ResponseReceivedPartialState()
}

sealed class WalletCorePartialState {
    data class UserAuthenticationRequired(
        val authenticationData: List<AuthenticationData>,
    ) : WalletCorePartialState()

    data class Failure(val error: String) : WalletCorePartialState()
    data object Success : WalletCorePartialState()
    data class Redirect(val uri: URI) : WalletCorePartialState()
    data object RequestIsReadyToBeSent : WalletCorePartialState()
    data class IntentToSend(val intent: Intent) : WalletCorePartialState()
}

/**
 * The app's single entry point to a live presentation session (one instance per presentation,
 * inside the per-presentation Koin scope). Requests are exposed as pure-domain
 * [PresentationCombinationDomain]s and the raw Wallet Core SDK matches never leave the controller,
 * so no Wallet Core types reach the UI.
 * */
interface WalletCorePresentationController {
    /**
     * Hot flow of the Wallet Core SDK's status callbacks. The SDK listener attaches on first collection
     * (shared `Lazily`, which is also what starts the configured transport) and is removed when
     * the per-presentation scope is cancelled, ending the presentation.
     * */
    val events: SharedFlow<TransferEventPartialState>

    /**
     * The user's pending disclosure decision — one [PresentationSelectionDomain] per match kept.
     * (The per-credential [KeyUnlockData] captured during the biometric step lives in the
     * controller, not on the selection.)
     * */
    val disclosedDocuments: List<PresentationSelectionDomain>?

    /**
     * Verifier name so it can be retrieve across screens
     * */
    val verifierName: String?

    val verifierIsTrusted: Boolean?

    /**
     * Who started the presentation
     * */
    val initiatorRoute: String

    val redirectUri: URI?

    val pendingIntent: Intent?

    /**
     * Set [PresentationControllerConfig]
     * */
    fun setConfig(config: PresentationControllerConfig)

    /**
     * Terminates the presentation and kills the coroutine scope that [events] live in.
     * */
    fun stopPresentation()

    /**
     * Starts QR engagement. This will trigger [events] emission.
     *
     * [TransferEventPartialState.QrEngagementReady] -> QR String to show QR
     *
     * [TransferEventPartialState.Connecting] -> Connecting
     *
     * [TransferEventPartialState.Connected] -> Connected. We can proceed to the next screen
     * */
    fun startQrEngagement()

    /**
     * Enable/Disable NFC service
     * */
    fun toggleNfcEngagement(componentActivity: ComponentActivity, toggle: Boolean)

    /**
     * Emits the key-unlock step for the send flow:
     * [CheckKeyUnlockPartialState.UserAuthenticationRequired] (one biometric prompt per
     * credential) when the config requires authentication, otherwise
     * [CheckKeyUnlockPartialState.RequestIsReadyToBeSent].
     * */
    fun checkForKeyUnlock(): Flow<CheckKeyUnlockPartialState>

    /**
     * Build the Wallet Core [CredentialPresentmentSelection] from [disclosedDocuments] and
     * dispatch it to the Wallet Core SDK.
     */
    suspend fun sendRequestedDocuments(): SendRequestedDocumentsPartialState

    fun updateRequestedDocuments(disclosedDocuments: List<PresentationSelectionDomain>?)

    fun mappedCallbackStateFlow(): Flow<ResponseReceivedPartialState>

    /**
     * Merges [checkForKeyUnlock] and [mappedCallbackStateFlow] into the single
     * [WalletCorePartialState] stream the loading screen observes.
     * */
    fun observeSentDocumentsRequest(): Flow<WalletCorePartialState>
}

class WalletCorePresentationControllerImpl(
    private val resourceProvider: ResourceProvider,
    private val prefKeys: PrefKeys,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    walletCore: EudiWallet? = null,
) : WalletCorePresentationController {

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

    private val genericErrorMessage = resourceProvider.genericErrorMessage()

    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    private lateinit var _config: PresentationControllerConfig

    override var disclosedDocuments: List<PresentationSelectionDomain>? = null

    // The Wallet Core SDK request, held from the onRequestReceived callback until send.
    private var processedRequest: RequestProcessor.ProcessedRequest.Success? = null

    // Raw Wallet Core SDK matches kept only here (keyed by documentId/credentialId/queryId) so the
    // UI-facing types stay free of Wallet Core types; re-paired to the domain selections at send.
    private var matchByKey: Map<Triple<String, String, String?>, CredentialPresentmentSetOptionMemberMatch> =
        emptyMap()

    // Per-credential key-unlock data captured in [checkForKeyUnlock], consumed by
    // [sendRequestedDocuments]; kept here rather than on the UI-facing selection.
    private val keyUnlockDataByCredentialId: MutableMap<String, KeyUnlockData> = mutableMapOf()

    override var verifierName: String? = null

    override var verifierIsTrusted: Boolean? = null

    override val initiatorRoute: String
        get() = requireConfig().initiatorRoute

    override var redirectUri: URI? = null

    override var pendingIntent: Intent? = null

    override fun setConfig(config: PresentationControllerConfig) {
        _config = config
    }

    override val events: SharedFlow<TransferEventPartialState> = callbackFlow {
        val eventListenerWrapper = EudiWalletListenerWrapper(
            onQrEngagementReady = { qrCode ->
                trySendBlocking(
                    TransferEventPartialState.QrEngagementReady(qrCode = qrCode)
                )
            },
            onConnected = {
                trySendBlocking(
                    TransferEventPartialState.Connected
                )
            },
            onConnecting = {
                trySendBlocking(
                    TransferEventPartialState.Connecting
                )
            },
            onDisconnected = {
                trySendBlocking(
                    TransferEventPartialState.Disconnected
                )
            },
            onError = { errorMessage ->
                trySendBlocking(
                    TransferEventPartialState.Error(
                        error = errorMessage.ifEmpty { genericErrorMessage }
                    )
                )
            },
            onRequestReceived = { result ->
                trySendBlocking(handleRequestReceived(result))
            },
            onResponseSent = {
                trySendBlocking(
                    TransferEventPartialState.ResponseSent
                )
            },
            onRedirect = { uri ->
                redirectUri = uri

                trySendBlocking(
                    TransferEventPartialState.Redirect(
                        uri = uri
                    )
                )
            },

            intentToSend = { intent ->
                pendingIntent = intent
                trySendBlocking(
                    TransferEventPartialState.IntentToSend(intent = intent)
                )
            }
        )

        addListener(eventListenerWrapper)
        awaitClose {
            removeListener(eventListenerWrapper)
            eudiWallet.stopProximityPresentation()
        }
    }.safeAsync {
        TransferEventPartialState.Error(
            error = it.localizedMessage ?: resourceProvider.genericErrorMessage()
        )
    }.shareIn(coroutineScope, SharingStarted.Lazily, 2)

    override fun startQrEngagement() {
        eudiWallet.startProximityPresentation()
    }

    override fun toggleNfcEngagement(componentActivity: ComponentActivity, toggle: Boolean) {
        try {
            if (toggle) {
                eudiWallet.enableNFCEngagement(componentActivity)
            } else {
                eudiWallet.disableNFCEngagement(componentActivity)
            }
        } catch (_: Exception) {
        }
    }

    override fun checkForKeyUnlock(): Flow<CheckKeyUnlockPartialState> {
        return flow {
            disclosedDocuments?.let { selections ->

                // fresh per send attempt
                keyUnlockDataByCredentialId.clear()

                val authenticationData = mutableListOf<AuthenticationData>()

                if (eudiWallet.config.userAuthenticationRequired) {

                    // one prompt per credential, not per selection: a multi-query request can
                    // disclose the same credential under several queryIds and its key unlocks
                    // once, so distinctBy avoids N identical biometric prompts
                    val distinctCredentialSelections = selections.distinctBy { it.credentialId }

                    for (selection in distinctCredentialSelections) {
                        val kud =
                            eudiWallet.getDefaultKeyUnlockData(documentId = selection.documentId)
                        val cryptoObject = kud?.getCryptoObjectForSigning()

                        authenticationData.add(
                            AuthenticationData(
                                crypto = BiometricCrypto(cryptoObject),
                                onAuthenticationSuccess = {
                                    if (kud != null) {
                                        keyUnlockDataByCredentialId[selection.credentialId] = kud
                                    }
                                }
                            )
                        )
                    }

                    emit(
                        CheckKeyUnlockPartialState.UserAuthenticationRequired(
                            authenticationData
                        )
                    )

                } else {
                    emit(
                        CheckKeyUnlockPartialState.RequestIsReadyToBeSent
                    )
                }
            }
        }.safeAsync {
            CheckKeyUnlockPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMessage
            )
        }
    }

    override suspend fun sendRequestedDocuments(): SendRequestedDocumentsPartialState {
        val selectionsDomain = disclosedDocuments
            ?: return SendRequestedDocumentsPartialState.Failure(error = genericErrorMessage)
        val processed = processedRequest
            ?: return SendRequestedDocumentsPartialState.Failure(error = genericErrorMessage)

        return runCatching {
            // re-pair each selection domain to its Wallet Core match by (documentId, credentialId, queryId),
            // then keep only the user-confirmed claims; copy() keeps source/transactionData so the
            // processor can re-associate the originating query.
            val walletCoreMatches = selectionsDomain.map { selectionDomain ->
                // every selection came from a stored match, so this must resolve; if it ever
                // doesn't, fail loudly rather than silently under-disclose
                val match = matchByKey[Triple(
                    selectionDomain.documentId,
                    selectionDomain.credentialId,
                    selectionDomain.queryId
                )] ?: error(
                    "No stored match for selection (documentId=${selectionDomain.documentId}, " +
                            "credentialId=${selectionDomain.credentialId}, queryId=${selectionDomain.queryId})"
                )
                match.copy(
                    claims = match
                        .claims
                        .filterKeys { requestedClaim ->
                            requestedClaim.toClaimPath() in selectionDomain.selectedClaims
                        }
                )
            }

            val walletCoreSelection = CredentialPresentmentSelection(matches = walletCoreMatches)

            val keyUnlockData = selectionsDomain.mapNotNull { selectionDomain ->
                keyUnlockDataByCredentialId[selectionDomain.credentialId]?.let { keyUnlockData ->
                    selectionDomain.credentialId to keyUnlockData
                }
            }.toMap()

            processed.generateResponse(
                selection = walletCoreSelection,
                keyUnlockData = keyUnlockData,
            ).toKotlinResult()
                .fold(
                    onSuccess = {
                        eudiWallet.sendResponse(it.response)
                        SendRequestedDocumentsPartialState.RequestSent
                    },
                    onFailure = {
                        SendRequestedDocumentsPartialState.Failure(
                            error = it.localizedMessage ?: genericErrorMessage
                        )
                    }
                )
        }.getOrElse {
            SendRequestedDocumentsPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMessage
            )
        }
    }

    override fun mappedCallbackStateFlow(): Flow<ResponseReceivedPartialState> {
        return events.mapNotNull { response ->
            when (response) {

                // Fix: Wallet core should return Success state here
                is TransferEventPartialState.Error -> {
                    if (response.error == "Peer disconnected without proper session termination") {
                        ResponseReceivedPartialState.Success
                    } else {
                        ResponseReceivedPartialState.Failure(error = response.error)
                    }
                }

                is TransferEventPartialState.Redirect -> {
                    ResponseReceivedPartialState.Redirect(uri = response.uri)
                }

                is TransferEventPartialState.Disconnected -> {
                    when {
                        events.replayCache.firstOrNull() is TransferEventPartialState.Redirect -> null
                        else -> ResponseReceivedPartialState.Success
                    }
                }

                is TransferEventPartialState.ResponseSent -> ResponseReceivedPartialState.Success

                is TransferEventPartialState.IntentToSend -> {
                    ResponseReceivedPartialState.IntentToSend(intent = response.intent)
                }

                else -> null
            }
        }.safeAsync {
            ResponseReceivedPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMessage
            )
        }
    }

    override fun observeSentDocumentsRequest(): Flow<WalletCorePartialState> =
        merge(checkForKeyUnlock(), mappedCallbackStateFlow()).map {
            when (it) {
                is CheckKeyUnlockPartialState.Failure -> {
                    WalletCorePartialState.Failure(it.error)
                }

                is CheckKeyUnlockPartialState.UserAuthenticationRequired -> {
                    WalletCorePartialState.UserAuthenticationRequired(it.authenticationData)
                }

                is ResponseReceivedPartialState.Failure -> {
                    WalletCorePartialState.Failure(it.error)
                }

                is ResponseReceivedPartialState.Redirect -> {
                    WalletCorePartialState.Redirect(
                        uri = it.uri
                    )
                }

                is CheckKeyUnlockPartialState.RequestIsReadyToBeSent -> {
                    WalletCorePartialState.RequestIsReadyToBeSent
                }

                is ResponseReceivedPartialState.IntentToSend -> {
                    WalletCorePartialState.IntentToSend(intent = it.intent)
                }

                else -> {
                    WalletCorePartialState.Success
                }
            }
        }.safeAsync {
            WalletCorePartialState.Failure(
                error = it.localizedMessage ?: genericErrorMessage
            )
        }

    override fun updateRequestedDocuments(disclosedDocuments: List<PresentationSelectionDomain>?) {
        this.disclosedDocuments = disclosedDocuments
    }

    override fun stopPresentation() {
        coroutineScope.cancel()
        CoroutineScope(dispatcher).launch {
            eudiWallet.stopProximityPresentation()
        }
    }

    /**
     * Ingests a processed request from the Wallet Core SDK: stores the per-session state the rest of the
     * flow needs (the raw [processedRequest], verifier identity/trust, the private [matchByKey])
     * and projects it into a [TransferEventPartialState.RequestReceived].
     * A throw here (ours or the SDK's) becomes an Error state, not a crash.
     */
    private fun handleRequestReceived(
        result: RequestProcessor.ProcessedRequest,
    ): TransferEventPartialState {
        return runCatching {
            val success = result.getOrThrow()
            processedRequest = success

            verifierName = success.trustMetadata?.displayName
            val isTrusted = success.trustMetadata != null
            verifierIsTrusted = isTrusted

            val combinationsDomain = success.buildCombinationsDomain()

            matchByKey = success.buildMatchByKey()

            TransferEventPartialState.RequestReceived(
                combinationsDomain = combinationsDomain,
                verifierName = verifierName,
                verifierIsTrusted = isTrusted
            )
        }.getOrElse { throwable ->
            TransferEventPartialState.Error(
                error = throwable.localizedMessage ?: genericErrorMessage
            )
        }
    }

    private fun addListener(listener: TransferEvent.Listener) {
        val safeConfig = requireConfig()

        eudiWallet.addTransferEventListener(listener)

        when (safeConfig) {
            is PresentationControllerConfig.OpenId4VP -> {
                eudiWallet.startRemotePresentation(safeConfig.uri.toUri())
            }

            is PresentationControllerConfig.Ble -> {
                // No-op
            }

            is PresentationControllerConfig.DcApi -> {
                eudiWallet.startDCAPIPresentation(safeConfig.startIntent)
            }
        }
    }

    private fun removeListener(listener: TransferEvent.Listener) {
        requireConfig()
        eudiWallet.removeTransferEventListener(listener)
    }

    private fun requireConfig(): PresentationControllerConfig {
        if (!::_config.isInitialized) {
            throw IllegalStateException(
                "setConfig() must be called before using the WalletCorePresentationController"
            )
        }
        return _config
    }
}

private fun RequestProcessor.ProcessedRequest.Success.buildCombinationsDomain(): List<PresentationCombinationDomain> {
    return presentmentSelections.map { selection ->
        PresentationCombinationDomain(
            matches = selection.matches.map { match ->
                PresentationMatchDomain.from(match)
            },
        )
    }
}

/**
 * The send-time lookup `(documentId, credentialId, queryId)` → raw Wallet Core match that
 * [sendRequestedDocuments][WalletCorePresentationController.sendRequestedDocuments] re-pairs each selection against.
 *
 * An identity can recur across combinations (one credential satisfying the same query in each), so
 * they're collapsed to one match. The copies are equivalent, so the first wins; a `check` fails
 * loudly if they ever differ, rather than re-pairing consent to the wrong claims.
 */
private fun RequestProcessor.ProcessedRequest.Success.buildMatchByKey():
        Map<Triple<String, String, String?>, CredentialPresentmentSetOptionMemberMatch> {
    return presentmentSelections
        .flatMap { it.matches }
        .groupBy { it.identityKey }
        .mapValues { (identityKey, matches) ->
            matches.first().also { first ->
                check(matches.all { it.claims.keys == first.claims.keys }) {
                    "Conflicting matches for the same identity $identityKey: " +
                            "different claim sets for one (document, credential, query)"
                }
            }
        }
}