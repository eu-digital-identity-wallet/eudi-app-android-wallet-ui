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

package eu.europa.ec.corelogic.worker

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import eu.europa.ec.corelogic.controller.IssueDocumentsPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.util.CoreActions
import eu.europa.ec.corelogic.util.CoreActions.RE_ISSUANCE_IDS_DETAILS_EXTRA
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings.CredentialPolicy
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.storagelogic.dao.FailedReIssuedDocumentDao
import eu.europa.ec.storagelogic.model.FailedReIssuedDocument
import kotlinx.coroutines.flow.first
import org.koin.android.annotation.KoinWorker
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

@KoinWorker
class ReIssuanceWorkManager(
    appContext: Context,
    workerParams: WorkerParameters,
    private val failedReIssuedDocumentDao: FailedReIssuedDocumentDao,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val RE_ISSUANCE_WORK_NAME = "reIssuanceWorker"
    }

    override suspend fun doWork(): Result {

        try {

            val failed = mutableListOf<String>()
            val succeed = mutableListOf<String>()
            val idsRemoved = mutableListOf<String>()

            val now = Clock.System.now()

            walletCoreDocumentsController
                .getAllIssuedDocuments()
                .filter { document ->
                    when (val policy = document.credentialPolicy) {
                        is CredentialPolicy.LimitedTime -> document.isDueForLifetimeReIssue(
                            now = now,
                            reissueTriggerLifetimeLeft = policy.reissueTriggerLifetimeLeft,
                        )

                        is CredentialPolicy.OnceOnly -> document.isDueForUnusedCountReIssue(
                            now = now,
                            reissueTriggerUnused = policy.reissueTriggerUnused,
                        )

                        is CredentialPolicy.RotatingBatch -> document.isDueForLifetimeReIssue(
                            now = now,
                            reissueTriggerLifetimeLeft = policy.reissueTriggerLifetimeLeft,
                        )
                    }
                }
                .forEach { document ->

                    val state = walletCoreDocumentsController.reIssueDocument(
                        documentId = document.id,
                        issuerId = document.issuerMetadata?.credentialIssuerIdentifier.orEmpty(),
                        allowAuthorizationFallback = false
                    ).first()

                    when (state) {
                        is IssueDocumentsPartialState.DeferredSuccess -> {
                            succeed.addAll(state.deferredDocuments.keys)
                            idsRemoved.add(document.id)
                        }

                        is IssueDocumentsPartialState.PartialSuccess -> {
                            succeed.addAll(state.documentIds)
                            idsRemoved.add(document.id)
                        }

                        is IssueDocumentsPartialState.Success -> {
                            succeed.addAll(state.documentIds)
                            idsRemoved.add(document.id)
                        }

                        is IssueDocumentsPartialState.Failure -> {
                            failed.add(document.id)
                        }

                        is IssueDocumentsPartialState.UserAuthRequired -> {
                            state.resultHandler.onAuthenticationFailure()
                            failed.add(document.id)
                        }
                    }
                }

            removeAllFailedFromStorage()

            if (failed.isNotEmpty()) {
                storeFailedToStorage(failed)
            }

            if (succeed.isNotEmpty()) {
                notifyDocumentsList()
                notifyDocumentDetails(idsRemoved)
            }

            return Result.success()
        } catch (_: Exception) {
            return Result.failure()
        }
    }

    @Throws(IllegalArgumentException::class)
    private suspend fun storeFailedToStorage(ids: List<String>) {
        failedReIssuedDocumentDao.storeAll(
            ids.map { FailedReIssuedDocument(it) }
        )
    }

    @Throws(IllegalArgumentException::class)
    private suspend fun removeAllFailedFromStorage() {
        failedReIssuedDocumentDao.deleteAll()
    }

    private fun notifyDocumentDetails(removedIds: List<String>) {
        val detailsIntent = Intent(CoreActions.RE_ISSUANCE_WORK_REFRESH_DETAILS_ACTION).apply {
            setPackage(applicationContext.packageName)
            putStringArrayListExtra(
                RE_ISSUANCE_IDS_DETAILS_EXTRA,
                ArrayList(removedIds)
            )
        }
        applicationContext.sendBroadcast(detailsIntent)
    }

    private fun notifyDocumentsList() {
        val refreshIntent = Intent(CoreActions.RE_ISSUANCE_WORK_REFRESH_ACTION).apply {
            setPackage(applicationContext.packageName)
        }
        applicationContext.sendBroadcast(refreshIntent)
    }
}

/**
 * Re-issue when no currently-valid credential remains, or when the remaining lifetime is at or
 * below the policy's threshold. Used for [CredentialPolicy.LimitedTime] and
 * [CredentialPolicy.RotatingBatch], for which re-issuance is driven by remaining lifetime.
 *
 * The threshold travels with the document's stored policy — set by the issuer for a mandatory reuse
 * policy, or by the wallet's own [eu.europa.ec.corelogic.config.DocumentIssuanceConfig] for an
 * optional one. A null threshold is not expected; defensively it re-issues only once the document
 * has actually expired.
 *
 * A failed [IssuedDocument.getValidUntil] means nothing is valid right now (expired/exhausted),
 * which is itself a reason to re-issue.
 */
private suspend fun IssuedDocument.isDueForLifetimeReIssue(
    now: Instant,
    reissueTriggerLifetimeLeft: Duration?,
): Boolean {
    val validUntil = getValidUntil().getOrNull()?.toKotlinInstant() ?: return true
    val expirationLimit = reissueTriggerLifetimeLeft?.let { now.plus(it) } ?: now
    return validUntil <= expirationLimit
}

/**
 * Re-issue when the number of currently-valid unused credentials is at or below the policy's
 * threshold. For once-only, re-issuance is driven by the count of unused credentials, never
 * remaining lifetime.
 *
 * [IssuedDocument.credentialsCount] is intentionally avoided: it also counts expired credentials,
 * so an expired-but-unused batch would stay above the threshold forever; counting only valid
 * credentials lets it fall to the threshold and re-issue. A null threshold is not expected;
 * defensively it re-issues only once no usable credential remains.
 */
private suspend fun IssuedDocument.isDueForUnusedCountReIssue(
    now: Instant,
    reissueTriggerUnused: Int?,
): Boolean {
    val validUnusedCount = getCredentials().count { now >= it.validFrom && now <= it.validUntil }
    return validUnusedCount <= (reissueTriggerUnused ?: 0)
}