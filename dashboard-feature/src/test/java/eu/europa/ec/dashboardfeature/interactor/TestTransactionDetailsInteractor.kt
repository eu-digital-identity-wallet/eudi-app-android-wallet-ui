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
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.model.TransactionLogDataDomain
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsCardUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsDataSharedHolderUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsUi
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.PresentedClaim
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.PresentedDocument
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.StringResourceProviderMocker.mockTransformToUiItemsStrings
import eu.europa.ec.testfeature.util.mockedDefaultLocale
import eu.europa.ec.testfeature.util.mockedExceptionWithMessage
import eu.europa.ec.testfeature.util.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.util.mockedGenericErrorMessage
import eu.europa.ec.testfeature.util.mockedMdocPidFormat
import eu.europa.ec.testfeature.util.mockedSdJwtPidFormat
import eu.europa.ec.testfeature.util.mockedUuid
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

class TestTransactionDetailsInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var walletCoreDocumentsController: WalletCoreDocumentsController

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
            walletCoreDocumentsController = walletCoreDocumentsController,
            resourceProvider = resourceProvider,
            uuidProvider = uuidProvider,
        )

        whenever(resourceProvider.genericErrorMessage()).thenReturn(mockedGenericErrorMessage)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region getTransactionDetails

    // Case 1:
    // 1. walletCoreDocumentsController.getTransactionLog returns an IssuanceLog with
    //    status = TransactionLog.Status.Completed.

    // Case 1 Expected Result:
    // Success with a TransactionDetailsCardUi where transactionIsCompleted is true,
    // relyingPartyName and relyingPartyIsVerified are null, and dataSharedItems is empty.
    @Test
    fun `Given Case 1, When getTransactionDetails is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val transaction = TransactionLogDataDomain.IssuanceLog(
                id = mockedTransactionId,
                name = mockedTransactionName,
                status = TransactionLog.Status.Completed,
                creationLocalDateTime = mockedCreationLocalDateTime,
                creationLocalDate = mockedCreationLocalDate,
            )
            whenever(walletCoreDocumentsController.getTransactionLog(id = mockedTransactionId))
                .thenReturn(transaction)
            mockTransactionDetailsStrings()

            // When
            interactor.getTransactionDetails(transactionId = mockedTransactionId).runFlowTest {
                // Then
                val expectedResult = TransactionDetailsInteractorPartialState.Success(
                    transactionDetailsUi = TransactionDetailsUi(
                        transactionId = mockedTransactionId,
                        transactionDetailsCardUi = TransactionDetailsCardUi(
                            transactionTypeLabel = mockedIssuanceLabel,
                            transactionStatusLabel = mockedCompletedLabel,
                            transactionIsCompleted = true,
                            transactionDate = mockedCreationLocalDateTime.formatLocalDateTime(
                                pattern = FULL_DATETIME_PATTERN
                            ),
                            relyingPartyName = null,
                            relyingPartyIsVerified = null,
                        ),
                        transactionDetailsDataShared = TransactionDetailsDataSharedHolderUi(
                            dataSharedItems = emptyList()
                        ),
                        transactionDetailsDataSigned = null,
                    )
                )
                assertEquals(expectedResult, awaitItem())
            }
        }
    }

    // Case 2:
    // 1. walletCoreDocumentsController.getTransactionLog returns an IssuanceLog with
    //    status = TransactionLog.Status.Incomplete (mapped to TransactionStatusUi.Failed).

    // Case 2 Expected Result:
    // Success with transactionIsCompleted = false and the failed status label.
    @Test
    fun `Given Case 2, When getTransactionDetails is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val transaction = TransactionLogDataDomain.IssuanceLog(
                id = mockedTransactionId,
                name = mockedTransactionName,
                status = TransactionLog.Status.Incomplete,
                creationLocalDateTime = mockedCreationLocalDateTime,
                creationLocalDate = mockedCreationLocalDate,
            )
            whenever(walletCoreDocumentsController.getTransactionLog(id = mockedTransactionId))
                .thenReturn(transaction)
            mockTransactionDetailsStrings()

            // When
            interactor.getTransactionDetails(transactionId = mockedTransactionId).runFlowTest {
                // Then
                val result = awaitItem()
                assertTrue(result is TransactionDetailsInteractorPartialState.Success)
                result as TransactionDetailsInteractorPartialState.Success

                val card = result.transactionDetailsUi.transactionDetailsCardUi
                assertEquals(false, card.transactionIsCompleted)
                assertEquals(mockedFailedLabel, card.transactionStatusLabel)
                assertEquals(mockedIssuanceLabel, card.transactionTypeLabel)
            }
        }
    }

    // Case 3:
    // 1. walletCoreDocumentsController.getTransactionLog returns a PresentationLog with
    //    empty documents list and a relying party.

    // Case 3 Expected Result:
    // Success with relyingPartyName/relyingPartyIsVerified taken from the relying party and
    // dataSharedItems = empty (since documents is empty).
    @Test
    fun `Given Case 3, When getTransactionDetails is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val relyingParty = TransactionLog.RelyingParty(
                name = mockedRelyingPartyName,
                isVerified = true,
                certificateChain = emptyList(),
                readerAuth = null,
            )
            val transaction = TransactionLogDataDomain.PresentationLog(
                id = mockedTransactionId,
                name = mockedTransactionName,
                status = TransactionLog.Status.Completed,
                creationLocalDateTime = mockedCreationLocalDateTime,
                creationLocalDate = mockedCreationLocalDate,
                relyingParty = relyingParty,
                documents = emptyList(),
            )
            whenever(walletCoreDocumentsController.getTransactionLog(id = mockedTransactionId))
                .thenReturn(transaction)
            mockTransactionDetailsStrings()
            whenever(resourceProvider.getLocale()).thenReturn(mockedDefaultLocale)

            // When
            interactor.getTransactionDetails(transactionId = mockedTransactionId).runFlowTest {
                // Then
                val result = awaitItem()
                assertTrue(result is TransactionDetailsInteractorPartialState.Success)
                result as TransactionDetailsInteractorPartialState.Success

                val card = result.transactionDetailsUi.transactionDetailsCardUi
                assertEquals(mockedPresentationLabel, card.transactionTypeLabel)
                assertEquals(mockedRelyingPartyName, card.relyingPartyName)
                assertEquals(true, card.relyingPartyIsVerified)
                assertEquals(
                    emptyList<Any>(),
                    result.transactionDetailsUi.transactionDetailsDataShared.dataSharedItems
                )
            }
        }
    }

    // Case 4:
    // 1. walletCoreDocumentsController.getTransactionLog returns a PresentationLog with
    //    two PresentedDocuments to exercise both format branches (MsoMdoc and SdJwtVc) and both
    //    presentedClaim.value branches (non-null and null).

    // Case 4 Expected Result:
    // Success with dataSharedItems containing one NestedListItem per presented document.
    @Test
    fun `Given Case 4, When getTransactionDetails is called, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            // Two claims for this doc so the sortRecursivelyBy selector is invoked at the
            // current level (sortedBy needs >=2 elements to call the comparator).
            // The second PresentedClaim is a Mockito mock whose value getter returns null,
            // exercising the `value?.let { }` false branch (PresentedClaim.value is the
            // platform-typed Any so source-level null cannot be passed via the constructor).
            val nullValueClaim = mock<PresentedClaim>().also {
                whenever(it.path).thenReturn(listOf(mockedMdocNamespace, "given_name"))
                whenever(it.value).thenReturn(null)
            }
            val msoMdocDocument = PresentedDocument(
                format = mockedMdocPidFormat,
                metadata = null,
                claims = listOf(
                    PresentedClaim(
                        path = listOf(mockedMdocNamespace, "family_name"),
                        value = "ANDERSSON",
                        rawValue = "ANDERSSON",
                        metadata = null,
                    ),
                    PresentedClaim(
                        path = listOf(mockedMdocNamespace, "given_name"),
                        value = "JAN",
                        rawValue = "JAN",
                        metadata = null,
                    ),
                    nullValueClaim,
                ),
            )
            val sdJwtDocument = PresentedDocument(
                format = mockedSdJwtPidFormat,
                metadata = null,
                claims = listOf(
                    PresentedClaim(
                        path = listOf("family_name"),
                        value = "ANDERSSON",
                        rawValue = "ANDERSSON",
                        metadata = null,
                    ),
                ),
            )
            // Third doc with non-null metadata that resolves to a display name, exercising
            // the non-null branches of `metadata?.display?.firstOrNull()?.name`.
            val documentWithDisplayName = PresentedDocument(
                format = mockedMdocPidFormat,
                metadata = IssuerMetadata(
                    documentConfigurationIdentifier = "config",
                    display = listOf(
                        IssuerMetadata.Display(
                            name = mockedDisplayName,
                            locale = Locale.ENGLISH,
                            logo = null,
                            description = null,
                            backgroundColor = null,
                            textColor = null,
                            backgroundImageUri = null,
                        )
                    ),
                    claims = emptyList(),
                    credentialIssuerIdentifier = "issuer",
                    issuerDisplay = emptyList(),
                ),
                claims = listOf(
                    PresentedClaim(
                        path = listOf(mockedMdocNamespace, "family_name"),
                        value = "ANDERSSON",
                        rawValue = "ANDERSSON",
                        metadata = null,
                    ),
                ),
            )
            // Fourth doc with non-null metadata but an empty display list, exercising the
            // `?.firstOrNull()` null branch of `metadata?.display?.firstOrNull()?.name`.
            val documentWithEmptyDisplay = PresentedDocument(
                format = mockedMdocPidFormat,
                metadata = IssuerMetadata(
                    documentConfigurationIdentifier = "config",
                    display = emptyList(),
                    claims = emptyList(),
                    credentialIssuerIdentifier = "issuer",
                    issuerDisplay = emptyList(),
                ),
                claims = listOf(
                    PresentedClaim(
                        path = listOf(mockedMdocNamespace, "family_name"),
                        value = "ANDERSSON",
                        rawValue = "ANDERSSON",
                        metadata = null,
                    ),
                ),
            )
            // Note: IssuerMetadata.Display.name is non-nullable in the SDK's Kotlin source,
            // so the `?.name` safe-call's null branch is defensive code that cannot be
            // reached from the construction site — Kover may still flag line 400 as partial.
            val relyingParty = TransactionLog.RelyingParty(
                name = mockedRelyingPartyName,
                isVerified = false,
                certificateChain = emptyList(),
                readerAuth = null,
            )
            val transaction = TransactionLogDataDomain.PresentationLog(
                id = mockedTransactionId,
                name = mockedTransactionName,
                status = TransactionLog.Status.Completed,
                creationLocalDateTime = mockedCreationLocalDateTime,
                creationLocalDate = mockedCreationLocalDate,
                relyingParty = relyingParty,
                documents = listOf(
                    msoMdocDocument,
                    sdJwtDocument,
                    documentWithDisplayName,
                    documentWithEmptyDisplay,
                ),
            )
            whenever(walletCoreDocumentsController.getTransactionLog(id = mockedTransactionId))
                .thenReturn(transaction)
            mockTransactionDetailsStrings()
            mockTransformToUiItemsStrings(resourceProvider)
            whenever(uuidProvider.provideUuid()).thenReturn(mockedUuid)

            // When
            interactor.getTransactionDetails(transactionId = mockedTransactionId).runFlowTest {
                // Then
                val result = awaitItem()
                assertTrue(result is TransactionDetailsInteractorPartialState.Success)
                result as TransactionDetailsInteractorPartialState.Success

                val sharedItems =
                    result.transactionDetailsUi.transactionDetailsDataShared.dataSharedItems
                assertEquals(4, sharedItems.size)
                assertEquals(
                    mockedRelyingPartyName,
                    result.transactionDetailsUi.transactionDetailsCardUi.relyingPartyName
                )
                assertEquals(
                    false,
                    result.transactionDetailsUi.transactionDetailsCardUi.relyingPartyIsVerified
                )
            }
        }
    }

    // Case 5:
    // 1. walletCoreDocumentsController.getTransactionLog returns a SigningLog
    //    (a transaction type that does not include relyingParty or dataShared yet).

    // Case 5 Expected Result:
    // Success with relyingPartyName and relyingPartyIsVerified null, and dataSharedItems empty.
    @Test
    fun `Given Case 5, When getTransactionDetails is called, Then Case 5 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val transaction = TransactionLogDataDomain.SigningLog(
                id = mockedTransactionId,
                name = mockedTransactionName,
                status = TransactionLog.Status.Completed,
                creationLocalDateTime = mockedCreationLocalDateTime,
                creationLocalDate = mockedCreationLocalDate,
            )
            whenever(walletCoreDocumentsController.getTransactionLog(id = mockedTransactionId))
                .thenReturn(transaction)
            mockTransactionDetailsStrings()

            // When
            interactor.getTransactionDetails(transactionId = mockedTransactionId).runFlowTest {
                // Then
                val result = awaitItem()
                assertTrue(result is TransactionDetailsInteractorPartialState.Success)
                result as TransactionDetailsInteractorPartialState.Success

                val card = result.transactionDetailsUi.transactionDetailsCardUi
                assertEquals(mockedSigningLabel, card.transactionTypeLabel)
                assertNull(card.relyingPartyName)
                assertNull(card.relyingPartyIsVerified)
                assertEquals(
                    emptyList<Any>(),
                    result.transactionDetailsUi.transactionDetailsDataShared.dataSharedItems
                )
            }
        }
    }

    // Case 6:
    // 1. walletCoreDocumentsController.getTransactionLog returns null (no transaction for that id).

    // Case 6 Expected Result:
    // Failure with the generic error message.
    @Test
    fun `Given Case 6, When getTransactionDetails is called, Then Case 6 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getTransactionLog(id = mockedTransactionId))
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
    // 1. walletCoreDocumentsController.getTransactionLog throws an exception with a message.

    // Case 7 Expected Result:
    // Failure with the thrown exception's localized message.
    @Test
    fun `Given Case 7, When getTransactionDetails is called, Then Case 7 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getTransactionLog(id = mockedTransactionId))
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
    // 1. walletCoreDocumentsController.getTransactionLog throws an exception with no message.

    // Case 8 Expected Result:
    // Failure with the generic error message.
    @Test
    fun `Given Case 8, When getTransactionDetails is called, Then Case 8 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getTransactionLog(id = mockedTransactionId))
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
    //endregion

    //region requestDataDeletion
    @Test
    fun `When requestDataDeletion is called, Then Success state is returned`() {
        coroutineRule.runTest {
            // When
            interactor.requestDataDeletion(transactionId = mockedTransactionId).runFlowTest {
                // Then
                assertEquals(
                    TransactionDetailsInteractorRequestDataDeletionPartialState.Success,
                    awaitItem()
                )
            }
        }
    }
    //endregion

    //region reportSuspiciousTransaction
    @Test
    fun `When reportSuspiciousTransaction is called, Then Success state is returned`() {
        coroutineRule.runTest {
            // When
            interactor.reportSuspiciousTransaction(transactionId = mockedTransactionId)
                .runFlowTest {
                    // Then
                    assertEquals(
                        TransactionDetailsInteractorReportSuspiciousTransactionPartialState.Success,
                        awaitItem()
                    )
                }
        }
    }
    //endregion

    //region sealed Failure arms (data-class API surface)
    // The interactor only emits Success today, but the Failure arms are part of the sealed
    // API surface so the data-class equals/hashCode/copy/toString need exercise.
    @Test
    fun `Given requestDataDeletion Failure data class, When instantiated, Then equals_hashCode_copy work`() {
        val a =
            TransactionDetailsInteractorRequestDataDeletionPartialState.Failure(errorMessage = "err")
        val b = a.copy(errorMessage = "err")
        val c =
            TransactionDetailsInteractorRequestDataDeletionPartialState.Failure(errorMessage = "other")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertEquals("err", a.errorMessage)
        kotlin.test.assertNotEquals(a, c)
    }

    @Test
    fun `Given reportSuspiciousTransaction Failure data class, When instantiated, Then equals_hashCode_copy work`() {
        val a =
            TransactionDetailsInteractorReportSuspiciousTransactionPartialState.Failure(errorMessage = "err")
        val b = a.copy(errorMessage = "err")
        val c =
            TransactionDetailsInteractorReportSuspiciousTransactionPartialState.Failure(errorMessage = "other")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertEquals("err", a.errorMessage)
        kotlin.test.assertNotEquals(a, c)
    }
    //endregion

    //region helper functions
    private fun mockTransactionDetailsStrings() {
        whenever(resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_issuance))
            .thenReturn(mockedIssuanceLabel)
        whenever(resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_presentation))
            .thenReturn(mockedPresentationLabel)
        whenever(resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_signing))
            .thenReturn(mockedSigningLabel)
        whenever(resourceProvider.getString(R.string.transactions_filter_item_status_completed))
            .thenReturn(mockedCompletedLabel)
        whenever(resourceProvider.getString(R.string.transactions_filter_item_status_failed))
            .thenReturn(mockedFailedLabel)
        whenever(resourceProvider.getString(R.string.transaction_details_collapsed_supporting_text))
            .thenReturn(mockedTransactionDetailsSupportingText)
    }
    //endregion

    //region mocked objects
    private val mockedTransactionId = "mockedTransactionId"
    private val mockedTransactionName = "Mocked Transaction"
    private val mockedRelyingPartyName = "Mocked Relying Party"
    private val mockedMdocNamespace = "eu.europa.ec.eudi.pid.1"
    private val mockedCreationLocalDateTime: LocalDateTime =
        LocalDateTime.of(2026, 3, 15, 14, 30, 0)
    private val mockedCreationLocalDate: LocalDate = mockedCreationLocalDateTime.toLocalDate()
    private val mockedIssuanceLabel = "Issuance"
    private val mockedPresentationLabel = "Presentation"
    private val mockedSigningLabel = "Signing"
    private val mockedCompletedLabel = "Completed"
    private val mockedFailedLabel = "Failed"
    private val mockedTransactionDetailsSupportingText = "Transaction details supporting text"
    private val mockedDisplayName = "Mocked Display Name"
    //endregion
}
