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
import eu.europa.ec.authenticationlogic.controller.session.SessionController
import eu.europa.ec.corelogic.config.DeferredIssuanceConfig
import eu.europa.ec.corelogic.controller.IssueDeferredDocumentPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.controller.WalletDocumentOperationController
import eu.europa.ec.corelogic.controller.WalletDocumentOperationControllerImpl
import eu.europa.ec.corelogic.controller.WalletResetStatus
import eu.europa.ec.corelogic.util.CoreActions
import eu.europa.ec.corelogic.util.mockedDeferredConfig
import eu.europa.ec.corelogic.util.mockedDeferredDocumentData
import eu.europa.ec.corelogic.util.mockedDeferredSchedule
import eu.europa.ec.corelogic.util.mockedDocumentOperationState
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.storagelogic.dao.DeferredDocumentScheduleDao
import eu.europa.ec.storagelogic.model.DeferredDocumentSchedule
import eu.europa.ec.storagelogic.model.DeferredDocumentScheduleStatus
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
class TestDeferredIssuanceScheduler {
    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var scheduleDao: DeferredDocumentScheduleDao

    @Mock
    private lateinit var operationController: WalletDocumentOperationController

    @Mock
    private lateinit var documentsController: WalletCoreDocumentsController

    @Mock
    private lateinit var sessionController: SessionController

    @Mock
    private lateinit var resetStatus: WalletResetStatus

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var deferredDocument: DeferredDocument

    @Mock
    private lateinit var issuedDocument: IssuedDocument

    private val mockedSchedules = MutableStateFlow(emptyList<DeferredDocumentSchedule>())
    private val mockedOperationState = MutableStateFlow(mockedDocumentOperationState)
    private lateinit var scheduler: DeferredIssuanceScheduler
    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        whenever(context.packageName).thenReturn("eu.europa.ec.eudiwallet")
        whenever(scheduleDao.observeActive()).thenReturn(
            mockedSchedules.map { rows ->
                rows.filter { row -> row.status == DeferredDocumentScheduleStatus.ACTIVE }
            }
        )
        whenever(operationController.state).thenReturn(mockedOperationState)
        whenever(operationController.canPoll(anyOrNull())).thenReturn(true)
        whenever(sessionController.unlockedState).thenReturn(MutableStateFlow(true))
        whenever(resetStatus.isResetting).thenReturn(MutableStateFlow(false))
        whenever(deferredDocument.id).thenReturn(mockedDeferredSchedule.identifier)
        whenever(issuedDocument.id).thenReturn("original-document")
        whenever(documentsController.getDocumentById("original-document")).thenReturn(issuedDocument)
        whenever(documentsController.getDocumentById(mockedDeferredSchedule.identifier)).thenReturn(
            deferredDocument
        )
        whenever(documentsController.getAllDocuments()).thenReturn(emptyList())
        whenever(
            operationController.startOperation<Unit>(
                eq(false),
                any()
            )
        ).thenAnswer { invocation ->
            coroutineRule.testScope.backgroundScope.async {
                invocation.getArgument<suspend () -> Unit>(1).invoke()
            }
        }
        whenever(operationController.tryStartPolling(any(), anyOrNull(), any())).thenReturn(true)
        runBlocking {
            whenever(operationController.withDocumentLock<Any?>(any(), any()))
                .doSuspendableAnswer { invocation ->
                    invocation.getArgument<suspend () -> Any?>(1).invoke()
                }
        }
        scheduler = createScheduler(config = mockedDeferredConfig)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region start
    // Case 1:
    // 1. Local Core documents include a Deferred and an Issued document.
    // 2. Admission opens only after the first start call.
    //
    // Case 1 Expected Result:
    // Local recovery runs once, registering the Deferred fallback and clearing the Issued row.
    @Test
    fun `Given a closed gate, When Dashboard starts and later unlocks, Then recovery runs once after admission opens`() =
        coroutineRule.runTest {
            // Given
            whenever(operationController.canPoll(anyOrNull())).thenReturn(false)
            whenever(documentsController.getAllDocuments()).thenReturn(
                listOf(
                    deferredDocument,
                    issuedDocument
                )
            )

            // When
            scheduler.start()
            runCurrent()
            verify(documentsController, never()).getAllDocuments()
            whenever(operationController.canPoll(anyOrNull())).thenReturn(true)
            mockedOperationState.value = mockedDocumentOperationState.copy(isForeground = false)
            runCurrent()
            scheduler.start()
            runCurrent()

            // Then
            verify(documentsController).getAllDocuments()
            verify(scheduleDao).store(mockedDeferredSchedule.copy(nextAttemptAt = 5_000))
            verify(scheduleDao).delete("original-document")
            verify(operationController, never()).recordDeferredIssuance("original-document")
        }

