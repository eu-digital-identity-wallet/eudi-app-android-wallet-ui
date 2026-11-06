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

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption

/** Keeps the directory itself. A missing directory is already empty. */
@Throws(IOException::class)
fun File.deleteContents(recursively: Boolean) {
    listFilesForDeletion().forEach { file ->
        val deleted = if (recursively) file.deleteRecursively() else file.delete()
        if (!deleted && !file.isMissing()) {
            throw IOException("Unable to delete ${file.path}")
        }
    }
    if (listFilesForDeletion().isNotEmpty()) {
        throw IOException("Unable to empty $path")
    }
}

private fun File.listFilesForDeletion(): Array<File> =
    listFiles() ?: if (isMissing()) {
        emptyArray()
    } else {
        throw IOException("Unable to list $path")
    }

// File.exists() also returns false when metadata cannot be read.
private fun File.isMissing(): Boolean = Files.notExists(toPath(), LinkOption.NOFOLLOW_LINKS)