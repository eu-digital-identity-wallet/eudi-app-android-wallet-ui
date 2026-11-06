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
 * The last failed attempt to refresh a document's status: the status list could not be fetched,
 * parsed or verified. Removed by the next successful check.
 *
 * @property failedAt when the attempt failed, in milliseconds since the epoch.
 * @property reason a short description of the failure, for diagnosis.
 */
@Entity(tableName = "statusRefreshFailures")
data class StatusRefreshFailure(
    @PrimaryKey
    val identifier: String,
    val failedAt: Long,
    val reason: String,
)