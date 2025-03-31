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

package eu.europa.ec.storagelogic.workmanager

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.storagelogic.controller.RevokedDocumentsStorageController
import eu.europa.ec.storagelogic.model.RevokedDocument
import eu.europa.ec.storagelogic.receiver.RevocationWorkCompletionReceiver
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.random.Random

class RevocationWorkManager(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val revokedDocumentsController: RevokedDocumentsStorageController by inject()
    private val logController: LogController by inject()

    companion object {
        private const val TAG = "RevocationWorkManager"
    }

    override suspend fun doWork(): Result {
        try {
            logController.d(TAG) { "Checking for revoked documents..." }

            val mockedRequestResult = listOf("DF242-23321DF-F321F-LFKDSA3-${Random.nextInt(100, 1000)}")
            val resultToDomain =
                mockedRequestResult
                    .map {
                        RevokedDocument(
                            identifier = it
                        )
                    }

            if (mockedRequestResult.isNotEmpty()) {
                revokedDocumentsController.store(resultToDomain)
                val allRevokedDocuments = revokedDocumentsController.retrieveAll().map {
                    it.identifier
                }.toTypedArray()
                val intent = Intent(RevocationWorkCompletionReceiver.ACTION).apply {
                    putExtra(RevocationWorkCompletionReceiver.EXTRA_SHOW_NOTIFICATION, true)
                    putExtra(RevocationWorkCompletionReceiver.EXTRA_IDS, allRevokedDocuments)
                }
                applicationContext.sendBroadcast(intent)
            }

            logController.d(TAG) { "Done!" }

            return Result.success()
        } catch (ex: Exception) {
            logController.d(TAG) { ex.message ?: "RevocationWorkManager encountered an error" }
            return Result.failure()
        }
    }
}