    // Case 2:
    // 1. Two rows have different deadlines and a configured concurrency limit.
    //
    // Case 2 Expected Result:
    // Each deadline is scheduled independently and admission receives the configured limit.
    @Test
    fun `Given independent deadlines, When time advances, Then each document is submitted at its own deadline`() =
        coroutineRule.runTest {
            // Given
            scheduler =
                createScheduler(mockedDeferredConfig.copy(maxConcurrentDeferredRequests = 2))
            mockedSchedules.value = listOf(
                mockedDeferredSchedule.copy(identifier = "first", nextAttemptAt = 1_000),
                mockedDeferredSchedule.copy(identifier = "second", nextAttemptAt = 4_000),
            )

            // When
            scheduler.start()
            runCurrent()
            advanceTimeBy(999.milliseconds)
            runCurrent()

            // Then
            verify(operationController, never()).tryStartPolling(any(), anyOrNull(), any())

            // When
            advanceTimeBy(1.milliseconds)
            runCurrent()

            // Then
            verify(operationController).tryStartPolling(eq("first"), eq(2), any())
            verify(operationController, never()).tryStartPolling(eq("second"), anyOrNull(), any())

            // When
            mockedOperationState.value =
                mockedDocumentOperationState.copy(activeDocumentIds = setOf("first"))
            runCurrent()
            advanceTimeBy(3.seconds)
            runCurrent()

            // Then
            verify(operationController).tryStartPolling(eq("second"), eq(2), any())
        }

    // Case 3:
    // 1. A deadline passes while new polling is paused.
    //
    // Case 3 Expected Result:
    // One attempt is submitted on resume without replaying missed intervals.
    @Test
    fun `Given an overdue document, When polling resumes, Then one attempt is submitted with unlimited default concurrency`() =
        coroutineRule.runTest {
            // Given
            mockedSchedules.value = listOf(mockedDeferredSchedule.copy(nextAttemptAt = 1_000))
            whenever(operationController.canPoll(anyOrNull())).thenReturn(false)
            scheduler.start()
            runCurrent()

            // When
            advanceTimeBy(2.minutes)
            whenever(operationController.canPoll(anyOrNull())).thenReturn(true)
            mockedOperationState.value = mockedDocumentOperationState.copy(pendingOperations = 1)
            runCurrent()

            // Then
            verify(operationController).tryStartPolling(
                eq(mockedDeferredSchedule.identifier),
                isNull(),
                any()
            )
        }

    /**
     * Case 4:
     * 1. An issuer was rejected for one Deferred document. A second document is still polling.
     * 2. A fresh scheduler opens the same stored schedules after restart.
     *
     * Case 4 Expected Result:
     * The rejected document stays Failed. Only the other document is polled when its deadline passes.
     */
    @Test
    fun `Given a persisted trust rejection, When the Wallet restarts, Then the document stays Failed and only the other document is polled`() =
        coroutineRule.runTest {
            // Given
            val rejected = mockedDeferredSchedule.copy(
                status = DeferredDocumentScheduleStatus.ISSUER_NOT_TRUSTED,
            )
            val other =
                mockedDeferredSchedule.copy(identifier = "other-document", nextAttemptAt = 60_000)
            mockStoredSchedules(listOf(rejected, other))
            val otherDocument = mock<DeferredDocument> { on { id } doReturn other.identifier }
            whenever(documentsController.getAllDocuments()).thenReturn(
                listOf(
                    deferredDocument,
                    otherDocument
                )
            )
            whenever(documentsController.getDocumentById(other.identifier)).thenReturn(otherDocument)
            whenever(documentsController.issueDeferredDocument(other.identifier)).thenReturn(
                flowOf(
                    IssueDeferredDocumentPartialState.NotReady(
                        mockedDeferredDocumentData.copy(documentId = other.identifier),
                        1.days,
                    )
                )
            )
            val reset = WalletResetStatus()
            val operations = foregroundOperations(reset)
            val first = createScheduler(mockedDeferredConfig, operations, reset)

            // When
            first.start()
            runCurrent()

            // Then
            assertEquals(setOf(rejected.identifier), first.getFailedDocumentIds())

            // When
            operations.setForeground(false)
            val next = createScheduler(mockedDeferredConfig, foregroundOperations(reset), reset)
            next.start()
            runCurrent()
            advanceTimeBy(2.minutes)
            runCurrent()

            // Then
            assertEquals(rejected, scheduleDao.retrieve(rejected.identifier))
            assertEquals(setOf(rejected.identifier), next.getFailedDocumentIds())
            verify(documentsController, times(2)).getAllDocuments()
            verify(documentsController, never()).issueDeferredDocument(rejected.identifier)
            verify(documentsController).issueDeferredDocument(other.identifier)
            verify(documentsController, never()).deleteDocument(any())
            verify(scheduleDao, never()).delete(rejected.identifier)
        }

