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
import androidx.room.PrimaryKey

/**
 * Expiry and issuer trust rejection end polling. Documents shows both as Failed until user
 * removal.
 */
enum class DeferredDocumentScheduleStatus {
    ACTIVE,
    EXPIRED,
    ISSUER_NOT_TRUSTED,
}

/**
 * Scheduling metadata only; Wallet Core owns the document and its issuance context.
 *
 * @property identifier The Core document ID, retained when a Deferred document becomes Issued.
 * @property nextAttemptAt Earliest next attempt in epoch milliseconds, including the deadline saved
 * before a request.
 * @property lastIntervalMillis Last valid issuer interval; null until a valid interval is received.
 * @property status A retryable failure keeps the schedule active. Expiry and trust refusal survive
 * restart.
 * @property originalDocumentId Known Reissuance origin; null for initial issuance or unknown
 * recovery data.
 */
@Entity(tableName = "deferredDocumentSchedules")
data class DeferredDocumentSchedule(
    @PrimaryKey
    val identifier: String,
    val nextAttemptAt: Long,
    val lastIntervalMillis: Long?,
    val originalDocumentId: String?,
    val status: DeferredDocumentScheduleStatus = DeferredDocumentScheduleStatus.ACTIVE,
)