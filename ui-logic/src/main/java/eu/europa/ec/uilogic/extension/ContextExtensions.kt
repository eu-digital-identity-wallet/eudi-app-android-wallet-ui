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

package eu.europa.ec.uilogic.extension

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import eu.europa.ec.uilogic.container.EudiComponentActivity

fun Context.openDeepLink(deepLink: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = deepLink
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}

fun Context.getPendingDeepLink(): Uri? {
    return (this as? EudiComponentActivity)?.pendingDeepLink?.let { deepLink ->
        clearPendingDeepLink()
        deepLink
    }
}

fun Context.finish() {
    (this as? EudiComponentActivity)?.finish()
}

private fun Context.clearPendingDeepLink() {
    (this as? EudiComponentActivity)?.pendingDeepLink = null
}