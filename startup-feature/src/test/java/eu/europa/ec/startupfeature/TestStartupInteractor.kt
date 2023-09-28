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

package eu.europa.ec.startupfeature

import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.startupfeature.interactor.StartupInteractor
import eu.europa.ec.startupfeature.interactor.StartupInteractorImpl
import eu.europa.ec.startupfeature.interactor.StartupInteractorPartialState
import eu.europa.ec.startupfeature.repository.StartupRepoPartialState
import eu.europa.ec.startupfeature.repository.StartupRepository
import eu.europa.ec.testfeature.mockedExceptionWithMessage
import eu.europa.ec.testfeature.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.mockedGenericErrorMessage
import eu.europa.ec.testfeature.plainFailureMessage
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class TestStartupInteractor {

    //region Test Setup
    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var startupRepository: StartupRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var startupInteractor: StartupInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        startupInteractor = StartupInteractorImpl(
            startupRepository = startupRepository,
            resourceProvider = resourceProvider
        )

        `when`(resourceProvider.genericErrorMessage())
            .thenReturn(mockedGenericErrorMessage)
    }

    @After
    fun after() {
        closeable.close()
    }
    //endregion

    //region test
    @Test
    fun testSuccess() {
        coroutineRule.runTest {
            // Given
            val repositoryResponse = StartupRepoPartialState
                .Success
            fakeStartupRepoTestResponse(repositoryResponse = repositoryResponse)

            // When
            startupInteractor.test()
                .runFlowTest {
                    // Then
                    assertEquals(
                        StartupInteractorPartialState.Success,
                        awaitItem()
                    )
                }
        }
    }

    @Test
    fun testFailureWithMessage() {
        coroutineRule.runTest {
            // Given
            val repositoryResponse = StartupRepoPartialState
                .Failure(error = plainFailureMessage)
            fakeStartupRepoTestResponse(repositoryResponse = repositoryResponse)

            // When
            startupInteractor.test()
                .runFlowTest {
                    // Then
                    assertEquals(
                        StartupInteractorPartialState
                            .Failure(error = plainFailureMessage),
                        awaitItem()
                    )
                }
        }
    }

    @Test
    fun testFailureWithNoMessage() {
        coroutineRule.runTest {
            // Given
            val repositoryResponse = StartupRepoPartialState
                .Failure(error = null)
            fakeStartupRepoTestResponse(repositoryResponse = repositoryResponse)

            // When
            startupInteractor.test()
                .runFlowTest {
                    // Then
                    assertEquals(
                        StartupInteractorPartialState
                            .Failure(error = mockedGenericErrorMessage),
                        awaitItem()
                    )
                }
        }
    }

    @Test
    fun testExceptionWithMessage() {
        coroutineRule.runTest {
            // Given
            `when`(
                startupRepository.test()
            ).thenThrow(mockedExceptionWithMessage)

            // When
            startupInteractor.test()
                .runFlowTest {
                    // Then
                    assertEquals(
                        StartupInteractorPartialState
                            .Failure(error = mockedExceptionWithMessage.localizedMessage!!),
                        awaitItem()
                    )
                }
        }
    }

    @Test
    fun testExceptionWithNoMessage() {
        coroutineRule.runTest {
            // Given
            `when`(
                startupRepository.test()
            ).thenThrow(mockedExceptionWithNoMessage)

            // When
            startupInteractor.test()
                .runFlowTest {
                    // Then
                    assertEquals(
                        StartupInteractorPartialState
                            .Failure(error = mockedGenericErrorMessage),
                        awaitItem()
                    )
                }
        }
    }
    //endregion

    //region Fake Api calls
    private fun fakeStartupRepoTestResponse(repositoryResponse: StartupRepoPartialState) {
        `when`(
            startupRepository.test()
        ).thenReturn(repositoryResponse.toFlow())
    }
    //endregion
}