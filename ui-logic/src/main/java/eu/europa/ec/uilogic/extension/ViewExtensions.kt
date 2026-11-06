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
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent

private val interactionEventTypes = setOf(
    AccessibilityEvent.TYPE_VIEW_CLICKED,
    AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
    AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED,
    AccessibilityEvent.TYPE_VIEW_SCROLLED,
    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
)

/**
 * Calls [onInteraction] for each accessibility event in this view's hierarchy that follows a user's
 * action: a click, a long click, moving accessibility focus, scrolling or editing text. A screen
 * reader such as TalkBack acts through accessibility actions, not touches, so
 * `Activity.onUserInteraction` alone would count its user as idle. Such events are sent only while an
 * accessibility service is on.
 *
 * Install it on a window's root view, which has no accessibility delegate of its own; a view that
 * already has one is left as it is. Returns the call that removes it.
 */
fun View.reportAccessibilityInteraction(onInteraction: () -> Unit): () -> Unit {
    if (accessibilityDelegate != null) return {}
    val delegate = object : View.AccessibilityDelegate() {
        override fun onRequestSendAccessibilityEvent(
            host: ViewGroup,
            child: View,
            event: AccessibilityEvent,
        ): Boolean {
            if (event.eventType in interactionEventTypes) {
                onInteraction()
            }
            return super.onRequestSendAccessibilityEvent(host, child, event)
        }
    }
    accessibilityDelegate = delegate
    return {
        if (accessibilityDelegate === delegate) {
            accessibilityDelegate = null
        }
    }
}