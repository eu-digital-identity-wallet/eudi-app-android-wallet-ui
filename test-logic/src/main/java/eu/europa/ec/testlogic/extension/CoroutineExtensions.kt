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