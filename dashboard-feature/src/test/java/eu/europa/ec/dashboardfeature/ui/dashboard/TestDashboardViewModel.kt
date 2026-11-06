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

package eu.europa.ec.dashboardfeature.ui.dashboard

import android.content.Intent
import androidx.lifecycle.ViewModelStore
import eu.europa.ec.dashboardfeature.interactor.DashboardInteractor
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TestDashboardViewModel {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var interactor: DashboardInteractor

    @Mock
    private lateinit var uiSerializer: UiSerializer

    @Mock
    private lateinit var resources: ResourceProvider

    private val store = ViewModelStore()

    private lateinit var viewModel: DashboardViewModel

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
        whenever(resources.getString(any())).thenReturn("text")
        viewModel = DashboardViewModel(
            dashboardInteractor = interactor,
            uiSerializer = uiSerializer,
            resourceProvider = resources,
        )
        store.put("dashboard", viewModel)
    }

    @After
    fun after() {
        try {
            store.clear()
            coroutineRule.testScope.runCurrent()
        } finally {
            closeable.close()
        }
    }

    @Test
    fun `Given the dashboard, When Init is handled, Then it schedules the background work`() {
        viewModel.handleEvents(Event.Init(intent = null))

        verify(interactor, times(1)).scheduleBackgroundWork()
    }

    @Test
    fun `Given the dashboard opened by a link, When Init is handled, Then it schedules the background work`() {
        viewModel.handleEvents(Event.Init(intent = mock<Intent>()))

        verify(interactor, times(1)).scheduleBackgroundWork()
    }

    @Test
    fun `Given the dashboard, When Pop is handled, Then no background work is scheduled`() {
        viewModel.handleEvents(Event.Pop)

        verify(interactor, never()).scheduleBackgroundWork()
    }
}