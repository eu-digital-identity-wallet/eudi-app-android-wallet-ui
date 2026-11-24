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

package eu.europa.ec.corelogic.extension

import eu.europa.ec.corelogic.config.ReaderAuthenticationEnforcement
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceResponse
import eu.europa.ec.eudi.iso18013.transfer.response.device.ProcessedDeviceRequest
import eu.europa.ec.eudi.iso18013.transfer.toKotlinResult
import eu.europa.ec.eudi.wallet.trust.ReaderAuthenticationConfigBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.multipaz.cbor.Cbor
import org.multipaz.crypto.X509CertChain
import org.multipaz.presentment.CredentialQueryResult
import org.multipaz.presentment.CredentialSelection
import org.multipaz.request.Iso18013RequesterIdentity
import org.multipaz.request.Requester
import org.multipaz.request.RequesterIdentity
import org.multipaz.trustmanagement.TrustMetadata
import org.multipaz.util.Constants

class TestReaderAuthenticationExtensions {

    private val trustedReader = processedRequest(
        hasReaderAuthentication = true,
        isReaderTrusted = true,
    )

    private val untrustedReader = processedRequest(
        hasReaderAuthentication = true,
        isReaderTrusted = false,
    )

    private val readerWithoutAuthentication = processedRequest(
        hasReaderAuthentication = false,
        isReaderTrusted = false,
    )

    @Test
    fun `Given enforcement if present, When the reader authentication is not trusted, Then the request is refused`() {
        // When
        val refused = untrustedReader.isRefusedByReaderAuthentication(
            enforcement = ReaderAuthenticationEnforcement.ENFORCE_IF_PRESENT
        )

        // Then
        assertTrue(refused)
    }

    @Test
    fun `Given enforcement if present, When the reader sends no reader authentication, Then the request is not refused`() {
        // When
        val refused = readerWithoutAuthentication.isRefusedByReaderAuthentication(
            enforcement = ReaderAuthenticationEnforcement.ENFORCE_IF_PRESENT
        )

        // Then
        assertFalse(refused)
    }

    @Test
    fun `Given enforcement if present, When the reader authentication is trusted, Then the request is not refused`() {
        // When
        val refused = trustedReader.isRefusedByReaderAuthentication(
            enforcement = ReaderAuthenticationEnforcement.ENFORCE_IF_PRESENT
        )

        // Then
        assertFalse(refused)
    }

    @Test
    fun `Given authentication always required, When the reader sends no reader authentication, Then the request is refused`() {
        // When
        val refused = readerWithoutAuthentication.isRefusedByReaderAuthentication(
            enforcement = ReaderAuthenticationEnforcement.ALWAYS_REQUIRE
        )

        // Then
        assertTrue(refused)
    }

    @Test
    fun `Given authentication always required, When the reader authentication is not trusted, Then the request is refused`() {
        // When
        val refused = untrustedReader.isRefusedByReaderAuthentication(
            enforcement = ReaderAuthenticationEnforcement.ALWAYS_REQUIRE
        )

        // Then
        assertTrue(refused)
    }

    @Test
    fun `Given authentication always required, When the reader authentication is trusted, Then the request is not refused`() {
        // When
        val refused = trustedReader.isRefusedByReaderAuthentication(
            enforcement = ReaderAuthenticationEnforcement.ALWAYS_REQUIRE
        )

        // Then
        assertFalse(refused)
    }

    @Test
    fun `Given no enforcement, When the reader authentication is not trusted, Then the request is not refused`() {
        // When
        val refused = untrustedReader.isRefusedByReaderAuthentication(
            enforcement = ReaderAuthenticationEnforcement.DO_NOT_ENFORCE
        )

        // Then
        assertFalse(refused)
    }

    @Test
    fun `Given no enforcement, When the reader sends no reader authentication, Then the request is not refused`() {
        // When
        val refused = readerWithoutAuthentication.isRefusedByReaderAuthentication(
            enforcement = ReaderAuthenticationEnforcement.DO_NOT_ENFORCE
        )

        // Then
        assertFalse(refused)
    }

    @Test
    fun `Given enforcement if present, When the builder is configured, Then Wallet Core enforces if present`() {
        // Given
        val builder: ReaderAuthenticationConfigBuilder = mock()

        // When
        builder.setEnforcement(ReaderAuthenticationEnforcement.ENFORCE_IF_PRESENT)

        // Then
        verify(builder).enforceIfPresent()
        verifyNoMoreInteractions(builder)
    }

