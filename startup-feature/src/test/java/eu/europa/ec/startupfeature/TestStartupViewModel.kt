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

import eu.europa.ec.startupfeature.interactor.StartupInteractor
import eu.europa.ec.startupfeature.interactor.StartupInteractorPartialState
import eu.europa.ec.startupfeature.ui.Event
import eu.europa.ec.startupfeature.ui.StartupViewModel
import eu.europa.ec.startupfeature.ui.State
import eu.europa.ec.testfeature.plainFailureMessage
import eu.europa.ec.testlogic.base.TestApplication
import eu.europa.ec.testlogic.extension.expectNoEvents
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = TestApplication::class)
class TestStartupViewModel {

    //region Test Setup
    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var interactor: StartupInteractor

    private lateinit var viewModel: StartupViewModel

    private lateinit var closeable: AutoCloseable

    private lateinit var initialState: State

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        viewModel = StartupViewModel(
            interactor = interactor
        )

        initialState = viewModel.viewState.value
    }

    @After
    fun after() {
        closeable.close()
    }
    //endregion

    //region setInitialState()
    @Test
    fun `Assert setInitialState() sets the expected initial state`() {
        coroutineRule.runTest {
            assertEquals(
                State,
                initialState
            )
            viewModel.effect.expectNoEvents()
        }
    }
    //endregion

    //region Event.OnClick

    // Case 1:
    // no Event was called before OnClick
    // (so State has its default value)
    // interactor.test() returns Success
    @Test
    fun `Given Case 1, When Event#OnClick, Then the correct States and Effects are emitted`() {
        coroutineRule.runTest {
            // Given
            fakeStartupInteractorTestResponse(
                interactorResponse = StartupInteractorPartialState
                    .Success
            )
            val eventOnClick = Event.OnClick

            // When
            viewModel.setEvent(eventOnClick)

            // Then
            viewModel.viewStateHistory.expectNoEvents()

            viewModel.effect.expectNoEvents()
        }
    }

    // Case 2:
    // no Event was called before OnClick
    // (so State has its default value)
    // interactor.test() returns Failure
    @Test
    fun `Given Case 2, When Event#OnClick, Then the correct States and Effects are emitted`() {
        coroutineRule.runTest {
            // Given
            fakeStartupInteractorTestResponse(
                interactorResponse = StartupInteractorPartialState
                    .Failure(
                        error = plainFailureMessage
                    )
            )
            val eventOnClick = Event.OnClick

            // When
            viewModel.setEvent(eventOnClick)

            // Then
            viewModel.viewStateHistory.expectNoEvents()

            viewModel.effect.expectNoEvents()
        }
    }
    //endregion

    //region Fake Interactor calls
    private fun fakeStartupInteractorTestResponse(interactorResponse: StartupInteractorPartialState) {
        `when`(
            interactor.test()
        ).thenReturn(interactorResponse.toFlow())
    }
    //endregion
}