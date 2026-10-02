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

package eu.europa.ec.dashboardfeature.interactor

import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.businesslogic.util.FULL_DATETIME_PATTERN
import eu.europa.ec.businesslogic.util.formatLocalDateTime
import eu.europa.ec.corelogic.controller.RecordTransactionPartialState
import eu.europa.ec.corelogic.controller.WalletCoreTransactionLogController
import eu.europa.ec.corelogic.controller.WalletCoreTransactionRecordingController
import eu.europa.ec.corelogic.model.ClaimPathSegment
import eu.europa.ec.corelogic.model.ClaimRefDomain
import eu.europa.ec.corelogic.model.CommunicationMethodDomain
import eu.europa.ec.corelogic.model.CredentialClaimsDomain
import eu.europa.ec.corelogic.model.CredentialRefDomain
import eu.europa.ec.corelogic.model.DpaContactDomain
import eu.europa.ec.corelogic.model.InteractingPartyDomain
import eu.europa.ec.corelogic.model.LocalizedTextDomain
import eu.europa.ec.corelogic.model.PresentationTransactionDataDomain
import eu.europa.ec.corelogic.model.TransactionLogDomain
import eu.europa.ec.corelogic.model.TransactionResultDomain
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.PendingTransactionActionUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.PresentationActionCountsUiState
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDataProtectionAction
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsBodyUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsFieldUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsUi
import eu.europa.ec.dashboardfeature.util.mockedDataDeletionLogDomain
import eu.europa.ec.dashboardfeature.util.mockedDeletionLogDomain
import eu.europa.ec.dashboardfeature.util.mockedDetailedPresentationLogDomain
import eu.europa.ec.dashboardfeature.util.mockedDpaReportLogDomain
import eu.europa.ec.dashboardfeature.util.mockedIssuanceDetails
import eu.europa.ec.dashboardfeature.util.mockedIssuanceLogDomain
import eu.europa.ec.dashboardfeature.util.mockedNestedTransactionClaims
import eu.europa.ec.dashboardfeature.util.mockedNotCompletedTransactionLogDomains
import eu.europa.ec.dashboardfeature.util.mockedOtherTransactionCredential
import eu.europa.ec.dashboardfeature.util.mockedPresentationLogDomain
import eu.europa.ec.dashboardfeature.util.mockedReissuanceLogDomain
import eu.europa.ec.dashboardfeature.util.mockedSigningLogDomain
import eu.europa.ec.dashboardfeature.util.mockedTransactionClaimPath
import eu.europa.ec.dashboardfeature.util.mockedTransactionClaims
import eu.europa.ec.dashboardfeature.util.mockedTransactionCredential
import eu.europa.ec.dashboardfeature.util.mockedTransactionDetailsStrings
import eu.europa.ec.dashboardfeature.util.mockedTransactionIntermediary
import eu.europa.ec.dashboardfeature.util.mockedTransactionLanguageTag
import eu.europa.ec.dashboardfeature.util.mockedTransactionLogDomains
import eu.europa.ec.dashboardfeature.util.mockedTransactionParty
import eu.europa.ec.dashboardfeature.util.mockedTransactionPartyName
import eu.europa.ec.dashboardfeature.util.mockedTransactionPartyNames
import eu.europa.ec.dashboardfeature.util.mockedTransactionPartyWithContacts
import eu.europa.ec.dashboardfeature.util.mockedTransactionQualifiedIdentifier
import eu.europa.ec.dashboardfeature.util.mockedTransactionRegistration
import eu.europa.ec.dashboardfeature.util.mockedTransactionTypeLabels
import eu.europa.ec.dashboardfeature.util.mockedUnnamedTransactionLogDomains
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.StringResourceProviderMocker.mockTransactionDataStrings
import eu.europa.ec.testfeature.util.mockedExceptionWithMessage
import eu.europa.ec.testfeature.util.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.util.mockedGenericErrorMessage
import eu.europa.ec.testfeature.util.mockedTransactionDataApproval
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemLeadingContentDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.net.URI
import java.net.URLDecoder
import java.time.Instant
import java.time.LocalDateTime
import java.util.Locale

class TestTransactionDetailsInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var walletCoreTransactionLogController: WalletCoreTransactionLogController

    @Mock
    private lateinit var walletCoreTransactionRecordingController: WalletCoreTransactionRecordingController

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var uuidProvider: UuidProvider

    private lateinit var interactor: TransactionDetailsInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = TransactionDetailsInteractorImpl(
            walletCoreTransactionLogController = walletCoreTransactionLogController,
            walletCoreTransactionRecordingController = walletCoreTransactionRecordingController,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
        )

        whenever(resourceProvider.genericErrorMessage()).thenReturn(mockedGenericErrorMessage)
        mockTransactionDetailsStrings()
        mockTransactionDataStrings(resourceProvider = resourceProvider)
        whenever(uuidProvider.provideUuid()).thenReturn(mockedAttemptId)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region getTransactionDetails

    // Case 1:
    // 1. walletCoreTransactionLogController.getTransactionLog returns a CredentialIssuance with
    //    result = TransactionResultDomain.Completed.

    // Case 1 Expected Result:
    // Success with a TransactionDetailsCardUi where transactionIsCompleted is true,
    // the issuer name, both recorded counts and issued credential identifier are shown without a trigger.
    @Test
    fun `Given Case 1, When getTransactionDetails is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val transaction = mockedIssuanceLogDomain.copy(
                id = mockedTransactionId,
                result = TransactionResultDomain.Completed,
                time = mockedCreationLocalDateTime,
            )
            whenever(walletCoreTransactionLogController.getTransactionLog(id = mockedTransactionId))
                .thenReturn(transaction)

            // When
            interactor.getTransactionDetails(transactionId = mockedTransactionId).runFlowTest {
                // Then
                val details =
                    (awaitItem() as TransactionDetailsInteractorPartialState.Success).transactionDetailsUi
                val card = details.transactionDetailsCardUi
                assertEquals(mockedTransactionId, details.transactionId)
                assertTrue(card.transactionIsCompleted)
                assertEquals(mockedIssuanceLabel, card.transactionTypeLabel)
                assertEquals(mockedCompletedLabel, card.transactionStatusLabel)
                assertEquals(mockedIssuanceDetails.issuer.name?.text, card.partyName)
                assertEquals(
                    mockedCreationLocalDateTime.formatLocalDateTime(pattern = FULL_DATETIME_PATTERN),
                    card.transactionDate,
                )
                assertNull(card.nonCompletionReason)
                val body = details.body as TransactionDetailsBodyUi.Issuance
                assertEquals("PIDProvider", card.providerType)
                assertEquals(
                    listOf(
                        TransactionDetailsFieldUi(
                            "requested-count",
                            "Credentials requested",
                            "1",
                            null
                        ),
                        TransactionDetailsFieldUi("issued-count", "Credentials issued", "1", null),
                    ),
                    card.metadata.single().fields,
                )
                assertEquals("CREDENTIALS ISSUED", body.credentials!!.title)
                assertEquals(
                    mockedTransactionCredential.identifier,
                    body.credentials.items.single().item.textValue()
                )
            }
        }
    }

    // Case 2:
    // 1. walletCoreTransactionLogController.getTransactionLog returns a CredentialIssuance with
    //    result = TransactionResultDomain.NotCompleted (mapped to TransactionStatusUi.NotCompleted).

    // Case 2 Expected Result:
    // Success with transactionIsCompleted = false and the Not completed status label and no invented reason.
    @Test
    fun `Given Case 2, When getTransactionDetails is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val transaction = mockedIssuanceLogDomain.copy(
                id = mockedTransactionId,
                result = TransactionResultDomain.NotCompleted(reason = null),
                time = mockedCreationLocalDateTime,
            )
            whenever(walletCoreTransactionLogController.getTransactionLog(id = mockedTransactionId))
                .thenReturn(transaction)

            // When
            interactor.getTransactionDetails(transactionId = mockedTransactionId).runFlowTest {
                // Then
                val result = awaitItem()
                assertTrue(result is TransactionDetailsInteractorPartialState.Success)
                result as TransactionDetailsInteractorPartialState.Success

                val card = result.transactionDetailsUi.transactionDetailsCardUi
                assertEquals(false, card.transactionIsCompleted)
                assertEquals(mockedNotCompletedLabel, card.transactionStatusLabel)
                assertNull(card.nonCompletionReason)
                assertEquals(mockedIssuanceLabel, card.transactionTypeLabel)
            }
        }
    }

    // Case 3:
    // 1. walletCoreTransactionLogController.getTransactionLog returns a Presentation.

    // Case 3 Expected Result:
    // Success with the presentation label and party name, without a verified badge or claim values.
    @Test
    fun `Given Case 3, When getTransactionDetails is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val transaction = mockedPresentationLogDomain.copy(
                id = mockedTransactionId,
                result = TransactionResultDomain.Completed,
                time = mockedCreationLocalDateTime,
            )
            whenever(walletCoreTransactionLogController.getTransactionLog(id = mockedTransactionId))
                .thenReturn(transaction)

            // When
            interactor.getTransactionDetails(transactionId = mockedTransactionId).runFlowTest {
                // Then
                val result = awaitItem()
                assertTrue(result is TransactionDetailsInteractorPartialState.Success)
                result as TransactionDetailsInteractorPartialState.Success

                val card = result.transactionDetailsUi.transactionDetailsCardUi
                assertEquals(mockedPresentationLabel, card.transactionTypeLabel)
                assertEquals(mockedTransactionPartyName, card.partyName)
                val body = result.transactionDetailsUi.body as TransactionDetailsBodyUi.Presentation
                assertEquals("DATA REQUESTED", body.requested.title)
                assertEquals("DATA SHARED", body.shared.title)
                assertEquals(
                    mockedTransactionCredential.identifier,
                    body.requested.groups.single().header.textValue()
                )
                assertEquals(
                    mockedTransactionCredential.identifier,
                    body.shared.groups.single().header.textValue()
                )
                assertEquals(
                    mockedTransactionClaimPath,
                    body.requested.groups.single().items.single().header.textValue()
                )
                assertEquals(
                    mockedTransactionClaimPath,
                    body.shared.groups.single().items.single().header.textValue()
                )
                assertTrue(body.requested.groups.single().header.itemId != body.shared.groups.single().header.itemId)
                assertEquals(
                    ListItemTrailingContentDataUi.Icon(AppIcons.KeyboardArrowDown),
                    body.requested.groups.single().header.trailingContentData,
                )
                assertEquals(
                    ListItemTrailingContentDataUi.Icon(AppIcons.KeyboardArrowDown),
                    body.shared.groups.single().header.trailingContentData,
                )
                assertTrue(card.metadata.isEmpty())
                assertEquals("ServiceProvider", card.providerType)
            }
        }
    }


    // Case 5:
    // 1. walletCoreTransactionLogController.getTransactionLog returns a SigningSealing.
    //
    // Case 5 Expected Result:
    // Success with the service name and a plain filename; technical metadata is omitted.
    @Test
    fun `Given Case 5, When getTransactionDetails is called, Then Case 5 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val transaction = mockedSigningLogDomain.copy(
                id = mockedTransactionId,
                result = TransactionResultDomain.Completed,
                time = mockedCreationLocalDateTime,
            )
            whenever(walletCoreTransactionLogController.getTransactionLog(id = mockedTransactionId))
                .thenReturn(transaction)

            // When
            interactor.getTransactionDetails(transactionId = mockedTransactionId).runFlowTest {
                // Then
                val result = awaitItem()
                assertTrue(result is TransactionDetailsInteractorPartialState.Success)
                result as TransactionDetailsInteractorPartialState.Success

                val card = result.transactionDetailsUi.transactionDetailsCardUi
                assertEquals(mockedSigningLabel, card.transactionTypeLabel)
                assertEquals(mockedSigningLogDomain.service.name?.text, card.partyName)
                val body = result.transactionDetailsUi.body as TransactionDetailsBodyUi.Signing
                assertEquals("ESigESealCreationProvider", card.providerType)
                assertTrue(card.metadata.isEmpty())
                val filename = body.document!!.items.single()
                assertEquals("signed.pdf", filename.item.textValue())
                assertNull(filename.url)
                assertNull(filename.item.trailingContentData)
                assertEquals(listOf(body.document), body.sections)
            }
        }
    }

    // Case 6:
    // 1. walletCoreTransactionLogController.getTransactionLog returns null (no transaction for that id).

    // Case 6 Expected Result:
    // Failure with the generic error message.
    @Test
    fun `Given Case 6, When getTransactionDetails is called, Then Case 6 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreTransactionLogController.getTransactionLog(id = mockedTransactionId))
                .thenReturn(null)

            // When
            interactor.getTransactionDetails(transactionId = mockedTransactionId).runFlowTest {
                // Then
                assertEquals(
                    TransactionDetailsInteractorPartialState.Failure(
                        error = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 7:
    // 1. walletCoreTransactionLogController.getTransactionLog throws an exception with a message.

    // Case 7 Expected Result:
    // Failure with the thrown exception's localized message.
    @Test
    fun `Given Case 7, When getTransactionDetails is called, Then Case 7 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreTransactionLogController.getTransactionLog(id = mockedTransactionId))
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.getTransactionDetails(transactionId = mockedTransactionId).runFlowTest {
                // Then
                assertEquals(
                    TransactionDetailsInteractorPartialState.Failure(
                        error = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 8:
    // 1. walletCoreTransactionLogController.getTransactionLog throws an exception with no message.

    // Case 8 Expected Result:
    // Failure with the generic error message.
    @Test
    fun `Given Case 8, When getTransactionDetails is called, Then Case 8 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreTransactionLogController.getTransactionLog(id = mockedTransactionId))
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.getTransactionDetails(transactionId = mockedTransactionId).runFlowTest {
                // Then
                assertEquals(
                    TransactionDetailsInteractorPartialState.Failure(
                        error = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 9:
    // 1. Each supported transaction is requested by its stored id.
    //
    // Case 9 Expected Result:
    // All types return a dated status card without a verified badge or claim values.
    @Test
    fun `Given every supported transaction type, When getTransactionDetails is called, Then each card renders`() {
        coroutineRule.runTest {
            // Given

            mockedTransactionLogDomains.forEachIndexed { index, transaction ->
                mockGetTransactionLogCall(response = transaction)

                // When
                interactor.getTransactionDetails(transactionId = transaction.id).runFlowTest {
                    // Then
                    val details =
                        (awaitItem() as TransactionDetailsInteractorPartialState.Success).transactionDetailsUi
                    val card = details.transactionDetailsCardUi
                    assertEquals(transaction.id, details.transactionId)
                    assertEquals(mockedTransactionTypeLabels[index], card.transactionTypeLabel)
                    assertEquals(mockedCompletedLabel, card.transactionStatusLabel)
                    assertTrue(card.transactionIsCompleted)
                    assertEquals(
                        transaction.time.formatLocalDateTime(pattern = FULL_DATETIME_PATTERN),
                        card.transactionDate,
                    )
                    assertEquals(mockedTransactionPartyNames[index], card.partyName)
                    assertNull(card.nonCompletionReason)
                    assertTrue(details.body.sections.isNotEmpty())
                    assertTrue(details.body.sections.all { section -> section.title.isNotBlank() })
                }
            }
        }
    }

    // Case 10:
    // 1. Requested and shared credentials differ, and requested claims use nested paths.
    //
    // Case 10 Expected Result:
    // Groups stay separate; typed paths and raw credential identifiers remain readable.
    @Test
    fun `Given Case 10, When getTransactionDetails is called, Then Case 10 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val transaction = mockedPresentationLogDomain.copy(
                claimsRequested = mockedNestedTransactionClaims,
                claimsPresented = mockedTransactionClaims,
            )
            mockGetTransactionLogCall(transaction)

            // When
            interactor.getTransactionDetails(transaction.id).runFlowTest {
                // Then
                val body = (awaitItem() as TransactionDetailsInteractorPartialState.Success)
                    .transactionDetailsUi.body as TransactionDetailsBodyUi.Presentation
                val requested = body.requested.groups.single()
                assertEquals(
                    mockedOtherTransactionCredential.identifier,
                    requested.header.textValue()
                )
                assertEquals(
                    listOf(
                        "[\"addresses\"][0][\"street.name\"]",
                        "[\"addresses\"][*]",
                        "Attribute identifier unavailable"
                    ),
                    requested.items.map { row -> row.header.textValue() },
                )
                assertEquals(
                    mockedTransactionClaimPath,
                    body.shared.groups.single().items.single().header.textValue()
                )
                assertTrue(requested.items.all { row -> row.header.overlineText == null && row.header.trailingContentData == null })
            }
        }
    }

    // Case 11:
    // 1. A presentation carries complete party, registration and authority metadata.
    //
    // Case 11 Expected Result:
    // Only approved metadata is in the card, in order; authority contacts remain available to reporting.
    @Test
    fun `Given Case 11, When getTransactionDetails is called, Then Case 11 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockGetTransactionLogCall(mockedDetailedPresentationLogDomain)

            // When
            interactor.getTransactionDetails(mockedDetailedPresentationLogDomain.id).runFlowTest {
                // Then
                val details =
                    (awaitItem() as TransactionDetailsInteractorPartialState.Success).transactionDetailsUi
                val body = details.body as TransactionDetailsBodyUi.Presentation
                val groups = details.transactionDetailsCardUi.metadata
                assertEquals(
                    listOf(
                        listOf("purpose"),
                        listOf("privacy:0", "privacy:1"),
                        listOf("party:contact:0", "party:contact:1", "party:contact:2"),
                    ),
                    groups.map { group -> group.fields.map { field -> field.id } },
                )
                val fields =
                    groups.flatMap { group -> group.fields }.associateBy { field -> field.id }
                assertEquals(
                    mockedTransactionRegistration.purpose,
                    fields.getValue("purpose").value
                )
                assertNull(fields.getValue("purpose").url)
                assertNull(fields.getValue("party:contact:0").url)
                assertEquals(
                    mockedTransactionRegistration.privacyPolicyUrls,
                    groups[1].fields.map { field -> field.url })
                assertEquals(
                    listOf(
                        "mailto:authority@example.com",
                        "tel:+302101234567",
                        "https://example.com/authority"
                    ),
                    body.reportContacts.map { contact -> contact.url },
                )
                assertEquals(listOf(body.requested, body.shared), body.sections)
            }
        }
    }

    // Case 12:
    // 1. Registration URLs and contacts include unsupported schemes, malformed URLs and country text.
    //
    // Case 12 Expected Result:
    // Unusable links remain plain text and blank contact fields are omitted.
    @Test
    fun `Given Case 12, When getTransactionDetails is called, Then Case 12 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val invalidLinks = listOf(
                "javascript:alert(1)", "file:///data/private", "intent://launch",
                "https://", "/relative/path", "GR", "mailto:not-an-email",
            )
            val transaction = mockedDetailedPresentationLogDomain.copy(
                party = mockedTransactionPartyWithContacts.copy(contacts = invalidLinks + " "),
                registration = mockedTransactionRegistration.copy(
                    registrarUrl = invalidLinks.first(),
                    privacyPolicyUrls = invalidLinks,
                    dpa = DpaContactDomain(null, null, invalidLinks),
                ),
            )
            mockGetTransactionLogCall(transaction)

            // When
            interactor.getTransactionDetails(transaction.id).runFlowTest {
                // Then
                val details =
                    (awaitItem() as TransactionDetailsInteractorPartialState.Success).transactionDetailsUi
                val fields =
                    details.transactionDetailsCardUi.metadata.flatMap { group -> group.fields }
                assertTrue(fields.all { field -> field.url == null })
                assertEquals(
                    invalidLinks,
                    fields.filter { field -> field.id.startsWith("party:contact:") }
                        .map { field -> field.value })
                assertTrue(fields.none { field -> field.id.startsWith("authority:") || field.id == "registrar" })
            }
        }
    }

    // Case 13:
    // 1. Each transaction type has a missing or blank party name.
    //
    // Case 13 Expected Result:
    // Cards omit blank names; signing omits the document section when its filename is blank.
    @Test
    fun `Given Case 13, When getTransactionDetails is called, Then Case 13 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockedUnnamedTransactionLogDomains.forEach { transaction ->
                mockGetTransactionLogCall(transaction)

                // When
                interactor.getTransactionDetails(transaction.id).runFlowTest {
                    // Then
                    val details =
                        (awaitItem() as TransactionDetailsInteractorPartialState.Success).transactionDetailsUi
                    assertNull(details.transactionDetailsCardUi.partyName)
                    if (transaction is TransactionLogDomain.SigningSealing) {
                        assertTrue(details.body.sections.isEmpty())
                    }
                    details.body.sections.forEach { section ->
                        if (section.items.isEmpty() && section.groups.isEmpty()) {
                            assertTrue(section.emptyItem?.textValue()?.isNotBlank() == true)
                        } else {
                            assertNull(section.emptyItem)
                        }
                    }
                    assertTrue(details.body.sections.flatMap { section -> section.items }
                        .all { row -> row.item.textValue().isNotBlank() })
                }
            }
        }
    }

    // Case 14:
    // 1. Issuance and re-issuance have all three trigger values and a partial batch.
    //
    // Case 14 Expected Result:
    // Both counts precede the reissuance-only trigger; one issued list has no inferred per-item outcomes.
    @Test
    fun `Given Case 14, When getTransactionDetails is called, Then Case 14 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            listOf(true, false, null).forEach { trigger ->
                val details = mockedIssuanceDetails.copy(
                    requestedCount = 3, issuedCount = 1, isUserTriggered = trigger,
                )
                val transactions = listOf(
                    mockedIssuanceLogDomain.copy(details = details),
                    mockedReissuanceLogDomain.copy(details = details),
                )
                transactions.forEachIndexed { index, transaction ->
                    mockGetTransactionLogCall(transaction)

                    // When
                    interactor.getTransactionDetails(transaction.id).runFlowTest {
                        // Then
                        val result =
                            (awaitItem() as TransactionDetailsInteractorPartialState.Success).transactionDetailsUi
                        val fields =
                            result.transactionDetailsCardUi.metadata.flatMap { group -> group.fields }
                        val expectedTrigger = if (index == 0) null else when (trigger) {
                            true -> "Wallet holder"
                            false -> "The Wallet"
                            null -> null
                        }
                        assertEquals(
                            expectedTrigger,
                            fields.singleOrNull { field -> field.id == "trigger" }?.value,
                        )
                        assertEquals(
                            listOf(
                                TransactionDetailsFieldUi(
                                    "requested-count",
                                    "Credentials requested",
                                    "3",
                                    null
                                ),
                                TransactionDetailsFieldUi(
                                    "issued-count",
                                    "Credentials issued",
                                    "1",
                                    null
                                ),
                            ),
                            result.transactionDetailsCardUi.metadata.first().fields,
                        )
                        assertEquals("CREDENTIALS ISSUED", result.body.sections.single().title)
                        val credential = result.body.sections.single().items.single()
                        assertEquals(
                            mockedTransactionCredential.identifier,
                            credential.item.textValue()
                        )
                        assertNull(credential.item.supportingContentData)
                        assertNull(credential.item.trailingContentData)
                    }
                }
            }
        }
    }

    // Case 15:
    // 1. Every transaction type is not completed, with an optional reason.
    //
    // Case 15 Expected Result:
    // Recorded reasons are preserved; missing or blank reasons stay absent.
    @Test
    fun `Given Case 15, When getTransactionDetails is called, Then Case 15 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockedNotCompletedTransactionLogDomains.forEach { transaction ->
                mockGetTransactionLogCall(transaction)

                // When
                interactor.getTransactionDetails(transaction.id).runFlowTest {
                    // Then
                    val card = (awaitItem() as TransactionDetailsInteractorPartialState.Success)
                        .transactionDetailsUi.transactionDetailsCardUi
                    assertEquals(false, card.transactionIsCompleted)
                    assertEquals("Not completed", card.transactionStatusLabel)
                    val reason = (transaction.result as TransactionResultDomain.NotCompleted).reason
                    assertEquals(
                        reason?.takeIf { value -> value.isNotBlank() },
                        card.nonCompletionReason
                    )
                }
            }
        }
    }

    // Case 16:
    // 1. A deletion refers to a removed credential and still carries issuer identity.
    //
    // Case 16 Expected Result:
    // The original issuer name and raw credential stay visible; issuer identifiers and contacts are omitted.
    @Test
    fun `Given Case 16, When getTransactionDetails is called, Then Case 16 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val transaction = mockedDeletionLogDomain.copy(
                credential = mockedOtherTransactionCredential,
                issuer = mockedTransactionPartyWithContacts,
            )
            mockGetTransactionLogCall(transaction)

            // When
            interactor.getTransactionDetails(transaction.id).runFlowTest {
                // Then
                val details =
                    (awaitItem() as TransactionDetailsInteractorPartialState.Success).transactionDetailsUi
                val body = details.body as TransactionDetailsBodyUi.Deletion
                assertEquals("CREDENTIALS", body.credential!!.title)
                assertEquals(
                    mockedOtherTransactionCredential.identifier,
                    body.credential.items.single().item.textValue()
                )
                assertEquals(
                    transaction.issuer.name?.text,
                    details.transactionDetailsCardUi.partyName
                )
                assertNull(details.transactionDetailsCardUi.providerType)
                assertTrue(details.transactionDetailsCardUi.metadata.isEmpty())
                assertEquals(listOf(body.credential), body.sections)
            }
        }
    }

    // Case 17:
    // 1. Signing metadata has a digest and zero, missing, negative or large file sizes.
    //
    // Case 17 Expected Result:
    // Only the recorded filename is displayed, regardless of technical metadata.
    @Test
    fun `Given Case 17, When getTransactionDetails is called, Then Case 17 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            listOf(0L, null, -1L, Long.MAX_VALUE).forEach { size ->
                val transaction =
                    mockedSigningLogDomain.copy(fileSizeBytes = size, dtbsr = "c2lnbmVk")
                mockGetTransactionLogCall(transaction)

                // When
                interactor.getTransactionDetails(transaction.id).runFlowTest {
                    // Then
                    val details =
                        (awaitItem() as TransactionDetailsInteractorPartialState.Success).transactionDetailsUi
                    val body = details.body as TransactionDetailsBodyUi.Signing
                    assertEquals(listOf(body.document), body.sections)
                    val filename = body.document!!.items.single()
                    assertEquals("filename", filename.item.itemId)
                    assertEquals(transaction.fileName, filename.item.textValue())
                    assertNull(filename.url)
                    assertNull(filename.item.trailingContentData)
                    assertTrue(body.document.groups.isEmpty())
                    assertTrue(details.transactionDetailsCardUi.metadata.isEmpty())
                }
            }
        }
    }

    // Case 18:
    // 1. A data-deletion request contains multiple raw credential identifiers.
    //
    // Case 18 Expected Result:
    // Only that request's claims appear, with independent credential groups.
    @Test
    fun `Given Case 18, When getTransactionDetails is called, Then Case 18 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val transaction = mockedDataDeletionLogDomain.copy(
                claims = mockedTransactionClaims + mockedNestedTransactionClaims
            )
            mockGetTransactionLogCall(transaction)

            // When
            interactor.getTransactionDetails(transaction.id).runFlowTest {
                // Then
                val body = (awaitItem() as TransactionDetailsInteractorPartialState.Success)
                    .transactionDetailsUi.body as TransactionDetailsBodyUi.DataDeletionRequest
                assertEquals(
                    listOf(
                        mockedTransactionCredential.identifier,
                        mockedOtherTransactionCredential.identifier
                    ),
                    body.claims.groups.map { group -> group.header.textValue() },
                )
                assertEquals(
                    mockedTransactionClaimPath,
                    body.claims.groups.first().items.single().header.textValue()
                )
                assertEquals(
                    2,
                    body.claims.groups.map { group -> group.header.itemId }.distinct().size
                )
                body.claims.groups.forEach { group ->
                    assertEquals(
                        ListItemTrailingContentDataUi.Icon(AppIcons.KeyboardArrowDown),
                        group.header.trailingContentData,
                    )
                }
                assertEquals(mockedTransactionPartyName, body.party.items.first().item.textValue())
            }
        }
    }

    // Case 19:
    // 1. Authority reports contain full, partial or absent information.
    //
    // Case 19 Expected Result:
    // Only recorded fields are displayed, with an empty state when both fields are absent.
    @Test
    fun `Given Case 19, When getTransactionDetails is called, Then Case 19 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val transactions = listOf(
                mockedDpaReportLogDomain,
                mockedDpaReportLogDomain.copy(dpaName = null),
                mockedDpaReportLogDomain.copy(
                    dpaName = LocalizedTextDomain(
                        mockedTransactionLanguageTag,
                        " "
                    ), dpaCountry = null
                ),
            )
            transactions.forEachIndexed { index, transaction ->
                mockGetTransactionLogCall(transaction)

                // When
                interactor.getTransactionDetails(transaction.id).runFlowTest {
                    // Then
                    val body = (awaitItem() as TransactionDetailsInteractorPartialState.Success)
                        .transactionDetailsUi.body as TransactionDetailsBodyUi.DpaReport
                    assertEquals(2 - index, body.authority.items.size)
                    if (body.authority.items.isEmpty()) {
                        assertEquals(
                            "No information recorded",
                            body.authority.emptyItem?.textValue()
                        )
                    } else {
                        assertNull(body.authority.emptyItem)
                    }
                    assertEquals(
                        transaction.dpaCountry?.text,
                        body.authority.items.firstOrNull { row -> row.item.itemId == "authority:country" }?.item?.textValue()
                    )
                }
            }
        }
    }

    // Case 20:
    // 1. A presentation has empty requested data, shared data or both, and a blank non-completion reason.
    //
    // Case 20 Expected Result:
    // Both sections remain visible; only empty sections have a plain prepared message row.
    // There is no reason fallback or empty card metadata.
    @Test
    fun `Given Case 20, When getTransactionDetails is called, Then Case 20 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val emptyClaims = emptyList<CredentialClaimsDomain>()
            listOf(
                emptyClaims to emptyClaims,
                emptyClaims to mockedTransactionClaims,
                mockedTransactionClaims to emptyClaims,
            ).forEach { (requestedClaims, sharedClaims) ->
                val transaction = mockedPresentationLogDomain.copy(
                    result = TransactionResultDomain.NotCompleted(" "),
                    party = InteractingPartyDomain(null, null, emptyList()),
                    claimsRequested = requestedClaims,
                    claimsPresented = sharedClaims,
                )
                mockGetTransactionLogCall(transaction)

                // When
                interactor.getTransactionDetails(transaction.id).runFlowTest {
                    // Then
                    val details =
                        (awaitItem() as TransactionDetailsInteractorPartialState.Success).transactionDetailsUi
                    val body = details.body as TransactionDetailsBodyUi.Presentation
                    assertEquals(listOf(body.requested, body.shared), body.sections)
                    assertEquals(requestedClaims.isEmpty(), body.requested.groups.isEmpty())
                    assertEquals(
                        if (requestedClaims.isEmpty()) {
                            ListItemDataUi(
                                itemId = "requested:empty",
                                mainContentData = ListItemMainContentDataUi.Text("No data requested"),
                            )
                        } else {
                            null
                        },
                        body.requested.emptyItem,
                    )
                    assertEquals(sharedClaims.isEmpty(), body.shared.groups.isEmpty())
                    assertEquals(
                        if (sharedClaims.isEmpty()) {
                            ListItemDataUi(
                                itemId = "shared:empty",
                                mainContentData = ListItemMainContentDataUi.Text("No data shared"),
                            )
                        } else {
                            null
                        },
                        body.shared.emptyItem,
                    )
                    assertTrue(details.transactionDetailsCardUi.metadata.isEmpty())
                    assertNull(details.transactionDetailsCardUi.nonCompletionReason)
                }
            }
        }
    }

    // Case 21:
    // 1. Repeated credential types occur in both sections and a claim key contains quotes.
    //
    // Case 21 Expected Result:
    // All group/field IDs are unique; raw paths escape keys and empty claim lists stay explicit.
    @Test
    fun `Given Case 21, When getTransactionDetails is called, Then Case 21 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val claims = mockedTransactionClaims.single().copy(
                claims = listOf(
                    ClaimRefDomain(listOf(ClaimPathSegment.Key("quoted\"key")))
                )
            )
            val transaction = mockedPresentationLogDomain.copy(
                claimsRequested = listOf(claims, claims.copy(claims = emptyList())),
                claimsPresented = listOf(claims),
            )
            mockGetTransactionLogCall(transaction)

            // When
            interactor.getTransactionDetails(transaction.id).runFlowTest {
                // Then
                val body = (awaitItem() as TransactionDetailsInteractorPartialState.Success)
                    .transactionDetailsUi.body as TransactionDetailsBodyUi.Presentation
                assertNull(body.requested.emptyItem)
                assertNull(body.shared.emptyItem)
                val groups = body.requested.groups + body.shared.groups
                val ids =
                    groups.map { group -> group.header.itemId } + groups.flatMap { group -> group.items }
                        .map { row -> row.header.itemId }
                assertEquals(ids.size, ids.distinct().size)
                assertEquals(
                    "[\"quoted\\\"key\"]",
                    groups.first().items.single().header.textValue()
                )
                assertEquals(
                    "No attributes recorded",
                    body.requested.groups.last().items.single().header.textValue()
                )
            }
        }
    }


    // Case 22:
    // 1. A presentation contains a credential whose type differs from its namespace and a credential with nested paths.
    //
    // Case 22 Expected Result:
    // Both requested and shared groups show the raw credential identifiers and exact escaped paths.
    @Test
    fun `Given Case 22, When getTransactionDetails is called, Then Case 22 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val mdocIdentifier = "org.iso.18013.5.1.mDL"
            val namespace = "org.iso.18013.5.1"
            val vct = "urn:example:Identity:V1"
            val claims = listOf(
                CredentialClaimsDomain(
                    CredentialRefDomain(mdocIdentifier),
                    listOf(
                        ClaimRefDomain(
                            listOf(
                                ClaimPathSegment.Key(namespace),
                                ClaimPathSegment.Key("family_name")
                            )
                        )
                    ),
                ),
                CredentialClaimsDomain(
                    CredentialRefDomain(vct),
                    listOf(
                        ClaimRefDomain(
                            listOf(
                                ClaimPathSegment.Key("address"),
                                ClaimPathSegment.Key("street_address")
                            )
                        ),
                        ClaimRefDomain(
                            listOf(
                                ClaimPathSegment.Key("nationalities"),
                                ClaimPathSegment.Index(0)
                            )
                        ),
                        ClaimRefDomain(
                            listOf(
                                ClaimPathSegment.Key("nationalities"),
                                ClaimPathSegment.AllElements
                            )
                        ),
                        ClaimRefDomain(
                            listOf(
                                ClaimPathSegment.Key("0"),
                                ClaimPathSegment.Key("quoted\"\\key")
                            )
                        ),
                    ),
                ),
            )
            val transaction =
                mockedPresentationLogDomain.copy(claimsRequested = claims, claimsPresented = claims)
            mockGetTransactionLogCall(transaction)

            // When
            interactor.getTransactionDetails(transaction.id).runFlowTest {
                // Then
                val body = (awaitItem() as TransactionDetailsInteractorPartialState.Success)
                    .transactionDetailsUi.body as TransactionDetailsBodyUi.Presentation
                listOf(body.requested, body.shared).forEach { section ->
                    assertEquals(
                        listOf(mdocIdentifier, vct),
                        section.groups.map { group -> group.header.textValue() })
                    assertEquals(
                        listOf(
                            "[\"org.iso.18013.5.1\"][\"family_name\"]",
                            "[\"address\"][\"street_address\"]",
                            "[\"nationalities\"][0]",
                            "[\"nationalities\"][*]",
                            "[\"0\"][\"quoted\\\"\\\\key\"]",
                        ),
                        section.groups.flatMap { group -> group.items }
                            .map { row -> row.header.textValue() },
                    )
                }
            }
        }
    }

    // Case 23:
    // 1. A presentation has distinct relying-party and intermediary identity, plus authority data.
    // 2. Intermediary contacts include web, email, phone, an unsupported link and a blank value.
    //
    // Case 23 Expected Result:
    // The card ends with intermediary name/contact fields labeled with their owner.
    @Test
    fun `Given intermediary details, When getTransactionDetails is called, Then intermediary fields identify their owner`() {
        coroutineRule.runTest {
            // Given
            val nameLabel =
                mockedTransactionDetailsStrings.getValue(R.string.transaction_details_intermediary_name_label)
            val contactLabel =
                mockedTransactionDetailsStrings.getValue(R.string.transaction_details_intermediary_contact_label)
            val identifier = mockedTransactionQualifiedIdentifier.copy(value = "intermediary-456")
            val intermediary = mockedTransactionIntermediary.copy(
                identifier = identifier,
                contacts = listOf(
                    mockedActionWebUrl,
                    mockedActionMailUrl,
                    mockedActionPhoneUrl,
                    "javascript:alert(1)",
                    " "
                ),
            )
            val transaction = mockedDetailedPresentationLogDomain.copy(intermediary = intermediary)
            mockGetTransactionLogCall(transaction)

            // When
            interactor.getTransactionDetails(transaction.id).runFlowTest {
                // Then
                val details =
                    (awaitItem() as TransactionDetailsInteractorPartialState.Success).transactionDetailsUi
                val body = details.body as TransactionDetailsBodyUi.Presentation
                val group = details.transactionDetailsCardUi.metadata.last()
                assertEquals(
                    listOf(
                        TransactionDetailsFieldUi(
                            "intermediary:name",
                            nameLabel,
                            intermediary.name!!.text,
                            null
                        ),
                        TransactionDetailsFieldUi(
                            "intermediary:contact:0",
                            contactLabel,
                            mockedActionWebUrl,
                            mockedActionWebUrl
                        ),
                        TransactionDetailsFieldUi(
                            "intermediary:contact:1",
                            contactLabel,
                            mockedActionMailUrl,
                            mockedActionMailUrl
                        ),
                        TransactionDetailsFieldUi(
                            "intermediary:contact:2",
                            contactLabel,
                            mockedActionPhoneUrl,
                            mockedActionPhoneUrl
                        ),
                        TransactionDetailsFieldUi(
                            "intermediary:contact:3",
                            contactLabel,
                            "javascript:alert(1)",
                            null
                        ),
                    ),
                    group.fields,
                )
                assertEquals(listOf(body.requested, body.shared), body.sections)
                assertEquals(mockedTransactionPartyName, details.transactionDetailsCardUi.partyName)
            }
        }
    }

    // Case 24:
    // 1. Only the intermediary name, identifier or contact is recorded, without registration data.
    // 2. The relying party and authority have no action contacts.
    //
    // Case 24 Expected Result:
    // Names and contacts display independently with intermediary labels; identifier-only metadata stays hidden and supplies no action contacts.
    @Test
    fun `Given partial intermediary details, When getTransactionDetails is called, Then available fields are shown independently`() {
        coroutineRule.runTest {
            // Given
            val nameLabel =
                mockedTransactionDetailsStrings.getValue(R.string.transaction_details_intermediary_name_label)
            val contactLabel =
                mockedTransactionDetailsStrings.getValue(R.string.transaction_details_intermediary_contact_label)
            val intermediaries = listOf(
                mockedTransactionIntermediary to listOf(
                    TransactionDetailsFieldUi(
                        "intermediary:name",
                        nameLabel,
                        mockedTransactionIntermediary.name!!.text,
                        null
                    ),
                ),
                mockedTransactionIntermediary.copy(
                    name = null,
                    identifier = mockedTransactionQualifiedIdentifier
                ) to emptyList(),
                mockedTransactionIntermediary.copy(
                    name = LocalizedTextDomain(
                        mockedTransactionLanguageTag,
                        " "
                    ), contacts = listOf(mockedActionMailUrl)
                ) to listOf(
                    TransactionDetailsFieldUi(
                        "intermediary:contact:0",
                        contactLabel,
                        mockedActionMailUrl,
                        mockedActionMailUrl
                    ),
                ),
            )
            intermediaries.forEach { (intermediary, expectedFields) ->
                val transaction = mockedPresentationLogDomain.copy(intermediary = intermediary)
                mockGetTransactionLogCall(transaction)

                // When
                interactor.getTransactionDetails(transaction.id).runFlowTest {
                    // Then
                    val details =
                        (awaitItem() as TransactionDetailsInteractorPartialState.Success).transactionDetailsUi
                    val body = details.body as TransactionDetailsBodyUi.Presentation
                    assertEquals(
                        expectedFields,
                        details.transactionDetailsCardUi.metadata.flatMap { group -> group.fields })
                    assertEquals(listOf(body.requested, body.shared), body.sections)
                    assertTrue(body.deletionContacts.isEmpty())
                    assertTrue(body.reportContacts.isEmpty())
                }
            }
        }
    }

    // Case 25:
    // 1. Intermediary data is absent, empty or contains only blank values.
    //
    // Case 25 Expected Result:
    // No intermediary section or placeholder appears in the details list.
    @Test
    fun `Given no displayable intermediary details, When getTransactionDetails is called, Then the intermediary section is omitted`() {
        coroutineRule.runTest {
            // Given
            val intermediaries = listOf(
                null,
                InteractingPartyDomain(null, null, emptyList()),
                mockedTransactionIntermediary.copy(
                    name = LocalizedTextDomain(mockedTransactionLanguageTag, " \t"),
                    identifier = mockedTransactionQualifiedIdentifier.copy(
                        value = "",
                        schemeUri = " "
                    ),
                    contacts = listOf("", " \t"),
                ),
            )
            intermediaries.forEach { intermediary ->
                val transaction = mockedPresentationLogDomain.copy(intermediary = intermediary)
                mockGetTransactionLogCall(transaction)

                // When
                interactor.getTransactionDetails(transaction.id).runFlowTest {
                    // Then
                    val details =
                        (awaitItem() as TransactionDetailsInteractorPartialState.Success).transactionDetailsUi
                    val body = details.body as TransactionDetailsBodyUi.Presentation
                    assertTrue(details.transactionDetailsCardUi.metadata.isEmpty())
                    assertEquals(listOf(body.requested, body.shared), body.sections)
                }
            }
        }
    }


    // Case 26:
    // 1. Presentation and both issuance types have repeated contacts, including plain EU text.
    //
    // Case 26 Expected Result:
    // Display order and duplicates remain intact; action contacts retain their independent deduplication.
    @Test
    fun `Given duplicate contacts, When getTransactionDetails is called, Then display preserves all occurrences`() {
        coroutineRule.runTest {
            // Given
            val contacts = listOf(
                "EU",
                mockedActionWebUrl,
                mockedActionWebUrl,
                mockedActionMailUrl,
                mockedActionPhoneUrl
            )
            val issuer =
                mockedIssuanceDetails.copy(issuer = mockedTransactionParty.copy(contacts = contacts))
            val transactions = listOf(
                mockedPresentationLogDomain.copy(party = mockedTransactionParty.copy(contacts = contacts)),
                mockedIssuanceLogDomain.copy(details = issuer),
                mockedReissuanceLogDomain.copy(details = issuer),
            )
            transactions.forEach { transaction ->
                mockGetTransactionLogCall(transaction)

                // When
                interactor.getTransactionDetails(transaction.id).runFlowTest {
                    // Then
                    val details =
                        (awaitItem() as TransactionDetailsInteractorPartialState.Success).transactionDetailsUi
                    val fields =
                        details.transactionDetailsCardUi.metadata.flatMap { group -> group.fields }
                            .filter { field -> field.label == "Contact" }
                    assertEquals(contacts, fields.map { field -> field.value })
                    assertEquals(fields.size, fields.map { field -> field.id }.distinct().size)
                    assertNull(fields.first().url)
                    assertEquals(
                        listOf(
                            null,
                            mockedActionWebUrl,
                            mockedActionWebUrl,
                            mockedActionMailUrl,
                            mockedActionPhoneUrl
                        ), fields.map { field -> field.url })
                    (details.body as? TransactionDetailsBodyUi.Presentation)?.let { body ->
                        assertEquals(3, body.deletionContacts.size)
                    }
                }
            }
        }
    }

    // Case 27:
    // 1. A nameless party has a recorded type and contact; registration contains only hidden metadata.
    //
    // Case 27 Expected Result:
    // Available type/contact fields remain visible independently and hidden fields create no groups.
    @Test
    fun `Given sparse metadata, When getTransactionDetails is called, Then only independent available fields are shown`() {
        coroutineRule.runTest {
            // Given
            val transaction = mockedDetailedPresentationLogDomain.copy(
                party = mockedTransactionPartyWithContacts.copy(
                    name = null,
                    contacts = listOf("EU")
                ),
                partyType = "ARecordedProviderType",
                registration = mockedTransactionRegistration.copy(
                    purpose = " ",
                    privacyPolicyUrls = emptyList()
                ),
            )
            mockGetTransactionLogCall(transaction)

            // When
            interactor.getTransactionDetails(transaction.id).runFlowTest {
                // Then
                val details =
                    (awaitItem() as TransactionDetailsInteractorPartialState.Success).transactionDetailsUi
                val card = details.transactionDetailsCardUi
                assertNull(card.partyName)
                assertEquals("ARecordedProviderType", card.providerType)
                assertEquals(
                    listOf(TransactionDetailsFieldUi("party:contact:0", "Contact", "EU", null)),
                    card.metadata.single().fields,
                )
                assertTrue((details.body as TransactionDetailsBodyUi.Presentation).reportContacts.isNotEmpty())
            }
        }
    }

    // Case 28:
    // 1. Signing has a dedicated transaction identifier but no filename.
    //
    // Case 28 Expected Result:
    // The dedicated identifier is card metadata; missing filenames create no document section or file action.
    @Test
    fun `Given signing identifiers and absent filenames, When getTransactionDetails is called, Then only dedicated metadata is shown`() {
        coroutineRule.runTest {
            // Given
            listOf<String?>(null, "", " ").forEach { filename ->
                val transaction = mockedSigningLogDomain.copy(
                    signingTransactionId = "dedicated-signing-id",
                    serviceType = "ARecordedSigningType",
                    fileName = filename,
                    dtbsr = "digest",
                )
                mockGetTransactionLogCall(transaction)

                // When
                interactor.getTransactionDetails(transaction.id).runFlowTest {
                    // Then
                    val details =
                        (awaitItem() as TransactionDetailsInteractorPartialState.Success).transactionDetailsUi
                    assertEquals(
                        "ARecordedSigningType",
                        details.transactionDetailsCardUi.providerType
                    )
                    val field = details.transactionDetailsCardUi.metadata.single().fields.single()
                    assertEquals("dedicated-signing-id", field.value)
                    assertEquals("Signing transaction identifier", field.label)
                    assertNull(field.url)
                    assertTrue(details.body.sections.isEmpty())
                }
            }
        }
    }

    // Case 29:
    // 1. Optional presentation type/registration or signing identifier values are absent or blank.
    //
    // Case 29 Expected Result:
    // Hidden metadata and the generic transaction ID never create expandable card content.
    @Test
    fun `Given no permitted extra metadata, When getTransactionDetails is called, Then the card has no expansion content`() {
        coroutineRule.runTest {
            // Given
            val transactions = listOf(
                mockedPresentationLogDomain.copy(
                    partyType = " ",
                    party = mockedTransactionPartyWithContacts.copy(contacts = emptyList()),
                    registration = mockedTransactionRegistration.copy(
                        purpose = null,
                        privacyPolicyUrls = listOf(" ")
                    ),
                ),
                mockedSigningLogDomain.copy(serviceType = " ", signingTransactionId = " "),
            )
            transactions.forEach { transaction ->
                mockGetTransactionLogCall(transaction)

                // When
                interactor.getTransactionDetails(transaction.id).runFlowTest {
                    // Then
                    val card =
                        (awaitItem() as TransactionDetailsInteractorPartialState.Success).transactionDetailsUi.transactionDetailsCardUi
                    assertNull(card.providerType)
                    assertTrue(card.metadata.isEmpty())
                }
            }
        }
    }

    // Case 30:
    // 1. Not-completed issuance and reissuance have no credential identifiers and various recorded counts.
    //
    // Case 30 Expected Result:
    // Recorded counts, including zero, remain visible without inventing identifiers or deriving counts from the list.
    @Test
    fun `Given no recorded credentials, When getTransactionDetails is called, Then no credentials are inferred`() {
        coroutineRule.runTest {
            // Given
            listOf(0 to 0, 3 to 0, 3 to 1).forEach { (requestedCount, issuedCount) ->
                val issuanceDetails = mockedIssuanceDetails.copy(
                    credentials = emptyList(),
                    requestedCount = requestedCount,
                    issuedCount = issuedCount,
                    isUserTriggered = null,
                )
                val transactions = listOf(
                    mockedIssuanceLogDomain.copy(
                        details = issuanceDetails,
                        result = TransactionResultDomain.NotCompleted(reason = null),
                    ),
                    mockedReissuanceLogDomain.copy(
                        details = issuanceDetails,
                        result = TransactionResultDomain.NotCompleted(reason = null),
                    ),
                )
                transactions.forEach { transaction ->
                    mockGetTransactionLogCall(transaction)

                    // When
                    interactor.getTransactionDetails(transaction.id).runFlowTest {
                        // Then
                        val details =
                            (awaitItem() as TransactionDetailsInteractorPartialState.Success).transactionDetailsUi
                        assertTrue(details.body.sections.isEmpty())
                        assertEquals(false, details.transactionDetailsCardUi.transactionIsCompleted)
                        assertEquals(
                            listOf(
                                TransactionDetailsFieldUi(
                                    "requested-count",
                                    "Credentials requested",
                                    requestedCount.toString(),
                                    null,
                                ),
                                TransactionDetailsFieldUi(
                                    "issued-count",
                                    "Credentials issued",
                                    issuedCount.toString(),
                                    null,
                                ),
                            ),
                            details.transactionDetailsCardUi.metadata.single().fields,
                        )
                    }
                }
            }
        }
    }

    //endregion

    //region deleteTransaction

    // Case 1:
    // 1. Deletion succeeds for each supported completed and not-completed transaction.
    //
    // Case 1 Expected Result:
    // Success follows one delete call with the selected identifier, without loading its type.
    @Test
    fun `Given Case 1, When deleteTransaction is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val transactions = mockedTransactionLogDomains + mockedNotCompletedTransactionLogDomains
            transactions.forEach { transaction ->
                mockDeleteTransactionLogCall(transactionId = transaction.id)

                // When
                interactor.deleteTransaction(transactionId = transaction.id).runFlowTest {
                    // Then
                    assertEquals(
                        TransactionDetailsInteractorDeleteTransactionPartialState.Success,
                        awaitItem()
                    )
                    awaitComplete()
                }
                verify(walletCoreTransactionLogController).deleteTransactionLog(id = transaction.id)
            }
            verifyNoMoreInteractions(walletCoreTransactionLogController)
        }
    }

    // Case 2:
    // 1. The storage deletion has started but has not completed.
    //
    // Case 2 Expected Result:
    // No success is emitted until storage confirms completion.
    @Test
    fun `Given Case 2, When deleteTransaction is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val started = CompletableDeferred<Unit>()
            val finished = CompletableDeferred<Unit>()
            whenever(walletCoreTransactionLogController.deleteTransactionLog(id = mockedTransactionId))
                .doSuspendableAnswer {
                    started.complete(Unit)
                    finished.await()
                }

            // When
            interactor.deleteTransaction(transactionId = mockedTransactionId).runFlowTest {
                started.await()

                // Then
                expectNoEvents()
                finished.complete(Unit)
                assertEquals(
                    TransactionDetailsInteractorDeleteTransactionPartialState.Success,
                    awaitItem()
                )
                awaitComplete()
            }
        }
    }

    // Case 3:
    // 1. Storage throws an exception with a message.
    //
    // Case 3 Expected Result:
    // Failure contains that message and no success is emitted.
    @Test
    fun `Given Case 3, When deleteTransaction is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockDeleteTransactionLogFailure(exception = mockedExceptionWithMessage)

            // When
            interactor.deleteTransaction(transactionId = mockedTransactionId).runFlowTest {
                // Then
                assertEquals(
                    TransactionDetailsInteractorDeleteTransactionPartialState.Failure(
                        errorMessage = mockedExceptionWithMessage.localizedMessage!!,
                    ),
                    awaitItem(),
                )
                awaitComplete()
            }
        }
    }

    // Case 4:
    // 1. Storage throws an exception without a message.
    //
    // Case 4 Expected Result:
    // Failure uses the generic error message.
    @Test
    fun `Given Case 4, When deleteTransaction is called, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockDeleteTransactionLogFailure(exception = mockedExceptionWithNoMessage)

            // When
            interactor.deleteTransaction(transactionId = mockedTransactionId).runFlowTest {
                // Then
                assertEquals(
                    TransactionDetailsInteractorDeleteTransactionPartialState.Failure(
                        errorMessage = mockedGenericErrorMessage,
                    ),
                    awaitItem(),
                )
                awaitComplete()
            }
        }
    }
    //endregion

    //region action availability
    // Case 1:
    // 1. Recorded contacts mix supported channels, duplicates, country text and unsafe links.
    //
    // Case 1 Expected Result:
    // Both actions expose valid, distinct channels; loading details does not record an action.
    @Test
    fun `Given Case 1, When action availability is loaded, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val transaction = mockedActionPresentation.copy(
                party = mockedActionPresentation.party.copy(contacts = mockedMixedContacts),
                registration = mockedTransactionRegistration.copy(
                    dpa = mockedTransactionRegistration.dpa!!.copy(contacts = mockedMixedContacts),
                ),
            )
            mockGetTransactionLogCall(transaction)

            // When
            interactor.getTransactionDetails(transaction.id).runFlowTest {
                // Then
                val body = (awaitItem() as TransactionDetailsInteractorPartialState.Success)
                    .transactionDetailsUi.body as TransactionDetailsBodyUi.Presentation
                val expectedUrls = listOf(
                    mockedActionWebUrl, mockedActionMailUrl, mockedActionPhoneUrl,
                    "mailto:o%27connor@example.com",
                )
                assertEquals(expectedUrls, body.deletionContacts.map { contact -> contact.url })
                assertEquals(expectedUrls, body.reportContacts.map { contact -> contact.url })
                verifyNoInteractions(walletCoreTransactionRecordingController)
                awaitComplete()
            }
        }
    }

    // Case 2:
    // 1. A failed presentation has no shared data but has an authority contact.
    //
    // Case 2 Expected Result:
    // Deletion is unavailable; reporting remains available.
    @Test
    fun `Given Case 2, When action availability is loaded, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val transaction = mockedActionPresentation.copy(
                result = TransactionResultDomain.NotCompleted("Declined"),
                claimsPresented = emptyList(),
            )
            mockGetTransactionLogCall(transaction)

            // When
            interactor.getTransactionDetails(transaction.id).runFlowTest {
                // Then
                val body = (awaitItem() as TransactionDetailsInteractorPartialState.Success)
                    .transactionDetailsUi.body as TransactionDetailsBodyUi.Presentation
                assertTrue(body.deletionContacts.isEmpty())
                assertEquals(
                    listOf(mockedActionWebUrl, mockedActionMailUrl, mockedActionPhoneUrl),
                    body.reportContacts.map { contact -> contact.url },
                )
                awaitComplete()
            }
        }
    }

    // Case 3:
    // 1. Contacts are missing or invalid; registrar and privacy-policy links may exist.
    //
    // Case 3 Expected Result:
    // Neither action has a channel; unrelated registration links are not substituted.
    @Test
    fun `Given Case 3, When action availability is loaded, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val transactions = listOf(
                mockedActionPresentation.copy(
                    party = mockedActionPresentation.party.copy(contacts = emptyList()),
                    registration = null,
                ),
                mockedActionPresentation.copy(
                    party = mockedActionPresentation.party.copy(contacts = mockedInvalidActionContacts),
                    registration = mockedTransactionRegistration.copy(
                        dpa = mockedTransactionRegistration.dpa!!.copy(contacts = mockedInvalidActionContacts),
                    ),
                ),
            )
            transactions.forEach { transaction ->
                mockGetTransactionLogCall(transaction)

                // When
                interactor.getTransactionDetails(transaction.id).runFlowTest {
                    // Then
                    val body = (awaitItem() as TransactionDetailsInteractorPartialState.Success)
                        .transactionDetailsUi.body as TransactionDetailsBodyUi.Presentation
                    assertTrue(body.deletionContacts.isEmpty())
                    assertTrue(body.reportContacts.isEmpty())
                    awaitComplete()
                }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController)
        }
    }

    // Case 4:
    // 1. Disclosed data and one DDR contact exist without a registration certificate.
    // 2. Each method is checked for completed and interrupted presentations.
    //
    // Case 4 Expected Result:
    // Only DDR is available, including a short phone number.
    @Test
    fun `Given Case 4, When action availability is loaded, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val results = listOf(
                TransactionResultDomain.Completed,
                TransactionResultDomain.NotCompleted("Interrupted"),
            )
            val contacts = listOf(mockedActionWebUrl, mockedActionMailUrl, "tel:1234")
            results.forEach { result ->
                contacts.forEach { contact ->
                    val transaction = mockedActionPresentation.copy(
                        result = result,
                        registration = null,
                        party = mockedActionPresentation.party.copy(contacts = listOf(contact)),
                    )
                    mockGetTransactionLogCall(transaction)

                    // When
                    interactor.getTransactionDetails(transaction.id).runFlowTest {
                        // Then
                        val body = (awaitItem() as TransactionDetailsInteractorPartialState.Success)
                            .transactionDetailsUi.body as TransactionDetailsBodyUi.Presentation
                        assertEquals(
                            listOf(contact),
                            body.deletionContacts.map { channel -> channel.url })
                        assertTrue(body.reportContacts.isEmpty())
                        awaitComplete()
                    }
                }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController)
        }
    }

    // Case 5:
    // 1. Only a DPA contact exists; the authority has no optional name or country.
    //
    // Case 5 Expected Result:
    // Reporting is available for the interrupted presentation; DDR does not borrow its contact.
    @Test
    fun `Given Case 5, When action availability is loaded, Then Case 5 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val transaction = mockedActionPresentation.copy(
                result = TransactionResultDomain.NotCompleted("Interrupted"),
                party = mockedActionPresentation.party.copy(contacts = emptyList()),
                registration = mockedTransactionRegistration.copy(
                    dpa = DpaContactDomain(
                        name = null,
                        country = null,
                        contacts = listOf(mockedActionMailUrl),
                    ),
                ),
            )
            mockGetTransactionLogCall(transaction)

            // When
            interactor.getTransactionDetails(transaction.id).runFlowTest {
                // Then
                val body = (awaitItem() as TransactionDetailsInteractorPartialState.Success)
                    .transactionDetailsUi.body as TransactionDetailsBodyUi.Presentation
                assertTrue(body.deletionContacts.isEmpty())
                assertEquals(
                    listOf(mockedActionMailUrl),
                    body.reportContacts.map { channel -> channel.url })
                awaitComplete()
            }
            verifyNoInteractions(walletCoreTransactionRecordingController)
        }
    }

    // Case 6:
    // 1. Credential groups contain no disclosed claims, although both actions have contacts.
    //
    // Case 6 Expected Result:
    // DDR is unavailable and reporting remains available regardless of the result.
    @Test
    fun `Given Case 6, When action availability is loaded, Then Case 6 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val results = listOf(
                TransactionResultDomain.Completed,
                TransactionResultDomain.NotCompleted("Interrupted"),
            )
            results.forEach { result ->
                val transaction = mockedActionPresentation.copy(
                    result = result,
                    claimsPresented = listOf(
                        mockedTransactionClaims.first().copy(claims = emptyList())
                    ),
                )
                mockGetTransactionLogCall(transaction)

                // When
                interactor.getTransactionDetails(transaction.id).runFlowTest {
                    // Then
                    val body = (awaitItem() as TransactionDetailsInteractorPartialState.Success)
                        .transactionDetailsUi.body as TransactionDetailsBodyUi.Presentation
                    assertTrue(body.deletionContacts.isEmpty())
                    assertEquals(
                        listOf(mockedActionWebUrl, mockedActionMailUrl, mockedActionPhoneUrl),
                        body.reportContacts.map { channel -> channel.url },
                    )
                    awaitComplete()
                }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController)
        }
    }
    //endregion


    //region getDataDeletionRequest

    // Case 1:
    // 1. DDR contacts contain multiple methods, invalid values or repeated websites.
    //
    // Case 1 Expected Result:
    // Website precedes email, then phone; matching copy is prepared without recording an attempt.
    @Test
    fun `Given Case 1, When getDataDeletionRequest is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val cases = listOf(
                listOf(
                    mockedActionPhoneUrl,
                    mockedActionMailUrl,
                    mockedActionWebUrl,
                    mockedActionWebUrl + "/other"
                ) to (mockedActionWebUrl to "Website"),
                listOf(
                    "https://",
                    mockedActionPhoneUrl,
                    mockedActionMailUrl
                ) to (mockedActionMailUrl to "Email"),
                listOf("GR", mockedActionPhoneUrl) to (mockedActionPhoneUrl to "Phone"),
            )
            cases.forEach { (contacts, expected) ->
                val (expectedUrl, expectedMethod) = expected
                val presentation = mockedActionPresentation.copy(
                    party = mockedActionPresentation.party.copy(contacts = contacts),
                )
                mockGetTransactionLogCall(presentation)

                // When
                interactor.getDataDeletionRequest(presentation.id).runFlowTest {
                    // Then
                    val request =
                        (awaitItem() as TransactionDetailsInteractorDataDeletionPartialState.Success).request
                    assertEquals(expectedUrl, request.contactUrl)
                    assertEquals(
                        "$expectedMethod description for ${presentation.party.name?.text}",
                        request.description,
                    )
                    assertEquals(
                        "$expectedMethod action for ${presentation.party.name?.text}",
                        request.buttonText,
                    )
                    assertEquals(
                        "Retention for ${presentation.party.name?.text}",
                        request.retentionNotice,
                    )
                    assertEquals(
                        "<b>$expectedMethod responsibility.</b> Continue outside.",
                        request.responsibility,
                    )
                    awaitComplete()
                }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController, uuidProvider)
        }
    }

    // Case 2:
    // 1. The requested parent is missing or is another transaction type.
    //
    // Case 2 Expected Result:
    // The explanation is unavailable and nothing is recorded.
    @Test
    fun `Given Case 2, When getDataDeletionRequest is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val transactions = listOf(null) +
                    mockedTransactionLogDomains.filterNot { transaction -> transaction is TransactionLogDomain.Presentation }
            transactions.forEach { transaction ->
                whenever(walletCoreTransactionLogController.getTransactionLog(mockedTransactionId))
                    .thenReturn(transaction)

                // When
                interactor.getDataDeletionRequest(mockedTransactionId).runFlowTest {
                    // Then
                    assertEquals(
                        TransactionDetailsInteractorDataDeletionPartialState.Failure(
                            mockedActionUnavailable
                        ),
                        awaitItem(),
                    )
                    awaitComplete()
                }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController, uuidProvider)
        }
    }

    // Case 3:
    // 1. DDR contacts or actual disclosed claims are absent, although DPA contacts exist.
    //
    // Case 3 Expected Result:
    // A stale route is unavailable; empty groups and DPA contacts cannot enable DDR.
    @Test
    fun `Given Case 3, When getDataDeletionRequest is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val presentations = listOf(
                mockedActionPresentation.copy(claimsPresented = emptyList()),
                mockedActionPresentation.copy(
                    claimsPresented = listOf(
                        mockedTransactionClaims.first().copy(claims = emptyList())
                    ),
                ),
                mockedActionPresentation.copy(
                    party = mockedActionPresentation.party.copy(contacts = emptyList()),
                ),
                mockedActionPresentation.copy(
                    party = mockedActionPresentation.party.copy(contacts = mockedInvalidActionContacts),
                ),
            )
            presentations.forEach { presentation ->
                mockGetTransactionLogCall(presentation)

                // When
                interactor.getDataDeletionRequest(presentation.id).runFlowTest {
                    // Then
                    assertEquals(
                        TransactionDetailsInteractorDataDeletionPartialState.Failure(
                            mockedActionUnavailable
                        ),
                        awaitItem(),
                    )
                    awaitComplete()
                }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController, uuidProvider)
        }
    }

    // Case 4:
    // 1. An interrupted presentation disclosed claims and has a short phone number, without registration.
    //
    // Case 4 Expected Result:
    // The recorded contact enables DDR independently of completion or certificate presence.
    @Test
    fun `Given Case 4, When getDataDeletionRequest is called, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val presentation = mockedActionPresentation.copy(
                result = TransactionResultDomain.NotCompleted("Interrupted"),
                registration = null,
                party = mockedActionPresentation.party.copy(contacts = listOf("1234")),
            )
            mockGetTransactionLogCall(presentation)

            // When
            interactor.getDataDeletionRequest(presentation.id).runFlowTest {
                // Then
                val request =
                    (awaitItem() as TransactionDetailsInteractorDataDeletionPartialState.Success).request
                assertEquals(
                    "Phone action for ${presentation.party.name?.text}",
                    request.buttonText
                )
                assertEquals("tel:1234", request.contactUrl)
                awaitComplete()
            }
            verifyNoInteractions(walletCoreTransactionRecordingController, uuidProvider)
        }
    }

    // Case 5:
    // 1. RP identity has Unicode, no name, or neither a name nor an identifier.
    //
    // Case 5 Expected Result:
    // The display preserves literal recorded names and uses a neutral label when absent, never the identifier.
    @Test
    fun `Given Case 5, When getDataDeletionRequest is called, Then Case 5 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val defaultName = "Relying party"
            doReturn(defaultName).whenever(resourceProvider)
                .getString(R.string.data_deletion_relying_party_default_name)
            val parties = listOf(
                mockedActionPresentation.party.copy(
                    name = LocalizedTextDomain(
                        "el",
                        "Ταξίδι & <Travel>"
                    )
                ) to "Ταξίδι & <Travel>",
                mockedActionPresentation.party.copy(
                    name = LocalizedTextDomain(
                        mockedTransactionLanguageTag,
                        " "
                    )
                ) to defaultName,
                mockedActionPresentation.party.copy(name = null) to defaultName,
                mockedActionPresentation.party.copy(name = null, identifier = null) to defaultName,
            )
            parties.forEach { (party, expectedName) ->
                val presentation = mockedActionPresentation.copy(party = party)
                mockGetTransactionLogCall(presentation)

                // When
                interactor.getDataDeletionRequest(presentation.id).runFlowTest {
                    // Then
                    val request =
                        (awaitItem() as TransactionDetailsInteractorDataDeletionPartialState.Success).request
                    assertEquals("Website description for $expectedName", request.description)
                    assertEquals("Website action for $expectedName", request.buttonText)
                    assertEquals("Retention for $expectedName", request.retentionNotice)
                    awaitComplete()
                }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController, uuidProvider)
        }
    }

    // Case 6:
    // 1. Loading the parent throws, with or without a message.
    //
    // Case 6 Expected Result:
    // The ViewModel receives a failure state; loading has no recording side effects.
    @Test
    fun `Given Case 6, When getDataDeletionRequest is called, Then Case 6 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            listOf(mockedExceptionWithMessage, mockedExceptionWithNoMessage).forEach { exception ->
                doThrow(exception).whenever(walletCoreTransactionLogController)
                    .getTransactionLog(mockedTransactionId)

                // When
                interactor.getDataDeletionRequest(mockedTransactionId).runFlowTest {
                    // Then
                    assertEquals(
                        TransactionDetailsInteractorDataDeletionPartialState.Failure(
                            exception.localizedMessage ?: mockedGenericErrorMessage
                        ),
                        awaitItem(),
                    )
                    awaitComplete()
                }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController, uuidProvider)
        }
    }

    //endregion

    //region getDpaReport

    // Case 1:
    // 1. The DPA has mixed, repeated and unsupported contacts; the RP has a different contact.
    //
    // Case 1 Expected Result:
    // All distinct DPA contacts are shown in Phone, Email, Website order.
    @Test
    fun `Given Case 1, When getDpaReport is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val authority = DpaContactDomain(
                name = LocalizedTextDomain("el", "Αρχή & <Authority>"),
                country = LocalizedTextDomain(mockedTransactionLanguageTag, "GR"),
                contacts = listOf(
                    mockedActionWebUrl, "first@example.com", "+30 (210) 123-4567",
                    "mailto:second@example.com", mockedActionWebUrl, mockedActionPhoneUrl,
                ) + mockedInvalidActionContacts,
            )
            val presentation = mockedActionPresentation.copy(
                party = mockedActionPresentation.party.copy(contacts = listOf("https://rp.example.com")),
                registration = mockedTransactionRegistration.copy(dpa = authority),
            )
            mockGetTransactionLogCall(presentation)

            // When
            interactor.getDpaReport(presentation.id).runFlowTest {
                // Then
                val report =
                    (awaitItem() as TransactionDetailsInteractorDpaReportPartialState.Success).report
                assertEquals(authority.name?.text, report.authority)
                assertEquals(
                    listOf(
                        mockedActionPhoneUrl to "+30 (210) 123-4567",
                        "mailto:first@example.com" to "first@example.com",
                        "mailto:second@example.com" to "second@example.com",
                        mockedActionWebUrl to mockedActionWebUrl,
                    ),
                    report.contacts.map { contact -> contact.url to contact.item.textValue() },
                )
                assertEquals(
                    listOf(AppIcons.Call, AppIcons.Email, AppIcons.Email, AppIcons.Link),
                    report.contacts.map { contact ->
                        (contact.item.leadingContentData as ListItemLeadingContentDataUi.Icon).iconData
                    },
                )
                assertEquals(
                    listOf("Call", "Open email", "Open email", "Visit website"),
                    report.contacts.map { contact ->
                        (contact.item.trailingContentData as ListItemTrailingContentDataUi.TextWithIcon).text
                    },
                )
                report.contacts.forEach { contact ->
                    assertEquals(contact.url, contact.item.itemId)
                    assertEquals(
                        AppIcons.KeyboardArrowRight,
                        (contact.item.trailingContentData as ListItemTrailingContentDataUi.TextWithIcon).iconData,
                    )
                }
                assertEquals(
                    "<b>Report responsibility.</b> Continue outside.",
                    report.responsibility,
                )
                assertEquals("Authority follows up.", report.followUp)
                awaitComplete()
            }
            verify(walletCoreTransactionLogController).getTransactionLog(presentation.id)
            verifyNoInteractions(walletCoreTransactionRecordingController, uuidProvider)
        }
    }

    // Case 2:
    // 1. An interrupted presentation has no disclosed claims or RP contacts, but one DPA contact.
    // 2. DPA identity is missing or only its name/country is available.
    //
    // Case 2 Expected Result:
    // Each supported method remains available; only the nonblank DPA name is displayed.
    @Test
    fun `Given Case 2, When getDpaReport is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val recordedName =
                LocalizedTextDomain(mockedTransactionLanguageTag, "Recorded authority")
            val recordedCountry = LocalizedTextDomain(mockedTransactionLanguageTag, "PT")
            val blankIdentity = LocalizedTextDomain(mockedTransactionLanguageTag, " ")
            listOf(
                Triple(null, null, null),
                Triple(blankIdentity, blankIdentity, null),
                Triple(null, recordedCountry, null),
                Triple(blankIdentity, recordedCountry, null),
                Triple(recordedName, null, recordedName.text),
                Triple(recordedName, blankIdentity, recordedName.text),
            ).forEach { (name, country, expectedAuthority) ->
                listOf(
                    mockedActionPhoneUrl,
                    mockedActionMailUrl,
                    mockedActionWebUrl
                ).forEach { contact ->
                    val presentation = mockedActionPresentation.copy(
                        result = TransactionResultDomain.NotCompleted("Interrupted"),
                        claimsPresented = emptyList(),
                        party = mockedActionPresentation.party.copy(contacts = emptyList()),
                        registration = mockedTransactionRegistration.copy(
                            dpa = DpaContactDomain(
                                name,
                                country,
                                listOf(contact)
                            ),
                        ),
                    )
                    mockGetTransactionLogCall(presentation)

                    // When
                    interactor.getDpaReport(presentation.id).runFlowTest {
                        // Then
                        val report =
                            (awaitItem() as TransactionDetailsInteractorDpaReportPartialState.Success).report
                        assertEquals(expectedAuthority, report.authority)
                        assertEquals(
                            listOf(contact),
                            report.contacts.map { availableContact -> availableContact.url })
                        awaitComplete()
                    }
                }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController, uuidProvider)
        }
    }

    // Case 3:
    // 1. The requested parent is missing or is another transaction type.
    //
    // Case 3 Expected Result:
    // Reporting is unavailable without any recording or ID allocation.
    @Test
    fun `Given Case 3, When getDpaReport is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val transactions = listOf(null) +
                    mockedTransactionLogDomains.filterNot { transaction -> transaction is TransactionLogDomain.Presentation }
            transactions.forEach { transaction ->
                whenever(walletCoreTransactionLogController.getTransactionLog(mockedTransactionId))
                    .thenReturn(transaction)

                // When
                interactor.getDpaReport(mockedTransactionId).runFlowTest {
                    // Then
                    assertEquals(
                        TransactionDetailsInteractorDpaReportPartialState.Failure(
                            mockedActionUnavailable
                        ),
                        awaitItem(),
                    )
                    awaitComplete()
                }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController, uuidProvider)
        }
    }

    // Case 4:
    // 1. RP contacts exist, but DPA registration or usable DPA contacts are absent.
    //
    // Case 4 Expected Result:
    // Reporting is unavailable; no RP or replacement authority contact is used.
    @Test
    fun `Given Case 4, When getDpaReport is called, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val registrations = listOf(
                null,
                mockedTransactionRegistration.copy(dpa = null),
                mockedTransactionRegistration.copy(
                    dpa = DpaContactDomain(
                        LocalizedTextDomain(mockedTransactionLanguageTag, "Authority"),
                        LocalizedTextDomain(mockedTransactionLanguageTag, "GR"),
                        emptyList(),
                    )
                ),
                mockedTransactionRegistration.copy(
                    dpa = DpaContactDomain(
                        LocalizedTextDomain(mockedTransactionLanguageTag, "Authority"),
                        LocalizedTextDomain(mockedTransactionLanguageTag, "GR"),
                        mockedInvalidActionContacts,
                    )
                ),
            )
            registrations.forEach { registration ->
                val presentation = mockedActionPresentation.copy(registration = registration)
                mockGetTransactionLogCall(presentation)

                // When
                interactor.getDpaReport(presentation.id).runFlowTest {
                    // Then
                    assertEquals(
                        TransactionDetailsInteractorDpaReportPartialState.Failure(
                            mockedActionUnavailable
                        ),
                        awaitItem(),
                    )
                    awaitComplete()
                }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController, uuidProvider)
        }
    }

    // Case 5:
    // 1. Loading the parent throws, with or without a message.
    //
    // Case 5 Expected Result:
    // The ViewModel receives a failure state with no recording side effects.
    @Test
    fun `Given Case 5, When getDpaReport is called, Then Case 5 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            listOf(mockedExceptionWithMessage, mockedExceptionWithNoMessage).forEach { exception ->
                doThrow(exception).whenever(walletCoreTransactionLogController)
                    .getTransactionLog(mockedTransactionId)

                // When
                interactor.getDpaReport(mockedTransactionId).runFlowTest {
                    // Then
                    assertEquals(
                        TransactionDetailsInteractorDpaReportPartialState.Failure(
                            exception.localizedMessage ?: mockedGenericErrorMessage
                        ),
                        awaitItem(),
                    )
                    awaitComplete()
                }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController, uuidProvider)
        }
    }

    // Case 6:
    // 1. The DPA contact disappears after the reporting screen loads.
    //
    // Case 6 Expected Result:
    // Preparation rechecks availability and refuses the stale selection.
    @Test
    fun `Given Case 6, When preparing a stale DPA contact, Then Case 6 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockGetTransactionLogCall(mockedActionPresentation)
            interactor.getDpaReport(mockedActionPresentation.id).runFlowTest {
                assertTrue(awaitItem() is TransactionDetailsInteractorDpaReportPartialState.Success)
                awaitComplete()
            }
            mockGetTransactionLogCall(
                mockedActionPresentation.copy(
                    registration = mockedTransactionRegistration.copy(
                        dpa = DpaContactDomain(
                            LocalizedTextDomain(mockedTransactionLanguageTag, "Authority"),
                            LocalizedTextDomain(mockedTransactionLanguageTag, "GR"),
                            emptyList(),
                        ),
                    ),
                )
            )

            // When
            interactor.prepareDataProtectionAction(
                transactionId = mockedActionPresentation.id,
                action = TransactionDataProtectionAction.ReportSuspiciousTransaction,
                contactUrl = mockedActionMailUrl,
            ).runFlowTest {
                // Then
                assertEquals(
                    TransactionDetailsInteractorDataProtectionPartialState.Failure(
                        mockedActionUnavailable
                    ),
                    awaitItem(),
                )
                awaitComplete()
            }
            verifyNoInteractions(walletCoreTransactionRecordingController, uuidProvider)
        }
    }

    //endregion

    //region prepareDataProtectionAction

    // Case 1:
    // 1. Both action types are prepared for each supported communication method.
    //
    // Case 1 Expected Result:
    // The selected method, parent, target and ID are captured without any recording.
    @Test
    fun `Given Case 1, When preparing contacts, Then no attempt is recorded`() {
        coroutineRule.runTest {
            // Given
            mockGetTransactionLogCall(mockedActionPresentation)
            val contacts = mapOf(
                mockedActionWebUrl to CommunicationMethodDomain.Website,
                mockedActionMailUrl to CommunicationMethodDomain.Email,
                mockedActionPhoneUrl to CommunicationMethodDomain.Phone,
            )
            TransactionDataProtectionAction.entries.forEach { action ->
                contacts.forEach { (url, method) ->
                    // When
                    interactor.prepareDataProtectionAction(mockedActionPresentation.id, action, url)
                        .runFlowTest {
                            // Then
                            val pending =
                                (awaitItem() as TransactionDetailsInteractorDataProtectionPartialState.Success).pendingAction
                            assertEquals(mockedAttemptId, pending.id)
                            assertEquals(mockedActionPresentation, pending.presentation)
                            assertEquals(action, pending.action)
                            assertEquals(method, pending.communicationMethod)
                            assertEquals(url, pending.contactUrl)
                            assertEquals(url, pending.launchUrl.substringBefore('?'))
                            assertNull(pending.launchedAt)
                            assertEquals(
                                if (action == TransactionDataProtectionAction.ReportSuspiciousTransaction) mockedActionPresentation.registration?.dpa else null,
                                pending.authority,
                            )
                        }
                }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController)
        }
    }

    // Case 2:
    // 1. Email identity contains Unicode, line breaks and query delimiters.
    //
    // Case 2 Expected Result:
    // Recipient, subject and body remain independently encoded without writing a history row.
    @Test
    fun `Given Case 2, When preparing email, Then the draft is encoded safely`() {
        coroutineRule.runTest {
            // Given
            val presentation = mockedActionPresentation.copy(
                party = mockedActionPresentation.party.copy(
                    name = LocalizedTextDomain(
                        "el",
                        "Αρχή & Services +\r\nbcc=other@example.com"
                    )
                ),
            )
            mockGetTransactionLogCall(presentation)
            TransactionDataProtectionAction.entries.forEach { action ->
                // When
                interactor.prepareDataProtectionAction(presentation.id, action, mockedActionMailUrl)
                    .runFlowTest {
                        // Then
                        val url =
                            (awaitItem() as TransactionDetailsInteractorDataProtectionPartialState.Success).pendingAction.launchUrl
                        assertEquals(mockedActionMailUrl, url.substringBefore("?"))
                        assertEquals(url, URI(url).toASCIIString())
                        val parameters = decodeMailParameters(url)
                        assertEquals(setOf("subject", "body"), parameters.keys)
                        val prefix =
                            if (action == TransactionDataProtectionAction.RequestDataDeletion) "Erasure" else "Report"
                        assertEquals(
                            prefix + ": Αρχή & Services + bcc=other@example.com",
                            parameters["subject"]
                        )
                        assertEquals(
                            "Party:\n" + presentation.party.name?.text + "\n" + mockedTransactionQualifiedIdentifier.value +
                                    "\n" + mockedTransactionQualifiedIdentifier.schemeUri + "\nDate: " +
                                    presentation.time.formatLocalDateTime(pattern = FULL_DATETIME_PATTERN),
                            parameters["body"],
                        )
                    }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController)
        }
    }

    // Case 3:
    // 1. The parent is missing or is another transaction type.
    //
    // Case 3 Expected Result:
    // Preparation fails without recording.
    @Test
    fun `Given Case 3, When preparing an invalid parent, Then the action is unavailable`() {
        coroutineRule.runTest {
            // Given
            val transactions = listOf(null) +
                    mockedTransactionLogDomains.filterNot { transaction -> transaction is TransactionLogDomain.Presentation }
            transactions.forEach { transaction ->
                whenever(walletCoreTransactionLogController.getTransactionLog(mockedTransactionId)).thenReturn(
                    transaction
                )
                TransactionDataProtectionAction.entries.forEach { action ->
                    // When
                    interactor.prepareDataProtectionAction(
                        mockedTransactionId,
                        action,
                        mockedActionWebUrl
                    ).runFlowTest {
                        // Then
                        assertEquals(
                            TransactionDetailsInteractorDataProtectionPartialState.Failure(
                                mockedActionUnavailable
                            ),
                            awaitItem(),
                        )
                    }
                }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController)
        }
    }

    // Case 4:
    // 1. A contact has disappeared or the DDR presentation has no disclosed data.
    //
    // Case 4 Expected Result:
    // Stale or ineligible actions cannot launch or record.
    @Test
    fun `Given Case 4, When preparing a stale action, Then the action is unavailable`() {
        coroutineRule.runTest {
            // Given
            val cases = listOf(
                mockedActionPresentation.copy(claimsPresented = emptyList()) to TransactionDataProtectionAction.RequestDataDeletion,
                mockedActionPresentation.copy(
                    claimsPresented = listOf(
                        mockedTransactionClaims.first().copy(claims = emptyList())
                    ),
                ) to TransactionDataProtectionAction.RequestDataDeletion,
                mockedActionPresentation.copy(party = mockedActionPresentation.party.copy(contacts = emptyList())) to TransactionDataProtectionAction.RequestDataDeletion,
                mockedActionPresentation.copy(registration = null) to TransactionDataProtectionAction.ReportSuspiciousTransaction,
            )
            cases.forEach { (presentation, action) ->
                mockGetTransactionLogCall(presentation)
                // When
                interactor.prepareDataProtectionAction(presentation.id, action, mockedActionWebUrl)
                    .runFlowTest {
                        // Then
                        assertEquals(
                            TransactionDetailsInteractorDataProtectionPartialState.Failure(
                                mockedActionUnavailable
                            ),
                            awaitItem(),
                        )
                    }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController)
        }
    }

    // Case 5:
    // 1. Loading the parent throws, with or without a message.
    //
    // Case 5 Expected Result:
    // The interactor returns a failure state without recording.
    @Test
    fun `Given Case 5, When preparing fails, Then an error state is returned`() {
        coroutineRule.runTest {
            // Given
            listOf(mockedExceptionWithMessage, mockedExceptionWithNoMessage).forEach { exception ->
                doThrow(exception).whenever(walletCoreTransactionLogController)
                    .getTransactionLog(mockedTransactionId)
                TransactionDataProtectionAction.entries.forEach { action ->
                    // When
                    interactor.prepareDataProtectionAction(
                        mockedTransactionId,
                        action,
                        mockedActionWebUrl
                    ).runFlowTest {
                        // Then
                        assertEquals(
                            TransactionDetailsInteractorDataProtectionPartialState.Failure(
                                exception.localizedMessage ?: mockedGenericErrorMessage
                            ),
                            awaitItem(),
                        )
                    }
                }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController)
        }
    }

    // Case 6:
    // 1. Presentation data changes after details load; the user then starts two distinct attempts.
    //
    // Case 6 Expected Result:
    // Each preparation reloads the parent and receives its own attempt ID.
    @Test
    fun `Given Case 6, When preparing separate attempts, Then fresh data and distinct IDs are used`() {
        coroutineRule.runTest {
            // Given
            mockGetTransactionLogCall(mockedActionPresentation)
            interactor.getTransactionDetails(mockedActionPresentation.id)
                .runFlowTest { awaitItem() }
            val updated =
                mockedActionPresentation.copy(claimsPresented = mockedNestedTransactionClaims)
            mockGetTransactionLogCall(updated)
            val ids = listOf(mockedAttemptId, mockedAttemptId + "-next")
            whenever(uuidProvider.provideUuid()).thenReturn(ids[0], ids[1])
            ids.forEach { id ->
                // When
                interactor.prepareDataProtectionAction(
                    updated.id,
                    TransactionDataProtectionAction.RequestDataDeletion,
                    mockedActionWebUrl
                ).runFlowTest {
                    // Then
                    val pending =
                        (awaitItem() as TransactionDetailsInteractorDataProtectionPartialState.Success).pendingAction
                    assertEquals(id, pending.id)
                    assertEquals(updated, pending.presentation)
                }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController)
        }
    }

    // Case 7:
    // 1. Available intermediary names contain Unicode and URI delimiters, or are missing.
    //
    // Case 7 Expected Result:
    // Only DPAR drafts include a nonblank intermediary name, encoded inside the body.
    @Test
    fun `Given Case 7, When preparing email with an intermediary, Then Case 7 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            listOf(
                null,
                " ",
                "Μεσάζων & Service +\r\nbcc=other@example.com"
            ).forEach { intermediaryName ->
                val presentation = mockedActionPresentation.copy(
                    intermediary = mockedTransactionIntermediary.copy(name = intermediaryName?.let { name ->
                        LocalizedTextDomain("el", name)
                    }),
                )
                mockGetTransactionLogCall(presentation)
                TransactionDataProtectionAction.entries.forEach { action ->
                    // When
                    interactor.prepareDataProtectionAction(
                        presentation.id,
                        action,
                        mockedActionMailUrl
                    ).runFlowTest {
                        // Then
                        val url =
                            (awaitItem() as TransactionDetailsInteractorDataProtectionPartialState.Success).pendingAction.launchUrl
                        assertEquals(mockedActionMailUrl, url.substringBefore("?"))
                        assertEquals(url, URI(url).toASCIIString())
                        val parameters = decodeMailParameters(url)
                        assertEquals(setOf("subject", "body"), parameters.keys)
                        val prefix =
                            if (action == TransactionDataProtectionAction.RequestDataDeletion) "Erasure" else "Report"
                        assertEquals(
                            prefix + ": " + presentation.party.name?.text,
                            parameters["subject"]
                        )
                        val intermediary =
                            if (action == TransactionDataProtectionAction.ReportSuspiciousTransaction &&
                                !intermediaryName.isNullOrBlank()
                            ) "\n\nIntermediary:\n$intermediaryName" else ""
                        assertEquals(
                            "Party:\n" + presentation.party.name?.text + "\n" + mockedTransactionQualifiedIdentifier.value +
                                    "\n" + mockedTransactionQualifiedIdentifier.schemeUri + "\nDate: " +
                                    presentation.time.formatLocalDateTime(pattern = FULL_DATETIME_PATTERN) + intermediary,
                            parameters["body"],
                        )
                        awaitComplete()
                    }
                }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController)
        }
    }

    // Case 8:
    // 1. The recorded email contact contains encoded mailbox characters.
    //
    // Case 8 Expected Result:
    // Both actions accept the normalized target and preserve the recipient in the prepared draft.
    @Test
    fun `Given Case 8, When prepareDataProtectionAction is called, Then Case 8 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val contacts = listOf("MAILTO:privacy%2Bwallet%2frequests%3ddelete@EXAMPLE.COM")
            val expectedRecipient = "mailto:privacy+wallet%2Frequests%3Ddelete@example.com"
            val presentation = mockedActionPresentation.copy(
                party = mockedActionPresentation.party.copy(contacts = contacts),
                registration = mockedTransactionRegistration.copy(
                    dpa = mockedTransactionRegistration.dpa!!.copy(contacts = contacts),
                ),
            )
            mockGetTransactionLogCall(presentation)

            TransactionDataProtectionAction.entries.forEach { action ->
                // When
                interactor.prepareDataProtectionAction(
                    transactionId = presentation.id,
                    action = action,
                    contactUrl = expectedRecipient,
                ).runFlowTest {
                    // Then
                    val pending =
                        (awaitItem() as TransactionDetailsInteractorDataProtectionPartialState.Success).pendingAction
                    assertEquals(CommunicationMethodDomain.Email, pending.communicationMethod)
                    assertEquals(expectedRecipient, pending.contactUrl)
                    assertEquals(expectedRecipient, pending.launchUrl.substringBefore('?'))
                    assertEquals(
                        "privacy+wallet/requests=delete@example.com",
                        URI(pending.launchUrl.substringBefore('?')).schemeSpecificPart,
                    )
                    assertEquals(
                        setOf("subject", "body"),
                        decodeMailParameters(pending.launchUrl).keys
                    )
                    assertNull(pending.launchedAt)
                    awaitComplete()
                }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController)
        }
    }

    //endregion

    //region recordDataProtectionAction

    // Case 1:
    // 1. An action was prepared but no successful launch was acknowledged.
    //
    // Case 1 Expected Result:
    // No history row is recorded for a cancelled or failed launch.
    @Test
    fun `Given Case 1, When saving an unlaunched action, Then recording is refused`() {
        coroutineRule.runTest {
            // Given
            val pending = mockedPendingAction.copy(launchedAt = null)
            // When
            interactor.recordDataProtectionAction(pending).runFlowTest {
                // Then
                assertEquals(
                    RecordTransactionPartialState.Failure(mockedGenericErrorMessage),
                    awaitItem()
                )
            }
            verifyNoInteractions(walletCoreTransactionRecordingController)
        }
    }

    // Case 2:
    // 1. Both actions launch through each supported method.
    //
    // Case 2 Expected Result:
    // The selected method, stable ID and launch instant reach the recorder.
    @Test
    fun `Given Case 2, When saving launched actions, Then exact attempt data reaches the recorder`() {
        coroutineRule.runTest {
            // Given
            mockRecordActionCall(RecordTransactionPartialState.Success)
            CommunicationMethodDomain.entries.forEach { method ->
                // When
                val deletion = mockedPendingAction.copy(communicationMethod = method)
                val report = deletion.copy(
                    action = TransactionDataProtectionAction.ReportSuspiciousTransaction,
                    authority = mockedActionPresentation.registration?.dpa,
                )
                listOf(deletion, report).forEach { pending ->
                    interactor.recordDataProtectionAction(pending).runFlowTest {
                        // Then
                        assertEquals(RecordTransactionPartialState.Success, awaitItem())
                    }
                }
                verify(walletCoreTransactionRecordingController).recordDataDeletionRequest(
                    id = mockedAttemptId,
                    time = mockedLaunchTime,
                    presentation = mockedActionPresentation,
                    communicationMethod = method,
                )
                verify(walletCoreTransactionRecordingController).recordDpaReport(
                    id = mockedAttemptId,
                    time = mockedLaunchTime,
                    parentPresentationId = mockedActionPresentation.id,
                    authority = mockedActionPresentation.registration!!.dpa!!,
                    communicationMethod = method,
                )
            }
            verifyNoInteractions(walletCoreTransactionLogController, uuidProvider)
        }
    }

    // Case 3:
    // 1. Saving fails; the caller retries the same launched action.
    //
    // Case 3 Expected Result:
    // Both writes use identical ID, time and snapshot; no fresh ID or parent lookup occurs.
    @Test
    fun `Given Case 3, When retrying a failed save, Then the original attempt is reused`() {
        coroutineRule.runTest {
            // Given
            val failure = RecordTransactionPartialState.Failure(mockedGenericErrorMessage)
            whenever(
                walletCoreTransactionRecordingController.recordDataDeletionRequest(
                    any(),
                    any(),
                    any(),
                    any()
                )
            )
                .thenReturn(failure, RecordTransactionPartialState.Success)
            listOf(failure, RecordTransactionPartialState.Success).forEach { expected ->
                // When
                interactor.recordDataProtectionAction(mockedPendingAction).runFlowTest {
                    // Then
                    assertEquals(expected, awaitItem())
                }
            }
            verify(
                walletCoreTransactionRecordingController,
                org.mockito.kotlin.times(2)
            ).recordDataDeletionRequest(
                id = mockedAttemptId,
                time = mockedLaunchTime,
                presentation = mockedActionPresentation,
                communicationMethod = CommunicationMethodDomain.Website,
            )
            verifyNoInteractions(walletCoreTransactionLogController, uuidProvider)
        }
    }

    // Case 4:
    // 1. The selected authority differs from the authority recorded in the presentation.
    //
    // Case 4 Expected Result:
    // Reporting stores the selected authority.
    @Test
    fun `Given Case 4, When saving a report, Then the selected authority is recorded`() {
        coroutineRule.runTest {
            // Given
            mockRecordActionCall(RecordTransactionPartialState.Success)
            val authority = DpaContactDomain(
                LocalizedTextDomain(mockedTransactionLanguageTag, "Selected authority"),
                LocalizedTextDomain(mockedTransactionLanguageTag, "Selected country"),
                emptyList(),
            )
            val pending = mockedPendingAction.copy(
                action = TransactionDataProtectionAction.ReportSuspiciousTransaction,
                authority = authority,
            )
            // When
            interactor.recordDataProtectionAction(pending).runFlowTest {
                // Then
                assertEquals(RecordTransactionPartialState.Success, awaitItem())
            }
            verify(walletCoreTransactionRecordingController).recordDpaReport(
                id = mockedAttemptId,
                time = mockedLaunchTime,
                parentPresentationId = mockedActionPresentation.id,
                authority = authority,
                communicationMethod = CommunicationMethodDomain.Website,
            )
        }
    }

    // Case 5:
    // 1. Recording throws, with or without a message.
    //
    // Case 5 Expected Result:
    // The interactor maps the exception to Failure.
    @Test
    fun `Given Case 5, When the recorder throws, Then an error state is returned`() {
        coroutineRule.runTest {
            // Given
            listOf(mockedExceptionWithMessage, mockedExceptionWithNoMessage).forEach { exception ->
                whenever(
                    walletCoreTransactionRecordingController.recordDataDeletionRequest(
                        any(),
                        any(),
                        any(),
                        any()
                    )
                )
                    .thenThrow(exception)
                // When
                interactor.recordDataProtectionAction(mockedPendingAction).runFlowTest {
                    // Then
                    assertEquals(
                        RecordTransactionPartialState.Failure(
                            exception.localizedMessage ?: mockedGenericErrorMessage
                        ),
                        awaitItem(),
                    )
                }
            }
        }
    }

    // Case 6:
    // 1. Recording is pending.
    //
    // Case 6 Expected Result:
    // Success is emitted only after the recorder acknowledges storage.
    @Test
    fun `Given Case 6, When saving is pending, Then success waits for acknowledgement`() {
        coroutineRule.runTest {
            // Given
            val started = CompletableDeferred<Unit>()
            val finished = CompletableDeferred<Unit>()
            whenever(
                walletCoreTransactionRecordingController.recordDataDeletionRequest(
                    any(),
                    any(),
                    any(),
                    any()
                )
            )
                .doSuspendableAnswer {
                    started.complete(Unit)
                    finished.await()
                    RecordTransactionPartialState.Success
                }
            // When
            interactor.recordDataProtectionAction(mockedPendingAction).runFlowTest {
                started.await()
                // Then
                expectNoEvents()
                finished.complete(Unit)
                assertEquals(RecordTransactionPartialState.Success, awaitItem())
            }
        }
    }

    // Case 7:
    // 1. A report has no selected authority.
    //
    // Case 7 Expected Result:
    // The missing authority produces a failure without recording.
    @Test
    fun `Given Case 7, When saving a report without an authority, Then recording is refused`() {
        coroutineRule.runTest {
            // Given
            val pending =
                mockedPendingAction.copy(action = TransactionDataProtectionAction.ReportSuspiciousTransaction)
            // When
            interactor.recordDataProtectionAction(pending).runFlowTest {
                // Then
                assertEquals(
                    RecordTransactionPartialState.Failure(mockedGenericErrorMessage),
                    awaitItem()
                )
            }
            verifyNoInteractions(walletCoreTransactionRecordingController)
        }
    }
    //endregion

    //region observePresentationActionCounts

    // Case 1: No related actions returns loading followed by both zero counts.
    @Test
    fun `Given Case 1, When observePresentationActionCounts is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockObservePresentationActionsCall(flowOf(emptyList()))

            // When
            interactor.observePresentationActionCounts(mockedPresentationLogDomain.id).runFlowTest {
                // Then
                assertEquals(PresentationActionCountsUiState.Loading, awaitItem())
                assertEquals(PresentationActionCountsUiState.Content(0, 0), awaitItem())
                awaitComplete()
            }
            verify(walletCoreTransactionLogController).observePresentationActions(
                mockedPresentationLogDomain.id
            )
            verifyNoMoreInteractions(walletCoreTransactionLogController)
            verifyNoInteractions(walletCoreTransactionRecordingController)
        }
    }

    // Case 2: Both action types are counted regardless of their recorded outcome.
    @Test
    fun `Given Case 2, When observePresentationActionCounts is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockObservePresentationActionsCall(
                flowOf(
                    listOf(
                        mockedDataDeletionLogDomain,
                        mockedDataDeletionLogDomain.copy(
                            id = "second-ddr",
                            result = TransactionResultDomain.NotCompleted(reason = null),
                        ),
                        mockedDpaReportLogDomain,
                        mockedDpaReportLogDomain.copy(
                            id = "second-dpar",
                            result = TransactionResultDomain.NotCompleted(reason = mockedGenericErrorMessage),
                        ),
                    )
                )
            )

            // When
            interactor.observePresentationActionCounts(mockedPresentationLogDomain.id).runFlowTest {
                // Then
                assertEquals(PresentationActionCountsUiState.Loading, awaitItem())
                assertEquals(PresentationActionCountsUiState.Content(2, 2), awaitItem())
                awaitComplete()
            }
        }
    }

    // Case 3: Each new snapshot replaces the counts, including removals and an empty history.
    @Test
    fun `Given Case 3, When observePresentationActionCounts is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockObservePresentationActionsCall(
                flowOf(
                    listOf(mockedDataDeletionLogDomain),
                    listOf(mockedDataDeletionLogDomain, mockedDpaReportLogDomain),
                    listOf(mockedDpaReportLogDomain),
                    emptyList(),
                )
            )

            // When
            interactor.observePresentationActionCounts(mockedPresentationLogDomain.id).runFlowTest {
                // Then
                assertEquals(PresentationActionCountsUiState.Loading, awaitItem())
                assertEquals(PresentationActionCountsUiState.Content(1, 0), awaitItem())
                assertEquals(PresentationActionCountsUiState.Content(1, 1), awaitItem())
                assertEquals(PresentationActionCountsUiState.Content(0, 1), awaitItem())
                assertEquals(PresentationActionCountsUiState.Content(0, 0), awaitItem())
                awaitComplete()
            }
        }
    }

    // Case 4: A later read failure returns an error instead of zero counts or another recording.
    @Test
    fun `Given Case 4, When observePresentationActionCounts is called, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            listOf(mockedExceptionWithMessage, mockedExceptionWithNoMessage).forEach { exception ->
                // Given
                mockObservePresentationActionsCall(
                    flow {
                        emit(listOf(mockedDataDeletionLogDomain))
                        throw exception
                    }
                )

                // When
                interactor.observePresentationActionCounts(mockedPresentationLogDomain.id)
                    .runFlowTest {
                        // Then
                        assertEquals(PresentationActionCountsUiState.Loading, awaitItem())
                        assertEquals(PresentationActionCountsUiState.Content(1, 0), awaitItem())
                        assertEquals(
                            PresentationActionCountsUiState.Failure(
                                exception.localizedMessage ?: mockedGenericErrorMessage
                            ),
                            awaitItem(),
                        )
                        awaitComplete()
                    }
            }
            verifyNoInteractions(walletCoreTransactionRecordingController)
        }
    }

    // Case 5: Failing to start observation returns the same typed failure.
    @Test
    fun `Given Case 5, When observePresentationActionCounts is called, Then Case 5 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(
                walletCoreTransactionLogController.observePresentationActions(
                    mockedPresentationLogDomain.id
                )
            )
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.observePresentationActionCounts(mockedPresentationLogDomain.id).runFlowTest {
                // Then
                assertEquals(PresentationActionCountsUiState.Loading, awaitItem())
                assertEquals(
                    PresentationActionCountsUiState.Failure(
                        mockedExceptionWithMessage.localizedMessage ?: mockedGenericErrorMessage,
                    ),
                    awaitItem(),
                )
                awaitComplete()
            }
        }
    }

    // Case 6: Cancelling the consumer cancels the active observation.
    @Test
    fun `Given Case 6, When observePresentationActionCounts is called, Then Case 6 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val observationStopped = CompletableDeferred<Unit>()
            mockObservePresentationActionsCall(
                flow {
                    try {
                        emit(listOf(mockedDpaReportLogDomain))
                        awaitCancellation()
                    } finally {
                        observationStopped.complete(Unit)
                    }
                }
            )

            // When
            interactor.observePresentationActionCounts(mockedPresentationLogDomain.id).runFlowTest {
                // Then
                assertEquals(PresentationActionCountsUiState.Loading, awaitItem())
                assertEquals(PresentationActionCountsUiState.Content(0, 1), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            observationStopped.await()
            verifyNoInteractions(walletCoreTransactionRecordingController)
        }
    }
    //endregion

    //region recorded transaction data

    // Case 1:
    // 1. A presentation with privacy actions receives two recorded approval entries.
    //
    // Case 1 Expected Result:
    // One collapsed group follows the claim sections; its data does not alter status or actions.
    @Test
    fun `Given recorded signing requests, When details are prepared, Then presentation behavior is preserved`() {
        coroutineRule.runTest {
            // Given
            val baseline = mockedPresentationLogDomain.copy(
                party = mockedTransactionPartyWithContacts,
                registration = mockedTransactionRegistration,
            )
            mockGetTransactionLogCall(response = baseline)
            lateinit var original: TransactionDetailsUi
            interactor.getTransactionDetails(transactionId = baseline.id).runFlowTest {
                original =
                    (awaitItem() as TransactionDetailsInteractorPartialState.Success).transactionDetailsUi
            }
            val recorded = baseline.copy(
                transactionData = List(2) { mockedTransactionDataApproval.copy(displayName = null) },
            )
            mockGetTransactionLogCall(response = recorded)

            // When
            interactor.getTransactionDetails(transactionId = recorded.id).runFlowTest {
                // Then
                val details =
                    (awaitItem() as TransactionDetailsInteractorPartialState.Success).transactionDetailsUi
                val body = details.body as TransactionDetailsBodyUi.Presentation
                assertEquals(original.transactionDetailsCardUi, details.transactionDetailsCardUi)
                assertEquals(original.body, body.copy(transactionData = null))
                val section = body.transactionData!!
                assertEquals(listOf(body.requested, body.shared, section), body.sections)
                assertEquals("SIGNING REQUEST", section.title)
                assertTrue(section.items.isEmpty())
                val group = section.groups.single()
                assertEquals("Signature details", group.header.textValue())
                assertEquals(
                    ListItemTrailingContentDataUi.Icon(iconData = AppIcons.KeyboardArrowDown),
                    group.header.trailingContentData,
                )
                assertEquals(2, group.items.count { row -> row.header.overlineText == "Document" })
                assertTrue(group.items.none { row -> row.header.overlineText == "Transaction type" })
                assertTrue(group.items.all { row -> row.header.trailingContentData == null })
                val groupIds =
                    body.sections.flatMap { item -> item.groups }.map { item -> item.header.itemId }
                assertEquals(groupIds.size, groupIds.distinct().size)
            }
            verifyNoInteractions(walletCoreTransactionRecordingController)
        }
    }

    // Case 2:
    // 1. An older presentation has no recorded transaction data.
    //
    // Case 2 Expected Result:
    // Only its existing requested/shared sections remain.
    @Test
    fun `Given a presentation without transaction data, When details are prepared, Then no signing request section is added`() {
        coroutineRule.runTest {
            // Given
            mockGetTransactionLogCall(response = mockedPresentationLogDomain)

            // When
            interactor.getTransactionDetails(transactionId = mockedPresentationLogDomain.id)
                .runFlowTest {
                    // Then
                    val body = (awaitItem() as TransactionDetailsInteractorPartialState.Success)
                        .transactionDetailsUi.body as TransactionDetailsBodyUi.Presentation
                    assertNull(body.transactionData)
                    assertEquals(listOf(body.requested, body.shared), body.sections)
                }
        }
    }

    // Case 3:
    // 1. A failed presentation contains one unavailable transaction-data entry and one valid approval.
    //
    // Case 3 Expected Result:
    // Both entries are represented without changing the recorded failure or hiding shared claims.
    @Test
    fun `Given unavailable data in a failed presentation, When details are prepared, Then the remaining record stays visible`() {
        coroutineRule.runTest {
            // Given
            val transaction = mockedPresentationLogDomain.copy(
                result = TransactionResultDomain.NotCompleted(reason = mockedGenericErrorMessage),
                transactionData = listOf(
                    PresentationTransactionDataDomain.Unavailable,
                    mockedTransactionDataApproval.copy(displayName = null),
                ),
            )
            mockGetTransactionLogCall(response = transaction)

            // When
            interactor.getTransactionDetails(transactionId = transaction.id).runFlowTest {
                // Then
                val details =
                    (awaitItem() as TransactionDetailsInteractorPartialState.Success).transactionDetailsUi
                assertEquals(false, details.transactionDetailsCardUi.transactionIsCompleted)
                assertEquals(
                    mockedGenericErrorMessage,
                    details.transactionDetailsCardUi.nonCompletionReason
                )
                val body = details.body as TransactionDetailsBodyUi.Presentation
                assertTrue(body.shared.groups.isNotEmpty())
                val rows = body.transactionData!!.groups.single().items
                assertTrue(rows.any { row -> row.header.textValue() == "Details for this transaction are unavailable." })
                assertTrue(rows.any { row -> row.header.textValue() == mockedTransactionDataApproval.documentDigests.single().label })
            }
        }
    }

    //endregion

    //region helper functions
    private fun ListItemDataUi.textValue(): String =
        (mainContentData as ListItemMainContentDataUi.Text).text

    private fun mockObservePresentationActionsCall(actions: Flow<List<TransactionLogDomain.PresentationAction>>) {
        whenever(
            walletCoreTransactionLogController.observePresentationActions(
                mockedPresentationLogDomain.id
            )
        )
            .thenReturn(actions)
    }

    private suspend fun mockRecordActionCall(result: RecordTransactionPartialState) {
        whenever(
            walletCoreTransactionRecordingController.recordDataDeletionRequest(
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(result)
        whenever(
            walletCoreTransactionRecordingController.recordDpaReport(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(result)
    }

    private fun decodeMailParameters(url: String): Map<String, String> {
        return url.substringAfter("?").split("&").associate { parameter ->
            parameter.substringBefore("=") to
                    URLDecoder.decode(parameter.substringAfter("="), Charsets.UTF_8.name())
        }
    }

    private suspend fun mockDeleteTransactionLogCall(transactionId: String) {
        whenever(walletCoreTransactionLogController.deleteTransactionLog(id = transactionId)).thenReturn(
            Unit
        )
    }

    private suspend fun mockDeleteTransactionLogFailure(exception: Throwable) {
        whenever(walletCoreTransactionLogController.deleteTransactionLog(id = mockedTransactionId))
            .thenThrow(exception)
    }

    private suspend fun mockGetTransactionLogCall(response: TransactionLogDomain) {
        whenever(walletCoreTransactionLogController.getTransactionLog(id = response.id)).thenReturn(
            response
        )
    }

    private fun mockTransactionDetailsStrings() {
        (mockedTransactionDetailsStrings + mockedPrivacyActionStrings +
                (R.string.transaction_details_action_unavailable to mockedActionUnavailable))
            .forEach { (resourceId, value) ->
                whenever(resourceProvider.getString(resourceId)).thenReturn(value)
            }
        listOf(
            R.string.data_deletion_website_description,
            R.string.data_deletion_email_description,
            R.string.data_deletion_phone_description,
            R.string.data_deletion_website_button,
            R.string.data_deletion_email_button,
            R.string.data_deletion_phone_button,
            R.string.data_deletion_retention_notice,
        ).forEach { resourceId ->
            whenever(resourceProvider.getString(eq(resourceId), any<String>()))
                .thenAnswer { invocation ->
                    String.format(
                        Locale.ROOT,
                        mockedPrivacyActionStrings.getValue(resourceId),
                        invocation.getArgument<String>(1),
                    )
                }
        }
        listOf(
            R.string.transaction_details_deletion_email_subject to "Erasure",
            R.string.transaction_details_report_email_subject to "Report",
        ).forEach { (resourceId, prefix) ->
            whenever(resourceProvider.getString(eq(resourceId), any<String>()))
                .thenAnswer { invocation -> prefix + ": " + invocation.getArgument<String>(1) }
        }
        listOf(
            R.string.transaction_details_deletion_email_body,
            R.string.transaction_details_report_email_body,
        ).forEach { resourceId ->
            whenever(resourceProvider.getString(eq(resourceId), any<String>(), any<String>()))
                .thenAnswer { invocation ->
                    "Party:\n" + invocation.getArgument<String>(1) + "\nDate: " + invocation.getArgument<String>(
                        2
                    )
                }
        }
        whenever(
            resourceProvider.getString(
                eq(R.string.transaction_details_report_email_intermediary),
                any<String>()
            )
        )
            .thenAnswer { invocation -> "\n\nIntermediary:\n" + invocation.getArgument<String>(1) }
    }
    //endregion

    //region mocked objects
    private val mockedPrivacyActionStrings = mapOf(
        R.string.data_deletion_relying_party_default_name to "Relying party",
        R.string.data_deletion_website_description to "Website description for %1\$s",
        R.string.data_deletion_email_description to "Email description for %1\$s",
        R.string.data_deletion_phone_description to "Phone description for %1\$s",
        R.string.data_deletion_website_responsibility to "<b>Website responsibility.</b> Continue outside.",
        R.string.data_deletion_email_responsibility to "<b>Email responsibility.</b> Continue outside.",
        R.string.data_deletion_phone_responsibility to "<b>Phone responsibility.</b> Continue outside.",
        R.string.data_deletion_retention_notice to "Retention for %1\$s",
        R.string.data_deletion_website_button to "Website action for %1\$s",
        R.string.data_deletion_email_button to "Email action for %1\$s",
        R.string.data_deletion_phone_button to "Phone action for %1\$s",
        R.string.dpa_report_responsibility to "<b>Report responsibility.</b> Continue outside.",
        R.string.dpa_report_follow_up to "Authority follows up.",
        R.string.dpa_report_call_button to "Call",
        R.string.dpa_report_email_button to "Open email",
        R.string.dpa_report_website_button to "Visit website",
    )

    private val mockedActionWebUrl = "https://example.com/contact"
    private val mockedActionMailUrl = "mailto:privacy+wallet@example.com"
    private val mockedActionPhoneUrl = "tel:+302101234567"
    private val mockedActionUnavailable = "Action unavailable"
    private val mockedInvalidActionContacts = listOf(
        "GR", "", "javascript:alert(1)", "intent://launch", "file:///private", "https://",
        "mailto:authority@example.com?bcc=other@example.com", "authority@example.com#fragment",
        "authority%0d%0a@example.com", "https://user:password@example.com",
        "tel:+302101234567;ext=99", "mailto:authority@example.com\r\nbcc:other@example.com",
    )
    private val mockedMixedContacts = listOf(
        " " + mockedActionWebUrl + " ", "privacy+wallet@example.com", "+30 (210) 123-4567",
        mockedActionWebUrl, "MAILTO:privacy+wallet@example.com", "TEL:+302101234567",
        "o'connor@example.com",
    ) + mockedInvalidActionContacts
    private val mockedActionPresentation = mockedDetailedPresentationLogDomain.copy(
        party = mockedTransactionPartyWithContacts.copy(
            contacts = listOf(mockedActionWebUrl, mockedActionMailUrl, mockedActionPhoneUrl),
        ),
        registration = mockedTransactionRegistration.copy(
            dpa = mockedTransactionRegistration.dpa!!.copy(
                contacts = listOf(mockedActionWebUrl, mockedActionMailUrl, mockedActionPhoneUrl),
            ),
        ),
    )

    private val mockedAttemptId = "privacy-attempt"
    private val mockedLaunchTime = Instant.parse("2026-09-17T08:30:00Z")
    private val mockedPendingAction = PendingTransactionActionUi(
        id = mockedAttemptId,
        presentation = mockedActionPresentation,
        action = TransactionDataProtectionAction.RequestDataDeletion,
        contactUrl = mockedActionWebUrl,
        launchUrl = mockedActionWebUrl,
        communicationMethod = CommunicationMethodDomain.Website,
        authority = null,
        launchedAt = mockedLaunchTime,
    )

    private val mockedTransactionId = "mockedTransactionId"
    private val mockedCreationLocalDateTime: LocalDateTime =
        LocalDateTime.of(2026, 3, 15, 14, 30, 0)
    private val mockedIssuanceLabel = "Issuance"
    private val mockedPresentationLabel = "Presentation"
    private val mockedSigningLabel = "Signing"
    private val mockedCompletedLabel = "Completed"
    private val mockedNotCompletedLabel = "Not completed"
    //endregion
}