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

import eu.europa.ec.corelogic.model.DocumentChecksumDomain
import eu.europa.ec.corelogic.model.PresentationTransactionDataDomain
import eu.europa.ec.corelogic.model.QesDocumentDigestDomain
import eu.europa.ec.corelogic.model.QesSignatureRequestDomain
import eu.europa.ec.corelogic.model.SigningAttributeDomain
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionalData
import eu.europa.ec.eudi.wallet.transfer.openId4vp.TransactionDataType
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.AccessControlMethod
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.Attribute
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.Checksum
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.DocumentDigest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.QesApprovalRequest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.QesRequest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.SignatureRequest
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.multipaz.crypto.Algorithm
import org.multipaz.documenttype.TransactionType
import org.multipaz.presentment.TransactionData
import org.multipaz.presentment.TransactionProtocol

class TestPresentationTransactionDataExtensions {

    @Mock
    private lateinit var transactionType: TransactionType<Any>

    private lateinit var closeable: AutoCloseable

    private val mockedDisplayName = "SDK transaction display name"
    private val mockedQueryIds = listOf("query_1", "query_0", "query_1")
    private val mockedSigningCredentialId = "signing-credential"
    private val mockedDocumentLabel = "  Contract.pdf  "
    private val mockedDocumentHash = "+/8="
    private val mockedDocumentHashAlgorithmOid = "2.16.840.1.101.3.4.2.2"
    private val mockedChecksumValue = "AQIDBA=="
    private val mockedChecksumAlgorithmOid = "2.16.840.1.101.3.4.2.1"
    private val mockedDocumentHref = "https://documents.example/contract.pdf?ref=%2F"
    private val mockedResponseUri = "https://verifier.example/response?request=%2B"
    private val mockedOneTimePassword = "000123"
    private val mockedQualifier = "eu_eidas_qeseal"
    private val mockedSignatureFormat = "P"
    private val mockedConformanceLevel = "AdES-B-LT"
    private val mockedSigningAlgorithmOid = "1.2.840.113549.1.1.11"
    private val mockedSignatureCount = 4
    private val mockedRawBytes = ByteString(byteArrayOf(1, 2, 3))
    private val mockedAttributes = listOf(
        Attribute(name = "location", value = "  Athens\n"),
        Attribute(name = "reason", value = null),
        Attribute(name = "description", value = ""),
    )
    private val mockedAttributesDomain = listOf(
        SigningAttributeDomain(name = "location", value = "  Athens\n"),
        SigningAttributeDomain(name = "reason", value = null),
        SigningAttributeDomain(name = "description", value = ""),
    )
    private val mockedChecksum = Checksum(
        value = mockedChecksumValue,
        algorithmOid = mockedChecksumAlgorithmOid,
    )
    private val mockedChecksumDomain = DocumentChecksumDomain(
        value = mockedChecksumValue,
        algorithmOid = mockedChecksumAlgorithmOid,
    )
    private val mockedAccess = AccessControlMethod(
        accessMode = AccessControlMethod.ACCESS_MODE_ONE_TIME_PASSWORD,
        oneTimePassword = mockedOneTimePassword,
    )
    private val mockedDigest = DocumentDigest(
        label = mockedDocumentLabel,
        hash = mockedDocumentHash,
        hashType = DocumentDigest.HASH_TYPE_DTBSR,
        signedProperties = mockedAttributes,
        href = mockedDocumentHref,
        checksum = mockedChecksum,
        access = mockedAccess,
    )
    private val mockedDigestDomain = QesDocumentDigestDomain(
        label = mockedDocumentLabel,
        hash = mockedDocumentHash,
        hashType = DocumentDigest.HASH_TYPE_DTBSR,
        signedProperties = mockedAttributesDomain,
        href = mockedDocumentHref,
        checksum = mockedChecksumDomain,
        oneTimePassword = mockedOneTimePassword,
    )
    private val mockedApproval = QesApprovalRequest(
        type = QesApprovalRequest.TYPE,
        credentialIds = mockedQueryIds,
        hashAlgorithms = listOf("sha-256"),
        credentialId = mockedSigningCredentialId,
        signatureQualifier = mockedQualifier,
        numSignatures = mockedSignatureCount,
        documentDigests = listOf(mockedDigest),
        hashAlgorithmOid = mockedDocumentHashAlgorithmOid,
    )
    private val mockedReference = SignatureRequest.WithDocumentReference(
        signatureQualifier = mockedQualifier,
        responseUri = mockedResponseUri,
        signatureFormat = mockedSignatureFormat,
        conformanceLevel = mockedConformanceLevel,
        signedProperties = mockedAttributes,
        label = mockedDocumentLabel,
        access = mockedAccess,
        href = mockedDocumentHref,
        checksum = mockedChecksum,
        signAlgo = mockedSigningAlgorithmOid,
    )
    private val mockedReferenceDomain = QesSignatureRequestDomain(
        label = mockedDocumentLabel,
        signatureQualifier = mockedQualifier,
        responseUri = mockedResponseUri,
        signatureFormat = mockedSignatureFormat,
        conformanceLevel = mockedConformanceLevel,
        signedProperties = mockedAttributesDomain,
        href = mockedDocumentHref,
        checksum = mockedChecksumDomain,
        oneTimePassword = mockedOneTimePassword,
    )

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        whenever(transactionType.displayName).thenReturn(mockedDisplayName)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region toPresentationTransactionDataDomain

