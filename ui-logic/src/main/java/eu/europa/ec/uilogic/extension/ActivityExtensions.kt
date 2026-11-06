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

import android.app.Activity
import android.content.Intent
import android.view.WindowManager

/**
 * The package that started [this] activity, as Android recorded it. [Activity.getReferrer] returns
 * the sender's [Intent.EXTRA_REFERRER] or [Intent.EXTRA_REFERRER_NAME] first, which Chrome sets to
 * the web page and which a replayed intent keeps, so the launch intent is set aside while the
 * referrer is read and then put back.
 */
internal fun Activity.launchingPackage(): String? {
    val launch = intent
    intent = Intent()
    return try {
        referrer?.host
    } finally {
        intent = launch
    }
}

/**
 * Keeps [this] activity's window out of screenshots, screen recordings and the recent-apps preview
 * when [isPrevented] is true, with [WindowManager.LayoutParams.FLAG_SECURE]; dialogs and bottom
 * sheets shown over the window inherit it. When false the flag is cleared.
 *
 * @param isPrevented Whether screen capture is prevented, as
 * `ConfigUILogic.shouldPreventScreenCapture` sets it.
 */
internal fun Activity.setScreenCapturePrevented(isPrevented: Boolean) {
    if (isPrevented) {
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}