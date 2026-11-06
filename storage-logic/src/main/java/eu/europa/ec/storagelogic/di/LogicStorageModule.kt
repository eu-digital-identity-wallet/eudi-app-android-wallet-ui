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

package eu.europa.ec.storagelogic.di

import android.content.Context
import androidx.room.Room
import eu.europa.ec.businesslogic.controller.crypto.CryptoController
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.storagelogic.dao.BookmarkDao
import eu.europa.ec.storagelogic.dao.FailedReIssuedDocumentDao
import eu.europa.ec.storagelogic.dao.IssuerTrustDao
import eu.europa.ec.storagelogic.dao.RevokedDocumentDao
import eu.europa.ec.storagelogic.dao.StatusRefreshFailureDao
import eu.europa.ec.storagelogic.dao.TransactionLogDao
import eu.europa.ec.storagelogic.service.DatabaseEraser
import eu.europa.ec.storagelogic.service.DatabaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val logicStorageModule = module {
    singleOf(::provideAppDatabase)
    singleOf(::provideDatabaseEraser)
    singleOf(::provideBookmarkDao)
    singleOf(::provideRevokedDocumentDao)
    singleOf(::provideTransactionLogDao)
    singleOf(::provideFailedReIssuedDocumentDao)
    singleOf(::provideIssuerTrustDao)
    singleOf(::provideStatusRefreshFailureDao)
}

/**
 * Opens the database with the key kept in the preferences. Without a stored key, an existing
 * database file was written with a key that is gone, for example because the preferences were lost,
 * and cannot be opened; it is deleted before a new key is created.
 */
fun provideAppDatabase(
    context: Context,
    prefKeys: PrefKeys,
    cryptoController: CryptoController
): DatabaseService {
    System.loadLibrary("sqlcipher")
    val key = runBlocking(Dispatchers.IO) {
        prefKeys.getDbKey() ?: cryptoController.createRandomKey(context).also {
            context.deleteDatabase(DatabaseService.DATABASE_NAME)
            prefKeys.setDbKey(it)
        }
    }
    return Room.databaseBuilder(
        context,
        DatabaseService::class.java,
        DatabaseService.DATABASE_NAME
    )
        .openHelperFactory(SupportOpenHelperFactory(key))
        .fallbackToDestructiveMigration(true)
        .build()
}

fun provideDatabaseEraser(service: DatabaseService): DatabaseEraser =
    DatabaseEraser(service)

fun provideBookmarkDao(service: DatabaseService): BookmarkDao = service.bookmarkDao()

fun provideRevokedDocumentDao(service: DatabaseService): RevokedDocumentDao =
    service.revokedDocumentDao()

fun provideTransactionLogDao(service: DatabaseService): TransactionLogDao =
    service.transactionLogDao()

fun provideFailedReIssuedDocumentDao(service: DatabaseService): FailedReIssuedDocumentDao =
    service.failedReIssuedDocumentDao()

fun provideIssuerTrustDao(service: DatabaseService): IssuerTrustDao =
    service.issuerTrustDao()

fun provideStatusRefreshFailureDao(service: DatabaseService): StatusRefreshFailureDao =
    service.statusRefreshFailureDao()