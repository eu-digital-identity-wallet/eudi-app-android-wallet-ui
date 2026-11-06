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

package eu.europa.ec.corelogic.controller

import eu.europa.ec.authenticationlogic.controller.session.SessionController
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.eudi.wallet.dcapi.registration.DCAPIRegistration
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TestWalletCoreResetController {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var walletCoreDocumentsController: WalletCoreDocumentsController

    @Mock
    private lateinit var prefKeys: PrefKeys

    @Mock
    private lateinit var sessionController: SessionController

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var closeable: AutoCloseable

    private val steps = mutableListOf<String>()

    private val storedDocuments = mutableListOf<Document>()

    private val resetStatus = WalletResetStatus()

    private val registration = object : WalletCoreDCAPIRegistrationController {
        override val excludedDocumentIds: Set<String> = emptySet()
        override fun attach(createRegistration: () -> DCAPIRegistration) {}
        override suspend fun registerCredentials() {}
        override suspend fun unregisterCredentials() {}
        override suspend fun <T> clearAround(block: suspend () -> T): T {
            steps.add("hold registry")
            val result = block()
            steps.add("clear registry")
            return result
        }
    }

    private val pid: IssuedDocument = mock { on { id } doReturn "pid" }
    private val mdl: IssuedDocument = mock { on { id } doReturn "mdl" }

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        whenever(resourceProvider.genericErrorMessage()).thenReturn("error")
        whenever(walletCoreDocumentsController.getAllStoredDocuments()).then { storedDocuments.toList() }
        whenever(sessionController.lock()).then { steps.add("lock"); Unit }
        runBlocking {
            whenever(prefKeys.clearAllExceptSessionIdAndDbKey()).then { steps.add("clear prefs"); Unit }
        }
    }

    @After
    fun after() {
        closeable.close()
    }

    @Test
    fun `Given documents and a PID, When the wallet is reset, Then every step runs in order and no work is scheduled`() {
        coroutineRule.runTest {
            givenDocuments(listOf(pid, mdl), mainPid = pid)

            val result = buildController().resetWallet()

            assertEquals(ResetWalletPartialState.Success, result)
            assertEquals(
                listOf(
                    "stop work",
                    "hold registry",
                    "delete mdl",
                    "delete pid",
                    "clear registry",
                    "clear history",
                    "clear database",
                    "clear prefs",
                    "delete logs",
                    "delete cache",
                    "lock",
                ),
                steps
            )
        }
    }

    @Test
    fun `Given a reset, When it runs, Then the status says so until it ends`() {
        coroutineRule.runTest {
            givenDocuments(emptyList(), mainPid = null)
            val statusDuringReset = mutableListOf<Boolean>()

            buildController(
                clearDatabase = { statusDuringReset.add(resetStatus.isResetting.value) },
            ).resetWallet()

            assertEquals(listOf(true), statusDuringReset)
            assertFalse(resetStatus.isResetting.value)
        }
    }

    /**
     * An [Error] is not caught as a failed step, but the reset must still stop reporting itself
     * running, or every link and Digital Credential API request would be dropped until the process
     * ends.
     */
    @Test
    fun `Given a step that throws an Error, When the wallet is reset, Then the status no longer says it runs`() {
        coroutineRule.runTest {
            givenDocuments(emptyList(), mainPid = null)

            runCatching {
                buildController(clearDatabase = { throw StackOverflowError() }).resetWallet()
            }

            assertFalse(resetStatus.isResetting.value)
        }
    }

    @Test
    fun `Given a worker still working, When the wallet is reset, Then nothing is deleted and a failure returned`() {
        coroutineRule.runTest {
            givenDocuments(listOf(mdl), mainPid = null)

            val result = buildController(stopBackgroundWork = { steps.add("stop work"); false })
                .resetWallet()

            assertEquals(ResetWalletPartialState.Failure(errorMessage = "error"), result)
            assertEquals(listOf("stop work", "schedule work"), steps)
            assertFalse(resetStatus.isResetting.value)
        }
    }

    /**
     * The background work is scheduled once the reset no longer reports itself running, so that
     * nothing it starts sees a reset in progress.
     */
    @Test
    fun `Given a failed reset, When work is scheduled again, Then the reset has already ended`() {
        coroutineRule.runTest {
            givenDocuments(listOf(mdl), mainPid = null)
            val statusWhenScheduled = mutableListOf<Boolean>()

            buildController(
                stopBackgroundWork = { false },
                scheduleBackgroundWork = { statusWhenScheduled.add(resetStatus.isResetting.value) },
            ).resetWallet()

            assertEquals(listOf(false), statusWhenScheduled)
        }
    }

    @Test
    fun `Given no PID, When the wallet is reset, Then every document is still deleted`() {
        coroutineRule.runTest {
            givenDocuments(listOf(mdl), mainPid = null)

            val result = buildController().resetWallet()

            assertEquals(ResetWalletPartialState.Success, result)
            assertEquals(true, "delete mdl" in steps)
        }
    }

    @Test
    fun `Given a document that cannot be deleted, When the wallet is reset, Then nothing else is cleared and a failure returned`() {
        coroutineRule.runTest {
            givenDocuments(listOf(mdl), mainPid = null)
            whenever(walletCoreDocumentsController.deleteDocument("mdl")).thenReturn(
                flow { emit(DeleteDocumentPartialState.Failure(errorMessage = "storage error")) }
            )

            val result = buildController().resetWallet()

            assertEquals(ResetWalletPartialState.Failure(errorMessage = "error"), result)
            assertEquals(
                listOf("stop work", "hold registry", "clear registry", "schedule work"),
                steps
            )
        }
    }

    @Test
    fun `Given a document stored while the reset runs, When the wallet is reset, Then it is deleted on the next pass`() {
        coroutineRule.runTest {
            givenDocuments(listOf(mdl), mainPid = null)
            var lateDocumentStored = false
            whenever(walletCoreDocumentsController.deleteDocument("mdl")).thenReturn(
                flow {
                    steps.add("delete mdl")
                    storedDocuments.remove(mdl)
                    if (!lateDocumentStored) {
                        lateDocumentStored = true
                        storedDocuments.add(pid)
                    }
                    emit(DeleteDocumentPartialState.Success)
                }
            )

            val result = buildController().resetWallet()

            assertEquals(ResetWalletPartialState.Success, result)
            assertEquals(true, "delete pid" in steps)
        }
    }

    @Test
    fun `Given a step that fails, When the wallet is reset, Then a failure is returned and work is scheduled again`() {
        coroutineRule.runTest {
            givenDocuments(emptyList(), mainPid = null)

            val result =
                buildController(clearDatabase = { throw IllegalStateException("database error") })
                    .resetWallet()

            assertEquals(ResetWalletPartialState.Failure(errorMessage = "error"), result)
            assertEquals(
                listOf(
                    "stop work",
                    "hold registry",
                    "clear registry",
                    "clear history",
                    "schedule work"
                ), steps
            )
        }
    }

    @Test
    fun `Given a step that throws a cancellation, When the wallet is reset, Then a failure is returned`() {
        coroutineRule.runTest {
            givenDocuments(emptyList(), mainPid = null)

            val result =
                buildController(deleteCacheFiles = { throw CancellationException("cancelled") })
                    .resetWallet()

            assertEquals(ResetWalletPartialState.Failure(errorMessage = "error"), result)
            assertEquals("schedule work", steps.last())
        }
    }

    @Test
    fun `Given a reset that failed after the preferences were cleared, When it is retried, Then it completes`() {
        coroutineRule.runTest {
            givenDocuments(emptyList(), mainPid = null)
            var firstRun = true
            val controller = buildController(
                deleteLogFiles = {
                    if (firstRun) {
                        firstRun = false
                        throw IllegalStateException("log error")
                    }
                    steps.add("delete logs")
                },
            )

            val first = controller.resetWallet()
            steps.clear()
            val second = controller.resetWallet()

            assertEquals(ResetWalletPartialState.Failure(errorMessage = "error"), first)
            assertEquals(ResetWalletPartialState.Success, second)
            assertEquals("lock", steps.last())
        }
    }

    private fun givenDocuments(documents: List<Document>, mainPid: IssuedDocument?) {
        storedDocuments.clear()
        storedDocuments.addAll(documents)
        whenever(walletCoreDocumentsController.getMainPidDocument()).thenReturn(mainPid)
        listOf(pid, mdl).forEach { document ->
            whenever(walletCoreDocumentsController.deleteDocument(document.id)).thenReturn(
                flow {
                    steps.add("delete ${document.id}")
                    storedDocuments.remove(document)
                    emit(DeleteDocumentPartialState.Success)
                }
            )
        }
    }

    private fun buildController(
        stopBackgroundWork: suspend () -> Boolean = { steps.add("stop work"); true },
        clearTransactionLogs: suspend () -> Unit = { steps.add("clear history") },
        clearDatabase: () -> Unit = { steps.add("clear database") },
        deleteLogFiles: () -> Unit = { steps.add("delete logs") },
        deleteCacheFiles: () -> Unit = { steps.add("delete cache") },
        scheduleBackgroundWork: () -> Unit = { steps.add("schedule work") },
    ) = WalletCoreResetControllerImpl(
        walletCoreDocumentsController = walletCoreDocumentsController,
        walletCoreDCAPIRegistrationController = registration,
        prefKeys = prefKeys,
        sessionController = sessionController,
        resourceProvider = resourceProvider,
        resetStatus = resetStatus,
        stopBackgroundWork = stopBackgroundWork,
        scheduleBackgroundWork = scheduleBackgroundWork,
        clearTransactionLogs = clearTransactionLogs,
        clearDatabase = clearDatabase,
        deleteLogFiles = deleteLogFiles,
        deleteCacheFiles = deleteCacheFiles,
        dispatcher = coroutineRule.testDispatcher,
    )
}