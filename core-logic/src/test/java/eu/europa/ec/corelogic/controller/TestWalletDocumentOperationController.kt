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

package eu.europa.ec.corelogic.controller

import eu.europa.ec.authenticationlogic.controller.session.SessionController
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class TestWalletDocumentOperationController {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var sessionController: SessionController

    private lateinit var controller: WalletDocumentOperationController

    private lateinit var closeable: AutoCloseable

    private var reachedCompletingState = false

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        reachedCompletingState = false
        controller = WalletDocumentOperationControllerImpl(
            sessionController = sessionController,
            resetStatus = WalletResetStatus(),
            coroutineScope = coroutineRule.testScope.backgroundScope,
        )
    }

    @After
    fun after() {
        closeable.close()
    }

    /**
     * 1. An abandonable operation emits an intermediate state and then waits for its result.
     * 2. Its collector stops after the intermediate state.
     *
     * Expected result:
     * No operation stays pending, and the operation still runs to its completing state.
     */
    @Test
    fun `Given an abandonable operation, When its collector leaves early, Then it is no longer waited for`() =
        coroutineRule.runTest {
            // Given
            val tracked = controller.trackedOperationFlow(
                completeWhen = { state -> state == COMPLETED },
                isResetOperation = false,
                isAbandonable = { true },
                flowToCollect = { operationFlow() },
            )

            // When
            val firstState = tracked.first()
            runCurrent()
            val pendingAfterLeaving = controller.state.value.pendingOperations
            advanceTimeBy(OPERATION_DURATION)
            runCurrent()

            // Then
            assertEquals(STARTED, firstState)
            assertEquals(0, pendingAfterLeaving)
            assertTrue(reachedCompletingState)
            assertEquals(0, controller.state.value.pendingOperations)
        }

    /**
     * 1. An operation that is not abandonable emits an intermediate state and then waits.
     * 2. Its collector stops after the intermediate state.
     *
     * Expected result:
     * The operation keeps running and stays pending until its flow reaches the completing state.
     */
    @Test
    fun `Given a kept operation, When its collector leaves early, Then it runs to completion`() =
        coroutineRule.runTest {
            // Given
            val tracked = controller.trackedOperationFlow(
                completeWhen = { state -> state == COMPLETED },
                isResetOperation = false,
                isAbandonable = { false },
                flowToCollect = { operationFlow() },
            )

            // When
            val firstState = tracked.first()
            runCurrent()
            val pendingWhileRunning = controller.state.value.pendingOperations
            advanceTimeBy(OPERATION_DURATION)
            runCurrent()

            // Then
            assertEquals(STARTED, firstState)
            assertEquals(1, pendingWhileRunning)
            assertTrue(reachedCompletingState)
            assertEquals(0, controller.state.value.pendingOperations)
        }

    /** Emits [STARTED], then [COMPLETED] after [OPERATION_DURATION] of virtual time. */
    private fun operationFlow(): Flow<String> = flow {
        emit(STARTED)
        delay(OPERATION_DURATION)
        reachedCompletingState = true
        emit(COMPLETED)
    }

    private fun runCurrent() = coroutineRule.testScope.testScheduler.runCurrent()

    private fun advanceTimeBy(duration: Duration) =
        coroutineRule.testScope.testScheduler.advanceTimeBy(duration)

    private companion object {
        const val STARTED = "started"
        const val COMPLETED = "completed"
        val OPERATION_DURATION = 10.seconds
    }
}
