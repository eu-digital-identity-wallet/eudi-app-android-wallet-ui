/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
