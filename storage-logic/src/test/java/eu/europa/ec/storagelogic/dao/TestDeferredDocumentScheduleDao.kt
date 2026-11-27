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
import eu.europa.ec.storagelogic.model.DeferredDocumentSchedule
import eu.europa.ec.storagelogic.model.DeferredDocumentScheduleStatus
import eu.europa.ec.storagelogic.service.DatabaseService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TestDeferredDocumentScheduleDao {

    private val database = Room.inMemoryDatabaseBuilder(
        RuntimeEnvironment.getApplication(),
        DatabaseService::class.java,
    ).allowMainThreadQueries().build()

    private val dao = database.deferredDocumentScheduleDao()

    @After
    fun after() {
        database.close()
    }

    /**
     * 1. A schedule is stored for a document.
     * 2. A later callback stores the same document with a later deadline.
     *
     * Expected result:
     * The first deadline is kept.
     */
    @Test
    fun `Given a stored schedule, When its ID is stored again, Then the first deadline stays`() =
        runBlocking {
            // Given
            dao.store(mockedSchedule)

            // When
            dao.store(mockedSchedule.copy(nextAttemptAt = 9_000, lastIntervalMillis = 4_000))

            // Then
            assertEquals(listOf(mockedSchedule), dao.retrieveAll())
        }

    /**
     * 1. A schedule has stopped because its transaction expired.
     * 2. A repeated callback stores the same document as active.
     *
     * Expected result:
     * The terminal status is kept.
     */
    @Test
    fun `Given an expired schedule, When its ID is stored as active, Then it stays expired`() =
        runBlocking {
            // Given
            val expired = mockedSchedule.copy(status = DeferredDocumentScheduleStatus.EXPIRED)
            dao.store(expired)

            // When
            dao.store(mockedSchedule)

            // Then
            assertEquals(expired, dao.retrieve(mockedSchedule.identifier))
        }

    @Test
    fun `Given a stored schedule, When a list with its ID is stored, Then the first row stays`() =
        runBlocking {
            // Given
            dao.store(mockedSchedule)

            // When
            dao.storeAll(
                listOf(
                    mockedSchedule.copy(nextAttemptAt = 9_000),
                    mockedSchedule.copy(identifier = "other", nextAttemptAt = 7_000),
                )
            )

            // Then
            assertEquals(
                listOf(
                    mockedSchedule,
                    mockedSchedule.copy(identifier = "other", nextAttemptAt = 7_000),
                ),
                dao.retrieveAll(),
            )
        }

    /**
     * 1. Three active rows, two of them with the same deadline.
     * 2. One expired row and one row refused for issuer trust, both due earlier.
     *
     * Expected result:
     * Only the active rows, ordered by deadline and then by identifier.
     */
    @Test
    fun `Given mixed rows, When observeActive is read, Then active rows come in order`() =
        runBlocking {
            // Given
            dao.storeAll(
                listOf(
                    mockedSchedule.copy(identifier = "b", nextAttemptAt = 2_000),
                    mockedSchedule.copy(identifier = "a", nextAttemptAt = 2_000),
                    mockedSchedule.copy(identifier = "c", nextAttemptAt = 1_000),
                    mockedSchedule.copy(
                        identifier = "expired",
                        nextAttemptAt = 10,
                        status = DeferredDocumentScheduleStatus.EXPIRED,
                    ),
                    mockedSchedule.copy(
                        identifier = "untrusted",
                        nextAttemptAt = 20,
                        status = DeferredDocumentScheduleStatus.ISSUER_NOT_TRUSTED,
                    ),
                )
            )

            // When
            val active = dao.observeActive().first()

            // Then
            assertEquals(listOf("c", "a", "b"), active.map { it.identifier })
        }

    @Test
    fun `Given an active row, When it is marked expired, Then observeActive drops it`() =
        runBlocking {
            // Given
            dao.store(mockedSchedule)

            // When
            dao.update(mockedSchedule.copy(status = DeferredDocumentScheduleStatus.EXPIRED))

            // Then
            assertEquals(emptyList<DeferredDocumentSchedule>(), dao.observeActive().first())
        }

    /**
     * 1. One row has no known original and another already has one.
     * 2. The same original is attached to both, and to an ID without a row.
     *
     * Expected result:
     * Only the row without an original gains it; the known original stays and no row is created.
     */
    @Test
    fun `Given known and unknown origins, When attached, Then only the unknown is filled`() =
        runBlocking {
            // Given
            dao.storeAll(
                listOf(
                    mockedSchedule.copy(identifier = "unknown"),
                    mockedSchedule.copy(identifier = "known", originalDocumentId = "first"),
                )
            )

            // When
            dao.attachOriginalIfUnknown(documentId = "unknown", originalId = "second")
            dao.attachOriginalIfUnknown(documentId = "known", originalId = "second")
            dao.attachOriginalIfUnknown(documentId = "missing", originalId = "second")

            // Then
            assertEquals("second", dao.retrieve("unknown")?.originalDocumentId)
            assertEquals("first", dao.retrieve("known")?.originalDocumentId)
            assertNull(dao.retrieve("missing"))
        }

    /**
     * 1. An active replacement of one original and an expired replacement of another.
     *
     * Expected result:
     * Only the original with the active replacement has a pending replacement.
     */
    @Test
    fun `Given two replacements, When asked if pending, Then only the active one counts`() =
        runBlocking {
            // Given
            dao.storeAll(
                listOf(
                    mockedSchedule.copy(identifier = "active", originalDocumentId = "first"),
                    mockedSchedule.copy(
                        identifier = "expired",
                        originalDocumentId = "second",
                        status = DeferredDocumentScheduleStatus.EXPIRED,
                    ),
                    mockedSchedule.copy(identifier = "initial"),
                )
            )

            // When
            val first = dao.hasPendingReplacement("first")
            val second = dao.hasPendingReplacement("second")
            val other = dao.hasPendingReplacement("other")

            // Then
            assertTrue(first)
            assertFalse(second)
            assertFalse(other)
        }

    @Test
    fun `Given mixed rows, When getTerminalDocumentIds is called, Then stopped IDs return`() =
        runBlocking {
            // Given
            dao.storeAll(
                listOf(
                    mockedSchedule.copy(identifier = "active"),
                    mockedSchedule.copy(
                        identifier = "expired",
                        status = DeferredDocumentScheduleStatus.EXPIRED,
                    ),
                    mockedSchedule.copy(
                        identifier = "untrusted",
                        status = DeferredDocumentScheduleStatus.ISSUER_NOT_TRUSTED,
                    ),
                )
            )

            // When
            val terminalIds = dao.getTerminalDocumentIds()

            // Then
            assertEquals(setOf("expired", "untrusted"), terminalIds.toSet())
            assertEquals(2, terminalIds.size)
        }

    @Test
    fun `Given two rows, When one is deleted, Then only the other remains`() =
        runBlocking {
            // Given
            dao.storeAll(listOf(mockedSchedule, mockedSchedule.copy(identifier = "other")))

            // When
            dao.delete(mockedSchedule.identifier)

            // Then
            assertEquals(listOf(mockedSchedule.copy(identifier = "other")), dao.retrieveAll())
        }

    @Test
    fun `Given active and stopped rows, When deleteAll is called, Then no row remains`() =
        runBlocking {
            // Given
            dao.storeAll(
                listOf(
                    mockedSchedule,
                    mockedSchedule.copy(
                        identifier = "expired",
                        status = DeferredDocumentScheduleStatus.EXPIRED,
                    ),
                )
            )

            // When
            dao.deleteAll()

            // Then
            assertEquals(emptyList<DeferredDocumentSchedule>(), dao.retrieveAll())
        }

    private val mockedSchedule = DeferredDocumentSchedule(
        identifier = "pending",
        nextAttemptAt = 5_000,
        lastIntervalMillis = null,
        originalDocumentId = null,
    )
}
