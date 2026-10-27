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

import eu.europa.ec.corelogic.model.RecordedRegistrationCheckDomain
import eu.europa.ec.corelogic.model.RegistrationFailureReasonDomain
import eu.europa.ec.corelogic.model.TransactionLogDomain
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.wallet.presentation.PresentationManager
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason
import eu.europa.ec.eudi.wallet.transactionLogging.DefaultTransactionLogManager
import eu.europa.ec.eudi.wallet.transactionLogging.model.MultiLangString
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionEntry
import eu.europa.ec.eudi.wallet.transactionLogging.producers.presentation.PresentationLogBuilder
import eu.europa.ec.eudi.wallet.transactionLogging.producers.presentation.PresentationLogger
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.storagelogic.dao.TransactionLogDao
import eu.europa.ec.storagelogic.model.TransactionLog
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.multipaz.presentment.CredentialSelection
import org.multipaz.securearea.KeyUnlockData
import java.util.Locale

class TestWalletCoreTransactionLogController {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private val transactionLogDao: TransactionLogDao = mock()

    private val resourceProvider: ResourceProvider = mock()

    private val controllerScope = CoroutineScope(coroutineRule.testDispatcher + SupervisorJob())

    private lateinit var controller: WalletCoreTransactionLogControllerImpl

    @Before
    fun before() {
        whenever(resourceProvider.getLocale()).thenReturn(Locale.ENGLISH)
        controller = WalletCoreTransactionLogControllerImpl(
            transactionLogDao = transactionLogDao,
            resourceProvider = resourceProvider,
            transactionDataTypes = emptyList(),
            dispatcher = coroutineRule.testDispatcher,
            coroutineScope = controllerScope,
        )
    }

    @After
    fun after() {
        controllerScope.cancel()
    }

    @Test
    fun `Given Wallet Core logged a new presentation, When its request arrives, Then the registration check is recorded for that entry`() =
        coroutineRule.runTest {
            val request = processedRequest(registration = notBound)
            val entry = loggedEntryFor(request)

            controller.log(entry)
            controller.onTransferEvent(TransferEvent.RequestReceived(request, mock()))
            coroutineRule.testScope.advanceUntilIdle()

            verify(transactionLogDao).recordRegistrationCheck(
                identifier = entry.transactionIdentifier,
                check = "NOT_VERIFIED",
                failureReason = "NOT_BOUND_TO_REQUESTER",
            )
        }

    @Test
    fun `Given a request without a registration evaluation, When it arrives, Then it is recorded as not evaluated`() =
        coroutineRule.runTest {
            val request = processedRequest(registration = null)
            val entry = loggedEntryFor(request)

            controller.log(entry)
            controller.onTransferEvent(TransferEvent.RequestReceived(request, mock()))
            coroutineRule.testScope.advanceUntilIdle()

            verify(transactionLogDao).recordRegistrationCheck(
                identifier = entry.transactionIdentifier,
                check = "NOT_EVALUATED",
                failureReason = null,
            )
        }

    @Test
    fun `Given the logged entry names another relying party, When the request arrives, Then nothing is recorded`() =
        coroutineRule.runTest {
            val request = processedRequest(registration = notBound)
            val entry = loggedEntryFor(request).copy(
                interactingPartyName = MultiLangString(lang = "en", content = "Another party"),
            )

            controller.log(entry)
            controller.onTransferEvent(TransferEvent.RequestReceived(request, mock()))
            coroutineRule.testScope.advanceUntilIdle()

            verify(transactionLogDao, never()).recordRegistrationCheck(any(), any(), anyOrNull())
        }

    @Test
    fun `Given only a later write of an entry already seen, When a request arrives, Then nothing is recorded for it`() =
        coroutineRule.runTest {
            val first = processedRequest(registration = notBound)
            val entry = loggedEntryFor(first)
            controller.log(entry)
            controller.onTransferEvent(TransferEvent.RequestReceived(first, mock()))

            controller.log(entry.copy(listOfClaimsPresented = emptyList()))
            controller.onTransferEvent(TransferEvent.RequestReceived(first, mock()))
            coroutineRule.testScope.advanceUntilIdle()

            verify(transactionLogDao).recordRegistrationCheck(any(), any(), anyOrNull())
        }

    @Test
    fun `Given a request that could not be processed, When it arrives, Then the pending entry is dropped`() =
        coroutineRule.runTest {
            val entry = loggedEntryFor(processedRequest(registration = notBound))
            controller.log(entry)

            controller.onTransferEvent(
                TransferEvent.RequestReceived(
                    RequestProcessor.ProcessedRequest.Failure(IllegalStateException("unreadable")),
                    mock(),
                )
            )
            controller.onTransferEvent(
                TransferEvent.RequestReceived(processedRequest(registration = notBound), mock())
            )
            coroutineRule.testScope.advanceUntilIdle()

            verify(transactionLogDao, never()).recordRegistrationCheck(any(), any(), anyOrNull())
        }