    // Case 1:
    // 1. An approval supplies access details, attributes and distinct document/checksum algorithms.
    // 2. The signature count differs from the document count; binding algorithms also differ.
    //
    // Case 1 Expected Result:
    // All display values retain their source meaning and the original transaction stays unchanged.
    @Test
    fun `Given a complete approval, When toPresentationTransactionDataDomain is called, Then source values remain distinct`() {
        // Given
        val transaction = mockedTransaction(payload = mockedApproval)

        // When
        val result = transaction.toPresentationTransactionDataDomain()

        // Then
        assertEquals(
            PresentationTransactionDataDomain.QesApproval(
                displayName = mockedDisplayName,
                credentialIds = mockedQueryIds,
                credentialId = mockedSigningCredentialId,
                signatureQualifier = mockedQualifier,
                numSignatures = mockedSignatureCount,
                hashAlgorithmOid = mockedDocumentHashAlgorithmOid,
                documentDigests = listOf(mockedDigestDomain),
            ),
            result,
        )
        assertSame(mockedApproval, transaction.payload)
        assertEquals(mockedRawBytes, transaction.rawBytes)
        assertEquals(listOf(Algorithm.SHA512), transaction.hashAlgorithms)
    }

    // Case 2:
    // 1. An approval has no qualifier or optional document metadata.
    //
    // Case 2 Expected Result:
    // Missing values remain null without inferred labels, signature type or integrity status.
    @Test
    fun `Given a minimal approval, When toPresentationTransactionDataDomain is called, Then optional values remain absent`() {
        // Given
        val transaction = mockedTransaction(
            payload = mockedApproval.copy(
                signatureQualifier = null,
                documentDigests = listOf(DocumentDigest(hash = mockedDocumentHash)),
            )
        )

        // When
        val result = transaction.toPresentationTransactionDataDomain()
                as PresentationTransactionDataDomain.QesApproval

        // Then
        assertEquals(mockedDisplayName, result.displayName)
        assertNull(result.signatureQualifier)
        assertEquals(
            listOf(
                QesDocumentDigestDomain(
                    label = null,
                    hash = mockedDocumentHash,
                    hashType = DocumentDigest.HASH_TYPE_DTBSR,
                    signedProperties = null,
                    href = null,
                    checksum = null,
                    oneTimePassword = null,
                )
            ),
            result.documentDigests,
        )
    }

