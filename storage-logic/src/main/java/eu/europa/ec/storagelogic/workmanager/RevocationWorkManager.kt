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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.storagelogic.controller.RevokedDocumentsStorageController
import eu.europa.ec.storagelogic.model.RevokedDocument
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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

            revokedDocumentsController.store(listOf("DF242-23321DF-F321F-F1413").map {
                RevokedDocument(
                    identifier = it
                )
            })

            logController.d(TAG) { "Done!" }

            return Result.success()
        } catch (ex: Exception) {
            logController.d(TAG) { ex.message ?: "RevocationWorkManager encountered an error" }
            return Result.failure()
        }
    }
}