/*
 * Copyright (c) 2025 European Commission
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

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import eu.europa.ec.businesslogic.config.ConfigLogic
import fr.bipi.treessence.file.FileLoggerTree
import timber.log.Timber
import java.io.File

interface LogController {
    fun d(tag: String, message: () -> String)
    fun d(message: () -> String)
    fun e(tag: String, message: () -> String)
    fun e(tag: String, exception: Throwable)
    fun e(message: () -> String)
    fun e(exception: Throwable)
    fun w(tag: String, message: () -> String)
    fun w(message: () -> String)
    fun i(tag: String, message: () -> String)
    fun i(message: () -> String)
    fun retrieveLogFileUris(): List<Uri>
}

class LogControllerImpl(
    private val context: Context,
    configLogic: ConfigLogic
) : LogController {

    companion object {
        private const val LOG_FILE_NAME = "eudi-android-wallet-logs%g.txt"
        private const val FILE_SIZE_LIMIT = 5242880
        private const val FILE_LIMIT = 10
    }

    private val logsDir = File(context.filesDir.absolutePath + "/logs")

    private val fileLoggerTree: FileLoggerTree = FileLoggerTree.Builder()
        .withFileName(LOG_FILE_NAME)
        .withDir(logsDir)
        .withSizeLimit(FILE_SIZE_LIMIT)
        .withFileLimit(FILE_LIMIT)
        .withMinPriority(Log.DEBUG)
        .appendToFile(true)
        .build()

    init {
        Timber.plant(Timber.DebugTree(), fileLoggerTree)
    }

    private val tag: String = "EUDI Wallet ${configLogic.appFlavor}-${configLogic.appBuildType}"

    override fun d(tag: String, message: () -> String) {
        Timber.tag(tag).d(message())
    }

    override fun d(message: () -> String) {
        d(tag = tag, message = message)
    }

    override fun e(tag: String, message: () -> String) {
        Timber.tag(tag).e(message())
    }

    override fun e(tag: String, exception: Throwable) {
        Timber.tag(tag).e(exception.message.orEmpty())
    }

    override fun e(message: () -> String) {
        e(tag, message)
    }

    override fun e(exception: Throwable) {
        e(tag, exception)
    }

    override fun w(tag: String, message: () -> String) {
        Timber.tag(tag).w(message())
    }

    override fun w(message: () -> String) {
        w(tag, message)
    }

    override fun i(tag: String, message: () -> String) {
        Timber.tag(tag).i(message())
    }

    override fun i(message: () -> String) {
        i(tag, message)
    }

    override fun retrieveLogFileUris(): List<Uri> {
        return fileLoggerTree.files.map {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", it)
        }
    }
}