    // Case 3:
    // 1. An approval contains repeated documents and different hash representations.
    // 2. The qualifier and document algorithm are unfamiliar.
    //
    // Case 3 Expected Result:
    // Every occurrence, representation and unfamiliar value is preserved in source order.
    @Test
    fun `Given repeated approval documents and unfamiliar codes, When toPresentationTransactionDataDomain is called, Then nothing is merged or normalized`() {
        // Given
        val mockedUnknownQualifier = "provider_specific_signature"
        val mockedUnknownAlgorithmOid = "1.3.6.1.4.1.99999.1"
        val transaction = mockedTransaction(
            payload = mockedApproval.copy(
                credentialId = null,
                signatureQualifier = mockedUnknownQualifier,
                hashAlgorithmOid = mockedUnknownAlgorithmOid,
                documentDigests = listOf(
                    mockedDigest.copy(hashType = DocumentDigest.HASH_TYPE_SODR),
                    mockedDigest,
                    mockedDigest.copy(hashType = DocumentDigest.HASH_TYPE_SDR),
                    mockedDigest,
                ),
            )
        )

        // When
        val result = transaction.toPresentationTransactionDataDomain()
                as PresentationTransactionDataDomain.QesApproval

        // Then
        assertEquals(mockedQueryIds, result.credentialIds)
        assertNull(result.credentialId)
        assertEquals(mockedUnknownQualifier, result.signatureQualifier)
        assertEquals(mockedUnknownAlgorithmOid, result.hashAlgorithmOid)
        assertEquals(
            listOf(
                mockedDigestDomain.copy(hashType = DocumentDigest.HASH_TYPE_SODR),
                mockedDigestDomain,
                mockedDigestDomain.copy(hashType = DocumentDigest.HASH_TYPE_SDR),
                mockedDigestDomain,
            ),
            result.documentDigests,
        )
    }

    // Case 4:
    // 1. A request mixes referenced and inline documents, including a repeated reference.
    // 2. Both variants supply signature metadata.
    //
    // Case 4 Expected Result:
    // Order and metadata survive; only referenced documents have href/checksum/OTP.
    @Test
    fun `Given mixed signature requests, When toPresentationTransactionDataDomain is called, Then reference fields stay with their documents`() {
        // Given
        val mockedInlineLabel = "Inline document"
        val inlineDocument = SignatureRequest.WithDocument(
            signatureQualifier = mockedQualifier,
            responseUri = mockedResponseUri,
            signatureFormat = mockedSignatureFormat,
            conformanceLevel = mockedConformanceLevel,
            signedProperties = mockedAttributes,
            label = mockedInlineLabel,
            document = "aW5saW5l",
            signAlgo = mockedSigningAlgorithmOid,
        )
        val transaction = mockedTransaction(
            payload = QesRequest(
                type = QesRequest.TYPE,
                credentialIds = mockedQueryIds,
                signatureRequests = listOf(mockedReference, inlineDocument, mockedReference),
            )
        )

        // When
        val result = transaction.toPresentationTransactionDataDomain()

        // Then
        assertEquals(
            PresentationTransactionDataDomain.Qes(
                displayName = mockedDisplayName,
                credentialIds = mockedQueryIds,
                signatureRequests = listOf(
                    mockedReferenceDomain,
                    mockedReferenceDomain.copy(
                        label = mockedInlineLabel,
                        href = null,
                        checksum = null,
                        oneTimePassword = null,
                    ),
                    mockedReferenceDomain,
                ),
            ),
            result,
        )
    }

