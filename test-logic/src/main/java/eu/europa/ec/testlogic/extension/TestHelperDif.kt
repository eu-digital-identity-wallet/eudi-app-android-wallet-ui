/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
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