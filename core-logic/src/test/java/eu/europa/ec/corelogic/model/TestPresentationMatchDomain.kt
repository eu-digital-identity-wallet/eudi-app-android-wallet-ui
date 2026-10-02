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

package eu.europa.ec.corelogic.model

import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.DocumentDigest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.QesApprovalRequest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.QesApprovalTransactionType
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.multipaz.claim.Claim
import org.multipaz.claim.JsonClaim
import org.multipaz.credential.Credential
import org.multipaz.document.Document
import org.multipaz.documenttype.TransactionType
import org.multipaz.mdoc.request.DocRequest
import org.multipaz.openid.dcql.DcqlCredentialQuery
import org.multipaz.presentment.CredentialMatchSource
import org.multipaz.presentment.CredentialMatchSourceIso18013
import org.multipaz.presentment.CredentialMatchSourceOpenID4VP
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch
import org.multipaz.presentment.TransactionData
import org.multipaz.presentment.TransactionProtocol
import org.multipaz.request.JsonRequestedClaim
import org.multipaz.request.MdocRequestedClaim
import org.multipaz.request.RequestedClaim

class TestPresentationMatchDomain {

    @Mock
    private lateinit var credential: Credential

    @Mock
    private lateinit var document: Document

    @Mock
    private lateinit var claim: JsonClaim

    @Mock
    private lateinit var credentialQuery: DcqlCredentialQuery

    @Mock
    private lateinit var docRequest: DocRequest

    @Mock
    private lateinit var unsupportedTransaction: TransactionData<Any>

    @Mock
    private lateinit var unsupportedTransactionType: TransactionType<Any>

    private lateinit var closeable: AutoCloseable

