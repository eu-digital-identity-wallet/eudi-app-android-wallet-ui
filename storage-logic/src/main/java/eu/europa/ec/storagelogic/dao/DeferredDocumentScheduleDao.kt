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

package eu.europa.ec.storagelogic.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import eu.europa.ec.storagelogic.dao.type.StorageDao
import eu.europa.ec.storagelogic.model.DeferredDocumentSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface DeferredDocumentScheduleDao : StorageDao<DeferredDocumentSchedule> {

    @Query(
        "SELECT * FROM deferredDocumentSchedules WHERE status = 'ACTIVE' " +
                "ORDER BY nextAttemptAt, identifier"
    )
    fun observeActive(): Flow<List<DeferredDocumentSchedule>>

    @Query("SELECT * FROM deferredDocumentSchedules ORDER BY nextAttemptAt, identifier")
    override suspend fun retrieveAll(): List<DeferredDocumentSchedule>

    @Query("SELECT * FROM deferredDocumentSchedules WHERE identifier = :identifier")
    override suspend fun retrieve(identifier: String): DeferredDocumentSchedule?

    /** Repeated callbacks must not reset an existing deadline or stopped state. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun store(value: DeferredDocumentSchedule)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    override suspend fun storeAll(values: List<DeferredDocumentSchedule>)

    /** A callback can supply provenance after bootstrap has already discovered this ID. */
    @Query(
        "UPDATE deferredDocumentSchedules SET originalDocumentId = :originalId " +
                "WHERE identifier = :documentId AND originalDocumentId IS NULL"
    )
    suspend fun attachOriginalIfUnknown(documentId: String, originalId: String)

    @Update
    override suspend fun update(value: DeferredDocumentSchedule)

    @Query("DELETE FROM deferredDocumentSchedules WHERE identifier = :identifier")
    override suspend fun delete(identifier: String)

    @Query("DELETE FROM deferredDocumentSchedules")
    override suspend fun deleteAll()

    /** Only replacements with further issuance attempts block automatic Reissuance. */
    @Query(
        "SELECT EXISTS(SELECT 1 FROM deferredDocumentSchedules " +
                "WHERE originalDocumentId = :originalId AND status = 'ACTIVE')"
    )
    suspend fun hasPendingReplacement(originalId: String): Boolean

    @Query("SELECT identifier FROM deferredDocumentSchedules WHERE status != 'ACTIVE'")
    suspend fun getTerminalDocumentIds(): List<String>
}