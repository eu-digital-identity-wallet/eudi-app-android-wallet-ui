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

package eu.europa.ec.corelogic.worker

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import eu.europa.ec.corelogic.config.WalletCoreConfig
import eu.europa.ec.corelogic.controller.IssueDocumentsPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.util.CoreActions
import eu.europa.ec.corelogic.util.CoreActions.RE_ISSUANCE_IDS_DETAILS_EXTRA
import eu.europa.ec.storagelogic.dao.FailedReIssuedDocumentDao
import eu.europa.ec.storagelogic.model.FailedReIssuedDocument
import kotlinx.coroutines.flow.first
import org.koin.android.annotation.KoinWorker

@KoinWorker
class ReIssuanceWorkManager(
    appContext: Context,
    workerParams: WorkerParameters,
    private val failedReIssuedDocumentDao: FailedReIssuedDocumentDao,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val walletCoreConfig: WalletCoreConfig
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val RE_ISSUANCE_WORK_NAME = "reIssuanceWorker"
    }

    override suspend fun doWork(): Result {

        try {

            val failed = mutableListOf<String>()
            val succeed = mutableListOf<String>()
            val idsRemoved = mutableListOf<String>()

            val config = walletCoreConfig.documentIssuanceConfig.reissuanceRule
            val now = java.time.Instant.now()
            val expirationLimit = now.plus(
                java.time.Duration.ofHours(config.minExpirationHours.toLong())
            )

            walletCoreDocumentsController
                .getAllIssuedDocuments()
                .filter { credential ->

                    val belowMinCount =
                        credential.credentialsCount() <= config.minNumberOfCredentials

                    val expiresWithinThreshold =
                        credential.getValidUntil()
                            .map { validUntil -> validUntil.isBefore(expirationLimit) }
                            .getOrDefault(false)

                    belowMinCount || expiresWithinThreshold
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
                            state.resultHandler.onAuthenticationFailure
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
            putStringArrayListExtra(
                RE_ISSUANCE_IDS_DETAILS_EXTRA,
                ArrayList(removedIds)
            )
        }
        applicationContext.sendBroadcast(detailsIntent)
    }

    private fun notifyDocumentsList() {
        val refreshIntent = Intent(CoreActions.RE_ISSUANCE_WORK_REFRESH_ACTION)
        applicationContext.sendBroadcast(refreshIntent)
    }
}