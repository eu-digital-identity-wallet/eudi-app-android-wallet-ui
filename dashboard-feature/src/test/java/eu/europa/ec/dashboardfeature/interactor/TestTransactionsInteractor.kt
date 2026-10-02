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

import eu.europa.ec.businesslogic.extension.filterByQuery
import eu.europa.ec.businesslogic.validator.FilterValidator
import eu.europa.ec.businesslogic.validator.FilterValidatorImpl
import eu.europa.ec.businesslogic.validator.FilterValidatorPartialState
import eu.europa.ec.businesslogic.validator.model.FilterAction
import eu.europa.ec.businesslogic.validator.model.FilterElement
import eu.europa.ec.businesslogic.validator.model.FilterElement.FilterItem
import eu.europa.ec.businesslogic.validator.model.FilterGroup
import eu.europa.ec.businesslogic.validator.model.FilterMultipleAction
import eu.europa.ec.businesslogic.validator.model.FilterableAttributes
import eu.europa.ec.businesslogic.validator.model.FilterableItem
import eu.europa.ec.businesslogic.validator.model.FilterableItemPayload
import eu.europa.ec.businesslogic.validator.model.FilterableList
import eu.europa.ec.businesslogic.validator.model.Filters
import eu.europa.ec.businesslogic.validator.model.SortOrder
import eu.europa.ec.corelogic.controller.WalletCoreTransactionLogController
import eu.europa.ec.corelogic.model.LocalizedTextDomain
import eu.europa.ec.corelogic.model.TransactionLogDomain
import eu.europa.ec.corelogic.model.TransactionResultDomain
import eu.europa.ec.dashboardfeature.ui.transactions.list.model.TransactionCategoryUi
import eu.europa.ec.dashboardfeature.ui.transactions.list.model.TransactionFilterIds
import eu.europa.ec.dashboardfeature.ui.transactions.list.model.TransactionUi
import eu.europa.ec.dashboardfeature.ui.transactions.list.model.TransactionsFilterableAttributes
import eu.europa.ec.dashboardfeature.ui.transactions.model.TransactionStatusUi
import eu.europa.ec.dashboardfeature.ui.transactions.model.TransactionTypeUi
import eu.europa.ec.dashboardfeature.util.mockedCredentialDeletionTypeLabel
import eu.europa.ec.dashboardfeature.util.mockedDataDeletionLogDomain
import eu.europa.ec.dashboardfeature.util.mockedDataDeletionTypeLabel
import eu.europa.ec.dashboardfeature.util.mockedDeletionLogDomain
import eu.europa.ec.dashboardfeature.util.mockedDpaReportLogDomain
import eu.europa.ec.dashboardfeature.util.mockedDpaReportTypeLabel
import eu.europa.ec.dashboardfeature.util.mockedIssuanceLogDomain
import eu.europa.ec.dashboardfeature.util.mockedNotCompletedTransactionLogDomains
import eu.europa.ec.dashboardfeature.util.mockedOtherTransactionCredential
import eu.europa.ec.dashboardfeature.util.mockedPresentationLogDomain
import eu.europa.ec.dashboardfeature.util.mockedPresentedTransactionCredential
import eu.europa.ec.dashboardfeature.util.mockedReissuanceLogDomain
import eu.europa.ec.dashboardfeature.util.mockedRequestedTransactionCredential
import eu.europa.ec.dashboardfeature.util.mockedSearchableTransactionLogDomains
import eu.europa.ec.dashboardfeature.util.mockedSigningLogDomain
import eu.europa.ec.dashboardfeature.util.mockedTransactionCredential
import eu.europa.ec.dashboardfeature.util.mockedTransactionDpaName
import eu.europa.ec.dashboardfeature.util.mockedTransactionIntermediary
import eu.europa.ec.dashboardfeature.util.mockedTransactionIntermediaryName
import eu.europa.ec.dashboardfeature.util.mockedTransactionIssuerName
import eu.europa.ec.dashboardfeature.util.mockedTransactionLanguageTag
import eu.europa.ec.dashboardfeature.util.mockedTransactionLogDomains
import eu.europa.ec.dashboardfeature.util.mockedTransactionPartyName
import eu.europa.ec.dashboardfeature.util.mockedTransactionServiceName
import eu.europa.ec.dashboardfeature.util.mockedUnnamedTransactionLogDomains
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.mockedExceptionWithMessage
import eu.europa.ec.testfeature.util.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.util.mockedGenericErrorMessage
import eu.europa.ec.testfeature.util.mockedPidDocName
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class TestTransactionsInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var filterValidator: FilterValidator

    @Mock
    private lateinit var walletCoreTransactionLogController: WalletCoreTransactionLogController

    private lateinit var interactor: TransactionsInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = TransactionsInteractorImpl(
            resourceProvider = resourceProvider,
            filterValidator = filterValidator,
            walletCoreTransactionLogController = walletCoreTransactionLogController,
        )

        whenever(resourceProvider.genericErrorMessage()).thenReturn(mockedGenericErrorMessage)
        mockGetFiltersStrings()
        mockPartyFilterNameCall()
    }

    @After
    fun after() {
        closeable.close()
    }

    //region revertFilters
    @Test
    fun `When revertFilters is called, Then filterValidator#revertFilters is invoked`() {
        // When
        interactor.revertFilters()

        // Then
        verify(filterValidator, times(1)).revertFilters()
    }
    //endregion

    //region updateLists
    @Test
    fun `When updateLists is called, Then filterValidator#updateLists is invoked with the same list`() {
        // Given
        val list = FilterableList(items = emptyList())

        // When
        interactor.updateLists(filterableList = list)

        // Then
        verify(filterValidator, times(1)).updateLists(list)
    }
    //endregion

    //region applySearch

    // Case 1:
    // 1. The query contains outer whitespace and two spaces between words.
    //
    // Case 1 Expected Result:
    // The query is forwarded unchanged; the shared validator handles normalization.
    @Test
    fun `Given Case 1, When applySearch is called, Then Case 1 Expected Result is returned`() {
        // Given
        val query = "  Example  issuer \t"

        // When
        interactor.applySearch(query = query)

        // Then
        verify(filterValidator, times(1)).applySearch(query)
    }

    // Case 2:
    // 1. The query is empty or contains only whitespace.
    //
    // Case 2 Expected Result:
    // Every query is forwarded unchanged to the shared validator.
    @Test
    fun `Given Case 2, When applySearch is called, Then Case 2 Expected Result is returned`() {
        // Given
        val queries = listOf("", "   ", "\t\n ")

        // When
        queries.forEach { query -> interactor.applySearch(query = query) }

        // Then
        queries.forEach { query -> verify(filterValidator, times(1)).applySearch(query) }
    }

    // Case 3:
    // 1. Named, unnamed and incomplete transactions have different dates.
    // 2. Only completed transactions are selected before searching.
    //
    // Case 3 Expected Result:
    // Padded search matches the named row; clearing it retains the status filter and date order,
    // including the unnamed row without search tags.
    @Test
    fun `Given Case 3, When applySearch is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val named = mockedIssuanceLogDomain.copy(
                time = mockedIssuanceLogDomain.time.plusHours(2)
            )
            val unnamed = mockedIssuanceLogDomain.copy(
                id = "unnamed-issuance",
                time = mockedIssuanceLogDomain.time.plusHours(1),
                details = mockedIssuanceLogDomain.details.copy(
                    issuer = mockedIssuanceLogDomain.details.issuer.copy(name = null)
                ),
            )
            val incomplete = mockedPresentationLogDomain.copy(
                time = mockedIssuanceLogDomain.time.plusHours(3),
                result = TransactionResultDomain.NotCompleted(" "),
            )
            mockGetTransactionLogsCall(response = listOf(unnamed, incomplete, named))
            mockTransactionRowStrings()
            mockGetFiltersStrings()
            mockPartyFilterNameCall()
            val realInteractor = TransactionsInteractorImpl(
                resourceProvider = resourceProvider,
                filterValidator = FilterValidatorImpl(
                    scope = coroutineRule.testScope.backgroundScope,
                    sharingStarted = SharingStarted.Eagerly,
                ),
                walletCoreTransactionLogController = walletCoreTransactionLogController,
            )
            realInteractor.getTransactions().runFlowTest {
                val source =
                    (awaitItem() as TransactionInteractorGetTransactionsPartialState.Success).allTransactions

                // When
                realInteractor.onFilterStateChange().runFlowTest {
                    realInteractor.initializeFilters(source)
                    realInteractor.applyFilters()
                    val initial =
                        awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult
                    realInteractor.updateFilter(
                        filterGroupId = TransactionFilterIds.FILTER_BY_STATUS_GROUP_ID,
                        filterId = TransactionFilterIds.FILTER_BY_STATUS_NOT_COMPLETED,
                    )
                    assertTrue(awaitItem() is TransactionInteractorFilterPartialState.FilterUpdateResult)
                    realInteractor.applySearch("  ${mockedTransactionIssuerName.uppercase()}  ")
                    val matched =
                        awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult

                    // Then
                    assertEquals(
                        listOf(incomplete.id, named.id, unnamed.id),
                        initial.transactionIds()
                    )
                    assertEquals(listOf(named.id), matched.transactionIds())
                    listOf(" \t\n", "").forEach { query ->
                        realInteractor.applySearch(query)
                        val cleared =
                            awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult
                        assertEquals(listOf(named.id, unnamed.id), cleared.transactionIds())
                        assertEquals(false, cleared.allDefaultFiltersAreSelected)
                        assertTrue(cleared.sortOrder is SortOrder.Descending)
                    }
                }
            }
        }
    }

    // Case 4:
    // 1. Presentations have both names or only an intermediary name, with mixed outcomes.
    // 2. A signing has a provider name but no filename; only completed transactions are selected.
    // 3. Completed and incomplete deletions have the same credential type and provider.
    //
    // Case 4 Expected Result:
    // Names or deletion credential types match independently, preserving selected filters and order.
    @Test
    fun `Given recorded party names and active filters, When applySearch is called, Then either name matches within those filters`() {
        coroutineRule.runTest {
            // Given
            val presentation = mockedPresentationLogDomain.copy(
                intermediary = mockedTransactionIntermediary,
            )
            val intermediaryOnly = presentation.copy(
                id = "intermediary-only",
                time = presentation.time.plusHours(1),
                party = presentation.party.copy(name = null),
            )
            val incomplete = intermediaryOnly.copy(
                id = "incomplete-intermediary",
                time = presentation.time.plusHours(2),
                result = TransactionResultDomain.NotCompleted(reason = null),
            )
            val signing = mockedSigningLogDomain.copy(
                time = presentation.time.plusHours(3),
                fileName = null,
            )
            val deletion = mockedDeletionLogDomain.copy(time = presentation.time.plusHours(4))
            val incompleteDeletion = deletion.copy(
                id = "incomplete-deletion",
                time = presentation.time.plusHours(5),
                result = TransactionResultDomain.NotCompleted(reason = null),
            )
            mockGetTransactionLogsCall(
                response = listOf(
                    presentation,
                    intermediaryOnly,
                    incomplete,
                    signing,
                    deletion,
                    incompleteDeletion,
                )
            )
            mockTransactionRowStrings()
            mockGetFiltersStrings()
            mockPartyFilterNameCall()
            val realInteractor = TransactionsInteractorImpl(
                resourceProvider = resourceProvider,
                filterValidator = FilterValidatorImpl(
                    scope = coroutineRule.testScope.backgroundScope,
                    sharingStarted = SharingStarted.Eagerly,
                ),
                walletCoreTransactionLogController = walletCoreTransactionLogController,
            )
            realInteractor.getTransactions().runFlowTest {
                val source =
                    (awaitItem() as TransactionInteractorGetTransactionsPartialState.Success).allTransactions

                // When
                realInteractor.onFilterStateChange().runFlowTest {
                    realInteractor.initializeFilters(source)
                    realInteractor.applyFilters()
                    assertTrue(awaitItem() is TransactionInteractorFilterPartialState.FilterApplyResult)
                    realInteractor.updateFilter(
                        filterGroupId = TransactionFilterIds.FILTER_BY_STATUS_GROUP_ID,
                        filterId = TransactionFilterIds.FILTER_BY_STATUS_NOT_COMPLETED,
                    )
                    assertTrue(awaitItem() is TransactionInteractorFilterPartialState.FilterUpdateResult)
                    val queries = listOf(
                        "  ${
                            mockedTransactionIntermediaryName.substringAfter(' ').uppercase()
                        }  " to listOf(intermediaryOnly.id, presentation.id),
                        mockedTransactionPartyName.uppercase() to listOf(presentation.id),
                        mockedTransactionServiceName.substringAfter(' ').uppercase() to listOf(
                            signing.id
                        ),
                        "  ${deletion.credential.identifier.uppercase()}  " to listOf(deletion.id),
                        mockedTransactionIssuerName.uppercase() to listOf(deletion.id),
                        "" to listOf(deletion.id, signing.id, intermediaryOnly.id, presentation.id),
                    )

                    // Then
                    queries.forEach { (query, expectedIds) ->
                        realInteractor.applySearch(query)
                        val result =
                            awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult
                        assertEquals("Query: $query", expectedIds, result.transactionIds())
                        assertEquals(false, result.allDefaultFiltersAreSelected)
                        assertTrue(result.sortOrder is SortOrder.Descending)
                    }
                }
            }
        }
    }
    //endregion

    //region applyFilters
    @Test
    fun `When applyFilters is called, Then filterValidator#applyFilters is invoked`() {
        // When
        interactor.applyFilters()

        // Then
        verify(filterValidator, times(1)).applyFilters()
    }
    //endregion

    //region updateFilter
    @Test
    fun `When updateFilter is called, Then filterValidator#updateFilter is invoked with the same ids`() {
        // Given
        val groupId = "groupId"
        val filterId = "filterId"

        // When
        interactor.updateFilter(filterGroupId = groupId, filterId = filterId)

        // Then
        verify(filterValidator, times(1)).updateFilter(groupId, filterId)
    }
    //endregion

    //region updateDateFilterById
    @Test
    fun `When updateDateFilterById is called, Then filterValidator#updateDateFilter is invoked with the same arguments`() {
        // Given
        val groupId = "groupId"
        val filterId = "filterId"
        val lower = LocalDateTime.of(2026, 1, 1, 0, 0)
        val upper = LocalDateTime.of(2026, 12, 31, 23, 59)

        // When
        interactor.updateDateFilterById(
            filterGroupId = groupId,
            filterId = filterId,
            lowerLimitDate = lower,
            upperLimitDate = upper,
        )

        // Then
        verify(filterValidator, times(1))
            .updateDateFilter(groupId, filterId, lower, upper)
    }
    //endregion

    //region resetFilters
    @Test
    fun `When resetFilters is called, Then filterValidator#resetFilters is invoked`() {
        // When
        interactor.resetFilters()

        // Then
        verify(filterValidator, times(1)).resetFilters()
    }
    //endregion

    //region updateSort
    @Test
    fun `When updateSort is called, Then filterValidator#updateSort is invoked with the same id`() {
        // When
        interactor.updateSort(filterId = "sortId")

        // Then
        verify(filterValidator, times(1)).updateSort("sortId")
    }
    //endregion

    //region initializeFilters
    @Test
    fun `When initializeFilters is called, Then filterValidator#initializeValidator is invoked with some Filters and the source list`() {
        // Given
        whenever(resourceProvider.getString(R.string.transactions_filter_item_no_relying_party_transactions))
            .thenReturn(mockedNoPartyFilterName)
        mockGetFiltersStrings()
        val list = FilterableList(items = emptyList())

        // When
        interactor.initializeFilters(filterableList = list)

        // Then
        // Strict equality on the Filters argument fails because the static filter group definitions
        // each carry FilterAction lambdas (Sort/Filter/FilterMultipleAction) that are not
        // reference-equal across two construction calls, so any<Filters>() is used.
        verify(filterValidator, times(1)).initializeValidator(
            org.mockito.kotlin.any<Filters>(),
            eq(list)
        )
    }
    //endregion

    //region refresh after deletion

    // Case 1:
    // 1. Search and applied date/status/type/party filters match two issuance rows.
    // 2. Reload removes the older row while another party remains in storage.
    //
    // Case 1 Expected Result:
    // Only the surviving match remains, with the query and selections preserved and new date bounds.
    @Test
    fun `Given Case 1, When transactions are refreshed, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val deleted =
                mockedIssuanceLogDomain.copy(time = mockedIssuanceLogDomain.time.minusMonths(2))
            val retained = mockedIssuanceLogDomain.copy(
                id = "retained-issuance",
                time = deleted.time.plusMonths(1),
            )
            val otherParty = mockedSigningLogDomain.copy(time = retained.time.plusMonths(1))
            mockGetTransactionLogsCall(response = listOf(deleted, retained, otherParty))
            val realInteractor =
                createInteractorWithRealFilters(scope = coroutineRule.testScope.backgroundScope)
            realInteractor.onFilterStateChange().runFlowTest {
                val initial = realInteractor.getTransactions()
                    .first() as TransactionInteractorGetTransactionsPartialState.Success
                realInteractor.initializeFilters(initial.allTransactions)
                realInteractor.applyFilters()
                awaitItem()
                listOf(
                    TransactionFilterIds.FILTER_BY_STATUS_GROUP_ID to TransactionFilterIds.FILTER_BY_STATUS_NOT_COMPLETED,
                    TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_GROUP_ID to TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_SIGNING,
                    TransactionFilterIds.FILTER_BY_PARTY_GROUP_ID to TransactionFilterIds.FILTER_BY_PARTY_WITHOUT_NAME,
                    TransactionFilterIds.FILTER_BY_PARTY_GROUP_ID to "party:$mockedTransactionServiceName",
                ).forEach { (group, filter) ->
                    realInteractor.updateFilter(filterGroupId = group, filterId = filter)
                    awaitItem()
                }
                realInteractor.updateDateFilterById(
                    filterGroupId = TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_GROUP_ID,
                    filterId = TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_RANGE,
                    lowerLimitDate = deleted.time,
                    upperLimitDate = retained.time,
                )
                awaitItem()
                realInteractor.applySearch("  ${mockedTransactionIssuerName.uppercase()}  ")
                val before =
                    awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult
                mockGetTransactionLogsCall(response = listOf(retained, otherParty))

                // When
                val refreshed = realInteractor.getTransactions()
                    .first() as TransactionInteractorGetTransactionsPartialState.Success
                realInteractor.initializeFilters(refreshed.allTransactions)
                realInteractor.applyFilters()
                val after = awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult

                // Then
                assertEquals(listOf(retained.id, deleted.id), before.transactionIds())
                assertEquals(listOf(retained.id), after.transactionIds())
                assertEquals(
                    retained.time.toLocalDate() to otherParty.time.toLocalDate(),
                    refreshed.availableDates
                )
                assertEquals(before.filters, after.filters)
                assertEquals(false, after.allDefaultFiltersAreSelected)
                assertEquals(1, after.transactions.size)
                assertTrue(after.sortOrder is SortOrder.Descending)

                realInteractor.resetFilters()
                val reset = awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult
                assertEquals(listOf(retained.id), reset.transactionIds())
                realInteractor.applySearch("")
                val cleared =
                    awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult
                assertEquals(listOf(otherParty.id, retained.id), cleared.transactionIds())
            }
        }
    }

    // Case 2:
    // 1. A party filter and search match the only row for that party.
    // 2. Reload removes that row, leaving a different party.
    //
    // Case 2 Expected Result:
    // The removed party option disappears, the remaining selection survives, and no empty period group remains.
    @Test
    fun `Given Case 2, When transactions are refreshed, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val deleted = mockedPresentationLogDomain
            val retained = mockedIssuanceLogDomain.copy(time = deleted.time.plusMonths(1))
            mockGetTransactionLogsCall(response = listOf(deleted, retained))
            val realInteractor =
                createInteractorWithRealFilters(scope = coroutineRule.testScope.backgroundScope)
            realInteractor.onFilterStateChange().runFlowTest {
                val initial = realInteractor.getTransactions()
                    .first() as TransactionInteractorGetTransactionsPartialState.Success
                realInteractor.initializeFilters(initial.allTransactions)
                realInteractor.applyFilters()
                awaitItem()
                realInteractor.updateFilter(
                    filterGroupId = TransactionFilterIds.FILTER_BY_PARTY_GROUP_ID,
                    filterId = "party:$mockedTransactionIssuerName",
                )
                awaitItem()
                realInteractor.applySearch(mockedTransactionPartyName)
                val before =
                    awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult
                mockGetTransactionLogsCall(response = listOf(retained))

                // When
                val refreshed = realInteractor.getTransactions()
                    .first() as TransactionInteractorGetTransactionsPartialState.Success
                realInteractor.initializeFilters(refreshed.allTransactions)
                realInteractor.applyFilters()
                val after = awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult

                // Then
                assertEquals(listOf(deleted.id), before.transactionIds())
                assertTrue(after.transactions.isEmpty())
                assertEquals(
                    retained.time.toLocalDate() to retained.time.toLocalDate(),
                    refreshed.availableDates
                )
                val parties =
                    after.filters.single { filter -> filter.header.itemId == TransactionFilterIds.FILTER_BY_PARTY_GROUP_ID }
                assertEquals(
                    listOf(
                        TransactionFilterIds.FILTER_BY_PARTY_WITHOUT_NAME,
                        "party:$mockedTransactionIssuerName"
                    ),
                    parties.nestedItems.map { item -> item.header.itemId },
                )
                assertEquals(
                    false,
                    (parties.nestedItems.last().header.trailingContentData as ListItemTrailingContentDataUi.Checkbox)
                        .checkboxData.isChecked,
                )
                realInteractor.resetFilters()
                val reset = awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult
                assertTrue(reset.transactions.isEmpty())
                realInteractor.applySearch("")
                val cleared =
                    awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult
                assertEquals(listOf(retained.id), cleared.transactionIds())
            }
        }
    }

    // Case 3:
    // 1. Reload removes the final visible row while a date filter and search are active.
    // 2. DDR and DPA report rows remain in storage.
    //
    // Case 3 Expected Result:
    // Empty results, no period groups or named parties, and no available date bounds.
    @Test
    fun `Given Case 3, When transactions are refreshed, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val deleted = mockedPresentationLogDomain
            mockGetTransactionLogsCall(response = listOf(deleted))
            val realInteractor =
                createInteractorWithRealFilters(scope = coroutineRule.testScope.backgroundScope)
            realInteractor.onFilterStateChange().runFlowTest {
                val initial = realInteractor.getTransactions()
                    .first() as TransactionInteractorGetTransactionsPartialState.Success
                realInteractor.initializeFilters(initial.allTransactions)
                realInteractor.applyFilters()
                awaitItem()
                realInteractor.updateDateFilterById(
                    filterGroupId = TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_GROUP_ID,
                    filterId = TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_RANGE,
                    lowerLimitDate = deleted.time,
                    upperLimitDate = deleted.time,
                )
                awaitItem()
                realInteractor.applySearch(mockedTransactionPartyName)
                awaitItem()
                mockGetTransactionLogsCall(
                    response = listOf(
                        mockedDataDeletionLogDomain,
                        mockedDpaReportLogDomain
                    )
                )

                // When
                val refreshed = realInteractor.getTransactions()
                    .first() as TransactionInteractorGetTransactionsPartialState.Success
                realInteractor.initializeFilters(refreshed.allTransactions)
                realInteractor.applyFilters()
                val after = awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult

                // Then
                assertTrue(refreshed.allTransactions.items.isEmpty())
                assertEquals(null, refreshed.availableDates)
                assertTrue(after.transactions.isEmpty())
                val parties =
                    after.filters.single { filter -> filter.header.itemId == TransactionFilterIds.FILTER_BY_PARTY_GROUP_ID }
                assertEquals(
                    listOf(TransactionFilterIds.FILTER_BY_PARTY_WITHOUT_NAME),
                    parties.nestedItems.map { item -> item.header.itemId },
                )
                realInteractor.applySearch("")
                assertTrue((awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult).transactions.isEmpty())
                realInteractor.resetFilters()
                assertTrue((awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult).transactions.isEmpty())
            }
        }
    }

    // Case 4:
    // 1. An applied date range matches the deleted row but excludes a surviving row from the same party.
    //
    // Case 4 Expected Result:
    // Reload retains that date constraint; clearing search does not expose the out-of-range row.
    @Test
    fun `Given Case 4, When transactions are refreshed, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val deleted = mockedIssuanceLogDomain
            val retained =
                deleted.copy(id = "outside-date-range", time = deleted.time.plusMonths(1))
            mockGetTransactionLogsCall(response = listOf(deleted, retained))
            val realInteractor =
                createInteractorWithRealFilters(scope = coroutineRule.testScope.backgroundScope)
            realInteractor.onFilterStateChange().runFlowTest {
                val initial = realInteractor.getTransactions()
                    .first() as TransactionInteractorGetTransactionsPartialState.Success
                realInteractor.initializeFilters(initial.allTransactions)
                realInteractor.applyFilters()
                awaitItem()
                realInteractor.updateDateFilterById(
                    filterGroupId = TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_GROUP_ID,
                    filterId = TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_RANGE,
                    lowerLimitDate = deleted.time,
                    upperLimitDate = deleted.time,
                )
                awaitItem()
                realInteractor.applySearch(mockedTransactionIssuerName)
                val before =
                    awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult
                mockGetTransactionLogsCall(response = listOf(retained))

                // When
                val refreshed = realInteractor.getTransactions()
                    .first() as TransactionInteractorGetTransactionsPartialState.Success
                realInteractor.initializeFilters(refreshed.allTransactions)
                realInteractor.applyFilters()
                val after = awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult

                // Then
                assertEquals(listOf(deleted.id), before.transactionIds())
                assertTrue(after.transactions.isEmpty())
                assertEquals(
                    retained.time.toLocalDate() to retained.time.toLocalDate(),
                    refreshed.availableDates
                )
                realInteractor.applySearch("")
                assertTrue((awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult).transactions.isEmpty())
                realInteractor.resetFilters()
                assertEquals(
                    listOf(retained.id),
                    (awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult).transactionIds(),
                )
            }
        }
    }
    //endregion

    //region getTransactionCategory

    // Case 1:
    // dateTime is today => TransactionCategoryUi.Today
    @Test
    fun `Given today, When getTransactionCategory is called, Then Today is returned`() {
        // Given
        val today = LocalDateTime.now()

        // When
        val result = interactor.getTransactionCategory(dateTime = today)

        // Then
        assertEquals(TransactionCategoryUi.Today, result)
    }

    // Case 2:
    // dateTime is within this week but not today => TransactionCategoryUi.ThisWeek
    @Test
    fun `Given a date within this week but not today, When getTransactionCategory is called, Then ThisWeek is returned`() {
        // Given
        val today = LocalDate.now()
        val mondayOfThisWeek = today.with(DayOfWeek.MONDAY)
        val thisWeekNotToday = if (today.dayOfWeek != DayOfWeek.MONDAY) {
            mondayOfThisWeek.atTime(10, 0)
        } else {
            mondayOfThisWeek.plusDays(1).atTime(10, 0)
        }

        // When
        val result = interactor.getTransactionCategory(dateTime = thisWeekNotToday)

        // Then
        assertEquals(TransactionCategoryUi.ThisWeek, result)
    }

    // Case 3:
    // dateTime is older than this week => TransactionCategoryUi.Month(dateTime)
    @Test
    fun `Given a date older than this week, When getTransactionCategory is called, Then Month is returned`() {
        // Given
        val twoMonthsAgo = LocalDateTime.now().minusMonths(2)

        // When
        val result = interactor.getTransactionCategory(dateTime = twoMonthsAgo)

        // Then
        assertTrue(result is TransactionCategoryUi.Month)
        assertEquals(TransactionCategoryUi.Month(twoMonthsAgo), result)
    }
    //endregion

    //region getTransactions

    // Case 1:
    // 1. The controller returns issuance, presentation and signing entries with
    //    creation dates that exercise the JustNow / WithinLastHour / Today / WithinMonth
    //    branches of toFormattedDisplayableDate.

    // Case 1 Expected Result:
    // Success state with allTransactions.items.size == 4 and a non-null availableDates pair.
    @Test
    fun `Given Case 1, When getTransactions is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            val justNow = LocalDateTime.now()
            val withinLastHour = LocalDateTime.now().minusMinutes(30)
            // "today" needs to be today's date AND more than 60 minutes ago so it doesn't fall
            // into the WithinLastHour branch. Subtracting 2 hours and floor-rounding to the hour
            // is stable for any clock-state except the first 2 hours after midnight; the test
            // dispatcher used here is wall-clock-driven so this picks the Today branch reliably
            // during business-hour CI runs and remains a documented limitation otherwise.
            val nowMinusTwoHours =
                LocalDateTime.now().minusHours(2).withMinute(0).withSecond(0).withNano(0)
            val today = if (nowMinusTwoHours.toLocalDate() == LocalDateTime.now().toLocalDate()) {
                nowMinusTwoHours
            } else {
                // Fallback: between 12am and 2am — pick noon of today instead which is still today
                // but may be in the future from now(). isToday() compares dates so it still works.
                LocalDateTime.now().toLocalDate().atTime(12, 0)
            }
            val twoMonthsAgo = LocalDateTime.now().minusMonths(2)

            val transactions = listOf(
                mockedIssuanceLogDomain.copy(
                    id = "tx1",
                    result = TransactionResultDomain.Completed,
                    time = justNow,
                ),
                mockedPresentationLogDomain.copy(
                    id = "tx2",
                    result = TransactionResultDomain.NotCompleted(reason = null),
                    time = withinLastHour,
                ),
                mockedSigningLogDomain.copy(
                    id = "tx3",
                    result = TransactionResultDomain.Completed,
                    time = today,
                ),
                mockedIssuanceLogDomain.copy(
                    id = "tx4",
                    result = TransactionResultDomain.NotCompleted(reason = null),
                    time = twoMonthsAgo,
                ),
            )
            whenever(walletCoreTransactionLogController.getTransactionLogs()).thenReturn(
                transactions
            )
            mockTransactionRowStrings()

            // When
            interactor.getTransactions().runFlowTest {
                // Then
                val result = awaitItem()
                assertTrue(result is TransactionInteractorGetTransactionsPartialState.Success)
                result as TransactionInteractorGetTransactionsPartialState.Success

                assertEquals(4, result.allTransactions.items.size)
                val availableDates = result.availableDates
                assertNotNull(availableDates)
                assertEquals(twoMonthsAgo.toLocalDate(), availableDates!!.first)
                assertEquals(justNow.toLocalDate(), availableDates.second)

                val presentationItem = result.allTransactions.items[1]
                val presentationAttrs =
                    presentationItem.attributes as TransactionsFilterableAttributes
                assertEquals(mockedTransactionPartyName, presentationAttrs.partyName)
                assertEquals(TransactionTypeUi.PRESENTATION, presentationAttrs.transactionType)
                assertEquals(TransactionStatusUi.NotCompleted, presentationAttrs.transactionStatus)

                val issuanceAttrs =
                    result.allTransactions.items[0].attributes as TransactionsFilterableAttributes
                assertEquals(
                    mockedIssuanceLogDomain.details.issuer.name?.text,
                    issuanceAttrs.partyName
                )

                val signingAttrs =
                    result.allTransactions.items[2].attributes as TransactionsFilterableAttributes
                assertEquals(mockedSigningLogDomain.service.name?.text, signingAttrs.partyName)
            }
        }
    }

    // Case 2:
    // 1. walletCoreTransactionLogController.getTransactionLogs throws an exception with a message.

    // Case 2 Expected Result:
    // Failure with the thrown exception's localized message.
    @Test
    fun `Given Case 2, When getTransactions is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreTransactionLogController.getTransactionLogs())
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.getTransactions().runFlowTest {
                // Then
                assertEquals(
                    TransactionInteractorGetTransactionsPartialState.Failure(
                        error = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 3:
    // 1. walletCoreTransactionLogController.getTransactionLogs throws an exception with no message.

    // Case 3 Expected Result:
    // Failure with the generic error message.
    @Test
    fun `Given Case 3, When getTransactions is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreTransactionLogController.getTransactionLogs())
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.getTransactions().runFlowTest {
                // Then
                assertEquals(
                    TransactionInteractorGetTransactionsPartialState.Failure(
                        error = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 4:
    // 1. The controller returns all seven supported transaction types.
    //
    // Case 4 Expected Result:
    // Only the five list-visible types produce rows with their id, title, type, date and status.
    @Test
    fun `Given every stored transaction type, When getTransactions is called, Then only visible types have rows`() {
        coroutineRule.runTest {
            // Given
            mockGetTransactionLogsCall(response = mockedTransactionLogDomains)
            mockTransactionRowStrings()

            // When
            interactor.getTransactions().runFlowTest {
                // Then
                val result = awaitItem() as TransactionInteractorGetTransactionsPartialState.Success
                val rows =
                    result.allTransactions.items.map { item -> item.payload as TransactionUi }
                assertEquals(
                    mockedListTransactionLogDomains.map { transaction -> transaction.id },
                    rows.map { row -> row.uiData.header.itemId })
                assertEquals(
                    mockedListTransactionTitles,
                    rows.map { row -> (row.uiData.header.mainContentData as ListItemMainContentDataUi.Text).text },
                )
                assertEquals(
                    mockedListTransactionTypeLabels,
                    rows.map { row -> (row.uiData.header.trailingContentData as ListItemTrailingContentDataUi.TextWithIcon).text },
                )
                rows.forEach { row ->
                    assertEquals(TransactionStatusUi.Completed, row.uiStatus)
                    assertEquals("Completed", row.uiData.header.overlineText)
                    assertNotNull(row.uiData.header.supportingContentData)
                }
                assertEquals(
                    mockedListTransactionLogDomains.map { transaction -> transaction.time },
                    result.allTransactions.items.map { item -> (item.attributes as TransactionsFilterableAttributes).creationLocalDateTime },
                )
            }
        }
    }

    // Case 5:
    // 1. The log contains completed and incomplete entries of every type, and every type filter is selected.
    //
    // Case 5 Expected Result:
    // Only the five visible types have filters, and each selects its completed and incomplete rows.
    @Test
    fun `Given all stored transaction types, When the type filters are applied, Then only visible types are reachable`() {
        coroutineRule.runTest {
            // Given
            mockGetTransactionLogsCall(response = mockedTransactionLogDomains + mockedNotCompletedTransactionLogDomains)
            mockTransactionRowStrings()
            mockGetFiltersStrings()
            @Suppress("UNCHECKED_CAST")
            val group = interactor.getFilters().filterGroups.first { filterGroup ->
                filterGroup.id == TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_GROUP_ID
            } as FilterGroup.MultipleSelectionFilterGroup<TransactionsFilterableAttributes>

            // When
            interactor.getTransactions().runFlowTest {
                val source =
                    (awaitItem() as TransactionInteractorGetTransactionsPartialState.Success).allTransactions
                val allSelected = group.filterableAction.applyFilter(source, group)

                // Then
                assertEquals(source.items.toSet(), allSelected.items.toSet())
                assertEquals(
                    mockedListTransactionTypeLabels.toSet(),
                    group.filters.map { filter -> filter.name }.toSet()
                )
                group.filters.forEach { filter ->
                    val selection = group.copy(filters = listOf(filter))
                    val selected = group.filterableAction.applyFilter(source, selection)
                    assertEquals(2, selected.items.size)
                    selected.items.forEach { item ->
                        val row = item.payload as TransactionUi
                        assertEquals(
                            filter.name,
                            (row.uiData.header.trailingContentData as ListItemTrailingContentDataUi.TextWithIcon).text,
                        )
                    }
                }
            }
        }
    }

    // Case 6:
    // 1. Party and display metadata are missing from otherwise valid entries.
    //
    // Case 6 Expected Result:
    // Rows use a raw credential identifier, file name or localized transaction type as appropriate.
    @Test
    fun `Given transactions with missing names, When getTransactions is called, Then every title has a fallback`() {
        coroutineRule.runTest {
            // Given
            val unknownIssuer = mockedIssuanceLogDomain.details.issuer.copy(name = null)
            val transactions = listOf(
                mockedPresentationLogDomain.copy(party = mockedPresentationLogDomain.party.copy(name = null)),
                mockedIssuanceLogDomain.copy(details = mockedIssuanceLogDomain.details.copy(issuer = unknownIssuer)),
                mockedIssuanceLogDomain.copy(
                    id = "unknown-issuance",
                    details = mockedIssuanceLogDomain.details.copy(
                        issuer = unknownIssuer,
                        credentials = emptyList()
                    ),
                ),
                mockedDeletionLogDomain.copy(issuer = unknownIssuer),
                mockedDeletionLogDomain.copy(
                    id = "unknown-deletion",
                    issuer = unknownIssuer,
                    credential = mockedTransactionCredential.copy(identifier = " "),
                ),
                mockedSigningLogDomain.copy(service = mockedSigningLogDomain.service.copy(name = null)),
                mockedSigningLogDomain.copy(
                    id = "unknown-signing",
                    service = mockedSigningLogDomain.service.copy(name = null),
                    fileName = null,
                ),
                mockedDataDeletionLogDomain.copy(party = mockedDataDeletionLogDomain.party.copy(name = null)),
                mockedDpaReportLogDomain.copy(dpaName = null),
            )
            mockGetTransactionLogsCall(response = transactions)
            mockTransactionRowStrings()

            // When
            interactor.getTransactions().runFlowTest {
                // Then
                val result = awaitItem() as TransactionInteractorGetTransactionsPartialState.Success
                val titles = result.allTransactions.items.map { item ->
                    val row = item.payload as TransactionUi
                    (row.uiData.header.mainContentData as ListItemMainContentDataUi.Text).text
                }
                assertEquals(
                    listOf(
                        "Presentation",
                        mockedTransactionCredential.identifier,
                        "Issuance",
                        mockedTransactionCredential.identifier,
                        mockedCredentialDeletionTypeLabel,
                        mockedSigningLogDomain.fileName,
                        "Signing",
                    ),
                    titles,
                )
            }
        }
    }


    // Case 7:
    // 1. Every transaction type carries party and credential or file metadata.
    //
    // Case 7 Expected Result:
    // Only visible rows supply party and searchable names, without duplicate tags.
    @Test
    fun `Given every transaction type with metadata, When getTransactions is called, Then party and search attributes cover only visible types`() {
        coroutineRule.runTest {
            // Given
            mockGetTransactionLogsCall(response = mockedSearchableTransactionLogDomains)
            mockTransactionRowStrings()

            // When
            interactor.getTransactions().runFlowTest {
                // Then
                val source =
                    (awaitItem() as TransactionInteractorGetTransactionsPartialState.Success).allTransactions
                val attributes =
                    source.items.map { item -> item.attributes as TransactionsFilterableAttributes }
                assertEquals(
                    mockedListTransactionPartyNames,
                    attributes.map { item -> item.partyName })
                assertEquals(
                    listOf(
                        listOf(mockedTransactionPartyName, mockedTransactionIntermediaryName),
                        listOf(mockedTransactionIssuerName),
                        listOf(mockedTransactionIssuerName),
                        listOf(
                            mockedTransactionIssuerName,
                            mockedOtherTransactionCredential.identifier
                        ),
                        listOf(mockedTransactionServiceName, mockedSigningLogDomain.fileName),
                    ),
                    attributes.map { item -> item.searchTags },
                )
            }
        }
    }

    // Case 8:
    // 1. Stored transactions contain party/file fields, deletion credential types and hidden action names.
    //
    // Case 8 Expected Result:
    // Allowed fields match without case sensitivity; other credential fields and friendly names do not.
    @Test
    fun `Given searchable transaction rows, When a query is applied, Then only allowed fields are matched`() {
        coroutineRule.runTest {
            // Given
            mockGetTransactionLogsCall(response = mockedSearchableTransactionLogDomains)
            mockTransactionRowStrings()
            val queries = listOf(
                mockedTransactionPartyName to setOf("presentation"),
                mockedTransactionIntermediaryName.uppercase() to setOf("presentation"),
                mockedTransactionIssuerName to setOf("issuance", "reissuance", "deletion"),
                mockedTransactionServiceName to setOf("signing"),
                mockedTransactionDpaName to emptySet(),
                mockedRequestedTransactionCredential.identifier.uppercase() to emptySet(),
                mockedPresentedTransactionCredential.identifier to emptySet(),
                mockedOtherTransactionCredential.identifier.uppercase() to setOf("deletion"),
                mockedOtherTransactionCredential.identifier.substringAfterLast(':') to setOf("deletion"),
                mockedTransactionCredential.identifier to emptySet(),
                mockedPidDocName to emptySet(),
                mockedDataDeletionTypeLabel to emptySet(),
                mockedDpaReportTypeLabel to emptySet(),
                "Presentation" to emptySet(),
                "SIGNED.PDF" to setOf("signing"),
                "no matching transaction" to emptySet(),
                "" to mockedListTransactionLogDomains.map { transaction -> transaction.id }.toSet(),
            )

            // When
            interactor.getTransactions().runFlowTest {
                val source =
                    (awaitItem() as TransactionInteractorGetTransactionsPartialState.Success).allTransactions

                // Then
                queries.forEach { (query, expectedIds) ->
                    val actualIds = source.filterByQuery(query).items.map { item ->
                        (item.payload as TransactionUi).uiData.header.itemId
                    }.toSet()
                    assertEquals("Query: $query", expectedIds, actualIds)
                }
            }
        }
    }

    // Case 9:
    // 1. Each type has completed and not-completed rows, including declined and deferred outcomes.
    //
    // Case 9 Expected Result:
    // Status filters retain all non-completed outcomes under the Not completed label.
    @Test
    fun `Given completed and incomplete transactions, When status filters are applied, Then every non-completed outcome uses one consistent status`() {
        coroutineRule.runTest {
            // Given
            mockGetTransactionLogsCall(response = mockedTransactionLogDomains + mockedNotCompletedTransactionLogDomains)
            mockTransactionRowStrings()
            mockGetFiltersStrings()
            @Suppress("UNCHECKED_CAST")
            val group = interactor.getFilters().filterGroups.first { filterGroup ->
                filterGroup.id == TransactionFilterIds.FILTER_BY_STATUS_GROUP_ID
            } as FilterGroup.MultipleSelectionFilterGroup<TransactionsFilterableAttributes>

            // When
            interactor.getTransactions().runFlowTest {
                val source =
                    (awaitItem() as TransactionInteractorGetTransactionsPartialState.Success).allTransactions
                val selected =
                    group.filters.single { filter -> filter.id == TransactionFilterIds.FILTER_BY_STATUS_NOT_COMPLETED }
                val incomplete = group.filterableAction.applyFilter(
                    source,
                    group.copy(filters = listOf(selected))
                )
                val rows = incomplete.items.map { item -> item.payload as TransactionUi }

                // Then
                assertEquals("Not completed", selected.name)
                assertEquals(source.items, group.filterableAction.applyFilter(source, group).items)
                assertEquals(
                    mockedListNotCompletedTransactionIds,
                    rows.map { row -> row.uiData.header.itemId })
                rows.forEach { row ->
                    assertEquals(TransactionStatusUi.NotCompleted, row.uiStatus)
                    assertEquals("Not completed", row.uiData.header.overlineText)
                }
            }
        }
    }

    // Case 10:
    // 1. All types have missing or blank party names and some display or file names are blank.
    //
    // Case 10 Expected Result:
    // Titles retain display fallbacks while search tags contain only available allowed fields.
    @Test
    fun `Given missing or blank transaction names, When getTransactions is called, Then rows retain usable fallbacks without blank parties`() {
        coroutineRule.runTest {
            // Given
            mockGetTransactionLogsCall(response = mockedUnnamedTransactionLogDomains)
            mockTransactionRowStrings()

            // When
            interactor.getTransactions().runFlowTest {
                // Then
                val source =
                    (awaitItem() as TransactionInteractorGetTransactionsPartialState.Success).allTransactions
                val attributes =
                    source.items.map { item -> item.attributes as TransactionsFilterableAttributes }
                assertTrue(attributes.all { item -> item.partyName == null })
                assertEquals(
                    listOf(
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        listOf(mockedTransactionCredential.identifier),
                        emptyList()
                    ),
                    attributes.map { item -> item.searchTags },
                )
                assertEquals(source, source.filterByQuery(""))
                assertTrue(source.filterByQuery(mockedPidDocName).items.isEmpty())
                assertEquals(
                    listOf(mockedDeletionLogDomain.id),
                    source.filterByQuery(mockedTransactionCredential.identifier).items.map { row ->
                        (row.payload as TransactionUi).uiData.header.itemId
                    },
                )
                val titles = source.items.map { item ->
                    ((item.payload as TransactionUi).uiData.header.mainContentData as ListItemMainContentDataUi.Text).text
                }
                assertEquals(
                    listOf(
                        "Presentation",
                        mockedTransactionCredential.identifier,
                        mockedTransactionCredential.identifier,
                        mockedTransactionCredential.identifier,
                        "Signing"
                    ),
                    titles,
                )
            }
        }
    }

    // Case 11:
    // 1. Some intermediary names are blank or equal the relying-party name.
    // 2. A signing's provider name equals its filename, or the provider name is blank.
    // 3. A deletion's credential type is blank or equals its provider name.
    //
    // Case 11 Expected Result:
    // Blank tags are omitted, equal tags occur once and the remaining fields stay searchable.
    @Test
    fun `Given blank and duplicate party names, When getTransactions is called, Then search tags contain only distinct usable names`() {
        coroutineRule.runTest {
            // Given
            val rows = listOf(
                mockedPresentationLogDomain.copy(
                    intermediary = mockedTransactionIntermediary.copy(
                        name = LocalizedTextDomain(
                            mockedTransactionLanguageTag,
                            mockedTransactionPartyName
                        )
                    )
                ),
                mockedPresentationLogDomain.copy(
                    id = "blank-intermediary",
                    intermediary = mockedTransactionIntermediary.copy(
                        name = LocalizedTextDomain(
                            mockedTransactionLanguageTag,
                            " \t"
                        )
                    ),
                ),
                mockedPresentationLogDomain.copy(
                    id = "blank-names",
                    party = mockedPresentationLogDomain.party.copy(name = null),
                    intermediary = mockedTransactionIntermediary.copy(
                        name = LocalizedTextDomain(
                            mockedTransactionLanguageTag,
                            " \t"
                        )
                    ),
                ),
                mockedSigningLogDomain.copy(fileName = mockedTransactionServiceName),
                mockedSigningLogDomain.copy(
                    id = "unnamed-provider",
                    service = mockedSigningLogDomain.service.copy(
                        name = LocalizedTextDomain(
                            mockedTransactionLanguageTag,
                            " \t"
                        )
                    ),
                ),
                mockedDeletionLogDomain.copy(
                    credential = mockedTransactionCredential.copy(identifier = mockedTransactionIssuerName),
                ),
                mockedDeletionLogDomain.copy(
                    id = "blank-credential-type",
                    credential = mockedTransactionCredential.copy(identifier = " \t"),
                ),
            )
            mockGetTransactionLogsCall(response = rows)
            mockTransactionRowStrings()

            // When
            interactor.getTransactions().runFlowTest {
                val source =
                    (awaitItem() as TransactionInteractorGetTransactionsPartialState.Success).allTransactions

                // Then
                assertEquals(
                    listOf(
                        listOf(mockedTransactionPartyName),
                        listOf(mockedTransactionPartyName),
                        emptyList(),
                        listOf(mockedTransactionServiceName),
                        listOf(mockedSigningLogDomain.fileName),
                        listOf(mockedTransactionIssuerName),
                        listOf(mockedTransactionIssuerName),
                    ),
                    source.items.map { item -> item.attributes.searchTags },
                )
            }
        }
    }

    // Case 12:
    // 1. An issuance is stored with DDR and DPA report rows outside its date range.
    // 2. Only the hidden actions carry the relying-party and authority names.
    //
    // Case 12 Expected Result:
    // Hidden actions affect neither dates nor parties and remain absent through search, filter edits and reset.
    @Test
    fun `Given hidden actions, When list criteria change, Then only visible transactions can be returned`() {
        coroutineRule.runTest {
            // Given
            val visible = mockedIssuanceLogDomain
            val request = mockedDataDeletionLogDomain.copy(time = visible.time.minusYears(1))
            val report = mockedDpaReportLogDomain.copy(time = visible.time.plusYears(1))
            mockGetTransactionLogsCall(response = listOf(request, visible, report))
            val realInteractor =
                createInteractorWithRealFilters(scope = coroutineRule.testScope.backgroundScope)

            // When
            val loaded = realInteractor.getTransactions()
                .first() as TransactionInteractorGetTransactionsPartialState.Success
            realInteractor.onFilterStateChange().runFlowTest {
                realInteractor.initializeFilters(loaded.allTransactions)
                realInteractor.applyFilters()
                val initial =
                    awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult

                // Then
                assertEquals(listOf(visible.id), initial.transactionIds())
                assertEquals(
                    visible.time.toLocalDate() to visible.time.toLocalDate(),
                    loaded.availableDates
                )
                val parties =
                    initial.filters.single { filter -> filter.header.itemId == TransactionFilterIds.FILTER_BY_PARTY_GROUP_ID }
                assertEquals(
                    listOf(
                        TransactionFilterIds.FILTER_BY_PARTY_WITHOUT_NAME,
                        "party:$mockedTransactionIssuerName"
                    ),
                    parties.nestedItems.map { item -> item.header.itemId },
                )
                listOf(
                    mockedTransactionPartyName,
                    mockedTransactionDpaName,
                    request.id,
                    report.id,
                    mockedDataDeletionTypeLabel,
                    mockedDpaReportTypeLabel,
                ).forEach { query ->
                    realInteractor.applySearch(query)
                    val matched =
                        awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult
                    assertTrue("Query: $query", matched.transactions.isEmpty())
                }
                listOf(" \t", "").forEach { query ->
                    realInteractor.applySearch(query)
                    assertEquals(
                        listOf(visible.id),
                        (awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult).transactionIds(),
                    )
                }
                listOf(
                    "by_transaction_type_data_deletion_request",
                    "by_transaction_type_dpa_report"
                ).forEach { removedId ->
                    realInteractor.updateFilter(
                        filterGroupId = TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_GROUP_ID,
                        filterId = removedId,
                    )
                    val updated =
                        awaitItem() as TransactionInteractorFilterPartialState.FilterUpdateResult
                    val types =
                        updated.filters.single { filter -> filter.header.itemId == TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_GROUP_ID }
                    assertEquals(
                        mockedListTransactionTypeFilterIds,
                        types.nestedItems.map { item -> item.header.itemId })
                }
                mockedListTransactionTypeFilterIds.forEach { typeId ->
                    realInteractor.updateFilter(
                        filterGroupId = TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_GROUP_ID,
                        filterId = typeId,
                    )
                    assertTrue(awaitItem() is TransactionInteractorFilterPartialState.FilterUpdateResult)
                }
                listOf(
                    TransactionFilterIds.FILTER_BY_STATUS_COMPLETE,
                    TransactionFilterIds.FILTER_BY_STATUS_NOT_COMPLETED,
                ).forEach { statusId ->
                    realInteractor.updateFilter(
                        filterGroupId = TransactionFilterIds.FILTER_BY_STATUS_GROUP_ID,
                        filterId = statusId,
                    )
                    assertTrue(awaitItem() is TransactionInteractorFilterPartialState.FilterUpdateResult)
                }
                realInteractor.applyFilters()
                assertTrue((awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult).transactions.isEmpty())
                realInteractor.resetFilters()
                assertEquals(
                    listOf(visible.id),
                    (awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult).transactionIds(),
                )
                realInteractor.updateFilter(
                    filterGroupId = TransactionFilterIds.FILTER_BY_PARTY_GROUP_ID,
                    filterId = "party:$mockedTransactionIssuerName",
                )
                awaitItem()
                realInteractor.applyFilters()
                assertTrue((awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult).transactions.isEmpty())
                realInteractor.resetFilters()
                assertEquals(
                    listOf(visible.id),
                    (awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult).transactionIds(),
                )
                realInteractor.updateDateFilterById(
                    filterGroupId = TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_GROUP_ID,
                    filterId = TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_RANGE,
                    lowerLimitDate = report.time,
                    upperLimitDate = report.time,
                )
                awaitItem()
                realInteractor.applyFilters()
                assertTrue((awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult).transactions.isEmpty())
                realInteractor.resetFilters()
                assertEquals(
                    listOf(visible.id),
                    (awaitItem() as TransactionInteractorFilterPartialState.FilterApplyResult).transactionIds(),
                )
            }
        }
    }
    //endregion

    //region getFilters
    @Test
    fun `When getFilters is called, Then the returned Filters contains the expected static filter groups`() {
        // Given
        mockGetFiltersStrings()

        // When
        val result = interactor.getFilters()

        // Then
        val groupIds = result.filterGroups.map { it.id }
        assertEquals(
            listOf(
                TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_GROUP_ID,
                TransactionFilterIds.FILTER_BY_STATUS_GROUP_ID,
                TransactionFilterIds.FILTER_BY_PARTY_GROUP_ID,
                TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_GROUP_ID,
            ),
            groupIds
        )
        assertEquals(SortOrder.Descending(isDefault = true), result.sortOrder)
        assertEquals(TransactionFilterIds.FILTER_SORT_GROUP_ID, result.sort?.id)
        val typeFilters = result.filterGroups.single { filterGroup ->
            filterGroup.id == TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_GROUP_ID
        }.filters
        assertEquals(mockedListTransactionTypeFilterIds, typeFilters.map { filter -> filter.id })
        assertTrue(typeFilters.all { filter -> filter.selected && filter.isDefault })
    }
    //endregion

    //region addDynamicFilters

    // Case 1:
    // 1. The party filter group is present and the transactions list contains items
    //    with party names; the dynamic party filters are populated, prefixed
    //    by the "no party" filter.
    @Test
    fun `Given Case 1, When addDynamicFilters is called, Then the party group is populated with both the no-party filter and one filter per distinct party`() {
        // Given
        whenever(resourceProvider.getString(R.string.transactions_filter_item_no_relying_party_transactions))
            .thenReturn(mockedNoPartyFilterName)
        val initialFilters = Filters(
            filterGroups = listOf(
                FilterGroup.MultipleSelectionFilterGroup<TransactionsFilterableAttributes>(
                    id = TransactionFilterIds.FILTER_BY_PARTY_GROUP_ID,
                    name = "Relying Party",
                    filters = emptyList(),
                    filterableAction = FilterMultipleAction { _, _ -> true },
                )
            ),
            sortOrder = SortOrder.Descending(isDefault = true),
        )
        val transactions = FilterableList(
            items = listOf(
                filterableItemWithParty(name = "Acme"),
                filterableItemWithParty(name = null),
                filterableItemWithParty(name = "Acme"), // duplicate to verify distinctBy
            )
        )

        // When
        val result = interactor.addDynamicFilters(transactions, initialFilters)

        // Then
        val updatedGroup =
            result.filterGroups.first { group -> group.id == TransactionFilterIds.FILTER_BY_PARTY_GROUP_ID }
        val ids = updatedGroup.filters.map { it.id }
        assertEquals(
            listOf(TransactionFilterIds.FILTER_BY_PARTY_WITHOUT_NAME, "party:Acme"),
            ids
        )
    }

    // Case 2:
    // 1. A filter group whose id is not the party group => left unchanged.
    @Test
    fun `Given Case 2, When addDynamicFilters is called with a non-party group, Then the group is left unchanged`() {
        // Given
        val unrelatedGroup = FilterGroup.SingleSelectionFilterGroup(
            id = "unrelated_group",
            name = "Unrelated",
            filters = listOf(
                FilterItem(
                    id = "x",
                    name = "X",
                    selected = false,
                    isDefault = true,
                )
            )
        )
        val initialFilters = Filters(
            filterGroups = listOf(unrelatedGroup),
            sortOrder = SortOrder.Ascending(isDefault = true),
        )
        val transactions = FilterableList(items = emptyList())

        // When
        val result = interactor.addDynamicFilters(transactions, initialFilters)

        // Then
        assertEquals(unrelatedGroup, result.filterGroups.first())
    }

    // Case 3:
    // 1. Storage contains all seven transaction types and an unnamed presentation.
    //
    // Case 3 Expected Result:
    // Only visible rows supply distinct sorted parties, and each party selects its rows.
    @Test
    fun `Given parties across transaction types, When addDynamicFilters is called, Then every party and the no-party option select the right rows`() {
        coroutineRule.runTest {
            // Given
            val unnamed = mockedPresentationLogDomain.copy(
                id = "unnamed",
                party = mockedPresentationLogDomain.party.copy(name = null)
            )
            mockGetTransactionLogsCall(response = mockedTransactionLogDomains + unnamed)
            mockTransactionRowStrings()
            mockGetFiltersStrings()
            mockPartyFilterNameCall()
            val expectedIds = mapOf(
                mockedNoPartyFilterName to setOf(unnamed.id),
                mockedTransactionPartyName to setOf("presentation"),
                mockedTransactionIssuerName to setOf("issuance", "reissuance", "deletion"),
                mockedTransactionServiceName to setOf("signing"),
            )

            // When
            interactor.getTransactions().runFlowTest {
                val source =
                    (awaitItem() as TransactionInteractorGetTransactionsPartialState.Success).allTransactions

                @Suppress("UNCHECKED_CAST")
                val group = interactor.addDynamicFilters(
                    source,
                    interactor.getFilters()
                ).filterGroups.first { filterGroup ->
                    filterGroup.id == TransactionFilterIds.FILTER_BY_PARTY_GROUP_ID
                } as FilterGroup.MultipleSelectionFilterGroup<TransactionsFilterableAttributes>

                // Then
                assertEquals("Relying Party", group.name)
                assertEquals(mockedNoPartyFilterName, group.filters.first().name)
                assertEquals(
                    mockedListTransactionPartyNames.distinct()
                        .sortedBy { partyName -> partyName.lowercase() },
                    group.filters.drop(1).map { filter -> filter.name },
                )
                val allSelected = group.filterableAction.applyFilter(source, group)
                assertEquals(source.items.size, allSelected.items.size)
                assertEquals(source.items.toSet(), allSelected.items.toSet())
                group.filters.forEach { filter ->
                    val result = group.filterableAction.applyFilter(
                        source,
                        group.copy(filters = listOf(filter))
                    )
                    assertEquals(
                        expectedIds.getValue(filter.name),
                        result.items.map { item -> (item.payload as TransactionUi).uiData.header.itemId }
                            .toSet(),
                    )
                }
            }
        }
    }

    // Case 4:
    // 1. A real party's name equals the reserved no-party filter identifier.
    //
    // Case 4 Expected Result:
    // Its named filter remains distinct and selects only the named row.
    @Test
    fun `Given a party name equal to the no-party filter id, When addDynamicFilters is called, Then the named party has a distinct working filter`() {
        // Given
        mockGetFiltersStrings()
        mockPartyFilterNameCall()
        val name = TransactionFilterIds.FILTER_BY_PARTY_WITHOUT_NAME
        val named = filterableItemWithParty(name = name)
        val unnamed = filterableItemWithParty(name = null)
        val source = FilterableList(listOf(named, unnamed))

        // When
        @Suppress("UNCHECKED_CAST")
        val group =
            interactor.addDynamicFilters(
                source,
                interactor.getFilters()
            ).filterGroups.first { filterGroup ->
                filterGroup.id == TransactionFilterIds.FILTER_BY_PARTY_GROUP_ID
            } as FilterGroup.MultipleSelectionFilterGroup<TransactionsFilterableAttributes>
        val namedFilter = group.filters.single { filter -> filter.name == name }
        val selected =
            group.filterableAction.applyFilter(source, group.copy(filters = listOf(namedFilter)))

        // Then
        assertEquals(2, group.filters.map { filter -> filter.id }.distinct().size)
        assertEquals(listOf(named), selected.items)
    }

    // Case 5:
    // 1. An issuer filter selects issuance, re-issuance and deletion rows.
    // 2. The query names either the provider or an excluded credential.
    //
    // Case 5 Expected Result:
    // Provider search retains the selected party's rows; credential search remains excluded.
    @Test
    fun `Given an issuer filter and search, When both are applied, Then only allowed provider matches remain`() {
        coroutineRule.runTest {
            // Given
            mockGetTransactionLogsCall(response = mockedSearchableTransactionLogDomains)
            mockTransactionRowStrings()
            mockGetFiltersStrings()
            mockPartyFilterNameCall()

            // When
            interactor.getTransactions().runFlowTest {
                val source =
                    (awaitItem() as TransactionInteractorGetTransactionsPartialState.Success).allTransactions

                @Suppress("UNCHECKED_CAST")
                val group = interactor.addDynamicFilters(
                    source,
                    interactor.getFilters()
                ).filterGroups.first { filterGroup ->
                    filterGroup.id == TransactionFilterIds.FILTER_BY_PARTY_GROUP_ID
                } as FilterGroup.MultipleSelectionFilterGroup<TransactionsFilterableAttributes>
                val issuer =
                    group.filters.single { filter -> filter.name == mockedTransactionIssuerName }
                val filtered =
                    group.filterableAction.applyFilter(source, group.copy(filters = listOf(issuer)))
                val providerMatches =
                    filtered.filterByQuery(mockedTransactionIssuerName.uppercase())
                val credentialMatches =
                    filtered.filterByQuery(mockedRequestedTransactionCredential.identifier)

                // Then
                assertEquals(
                    listOf("issuance", "reissuance", "deletion"),
                    providerMatches.items.map { item -> (item.payload as TransactionUi).uiData.header.itemId },
                )
                assertTrue(credentialMatches.items.isEmpty())
            }
        }
    }

    //endregion

    //region onFilterStateChange

    // Case 1:
    // 1. filterValidator emits FilterApplyResult containing one TransactionUi payload and a Filters
    //    object with all four filter-group variants so both Checkbox and RadioButton trailing
    //    content branches are exercised.

    // Case 1 Expected Result:
    // TransactionInteractorFilterPartialState.FilterApplyResult with grouped transactions and
    // ExpandableListItemUi.NestedListItem filters containing the expected trailing types.
    @Test
    fun `Given Case 1, When onFilterStateChange emits FilterApplyResult, Then FilterApplyResult is mapped`() {
        coroutineRule.runTest {
            // Given
            val transactionItem = filterableTransactionItem()
            val filtersFromValidator = filtersWithEachGroupType()
            whenever(filterValidator.onFilterStateChange()).thenReturn(
                FilterValidatorPartialState.FilterListResult.FilterApplyResult(
                    filteredList = FilterableList(items = listOf(transactionItem)),
                    allDefaultFiltersAreSelected = true,
                    updatedFilters = filtersFromValidator,
                ).toFlow()
            )

            // When
            interactor.onFilterStateChange().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is TransactionInteractorFilterPartialState.FilterApplyResult)
                state as TransactionInteractorFilterPartialState.FilterApplyResult

                assertEquals(1, state.transactions.size)
                assertEquals(TransactionCategoryUi.Today, state.transactions.first().first)
                assertEquals(filtersFromValidator.sortOrder, state.sortOrder)
                assertEquals(true, state.allDefaultFiltersAreSelected)
                assertEquals(filtersFromValidator.filterGroups.size, state.filters.size)

                state.filters.forEach { filter ->
                    assertEquals(false, filter.isExpanded)
                    val headerTrailingContent = filter.header.trailingContentData
                    assertTrue(headerTrailingContent is ListItemTrailingContentDataUi.Icon)
                    assertEquals(
                        AppIcons.KeyboardArrowDown,
                        (headerTrailingContent as ListItemTrailingContentDataUi.Icon).iconData
                    )
                }

                val trailingTypes = state.filters.flatMap { nested ->
                    nested.nestedItems.map { it.header.trailingContentData }
                }
                assertTrue(trailingTypes.any { it is ListItemTrailingContentDataUi.Checkbox })
                assertTrue(trailingTypes.any { it is ListItemTrailingContentDataUi.RadioButton })
            }
        }
    }

    // Case 2:
    // 1. filterValidator emits FilterListEmptyResult.

    // Case 2 Expected Result:
    // FilterApplyResult with empty transactions.
    @Test
    fun `Given Case 2, When onFilterStateChange emits FilterListEmptyResult, Then FilterApplyResult with empty transactions is mapped`() {
        coroutineRule.runTest {
            // Given
            whenever(filterValidator.onFilterStateChange()).thenReturn(
                FilterValidatorPartialState.FilterListResult.FilterListEmptyResult(
                    updatedFilters = Filters.emptyFilters(),
                    allDefaultFiltersAreSelected = false,
                ).toFlow()
            )

            // When
            interactor.onFilterStateChange().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is TransactionInteractorFilterPartialState.FilterApplyResult)
                state as TransactionInteractorFilterPartialState.FilterApplyResult

                assertTrue(state.transactions.isEmpty())
                assertTrue(state.filters.isEmpty())
                assertEquals(false, state.allDefaultFiltersAreSelected)
            }
        }
    }

    // Case 3:
    // 1. filterValidator emits FilterUpdateResult.

    // Case 3 Expected Result:
    // FilterUpdateResult containing the updated filters mapped to ExpandableListItemUi.
    @Test
    fun `Given Case 3, When onFilterStateChange emits FilterUpdateResult, Then FilterUpdateResult is mapped`() {
        coroutineRule.runTest {
            // Given
            val filtersFromValidator = filtersWithEachGroupType()
            whenever(filterValidator.onFilterStateChange()).thenReturn(
                FilterValidatorPartialState.FilterUpdateResult(
                    updatedFilters = filtersFromValidator,
                ).toFlow()
            )

            // When
            interactor.onFilterStateChange().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is TransactionInteractorFilterPartialState.FilterUpdateResult)
                state as TransactionInteractorFilterPartialState.FilterUpdateResult

                assertEquals(filtersFromValidator.filterGroups.size, state.filters.size)
                assertEquals(filtersFromValidator.sortOrder, state.sortOrder)
            }
        }
    }
    //endregion

    //region filter lambdas defined in getFilters()
    // These tests invoke each FilterAction / FilterMultipleAction lambda directly to exercise
    // the predicate bodies, which would otherwise stay uncovered (the lambdas are stored on
    // the returned Filters but never executed by the interactor itself).

    @Test
    fun `When the sort filter's selector is applied, Then it returns the creationLocalDateTime of the attributes`() {
        // Given
        mockGetFiltersStrings()
        val filters = interactor.getFilters()
        val sort = filters.sort!!

        @Suppress("UNCHECKED_CAST")
        val sortAction = sort.filters.first().filterableAction
                as FilterAction.Sort<TransactionsFilterableAttributes, LocalDateTime>
        val attrs = attributes(creationLocalDateTime = LocalDateTime.of(2026, 5, 1, 12, 0))

        // When
        val key = sortAction.selector(attrs)

        // Then
        assertEquals(attrs.creationLocalDateTime, key)
    }

    @Test
    fun `When the date-range filter is applied, Then in-range matches, out-of-range fails, and non-DateTimeRangeFilterItem defaults to true`() {
        // Given
        mockGetFiltersStrings()
        val filters = interactor.getFilters()
        val dateGroup = filters.filterGroups.first { filterGroup ->
            filterGroup.id == TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_GROUP_ID
        }

        @Suppress("UNCHECKED_CAST")
        val dateAction = dateGroup.filters.first().filterableAction
                as FilterAction.Filter<TransactionsFilterableAttributes>
        val rangeFilter = FilterElement.DateTimeRangeFilterItem(
            id = TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_RANGE,
            name = "Range",
            selected = true,
            isDefault = true,
            startDateTime = LocalDateTime.of(2026, 1, 1, 0, 0),
            endDateTime = LocalDateTime.of(2026, 12, 31, 23, 59),
        )
        val inRange = attributes(creationLocalDateTime = LocalDateTime.of(2026, 5, 1, 12, 0))
        val outOfRange = attributes(creationLocalDateTime = LocalDateTime.of(2027, 5, 1, 12, 0))
        val beforeStart = attributes(creationLocalDateTime = LocalDateTime.of(2025, 5, 1, 12, 0))
        val nullDate = attributes(creationLocalDateTime = null)
        val nonDateFilter = FilterItem(
            id = "non_date",
            name = "Non date",
            selected = true,
            isDefault = false,
        )

        // When + Then — exercises both branches of isDateAttributeWithinFilterRange and
        // both sides of the ClosedRange.contains comparison (date after the end as well as
        // date before the start, so the in-operator's two comparisons both flip true→false).
        assertTrue(dateAction.predicate(inRange, rangeFilter))
        assertTrue(!dateAction.predicate(outOfRange, rangeFilter))
        assertTrue(!dateAction.predicate(beforeStart, rangeFilter))
        assertTrue(dateAction.predicate(nullDate, rangeFilter))
        assertTrue(dateAction.predicate(inRange, nonDateFilter))
    }

    @Test
    fun `When the status filter predicate is applied, Then completed-failed-other arms are all evaluated`() {
        // Given
        mockGetFiltersStrings()
        val filters = interactor.getFilters()
        val statusGroup = filters.filterGroups.first { filterGroup ->
            filterGroup.id == TransactionFilterIds.FILTER_BY_STATUS_GROUP_ID
        }

        @Suppress("UNCHECKED_CAST")
        val statusAction =
            (statusGroup as FilterGroup.MultipleSelectionFilterGroup<TransactionsFilterableAttributes>)
                .filterableAction
        val completedFilter = FilterItem(
            id = TransactionFilterIds.FILTER_BY_STATUS_COMPLETE,
            name = "Completed",
            selected = true,
            isDefault = true,
        )
        val notCompletedFilter = FilterItem(
            id = TransactionFilterIds.FILTER_BY_STATUS_NOT_COMPLETED,
            name = "Not completed",
            selected = true,
            isDefault = true,
        )
        val otherFilter = FilterItem(
            id = "other",
            name = "Other",
            selected = true,
            isDefault = false,
        )

        // When + Then
        assertTrue(
            statusAction.predicate(
                attributes(status = TransactionStatusUi.Completed),
                completedFilter
            )
        )
        assertTrue(
            !statusAction.predicate(
                attributes(status = TransactionStatusUi.NotCompleted),
                completedFilter
            )
        )
        assertTrue(
            statusAction.predicate(
                attributes(status = TransactionStatusUi.NotCompleted),
                notCompletedFilter
            )
        )
        assertTrue(
            !statusAction.predicate(
                attributes(status = TransactionStatusUi.Completed),
                notCompletedFilter
            )
        )
        assertTrue(
            statusAction.predicate(
                attributes(status = TransactionStatusUi.Completed),
                otherFilter
            )
        )
    }

    @Test
    fun `When the party filter predicate is applied, Then no-name-with-name-and-mismatch arms are all evaluated`() {
        // Given
        mockGetFiltersStrings()
        val filters = interactor.getFilters()
        val rpGroup = filters.filterGroups.first { filterGroup ->
            filterGroup.id == TransactionFilterIds.FILTER_BY_PARTY_GROUP_ID
        }

        @Suppress("UNCHECKED_CAST")
        val rpAction =
            (rpGroup as FilterGroup.MultipleSelectionFilterGroup<TransactionsFilterableAttributes>)
                .filterableAction
        val withoutNameFilter = FilterItem(
            id = TransactionFilterIds.FILTER_BY_PARTY_WITHOUT_NAME,
            name = "Without name",
            selected = true,
            isDefault = true,
        )
        val acmeFilter = FilterItem(
            id = "Acme",
            name = "Acme",
            selected = true,
            isDefault = true,
        )

        // When + Then
        // Branch: filter is FILTER_BY_PARTY_WITHOUT_NAME and attrs.name is null → true
        assertTrue(rpAction.predicate(attributes(partyName = null), withoutNameFilter))
        // Branch: filter is FILTER_BY_PARTY_WITHOUT_NAME and attrs.name is non-null → false
        assertTrue(!rpAction.predicate(attributes(partyName = "Acme"), withoutNameFilter))
        // Branch: filter is a name and attrs.name matches → true
        assertTrue(rpAction.predicate(attributes(partyName = "Acme"), acmeFilter))
        // Branch: filter is a name and attrs.name does not match → false
        assertTrue(!rpAction.predicate(attributes(partyName = "Other"), acmeFilter))
        // Branch: filter is a name and attrs.name is null → default false
        assertTrue(!rpAction.predicate(attributes(partyName = null), acmeFilter))
    }

    @Test
    fun `When the transaction-type filter predicate is applied, Then all four arms are evaluated`() {
        // Given
        mockGetFiltersStrings()
        val filters = interactor.getFilters()
        val typeGroup = filters.filterGroups.first { filterGroup ->
            filterGroup.id == TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_GROUP_ID
        }

        @Suppress("UNCHECKED_CAST")
        val typeAction =
            (typeGroup as FilterGroup.MultipleSelectionFilterGroup<TransactionsFilterableAttributes>)
                .filterableAction
        val presentationFilter = FilterItem(
            id = TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_PRESENTATION,
            name = "Presentation",
            selected = true,
            isDefault = true,
        )
        val issuanceFilter = FilterItem(
            id = TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_ISSUANCE,
            name = "Issuance",
            selected = true,
            isDefault = true,
        )
        val signingFilter = FilterItem(
            id = TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_SIGNING,
            name = "Signing",
            selected = true,
            isDefault = true,
        )
        val otherFilter = FilterItem(
            id = "other",
            name = "Other",
            selected = true,
            isDefault = false,
        )

        // When + Then — also exercise the `==` false outcomes by mismatching the filter's
        // expected type against the attribute's type so each arm sees the equality return false.
        assertTrue(
            typeAction.predicate(
                attributes(type = TransactionTypeUi.PRESENTATION),
                presentationFilter
            )
        )
        assertTrue(
            !typeAction.predicate(
                attributes(type = TransactionTypeUi.ISSUANCE),
                presentationFilter
            )
        )
        assertTrue(
            typeAction.predicate(
                attributes(type = TransactionTypeUi.ISSUANCE),
                issuanceFilter
            )
        )
        assertTrue(
            !typeAction.predicate(
                attributes(type = TransactionTypeUi.PRESENTATION),
                issuanceFilter
            )
        )
        assertTrue(
            typeAction.predicate(
                attributes(type = TransactionTypeUi.SIGNING),
                signingFilter
            )
        )
        assertTrue(
            !typeAction.predicate(
                attributes(type = TransactionTypeUi.PRESENTATION),
                signingFilter
            )
        )
        assertTrue(!typeAction.predicate(attributes(type = TransactionTypeUi.SIGNING), otherFilter))
    }
    //endregion

    //region onFilterStateChange non-TransactionUi payload
    // Covers the `payload as? TransactionUi` null branch where a FilterableItem's payload is
    // not a TransactionUi, so mapNotNull filters it out.
    @Test
    fun `Given a FilterApplyResult that contains a non-TransactionUi payload, When onFilterStateChange emits it, Then that item is filtered out`() {
        coroutineRule.runTest {
            // Given
            val transactionItem = filterableTransactionItem()
            val foreignItem = FilterableItem(
                payload = ForeignPayload,
                attributes = object : FilterableAttributes {
                    override val searchTags: List<String> = emptyList()
                },
            )
            whenever(filterValidator.onFilterStateChange()).thenReturn(
                FilterValidatorPartialState.FilterListResult.FilterApplyResult(
                    filteredList = FilterableList(items = listOf(transactionItem, foreignItem)),
                    allDefaultFiltersAreSelected = true,
                    updatedFilters = Filters.emptyFilters(),
                ).toFlow()
            )

            // When
            interactor.onFilterStateChange().runFlowTest {
                // Then
                val state = awaitItem()
                assertTrue(state is TransactionInteractorFilterPartialState.FilterApplyResult)
                state as TransactionInteractorFilterPartialState.FilterApplyResult

                val totalItems = state.transactions.sumOf { it.second.size }
                assertEquals(1, totalItems)
            }
        }
    }
    //endregion

    //region helper functions
    private fun createInteractorWithRealFilters(scope: CoroutineScope): TransactionsInteractor {
        mockTransactionRowStrings()
        mockGetFiltersStrings()
        mockPartyFilterNameCall()
        return TransactionsInteractorImpl(
            resourceProvider = resourceProvider,
            filterValidator = FilterValidatorImpl(
                scope = scope,
                sharingStarted = SharingStarted.Eagerly
            ),
            walletCoreTransactionLogController = walletCoreTransactionLogController,
        )
    }

    private fun TransactionInteractorFilterPartialState.FilterApplyResult.transactionIds(): List<String> {
        return transactions.flatMap { (_, items) -> items }
            .map { transaction -> transaction.uiData.header.itemId }
    }

    private suspend fun mockGetTransactionLogsCall(response: List<TransactionLogDomain>) {
        whenever(walletCoreTransactionLogController.getTransactionLogs()).thenReturn(response)
    }

    private fun mockPartyFilterNameCall() {
        whenever(resourceProvider.getString(R.string.transactions_filter_item_no_relying_party_transactions))
            .thenReturn(mockedNoPartyFilterName)
    }

    private fun mockGetFiltersStrings() {
        whenever(resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_reissuance))
            .thenReturn("Re-issuance")
        whenever(resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_deletion))
            .thenReturn(mockedCredentialDeletionTypeLabel)

        whenever(resourceProvider.getString(R.string.transactions_screen_filters_sort_by))
            .thenReturn("Sort by")
        whenever(resourceProvider.getString(R.string.transactions_screen_filters_sort_transaction_date))
            .thenReturn("Transaction date")
        whenever(resourceProvider.getString(R.string.transactions_screen_filter_by_date_period))
            .thenReturn("Date period")
        whenever(resourceProvider.getString(R.string.transactions_screen_filter_by_status))
            .thenReturn("Status")
        whenever(resourceProvider.getString(R.string.transactions_filter_item_status_completed))
            .thenReturn("Completed")
        whenever(resourceProvider.getString(R.string.transactions_filter_item_status_not_completed))
            .thenReturn("Not completed")
        whenever(resourceProvider.getString(R.string.transactions_screen_filters_filter_by_relying_party))
            .thenReturn("Relying Party")
        whenever(resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type))
            .thenReturn("Transaction type")
        whenever(resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_presentation))
            .thenReturn("Presentation")
        whenever(resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_issuance))
            .thenReturn("Issuance")
        whenever(resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_signing))
            .thenReturn("Signing")
    }

    private fun mockTransactionRowStrings() {
        whenever(resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_reissuance))
            .thenReturn("Re-issuance")
        whenever(resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_deletion))
            .thenReturn(mockedCredentialDeletionTypeLabel)

        whenever(resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_presentation))
            .thenReturn("Presentation")
        whenever(resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_issuance))
            .thenReturn("Issuance")
        whenever(resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_signing))
            .thenReturn("Signing")
        whenever(resourceProvider.getString(R.string.transactions_filter_item_status_completed))
            .thenReturn("Completed")
        whenever(resourceProvider.getString(R.string.transactions_filter_item_status_not_completed))
            .thenReturn("Not completed")
        whenever(resourceProvider.getString(R.string.transactions_screen_0_minutes_ago_message))
            .thenReturn("Just now")
        whenever(
            resourceProvider.getQuantityString(
                eq(R.plurals.transactions_screen_some_minutes_ago_message),
                org.mockito.kotlin.any(),
                org.mockito.kotlin.any()
            )
        ).thenReturn("30 min ago")
    }

    private fun filterableItemWithParty(name: String?): FilterableItem {
        val attributes = TransactionsFilterableAttributes(
            searchTags = emptyList(),
            transactionStatus = TransactionStatusUi.Completed,
            transactionType = TransactionTypeUi.PRESENTATION,
            creationLocalDateTime = LocalDateTime.now(),
            partyName = name,
        )
        return FilterableItem(
            payload = stubTransactionUi(),
            attributes = attributes,
        )
    }

    private fun stubTransactionUi(): TransactionUi {
        return TransactionUi(
            uiData = ExpandableListItemUi.SingleListItem(
                header = ListItemDataUi(
                    itemId = "id",
                    mainContentData = ListItemMainContentDataUi.Text("Tx"),
                )
            ),
            uiStatus = TransactionStatusUi.Completed,
            transactionCategoryUi = TransactionCategoryUi.Today,
        )
    }

    private fun filterableTransactionItem(): FilterableItem {
        return FilterableItem(
            payload = stubTransactionUi(),
            attributes = TransactionsFilterableAttributes(
                searchTags = emptyList(),
                transactionStatus = TransactionStatusUi.Completed,
                transactionType = TransactionTypeUi.PRESENTATION,
                creationLocalDateTime = LocalDateTime.now(),
                partyName = mockedPartyName,
            ),
        )
    }

    private fun filtersWithEachGroupType(): Filters {
        return Filters(
            filterGroups = listOf(
                FilterGroup.SingleSelectionFilterGroup(
                    id = "single",
                    name = "Single",
                    filters = listOf(filterItem("s1")),
                ),
                FilterGroup.ReversibleSingleSelectionFilterGroup(
                    id = "single_rev",
                    name = "Single Rev",
                    filters = listOf(filterItem("sr1")),
                ),
                FilterGroup.MultipleSelectionFilterGroup<TransactionsFilterableAttributes>(
                    id = "multi",
                    name = "Multi",
                    filters = listOf(filterItem("m1")),
                    filterableAction = FilterMultipleAction { _, _ -> true },
                ),
                FilterGroup.ReversibleMultipleSelectionFilterGroup<TransactionsFilterableAttributes>(
                    id = "multi_rev",
                    name = "Multi Rev",
                    filters = listOf(filterItem("mr1")),
                    filterableAction = FilterMultipleAction { _, _ -> true },
                ),
            ),
            sortOrder = SortOrder.Descending(isDefault = true),
        )
    }

    private fun filterItem(id: String): FilterElement = FilterItem(
        id = id,
        name = id,
        selected = true,
        isDefault = true,
    )
    //endregion

    //region DateTimeCategoryPartialState sealed arms (data-class API surface)
    // The Today/WithinLastHour/etc. arms are produced from clock-driven branches inside
    // getTransactions; exercise the data-class methods directly for stable coverage.
    @Test
    fun `Given Today partial-state data class, When instantiated, Then equals_hashCode_copy work`() {
        val a = TransactionInteractorDateTimeCategoryPartialState.Today(time = "10:30")
        val b = a.copy(time = "10:30")
        val c = TransactionInteractorDateTimeCategoryPartialState.Today(time = "11:30")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertEquals("10:30", a.time)
        kotlin.test.assertNotEquals(a, c)
    }
    //endregion

    //region mocked objects
    private val mockedPartyName = "Mocked Party"
    private val mockedNoPartyFilterName = "No Relying Party"
    private val mockedListTransactionLogDomains = listOf(
        mockedPresentationLogDomain,
        mockedIssuanceLogDomain,
        mockedReissuanceLogDomain,
        mockedDeletionLogDomain,
        mockedSigningLogDomain,
    )
    private val mockedListTransactionTypeLabels = listOf(
        "Presentation", "Issuance", "Re-issuance", mockedCredentialDeletionTypeLabel, "Signing",
    )
    private val mockedListTransactionTypeFilterIds = listOf(
        TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_PRESENTATION,
        TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_ISSUANCE,
        TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_REISSUANCE,
        TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_DELETION,
        TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_SIGNING,
    )
    private val mockedListTransactionTitles = listOf(
        mockedTransactionPartyName,
        mockedTransactionIssuerName,
        mockedTransactionIssuerName,
        mockedTransactionIssuerName,
        mockedTransactionServiceName,
    )
    private val mockedListTransactionPartyNames = listOf(
        mockedTransactionPartyName,
        mockedTransactionIssuerName,
        mockedTransactionIssuerName,
        mockedTransactionIssuerName,
        mockedTransactionServiceName,
    )
    private val mockedListNotCompletedTransactionIds = listOf(
        "incomplete-presentation", "incomplete-issuance", "incomplete-reissuance",
        "incomplete-deletion", "incomplete-signing",
    )

    private fun attributes(
        status: TransactionStatusUi = TransactionStatusUi.Completed,
        type: TransactionTypeUi = TransactionTypeUi.PRESENTATION,
        creationLocalDateTime: LocalDateTime? = LocalDateTime.now(),
        partyName: String? = null,
    ): TransactionsFilterableAttributes = TransactionsFilterableAttributes(
        searchTags = emptyList(),
        transactionStatus = status,
        transactionType = type,
        creationLocalDateTime = creationLocalDateTime,
        partyName = partyName,
    )

    private object ForeignPayload : FilterableItemPayload
    //endregion
}