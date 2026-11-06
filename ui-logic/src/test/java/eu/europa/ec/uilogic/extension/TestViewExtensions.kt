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

package eu.europa.ec.uilogic.extension

import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TestViewExtensions {

    private val context = RuntimeEnvironment.getApplication()
    private val window = FrameLayout(context)
    private val root = FrameLayout(context).also { window.addView(it) }
    private val child = View(context).also { root.addView(it) }

    private var interactions = 0

    private fun send(type: Int) {
        root.requestSendAccessibilityEvent(child, AccessibilityEvent(type))
    }

    @Test
    fun `Given the reporting, When a screen reader clicks, moves focus or scrolls, Then each counts as interaction`() {
        root.reportAccessibilityInteraction { interactions++ }

        send(AccessibilityEvent.TYPE_VIEW_CLICKED)
        send(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED)
        send(AccessibilityEvent.TYPE_VIEW_SCROLLED)

        assertEquals(3, interactions)
    }

    @Test
    fun `Given the reporting, When content changes on its own, Then it does not count as interaction`() {
        root.reportAccessibilityInteraction { interactions++ }

        send(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)

        assertEquals(0, interactions)
    }

    @Test
    fun `Given the reporting is removed, When a screen reader clicks, Then nothing is reported`() {
        val remove = root.reportAccessibilityInteraction { interactions++ }

        remove()
        send(AccessibilityEvent.TYPE_VIEW_CLICKED)

        assertEquals(0, interactions)
    }

    @Test
    fun `Given a view with its own delegate, When the reporting is installed, Then that delegate is kept`() {
        val own = View.AccessibilityDelegate()
        root.accessibilityDelegate = own

        root.reportAccessibilityInteraction { interactions++ }

        assertSame(own, root.accessibilityDelegate)
    }
}