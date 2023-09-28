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
import app.cash.turbine.test
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.milliseconds

fun CoroutineTestRule.runTest(block: suspend CoroutineScope.() -> Unit): Unit =
    testScope.runTest { block() }

suspend fun <T> Flow<T>.runFlowTest(
    timeOut: Long? = null,
    block: suspend ReceiveTurbine<T>.() -> Unit
) {
    test(timeOut?.milliseconds) {
        block()
        cancelAndConsumeRemainingEvents()
    }
}

suspend fun <T> Flow<T>.expectNoEvents() {
    test {
        expectNoEvents()
    }
}