    /**
     * Case 5:
     * 1. A poll is in flight when the Wallet leaves the foreground.
     * 2. Its next deadline passes in the background, and the Wallet returns with the session locked.
     *
     * Case 5 Expected Result:
     * Save the response on arrival. Unlocking starts one overdue poll for this ID.
     */
    @Test
    fun `Given an in-flight poll, When its next deadline passes in the background, Then returning requires unlock to retry`() =
        coroutineRule.runTest {
            // Given
            val row = mockedDeferredSchedule
            mockStoredSchedules(listOf(row))
            val unlocked = MutableStateFlow(true)
            whenever(sessionController.unlockedState).thenReturn(unlocked)
            val response = CompletableDeferred<Unit>()
            whenever(documentsController.issueDeferredDocument(row.identifier)).thenReturn(flow {
                response.await()
                emit(
                    IssueDeferredDocumentPartialState.NotReady(
                        mockedDeferredDocumentData,
                        10.seconds
                    )
                )
            })
            val reset = WalletResetStatus()
            val operations = foregroundOperations(reset)
            whenever(sessionController.isUnlocked).thenAnswer { unlocked.value }
            createScheduler(mockedDeferredConfig, operations, reset).start()
            runCurrent()
            verify(documentsController).issueDeferredDocument(row.identifier)

            // When
            operations.setForeground(false)
            advanceTimeBy(12.seconds)
            response.complete(Unit)
            runCurrent()

            // Then
            assertEquals(
                row.copy(nextAttemptAt = 23_000, lastIntervalMillis = 10_000),
                scheduleDao.retrieve(row.identifier),
            )

            // When
            advanceTimeBy(1.minutes)
            runCurrent()

            // Then
            verify(documentsController).issueDeferredDocument(row.identifier)

            // When
            unlocked.value = false
            operations.setForeground(true)
            runCurrent()

            // Then
            verify(documentsController).issueDeferredDocument(row.identifier)

            // When
            unlocked.value = true
            runCurrent()

            // Then
            verify(documentsController, times(2)).issueDeferredDocument(row.identifier)
        }
    //endregion

    //region wallet reset
    // Case 1:
    // 1. Reset creates a new wallet lifecycle while the old scheduler still observes state.
    //
    // Case 1 Expected Result:
    // Recovery waits for Dashboard to start scheduling for the new wallet.
    @Test
    fun `Given a new wallet after reset, When its generation changes, Then recovery waits for Dashboard`() =
        coroutineRule.runTest {
            // Given
            val newWalletState = mockedDocumentOperationState.copy(walletGeneration = 1)

            // When
            scheduler.start()
            runCurrent()

            // Then
            verify(documentsController).getAllDocuments()

            // When
            mockedOperationState.value = newWalletState
            runCurrent()
            advanceTimeBy(1.minutes)
            runCurrent()

            // Then
            verify(documentsController).getAllDocuments()

            // When
            scheduler.start()
            runCurrent()

            // Then
            verify(documentsController, times(2)).getAllDocuments()
        }
    //endregion

    //region attempt
    // Case 1:
    // 1. A previously failing document returns NotReady with a new interval.
    //
    // Case 1 Expected Result:
    // Write the pre-call deadline first. Only the deadline calculated from the new interval gets the margin.
    @Test
    fun `Given NotReady, When an attempt completes, Then deadline writes surround the Core call`() =
        coroutineRule.runTest {
            // Given
            val schedule = mockedDeferredSchedule.copy(lastIntervalMillis = 50_000)
            val result =
                IssueDeferredDocumentPartialState.NotReady(mockedDeferredDocumentData, 120.seconds)
            whenever(
                operationController.setRetryableDeferredFailure(
                    schedule.identifier,
                    false
                )
            ).thenReturn(true)

            // When
            executeAttempt(schedule, result)

            // Then
            inOrder(scheduleDao, documentsController) {
                verify(scheduleDao).update(schedule.copy(nextAttemptAt = 50_000))
                verify(documentsController).issueDeferredDocument(schedule.identifier)
                verify(scheduleDao).update(
                    schedule.copy(
                        nextAttemptAt = 121_000,
                        lastIntervalMillis = 120_000,
                    )
                )
            }
            verifyBroadcast(CoreActions.DEFERRED_DOCUMENT_STATUS_CHANGED_ACTION)
        }

