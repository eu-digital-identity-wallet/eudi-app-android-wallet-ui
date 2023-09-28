package eu.europa.ec.testlogic.extension

import app.cash.turbine.ReceiveTurbine
import junit.framework.TestCase

/**
 *
 * Helper function to test the difference between states
 *
 * You must provide either the previousState or number of statesToSkip
 *
 * @param startingState the state you want to start with
 * @param statesToSkip how many states to skip. It calls ReceiveTurbine awaitItem() under the hood
 * @param reduceState the first dif to calculate
 *
 */

suspend fun <T> ReceiveTurbine<T>.defInit(
    startingState: T? = null,
    statesToSkip: Int? = null,
    reduceState: T.() -> T
): Pair<T, suspend () -> T> {
    val expectedItem =
        when (startingState) {
            null -> {
                statesToSkip?.let { skipItems(statesToSkip - 1) }
                awaitItem()
            }

            else -> startingState
        }.reduceState()

    val actualState = awaitItem()
    TestCase.assertEquals(
        expectedItem,
        actualState
    )

    return Pair(actualState) { awaitItem() }
}

/**
 * After you call defInit then you can assert only the expected diffs between states
 * e.x defInit(){
 *    firstExpectedState
 * }.dif{
 *    secondExpectedState
 *  }.dif{
 *    (N)expectedState
 *  }
 */
suspend fun <T> Pair<T, suspend () -> T>.dif(reduce: T.() -> T): Pair<T, suspend () -> T> {
    val awaitItem = this.second
    val newStateWithDiff = this.first.reduce()
    TestCase.assertEquals(
        newStateWithDiff,
        awaitItem()
    )
    return Pair(newStateWithDiff, this.second)
}