    private val mockedDocumentId = "identity-document"
    private val mockedCredentialId = "identity-credential"
    private val mockedQueryId = "query_0"
    private val mockedClaimName = "family_name"
    private val mockedNamespace = "eu.europa.ec.eudi.pid.1"
    private val mockedFirstSignatureCount = 2
    private val mockedSecondSignatureCount = 3
    private val mockedRequestedClaim = JsonRequestedClaim(
        id = null,
        vctValues = listOf("urn:eudi:pid:1"),
        claimPath = JsonArray(listOf(JsonPrimitive(mockedClaimName))),
        values = null,
    )

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        mockCredentialIdentity()
    }

    @After
    fun after() {
        closeable.close()
    }

    //region from

    // Case 1:
    // 1. A match contains a requested claim and several approval occurrences.
    //
    // Case 1 Expected Result:
    // Identity, query, claims and transaction order survive without changing the SDK match.
    @Test
    fun `Given a match with repeated transactions, When from is called, Then identity claims and source occurrences are retained`() {
        // Given
        val first = mockedApprovalTransaction(signatureCount = mockedFirstSignatureCount)
        val second = mockedApprovalTransaction(signatureCount = mockedSecondSignatureCount)
        val transactions = listOf(first, second, first)
        val source = CredentialMatchSourceOpenID4VP(credentialQuery = credentialQuery)
        val match = mockedMatch(
            source = source,
            transactions = transactions,
            claims = mapOf(mockedRequestedClaim to claim),
        )

        // When
        val result = PresentationMatchDomain.from(match = match)

        // Then
        assertEquals(mockedDocumentId, result.documentId)
        assertEquals(mockedCredentialId, result.credentialId)
        assertEquals(mockedQueryId, result.queryId)
        assertEquals(
            listOf(
                ClaimPathDomain.ofPlainKeys(
                    names = listOf(mockedClaimName),
                    type = ClaimType.SdJwtVc,
                )
            ),
            result.requestedClaims,
        )
        assertEquals(
            listOf(
                mockedFirstSignatureCount,
                mockedSecondSignatureCount,
                mockedFirstSignatureCount
            ),
            result.transactionData.map { transaction ->
                (transaction as PresentationTransactionDataDomain.QesApproval).numSignatures
            },
        )
        assertSame(transactions, match.transactionData)
        assertSame(source, match.source)
        assertEquals(mapOf(mockedRequestedClaim to claim), match.claims)
    }

    // Case 2:
    // 1. Two candidate matches share credential/query identity but carry different transactions.
    //
    // Case 2 Expected Result:
    // Each domain projection keeps only the transaction from its own source match.
    @Test
    fun `Given different match transactions, When from is called for each, Then no transaction is borrowed from another match`() {
        // Given
        val firstMatch = mockedMatch(
            source = CredentialMatchSourceOpenID4VP(credentialQuery = credentialQuery),
            transactions = listOf(mockedApprovalTransaction(signatureCount = mockedFirstSignatureCount)),
            claims = mapOf(mockedRequestedClaim to claim),
        )
        val secondMatch = firstMatch.copy(
            transactionData = listOf(
                mockedApprovalTransaction(signatureCount = mockedSecondSignatureCount)
            ),
        )

        // When
        val first = PresentationMatchDomain.from(match = firstMatch)
        val second = PresentationMatchDomain.from(match = secondMatch)

        // Then
        assertEquals(mockedQueryId, first.queryId)
        assertEquals(mockedQueryId, second.queryId)
        assertEquals(
            mockedFirstSignatureCount,
            (first.transactionData.single() as PresentationTransactionDataDomain.QesApproval).numSignatures,
        )
        assertEquals(
            mockedSecondSignatureCount,
            (second.transactionData.single() as PresentationTransactionDataDomain.QesApproval).numSignatures,
        )
    }

    // Case 3:
    // 1. An mdoc match has no transaction data.
    //
    // Case 3 Expected Result:
    // Its query remains null, transactions remain empty and claims keep the mdoc namespace.
    @Test
    fun `Given an mdoc match without transactions, When from is called, Then its existing projection is preserved`() {
        // Given
        val requestedClaim = MdocRequestedClaim(
            docType = mockedNamespace,
            namespaceName = mockedNamespace,
            dataElementName = mockedClaimName,
            intentToRetain = false,
        )
        val match = mockedMatch(
            source = CredentialMatchSourceIso18013(docRequest = docRequest),
            transactions = emptyList(),
            claims = mapOf(requestedClaim to claim),
        )

        // When
        val result = PresentationMatchDomain.from(match = match)

        // Then
        assertNull(result.queryId)
        assertEquals(emptyList<PresentationTransactionDataDomain>(), result.transactionData)
        assertEquals(
            listOf(
                ClaimPathDomain.ofPlainKeys(
                    names = listOf(mockedClaimName),
                    type = ClaimType.MsoMdoc(namespace = mockedNamespace),
                )
            ),
            result.requestedClaims,
        )
    }

    // Case 4:
    // 1. An unsupported transaction appears between two supported approvals.
    //
    // Case 4 Expected Result:
    // Its fallback retains that position and the supported entries remain available.
    @Test
    fun `Given an unsupported transaction among approvals, When from is called, Then the fallback preserves its occurrence`() {
        // Given
        mockUnsupportedTransactionPayload()
        val approval = mockedApprovalTransaction(signatureCount = mockedFirstSignatureCount)
        val match = mockedMatch(
            source = CredentialMatchSourceOpenID4VP(credentialQuery = credentialQuery),
            transactions = listOf(approval, unsupportedTransaction, approval),
            claims = mapOf(mockedRequestedClaim to claim),
        )

        // When
        val result = PresentationMatchDomain.from(match = match)

        // Then
        assertEquals(3, result.transactionData.size)
        assertEquals(PresentationTransactionDataDomain.Unavailable, result.transactionData[1])
        assertEquals(result.transactionData[0], result.transactionData[2])
        assertEquals(
            mockedFirstSignatureCount,
            (result.transactionData[0] as PresentationTransactionDataDomain.QesApproval).numSignatures,
        )
    }

    //endregion

    //region helper functions
    private fun mockCredentialIdentity() {
        whenever(credential.document).thenReturn(document)
        whenever(document.identifier).thenReturn(mockedDocumentId)
        whenever(credential.identifier).thenReturn(mockedCredentialId)
        whenever(credentialQuery.id).thenReturn(mockedQueryId)
    }

    private fun mockUnsupportedTransactionPayload() {
        whenever(unsupportedTransaction.type).thenReturn(unsupportedTransactionType)
        whenever(unsupportedTransactionType.displayName).thenReturn("Unsupported transaction")
        whenever(unsupportedTransaction.payload).thenReturn("unsupported payload")
    }

    private fun mockedMatch(
        source: CredentialMatchSource,
        transactions: List<TransactionData<*>>,
        claims: Map<RequestedClaim, Claim>,
    ): CredentialPresentmentSetOptionMemberMatch =
        CredentialPresentmentSetOptionMemberMatch(
            credential = credential,
            claims = claims,
            source = source,
            transactionData = transactions,
            transactionUserInput = emptyMap(),
        )

    private fun mockedApprovalTransaction(signatureCount: Int): TransactionData<QesApprovalRequest> =
        TransactionData(
            type = QesApprovalTransactionType,
            payload = QesApprovalRequest(
                type = QesApprovalRequest.TYPE,
                credentialIds = listOf(mockedQueryId),
                credentialId = "signing-credential",
                numSignatures = signatureCount,
                documentDigests = listOf(DocumentDigest(hash = "+/8=")),
                hashAlgorithmOid = "2.16.840.1.101.3.4.2.1",
            ),
            protocol = TransactionProtocol.OPENID4VP,
            rawBytes = ByteString(byteArrayOf(1, 2, 3)),
            hashAlgorithms = null,
        )
    //endregion
}