    // Case 2: A replacement can fail and retry even while its original is still in the Wallet.
    @Test
    fun `Given a failed replacement, When Core reports failure, Then another attempt is scheduled and the failure is visible`() =
        coroutineRule.runTest {
            // Given
            val schedule = mockedDeferredSchedule.copy(originalDocumentId = "original-document")
            whenever(
                operationController.setRetryableDeferredFailure(
                    schedule.identifier,
                    true
                )
            ).thenReturn(true)

            // When
            executeAttempt(schedule, IssueDeferredDocumentPartialState.Failed)

            // Then
            verify(scheduleDao).update(schedule.copy(nextAttemptAt = 30_000))
            verify(documentsController, never()).deleteDocument(any())
            verifyBroadcast(CoreActions.DEFERRED_DOCUMENT_STATUS_CHANGED_ACTION)
        }

    // Case 3: A local lookup error cannot establish that the document is missing or failed.
    @Test
    fun `Given an ambiguous Core lookup, When the document is due, Then the schedule is retained without a ping`() =
        coroutineRule.runTest {
            // Given
            val schedule = mockedDeferredSchedule
            whenever(documentsController.getDocumentById(schedule.identifier)).thenReturn(null)

            // When
            executeAttempt(schedule, IssueDeferredDocumentPartialState.Unavailable)

            // Then
            verify(documentsController, never()).issueDeferredDocument(any())
            verify(scheduleDao, never()).update(any())
            verify(scheduleDao, never()).delete(any())
            verify(context, never()).sendBroadcast(any())
        }

    // Case 4:
    // 1. Core issues a previously Deferred document.
    //
    // Case 4 Expected Result:
    // Delete the schedule before broadcasting the individual document ID.
    @Test
    fun `Given an Issued response, When an attempt completes, Then its schedule is cleared before notification`() =
        coroutineRule.runTest {
            // Given
            val schedule = mockedDeferredSchedule

            // When
            executeAttempt(
                schedule,
                IssueDeferredDocumentPartialState.Issued(mockedDeferredDocumentData)
            )

            // Then
            inOrder(scheduleDao, operationController, context) {
                verify(scheduleDao).delete(schedule.identifier)
                verify(operationController).recordDeferredIssuance(schedule.identifier)
                verify(context).sendBroadcast(any())
            }
            verifyBroadcast(CoreActions.DEFERRED_DOCUMENT_ISSUED_ACTION)
        }

    // Case 5:
    // 1. The request is blocked after saving the deadline and before the Core call.
    //
    // Case 5 Expected Result:
    // Restore the previous deadline and leave the existing indication alone.
    @Test
    fun `Given a paused attempt, When Core admission is refused, Then no failure is recorded`() =
        coroutineRule.runTest {
            // Given
            val schedule = mockedDeferredSchedule

            // When
            executeAttempt(schedule, IssueDeferredDocumentPartialState.Paused)

            // Then
            verify(scheduleDao).update(schedule)
            verify(context, never()).sendBroadcast(any())
        }

    /**
     * Case 6:
     * 1. The issuer reports expiration while the document is still Deferred in the wallet.
     *
     * Case 6 Expected Result:
     * The persisted expiry ends polling across restarts and the document remains available for Remove.
     */
    @Test
    fun `Given a Deferred document, When Core returns Expired, Then it is retained as terminal Failed`() =
        coroutineRule.runTest {
            // Given
            val documentId = mockedDeferredSchedule.identifier

            // When
            executeAttempt(
                mockedDeferredSchedule,
                IssueDeferredDocumentPartialState.Expired(documentId)
            )

            // Then
            verify(scheduleDao).update(mockedDeferredSchedule.copy(status = DeferredDocumentScheduleStatus.EXPIRED))
            verify(documentsController, never()).deleteDocument(any())
            verifyBroadcast(CoreActions.DEFERRED_DOCUMENT_STATUS_CHANGED_ACTION)
        }

    /**
     * Case 7:
     * 1. The wallet no longer trusts the issuer of a document that is still Deferred.
     *
     * Case 7 Expected Result:
     * Persist the terminal status before refreshing Documents. The user can still remove the document.
     */
    @Test
    fun `Given a Deferred document, When Core returns IssuerNotTrusted, Then its terminal failure is stored before Documents is notified`() =
        coroutineRule.runTest {
            // Given
            val documentId = mockedDeferredSchedule.identifier

            // When
            executeAttempt(
                mockedDeferredSchedule,
                IssueDeferredDocumentPartialState.IssuerNotTrusted(documentId)
            )

            // Then
            inOrder(scheduleDao, operationController, context) {
                verify(scheduleDao).update(
                    mockedDeferredSchedule.copy(status = DeferredDocumentScheduleStatus.ISSUER_NOT_TRUSTED)
                )
                verify(operationController).setRetryableDeferredFailure(documentId, failed = false)
                verify(context).sendBroadcast(any())
            }
            verify(scheduleDao, never()).delete(documentId)
            verify(documentsController, never()).deleteDocument(any())
            verifyBroadcast(CoreActions.DEFERRED_DOCUMENT_STATUS_CHANGED_ACTION)
        }
    //endregion

