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

import androidx.room.Room
import eu.europa.ec.storagelogic.model.RevocationStatus
import eu.europa.ec.storagelogic.model.RevokedDocument
import eu.europa.ec.storagelogic.service.DatabaseService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TestRevokedDocumentDao {

    private val database = Room.inMemoryDatabaseBuilder(
        RuntimeEnvironment.getApplication(),
        DatabaseService::class.java,
    ).allowMainThreadQueries().build()

    private val dao = database.revokedDocumentDao()

    @After
    fun after() {
        database.close()
    }

    @Test
    fun `Given a suspended document, When it is stored again as invalid, Then its record is replaced`() =
        runBlocking {
            dao.store(RevokedDocument(identifier = "document", status = RevocationStatus.SUSPENDED))

            dao.store(RevokedDocument(identifier = "document", status = RevocationStatus.INVALID))

            assertEquals(
                listOf(RevokedDocument(identifier = "document", status = RevocationStatus.INVALID)),
                dao.retrieveAll(),
            )
        }
}