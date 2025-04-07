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

package eu.europa.ec.corelogic.workmanager

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.model.RevokedDocumentPayload
import eu.europa.ec.corelogic.util.CoreActions
import eu.europa.ec.corelogic.util.CoreActions.REVOCATION_IDS_DETAILS_EXTRA
import eu.europa.ec.storagelogic.controller.RevokedDocumentsStorageController
import eu.europa.ec.storagelogic.model.RevokedDocument
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RevocationWorkManager(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val revokedDocumentsController: RevokedDocumentsStorageController by inject()
    private val walletCoreDocumentsController: WalletCoreDocumentsController by inject()
    private val logController: LogController by inject()

    companion object {
        private const val TAG = "RevocationWorkManager"
        const val REVOCATION_WORK_NAME = "revocationWorker"
    }

    override suspend fun doWork(): Result {
        try {
            logController.d(TAG) { "Checking for revoked documents..." }

            // Mock request
            delay(50000)
            val mockedRequestResult =
                listOf("Document_EudiWalletDocumentManager_597c1f9f-91bd-4848-aaac-414b8720a79e")
            val resultToDomain =
                mockedRequestResult
                    .map {
                        RevokedDocument(
                            identifier = it
                        )
                    }

            val revokedDocumentsPayloadList = walletCoreDocumentsController.getAllDocuments()
                .filter { mockedRequestResult.contains(it.id) }
                .map { RevokedDocumentPayload(name = it.name, id = it.id) }
                .toTypedArray()

            if (mockedRequestResult.isNotEmpty()) {
                revokedDocumentsController.store(resultToDomain)
                val messageIntent = Intent(CoreActions.REVOCATION_WORK_MESSAGE_ACTION).apply {
                    putParcelableArrayListExtra(
                        CoreActions.REVOCATION_IDS_EXTRA,
                        ArrayList(revokedDocumentsPayloadList.toList())
                    )
                }
                val refreshIntent = Intent(CoreActions.REVOCATION_WORK_REFRESH_ACTION)
                val detailsIntent =
                    Intent(CoreActions.REVOCATION_WORK_REFRESH_DETAILS_ACTION).apply {
                        putStringArrayListExtra(
                            REVOCATION_IDS_DETAILS_EXTRA,
                            ArrayList(mockedRequestResult)
                        )
                    }

                applicationContext.sendBroadcast(messageIntent)
                applicationContext.sendBroadcast(detailsIntent)
                applicationContext.sendBroadcast(refreshIntent)
            }

            logController.d(TAG) { "Done!" }

            return Result.success()
        } catch (ex: Exception) {
            logController.d(TAG) { ex.message ?: "RevocationWorkManager encountered an error" }
            return Result.failure()
        }
    }
}