    //region successful Deferred issuance
    @Test
    fun `Given three Deferred replacements issued without an observer, When Documents opens, Then each ID is retained until seen or removed`() =
        coroutineRule.runTest {
            // Given
            val ids = listOf("R1", "R2", "R3")
            mockStoredSchedules(ids.map { id ->
                mockedDeferredSchedule.copy(
                    identifier = id,
                    originalDocumentId = "original-document"
                )
            })
            whenever(scheduleDao.delete(any())).thenAnswer { call ->
                mockedSchedules.value =
                    mockedSchedules.value.filterNot { it.identifier == call.getArgument<String>(0) }
                Unit
            }
            ids.forEach { id ->
                whenever(documentsController.getDocumentById(id)).thenReturn(deferredDocument)
                whenever(documentsController.issueDeferredDocument(id)).thenReturn(
                    flowOf(
                        IssueDeferredDocumentPartialState.Issued(
                            mockedDeferredDocumentData.copy(
                                documentId = id
                            )
                        )
                    )
                )
            }
            val reset = WalletResetStatus()
            val operations = foregroundOperations(reset)
            val scheduler = createScheduler(mockedDeferredConfig, operations, reset)

            // When
            scheduler.start()
            runCurrent()

            // Then
            assertEquals(ids.toSet(), scheduler.unseenDeferredIssuanceIds.value)
            val newProcess =
                createScheduler(mockedDeferredConfig, foregroundOperations(reset), reset)
            assertTrue(newProcess.unseenDeferredIssuanceIds.value.isEmpty())

            // When
            scheduler.markDeferredIssuanceSeen("R1")
            operations.recordRemoval("R2")

            // Then
            assertEquals(setOf("R3"), scheduler.unseenDeferredIssuanceIds.value)

            // When
            operations.onWalletReset()

            // Then
            assertTrue(scheduler.unseenDeferredIssuanceIds.value.isEmpty())
        }
    //endregion

    //region failure indications and restart
    @Test
    fun `Given a retryable failure and an expiry, When a new process opens, Then only expiry stays red until the next result`() =
        coroutineRule.runTest {
            // Given
            val documentId = mockedDeferredSchedule.identifier
            val expired = mockedDeferredSchedule.copy(
                identifier = "expired",
                status = DeferredDocumentScheduleStatus.EXPIRED
            )
            val issuerInterval = 20.seconds
            val firstFailureAt = issuerInterval.inWholeMilliseconds + 1_000
            mockStoredSchedules(listOf(mockedDeferredSchedule, expired))
            whenever(documentsController.issueDeferredDocument(documentId)).thenReturn(
                flowOf(
                    IssueDeferredDocumentPartialState.NotReady(
                        mockedDeferredDocumentData,
                        issuerInterval
                    )
                ),
                flowOf(IssueDeferredDocumentPartialState.Failed),
                flowOf(IssueDeferredDocumentPartialState.Failed),
                flowOf(
                    IssueDeferredDocumentPartialState.NotReady(
                        mockedDeferredDocumentData,
                        48.seconds
                    )
                ),
            )
            val reset = WalletResetStatus()
            val firstOperations = foregroundOperations(reset)
            val first = createScheduler(mockedDeferredConfig, firstOperations, reset)

            // When
            first.start()
            runCurrent()

            // Then
            val waiting = mockedDeferredSchedule.copy(
                nextAttemptAt = firstFailureAt,
                lastIntervalMillis = issuerInterval.inWholeMilliseconds,
            )
            assertEquals(waiting, scheduleDao.retrieve(documentId))

            // When
            advanceTimeBy(firstFailureAt.milliseconds)
            runCurrent()

            // Then
            assertEquals(
                waiting.copy(nextAttemptAt = firstFailureAt + 30_000),
                scheduleDao.retrieve(documentId),
            )
            assertEquals(setOf(documentId, expired.identifier), first.getFailedDocumentIds())

            // When
            firstOperations.setForeground(false)
            advanceTimeBy(10.seconds)
            val next = createScheduler(mockedDeferredConfig, foregroundOperations(reset), reset)
            next.start()
            runCurrent()
            advanceTimeBy(19_999.milliseconds)
            runCurrent()

            // Then
            assertEquals(setOf(expired.identifier), next.getFailedDocumentIds())
            verify(documentsController, times(2)).issueDeferredDocument(documentId)

            // When
            advanceTimeBy(1.milliseconds)
            runCurrent()

            // Then
            assertEquals(setOf(documentId, expired.identifier), next.getFailedDocumentIds())
            verify(documentsController, times(3)).issueDeferredDocument(documentId)

            // When
            advanceTimeBy(30.seconds)
            runCurrent()

            // Then
            assertEquals(setOf(expired.identifier), next.getFailedDocumentIds())
            assertEquals(
                mockedDeferredSchedule.copy(
                    nextAttemptAt = coroutineRule.testScope.testScheduler.currentTime + 49_000,
                    lastIntervalMillis = 48_000,
                ),
                scheduleDao.retrieve(documentId),
            )
            verify(documentsController, never()).issueDeferredDocument(expired.identifier)
        }

