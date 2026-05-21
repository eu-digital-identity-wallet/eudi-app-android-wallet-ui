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

import eu.europa.ec.businesslogic.validator.FilterValidator
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
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.model.TransactionLogDataDomain
import eu.europa.ec.dashboardfeature.ui.transactions.list.model.TransactionCategoryUi
import eu.europa.ec.dashboardfeature.ui.transactions.list.model.TransactionFilterIds
import eu.europa.ec.dashboardfeature.ui.transactions.list.model.TransactionUi
import eu.europa.ec.dashboardfeature.ui.transactions.list.model.TransactionsFilterableAttributes
import eu.europa.ec.dashboardfeature.ui.transactions.model.TransactionStatusUi
import eu.europa.ec.dashboardfeature.ui.transactions.model.TransactionTypeUi
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.PresentedDocument
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.mockedDefaultLocale
import eu.europa.ec.testfeature.util.mockedExceptionWithMessage
import eu.europa.ec.testfeature.util.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.util.mockedGenericErrorMessage
import eu.europa.ec.testfeature.util.mockedMdocPidFormat
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
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
    private lateinit var walletCoreDocumentsController: WalletCoreDocumentsController

    private lateinit var interactor: TransactionsInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = TransactionsInteractorImpl(
            resourceProvider = resourceProvider,
            filterValidator = filterValidator,
            walletCoreDocumentsController = walletCoreDocumentsController,
        )

        whenever(resourceProvider.genericErrorMessage()).thenReturn(mockedGenericErrorMessage)
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
    @Test
    fun `When applySearch is called, Then filterValidator#applySearch is invoked with the same query`() {
        // Given
        val query = "search"

        // When
        interactor.applySearch(query = query)

        // Then
        verify(filterValidator, times(1)).applySearch(query)
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

    //region updateSortOrder
    @Test
    fun `When updateSortOrder is called, Then filterValidator#updateSortOrder is invoked with the same order`() {
        // Given
        val sortOrder = SortOrder.Ascending(isDefault = false)

        // When
        interactor.updateSortOrder(sortOrder = sortOrder)

        // Then
        verify(filterValidator, times(1)).updateSortOrder(sortOrder)
    }
    //endregion

    //region initializeFilters
    @Test
    fun `When initializeFilters is called, Then filterValidator#initializeValidator is invoked with some Filters and the source list`() {
        // Given
        whenever(resourceProvider.getString(R.string.transactions_filter_item_no_relying_party_transactions))
            .thenReturn(mockedNoRelyingPartyFilterName)
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
    // 1. walletCoreDocumentsController.getTransactionLogs returns a list with one of each
    //    TransactionLogDataDomain subtype (IssuanceLog, PresentationLog, SigningLog) and
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
                TransactionLogDataDomain.IssuanceLog(
                    id = "tx1",
                    name = "Issuance tx",
                    status = TransactionLog.Status.Completed,
                    creationLocalDateTime = justNow,
                    creationLocalDate = justNow.toLocalDate(),
                ),
                TransactionLogDataDomain.PresentationLog(
                    id = "tx2",
                    name = "Presentation tx",
                    status = TransactionLog.Status.Incomplete,
                    creationLocalDateTime = withinLastHour,
                    creationLocalDate = withinLastHour.toLocalDate(),
                    relyingParty = TransactionLog.RelyingParty(
                        name = mockedRelyingPartyName,
                        isVerified = true,
                        certificateChain = emptyList(),
                        readerAuth = null,
                    ),
                    documents = listOf(
                        PresentedDocument(
                            format = mockedMdocPidFormat,
                            metadata = IssuerMetadata(
                                documentConfigurationIdentifier = "config",
                                display = listOf(
                                    IssuerMetadata.Display(
                                        name = mockedDocumentDisplayName,
                                        locale = mockedDefaultLocale,
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
                            claims = emptyList(),
                        ),
                    ),
                ),
                TransactionLogDataDomain.SigningLog(
                    id = "tx3",
                    name = "Signing tx",
                    status = TransactionLog.Status.Completed,
                    creationLocalDateTime = today,
                    creationLocalDate = today.toLocalDate(),
                ),
                TransactionLogDataDomain.IssuanceLog(
                    id = "tx4",
                    name = "Issuance old",
                    status = TransactionLog.Status.Error,
                    creationLocalDateTime = twoMonthsAgo,
                    creationLocalDate = twoMonthsAgo.toLocalDate(),
                ),
            )
            whenever(walletCoreDocumentsController.getTransactionLogs()).thenReturn(transactions)
            whenever(resourceProvider.getLocale()).thenReturn(mockedDefaultLocale)
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
                assertEquals(mockedRelyingPartyName, presentationAttrs.relyingPartyName)
                assertEquals(TransactionTypeUi.PRESENTATION, presentationAttrs.transactionType)
                assertEquals(TransactionStatusUi.Failed, presentationAttrs.transactionStatus)

                val issuanceAttrs =
                    result.allTransactions.items[0].attributes as TransactionsFilterableAttributes
                assertNull(issuanceAttrs.relyingPartyName)

                val signingAttrs =
                    result.allTransactions.items[2].attributes as TransactionsFilterableAttributes
                assertNull(signingAttrs.relyingPartyName)
            }
        }
    }

    // Case 2:
    // 1. walletCoreDocumentsController.getTransactionLogs throws an exception with a message.

    // Case 2 Expected Result:
    // Failure with the thrown exception's localized message.
    @Test
    fun `Given Case 2, When getTransactions is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getTransactionLogs())
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
    // 1. walletCoreDocumentsController.getTransactionLogs throws an exception with no message.

    // Case 3 Expected Result:
    // Failure with the generic error message.
    @Test
    fun `Given Case 3, When getTransactions is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getTransactionLogs())
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
                TransactionFilterIds.FILTER_SORT_GROUP_ID,
                TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_GROUP_ID,
                TransactionFilterIds.FILTER_BY_STATUS_GROUP_ID,
                TransactionFilterIds.FILTER_BY_RELYING_PARTY_GROUP_ID,
                TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_GROUP_ID,
            ),
            groupIds
        )
        assertEquals(SortOrder.Descending(isDefault = true), result.sortOrder)
    }
    //endregion

    //region addDynamicFilters

    // Case 1:
    // 1. The relying-party filter group is present and the transactions list contains items
    //    with relying party names; the dynamic relying-party filters are populated, prefixed
    //    by the "no relying party" filter.
    @Test
    fun `Given Case 1, When addDynamicFilters is called, Then the relying-party group is populated with both the no-relying-party filter and one filter per distinct relying party`() {
        // Given
        whenever(resourceProvider.getString(R.string.transactions_filter_item_no_relying_party_transactions))
            .thenReturn(mockedNoRelyingPartyFilterName)
        val initialFilters = Filters(
            filterGroups = listOf(
                FilterGroup.MultipleSelectionFilterGroup<TransactionsFilterableAttributes>(
                    id = TransactionFilterIds.FILTER_BY_RELYING_PARTY_GROUP_ID,
                    name = "Relying party",
                    filters = emptyList(),
                    filterableAction = FilterMultipleAction { _, _ -> true },
                )
            ),
            sortOrder = SortOrder.Descending(isDefault = true),
        )
        val transactions = FilterableList(
            items = listOf(
                filterableItemWithRelyingParty(name = "Acme"),
                filterableItemWithRelyingParty(name = null),
                filterableItemWithRelyingParty(name = "Acme"), // duplicate to verify distinctBy
            )
        )

        // When
        val result = interactor.addDynamicFilters(transactions, initialFilters)

        // Then
        val updatedGroup =
            result.filterGroups.first { it.id == TransactionFilterIds.FILTER_BY_RELYING_PARTY_GROUP_ID }
        val ids = updatedGroup.filters.map { it.id }
        assertEquals(
            listOf(TransactionFilterIds.FILTER_BY_RELYING_PARTY_WITHOUT_NAME, "Acme"),
            ids
        )
    }

    // Case 2:
    // 1. A filter group whose id is not the relying-party group => left unchanged.
    @Test
    fun `Given Case 2, When addDynamicFilters is called with a non-relying-party group, Then the group is left unchanged`() {
        // Given
        val unrelatedGroup = FilterGroup.SingleSelectionFilterGroup(
            id = TransactionFilterIds.FILTER_SORT_GROUP_ID,
            name = "Sort",
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
        val sortGroup = filters.filterGroups.first {
            it.id == TransactionFilterIds.FILTER_SORT_GROUP_ID
        }

        @Suppress("UNCHECKED_CAST")
        val sortAction = sortGroup.filters.first().filterableAction
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
        val dateGroup = filters.filterGroups.first {
            it.id == TransactionFilterIds.FILTER_BY_TRANSACTION_DATE_GROUP_ID
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
        val statusGroup = filters.filterGroups.first {
            it.id == TransactionFilterIds.FILTER_BY_STATUS_GROUP_ID
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
        val failedFilter = FilterItem(
            id = TransactionFilterIds.FILTER_BY_STATUS_FAILED,
            name = "Failed",
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
                attributes(status = TransactionStatusUi.Failed),
                completedFilter
            )
        )
        assertTrue(
            statusAction.predicate(
                attributes(status = TransactionStatusUi.Failed),
                failedFilter
            )
        )
        assertTrue(
            !statusAction.predicate(
                attributes(status = TransactionStatusUi.Completed),
                failedFilter
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
    fun `When the relying-party filter predicate is applied, Then no-name-with-name-and-mismatch arms are all evaluated`() {
        // Given
        mockGetFiltersStrings()
        val filters = interactor.getFilters()
        val rpGroup = filters.filterGroups.first {
            it.id == TransactionFilterIds.FILTER_BY_RELYING_PARTY_GROUP_ID
        }

        @Suppress("UNCHECKED_CAST")
        val rpAction =
            (rpGroup as FilterGroup.MultipleSelectionFilterGroup<TransactionsFilterableAttributes>)
                .filterableAction
        val withoutNameFilter = FilterItem(
            id = TransactionFilterIds.FILTER_BY_RELYING_PARTY_WITHOUT_NAME,
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
        // Branch: filter is FILTER_BY_RELYING_PARTY_WITHOUT_NAME and attrs.name is null → true
        assertTrue(rpAction.predicate(attributes(relyingPartyName = null), withoutNameFilter))
        // Branch: filter is FILTER_BY_RELYING_PARTY_WITHOUT_NAME and attrs.name is non-null → false
        assertTrue(!rpAction.predicate(attributes(relyingPartyName = "Acme"), withoutNameFilter))
        // Branch: filter is a name and attrs.name matches → true
        assertTrue(rpAction.predicate(attributes(relyingPartyName = "Acme"), acmeFilter))
        // Branch: filter is a name and attrs.name does not match → false
        assertTrue(!rpAction.predicate(attributes(relyingPartyName = "Other"), acmeFilter))
        // Branch: filter is a name and attrs.name is null → default false
        assertTrue(!rpAction.predicate(attributes(relyingPartyName = null), acmeFilter))
    }

    @Test
    fun `When the transaction-type filter predicate is applied, Then all four arms are evaluated`() {
        // Given
        mockGetFiltersStrings()
        val filters = interactor.getFilters()
        val typeGroup = filters.filterGroups.first {
            it.id == TransactionFilterIds.FILTER_BY_TRANSACTION_TYPE_GROUP_ID
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
    private fun mockGetFiltersStrings() {
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
        whenever(resourceProvider.getString(R.string.transactions_filter_item_status_failed))
            .thenReturn("Failed")
        whenever(resourceProvider.getString(R.string.transactions_screen_filters_filter_by_relying_party))
            .thenReturn("Relying party")
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
        whenever(resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_presentation))
            .thenReturn("Presentation")
        whenever(resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_issuance))
            .thenReturn("Issuance")
        whenever(resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_signing))
            .thenReturn("Signing")
        whenever(resourceProvider.getString(R.string.transactions_filter_item_status_completed))
            .thenReturn("Completed")
        whenever(resourceProvider.getString(R.string.transactions_filter_item_status_failed))
            .thenReturn("Failed")
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

    private fun filterableItemWithRelyingParty(name: String?): FilterableItem {
        val attributes = TransactionsFilterableAttributes(
            searchTags = emptyList(),
            transactionStatus = TransactionStatusUi.Completed,
            transactionType = TransactionTypeUi.PRESENTATION,
            creationLocalDateTime = LocalDateTime.now(),
            relyingPartyName = name,
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
                relyingPartyName = mockedRelyingPartyName,
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
    private val mockedRelyingPartyName = "Mocked Relying Party"
    private val mockedNoRelyingPartyFilterName = "No relying party"
    private val mockedDocumentDisplayName = "Mocked Document Display"

    private fun attributes(
        status: TransactionStatusUi = TransactionStatusUi.Completed,
        type: TransactionTypeUi = TransactionTypeUi.PRESENTATION,
        creationLocalDateTime: LocalDateTime? = LocalDateTime.now(),
        relyingPartyName: String? = null,
    ): TransactionsFilterableAttributes = TransactionsFilterableAttributes(
        searchTags = emptyList(),
        transactionStatus = status,
        transactionType = type,
        creationLocalDateTime = creationLocalDateTime,
        relyingPartyName = relyingPartyName,
    )

    private object ForeignPayload : FilterableItemPayload
    //endregion
}
