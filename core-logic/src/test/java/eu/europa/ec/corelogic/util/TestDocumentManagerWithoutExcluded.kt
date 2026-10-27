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

package eu.europa.ec.corelogic.util

import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class TestDocumentManagerWithoutExcluded {

    private val revoked: IssuedDocument = mock { on { id } doReturn "revoked" }
    private val valid: IssuedDocument = mock { on { id } doReturn "valid" }
    private val documentManager: DocumentManager = mock {
        on { getDocuments(anyOrNull()) } doReturn listOf(revoked, valid)
    }

    @Test
    fun `Given an excluded document, When the documents are read, Then it is left out`() {
        val filtered = DocumentManagerWithoutExcluded(documentManager) { setOf("revoked") }

        assertEquals(listOf(valid), filtered.getDocuments(null))
    }

    @Test
    fun `Given the excluded documents change, When the documents are read again, Then the new set applies`() {
        var excluded = setOf("revoked")
        val filtered = DocumentManagerWithoutExcluded(documentManager) { excluded }
        assertEquals(listOf(valid), filtered.getDocuments(null))

        excluded = emptySet()

        assertEquals(listOf(revoked, valid), filtered.getDocuments(null))
    }

    @Test
    fun `Given a predicate, When the documents are read, Then it is passed to the wallet`() {
        val predicate: (Document) -> Boolean = { true }
        val filtered = DocumentManagerWithoutExcluded(documentManager) { emptySet() }

        filtered.getDocuments(predicate)

        verify(documentManager).getDocuments(predicate)
    }
}