    /**
     * Case 2:
     * 1. Two documents fail. On retry, one receives NotReady without an interval and the other fails again.
     *
     * Case 2 Expected Result:
     * Only the recovering ID returns to Pending and reuses its raw issuer interval.
     * The repeated failure updates its deadline without another status notification.
     */
    @Test
    fun `Given two failures, When only one document recovers, Then their indications and deadlines stay independent`() =
        coroutineRule.runTest {
            // Given
            val recovering = mockedDeferredSchedule.copy(lastIntervalMillis = 20_000)
            val stillFailing = mockedDeferredSchedule.copy(identifier = "other-document")
            mockStoredSchedules(listOf(recovering, stillFailing))
            val otherDocument =
                mock<DeferredDocument> { on { id } doReturn stillFailing.identifier }
            whenever(documentsController.getDocumentById(stillFailing.identifier)).thenReturn(
                otherDocument
            )
            whenever(documentsController.issueDeferredDocument(stillFailing.identifier))
                .thenReturn(flowOf(IssueDeferredDocumentPartialState.Failed))
            whenever(documentsController.issueDeferredDocument(recovering.identifier)).thenReturn(
                flowOf(IssueDeferredDocumentPartialState.Failed),
                flowOf(
                    IssueDeferredDocumentPartialState.NotReady(
                        mockedDeferredDocumentData,
                        interval = null
                    )
                ),
            )
            val reset = WalletResetStatus()
            val scheduler =
                createScheduler(mockedDeferredConfig, foregroundOperations(reset), reset)

            // When
            scheduler.start()
            runCurrent()

            // Then
            assertEquals(
                setOf(recovering.identifier, stillFailing.identifier),
                scheduler.getFailedDocumentIds()
            )

            // When
            advanceTimeBy(30.seconds)
            runCurrent()

            // Then
            assertEquals(setOf(stillFailing.identifier), scheduler.getFailedDocumentIds())
            assertEquals(
                recovering.copy(nextAttemptAt = 50_000),
                scheduleDao.retrieve(recovering.identifier)
            )
            assertEquals(
                stillFailing.copy(nextAttemptAt = 60_000),
                scheduleDao.retrieve(stillFailing.identifier)
            )
            val changes = argumentCaptor<Intent>()
            verify(context, times(3)).sendBroadcast(changes.capture())
            assertTrue(changes.allValues.all { intent ->
                intent.action == CoreActions.DEFERRED_DOCUMENT_STATUS_CHANGED_ACTION
            })
            val ids = changes.allValues.map { intent ->
                intent.getStringExtra(CoreActions.DEFERRED_DOCUMENT_ID_EXTRA)
            }
            assertEquals(2, ids.count { id -> id == recovering.identifier })
            assertEquals(1, ids.count { id -> id == stillFailing.identifier })
        }
    //endregion

    //region per-document deadlines
    @Test
    fun `Given two documents due at five seconds, When their issuers return different intervals, Then each has its own deadline`() =
        coroutineRule.runTest {
            // Given
            val first = mockedDeferredSchedule.copy(nextAttemptAt = 5_000)
            val second = first.copy(identifier = "second")
            mockStoredSchedules(listOf(first, second))
            whenever(documentsController.getDocumentById(second.identifier)).thenReturn(
                deferredDocument
            )
            listOf(first to 10.seconds, second to 48.seconds).forEach { (row, interval) ->
                val data = mockedDeferredDocumentData.copy(documentId = row.identifier)
                whenever(documentsController.issueDeferredDocument(row.identifier))
                    .thenReturn(flowOf(IssueDeferredDocumentPartialState.NotReady(data, interval)))
            }
            val reset = WalletResetStatus()
            createScheduler(mockedDeferredConfig, foregroundOperations(reset), reset).start()

            // When
            advanceTimeBy(5.seconds)
            runCurrent()

            // Then
            assertEquals(
                first.copy(nextAttemptAt = 16_000, lastIntervalMillis = 10_000),
                scheduleDao.retrieve(first.identifier),
            )
            assertEquals(
                second.copy(nextAttemptAt = 54_000, lastIntervalMillis = 48_000),
                scheduleDao.retrieve(second.identifier),
            )

            // When
            advanceTimeBy(10_999.milliseconds)
            runCurrent()

            // Then
            verify(documentsController).issueDeferredDocument(first.identifier)

            // When
            advanceTimeBy(1.milliseconds)
            runCurrent()

            // Then
            verify(documentsController, times(2)).issueDeferredDocument(first.identifier)
            verify(documentsController).issueDeferredDocument(second.identifier)
        }