    @Test
    fun `Given Wallet Core's own presentation logger registered first, When a request arrives, Then the check lands on the entry it wrote`() =
        coroutineRule.runTest {
            val listeners = mutableListOf<TransferEvent.Listener>()
            val presentationManager: PresentationManager = mock {
                on { addTransferEventListener(any()) } doAnswer { invocation ->
                    listeners.add(invocation.getArgument(0))
                    invocation.mock as PresentationManager
                }
            }
            PresentationLogger(
                delegate = presentationManager,
                transactionLogManager = DefaultTransactionLogManager(controller),
            )
            listeners.add(controller)
            val event =
                TransferEvent.RequestReceived(processedRequest(registration = notBound), mock())

            listeners.forEach { listener -> listener.onTransferEvent(event) }
            coroutineRule.testScope.advanceUntilIdle()

            val stored = argumentCaptor<TransactionLog>()
            verify(transactionLogDao, atLeastOnce()).store(stored.capture())
            assertEquals(2, listeners.size)
            verify(transactionLogDao).recordRegistrationCheck(
                identifier = eq(stored.firstValue.identifier),
                check = eq("NOT_VERIFIED"),
                failureReason = eq("NOT_BOUND_TO_REQUESTER"),
            )
        }

    @Test
    fun `Given two new entries written before either request is seen, When the requests arrive, Then neither check is recorded`() =
        coroutineRule.runTest {
            val first = processedRequest(registration = null)
            val second = processedRequest(registration = notBound)
            controller.log(loggedEntryFor(first))
            controller.log(loggedEntryFor(second))
            controller.onTransferEvent(TransferEvent.RequestReceived(first, mock()))
            controller.onTransferEvent(TransferEvent.RequestReceived(second, mock()))

            val third = processedRequest(registration = notBound)
            val entry = loggedEntryFor(third)
            controller.log(entry)
            controller.onTransferEvent(TransferEvent.RequestReceived(third, mock()))
            coroutineRule.testScope.advanceUntilIdle()

            verify(transactionLogDao, times(1)).recordRegistrationCheck(any(), any(), anyOrNull())
            verify(transactionLogDao).recordRegistrationCheck(
                identifier = entry.transactionIdentifier,
                check = "NOT_VERIFIED",
                failureReason = "NOT_BOUND_TO_REQUESTER",
            )
        }

    @Test
    fun `Given a stored presentation with a recorded check, When the history is read, Then the entry carries that check`() =
        coroutineRule.runTest {
            controller.log(loggedEntryFor(processedRequest(registration = notBound)))
            coroutineRule.testScope.advanceUntilIdle()
            val stored = argumentCaptor<TransactionLog>()
            verify(transactionLogDao).store(stored.capture())
            whenever(transactionLogDao.retrieveAll()).thenReturn(
                listOf(
                    stored.firstValue.copy(
                        registrationCheck = "NOT_VERIFIED",
                        registrationFailureReason = "EXPIRED",
                    )
                )
            )

            val presentation =
                controller.getTransactionLogs().single() as TransactionLogDomain.Presentation

            assertEquals(
                RecordedRegistrationCheckDomain.NotVerified(reason = RegistrationFailureReasonDomain.EXPIRED),
                presentation.registrationCheck,
            )
        }

    private val notBound = RegistrationCertificateResult.Failed(
        reason = RegistrationFailureReason.NOT_BOUND_TO_REQUESTER,
    )

    /** A processed request whose registration evaluation is [registration]. */
    private fun processedRequest(
        registration: RegistrationCertificateResult?,
    ): RequestProcessor.ProcessedRequest.Success =
        object : RequestProcessor.ProcessedRequest.Success(
            presentmentData = mock(),
            requester = mock(),
            trustMetadata = null,
            wrpRegistration = registration,
        ) {
            override suspend fun generateResponse(
                selection: CredentialSelection,
                keyUnlockData: Map<String, KeyUnlockData>,
            ) = error("not used")
        }

    /** The entry Wallet Core writes when [request] arrives. */
    private fun loggedEntryFor(request: RequestProcessor.ProcessedRequest): TransactionEntry.Presentation {
        val builder = PresentationLogBuilder()
        return builder.withRelyingParty(builder.createEmptyPresentationLog(), request)
    }
}