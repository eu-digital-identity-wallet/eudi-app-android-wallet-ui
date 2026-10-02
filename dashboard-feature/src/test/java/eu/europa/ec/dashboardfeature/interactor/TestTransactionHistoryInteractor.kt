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

import eu.europa.ec.corelogic.controller.WalletCoreTransactionLogController
import eu.europa.ec.corelogic.model.CommunicationMethodDomain
import eu.europa.ec.corelogic.model.DpaContactDomain
import eu.europa.ec.corelogic.model.LocalizedTextDomain
import eu.europa.ec.corelogic.model.TransactionLogDomain
import eu.europa.ec.corelogic.model.TransactionResultDomain
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDataProtectionAction
import eu.europa.ec.dashboardfeature.util.mockedDataDeletionLogDomain
import eu.europa.ec.dashboardfeature.util.mockedDetailedPresentationLogDomain
import eu.europa.ec.dashboardfeature.util.mockedDpaReportLogDomain
import eu.europa.ec.dashboardfeature.util.mockedTransactionLanguageTag
import eu.europa.ec.dashboardfeature.util.mockedTransactionLogDomains
import eu.europa.ec.dashboardfeature.util.mockedTransactionRegistration
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.mockedExceptionWithMessage
import eu.europa.ec.testfeature.util.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.util.mockedGenericErrorMessage
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemSupportingContentDataUi
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
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

class TestTransactionHistoryInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var walletCoreTransactionLogController: WalletCoreTransactionLogController

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var interactor: TransactionHistoryInteractor
    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        interactor = TransactionHistoryInteractorImpl(
            walletCoreTransactionLogController = walletCoreTransactionLogController,
            resourceProvider = resourceProvider,
        )
        whenever(resourceProvider.genericErrorMessage()).thenReturn(mockedGenericErrorMessage)
        whenever(resourceProvider.getString(any())).thenAnswer { invocation ->
            mockedStrings.getValue(invocation.getArgument(0))
        }
        whenever(resourceProvider.getString(any(), any<String>())).thenAnswer { invocation ->
            String.format(
                Locale.ROOT,
                mockedStrings.getValue(invocation.getArgument(0)),
                invocation.getArgument<String>(1),
            )
        }
    }

    @After
    fun after() {
        closeable.close()
    }

    //region observeHistory

    // Case 1:
    // 1. Mixed actions include DDRs for two presentations with the same relying party.
    // 2. The selected presentation has no contacts or disclosed claims and did not complete.
    //
    // Case 1 Expected Result:
    // Only its DDRs appear, with the stored methods and attempt dates; history performs no writes.
    @Test
    fun `Given Case 1, When observeHistory is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val attempts = listOf(
                mockedDdr.copy(
                    id = "website",
                    communicationMethod = CommunicationMethodDomain.Website
                ),
                mockedDpar,
                mockedDdr.copy(id = "email", communicationMethod = CommunicationMethodDomain.Email),
                mockedDdr.copy(id = "other-parent", parentPresentationId = "another-presentation"),
                mockedDdr.copy(id = "phone", communicationMethod = CommunicationMethodDomain.Phone),
            )
            mockHistoryCall(flowOf(attempts), mockedParent)

            // When
            interactor.observeHistory(
                mockedParent.id,
                TransactionDataProtectionAction.RequestDataDeletion
            ).runFlowTest {
                // Then
                assertEquals(TransactionHistoryInteractorPartialState.Loading, awaitItem())
                val history =
                    (awaitItem() as TransactionHistoryInteractorPartialState.Success).history
                assertEquals(
                    "Previous data deletion requests for ${mockedParent.party.name?.text}",
                    history.title
                )
                assertEquals("Deletion disclaimer", history.disclaimer)
                assertEquals("Deletion introduction", history.introduction)
                assertEquals(
                    listOf("website", "email", "phone"),
                    history.items.map { item -> item.itemId })
                assertEquals(
                    listOf("Web form", "Email", "Phone call"),
                    history.items.map { item -> item.methodLabel() })
                assertEquals(
                    listOf(mockedDate, mockedDate, mockedDate),
                    history.items.map { item -> item.attemptDate() })
                assertTrue(history.items.all { item ->
                    item.overlineText == null && item.leadingContentData == null && item.trailingContentData == null
                })
                assertNull(history.authority)
                awaitComplete()
            }
            verify(walletCoreTransactionLogController).observePresentationActions(mockedParent.id)
            verify(walletCoreTransactionLogController).getTransactionLog(mockedParent.id)
            verifyNoMoreInteractions(walletCoreTransactionLogController)
        }
    }

    // Case 2:
    // 1. One parent presentation has a DPA and report attempts using all supported methods.
    // 2. Deletion requests and another parent's report are also emitted.
    //
    // Case 2 Expected Result:
    // One card uses the parent's DPA name; only this parent's report method/date rows appear.
    @Test
    fun `Given Case 2, When observeHistory is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val parent = mockedParent.copy(
                registration = mockedTransactionRegistration.copy(
                    dpa = DpaContactDomain(
                        mockedDpar.dpaName,
                        mockedDpar.dpaCountry,
                        emptyList(),
                    ),
                ),
            )
            val attempts = listOf(
                mockedDpar.copy(
                    id = "website", communicationMethod = CommunicationMethodDomain.Website,
                ),
                mockedDdr,
                mockedDpar.copy(id = "other-parent", parentPresentationId = "another-presentation"),
                mockedDpar.copy(
                    id = "email", communicationMethod = CommunicationMethodDomain.Email,
                ),
                mockedDpar.copy(
                    id = "phone", communicationMethod = CommunicationMethodDomain.Phone,
                ),
            )
            mockHistoryCall(flowOf(attempts), parent)

            // When
            interactor.observeHistory(
                parent.id,
                TransactionDataProtectionAction.ReportSuspiciousTransaction
            ).runFlowTest {
                // Then
                assertEquals(TransactionHistoryInteractorPartialState.Loading, awaitItem())
                val history =
                    (awaitItem() as TransactionHistoryInteractorPartialState.Success).history
                assertEquals(
                    "Previous transaction reports for ${parent.party.name?.text}",
                    history.title
                )
                assertEquals("Report disclaimer", history.disclaimer)
                assertEquals("Report introduction", history.introduction)
                assertEquals(
                    listOf("website", "email", "phone"),
                    history.items.map { item -> item.itemId })
                assertEquals(
                    listOf("Website", "Email", "Phone call"),
                    history.items.map { item -> item.methodLabel() })
                assertTrue(history.items.all { item -> item.overlineText == null })
                assertEquals(parent.registration?.dpa?.name?.text, history.authority)
                assertEquals(
                    listOf(mockedDate, mockedDate, mockedDate),
                    history.items.map { item -> item.attemptDate() })
                awaitComplete()
            }
            verify(walletCoreTransactionLogController).observePresentationActions(parent.id)
            verify(walletCoreTransactionLogController).getTransactionLog(parent.id)
            verifyNoMoreInteractions(walletCoreTransactionLogController)
        }
    }

    // Case 3:
    // 1. Successive snapshots add/remove attempts and leave only the other action kind.
    //
    // Case 3 Expected Result:
    // Each snapshot replaces the rows; no matching attempts is an empty success, not a failure.
    @Test
    fun `Given Case 3, When observeHistory is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockHistoryCall(
                attempts = flowOf(
                    emptyList(),
                    listOf(mockedDdr),
                    listOf(mockedDdr.copy(id = "replacement"), mockedDpar),
                    listOf(mockedDpar),
                ),
                parent = mockedParent,
            )

            // When
            interactor.observeHistory(
                mockedParent.id,
                TransactionDataProtectionAction.RequestDataDeletion
            ).runFlowTest {
                // Then
                assertEquals(TransactionHistoryInteractorPartialState.Loading, awaitItem())
                listOf(
                    emptyList(),
                    listOf(mockedDdr.id),
                    listOf("replacement"),
                    emptyList()
                ).forEach { expectedIds ->
                    val history =
                        (awaitItem() as TransactionHistoryInteractorPartialState.Success).history
                    assertEquals(expectedIds, history.items.map { item -> item.itemId })
                    assertEquals(
                        if (expectedIds.isEmpty()) "No deletion attempts" else "Deletion introduction",
                        history.introduction,
                    )
                    assertNull(history.authority)
                }
                awaitComplete()
            }
        }
    }

    // Case 4:
    // 1. RP identity contains Unicode, lacks a name, or lacks both name and identifier.
    //
    // Case 4 Expected Result:
    // Both history titles keep the recorded name as plain text, or use the neutral label.
    @Test
    fun `Given Case 4, When observeHistory is called, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val parties = listOf(
                mockedParent.party.copy(
                    name = LocalizedTextDomain(
                        "el",
                        "Ταξίδι & <Travel>"
                    )
                ) to "Ταξίδι & <Travel>",
                mockedParent.party.copy(
                    name = LocalizedTextDomain(
                        mockedTransactionLanguageTag,
                        " "
                    )
                ) to "Relying party",
                mockedParent.party.copy(name = null) to "Relying party",
                mockedParent.party.copy(name = null, identifier = null) to "Relying party",
            )
            parties.forEach { (party, expectedName) ->
                mockHistoryCall(flowOf(emptyList()), mockedParent.copy(party = party))

                listOf(
                    TransactionDataProtectionAction.RequestDataDeletion to "Previous data deletion requests for $expectedName",
                    TransactionDataProtectionAction.ReportSuspiciousTransaction to "Previous transaction reports for $expectedName",
                ).forEach { (action, expectedTitle) ->
                    // When
                    interactor.observeHistory(mockedParent.id, action).runFlowTest {
                        // Then
                        assertEquals(TransactionHistoryInteractorPartialState.Loading, awaitItem())
                        val history =
                            (awaitItem() as TransactionHistoryInteractorPartialState.Success).history
                        assertEquals(expectedTitle, history.title)
                        assertTrue(history.items.isEmpty())
                        assertNull(history.authority)
                        awaitComplete()
                    }
                }
            }
        }
    }

    // Case 5:
    // 1. The observer orders attempts by Instant across the clock going back.
    // 2. A newer attempt has an earlier local display time.
    //
    // Case 5 Expected Result:
    // Mapping preserves the recorded order instead of sorting local dates or formatted strings.
    @Test
    fun `Given Case 5, When observeHistory is called, Then Case 5 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val zone = ZoneId.of("Europe/Athens")
            val newer = mockedDdr.copy(
                id = "newer",
                time = Instant.parse("2026-10-25T01:15:00Z").atZone(zone).toLocalDateTime(),
            )
            val older = mockedDdr.copy(
                id = "older",
                time = Instant.parse("2026-10-25T00:45:00Z").atZone(zone).toLocalDateTime(),
            )
            mockHistoryCall(flowOf(listOf(newer, mockedDpar, older)), mockedParent)

            // When
            interactor.observeHistory(
                mockedParent.id,
                TransactionDataProtectionAction.RequestDataDeletion
            ).runFlowTest {
                // Then
                assertEquals(TransactionHistoryInteractorPartialState.Loading, awaitItem())
                val history =
                    (awaitItem() as TransactionHistoryInteractorPartialState.Success).history
                assertEquals(listOf("newer", "older"), history.items.map { item -> item.itemId })
                assertEquals(
                    listOf("25 October 2026 - 03:15", "25 October 2026 - 03:45"),
                    history.items.map { item -> item.attemptDate() },
                )
                awaitComplete()
            }
        }
    }

    // Case 6:
    // 1. The resource provider selects a non-default locale.
    //
    // Case 6 Expected Result:
    // Attempt dates use the project-default locale and the 24-hour history format.
    @Test
    fun `Given Case 6, When observeHistory is called, Then Case 6 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(resourceProvider.getLocale()).thenReturn(Locale.FRANCE)
            mockHistoryCall(flowOf(listOf(mockedDpar)), mockedParent)

            // When
            interactor.observeHistory(
                mockedParent.id,
                TransactionDataProtectionAction.ReportSuspiciousTransaction
            ).runFlowTest {
                // Then
                assertEquals(TransactionHistoryInteractorPartialState.Loading, awaitItem())
                val history =
                    (awaitItem() as TransactionHistoryInteractorPartialState.Success).history
                assertEquals(mockedDate, history.items.single().attemptDate())
                awaitComplete()
            }
        }
    }

    // Case 7:
    // 1. The route's parent is missing or is not a presentation.
    //
    // Case 7 Expected Result:
    // ParentNotFound is distinct from an empty history for either action kind.
    @Test
    fun `Given Case 7, When observeHistory is called, Then Case 7 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val parents = listOf(null) +
                    mockedTransactionLogDomains.filterNot { transaction -> transaction is TransactionLogDomain.Presentation }
            parents.forEach { parent ->
                mockHistoryCall(flowOf(emptyList()), parent)
                TransactionDataProtectionAction.entries.forEach { action ->
                    // When
                    interactor.observeHistory(mockedParent.id, action).runFlowTest {
                        // Then
                        assertEquals(TransactionHistoryInteractorPartialState.Loading, awaitItem())
                        assertEquals(
                            TransactionHistoryInteractorPartialState.ParentNotFound,
                            awaitItem()
                        )
                        awaitComplete()
                    }
                }
            }
        }
    }

    // Case 8:
    // 1. The parent is deleted after the first populated snapshot.
    //
    // Case 8 Expected Result:
    // The next emission reports ParentNotFound so the screen can leave the deleted presentation.
    @Test
    fun `Given Case 8, When observeHistory is called, Then Case 8 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockHistoryCall(flowOf(listOf(mockedDpar), emptyList()), mockedParent)
            whenever(walletCoreTransactionLogController.getTransactionLog(mockedParent.id))
                .thenReturn(mockedParent, null)

            // When
            interactor.observeHistory(
                mockedParent.id,
                TransactionDataProtectionAction.ReportSuspiciousTransaction
            ).runFlowTest {
                // Then
                assertEquals(TransactionHistoryInteractorPartialState.Loading, awaitItem())
                assertTrue(awaitItem() is TransactionHistoryInteractorPartialState.Success)
                assertEquals(TransactionHistoryInteractorPartialState.ParentNotFound, awaitItem())
                awaitComplete()
            }
        }
    }

    // Case 9:
    // 1. Starting the observation throws, with or without an error message.
    //
    // Case 9 Expected Result:
    // Loading is followed by a failure state, never an empty success.
    @Test
    fun `Given Case 9, When observeHistory is called, Then Case 9 Expected Result is returned`() {
        coroutineRule.runTest {
            listOf(mockedExceptionWithMessage, mockedExceptionWithNoMessage).forEach { exception ->
                // Given
                doThrow(exception).whenever(walletCoreTransactionLogController)
                    .observePresentationActions(mockedParent.id)

                // When
                interactor.observeHistory(
                    mockedParent.id,
                    TransactionDataProtectionAction.RequestDataDeletion
                ).runFlowTest {
                    // Then
                    assertEquals(TransactionHistoryInteractorPartialState.Loading, awaitItem())
                    assertEquals(
                        TransactionHistoryInteractorPartialState.Failure(
                            exception.localizedMessage ?: mockedGenericErrorMessage
                        ),
                        awaitItem(),
                    )
                    awaitComplete()
                }
            }
        }
    }

    // Case 10:
    // 1. Reading the parent throws, with or without an error message.
    //
    // Case 10 Expected Result:
    // The interactor contains the exception and exposes a failure, not ParentNotFound.
    @Test
    fun `Given Case 10, When observeHistory is called, Then Case 10 Expected Result is returned`() {
        coroutineRule.runTest {
            listOf(mockedExceptionWithMessage, mockedExceptionWithNoMessage).forEach { exception ->
                // Given
                mockHistoryCall(flowOf(emptyList()), mockedParent)
                doThrow(exception).whenever(walletCoreTransactionLogController)
                    .getTransactionLog(mockedParent.id)

                // When
                interactor.observeHistory(
                    mockedParent.id,
                    TransactionDataProtectionAction.RequestDataDeletion
                ).runFlowTest {
                    // Then
                    assertEquals(TransactionHistoryInteractorPartialState.Loading, awaitItem())
                    assertEquals(
                        TransactionHistoryInteractorPartialState.Failure(
                            exception.localizedMessage ?: mockedGenericErrorMessage
                        ),
                        awaitItem(),
                    )
                    awaitComplete()
                }
            }
        }
    }

    // Case 11:
    // 1. Resolving a localized method label throws during projection.
    //
    // Case 11 Expected Result:
    // Mapping errors remain inside the interactor's failure boundary.
    @Test
    fun `Given Case 11, When observeHistory is called, Then Case 11 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockHistoryCall(flowOf(listOf(mockedDdr)), mockedParent)
            doThrow(mockedExceptionWithMessage).whenever(resourceProvider)
                .getString(R.string.privacy_history_method_email)

            // When
            interactor.observeHistory(
                mockedParent.id,
                TransactionDataProtectionAction.RequestDataDeletion
            ).runFlowTest {
                // Then
                assertEquals(TransactionHistoryInteractorPartialState.Loading, awaitItem())
                assertEquals(
                    TransactionHistoryInteractorPartialState.Failure(
                        mockedExceptionWithMessage.localizedMessage ?: mockedGenericErrorMessage,
                    ),
                    awaitItem(),
                )
                awaitComplete()
            }
        }
    }

    // Case 12:
    // 1. An observation fails after showing content; the caller then retries.
    //
    // Case 12 Expected Result:
    // Failure stays distinct from empty history; a fresh subscription replaces the prior rows.
    @Test
    fun `Given Case 12, When observeHistory is retried, Then Case 12 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockHistoryCall(
                attempts = flow {
                    emit(listOf(mockedDpar))
                    throw mockedExceptionWithMessage
                },
                parent = mockedParent,
            )

            // When
            interactor.observeHistory(
                mockedParent.id,
                TransactionDataProtectionAction.ReportSuspiciousTransaction
            ).runFlowTest {
                // Then
                assertEquals(TransactionHistoryInteractorPartialState.Loading, awaitItem())
                assertTrue(awaitItem() is TransactionHistoryInteractorPartialState.Success)
                assertEquals(
                    TransactionHistoryInteractorPartialState.Failure(
                        mockedExceptionWithMessage.localizedMessage ?: mockedGenericErrorMessage,
                    ),
                    awaitItem(),
                )
                awaitComplete()
            }

            // Given
            mockHistoryCall(flowOf(emptyList()), mockedParent)

            // When
            interactor.observeHistory(
                mockedParent.id,
                TransactionDataProtectionAction.ReportSuspiciousTransaction
            ).runFlowTest {
                // Then
                assertEquals(TransactionHistoryInteractorPartialState.Loading, awaitItem())
                val history =
                    (awaitItem() as TransactionHistoryInteractorPartialState.Success).history
                assertTrue(history.items.isEmpty())
                assertEquals("No report attempts", history.introduction)
                assertNull(history.authority)
                awaitComplete()
            }
        }
    }

    // Case 13:
    // 1. The consumer cancels an active observation after receiving content.
    //
    // Case 13 Expected Result:
    // The upstream observation stops without converting cancellation into an error state.
    @Test
    fun `Given Case 13, When observeHistory is cancelled, Then Case 13 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val observationStopped = CompletableDeferred<Unit>()
            mockHistoryCall(
                attempts = flow {
                    try {
                        emit(listOf(mockedDdr))
                        awaitCancellation()
                    } finally {
                        observationStopped.complete(Unit)
                    }
                },
                parent = mockedParent,
            )

            // When
            interactor.observeHistory(
                mockedParent.id,
                TransactionDataProtectionAction.RequestDataDeletion
            ).runFlowTest {
                // Then
                assertEquals(TransactionHistoryInteractorPartialState.Loading, awaitItem())
                assertTrue(awaitItem() is TransactionHistoryInteractorPartialState.Success)
                cancelAndIgnoreRemainingEvents()
            }
            observationStopped.await()
            verify(resourceProvider, never()).genericErrorMessage()
        }
    }

    // Case 14:
    // 1. The parent presentation has a named DPA and a country.
    // 2. A later snapshot removes every attempt.
    //
    // Case 14 Expected Result:
    // The card shows only the DPA name; an empty history keeps no stale authority card.
    @Test
    fun `Given Case 14, When observeHistory is called, Then Case 14 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            mockHistoryCall(
                attempts = flowOf(
                    listOf(mockedDpar, mockedDpar.copy(id = "second")),
                    listOf(mockedDpar),
                    emptyList(),
                ),
                parent = mockedParent.copy(registration = mockedTransactionRegistration),
            )

            // When
            interactor.observeHistory(
                mockedParent.id,
                TransactionDataProtectionAction.ReportSuspiciousTransaction
            ).runFlowTest {
                // Then
                assertEquals(TransactionHistoryInteractorPartialState.Loading, awaitItem())
                listOf(
                    2 to mockedTransactionRegistration.dpa?.name?.text,
                    1 to mockedTransactionRegistration.dpa?.name?.text,
                    0 to null,
                ).forEach { (expectedCount, expectedAuthority) ->
                    val history =
                        (awaitItem() as TransactionHistoryInteractorPartialState.Success).history
                    assertEquals(expectedAuthority, history.authority)
                    assertEquals(expectedCount, history.items.size)
                    assertTrue(history.items.all { item -> item.overlineText == null })
                    assertEquals(
                        if (expectedCount == 0) "No report attempts" else "Report introduction",
                        history.introduction,
                    )
                }
                awaitComplete()
            }
        }
    }

    // Case 15:
    // 1. The parent has no DPA, or its DPA has a country but a null/blank name.
    //
    // Case 15 Expected Result:
    // No authority card or overlines appear; recorded method/date rows remain available.
    @Test
    fun `Given Case 15, When observeHistory is called, Then Case 15 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val unnamedAuthority = DpaContactDomain(
                name = null,
                country = LocalizedTextDomain(mockedTransactionLanguageTag, "BE"),
                contacts = emptyList(),
            )
            listOf(
                null,
                unnamedAuthority,
                unnamedAuthority.copy(
                    name = LocalizedTextDomain(
                        mockedTransactionLanguageTag,
                        " "
                    )
                ),
            ).forEach { authority ->
                val attempt = mockedDpar.copy(
                    dpaName = authority?.name,
                    dpaCountry = authority?.country,
                )
                mockHistoryCall(
                    attempts = flowOf(listOf(attempt)),
                    parent = mockedParent.copy(
                        registration = mockedTransactionRegistration.copy(dpa = authority),
                    ),
                )

                // When
                interactor.observeHistory(
                    mockedParent.id,
                    TransactionDataProtectionAction.ReportSuspiciousTransaction
                ).runFlowTest {
                    // Then
                    assertEquals(TransactionHistoryInteractorPartialState.Loading, awaitItem())
                    val history =
                        (awaitItem() as TransactionHistoryInteractorPartialState.Success).history
                    assertNull(history.authority)
                    assertNull(history.items.single().overlineText)
                    assertEquals(attempt.id, history.items.single().itemId)
                    assertEquals(mockedDate, history.items.single().attemptDate())
                    awaitComplete()
                }
            }
        }
    }
    //endregion

    //region helper functions
    private suspend fun mockHistoryCall(
        attempts: Flow<List<TransactionLogDomain.PresentationAction>>,
        parent: TransactionLogDomain?,
    ) {
        doReturn(attempts).whenever(walletCoreTransactionLogController)
            .observePresentationActions(mockedParent.id)
        doReturn(parent).whenever(walletCoreTransactionLogController)
            .getTransactionLog(mockedParent.id)
    }

    private fun ListItemDataUi.methodLabel(): String =
        (mainContentData as ListItemMainContentDataUi.Text).text

    private fun ListItemDataUi.attemptDate(): String =
        (supportingContentData as ListItemSupportingContentDataUi.Text).text
    //endregion

    //region mocked objects
    private val mockedParent = mockedDetailedPresentationLogDomain.copy(
        result = TransactionResultDomain.NotCompleted("Interrupted"),
        party = mockedDetailedPresentationLogDomain.party.copy(contacts = emptyList()),
        registration = null,
        claimsPresented = emptyList(),
    )
    private val mockedDdr = mockedDataDeletionLogDomain.copy(
        time = LocalDateTime.of(2026, 1, 6, 9, 15),
    )
    private val mockedDpar = mockedDpaReportLogDomain.copy(
        time = LocalDateTime.of(2026, 1, 6, 9, 15),
    )
    private val mockedDate = "06 January 2026 - 09:15"
    private val mockedStrings = mapOf(
        R.string.privacy_history_method_web_form to "Web form",
        R.string.privacy_history_method_website to "Website",
        R.string.privacy_history_method_email to "Email",
        R.string.privacy_history_method_phone to "Phone call",
        R.string.privacy_history_relying_party_default_name to "Relying party",
        R.string.privacy_history_deletion_title to "Previous data deletion requests for %1\$s",
        R.string.privacy_history_report_title to "Previous transaction reports for %1\$s",
        R.string.privacy_history_deletion_disclaimer to "Deletion disclaimer",
        R.string.privacy_history_report_disclaimer to "Report disclaimer",
        R.string.privacy_history_deletion_intro to "Deletion introduction",
        R.string.privacy_history_report_intro to "Report introduction",
        R.string.privacy_history_deletion_empty to "No deletion attempts",
        R.string.privacy_history_report_empty to "No report attempts",
    )
    //endregion
}