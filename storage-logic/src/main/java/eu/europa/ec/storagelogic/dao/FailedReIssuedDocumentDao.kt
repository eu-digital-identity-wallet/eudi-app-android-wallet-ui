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

package eu.europa.ec.storagelogic.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import eu.europa.ec.storagelogic.dao.type.StorageDao
import eu.europa.ec.storagelogic.model.FailedReIssuedDocument

@Dao
interface FailedReIssuedDocumentDao : StorageDao<FailedReIssuedDocument> {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    override suspend fun store(value: FailedReIssuedDocument)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    override suspend fun storeAll(values: List<FailedReIssuedDocument>)

    @Query("SELECT * FROM failedReIssuedDocuments WHERE identifier = :identifier")
    override suspend fun retrieve(identifier: String): FailedReIssuedDocument?

    @Query("SELECT * FROM failedReIssuedDocuments")
    override suspend fun retrieveAll(): List<FailedReIssuedDocument>

    @Update
    override suspend fun update(value: FailedReIssuedDocument)

    @Query("DELETE FROM failedReIssuedDocuments WHERE identifier = :identifier")
    override suspend fun delete(identifier: String)

    @Query("DELETE FROM failedReIssuedDocuments")
    override suspend fun deleteAll()
}