    @Test
    fun `Given a transient lookup failure, When local backoff expires without a Room change, Then the scheduler tries again`() =
        coroutineRule.runTest {
            // Given
            val documentId = mockedDeferredSchedule.identifier
            mockStoredSchedules(listOf(mockedDeferredSchedule))
            whenever(documentsController.getDocumentById(documentId)).thenReturn(
                null,
                deferredDocument
            )
            whenever(documentsController.issueDeferredDocument(documentId))
                .thenReturn(flowOf(IssueDeferredDocumentPartialState.Failed))
            val reset = WalletResetStatus()
            val scheduler =
                createScheduler(mockedDeferredConfig, foregroundOperations(reset), reset)
            scheduler.start()
            runCurrent()

            // When
            advanceTimeBy(29_999.milliseconds)
            runCurrent()

            // Then
            assertTrue(scheduler.getFailedDocumentIds().isEmpty())
            verify(documentsController, never()).issueDeferredDocument(documentId)

            // When
            advanceTimeBy(1.milliseconds)
            runCurrent()

            // Then
            assertEquals(setOf(documentId), scheduler.getFailedDocumentIds())
            verify(documentsController).issueDeferredDocument(documentId)
        }

    /**
     * Case 3:
     * 1. A due document has a long previous interval. The final local lookup reports Unavailable.
     *
     * Case 3 Expected Result:
     * Restore the due row, keep its current indication and retry after the local backoff.
     */
    @Test
    fun `Given an unavailable submission, When local backoff expires, Then the same document can be polled again`() =
        coroutineRule.runTest {
            // Given
            val row = mockedDeferredSchedule.copy(lastIntervalMillis = 1.days.inWholeMilliseconds)
            mockStoredSchedules(listOf(row))
            whenever(documentsController.issueDeferredDocument(row.identifier)).thenReturn(
                flowOf(IssueDeferredDocumentPartialState.Unavailable),
                flowOf(
                    IssueDeferredDocumentPartialState.NotReady(
                        mockedDeferredDocumentData,
                        10.seconds
                    )
                ),
            )
            val reset = WalletResetStatus()
            val scheduler =
                createScheduler(mockedDeferredConfig, foregroundOperations(reset), reset)

            // When
            scheduler.start()
            runCurrent()
            advanceTimeBy(29_999.milliseconds)
            runCurrent()

            // Then
            assertEquals(row, scheduleDao.retrieve(row.identifier))
            assertTrue(scheduler.getFailedDocumentIds().isEmpty())
            verify(context, never()).sendBroadcast(any())
            verify(documentsController).issueDeferredDocument(row.identifier)

            // When
            advanceTimeBy(1.milliseconds)
            runCurrent()

            // Then
            verify(documentsController, times(2)).issueDeferredDocument(row.identifier)
            assertEquals(
                row.copy(nextAttemptAt = 41_000, lastIntervalMillis = 10_000),
                scheduleDao.retrieve(row.identifier),
            )
        }
    //endregion

    //region concurrent deadlines
    @Test
    fun `Given eight overdue documents and three slots, When scheduling starts, Then the oldest three are submitted first`() =
        coroutineRule.runTest { verifyConcurrentAttempts(limit = 3) }

    @Test
    fun `Given eight overdue documents and no limit, When scheduling starts, Then each is submitted once`() =
        coroutineRule.runTest { verifyConcurrentAttempts(limit = null) }
    //endregion

    //region helper functions
    private fun foregroundOperations(reset: WalletResetStatus): WalletDocumentOperationController {
        whenever(sessionController.isUnlocked).thenReturn(true)
        return WalletDocumentOperationControllerImpl(
            sessionController = sessionController,
            resetStatus = reset,
            coroutineScope = coroutineRule.testScope.backgroundScope,
        ).apply { setForeground(true) }
    }

