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

package eu.europa.ec.corelogic.worker

import android.content.Context
import android.content.Intent
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import eu.europa.ec.corelogic.controller.WalletCoreDCAPIRegistrationController
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.util.CoreActions
import eu.europa.ec.eudi.statium.Status
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.storagelogic.dao.RevokedDocumentDao
import eu.europa.ec.storagelogic.dao.StatusRefreshFailureDao
import eu.europa.ec.storagelogic.model.RevocationStatus
import eu.europa.ec.storagelogic.model.RevokedDocument
import eu.europa.ec.storagelogic.model.StatusRefreshFailure
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import kotlinx.coroutines.CancellationException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TestRevocationWorkManager {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var revokedDocumentDao: RevokedDocumentDao

    @Mock
    private lateinit var statusRefreshFailureDao: StatusRefreshFailureDao

    @Mock
    private lateinit var walletCoreDocumentsController: WalletCoreDocumentsController

    @Mock
    private lateinit var walletCoreDCAPIRegistrationController:
            WalletCoreDCAPIRegistrationController

    private lateinit var closeable: AutoCloseable

    private val documentId = "document-1"

    private val document: IssuedDocument = mock {
        on { id } doReturn documentId
        on { name } doReturn "PID"
    }

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
    }

    @After
    fun after() {
        closeable.close()
    }

    @Test
    fun `Given a document recorded as Invalid, When its status reads Valid, Then it stays blocked`() {
        coroutineRule.runTest {
            givenStoredStatus(RevocationStatus.INVALID)
            givenObservedStatus(Status.Valid)

            runWorker()

            verify(revokedDocumentDao, never()).delete(documentId)
            verify(revokedDocumentDao, never()).store(any())
        }
    }

    @Test
    fun `Given a document newly revoked, When the check ends, Then the DC-API registry is updated`() {
        coroutineRule.runTest {
            givenStoredStatus(null)
            givenObservedStatus(Status.Invalid)
            whenever(revokedDocumentDao.retrieveAll())
                .thenReturn(
                    emptyList(),
                    listOf(RevokedDocument(documentId, RevocationStatus.INVALID)),
                )
            whenever(walletCoreDCAPIRegistrationController.excludedDocumentIds)
                .thenReturn(emptySet())

            runWorker()

            verify(walletCoreDCAPIRegistrationController).registerCredentials()
        }
    }

    @Test
    fun `Given a suspended document restored, When the check ends, Then the DC-API registry is updated`() {
        coroutineRule.runTest {
            givenStoredStatus(RevocationStatus.SUSPENDED)
            givenObservedStatus(Status.Valid)
            whenever(revokedDocumentDao.retrieveAll())
                .thenReturn(
                    listOf(RevokedDocument(documentId, RevocationStatus.SUSPENDED)),
                    emptyList(),
                )
            whenever(walletCoreDCAPIRegistrationController.excludedDocumentIds)
                .thenReturn(setOf(documentId))

            runWorker()

            verify(walletCoreDCAPIRegistrationController).registerCredentials()
        }
    }

    @Test
    fun `Given the registry already leaves out the blocked documents, When the check ends, Then it is not updated`() {
        coroutineRule.runTest {
            givenStoredStatus(RevocationStatus.INVALID)
            givenObservedStatus(Status.Valid)
            whenever(walletCoreDCAPIRegistrationController.excludedDocumentIds)
                .thenReturn(setOf(documentId))

            runWorker()

            verify(walletCoreDCAPIRegistrationController, never()).registerCredentials()
        }
    }

    @Test
    fun `Given a document recorded as Suspended, When its status reads Valid, Then it is restored`() {
        coroutineRule.runTest {
            givenStoredStatus(RevocationStatus.SUSPENDED)
            givenObservedStatus(Status.Valid)

            runWorker()

            verify(revokedDocumentDao).delete(documentId)
        }
    }

    @Test
    fun `Given a document recorded as unrecognised, When its status reads Valid, Then it is restored`() {
        coroutineRule.runTest {
            givenStoredStatus(RevocationStatus.UNRECOGNISED)
            givenObservedStatus(Status.Valid)

            runWorker()

            verify(revokedDocumentDao).delete(documentId)
        }
    }

    @Test
    fun `Given a document with no record, When its status reads Invalid, Then it is recorded as Invalid`() {
        coroutineRule.runTest {
            givenStoredStatus(null)
            givenObservedStatus(Status.Invalid)

            runWorker()

            verify(revokedDocumentDao).store(RevokedDocument(documentId, RevocationStatus.INVALID))
        }
    }

    @Test
    fun `Given a document with no record, When its status reads Suspended, Then it is recorded as Suspended`() {
        coroutineRule.runTest {
            givenStoredStatus(null)
            givenObservedStatus(Status.Suspended)

            runWorker()

            verify(revokedDocumentDao).store(
                RevokedDocument(
                    documentId,
                    RevocationStatus.SUSPENDED
                )
            )
        }
    }

    @Test
    fun `Given a suspended document, When its status reads Invalid, Then the record becomes Invalid`() {
        coroutineRule.runTest {
            givenStoredStatus(RevocationStatus.SUSPENDED)
            givenObservedStatus(Status.Invalid)

            runWorker()

            verify(revokedDocumentDao).store(RevokedDocument(documentId, RevocationStatus.INVALID))
        }
    }

    @Test
    fun `Given a document recorded as Invalid, When its status reads Suspended, Then the record stays Invalid`() {
        coroutineRule.runTest {
            givenStoredStatus(RevocationStatus.INVALID)
            givenObservedStatus(Status.Suspended)

            runWorker()

            verify(revokedDocumentDao, never()).store(any())
            verify(revokedDocumentDao, never()).delete(documentId)
        }
    }

    @Test
    fun `Given a document with no record, When its status is application specific, Then it is blocked`() {
        coroutineRule.runTest {
            givenStoredStatus(null)
            givenObservedStatus(Status(0x03u))

            runWorker()

            verify(revokedDocumentDao).store(
                RevokedDocument(
                    documentId,
                    RevocationStatus.UNRECOGNISED
                )
            )
        }
    }

    @Test
    fun `Given a document with no record, When its status is a reserved value, Then it is blocked`() {
        coroutineRule.runTest {
            givenStoredStatus(null)
            givenObservedStatus(Status(0x04u))

            runWorker()

            verify(revokedDocumentDao).store(
                RevokedDocument(
                    documentId,
                    RevocationStatus.UNRECOGNISED
                )
            )
        }
    }

    @Test
    fun `Given a document with a status reference, When the status cannot be resolved, Then the failure is recorded`() {
        coroutineRule.runTest {
            givenStoredStatus(null)
            whenever(walletCoreDocumentsController.resolveDocumentStatus(document))
                .thenReturn(Result.failure(IllegalStateException("status list unreachable")))
            whenever(walletCoreDocumentsController.hasStatusReference(document)).thenReturn(true)

            runWorker()

            val captor = argumentCaptor<StatusRefreshFailure>()
            verify(statusRefreshFailureDao).store(captor.capture())
            assertEquals(documentId, captor.firstValue.identifier)
            assertEquals("status list unreachable", captor.firstValue.reason)
            verify(revokedDocumentDao, never()).store(any())
        }
    }

    @Test
    fun `Given a document without a status reference, When the status cannot be resolved, Then no failure is recorded`() {
        coroutineRule.runTest {
            givenStoredStatus(null)
            whenever(walletCoreDocumentsController.resolveDocumentStatus(document))
                .thenReturn(Result.failure(IllegalStateException("No status list found in SD-JWT VC")))
            whenever(walletCoreDocumentsController.hasStatusReference(document)).thenReturn(false)

            runWorker()

            verify(statusRefreshFailureDao, never()).store(any())
        }
    }

    @Test
    fun `Given an earlier failed refresh, When the status resolves, Then the failure is cleared and details refresh`() {
        coroutineRule.runTest {
            givenStoredStatus(null)
            givenEarlierRefreshFailure()
            givenObservedStatus(Status.Valid)

            runWorker()

            verify(statusRefreshFailureDao).delete(documentId)
            assertEquals(listOf(documentId), detailsRefreshIds())
        }
    }

    @Test
    fun `Given no earlier failed refresh, When the status resolves, Then nothing changes`() {
        coroutineRule.runTest {
            givenStoredStatus(null)
            givenObservedStatus(Status.Valid)

            runWorker()

            verify(statusRefreshFailureDao, never()).delete(documentId)
            assertEquals(emptyList<Intent>(), broadcasts())
        }
    }

    @Test
    fun `Given a first failed refresh, When the worker runs, Then details refresh`() {
        coroutineRule.runTest {
            givenStoredStatus(null)
            whenever(walletCoreDocumentsController.resolveDocumentStatus(document))
                .thenReturn(Result.failure(IllegalStateException("status list unreachable")))
            whenever(walletCoreDocumentsController.hasStatusReference(document)).thenReturn(true)

            runWorker()

            assertEquals(listOf(documentId), detailsRefreshIds())
        }
    }

    @Test
    fun `Given a document blocked for the first time, When the worker runs, Then the revocation message is sent`() {
        coroutineRule.runTest {
            givenStoredStatus(null)
            givenObservedStatus(Status.Invalid)

            runWorker()

            assertEquals(
                listOf(
                    CoreActions.REVOCATION_WORK_MESSAGE_ACTION,
                    CoreActions.REVOCATION_WORK_REFRESH_DETAILS_ACTION,
                    CoreActions.REVOCATION_WORK_REFRESH_ACTION,
                ),
                broadcasts().map { it.action }
            )
            assertEquals(listOf(documentId), detailsRefreshIds())
        }
    }

    @Test
    fun `Given a suspended document, When it becomes Invalid, Then details refresh without a new revocation message`() {
        coroutineRule.runTest {
            givenStoredStatus(RevocationStatus.SUSPENDED)
            givenObservedStatus(Status.Invalid)

            runWorker()

            assertEquals(
                listOf(
                    CoreActions.REVOCATION_WORK_REFRESH_DETAILS_ACTION,
                    CoreActions.REVOCATION_WORK_REFRESH_ACTION,
                ),
                broadcasts().map { it.action }
            )
        }
    }

    @Test
    fun `Given a cancelled resolution, When the worker runs, Then the cancellation is not recorded as a failure`() {
        coroutineRule.runTest {
            givenStoredStatus(null)
            whenever(walletCoreDocumentsController.hasStatusReference(document)).thenReturn(true)
            whenever(walletCoreDocumentsController.resolveDocumentStatus(document))
                .thenReturn(Result.failure(CancellationException("stopped")))

            val thrown = runCatching { runWorker() }.exceptionOrNull()

            assertTrue(thrown is CancellationException)
            verify(statusRefreshFailureDao, never()).store(any())
        }
    }

    @Test
    fun `Given every stored and observed status, When revocationUpdateFor is called, Then the rules hold`() {
        val applicationSpecific = Status(0x03u)
        val reserved = Status(0x04u)
        val cases = listOf(
            Triple(null, Status.Valid, RevocationUpdate.Keep),
            Triple(null, Status.Invalid, RevocationUpdate.Record(RevocationStatus.INVALID)),
            Triple(null, Status.Suspended, RevocationUpdate.Record(RevocationStatus.SUSPENDED)),
            Triple(
                null,
                applicationSpecific,
                RevocationUpdate.Record(RevocationStatus.UNRECOGNISED)
            ),
            Triple(null, reserved, RevocationUpdate.Record(RevocationStatus.UNRECOGNISED)),
            Triple(RevocationStatus.INVALID, Status.Valid, RevocationUpdate.Keep),
            Triple(RevocationStatus.INVALID, Status.Invalid, RevocationUpdate.Keep),
            Triple(RevocationStatus.INVALID, Status.Suspended, RevocationUpdate.Keep),
            Triple(RevocationStatus.INVALID, applicationSpecific, RevocationUpdate.Keep),
            Triple(RevocationStatus.SUSPENDED, Status.Valid, RevocationUpdate.Clear),
            Triple(
                RevocationStatus.SUSPENDED,
                Status.Invalid,
                RevocationUpdate.Record(RevocationStatus.INVALID)
            ),
            Triple(RevocationStatus.SUSPENDED, Status.Suspended, RevocationUpdate.Keep),
            Triple(
                RevocationStatus.SUSPENDED,
                reserved,
                RevocationUpdate.Record(RevocationStatus.UNRECOGNISED)
            ),
            Triple(RevocationStatus.UNRECOGNISED, Status.Valid, RevocationUpdate.Clear),
            Triple(
                RevocationStatus.UNRECOGNISED,
                Status.Invalid,
                RevocationUpdate.Record(RevocationStatus.INVALID)
            ),
            Triple(
                RevocationStatus.UNRECOGNISED,
                Status.Suspended,
                RevocationUpdate.Record(RevocationStatus.SUSPENDED)
            ),
            Triple(RevocationStatus.UNRECOGNISED, applicationSpecific, RevocationUpdate.Keep),
        )

        cases.forEach { (stored, observed, expected) ->
            assertEquals("$stored then $observed", expected, revocationUpdateFor(stored, observed))
        }
    }

    @Test
    fun `Given a valid document, When the worker runs, Then it reports success`() {
        coroutineRule.runTest {
            givenStoredStatus(null)
            givenObservedStatus(Status.Valid)

            assertEquals(ListenableWorker.Result.success(), runWorker())
        }
    }

    private suspend fun givenStoredStatus(status: RevocationStatus?) {
        whenever(revokedDocumentDao.retrieveAll()).thenReturn(
            listOfNotNull(status?.let { RevokedDocument(identifier = documentId, status = it) })
        )
        whenever(statusRefreshFailureDao.retrieveAll()).thenReturn(emptyList())
        whenever(walletCoreDocumentsController.getAllIssuedDocuments()).thenReturn(listOf(document))
    }

    private suspend fun givenEarlierRefreshFailure() {
        whenever(statusRefreshFailureDao.retrieveAll()).thenReturn(
            listOf(StatusRefreshFailure(identifier = documentId, failedAt = 0L, reason = "earlier"))
        )
    }

    private fun broadcasts(): List<Intent> =
        shadowOf(RuntimeEnvironment.getApplication()).broadcastIntents

    private fun detailsRefreshIds(): List<String>? =
        broadcasts()
            .firstOrNull { it.action == CoreActions.REVOCATION_WORK_REFRESH_DETAILS_ACTION }
            ?.getStringArrayListExtra(CoreActions.REVOCATION_IDS_DETAILS_EXTRA)

    private suspend fun givenObservedStatus(status: Status) {
        whenever(walletCoreDocumentsController.resolveDocumentStatus(document))
            .thenReturn(Result.success(status))
    }

    private suspend fun runWorker(): ListenableWorker.Result {
        val worker =
            TestListenableWorkerBuilder<RevocationWorkManager>(RuntimeEnvironment.getApplication())
                .setWorkerFactory(object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker = RevocationWorkManager(
                        appContext = appContext,
                        workerParams = workerParameters,
                        revokedDocumentDao = revokedDocumentDao,
                        statusRefreshFailureDao = statusRefreshFailureDao,
                        walletCoreDocumentsController = walletCoreDocumentsController,
                        walletCoreDCAPIRegistrationController =
                            walletCoreDCAPIRegistrationController,
                    )
                })
                .build()
        return worker.doWork()
    }
}