    @Test
    fun `Given authentication always required, When the builder is configured, Then Wallet Core always requires it`() {
        // Given
        val builder: ReaderAuthenticationConfigBuilder = mock()

        // When
        builder.setEnforcement(ReaderAuthenticationEnforcement.ALWAYS_REQUIRE)

        // Then
        verify(builder).alwaysRequire()
        verifyNoMoreInteractions(builder)
    }

    @Test
    fun `Given no enforcement, When the builder is configured, Then Wallet Core does not enforce`() {
        // Given
        val builder: ReaderAuthenticationConfigBuilder = mock()

        // When
        builder.setEnforcement(ReaderAuthenticationEnforcement.DO_NOT_ENFORCE)

        // Then
        verify(builder).doNotEnforce()
        verifyNoMoreInteractions(builder)
    }

    @Test
    fun `Given every enforcement and reader, When Wallet Core builds the mdoc response, Then it sends no data exactly when the request is refused`() =
        runBlocking {
            for (enforcement in ReaderAuthenticationEnforcement.entries) {
                for (hasReaderAuthentication in listOf(true, false)) {
                    for (isReaderTrusted in listOf(true, false)) {
                        // Given
                        val request = deviceRequest(
                            enforcement = enforcement,
                            hasReaderAuthentication = hasReaderAuthentication,
                            isReaderTrusted = isReaderTrusted,
                        )

                        // When
                        val response = request
                            .generateResponse(
                                selection = CredentialSelection(matches = emptyList())
                            )
                            .toKotlinResult()
                            .getOrThrow()
                            .response as DeviceResponse

                        val status = Cbor.decode(response.deviceResponseBytes)["status"].asNumber

                        // Then
                        assertEquals(
                            "$enforcement, authentication $hasReaderAuthentication, " +
                                    "trusted $isReaderTrusted",
                            request.isRefusedByReaderAuthentication(enforcement),
                            response.documentIds.isEmpty() &&
                                    status == Constants.DEVICE_RESPONSE_STATUS_GENERAL_ERROR,
                        )
                    }
                }
            }
        }

    /**
     * A request as Wallet Core's mdoc processor builds it, held to the Wallet Core policy that
     * [enforcement] configures, so that its own response decides what the reader receives.
     */
    private fun deviceRequest(
        enforcement: ReaderAuthenticationEnforcement,
        hasReaderAuthentication: Boolean,
        isReaderTrusted: Boolean,
    ): ProcessedDeviceRequest = ProcessedDeviceRequest(
        documentManager = mock(),
        sessionTranscript = byteArrayOf(),
        presentmentData = CredentialQueryResult(credentialSets = emptyList()),
        requester = Requester(requesterIdentities = identities(hasReaderAuthentication)),
        trustMetadata = if (isReaderTrusted) TrustMetadata(displayName = "Reader") else null,
        readerAuthPolicy = when (enforcement) {
            ReaderAuthenticationEnforcement.DO_NOT_ENFORCE -> ReaderAuthPolicy.DoNotEnforce
            ReaderAuthenticationEnforcement.ENFORCE_IF_PRESENT ->
                ReaderAuthPolicy.EnforceIfPresent(readerTrustStore = mock<ReaderTrustStore>())

            ReaderAuthenticationEnforcement.ALWAYS_REQUIRE ->
                ReaderAuthPolicy.AlwaysRequire(readerTrustStore = mock<ReaderTrustStore>())
        },
    )

    /** The requester identities of a request: one when it carried reader authentication. */
    private fun identities(hasReaderAuthentication: Boolean): List<RequesterIdentity> =
        if (hasReaderAuthentication) {
            listOf(Iso18013RequesterIdentity(certChain = mock<X509CertChain>()))
        } else {
            emptyList()
        }

    /**
     * A received request as Wallet Core hands it over: a requester identity when the request
     * carried reader authentication, and trust metadata only when that authentication is trusted.
     */
    private fun processedRequest(
        hasReaderAuthentication: Boolean,
        isReaderTrusted: Boolean,
    ): RequestProcessor.ProcessedRequest.Success {
        val metadata = if (isReaderTrusted) TrustMetadata(displayName = "Reader") else null

        return mock {
            on { requester } doReturn Requester(
                requesterIdentities = identities(hasReaderAuthentication)
            )
            on { trustMetadata } doReturn metadata
        }
    }
}
