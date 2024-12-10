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
import eu.europa.ec.storagelogic.model.Bookmark
import eu.europa.ec.storagelogic.model.RealmBookmark
import eu.europa.ec.storagelogic.model.toBookmark
import eu.europa.ec.storagelogic.model.toBookmarks
import eu.europa.ec.storagelogic.model.toRealm
import eu.europa.ec.storagelogic.service.RealmService
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query

interface BookmarkStorageController : StorageController<Bookmark>

class BookmarkStorageControllerImpl(
    private val realmService: RealmService,
) : BookmarkStorageController {

    override suspend fun store(value: Bookmark) {
        realmService.get().writeBlocking {
            copyToRealm(value.toRealm())
        }
    }

    override suspend fun update(value: Bookmark) {
        realmService.get().writeBlocking {
            copyToRealm(value.toRealm(), updatePolicy = UpdatePolicy.ALL)
        }
    }

    override suspend fun store(values: List<Bookmark>) {
        realmService.get().writeBlocking {
            values.map { copyToRealm(it.toRealm()) }
        }
    }

    override suspend fun retrieve(identifier: String): Bookmark? {
        return retrieve("identifier == $0", identifier)
    }

    override suspend fun retrieve(query: String, vararg args: Any?): Bookmark? {
        return realmService.get().query<RealmBookmark>(query, *args)
            .find()
            .firstOrNull()
            .toBookmark()
    }

    override suspend fun retrieveAll(): List<Bookmark> {
        return realmService.get().query<RealmBookmark>().find().toBookmarks()
    }

    override suspend fun delete(identifier: String) {
        realmService.get().apply {
            query<RealmBookmark>("identifier == $0", identifier)
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
            val allValues = query<RealmBookmark>().find()
            delete(allValues)
        }
    }
}