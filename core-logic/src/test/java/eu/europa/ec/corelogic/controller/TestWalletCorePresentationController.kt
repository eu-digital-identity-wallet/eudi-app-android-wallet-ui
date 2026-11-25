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

import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.corelogic.config.ReaderAuthenticationEnforcement
import eu.europa.ec.corelogic.config.WalletCoreConfig
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.iso18013.transfer.response.ResponseResult
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.multipaz.crypto.X509CertChain
import org.multipaz.request.Iso18013RequesterIdentity
import org.multipaz.request.Requester
import org.multipaz.request.RequesterIdentity
import org.multipaz.trustmanagement.TrustMetadata
import java.util.Locale

/**
 * The controller's [WalletCorePresentationController.events] run Wallet Core's listener on
 * `Dispatchers.IO` (through `safeAsync`), so these tests use real dispatchers and wait for each
 * state with a timeout, as `TestWalletCoreDocumentsController` waits for its listener.
 */
class TestWalletCorePresentationController {

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var prefKeys: PrefKeys

    @Mock
    private lateinit var walletCoreConfig: WalletCoreConfig

    @Mock
    private lateinit var eudiWallet: EudiWallet

    private lateinit var controller: WalletCorePresentationController

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        runBlocking {
            whenever(prefKeys.getSessionId()).thenReturn("session")
        }
        whenever(resourceProvider.genericErrorMessage()).thenReturn("error")
        whenever(resourceProvider.getLocale()).thenReturn(Locale.US)
        whenever(walletCoreConfig.readerAuthenticationEnforcement)
            .thenReturn(ReaderAuthenticationEnforcement.ENFORCE_IF_PRESENT)
        controller = WalletCorePresentationControllerImpl(
            resourceProvider = resourceProvider,
            prefKeys = prefKeys,
            walletCoreConfig = walletCoreConfig,
            dispatcher = Dispatchers.Default,
            walletCore = eudiWallet,
        )
        controller.setConfig(PresentationControllerConfig.Ble(initiator = "initiator"))
    }

    @After
    fun after() {
        controller.stopPresentation()
        closeable.close()
    }

    @Test
    fun `Given a reader whose authentication is not trusted, When its request arrives, Then the presentation is blocked before consent`() {
        // When
        val state = receiveEach(processedRequest(hasReaderAuthentication = true)).single()

        // Then
        assertEquals(TransferEventPartialState.VerifierNotTrusted, state)
        assertNull(controller.verifierName)
        assertEquals(false, controller.verifierIsFullyVerified)
    }

    @Test
    fun `Given a reader that sends no reader authentication, When its request arrives, Then it reaches consent`() {
        // When
        val state = receiveEach(processedRequest(hasReaderAuthentication = false)).single()

        // Then
        assertTrue("state was $state", state is TransferEventPartialState.RequestReceived)
    }

    @Test
    fun `Given reader authentication always required, When a reader that sends none makes a request, Then the presentation is blocked before consent`() {
        // Given
        whenever(walletCoreConfig.readerAuthenticationEnforcement)
            .thenReturn(ReaderAuthenticationEnforcement.ALWAYS_REQUIRE)

        // When
        val state = receiveEach(processedRequest(hasReaderAuthentication = false)).single()

        // Then
        assertEquals(TransferEventPartialState.VerifierNotTrusted, state)
    }

    @Test
    fun `Given a trusted request was received, When a refused request follows in the session, Then the first one cannot be sent`() {
        // Given
        val states = receiveEach(
            processedRequest(hasReaderAuthentication = true, isReaderTrusted = true),
            processedRequest(hasReaderAuthentication = true),
        )
        controller.updateRequestedDocuments(emptyList())

        // When
        val sent = runBlocking { controller.sendRequestedDocuments() }

        // Then
        assertTrue("states were $states", states[0] is TransferEventPartialState.RequestReceived)
        assertEquals(TransferEventPartialState.VerifierNotTrusted, states[1])
        assertNull(controller.verifierName)
        assertTrue("sent was $sent", sent is SendRequestedDocumentsPartialState.Failure)
        verify(eudiWallet, never()).sendResponse(any())
    }

    @Test
    fun `Given a refused request, When an allowed request follows in the session, Then the allowed one is sent`() {
        // Given
        val states = receiveEach(
            processedRequest(hasReaderAuthentication = true),
            processedRequest(hasReaderAuthentication = true, isReaderTrusted = true),
        )
        controller.updateRequestedDocuments(emptyList())

        // When
        val sent = runBlocking { controller.sendRequestedDocuments() }

        // Then
        assertEquals(TransferEventPartialState.VerifierNotTrusted, states[0])
        assertTrue("states were $states", states[1] is TransferEventPartialState.RequestReceived)
        assertEquals(SendRequestedDocumentsPartialState.RequestSent, sent)
        verify(eudiWallet).sendResponse(any())
    }

    /**
     * Starts the presentation events and hands each of [requests], in order, to the listener the
     * controller registered with Wallet Core, returning the state each one produced. Call it once
     * per test: the events replay their last two states to a new collector.
     */
    private fun receiveEach(
        vararg requests: RequestProcessor.ProcessedRequest.Success,
    ): List<TransferEventPartialState> = runBlocking {
        val states = Channel<TransferEventPartialState>(capacity = Channel.UNLIMITED)
        val collector = launch(Dispatchers.Default) {
            controller.events.collect { states.send(it) }
        }
        val listener = argumentCaptor<TransferEvent.Listener>()
        verify(
            eudiWallet,
            timeout(EVENT_TIMEOUT_MILLIS)
        ).addTransferEventListener(listener.capture())

        val received = requests.map { request ->
            listener.firstValue.onTransferEvent(
                TransferEvent.RequestReceived(
                    processedRequest = request,
                    request = mock<Request>()
                )
            )
            withTimeout(EVENT_TIMEOUT_MILLIS) { states.receive() }
        }
        collector.cancel()

        received
    }

    /**
     * An mdoc reader's request that offers nothing to share: it names a requester identity only
     * when it carried reader authentication, and has trust metadata only when that is trusted.
     * Its response, should it be sent, is an empty success.
     */
    private fun processedRequest(
        hasReaderAuthentication: Boolean,
        isReaderTrusted: Boolean = false,
    ): RequestProcessor.ProcessedRequest.Success {
        val identities: List<RequesterIdentity> =
            if (hasReaderAuthentication) {
                listOf(Iso18013RequesterIdentity(certChain = mock<X509CertChain>()))
            } else {
                emptyList()
            }
        val metadata = if (isReaderTrusted) TrustMetadata(displayName = "Reader") else null

        val request: RequestProcessor.ProcessedRequest.Success = mock {
            on { requester } doReturn Requester(requesterIdentities = identities)
            on { trustMetadata } doReturn metadata
            on { presentmentSelections } doReturn emptyList()
            on { generateResponse(any(), any()) } doReturn
                    ResponseResult.Success(response = mock<Response>())
        }
        whenever(request.getOrThrow()).thenReturn(request)

        return request
    }

    private companion object {
        const val EVENT_TIMEOUT_MILLIS = 10_000L
    }
}