    // Case 5:
    // 1. A referenced document supplies only the required signature metadata.
    //
    // Case 5 Expected Result:
    // Optional fields remain null and its supplied location stays unchanged.
    @Test
    fun `Given a minimal referenced signature, When toPresentationTransactionDataDomain is called, Then optional fields are not invented`() {
        // Given
        val transaction = mockedTransaction(
            payload = QesRequest(
                type = QesRequest.TYPE,
                credentialIds = mockedQueryIds,
                signatureRequests = listOf(
                    SignatureRequest.WithDocumentReference(
                        signatureQualifier = mockedQualifier,
                        href = mockedDocumentHref,
                        signAlgo = mockedSigningAlgorithmOid,
                    )
                ),
            )
        )

        // When
        val result = transaction.toPresentationTransactionDataDomain()

        // Then
        assertEquals(
            PresentationTransactionDataDomain.Qes(
                displayName = mockedDisplayName,
                credentialIds = mockedQueryIds,
                signatureRequests = listOf(
                    QesSignatureRequestDomain(
                        label = null,
                        signatureQualifier = mockedQualifier,
                        responseUri = null,
                        signatureFormat = null,
                        conformanceLevel = null,
                        signedProperties = null,
                        href = mockedDocumentHref,
                        checksum = null,
                        oneTimePassword = null,
                    )
                ),
            ),
            result,
        )
    }

    // Case 6:
    // 1. A payload is not one of the supported QES types.
    //
    // Case 6 Expected Result:
    // The entry has an Unavailable projection without parsing its raw bytes.
    @Test
    fun `Given an unsupported payload, When toPresentationTransactionDataDomain is called, Then an unavailable entry is returned`() {
        // Given
        val transaction = mockedTransaction(payload = "unsupported payload")

        // When
        val result = transaction.toPresentationTransactionDataDomain()

        // Then
        assertEquals(PresentationTransactionDataDomain.Unavailable, result)
        assertEquals(mockedRawBytes, transaction.rawBytes)
    }

    // Case 7:
    // 1. The SDK payload uses mutable lists of references, documents and attributes.
    // 2. Those source lists change after the domain projection.
    //
    // Case 7 Expected Result:
    // The projected snapshot retains the values present when it was built.
    @Test
    fun `Given mutable source lists, When the source changes after mapping, Then the domain snapshot stays unchanged`() {
        // Given
        val queryIds = mockedQueryIds.toMutableList()
        val attributes = mockedAttributes.toMutableList()
        val documents = mutableListOf(mockedDigest.copy(signedProperties = attributes))
        val transaction = mockedTransaction(
            payload = mockedApproval.copy(
                credentialIds = queryIds,
                documentDigests = documents,
            )
        )

        // When
        val result = transaction.toPresentationTransactionDataDomain()
                as PresentationTransactionDataDomain.QesApproval
        queryIds.clear()
        attributes.clear()
        documents.clear()

        // Then
        assertEquals(mockedQueryIds, result.credentialIds)
        assertEquals(listOf(mockedDigestDomain), result.documentDigests)
    }

    //endregion

    //region toPresentationTransactionDataDomains

    // Case 1:
    // 1. A recorded list contains both payload types and a repeated approval.
    //
    // Case 1 Expected Result:
    // Configured parsers restore the display fields in recorded order without merging entries.
    @Test
    fun `Given both recorded payload types, When mapped with configured types, Then their fields and order survive`() {
        // Given
        val approval = Json.encodeToJsonElement(mockedApproval)
        val request = Json.encodeToJsonElement(
            QesRequest(
                type = QesRequest.TYPE,
                credentialIds = mockedQueryIds,
                signatureRequests = listOf(mockedReference),
            )
        )
        val recorded = TransactionalData(JsonArray(listOf(approval, request, approval)))

        // When
        val result = recorded.toPresentationTransactionDataDomains(
            types = listOf(TransactionDataType.QES_APPROVAL, TransactionDataType.QES),
        )

        // Then
        val expectedApproval = PresentationTransactionDataDomain.QesApproval(
            displayName = null,
            credentialIds = mockedQueryIds,
            credentialId = mockedSigningCredentialId,
            signatureQualifier = mockedQualifier,
            numSignatures = mockedSignatureCount,
            hashAlgorithmOid = mockedDocumentHashAlgorithmOid,
            documentDigests = listOf(mockedDigestDomain),
        )
        assertEquals(
            listOf(
                expectedApproval,
                PresentationTransactionDataDomain.Qes(
                    displayName = null,
                    credentialIds = mockedQueryIds,
                    signatureRequests = listOf(mockedReferenceDomain),
                ),
                expectedApproval,
            ),
            result,
        )
    }

