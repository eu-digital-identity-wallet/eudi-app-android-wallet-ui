import eu.europa.ec.businesslogic.validator.FilterValidator
import eu.europa.ec.businesslogic.validator.FilterValidatorImpl
import eu.europa.ec.businesslogic.validator.FilterValidatorPartialState
import eu.europa.ec.businesslogic.validator.sampleFilterableList
import eu.europa.ec.businesslogic.validator.sampleFilters
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class TestFilterValidator {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private val testDispatcher = StandardTestDispatcher(coroutineRule.testScope.testScheduler)
    private val testScope = CoroutineScope(testDispatcher)

    private val filterValidator: FilterValidator = FilterValidatorImpl(
        scope = testScope,
        sharingStarted = SharingStarted.Eagerly
    )

    // Case 1:
    // 1. filterValidator.updateFilter emits:
    // FilterValidatorPartialState.FilterUpdateResult with updated filters.
    //
    // Case 1 Expected Result:
    // FilterUpdateResult state should be emitted with non-empty filters.

    @Test
    fun `Given Case 1, When updateFilter is called, Then Case 1 Expected Result is returned`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given: A filter group and filter ID to update
                val filterGroupId = "group1"
                val filterId = "filter1"

                // When: Update filter is triggered
                filterValidator.initializeValidator(sampleFilters, sampleFilterableList)
                filterValidator.updateFilter(filterGroupId, filterId)

                // Then: FilterUpdateResult is emitted with updated filters
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterUpdateResult)
                assertFalse(emittedState.updatedFilters.isEmpty)
            }
        }

    // Case 2:
    // 1. filterValidator.applyFilters emits:
    // FilterValidatorPartialState.FilterListResult.FilterListEmptyResult when no items match.
    //
    // Case 2 Expected Result:
    // FilterListEmptyResult state should be emitted with hasMoreThanDefaultFilters = false.

    @Test
    fun `Given Case 2, When applyFilters is called, Then Case 2 Expected Result is returned`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given: No items match filters
                filterValidator.applyFilters()

                // When: Filters are applied

                // Then: Empty result state is emitted
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterListResult.FilterListEmptyResult)
                emittedState as FilterValidatorPartialState.FilterListResult.FilterListEmptyResult
                assertFalse(emittedState.hasMoreThanDefaultFilters)
            }
        }

    // Case 3:
    // 1. filterValidator.applyFilters emits:
    // FilterValidatorPartialState.FilterListResult.FilterApplyResult when items match.
    //
    // Case 3 Expected Result:
    // FilterApplyResult state should be emitted with a non-empty list.

    @Test
    fun `Given Case 3, When applyFilters is called, Then Case 3 Expected Result is returned`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given: A list with items matching filters
                filterValidator.initializeValidator(sampleFilters, sampleFilterableList)

                // When: Filters are applied
                filterValidator.applyFilters()

                // Then: A valid filtered list is emitted
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterListResult.FilterApplyResult)
                assertFalse(emittedState.updatedFilters.isEmpty)
                assertTrue((emittedState as FilterValidatorPartialState.FilterListResult.FilterApplyResult).filteredList.items.isNotEmpty())
            }
        }

    // Case 4:
    // 1. filterValidator.resetFilters emits:
    // FilterValidatorPartialState.FilterListResult.FilterListEmptyResult with default filters.
    //
    // Case 4 Expected Result:
    // Filters should be reset and FilterListEmptyResult should be emitted.

    @Test
    fun `Given Case 4, When resetFilters is called, Then Case 4 Expected Result is returned`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given: Filters have been modified
                filterValidator.resetFilters()

                // Then: Default filters are restored
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterListResult.FilterListEmptyResult)
                emittedState as FilterValidatorPartialState.FilterListResult.FilterListEmptyResult
                assertFalse(emittedState.hasMoreThanDefaultFilters)
            }
        }

    // Case 5:
    // 1. filterValidator.revertFilters emits:
    // FilterValidatorPartialState.FilterUpdateResult with previous applied filters.
    //
    // Case 5 Expected Result:
    // Filters should be reverted and previous state should be emitted.

    @Test
    fun `Given Case 5, When revertFilters is called, Then Case 5 Expected Result is returned`() =
        coroutineRule.runTest {
            filterValidator.onFilterStateChange().runFlowTest {
                // Given: Filters were previously applied
                filterValidator.revertFilters()

                // Then: Previous filters are restored
                val emittedState = awaitItem()
                assertTrue(emittedState is FilterValidatorPartialState.FilterUpdateResult)
                assertEquals(sampleFilters, emittedState.updatedFilters)
            }
        }
}

