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

package eu.europa.ec.businesslogic.controller.log

import eu.europa.ec.businesslogic.config.AppBuildType
import eu.europa.ec.businesslogic.config.ConfigLogic
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger
import logcat.asLog
import logcat.logcat

interface LogController {
    fun install()
    fun d(tag: String, message: () -> String)
    fun d(message: () -> String)
    fun e(tag: String, message: () -> String)
    fun e(tag: String, exception: Throwable)
    fun e(message: () -> String)
    fun w(tag: String, message: () -> String)
    fun w(message: () -> String)
}

class LogControllerImpl constructor(
    configLogic: ConfigLogic
) : LogController {

    private val flavorName = "EUDI Wallet"
    private val appBuildType = configLogic.appBuildType

    override fun install() {
        if (!LogcatLogger.isInstalled && AppBuildType.RELEASE != appBuildType) {
            LogcatLogger.install(AndroidLogcatLogger(LogPriority.VERBOSE))
        }
    }

    override fun d(tag: String, message: () -> String) {
        logcat(priority = LogPriority.DEBUG, tag = tag, message = message)
    }

    override fun d(message: () -> String) {
        d(tag = flavorName, message = message)
    }

    override fun e(tag: String, message: () -> String) {
        logcat(priority = LogPriority.ERROR, tag = tag, message = message)
    }

    override fun e(tag: String, exception: Throwable) {
        e(tag = tag, message = { exception.asLog() })
    }

    override fun e(message: () -> String) {
        e(tag = flavorName, message = message)
    }

    override fun w(tag: String, message: () -> String) {
        logcat(priority = LogPriority.WARN, tag = tag, message = message)
    }

    override fun w(message: () -> String) {
        w(tag = flavorName, message = message)
    }
}