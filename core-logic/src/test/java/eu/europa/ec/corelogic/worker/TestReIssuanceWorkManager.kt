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
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.corelogic.controller.IssueDocumentsPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.util.mockedDeferredDocumentData
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings.CredentialPolicy
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.storagelogic.dao.DeferredDocumentScheduleDao
import eu.europa.ec.storagelogic.dao.FailedReIssuedDocumentDao
import eu.europa.ec.storagelogic.model.FailedReIssuedDocument
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TestReIssuanceWorkManager {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var failedReIssuedDocumentDao: FailedReIssuedDocumentDao

    @Mock
    private lateinit var deferredScheduleDao: DeferredDocumentScheduleDao

    @Mock
    private lateinit var walletCoreDocumentsController: WalletCoreDocumentsController

    private lateinit var closeable: AutoCloseable

    private val document: IssuedDocument = mock {
        on { id } doReturn "revoked-document"
    }

    private val dueDocument: IssuedDocument = mock {
        on { id } doReturn "due-document"
        on { credentialPolicy } doReturn CredentialPolicy.OnceOnly()
        on { getCredentials() } doReturn emptyList()
    }

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        runBlocking {
            whenever(deferredScheduleDao.hasPendingReplacement(dueDocument.id)).thenReturn(false)
        }
    }

    @After
    fun after() {
        closeable.close()
    }

    @Test
    fun `Given a revoked document, When the worker runs, Then it is not re-issued`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getAllIssuedDocuments()).thenReturn(
                listOf(
                    document
                )
            )
            whenever(walletCoreDocumentsController.isDocumentRevoked(document.id)).thenReturn(true)

            // When
            runWorker()

            // Then
            verify(walletCoreDocumentsController).getAllIssuedDocuments()
            verify(walletCoreDocumentsController).isDocumentRevoked(document.id)
            verifyNoMoreInteractions(walletCoreDocumentsController)
        }
    }

    @Test
    fun `Given a re-issuance in progress, When the worker is cancelled, Then it holds its lock until the outcome arrives`() {
        coroutineRule.runTest {
            // Given
            val outcome = CompletableDeferred<IssueDocumentsPartialState>()
            whenever(walletCoreDocumentsController.getAllIssuedDocuments()).thenReturn(
                listOf(
                    dueDocument
                )
            )
            whenever(walletCoreDocumentsController.isDocumentRevoked(dueDocument.id)).thenReturn(
                false
            )
            whenever(walletCoreDocumentsController.reIssueDocument(any(), any(), any(), any()))
                .thenReturn(flow { emit(outcome.await()) })

            // When
            val work = coroutineRule.testScope.launch { runWorker() }
            coroutineRule.testScope.runCurrent()
            work.cancel()
            coroutineRule.testScope.runCurrent()

            // Then
            assertTrue(ReIssuanceWorkManager.runLock.isLocked)

            // When
            outcome.complete(IssueDocumentsPartialState.Failure(errorMessage = "error"))
            work.join()

            // Then
            assertFalse(ReIssuanceWorkManager.runLock.isLocked)
        }
    }

    /**
     * Case 3:
     * 1. Automatic Reissuance requires the user to unlock the document key.
     * 2. Core delivers its terminal failure after acknowledging cancellation.
     *
     * Case 3 Expected Result:
     * The worker cancels authentication once, waits for the outcome and records the original ID.
     */
    @Test
    fun `Given automatic Reissuance needs authentication, When the worker runs, Then it cancels and waits for the failure`() {
        coroutineRule.runTest {
            // Given
            val cancelAuthentication: () -> Unit = mock()
            val outcome = CompletableDeferred<IssueDocumentsPartialState>()
            whenever(walletCoreDocumentsController.getAllIssuedDocuments())
                .thenReturn(listOf(dueDocument))
            whenever(walletCoreDocumentsController.isDocumentRevoked(dueDocument.id)).thenReturn(
                false
            )
            whenever(
                walletCoreDocumentsController.reIssueDocument(
                    documentId = dueDocument.id,
                    issuerId = "",
                    allowAuthorizationFallback = false,
                )
            ).thenReturn(
                flow {
                    emit(
                        IssueDocumentsPartialState.UserAuthRequired(
                            crypto = BiometricCrypto(cryptoObject = null),
                            resultHandler = DeviceAuthenticationResult(
                                onAuthenticationError = cancelAuthentication,
                            ),
                        )
                    )
                    emit(outcome.await())
                }
            )

            // When
            val work = coroutineRule.testScope.async { runWorker() }
            coroutineRule.testScope.runCurrent()
            val waitingForOutcome = ReIssuanceWorkManager.runLock.isLocked
            outcome.complete(IssueDocumentsPartialState.Failure(errorMessage = "Cancelled"))
            val result = work.await()

            // Then
            verify(cancelAuthentication).invoke()
            assertTrue(waitingForOutcome)
            assertFalse(ReIssuanceWorkManager.runLock.isLocked)
            assertEquals(ListenableWorker.Result.success(), result)
            verify(failedReIssuedDocumentDao).storeAll(
                listOf(FailedReIssuedDocument(dueDocument.id))
            )
        }
    }

    /**
     * A persisted replacement is still active after an ordinary failure or a process restart.
     */
    @Test
    fun `Given a due original with an active replacement, When automatic Reissuance runs, Then it skips without recording failure`() =
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getAllIssuedDocuments()).thenReturn(
                listOf(
                    dueDocument
                )
            )
            whenever(walletCoreDocumentsController.isDocumentRevoked(dueDocument.id)).thenReturn(
                false
            )
            whenever(deferredScheduleDao.hasPendingReplacement(dueDocument.id)).thenReturn(true)

            // When
            val result = runWorker()

            // Then
            assertEquals(ListenableWorker.Result.success(), result)
            verify(walletCoreDocumentsController, never()).reIssueDocument(
                any(),
                any(),
                any(),
                any()
            )
            verify(failedReIssuedDocumentDao, never()).storeAll(any())
        }

    /**
     * Case 5:
     * 1. Two originals are due for automatic Reissuance. Only the first has an active replacement.
     * 2. On the next run the first replacement has stopped polling and the second original has an
     *    active one.
     *
     * Case 5 Expected Result:
     * Each run submits only the original that is eligible at that time. Skips leave no failure
     * record.
     */
    @Test
    fun `Given two due originals, When their active replacements change between runs, Then the worker checks each original again`() =
        coroutineRule.runTest {
            // Given
            val otherOriginal = mock<IssuedDocument> {
                on { id } doReturn "other-original"
                on { credentialPolicy } doReturn CredentialPolicy.OnceOnly()
                on { getCredentials() } doReturn emptyList()
            }
            val originals = listOf(dueDocument, otherOriginal)
            whenever(walletCoreDocumentsController.getAllIssuedDocuments()).thenReturn(originals)
            whenever(deferredScheduleDao.hasPendingReplacement(dueDocument.id)).thenReturn(
                true,
                false
            )
            whenever(deferredScheduleDao.hasPendingReplacement(otherOriginal.id)).thenReturn(
                false,
                true
            )
            originals.forEach { original ->
                whenever(walletCoreDocumentsController.isDocumentRevoked(original.id)).thenReturn(
                    false
                )
                whenever(walletCoreDocumentsController.getDocumentById(original.id)).thenReturn(
                    original
                )
                val replacement =
                    mapOf("replacement-${original.id}" to mockedDeferredDocumentData.formatType)
                whenever(
                    walletCoreDocumentsController.reIssueDocument(
                        original.id,
                        "",
                        allowAuthorizationFallback = false
                    )
                )
                    .thenReturn(flowOf(IssueDocumentsPartialState.DeferredSuccess(replacement)))
            }

            // When
            val firstRun = runWorker()

            // Then
            assertEquals(ListenableWorker.Result.success(), firstRun)
            verify(walletCoreDocumentsController).reIssueDocument(
                otherOriginal.id,
                "",
                allowAuthorizationFallback = false
            )
            verify(walletCoreDocumentsController, never()).reIssueDocument(
                dueDocument.id,
                "",
                allowAuthorizationFallback = false
            )

            // When
            val nextRun = runWorker()

            // Then
            assertEquals(ListenableWorker.Result.success(), nextRun)
            verify(walletCoreDocumentsController).reIssueDocument(
                dueDocument.id,
                "",
                allowAuthorizationFallback = false
            )
            verify(walletCoreDocumentsController).reIssueDocument(
                otherOriginal.id,
                "",
                allowAuthorizationFallback = false
            )
            verify(failedReIssuedDocumentDao, never()).storeAll(any())
        }

    private suspend fun runWorker(): ListenableWorker.Result {
        val worker =
            TestListenableWorkerBuilder<ReIssuanceWorkManager>(RuntimeEnvironment.getApplication())
                .setWorkerFactory(object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker = ReIssuanceWorkManager(
                        appContext = appContext,
                        workerParams = workerParameters,
                        failedReIssuedDocumentDao = failedReIssuedDocumentDao,
                        deferredScheduleDao = deferredScheduleDao,
                        walletCoreDocumentsController = walletCoreDocumentsController,
                    )
                })
                .build()
        return worker.doWork()
    }

}