    /** The mocked DAO shares persisted deadlines between schedulers with separate process-local state. */
    private suspend fun mockStoredSchedules(rows: List<DeferredDocumentSchedule>) {
        mockedSchedules.value = rows
        whenever(scheduleDao.retrieve(any())).thenAnswer { call ->
            mockedSchedules.value.find { row -> row.identifier == call.getArgument<String>(0) }
        }
        whenever(scheduleDao.update(any())).thenAnswer { call ->
            val updated = call.getArgument<DeferredDocumentSchedule>(0)
            mockedSchedules.value = mockedSchedules.value.map { row ->
                if (row.identifier == updated.identifier) updated else row
            }
            Unit
        }
        whenever(scheduleDao.getTerminalDocumentIds()).thenAnswer {
            mockedSchedules.value.filter { row -> row.status != DeferredDocumentScheduleStatus.ACTIVE }
                .map { row -> row.identifier }
        }
    }

    private suspend fun verifyConcurrentAttempts(limit: Int?) {
        // Given
        val rows = (1..8).map { index ->
            mockedDeferredSchedule.copy(identifier = "document-$index", nextAttemptAt = index - 9L)
        }
        mockStoredSchedules(rows.reversed())
        whenever(documentsController.getDocumentById(any())).thenReturn(deferredDocument)
        val submitted = mutableListOf<String>()
        val response = CompletableDeferred<Unit>()
        whenever(documentsController.issueDeferredDocument(any())).thenAnswer { call ->
            val documentId = call.getArgument<String>(0)
            flow {
                submitted.add(documentId)
                response.await()
                emit(
                    IssueDeferredDocumentPartialState.NotReady(
                        mockedDeferredDocumentData.copy(
                            documentId = documentId
                        ), 10.seconds
                    )
                )
            }
        }
        val reset = WalletResetStatus()
        val operations = foregroundOperations(reset)
        val config = mockedDeferredConfig.copy(maxConcurrentDeferredRequests = limit)

        // When
        createScheduler(config, operations, reset).start()
        runCurrent()

        // Then
        val ids = rows.map { row -> row.identifier }
        assertEquals(ids.take(limit ?: ids.size), submitted)

        // When
        operations.setForeground(false)
        response.complete(Unit)
        runCurrent()

        // Then
        assertEquals(ids.take(limit ?: ids.size), submitted)
        assertTrue(submitted.all { id -> scheduleDao.retrieve(id)?.nextAttemptAt == 11_000L })

        // When
        operations.setForeground(true)
        runCurrent()

        // Then
        assertEquals(ids, submitted)
    }

    private fun createScheduler(
        config: DeferredIssuanceConfig,
        operations: WalletDocumentOperationController = operationController,
        walletResetStatus: WalletResetStatus = resetStatus,
    ): DeferredIssuanceScheduler =
        DeferredIssuanceSchedulerImpl(
            scheduleDao = scheduleDao,
            walletDocumentOperationController = operations,
            sessionController = sessionController,
            resetStatus = walletResetStatus,
            config = config,
            getDocumentsController = { documentsController },
            context = context,
            now = { coroutineRule.testScope.testScheduler.currentTime },
            coroutineScope = coroutineRule.testScope.backgroundScope,
        )

    private suspend fun executeAttempt(
        schedule: DeferredDocumentSchedule,
        result: IssueDeferredDocumentPartialState,
    ) {
        whenever(scheduleDao.retrieve(schedule.identifier)).thenReturn(schedule)
        whenever(documentsController.issueDeferredDocument(schedule.identifier)).thenReturn(
            flowOf(
                result
            )
        )
        lateinit var action: suspend () -> Unit
        whenever(operationController.tryStartPolling(eq(schedule.identifier), isNull(), any()))
            .thenAnswer { invocation ->
                action = invocation.getArgument(2)
                true
            }
        mockedSchedules.value = listOf(schedule)
        scheduler.start()
        runCurrent()
        verify(operationController).tryStartPolling(eq(schedule.identifier), isNull(), any())
        action()
    }

    private fun verifyBroadcast(action: String) {
        val intent = argumentCaptor<Intent>()
        verify(context).sendBroadcast(intent.capture())
        assertEquals(action, intent.firstValue.action)
        assertEquals(context.packageName, intent.firstValue.`package`)
        assertEquals(
            mockedDeferredSchedule.identifier,
            intent.firstValue.getStringExtra(CoreActions.DEFERRED_DOCUMENT_ID_EXTRA)
        )
    }

    private fun runCurrent() = coroutineRule.testScope.testScheduler.runCurrent()
    private fun advanceTimeBy(duration: Duration) =
        coroutineRule.testScope.testScheduler.advanceTimeBy(duration)
    //endregion
}