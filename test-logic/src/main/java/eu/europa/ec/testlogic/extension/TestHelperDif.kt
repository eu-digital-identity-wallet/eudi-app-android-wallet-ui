/*
 * Copyright (c) 2023 European Commission
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