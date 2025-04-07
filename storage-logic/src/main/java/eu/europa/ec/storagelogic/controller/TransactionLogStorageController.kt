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

package eu.europa.ec.storagelogic.controller

import eu.europa.ec.storagelogic.controller.type.StorageController
import eu.europa.ec.storagelogic.model.RealmTransactionLog
import eu.europa.ec.storagelogic.model.TransactionLog
import eu.europa.ec.storagelogic.model.toRealm
import eu.europa.ec.storagelogic.model.toTransactionLog
import eu.europa.ec.storagelogic.model.toTransactionLogs
import eu.europa.ec.storagelogic.service.RealmService
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query

interface TransactionLogStorageController : StorageController<TransactionLog>

class TransactionLogStorageControllerImpl(
    private val realmService: RealmService,
) : TransactionLogStorageController {

    override suspend fun store(value: TransactionLog) {
        realmService.get().writeBlocking {
            copyToRealm(value.toRealm())
        }
    }

    override suspend fun update(value: TransactionLog) {
        realmService.get().writeBlocking {
            copyToRealm(value.toRealm(), updatePolicy = UpdatePolicy.ALL)
        }
    }

    override suspend fun store(values: List<TransactionLog>) {
        realmService.get().writeBlocking {
            values.map { copyToRealm(it.toRealm()) }
        }
    }

    override suspend fun retrieve(identifier: String): TransactionLog? {
        return retrieve("identifier == $0", identifier)
    }

    override suspend fun retrieve(query: String, vararg args: Any?): TransactionLog? {
        return realmService.get().query<RealmTransactionLog>(query, *args)
            .find()
            .firstOrNull()
            .toTransactionLog()
    }

    override suspend fun retrieveAll(): List<TransactionLog> {
        return realmService.get().query<RealmTransactionLog>().find().toTransactionLogs()
    }

    override suspend fun delete(identifier: String) {
        realmService.get().apply {
            query<RealmTransactionLog>("identifier == $0", identifier)
                .find()
                .firstOrNull()
                ?.let { result ->
                    writeBlocking {
                        findLatest(result)?.also {
                            delete(it)
                        }
                    }
                }
        }
    }

    override suspend fun deleteAll() {
        realmService.get().writeBlocking {
            val allValues = query<RealmTransactionLog>().find()
            delete(allValues)
        }
    }
}