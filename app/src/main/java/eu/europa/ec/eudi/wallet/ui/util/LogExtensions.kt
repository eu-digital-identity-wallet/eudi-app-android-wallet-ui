/*
 * Copyright (c) 2023 European Commission
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

package eu.europa.ec.eudi.wallet.ui.util

import android.util.Log
import eu.europa.ec.eudi.wallet.ui.BuildConfig

/**
 * Extension function for a DEBUG or ERROR log message.
 *
 * @param message the message you would like logged
 * @param exception An exception to log (optional)
 */
fun Any.log(message: String, exception: Throwable? = null) {
    if (!BuildConfig.DEBUG) return
    val tag: String = tagValue()
    if (exception == null) {
        Log.d(tag, message)
    } else {
        Log.e(tag, message, exception)
    }
}

/**
 * Extension function for an INFO log message
 *
 * @param message the message you would like logged
 */
fun Any.logInfo(message: String) {
    if (!BuildConfig.DEBUG) return
    val tag: String = tagValue()
    Log.i(tag, message)
}

/**
 * Extension function for a WARNING log message
 *
 * @param message the message you would like logged.
 */
fun Any.logWarning(message: String) {
    if (!BuildConfig.DEBUG) return
    val tag: String = tagValue()
    Log.w(tag, message)
}

/**
 * Extension function for an ERROR log message.
 *
 * @param message the message you would like logged.
 */
fun Any.logError(message: String) {
    if (!BuildConfig.DEBUG) return
    val tag: String = tagValue()
    Log.e(tag, message)
}

private fun Any.tagValue(): String {
    if (this is String) return this
    val fullClassName: String = this::class.qualifiedName ?: this::class.java.typeName
    val outerClassName = fullClassName.substringBefore('$')
    val simplerOuterClassName = outerClassName.substringAfterLast('.')
    return if (simplerOuterClassName.isEmpty()) {
        fullClassName
    } else {
        simplerOuterClassName.removeSuffix("Kt")
    }
}
