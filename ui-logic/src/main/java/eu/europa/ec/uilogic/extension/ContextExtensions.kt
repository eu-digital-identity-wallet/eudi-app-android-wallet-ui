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
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import eu.europa.ec.uilogic.container.EudiComponentActivity

fun Context.getPendingUri(): Uri? {
    return (this as? EudiComponentActivity)?.getCachedIntent()?.data.let { uri ->
        clearPendingIntent()
        uri
    }
}

fun Context.getPendingIntent(): Intent? {
    return (this as? EudiComponentActivity)?.getCachedIntent()?.let { intent ->
        clearPendingIntent()
        intent
    }
}

fun Context.cacheUri(uri: Uri) {
    val intent = Intent().apply {
        data = uri
    }
    (this as? EudiComponentActivity)?.cacheIntent(intent)
}

fun Context.finish() {
    (this as? EudiComponentActivity)?.finish()
}

fun Context.findActivity(): ComponentActivity {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    throw IllegalStateException("No Activity found.")
}

/** The activity this context belongs to, or null when there is none, as in a preview. */
internal fun Context.findActivityOrNull(): Activity? =
    generateSequence(this) { context -> (context as? ContextWrapper)?.baseContext }
        .filterIsInstance<Activity>()
        .firstOrNull()

private fun Context.clearPendingIntent() {
    (this as? EudiComponentActivity)?.cacheIntent(null)
}

/**
 * Opens [uri] as a link from a web page would be opened: with the app Android picks for it, normally
 * the browser. The scheme is normalised to lower case first, since Android matches schemes
 * case-sensitively. Does nothing when that app would be the wallet itself, so that the link cannot
 * come back into the wallet's own link handling, or when no app can open it.
 */
fun Context.openInBrowser(uri: Uri) {
    tryOpenInBrowser(uri)
}

/** Opens [uri] as [openInBrowser] does, and returns whether an app was started for it. */
fun Context.tryOpenInBrowser(uri: Uri): Boolean {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndNormalize(uri)
        .addCategory(Intent.CATEGORY_BROWSABLE)
    return try {
        val handler = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }?.activityInfo?.packageName
        if (handler == packageName) return false
        startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}

/**
 * Opens a link or contact that a relying party or issuer supplied. A web address opens as
 * [openInBrowser] opens it, so that it cannot come back into the wallet; a `mailto:` or `tel:`
 * address opens with the app Android picks for it. Any other scheme is ignored.
 */
fun Context.openExternalUri(uri: Uri) {
    when (uri.scheme?.lowercase()) {
        "http", "https" -> openInBrowser(uri)
        "mailto", "tel" -> openUrl(uri)
    }
}

/**
 * Parses a string url and sends the Action View Intent.
 *
 * @param uri the url to parse.
 */
fun Context.openUrl(uri: Uri) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: Exception) {
    }
}

fun Context.tryOpenUrl(uri: Uri): Boolean {
    return try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", packageName, null)
    intent.data = uri
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

fun Context.openBleSettings() {
    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

fun Context.openIntentChooser(intent: Intent, title: String? = null) {
    try {
        startActivity(Intent.createChooser(intent, title))
    } catch (_: Exception) {
    }
}