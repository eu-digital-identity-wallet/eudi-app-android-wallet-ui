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

package eu.europa.ec.uilogic.mvi

import org.junit.Assert.assertEquals
import org.junit.Test

class TestMviViewModel {

    private data object TestEvent : ViewEvent
    private data object TestState : ViewState
    private data object TestEffect : ViewSideEffect

    private class TestViewModel : MviViewModel<TestEvent, TestState, TestEffect>() {
        override fun setInitialState(): TestState = TestState
        override fun handleEvents(event: TestEvent) = Unit
    }

    @Test
    fun `Given the same key, When run repeatedly on one instance, Then the block runs once`() {
        val viewModel = TestViewModel()
        var runs = 0

        repeat(5) { viewModel.runOncePerInstance { runs++ } }

        assertEquals(1, runs)
    }

    @Test
    fun `Given a new instance, When run, Then the block runs again`() {
        var runs = 0

        TestViewModel().runOncePerInstance { runs++ }
        assertEquals(1, runs)

        TestViewModel().runOncePerInstance { runs++ }
        assertEquals(2, runs)
    }

    @Test
    fun `Given distinct keys, When run on one instance, Then each runs once`() {
        val viewModel = TestViewModel()
        val ran = mutableListOf<String>()

        repeat(3) {
            viewModel.runOncePerInstance("a") { ran += "a" }
            viewModel.runOncePerInstance("b") { ran += "b" }
        }

        assertEquals(listOf("a", "b"), ran)
    }

    @Test
    fun `Given the default key, When run, Then it is the documented init key`() {
        val viewModel = TestViewModel()
        var runs = 0

        viewModel.runOncePerInstance { runs++ }
        viewModel.runOncePerInstance(MviViewModel.ONE_TIME_INIT_KEY) { runs++ }

        assertEquals(1, runs)
    }
}