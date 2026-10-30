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

import android.view.KeyboardShortcutGroup
import android.view.Menu
import android.view.MotionEvent
import android.view.Window

/** Observes touches without consuming them. Returns the call that restores the window callback. */
internal fun Window.reportTouchInteraction(onInteraction: () -> Unit): () -> Unit {
    val originalCallback = callback ?: return {}
    val reportingCallback = object : Window.Callback by originalCallback {
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            onInteraction()
            return originalCallback.dispatchTouchEvent(event)
        }

        // Kotlin delegation does not forward Java default methods.
        override fun onProvideKeyboardShortcuts(
            data: MutableList<KeyboardShortcutGroup>?,
            menu: Menu?,
            deviceId: Int,
        ) {
            originalCallback.onProvideKeyboardShortcuts(data, menu, deviceId)
        }

        override fun onPointerCaptureChanged(hasCapture: Boolean) {
            originalCallback.onPointerCaptureChanged(hasCapture)
        }
    }
    callback = reportingCallback
    return {
        if (callback === reportingCallback) {
            callback = originalCallback
        }
    }
}