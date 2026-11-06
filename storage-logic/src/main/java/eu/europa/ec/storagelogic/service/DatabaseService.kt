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

package eu.europa.ec.storagelogic.service

import androidx.room.Database
import androidx.room.RoomDatabase
import eu.europa.ec.storagelogic.dao.BookmarkDao
import eu.europa.ec.storagelogic.dao.FailedReIssuedDocumentDao
import eu.europa.ec.storagelogic.dao.IssuerTrustDao
import eu.europa.ec.storagelogic.dao.RevokedDocumentDao
import eu.europa.ec.storagelogic.dao.StatusRefreshFailureDao
import eu.europa.ec.storagelogic.dao.TransactionLogDao
import eu.europa.ec.storagelogic.model.Bookmark
import eu.europa.ec.storagelogic.model.FailedReIssuedDocument
import eu.europa.ec.storagelogic.model.IssuerTrust
import eu.europa.ec.storagelogic.model.RevokedDocument
import eu.europa.ec.storagelogic.model.StatusRefreshFailure
import eu.europa.ec.storagelogic.model.TransactionLog

/**
 * The app's Room database. Schema changes bump the version and rely on the destructive fallback in
 * `provideAppDatabase`, which recreates the tables; no migrations are kept. Recreating the tables
 * drops the transaction history, bookmarks, data-deletion and DPA-report attempts, failed
 * re-issuance records and issuer-trust records with them; every document then shows its issuer
 * verification as unavailable until it is issued again. The revocation and status-refresh-failure
 * records are rebuilt by the revocation check run when the wallet opens.
 *
 * The version only goes up. With the same version and a different schema, Room's identity check
 * fails on every open and the fallback never runs; a lower version is a downgrade, which recreates
 * the tables as well. Version 2 was used from March to September 2026 with a different schema and
 * is skipped: a database still at that version would otherwise be opened as current and fail
 * Room's schema check.
 */
@Database(
    entities = [
        Bookmark::class,
        RevokedDocument::class,
        TransactionLog::class,
        FailedReIssuedDocument::class,
        IssuerTrust::class,
        StatusRefreshFailure::class,
    ],
    version = 5
)
abstract class DatabaseService : RoomDatabase() {

    companion object {
        const val DATABASE_NAME = "eudi.app.wallet.storage"
    }

    abstract fun bookmarkDao(): BookmarkDao
    abstract fun revokedDocumentDao(): RevokedDocumentDao
    abstract fun transactionLogDao(): TransactionLogDao
    abstract fun failedReIssuedDocumentDao(): FailedReIssuedDocumentDao
    abstract fun issuerTrustDao(): IssuerTrustDao
    abstract fun statusRefreshFailureDao(): StatusRefreshFailureDao
}