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

package eu.europa.ec.businesslogic.extension

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.io.File
import java.io.IOException

class TestFileExtensions {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Mock
    private lateinit var directory: File

    @Mock
    private lateinit var child: File

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region deleteContents

    // Case 1:
    // 1. A directory contains a file.
    //
    // Case 1 Expected Result:
    // Deleting the contents removes the file and keeps the directory.
    @Test
    fun `Given a directory with a file, When deleteContents is called, Then the directory is kept empty`() {
        // Given
        val directory = temporaryFolder.newFolder()
        val file = File(directory, "log.txt").apply { writeText("log contents") }

        // When
        directory.deleteContents(recursively = false)

        // Then
        assertTrue(directory.isDirectory)
        assertFalse(file.exists())
        assertTrue(directory.listFiles()?.isEmpty() == true)
    }

    // Case 2:
    // 1. A directory contains a nested directory and a file.
    //
    // Case 2 Expected Result:
    // Recursive deletion removes all contents and keeps the outer directory.
    @Test
    fun `Given nested contents, When deleteContents is recursive, Then the directory is kept empty`() {
        // Given
        val directory = temporaryFolder.newFolder()
        val nestedDirectory = File(directory, "signed_pdfs").apply { mkdirs() }
        File(nestedDirectory, "document.pdf").writeText("document contents")

        // When
        directory.deleteContents(recursively = true)

        // Then
        assertTrue(directory.isDirectory)
        assertFalse(nestedDirectory.exists())
        assertTrue(directory.listFiles()?.isEmpty() == true)
    }

    // Case 3:
    // 1. The directory is already absent.
    //
    // Case 3 Expected Result:
    // Cleanup succeeds without creating it.
    @Test
    fun `Given a missing directory, When deleteContents is called, Then cleanup succeeds`() {
        // Given
        val directory = File(temporaryFolder.root, "missing")

        // When
        directory.deleteContents(recursively = true)

        // Then
        assertFalse(directory.exists())
    }

    // Case 4:
    // 1. The directory exists but listing its contents fails.
    //
    // Case 4 Expected Result:
    // Cleanup throws IOException.
    @Test
    fun `Given an unreadable directory, When deleteContents is called, Then cleanup fails`() {
        // Given
        mockUnreadableDirectory()

        // When
        val result = runCatching { directory.deleteContents(recursively = true) }

        // Then
        assertTrue(result.exceptionOrNull() is IOException)
    }

    // Case 5:
    // 1. Deleting a file returns false and the file still exists.
    //
    // Case 5 Expected Result:
    // Both deletion modes throw IOException.
    @Test
    fun `Given a file cannot be deleted, When deleteContents is called, Then cleanup fails`() {
        // Given
        mockDirectoryContents(before = arrayOf(child), after = arrayOf(child))
        mockFailedFileDeletion(stillExists = true)

        // When
        val directResult = runCatching { directory.deleteContents(recursively = false) }
        val recursiveResult = runCatching { directory.deleteContents(recursively = true) }

        // Then
        assertTrue(directResult.exceptionOrNull() is IOException)
        assertTrue(recursiveResult.exceptionOrNull() is IOException)
    }

    // Case 6:
    // 1. A file disappears after the directory is listed and delete returns false.
    //
    // Case 6 Expected Result:
    // Cleanup accepts the already removed file.
    @Test
    fun `Given a file is already removed, When deletion returns false, Then cleanup succeeds`() {
        // Given
        mockDirectoryContents(before = arrayOf(child), after = emptyArray())
        mockFailedFileDeletion(stillExists = false)

        // When
        val result = runCatching { directory.deleteContents(recursively = false) }

        // Then
        assertTrue(result.isSuccess)
    }

    // Case 7:
    // 1. New contents appear after the first directory listing.
    //
    // Case 7 Expected Result:
    // Cleanup reports that the directory is not empty.
    @Test
    fun `Given contents remain after cleanup, When deleteContents finishes, Then cleanup fails`() {
        // Given
        mockDirectoryContents(before = emptyArray(), after = arrayOf(child))

        // When
        val result = runCatching { directory.deleteContents(recursively = true) }

        // Then
        assertTrue(result.exceptionOrNull() is IOException)
    }
    //endregion

    //region helper functions
    private fun mockUnreadableDirectory() {
        val existingDirectory = temporaryFolder.newFolder()
        whenever(directory.listFiles()).thenReturn(null)
        whenever(directory.toPath()).thenReturn(existingDirectory.toPath())
    }

    private fun mockDirectoryContents(before: Array<File>, after: Array<File>) {
        whenever(directory.listFiles()).thenReturn(before, after)
    }

    private fun mockFailedFileDeletion(stillExists: Boolean) {
        val file = temporaryFolder.newFile()
        if (!stillExists) {
            assertTrue(file.delete())
        }
        whenever(child.toPath()).thenReturn(file.toPath())
        whenever(child.isFile).thenReturn(true)
        whenever(child.delete()).thenReturn(false)
        whenever(child.exists()).thenReturn(stillExists)
    }
    //endregion
}