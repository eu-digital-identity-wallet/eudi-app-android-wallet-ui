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

package eu.europa.ec.corelogic.extension

import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.corelogic.controller.IssueDocumentsPartialState
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class TestIssuanceExtensions {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private var resumedRequests = 0
    private var cancelledRequests = 0
    private val observer = Job()
    private val coreCancellation = CompletableDeferred<Unit>()
    private val authenticationRequest = IssueDocumentsPartialState.UserAuthRequired(
        crypto = BiometricCrypto(cryptoObject = null),
        resultHandler = DeviceAuthenticationResult(
            onAuthenticationSuccess = { resumedRequests++ },
            onAuthenticationError = {
                cancelledRequests++
                coreCancellation.complete(Unit)
            },
        ),
    )

    //region withAuthenticationObserver

    /**
     * Case 1:
     * 1. The observer has left before Core asks for a key unlock.
     * 2. Core waits for cancellation before it emits the terminal failure.
     *
     * Case 1 Expected Result:
     * The request is cancelled on arrival and the failure reaches the tracked flow.
     */
    @Test
    fun `Given the observer has left, When authentication arrives, Then Core is cancelled immediately`() =
        coroutineRule.runTest {
            // Given
            observer.cancel()
            val failure = IssueDocumentsPartialState.Failure(errorMessage = "Cancelled")
            val issuance = flow {
                emit(authenticationRequest)
                coreCancellation.await()
                emit(failure)
            }

            // When
            issuance.withAuthenticationObserver(observer).runFlowTest {
                // Then
                assertTrue(awaitItem() is IssueDocumentsPartialState.UserAuthRequired)
                assertSame(failure, awaitItem())
                awaitComplete()
                assertEquals(1, cancelledRequests)
                assertEquals(0, resumedRequests)
            }
        }

    /**
     * Case 2:
     * 1. A request has been forwarded to the prompt.
     * 2. The observer leaves before the prompt answers.
     *
     * Case 2 Expected Result:
     * Core receives one cancellation even if the prompt later reports success or error.
     */
    @Test
    fun `Given an unanswered prompt, When the observer leaves, Then late callbacks cannot answer Core again`() =
        coroutineRule.runTest {
            // Given
            val resultHandler = observeAuthenticationRequest()

            // When
            observer.cancel()
            resultHandler.onAuthenticationSuccess()
            resultHandler.onAuthenticationError()

            // Then
            assertEquals(1, cancelledRequests)
            assertEquals(0, resumedRequests)
        }

    /**
     * Case 3:
     * 1. The prompt succeeds and a second authentication request arrives.
     *
     * Case 3 Expected Result:
     * The first request is resumed once. On exit only the unanswered request is cancelled.
     */
    @Test
    fun `Given an answered request and a pending request, When the observer leaves, Then only the pending one is cancelled`() =
        coroutineRule.runTest {
            // Given
            val answeredRequest = observeAuthenticationRequest()
            answeredRequest.onAuthenticationSuccess()
            val pendingRequest = observeAuthenticationRequest()

            // When
            answeredRequest.onAuthenticationSuccess()
            answeredRequest.onAuthenticationError()
            observer.cancel()
            pendingRequest.onAuthenticationSuccess()

            // Then
            assertEquals(1, resumedRequests)
            assertEquals(1, cancelledRequests)
        }

    /**
     * Case 4:
     * 1. The prompt reports an authentication error before the observer leaves.
     *
     * Case 4 Expected Result:
     * The prompt cancels Core once and a late success cannot resume the request.
     */
    @Test
    fun `Given a prompt error, When the observer later leaves, Then the request is answered only once`() =
        coroutineRule.runTest {
            // Given
            val resultHandler = observeAuthenticationRequest()

            // When
            resultHandler.onAuthenticationError()
            observer.cancel()
            resultHandler.onAuthenticationSuccess()

            // Then
            assertEquals(1, cancelledRequests)
            assertEquals(0, resumedRequests)
        }

    /**
     * Case 5:
     * 1. Issuance completes without needing authentication after the observer leaves.
     *
     * Case 5 Expected Result:
     * The terminal state reaches the tracked operation.
     */
    @Test
    fun `Given an absent observer, When Issuance completes on its own, Then the terminal state passes through`() =
        coroutineRule.runTest {
            // Given
            observer.cancel()
            val success = IssueDocumentsPartialState.Success(documentIds = listOf("issued"))

            // When
            flowOf(success).withAuthenticationObserver(observer).runFlowTest {
                // Then
                assertSame(success, awaitItem())
                awaitComplete()
            }
        }

    //endregion

    //region helper functions

    private suspend fun observeAuthenticationRequest(): DeviceAuthenticationResult {
        val state = flowOf(authenticationRequest).withAuthenticationObserver(observer).single()
        assertTrue(state is IssueDocumentsPartialState.UserAuthRequired)
        return state.resultHandler
    }

    //endregion
}