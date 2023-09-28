package eu.europa.ec.testlogic.rule

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.rules.TestWatcher

class CoroutineTestRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
    val testScope: TestScope = TestScope(testDispatcher)
) : TestWatcher()