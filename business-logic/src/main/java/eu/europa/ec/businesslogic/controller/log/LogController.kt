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

class LogControllerImpl(
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