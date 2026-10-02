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

import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.corelogic.extension.toDataDeletionRequestEntry
import eu.europa.ec.corelogic.extension.toDpaReportEntry
import eu.europa.ec.corelogic.extension.toSigningEntries
import eu.europa.ec.corelogic.model.CommunicationMethodDomain
import eu.europa.ec.corelogic.model.DpaContactDomain
import eu.europa.ec.corelogic.model.TransactionLogDomain
import eu.europa.ec.eudi.rqes.core.RqesSigningLogger
import eu.europa.ec.eudi.rqes.core.RqesSigningRecord
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogManager
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionEntry
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/** Records signings, data deletion requests and transaction reports in transaction history. */
interface WalletCoreTransactionRecordingController : RqesSigningLogger {
    suspend fun recordDataDeletionRequest(
        id: String,
        time: Instant,
        presentation: TransactionLogDomain.Presentation,
        communicationMethod: CommunicationMethodDomain,
    ): RecordTransactionPartialState

    suspend fun recordDpaReport(
        id: String,
        time: Instant,
        parentPresentationId: String,
        authority: DpaContactDomain,
        communicationMethod: CommunicationMethodDomain,
    ): RecordTransactionPartialState
}

sealed interface RecordTransactionPartialState {
    data object Success : RecordTransactionPartialState
    data class Failure(val errorMessage: String) : RecordTransactionPartialState
}

class WalletCoreTransactionRecordingControllerImpl(
    private val uuidProvider: UuidProvider,
    private val transactionLogManager: TransactionLogManager,
    private val walletCoreTransactionLogController: WalletCoreTransactionLogController,
    private val resourceProvider: ResourceProvider,
) : WalletCoreTransactionRecordingController {

    private val genericErrorMsg: String
        get() = resourceProvider.genericErrorMessage()

    override fun onSigningCompleted(record: RqesSigningRecord) {
        // A logging error should not change the signing result.
        runCatching {
            record.toSigningEntries(
                idProvider = {
                    uuidProvider.provideUuid()
                },
                time = Instant.now(),
            ).forEach { transactionEntry: TransactionEntry.SigningSealing ->
                transactionLogManager.log(transactionEntry)
            }
        }.onFailure { error ->
            if (error is CancellationException) throw error
        }
    }

    override suspend fun recordDataDeletionRequest(
        id: String,
        time: Instant,
        presentation: TransactionLogDomain.Presentation,
        communicationMethod: CommunicationMethodDomain,
    ): RecordTransactionPartialState = record(
        parentPresentationId = presentation.id,
        communicationMethod = communicationMethod,
    ) {
        presentation.toDataDeletionRequestEntry(id = id, time = time)
    }

    override suspend fun recordDpaReport(
        id: String,
        time: Instant,
        parentPresentationId: String,
        authority: DpaContactDomain,
        communicationMethod: CommunicationMethodDomain,
    ): RecordTransactionPartialState = record(
        parentPresentationId = parentPresentationId,
        communicationMethod = communicationMethod,
    ) {
        authority.toDpaReportEntry(id = id, time = time)
    }

    private suspend fun record(
        parentPresentationId: String,
        communicationMethod: CommunicationMethodDomain,
        createEntry: () -> TransactionEntry,
    ): RecordTransactionPartialState = withContext(Dispatchers.IO) {
        runCatching {
            walletCoreTransactionLogController.recordPresentationAction(
                parentPresentationId = parentPresentationId,
                entry = createEntry(),
                communicationMethod = communicationMethod,
            )
        }.fold(
            onSuccess = { recorded ->
                if (recorded) {
                    RecordTransactionPartialState.Success
                } else {
                    RecordTransactionPartialState.Failure(
                        errorMessage = genericErrorMsg
                    )
                }
            },
            onFailure = { error ->
                if (error is CancellationException) throw error

                RecordTransactionPartialState.Failure(
                    errorMessage = error.localizedMessage ?: genericErrorMsg
                )
            }
        )
    }
}