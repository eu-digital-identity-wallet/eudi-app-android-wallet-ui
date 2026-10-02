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
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import eu.europa.ec.storagelogic.dao.type.StorageDao
import eu.europa.ec.storagelogic.model.TransactionLog
import kotlinx.coroutines.flow.Flow

/** Payload updates preserve the original parent and communication method. */
@Dao
abstract class TransactionLogDao : StorageDao<TransactionLog> {
    @Transaction
    override suspend fun store(value: TransactionLog) {
        upsert(value.withStoredMetadata())
    }

    @Transaction
    override suspend fun storeAll(values: List<TransactionLog>) {
        values.forEach { transaction -> store(transaction) }
    }

    @Query("SELECT * FROM transactionLogs WHERE identifier = :identifier")
    abstract override suspend fun retrieve(identifier: String): TransactionLog?

    @Query("SELECT * FROM transactionLogs")
    abstract override suspend fun retrieveAll(): List<TransactionLog>

    @Query("SELECT * FROM transactionLogs WHERE parentPresentationId = :parentPresentationId")
    abstract fun observeByParentPresentationId(parentPresentationId: String): Flow<List<TransactionLog>>

    @Transaction
    override suspend fun update(value: TransactionLog) {
        updateStored(value.withStoredMetadata())
    }

    @Query("DELETE FROM transactionLogs WHERE identifier = :identifier")
    abstract override suspend fun delete(identifier: String)

    @Query("DELETE FROM transactionLogs")
    abstract override suspend fun deleteAll()

    // Upsert because @Insert(onConflict = OnConflictStrategy.REPLACE) deletes
    // the existing row and cascades to its linked DDR/DPAR records.
    @Upsert
    protected abstract suspend fun upsert(value: TransactionLog)

    @Update
    protected abstract suspend fun updateStored(value: TransactionLog)

    private suspend fun TransactionLog.withStoredMetadata(): TransactionLog {
        val stored = retrieve(identifier) ?: return this
        return copy(
            parentPresentationId = stored.parentPresentationId,
            communicationMethod = stored.communicationMethod,
        )
    }
}