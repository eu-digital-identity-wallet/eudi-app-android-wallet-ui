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
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import timber.log.Timber
import java.io.File

@RunWith(RobolectricTestRunner::class)
class TestFileLoggerTree {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private var tree: FileLoggerTree? = null

    @Before
    fun before() {
        Timber.uprootAll()
    }

    @After
    fun after() {
        Timber.uprootAll()
        tree?.close()
    }

    private val logsDir: File
        get() = File(temporaryFolder.root, "logs")

    /** Builds the tree and plants it, so logging goes through the real Timber path. */
    private fun plantTree(
        sizeLimit: Int = 1048576,
        fileLimit: Int = 3,
        minPriority: Int = Log.DEBUG,
    ): FileLoggerTree = FileLoggerTree.Builder()
        .withFileName(FILE_NAME)
        .withDir(logsDir)
        .withSizeLimit(sizeLimit)
        .withFileLimit(fileLimit)
        .withMinPriority(minPriority)
        .appendToFile(true)
        .build()
        .also {
            tree = it
            Timber.plant(it)
        }

    @Test
    fun `Given a message at minPriority, When logged, Then it is written to the log file`() {
        val logger = plantTree()

        Timber.tag("TAG").d("hello world")

        Assert.assertTrue(logger.files.single().readText().contains("D/TAG: hello world"))
    }

    @Test
    fun `Given a message below minPriority, When logged, Then it is not written`() {
        val logger = plantTree(minPriority = Log.WARN)

        Timber.tag("TAG").d("should be dropped")
        Timber.tag("TAG").e("should be kept")

        val contents = logger.files.single().readText()
        Assert.assertTrue(contents.contains("E/TAG: should be kept"))
        Assert.assertTrue(!contents.contains("should be dropped"))
    }

    @Test
    fun `Given a throwable, When logged, Then its stack trace is appended`() {
        val logger = plantTree()

        Timber.tag("TAG").e(IllegalStateException("kaboom"), "boom")

        val contents = logger.files.single().readText()
        Assert.assertTrue(contents.contains("E/TAG: boom"))
        Assert.assertTrue(contents.contains("IllegalStateException"))
        Assert.assertTrue(contents.contains("kaboom"))
    }

    @Test
    fun `Given no explicit tag, When logged, Then the default tag is used`() {
        val logger = plantTree()

        Timber.i("untagged")

        Assert.assertTrue(logger.files.single().readText().contains("I/EUDI: untagged"))
    }

    @Test
    fun `Given writes past the size limit, When logging, Then output rotates across files`() {
        val logger = plantTree(sizeLimit = 256, fileLimit = 3)

        repeat(40) { Timber.tag("TAG").d("message number %s", it) }

        Assert.assertTrue(
            "expected rotation, got ${logger.files.size} file(s)",
            logger.files.size > 1
        )
        Assert.assertTrue(logger.files.size <= 3)
    }

    @Test
    fun `Given lock files beside the logs, When files is read, Then only log files are returned`() {
        val logger = plantTree()
        Timber.tag("TAG").d("hello")

        Assert.assertTrue(logsDir.listFiles().orEmpty().any { it.name.endsWith(".lck") })
        Assert.assertTrue(logger.files.none { it.name.endsWith(".lck") })
    }

    @Test
    fun `Given a directory that does not exist yet, When building, Then it is created`() {
        val logger = plantTree()

        Timber.tag("TAG").d("hello")

        Assert.assertEquals(1, logger.files.size)
        Assert.assertTrue(logsDir.isDirectory)
    }

    private companion object {
        const val FILE_NAME = "eudi-android-wallet-logs%g.txt"
    }
}