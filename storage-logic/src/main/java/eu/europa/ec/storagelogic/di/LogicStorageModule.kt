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

package eu.europa.ec.storagelogic.di

import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.storagelogic.config.StorageConfig
import eu.europa.ec.storagelogic.config.StorageConfigImpl
import eu.europa.ec.storagelogic.controller.BookmarkStorageController
import eu.europa.ec.storagelogic.controller.BookmarkStorageControllerImpl
import eu.europa.ec.storagelogic.controller.RevokedDocumentsStorageController
import eu.europa.ec.storagelogic.controller.RevokedDocumentsStorageControllerImpl
import eu.europa.ec.storagelogic.controller.TransactionLogStorageController
import eu.europa.ec.storagelogic.controller.TransactionLogStorageControllerImpl
import eu.europa.ec.storagelogic.service.RealmService
import eu.europa.ec.storagelogic.service.RealmServiceImpl
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("eu.europa.ec.storagelogic")
class LogicStorageModule

@Single
fun provideStorageConfig(prefKeys: PrefKeys): StorageConfig =
    StorageConfigImpl(prefKeys)

@Single
fun provideRealmService(storageConfig: StorageConfig): RealmService =
    RealmServiceImpl(storageConfig)

@Factory
fun provideBookmarkStorageController(realmService: RealmService): BookmarkStorageController =
    BookmarkStorageControllerImpl(realmService)

@Factory
fun provideTransactionLogStorageController(realmService: RealmService): TransactionLogStorageController =
    TransactionLogStorageControllerImpl(realmService)

@Factory
fun provideRevokedDocumentsStorageController(realmService: RealmService): RevokedDocumentsStorageController =
    RevokedDocumentsStorageControllerImpl(realmService)