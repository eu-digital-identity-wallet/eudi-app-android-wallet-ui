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

package eu.europa.ec.businesslogic.controller.log

import android.content.Context
import eu.europa.ec.businesslogic.util.FileLoggerTree
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import timber.log.Timber
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class TestLogController {

    private val context: Context = RuntimeEnvironment.getApplication()

    private val logsDir: File
        get() = File(context.filesDir, "logs")

    private val loggedText: String
        get() = logsDir.listFiles().orEmpty()
            .filter { it.name.endsWith(".txt") }
            .joinToString(separator = "\n") { it.readText() }

    @Before
    fun before() {
        Timber.uprootAll()
        logsDir.deleteRecursively()
    }

    @After
    fun after() {
        Timber.forest().filterIsInstance<FileLoggerTree>().forEach { tree -> tree.close() }
        Timber.uprootAll()
    }

    @Test
    fun `Given logged messages, When the log files are deleted, Then they are gone and later messages go to a new file`() {
        val controller = LogControllerImpl(context = context, tag = "TAG")
        controller.i { "before the reset" }

        controller.deleteLogFiles()
        controller.i { "after the reset" }

        assertFalse(loggedText.contains("before the reset"))
        assertTrue(loggedText.contains("after the reset"))
    }

    @Test
    fun `Given the log files were deleted, When they are deleted again, Then logging goes on`() {
        val controller = LogControllerImpl(context = context, tag = "TAG")
        controller.i { "first" }

        controller.deleteLogFiles()
        controller.deleteLogFiles()
        controller.i { "second" }

        assertFalse(loggedText.contains("first"))
        assertTrue(loggedText.contains("second"))
    }

    //region deleteLogFiles
    // Case 1:
    // 1. A nonempty subdirectory cannot be removed as a log file.
    //
    // Case 1 Expected Result:
    // Cleanup reports failure, logging continues, and cleanup can be retried.
    @Test
    fun `Given a log entry cannot be deleted, When cleanup fails and is retried, Then logging continues`() {
        // Given
        val controller = LogControllerImpl(
            context = context,
            tag = "TAG",
            shouldWriteLogFile = true,
        )
        val remainingDirectory = File(logsDir, "remaining").apply { mkdirs() }
        File(remainingDirectory, "entry.txt").writeText("remaining contents")
        val mockedMessageAfterFailure = "after failed cleanup"
        val mockedMessageAfterRetry = "after cleanup retry"

        // When
        val result = runCatching { controller.deleteLogFiles() }
        controller.i { mockedMessageAfterFailure }

        // Then
        assertTrue(result.exceptionOrNull() is IOException)
        assertTrue(remainingDirectory.exists())
        assertTrue(loggedText.contains(mockedMessageAfterFailure))

        assertTrue(remainingDirectory.deleteRecursively())
        controller.deleteLogFiles()
        controller.i { mockedMessageAfterRetry }

        assertFalse(loggedText.contains(mockedMessageAfterFailure))
        assertTrue(loggedText.contains(mockedMessageAfterRetry))
    }
    //endregion

    @Test
    fun `Given a build that writes no log file, When messages are logged, Then no file is written or offered and old files are deleted`() {
        logsDir.mkdirs()
        File(logsDir, "eudi-android-wallet-logs0.txt").writeText("left by an earlier build")

        val controller = LogControllerImpl(
            context = context,
            tag = "TAG",
            shouldWriteLogFile = false,
        )
        controller.i { "not written" }

        assertTrue(logsDir.listFiles().orEmpty().isEmpty())
        assertTrue(controller.retrieveLogFileUris().isEmpty())
    }

    @Test
    fun `Given a build that writes no log file, When the log files are deleted, Then none is written afterwards`() {
        val controller = LogControllerImpl(
            context = context,
            tag = "TAG",
            shouldWriteLogFile = false,
        )

        controller.deleteLogFiles()
        controller.i { "not written" }

        assertTrue(logsDir.listFiles().orEmpty().isEmpty())
    }
}