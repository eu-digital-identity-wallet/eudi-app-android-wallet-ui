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

package eu.europa.ec.businesslogic.util

import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.logging.FileHandler
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogRecord

/**
 * A [Timber.Tree] that appends log messages to a rotating set of files.
 *
 * Replaces `fr.bipi.treessence.file.FileLoggerTree`, which was only distributed through
 * JitPack. Rotation is delegated to [FileHandler], so the file name follows
 * `java.util.logging` conventions: `%g` is substituted with the generation number of each
 * rotated file.
 */
class FileLoggerTree private constructor(
    private val minPriority: Int,
    private val fileLimit: Int,
    private val pathPattern: String,
    private val handler: FileHandler,
) : Timber.Tree() {

    /**
     * The log files that currently exist on disk.
     *
     * Names are derived from the configured pattern rather than by listing the directory,
     * so the `.lck` lock files [FileHandler] keeps alongside them are never exposed.
     */
    val files: List<File>
        get() = (0 until fileLimit)
            .map { File(pathPattern.replace(GENERATION_PLACEHOLDER, it.toString())) }
            .distinct()
            .filter { it.exists() }

    /** Releases the underlying file handles. */
    fun close() = handler.close()

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= minPriority

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        handler.publish(LogRecord(priority.toLevel(), format(priority, tag, message, t)))
    }

    private fun format(priority: Int, tag: String?, message: String, t: Throwable?): String =
        buildString {
            append(priority.toPriorityChar())
            append('/')
            append(tag ?: DEFAULT_TAG)
            append(": ")
            append(message)
            if (t != null) {
                append(System.lineSeparator())
                append(t.stackTraceToString())
            }
        }

    private fun Int.toLevel(): Level = when (this) {
        Log.VERBOSE -> Level.FINER
        Log.DEBUG -> Level.FINE
        Log.INFO -> Level.INFO
        Log.WARN -> Level.WARNING
        Log.ERROR, Log.ASSERT -> Level.SEVERE
        else -> Level.INFO
    }

    private fun Int.toPriorityChar(): Char = when (this) {
        Log.VERBOSE -> 'V'
        Log.DEBUG -> 'D'
        Log.INFO -> 'I'
        Log.WARN -> 'W'
        Log.ERROR -> 'E'
        Log.ASSERT -> 'A'
        else -> '?'
    }

    class Builder {
        private var fileName: String = DEFAULT_FILE_NAME
        private var dir: File? = null
        private var sizeLimit: Int = DEFAULT_SIZE_LIMIT
        private var fileLimit: Int = DEFAULT_FILE_LIMIT
        private var minPriority: Int = Log.DEBUG
        private var append: Boolean = true

        fun withFileName(fileName: String) = apply { this.fileName = fileName }

        fun withDir(dir: File) = apply { this.dir = dir }

        fun withSizeLimit(sizeLimit: Int) = apply { this.sizeLimit = sizeLimit }

        fun withFileLimit(fileLimit: Int) = apply { this.fileLimit = fileLimit }

        fun withMinPriority(priority: Int) = apply { this.minPriority = priority }

        fun appendToFile(append: Boolean) = apply { this.append = append }

        @Throws(IOException::class)
        fun build(): FileLoggerTree {
            val logDir = requireNotNull(dir) { "A log directory must be set via withDir()." }
            if (!logDir.exists() && !logDir.mkdirs()) {
                throw IOException("Unable to create log directory ${logDir.absolutePath}")
            }
            val pathPattern = File(logDir, fileName).absolutePath
            val handler = FileHandler(pathPattern, sizeLimit, fileLimit, append).apply {
                formatter = TimestampFormatter()
                level = Level.ALL
            }
            return FileLoggerTree(minPriority, fileLimit, pathPattern, handler)
        }
    }

    private companion object {
        const val GENERATION_PLACEHOLDER = "%g"
        const val DEFAULT_FILE_NAME = "log%g.txt"
        const val DEFAULT_SIZE_LIMIT = 1048576
        const val DEFAULT_FILE_LIMIT = 3
        const val DEFAULT_TAG = "EUDI"
    }
}

/** Prefixes each already-formatted record with a timestamp and terminates the line. */
private class TimestampFormatter : Formatter() {

    private val dateFormat = SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US)

    override fun format(record: LogRecord): String =
        "${dateFormat.format(Date(record.millis))} ${record.message}${System.lineSeparator()}"

    private companion object {
        const val TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS"
    }
}
