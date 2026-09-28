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
package eu.europa.ec.commonfeature.ui.request.transformer

import eu.europa.ec.commonfeature.ui.request.model.RequestTransactionDataUi
import eu.europa.ec.corelogic.model.DocumentChecksumDomain
import eu.europa.ec.corelogic.model.PresentationTransactionDataDomain
import eu.europa.ec.corelogic.model.QesSignatureRequestDomain
import eu.europa.ec.corelogic.model.SigningAttributeDomain
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.StringResourceProviderMocker.mockTransactionDataStrings
import eu.europa.ec.testfeature.util.mockedRequestCollapsedSupportingText
import eu.europa.ec.testfeature.util.mockedTransactionDataApproval
import eu.europa.ec.testfeature.util.mockedTransactionQueryId
import eu.europa.ec.testfeature.util.mockedValidPidWithBasicFieldsRequestMatch
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemSupportingContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class TestTransactionDataTransformer {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var transformer: TransactionDataTransformer
    private lateinit var closeable: AutoCloseable

    private val mockedRequestDisplayName = "QES request"

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        transformer = TransactionDataTransformer(resourceProvider = resourceProvider)
        mockTransactionDataStrings(resourceProvider = resourceProvider)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region transformToUi

    // Case 1:
    // 1. An approval supplies one labelled digest, no qualifier and no link.
    //
    // Case 1 Expected Result:
    // One collapsed section includes the SDK type name without a qualifier or integrity result.
    @Test
    fun `Given Case 1, When transformToUi is called, Then Case 1 Expected Result is returned`() {
        // Given
        val transactions = listOf(mockedTransactionDataApproval)

        // When
        val section = transform(
            transactions = transactions,
        )!!

        // Then
        assertEquals("Data to be signed", section.title)
        assertFalse(section.details.isExpanded)
        assertEquals("Signature details", section.details.header.text())
        assertEquals(
            ListItemSupportingContentDataUi.Text(mockedRequestCollapsedSupportingText),
            section.details.header.supportingContentData,
        )
        assertEquals(
            ListItemTrailingContentDataUi.Icon(iconData = AppIcons.KeyboardArrowDown),
            section.details.header.trailingContentData,
        )
        assertEquals(
            listOf(
                "Trust framework" to "eIDAS",
                "Transaction type" to mockedTransactionDataApproval.displayName,
                "Requested credentials" to mockedTransactionQueryId,
                "Signing credential ID" to "signing-credential-id",
                "Number of signatures" to "1",
                "Document" to "file-sample_150kB.pdf",
                "Hash representation" to "DTBSR",
                "DTBSR hash" to "YWJjZA==",
                "DTBSR hash algorithm" to "SHA-256",
            ),
            section.rows().map { row -> row.overlineText to row.text() },
        )
        assertTrue(section.documentUrlsByItemId.isEmpty())
    }

    // Case 2:
    // 1. An approval supplies all optional document fields and a count different from its document count.
    // 2. Document hashing and expected-checksum algorithms differ; attributes include name-only/empty values.
    //
    // Case 2 Expected Result:
    // Exact values and source ownership are retained; only the href action has a target.
    @Test
    fun `Given Case 2, When transformToUi is called, Then Case 2 Expected Result is returned`() {
        // Given
        val document = mockedTransactionDataApproval.documentDigests.single().copy(
            label = " Contract.pdf ",
            href = mockedDocumentUrl,
            checksum = DocumentChecksumDomain(value = "Y2hlY2s=", algorithmOid = mockedSha384Oid),
            signedProperties = listOf(
                SigningAttributeDomain(name = "Reason", value = " Signing\ncontract "),
                SigningAttributeDomain(name = "Name only", value = null),
                SigningAttributeDomain(name = "Empty", value = ""),
            ),
            oneTimePassword = mockedOtp,
        )
        val approval = mockedTransactionDataApproval.copy(
            signatureQualifier = "eu_eidas_qes",
            numSignatures = 4,
            documentDigests = listOf(document),
        )

        // When
        val section = transform(
            transactions = listOf(approval),
        )!!

        // Then
        assertEquals(listOf("Qualified electronic signature (QES)"), section.values("Signature type"))
        assertEquals(listOf("4"), section.values("Number of signatures"))
        assertEquals(listOf(" Contract.pdf "), section.values("Document"))
        assertEquals(listOf("Y2hlY2s="), section.values("Expected document checksum"))
        assertEquals(listOf("SHA-384"), section.values("Checksum algorithm"))
        assertEquals(listOf("SHA-256"), section.values("DTBSR hash algorithm"))
        assertEquals(listOf(mockedOtp), section.values("One-time password (OTP)"))
        assertEquals(listOf(" Signing\ncontract "), section.values("Reason"))
        assertTrue(section.rows().any { row -> row.overlineText == null && row.text() == "Name only" })
        assertEquals(listOf(""), section.values("Empty"))
        assertTrue(section.values("RP origin").isEmpty())
        assertTrue(section.values("RP identifier").isEmpty())
        assertEquals(listOf(mockedDocumentUrl), section.documentUrlsByItemId.values.toList())
        val action = section.rows().single { row -> row.itemId in section.documentUrlsByItemId }
        assertEquals("Open document", action.text())
        assertEquals(ListItemTrailingContentDataUi.Icon(AppIcons.OpenNew), action.trailingContentData)
        assertTrue(section.rows().filter { row -> row.itemId != action.itemId }
            .all { row -> row.trailingContentData == null })
    }

    // Case 3:
    // 1. A QES request contains an inline signature and a reference signature with optional metadata.
    //
    // Case 3 Expected Result:
    // Each document owns its type/format/profile/attributes/response URI; no approval count or DTBSR is invented.
    @Test
    fun `Given Case 3, When transformToUi is called, Then Case 3 Expected Result is returned`() {
        // Given
        val reference = mockedSignature.copy(
            label = "Reference.pdf",
            href = mockedDocumentUrl,
            checksum = DocumentChecksumDomain(value = "ZXhwZWN0ZWQ=", algorithmOid = mockedSha384Oid),
            oneTimePassword = mockedOtp,
            signatureQualifier = "eu_eidas_qeseal",
            signatureFormat = "P",
            conformanceLevel = "AdES-B-B",
            responseUri = mockedResponseUri,
            signedProperties = listOf(SigningAttributeDomain(name = "Purpose", value = "Contract")),
        )

        // When
        val section = transform(
            transactions = listOf(
                PresentationTransactionDataDomain.Qes(
                    displayName = mockedRequestDisplayName,
                    credentialIds = listOf(mockedTransactionQueryId),
                    signatureRequests = listOf(mockedSignature, reference),
                ),
            ),
        )!!

        // Then
        assertEquals(listOf("Inline.pdf", "Reference.pdf"), section.values("Document"))
        assertEquals(listOf(mockedRequestDisplayName), section.values("Transaction type"))
        assertEquals(listOf(mockedTransactionQueryId), section.values("Requested credentials"))
        assertEquals(
            listOf("Qualified electronic signature (QES)", "Qualified electronic seal"),
            section.values("Signature type"),
        )
        assertEquals(listOf("PAdES"), section.values("Signature format"))
        assertEquals(listOf("AdES-B-B"), section.values("Conformance level"))
        assertEquals(listOf(mockedResponseUri), section.values("Response URI"))
        assertEquals(listOf("Contract"), section.values("Purpose"))
        assertEquals(listOf(mockedOtp), section.values("One-time password (OTP)"))
        assertEquals(listOf(mockedDocumentUrl), section.documentUrlsByItemId.values.toList())
        assertTrue(section.values("DTBSR hash").isEmpty())
        assertTrue(section.values("Number of signatures").isEmpty())
        assertTrue(section.values("Signing credential ID").isEmpty())
        val referenceRows = section.rows().filter { row -> row.itemId.contains("/document-1/") }
        assertTrue(referenceRows.any { row -> row.text() == mockedResponseUri })
        assertEquals(1, section.values("Expected document checksum").size)
    }

    // Case 4:
    // 1. Non-null unknown codes, empty strings and a non-web href are supplied.
    //
    // Case 4 Expected Result:
    // Every supplied field remains unchanged; an empty label is not replaced, and no action is invented.
    @Test
    fun `Given Case 4, When transformToUi is called, Then Case 4 Expected Result is returned`() {
        // Given
        val approval = mockedTransactionDataApproval.copy(
            signatureQualifier = "",
            hashAlgorithmOid = "unknown-oid",
            documentDigests = listOf(
                mockedTransactionDataApproval.documentDigests.single().copy(
                    label = "",
                    hashType = "future-representation",
                    hash = " padded== ",
                    href = "javascript:alert(1)",
                    oneTimePassword = "",
                    checksum = DocumentChecksumDomain(value = "", algorithmOid = "custom-checksum"),
                ),
            ),
        )

        // When
        val section = transform(
            transactions = listOf(approval),
        )!!

        // Then
        assertEquals(listOf(""), section.values("Signature type"))
        assertEquals(listOf(""), section.values("Document"))
        assertEquals(listOf("future-representation"), section.values("Hash representation"))
        assertEquals(listOf(" padded== "), section.values("Document hash"))
        assertEquals(listOf("unknown-oid"), section.values("Document hash algorithm"))
        assertEquals(listOf("custom-checksum"), section.values("Checksum algorithm"))
        assertEquals(listOf(""), section.values("Expected document checksum"))
        assertEquals(listOf(""), section.values("One-time password (OTP)"))
        assertEquals(listOf("javascript:alert(1)"), section.values("Document location"))
        assertTrue(section.documentUrlsByItemId.isEmpty())
    }

    // Case 5:
    // 1. Signature requests contain all known qualifier families/formats and unknown values.
    //
    // Case 5 Expected Result:
    // Known codes have readable labels; unknown codes and the requested profile are retained.
    @Test
    fun `Given Case 5, When transformToUi is called, Then Case 5 Expected Result is returned`() {
        // Given
        val qualifiers = listOf(
            "eu_eidas_qes", "eu_eidas_qeseal", "eu_eidas_aes", "eu_eidas_aeseal",
            "eu_eidas_aesqc", "eu_eidas_aesealqc", "future-qualifier",
        )
        val formats = listOf("C", "X", "P", "J", "", null, "future-format")
        val request = PresentationTransactionDataDomain.Qes(
            displayName = mockedRequestDisplayName,
            credentialIds = emptyList(),
            signatureRequests = qualifiers.mapIndexed { index, qualifier ->
                mockedSignature.copy(signatureQualifier = qualifier, signatureFormat = formats[index])
            },
        )

        // When
        val section = transform(
            transactions = listOf(request),
        )!!

        // Then
        assertEquals(
            listOf(
                "Qualified electronic signature (QES)", "Qualified electronic seal",
                "Advanced electronic signature", "Advanced electronic seal",
                "Advanced electronic signature with a qualified certificate",
                "Advanced electronic seal with a qualified certificate", "future-qualifier",
            ),
            section.values("Signature type"),
        )
        assertEquals(listOf("CAdES", "XAdES", "PAdES", "JAdES", "", "future-format"),
            section.values("Signature format"))
    }

    // Case 6:
    // 1. Repeated approval occurrences and an unsupported occurrence share one match.
    //
    // Case 6 Expected Result:
    // Ordered transaction groups preserve every occurrence and have unique IDs within one section.
    @Test
    fun `Given Case 6, When transformToUi is called, Then Case 6 Expected Result is returned`() {
        // Given
        val transactions = listOf(
            mockedTransactionDataApproval,
            PresentationTransactionDataDomain.Unavailable,
            mockedTransactionDataApproval,
        )

        // When
        val section = transform(
            transactions = transactions,
        )!!

        // Then
        val headings = section.rows().filter { row -> row.overlineText == null }.map { row -> row.text() }
        assertEquals(
            listOf("Transaction 1", "Transaction 2", "Details for this transaction are unavailable.", "Transaction 3"),
            headings,
        )
        assertEquals(listOf("YWJjZA==", "YWJjZA=="), section.values("DTBSR hash"))
        val ids = section.rows().map { row -> row.itemId } + section.details.header.itemId
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { id -> id.startsWith(mockedSectionId) })
    }

    // Case 7:
    // 1. Several approval documents lack names, including two equal digests.
    //
    // Case 7 Expected Result:
    // Positional names represent all three entries without adding a Transaction 1 heading.
    @Test
    fun `Given Case 7, When transformToUi is called, Then Case 7 Expected Result is returned`() {
        // Given
        val missingName = mockedTransactionDataApproval.documentDigests.single().copy(label = null)

        // When
        val section = transform(
            transactions = listOf(mockedTransactionDataApproval.copy(documentDigests = List(3) { missingName })),
        )!!

        // Then
        assertEquals(listOf("Document 1", "Document 2", "Document 3"), section.values("Document"))
        assertEquals(3, section.values("DTBSR hash").size)
        assertFalse(section.rows().any { row -> row.text() == "Transaction 1" })
    }

    // Case 8:
    // 1. The payload supplies two query references and repeats the first one.
    //
    // Case 8 Expected Result:
    // References keep their order and duplicates without credential names.
    @Test
    fun `Given Case 8, When transformToUi is called, Then Case 8 Expected Result is returned`() {
        // Given
        val otherQuery = "query_other"
        val approval = mockedTransactionDataApproval.copy(
            credentialIds = listOf(mockedTransactionQueryId, otherQuery, mockedTransactionQueryId),
        )

        // When
        val section = transform(
            transactions = listOf(approval),
        )!!

        // Then
        assertEquals(
            listOf(mockedTransactionQueryId, otherQuery, mockedTransactionQueryId),
            section.values("Requested credentials"),
        )
        assertEquals(listOf("signing-credential-id"), section.values("Signing credential ID"))
    }

    // Case 9:
    // 1. Document hrefs include supported web locations and malformed or unsupported locations.
    //
    // Case 9 Expected Result:
    // All supplied locations remain text; only valid HTTP(S), host-bearing, credential-free URLs are actionable.
    @Test
    fun `Given Case 9, When transformToUi is called, Then Case 9 Expected Result is returned`() {
        // Given
        val valid = listOf(mockedDocumentUrl, "http://example.org:8080/file", "HTTPS://example.org/file#page=2")
        val invalid = listOf(
            "file:///document.pdf", "content://documents/1", "javascript:alert(1)", "/relative.pdf",
            "https:///missing-host", "https://user:password@example.org/file", "https://example.org/has space",
            "https://example.org/%zz", "https://example.org:99999/file", "https://example.org:0/file",
            "https://example.org/\nfile", "",
        )
        val locations = valid + invalid
        val request = PresentationTransactionDataDomain.Qes(
            displayName = mockedRequestDisplayName,
            credentialIds = emptyList(),
            signatureRequests = locations.map { href ->
                mockedSignature.copy(href = href, responseUri = mockedResponseUri)
            },
        )

        // When
        val section = transform(
            transactions = listOf(request),
        )!!

        // Then
        assertEquals(locations, section.values("Document location"))
        assertEquals(valid, section.documentUrlsByItemId.values.toList())
        assertEquals(valid.size, section.rows().count { row -> row.text() == "Open document" })
        assertEquals(locations.size, section.values("Response URI").size)
        assertFalse(section.documentUrlsByItemId.containsValue(mockedResponseUri))
    }

    // Case 10:
    // 1. The represented matches have no transaction data.
    //
    // Case 10 Expected Result:
    // No section is prepared.
    @Test
    fun `Given Case 10, When transformToUi is called, Then Case 10 Expected Result is returned`() {
        // Given
        val transactions = emptyList<PresentationTransactionDataDomain>()

        // When
        val section = transform(
            transactions = transactions,
        )

        // Then
        assertNull(section)
    }

    // Case 11:
    // 1. A represented match has an unsupported transaction.
    //
    // Case 11 Expected Result:
    // The informational fallback has no RP origin, RP identifier or link action.
    @Test
    fun `Given Case 11, When transformToUi is called, Then Case 11 Expected Result is returned`() {
        // Given
        val transactions = listOf(PresentationTransactionDataDomain.Unavailable)

        // When
        val section = transform(
            transactions = transactions,
        )!!

        // Then
        assertTrue(section.values("RP identifier").isEmpty())
        assertTrue(section.values("RP origin").isEmpty())
        assertTrue(section.rows().any { row -> row.text() == "Details for this transaction are unavailable." })
        assertTrue(section.documentUrlsByItemId.isEmpty())
    }

    // Case 12:
    // 1. Approval documents use SDR/SODR representations with a SHA-512 document algorithm.
    //
    // Case 12 Expected Result:
    // Representation-specific labels are accurate; no row describes these as DTBSR.
    @Test
    fun `Given Case 12, When transformToUi is called, Then Case 12 Expected Result is returned`() {
        // Given
        val digest = mockedTransactionDataApproval.documentDigests.single()
        val approval = mockedTransactionDataApproval.copy(
            hashAlgorithmOid = mockedSha512Oid,
            documentDigests = listOf(digest.copy(hashType = "sdr"), digest.copy(hashType = "sodr")),
        )

        // When
        val section = transform(
            transactions = listOf(approval),
        )!!

        // Then
        assertEquals(listOf("SDR", "SODR"), section.values("Hash representation"))
        assertEquals(listOf("YWJjZA=="), section.values("SDR hash"))
        assertEquals(listOf("YWJjZA=="), section.values("SODR hash"))
        assertEquals(listOf("SHA-512"), section.values("SDR hash algorithm"))
        assertEquals(listOf("SHA-512"), section.values("SODR hash algorithm"))
        assertTrue(section.values("DTBSR hash").isEmpty())
    }

    // Case 13:
    // 1. An approval and a QES request occur in the same section.
    //
    // Case 13 Expected Result:
    // Their fields stay in separate ordered groups; the request does not inherit the approval's count or hash.
    @Test
    fun `Given Case 13, When transformToUi is called, Then Case 13 Expected Result is returned`() {
        // Given
        val request = PresentationTransactionDataDomain.Qes(
            displayName = mockedRequestDisplayName,
            credentialIds = listOf(mockedTransactionQueryId),
            signatureRequests = listOf(mockedSignature.copy(responseUri = mockedResponseUri)),
        )

        // When
        val section = transform(
            transactions = listOf(mockedTransactionDataApproval, request),
        )!!

        // Then
        val approvalRows = section.rows().filter { row -> row.itemId.contains("/transaction-0/") }
        val requestRows = section.rows().filter { row -> row.itemId.contains("/transaction-1/") }
        assertEquals(
            listOf(mockedTransactionDataApproval.displayName, mockedRequestDisplayName),
            section.values("Transaction type"),
        )
        assertEquals(listOf("file-sample_150kB.pdf", "Inline.pdf"), section.values("Document"))
        assertTrue(approvalRows.any { row -> row.overlineText == "Number of signatures" })
        assertTrue(approvalRows.any { row -> row.overlineText == "DTBSR hash" })
        assertFalse(approvalRows.any { row -> row.overlineText == "Response URI" })
        assertTrue(requestRows.any { row -> row.text() == mockedResponseUri })
        assertTrue(requestRows.none { row ->
            row.overlineText in listOf("Number of signatures", "DTBSR hash", "Signing credential ID")
        })
        assertEquals(listOf("eIDAS"), section.values("Trust framework"))
    }

    //endregion

    //region transformRecordedToUi

    // Case 1:
    // 1. Recorded approval and signature entries contain document locations and other BA fields.
    //
    // Case 1 Expected Result:
    // Fields remain associated with their records, without document actions or invented RP context.
    @Test
    fun `Given recorded signing data, When transformRecordedToUi is called, Then fields are informational only`() {
        // Given
        val approval = mockedTransactionDataApproval.copy(
            displayName = null,
            documentDigests = mockedTransactionDataApproval.documentDigests.map { document ->
                document.copy(href = mockedDocumentUrl, oneTimePassword = mockedOtp)
            },
        )
        val request = PresentationTransactionDataDomain.Qes(
            displayName = null,
            credentialIds = listOf(mockedTransactionQueryId),
            signatureRequests = listOf(
                mockedSignature.copy(
                    label = null,
                    href = mockedDocumentUrl,
                    responseUri = mockedResponseUri,
                    signatureFormat = "P",
                    conformanceLevel = "AdES-B-LT",
                    checksum = DocumentChecksumDomain(value = "expected", algorithmOid = mockedSha384Oid),
                    signedProperties = listOf(SigningAttributeDomain(name = "reason", value = "Contract")),
                )
            ),
        )

        // When
        val rows = transformer.transformRecordedToUi(
            transactions = listOf(approval, request),
            sectionId = mockedSectionId,
        ).map { item -> item.header }

        // Then
        val values = rows.map { row -> row.overlineText to row.text() }
        assertFalse(rows.any { row -> row.overlineText == "Transaction type" })
        assertTrue(values.contains("Document location" to mockedDocumentUrl))
        assertTrue(values.contains("One-time password (OTP)" to mockedOtp))
        assertTrue(values.contains("Response URI" to mockedResponseUri))
        assertTrue(values.contains("DTBSR hash" to approval.documentDigests.single().hash))
        assertTrue(values.contains("DTBSR hash algorithm" to "SHA-256"))
        assertTrue(values.contains("Document" to "Document 1"))
        assertTrue(values.contains("Signature format" to "PAdES"))
        assertTrue(values.contains("Conformance level" to "AdES-B-LT"))
        assertTrue(values.contains("Expected document checksum" to "expected"))
        assertTrue(values.contains("Checksum algorithm" to "SHA-384"))
        assertTrue(values.contains("reason" to "Contract"))
        assertEquals(
            listOf(mockedTransactionQueryId, mockedTransactionQueryId),
            values.filter { (label, _) -> label == "Requested credentials" }.map { (_, value) -> value },
        )
        assertTrue(rows.all { row -> row.trailingContentData == null })
        assertFalse(rows.any { row -> row.itemId.endsWith("/open") })
        assertFalse(rows.any { row -> row.overlineText in listOf("RP origin", "RP identifier") })
        assertEquals(rows.size, rows.map { row -> row.itemId }.distinct().size)
    }

    // Case 2:
    // 1. Repeated unavailable records or no records are supplied.
    //
    // Case 2 Expected Result:
    // Unavailable entries retain distinct rows; no data produces no rows.
    @Test
    fun `Given unavailable or absent records, When transformRecordedToUi is called, Then no payload details are invented`() {
        // Given
        val unavailable = List(2) { PresentationTransactionDataDomain.Unavailable }

        // When
        val rows = transformer.transformRecordedToUi(
            transactions = unavailable,
            sectionId = mockedSectionId,
        )
        val absent = transformer.transformRecordedToUi(transactions = emptyList(), sectionId = mockedSectionId)

        // Then
        assertEquals(2, rows.count { row -> row.header.text() == "Details for this transaction are unavailable." })
        assertEquals(rows.size, rows.map { row -> row.header.itemId }.distinct().size)
        assertTrue(absent.isEmpty())
    }

    //endregion

    //region helpers

    private fun transform(
        transactions: List<PresentationTransactionDataDomain>,
    ): RequestTransactionDataUi? = transformer.transformToUi(
        matches = listOf(mockedValidPidWithBasicFieldsRequestMatch.copy(transactionData = transactions)),
        sectionId = mockedSectionId,
    )

    private fun RequestTransactionDataUi.rows(): List<ListItemDataUi> =
        details.nestedItems.map { item -> item.header }

    private fun RequestTransactionDataUi.values(label: String): List<String> =
        rows().filter { row -> row.overlineText == label }.map { row -> row.text() }

    private fun ListItemDataUi.text(): String =
        (mainContentData as ListItemMainContentDataUi.Text).text

    private val mockedSectionId = "transaction-data:request-id:0"
    private val mockedDocumentUrl = "https://documents.example.org/contract.pdf?token=a%2Fb#page=1"
    private val mockedResponseUri = "https://signer.example.org/response"
    private val mockedOtp = "000123"
    private val mockedSha384Oid = "2.16.840.1.101.3.4.2.2"
    private val mockedSha512Oid = "2.16.840.1.101.3.4.2.3"
    private val mockedSignature = QesSignatureRequestDomain(
        label = "Inline.pdf",
        signatureQualifier = "eu_eidas_qes",
        responseUri = null,
        signatureFormat = null,
        conformanceLevel = null,
        signedProperties = null,
        href = null,
        checksum = null,
        oneTimePassword = null,
    )

    //endregion
}