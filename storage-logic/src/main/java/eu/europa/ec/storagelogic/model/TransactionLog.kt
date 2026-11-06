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

package eu.europa.ec.storagelogic.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A transaction-log entry as Wallet Core wrote it ([value]), with what the app records beside it.
 *
 * @property registrationCheck for a presentation, the outcome of the relying party's registration
 * check when the data was shared (`VERIFIED`, `NOT_VERIFIED` or `NOT_EVALUATED`), or null when
 * none was recorded. Wallet Core does not keep it in the entry.
 * @property registrationFailureReason why the registration was not verified, when it was not.
 */
@Entity(
    tableName = "transactionLogs",
    foreignKeys = [
        ForeignKey(
            entity = TransactionLog::class,
            parentColumns = ["identifier"],
            childColumns = ["parentPresentationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["parentPresentationId"])]
)
data class TransactionLog(
    @PrimaryKey
    val identifier: String,
    val value: String,
    val parentPresentationId: String?,
    val communicationMethod: String?,
    val registrationCheck: String? = null,
    val registrationFailureReason: String? = null,
)