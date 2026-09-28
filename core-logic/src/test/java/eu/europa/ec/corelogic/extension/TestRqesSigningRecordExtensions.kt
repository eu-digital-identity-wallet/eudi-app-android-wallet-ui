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

import eu.europa.ec.corelogic.model.InteractingPartyDomain
import eu.europa.ec.corelogic.model.LocalizedTextDomain
import eu.europa.ec.corelogic.model.TransactionLogDomain
import eu.europa.ec.corelogic.model.TransactionResultDomain
import eu.europa.ec.corelogic.util.mockedEnglishLocale
import eu.europa.ec.corelogic.util.mockedTransactionId
import eu.europa.ec.corelogic.util.mockedTransactionTime
import eu.europa.ec.eudi.rqes.core.RqesSigningRecord
import eu.europa.ec.eudi.wallet.transactionLogging.model.MultiLangString
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionEntry
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionResult
import eu.europa.ec.eudi.wallet.transactionLogging.toJson
import eu.europa.ec.eudi.wallet.transactionLogging.toTransactionEntryOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class TestRqesSigningRecordExtensions {

    //region toSigningEntries

    // Case 1:
    // 1. A completed signing record contains all available metadata.
    //
    // Case 1 Expected Result:
    // One entry preserves the metadata, language and time, with separate operation and entry IDs.
    @Test
    fun `Given a completed record, When toSigningEntries is called, Then all recorded metadata is preserved`() {
        // Given
        val record = mockedRecord
        val ids = mockedSingleDocumentIds.iterator()

        // When
        val entries = record.toSigningEntries(
            idProvider = ids::next,
            time = mockedTransactionTime,
        )

        // Then
        assertEquals(listOf(mockedEntry), entries)
        assertFalse(ids.hasNext())
    }

    // Case 2:
    // 1. Failed records contain a reason or no reason.
    //
    // Case 2 Expected Result:
    // Every entry remains failed with the supplied reason and attempted file, with a fresh operation ID.
    @Test
    fun `Given failed records, When toSigningEntries is called, Then nullable reasons and attempted documents are preserved`() {
        // Given
        val reasons = listOf(mockedFailureReason, null)
        val ids = mockedTwoAttemptIds.iterator()

        // When
        val entries = reasons.map { reason ->
            mockedRecord.copy(outcome = RqesSigningRecord.Outcome.Failed(reason))
                .toSigningEntries(
                    idProvider = ids::next,
                    time = mockedTransactionTime,
                ).single()
        }

        // Then
        assertEquals(
            listOf(
                TransactionResult.NotCompleted(mockedFailureReason),
                TransactionResult.NotCompleted(null),
            ),
            entries.map { entry -> entry.transactionResult },
        )
        assertEquals(listOf(mockedFileName, mockedFileName), entries.map { entry -> entry.fileName })
        assertEquals(
            listOf(mockedSigningTransactionId, mockedOtherSigningTransactionId),
            entries.map { entry -> entry.signingTransactionIdentifier },
        )
        assertEquals(mockedEntryIds.take(2), entries.map { entry -> entry.transactionIdentifier })
        assertFalse(ids.hasNext())
    }

    // Case 3:
    // 1. A record has no optional metadata and its document label is empty.
    //
    // Case 3 Expected Result:
    // Only local operation and entry IDs are added; absent optional metadata remains absent.
    @Test
    fun `Given missing optional metadata, When toSigningEntries is called, Then only local identifiers are added`() {
        // Given
        val record = mockedRecord.copy(
            certificateSerialNumber = null,
            documents = listOf(
                RqesSigningRecord.SignedDocument(
                    label = "",
                    dtbsr = null,
                    sizeBytes = null
                )
            ),
            serviceName = null,
        )
        val ids = mockedSingleDocumentIds.iterator()

        // When
        val entry = record.toSigningEntries(
            idProvider = ids::next,
            time = mockedTransactionTime,
        ).single()

        // Then
        assertNull(entry.certificateIdentifier)
        assertNull(entry.dtbsr)
        assertNull(entry.fileSize)
        assertNull(entry.interactingPartyName)
        assertEquals(mockedSigningTransactionId, entry.signingTransactionIdentifier)
        assertEquals(mockedTransactionId, entry.transactionIdentifier)
        assertNull(entry.fileIdentifier)
        assertNull(entry.interactingPartyIdentifier)
        assertNull(entry.interactingPartyContact)
        assertEquals("", entry.fileName)
        assertEquals(
            TransactionEntry.SigningSealing.INTERACTING_PARTY_TYPE,
            entry.interactingPartyType
        )
    }

    // Case 4:
    // 1. Three documents include repeated names and digests.
    //
    // Case 4 Expected Result:
    // Three entries retain order and time, sharing one operation ID with separate entry IDs.
    @Test
    fun `Given repeated document metadata, When toSigningEntries is called, Then entries retain order and distinct identities`() {
        // Given
        val record = mockedRecord.copy(
            documents = listOf(
                mockedDocument,
                mockedDocument,
                mockedDocument.copy(label = mockedOtherFileName),
            ),
        )
        val ids = (listOf(mockedSigningTransactionId) + mockedEntryIds).iterator()

        // When
        val entries = record.toSigningEntries(
            idProvider = ids::next,
            time = mockedTransactionTime,
        )

        // Then
        assertEquals(mockedEntryIds, entries.map { entry -> entry.transactionIdentifier })
        assertEquals(
            listOf(mockedSigningTransactionId, mockedSigningTransactionId, mockedSigningTransactionId),
            entries.map { entry -> entry.signingTransactionIdentifier },
        )
        assertEquals(
            listOf(mockedFileName, mockedFileName, mockedOtherFileName),
            entries.map { entry -> entry.fileName })
        assertEquals(listOf(mockedDigest, mockedDigest, mockedDigest), entries.map { entry -> entry.dtbsr })
        assertEquals(
            listOf(mockedTransactionTime, mockedTransactionTime, mockedTransactionTime),
            entries.map { entry -> entry.time })
        assertFalse(ids.hasNext())
    }

    // Case 5:
    // 1. A completed callback contains no documents but has certificate and service metadata.
    //
    // Case 5 Expected Result:
    // One entry retains the outcome and metadata, with fresh IDs and null document fields.
    @Test
    fun `Given no documents, When toSigningEntries is called, Then the signing outcome is still recorded`() {
        // Given
        val record = mockedRecord.copy(documents = emptyList())
        val ids = mockedSingleDocumentIds.iterator()

        // When
        val entries = record.toSigningEntries(
            idProvider = ids::next,
            time = mockedTransactionTime,
        )

        // Then
        assertEquals(
            listOf(mockedEntry.copy(dtbsr = null, fileName = null, fileSize = null)),
            entries,
        )
        assertEquals(entries.single(), entries.single().toJson().toTransactionEntryOrNull())
        assertFalse(ids.hasNext())
    }

    // Case 6:
    // 1. Documents have zero and maximum Long byte sizes.
    //
    // Case 6 Expected Result:
    // Sizes remain lossless decimal strings.
    @Test
    fun `Given boundary file sizes, When toSigningEntries is called, Then byte sizes remain exact`() {
        // Given
        val record = mockedRecord.copy(
            documents = listOf(
                mockedDocument.copy(sizeBytes = 0L),
                mockedDocument.copy(sizeBytes = Long.MAX_VALUE),
            ),
        )
        val ids = (listOf(mockedSigningTransactionId) + mockedEntryIds.take(2)).iterator()

        // When
        val entries = record.toSigningEntries(idProvider = ids::next, time = mockedTransactionTime)

        // Then
        assertEquals(listOf("0", "9223372036854775807"), entries.map { entry -> entry.fileSize })
    }

    // Case 7:
    // 1. A mapped entry is serialized and read through the existing transaction domain mapper.
    //
    // Case 7 Expected Result:
    // Storage preserves the complete entry, and the domain retains the signing display fields.
    @Test
    fun `Given a mapped signing, When stored and read, Then the existing signing domain preserves its content`() {
        // Given
        val ids = mockedSingleDocumentIds.iterator()
        val entry = mockedRecord.toSigningEntries(
            idProvider = ids::next,
            time = mockedTransactionTime,
        ).single()

        // When
        val restored = entry.toJson().toTransactionEntryOrNull()
        val domain = restored?.toTransactionLogDomain(
            id = mockedTransactionId,
            userLocale = mockedEnglishLocale,
            parentPresentationId = null,
            communicationMethod = null,
            transactionDataTypes = emptyList(),
        )

        // Then
        assertEquals(entry, restored)
        assertEquals(
            TransactionLogDomain.SigningSealing(
                id = mockedTransactionId,
                time = mockedTransactionTime.atZone(ZoneId.systemDefault()).toLocalDateTime(),
                result = TransactionResultDomain.Completed,
                service = InteractingPartyDomain(
                    name = LocalizedTextDomain(
                        mockedServiceName.languageTag,
                        mockedServiceName.name
                    ),
                    identifier = null,
                    contacts = emptyList(),
                ),
                serviceType = TransactionEntry.SigningSealing.INTERACTING_PARTY_TYPE,
                signingTransactionId = mockedSigningTransactionId,
                certificateSerialNumber = mockedCertificate,
                fileName = mockedFileName,
                fileSizeBytes = mockedFileSize,
                dtbsr = mockedDigest,
            ),
            domain,
        )
    }

    // Case 8:
    // 1. Identical records arrive as separate callbacks with different receipt times.
    //
    // Case 8 Expected Result:
    // Each callback uses its supplied time and receives fresh operation and entry IDs.
    @Test
    fun `Given separate callbacks, When toSigningEntries is called, Then attempts do not share an identity`() {
        // Given
        val ids = mockedTwoAttemptIds.iterator()
        val laterTime = mockedTransactionTime.plusSeconds(1)

        // When
        val first =
            mockedRecord.toSigningEntries(idProvider = ids::next, time = mockedTransactionTime)
                .single()
        val second =
            mockedRecord.toSigningEntries(idProvider = ids::next, time = laterTime).single()

        // Then
        assertEquals(
            mockedEntryIds.take(2),
            listOf(first.transactionIdentifier, second.transactionIdentifier)
        )
        assertEquals(
            listOf(mockedSigningTransactionId, mockedOtherSigningTransactionId),
            listOf(first.signingTransactionIdentifier, second.signingTransactionIdentifier),
        )
        assertEquals(listOf(mockedTransactionTime, laterTime), listOf(first.time, second.time))
        assertFalse(ids.hasNext())
    }

    // Case 9:
    // 1. Failed callbacks contain a reason or no reason, without documents or optional metadata.
    //
    // Case 9 Expected Result:
    // Each callback records one failed entry with fresh IDs and no invented document metadata.
    @Test
    fun `Given failures without documents, When toSigningEntries is called, Then nullable reasons are still recorded`() {
        // Given
        val reasons = listOf(mockedFailureReason, null)
        val record = mockedRecord.copy(
            documents = emptyList(),
            certificateSerialNumber = null,
            serviceName = null,
        )
        val ids = mockedTwoAttemptIds.iterator()

        // When
        val entries = reasons.map { reason ->
            record.copy(outcome = RqesSigningRecord.Outcome.Failed(reason))
                .toSigningEntries(
                    idProvider = ids::next,
                    time = mockedTransactionTime,
                ).single()
        }

        // Then
        val expectedEntry = mockedEntry.copy(
            transactionResult = TransactionResult.NotCompleted(mockedFailureReason),
            certificateIdentifier = null,
            dtbsr = null,
            fileName = null,
            fileSize = null,
            interactingPartyName = null,
        )
        assertEquals(
            listOf(
                expectedEntry,
                expectedEntry.copy(
                    transactionIdentifier = mockedEntryIds[1],
                    signingTransactionIdentifier = mockedOtherSigningTransactionId,
                    transactionResult = TransactionResult.NotCompleted(null),
                ),
            ),
            entries,
        )
        assertFalse(ids.hasNext())
    }

    //endregion

    //region helper data

    private val mockedFileName = "agreement.pdf"
    private val mockedOtherFileName = "annex.pdf"
    private val mockedDigest = "AAECA/8="
    private val mockedCertificate = "00A12F"
    private val mockedFileSize = 12345L
    private val mockedFailureReason = "Signing was declined"
    private val mockedEntryIds = listOf(mockedTransactionId, "second-entry", "third-entry")
    private val mockedSigningTransactionId = "signing-transaction-id"
    private val mockedOtherSigningTransactionId = "other-signing-transaction-id"
    private val mockedSingleDocumentIds = listOf(mockedSigningTransactionId, mockedTransactionId)
    private val mockedTwoAttemptIds = listOf(
        mockedSigningTransactionId,
        mockedEntryIds[0],
        mockedOtherSigningTransactionId,
        mockedEntryIds[1],
    )
    private val mockedServiceName = RqesSigningRecord.LocalizedName(
        languageTag = "el-GR",
        name = "Πάροχος υπογραφής",
    )
    private val mockedDocument = RqesSigningRecord.SignedDocument(
        label = mockedFileName,
        dtbsr = mockedDigest,
        sizeBytes = mockedFileSize,
    )
    private val mockedRecord = RqesSigningRecord(
        outcome = RqesSigningRecord.Outcome.Completed,
        certificateSerialNumber = mockedCertificate,
        documents = listOf(mockedDocument),
        serviceName = mockedServiceName,
    )
    private val mockedEntry = TransactionEntry.SigningSealing(
        transactionIdentifier = mockedTransactionId,
        signingTransactionIdentifier = mockedSigningTransactionId,
        time = mockedTransactionTime,
        transactionResult = TransactionResult.Completed,
        certificateIdentifier = mockedCertificate,
        dtbsr = mockedDigest,
        fileName = mockedFileName,
        fileSize = "12345",
        interactingPartyName = MultiLangString(
            mockedServiceName.languageTag,
            mockedServiceName.name
        ),
    )

    //endregion
}