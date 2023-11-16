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