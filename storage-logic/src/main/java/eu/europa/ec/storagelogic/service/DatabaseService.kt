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

package eu.europa.ec.storagelogic.service

import androidx.room.Database
import androidx.room.RoomDatabase
import eu.europa.ec.storagelogic.dao.BookmarkDao
import eu.europa.ec.storagelogic.dao.RevokedDocumentDao
import eu.europa.ec.storagelogic.dao.TransactionLogDao
import eu.europa.ec.storagelogic.model.Bookmark
import eu.europa.ec.storagelogic.model.RevokedDocument
import eu.europa.ec.storagelogic.model.TransactionLog

@Database(
    entities = [
        Bookmark::class,
        RevokedDocument::class,
        TransactionLog::class
    ],
    version = 1
)
abstract class DatabaseService : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun revokedDocumentDao(): RevokedDocumentDao
    abstract fun transactionLogDao(): TransactionLogDao
}