    // Case 2:
    // 1. An approval is recorded but its type is not configured.
    //
    // Case 2 Expected Result:
    // Raw fallback data becomes an unavailable entry instead of bypassing the configured parser.
    @Test
    fun `Given an undeclared recorded type, When mapped, Then its details are unavailable`() {
        // Given
        val recorded =
            TransactionalData(JsonArray(listOf(Json.encodeToJsonElement(mockedApproval))))

        // When
        val result = recorded.toPresentationTransactionDataDomains(types = emptyList())

        // Then
        assertEquals(listOf(PresentationTransactionDataDomain.Unavailable), result)
    }

    // Case 3:
    // 1. Unreadable records precede a valid approval, including a malformed type property.
    //
    // Case 3 Expected Result:
    // Every unreadable record has a fallback and the valid record remains available.
    @Test
    fun `Given malformed and unknown recorded entries, When mapped, Then failures remain local to each entry`() {
        // Given
        val unreadable = listOf(
            """{"type":"unknown","label":"Do not display raw data"}""",
            """{"type":"${QesApprovalRequest.TYPE}"}""",
            """{"type":{}}""",
            """{"type":[]}""",
            """{}""",
            "null",
        ).map { source -> Json.parseToJsonElement(source) }
        val recorded = TransactionalData(
            JsonArray(unreadable + Json.encodeToJsonElement(mockedApproval))
        )

        // When
        val result = recorded.toPresentationTransactionDataDomains(
            types = listOf(TransactionDataType.QES_APPROVAL),
        )

        // Then
        assertEquals(
            List(unreadable.size) { PresentationTransactionDataDomain.Unavailable },
            result.dropLast(1),
        )
        val approval = result.last() as PresentationTransactionDataDomain.QesApproval
        assertEquals(listOf(mockedDigestDomain), approval.documentDigests)
        assertEquals(mockedSignatureCount, approval.numSignatures)
    }

    // Case 4:
    // 1. The recorded array is empty.
    //
    // Case 4 Expected Result:
    // No transaction-data entries are invented.
    @Test
    fun `Given an empty recorded array, When mapped, Then no entries are returned`() {
        // Given
        val recorded = TransactionalData(JsonArray(emptyList()))

        // When
        val result = recorded.toPresentationTransactionDataDomains(
            types = listOf(TransactionDataType.QES_APPROVAL, TransactionDataType.QES),
        )

        // Then
        assertEquals(emptyList<PresentationTransactionDataDomain>(), result)
    }

    // Case 5:
    // 1. The configured parser supplies a custom display name for an approval with no qualifier.
    //
    // Case 5 Expected Result:
    // History keeps recorded fields without adding the configured display name.
    @Test
    fun `Given a configured display name, When recorded data is mapped, Then no display name is added`() {
        // Given
        val approval = mockedApproval.copy(signatureQualifier = null)
        val recorded = TransactionalData(JsonArray(listOf(Json.encodeToJsonElement(approval))))
        whenever(transactionType.identifier).thenReturn(QesApprovalRequest.TYPE)
        whenever(transactionType.parseOpenId4VpRequest(any())).thenReturn(approval)

        // When
        val result = recorded.toPresentationTransactionDataDomains(
            types = listOf(TransactionDataType(parser = transactionType)),
        ).single() as PresentationTransactionDataDomain.QesApproval

        // Then
        assertNull(result.displayName)
        assertNull(result.signatureQualifier)
    }

    //endregion

    //region helper functions
    private fun mockedTransaction(payload: Any): TransactionData<Any> =
        TransactionData(
            type = transactionType,
            payload = payload,
            protocol = TransactionProtocol.OPENID4VP,
            rawBytes = mockedRawBytes,
            hashAlgorithms = listOf(Algorithm.SHA512),
        )
    //endregion
}