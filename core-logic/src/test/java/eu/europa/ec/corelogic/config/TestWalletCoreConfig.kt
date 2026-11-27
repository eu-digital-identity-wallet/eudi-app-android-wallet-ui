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

package eu.europa.ec.corelogic.config

import eu.europa.ec.corelogic.model.DocumentCategory
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.model.toDocumentCategory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.mock

class TestWalletCoreConfig {

    private val config: WalletCoreConfig = mock(defaultAnswer = Mockito.CALLS_REAL_METHODS)

    /** The issuers issue the EHIC as an SD-JWT VC with this vct. */
    @Test
    fun `Given the EHIC SD-JWT VC, When its category is looked up, Then it is Health`() {
        // Given
        val ehic = DocumentIdentifier.OTHER(formatType = "urn:eudi:ehic:1")

        // When
        val category = ehic.toDocumentCategory(allCategories = config.documentCategories)

        // Then
        assertEquals(DocumentCategory.Health, category)
    }
}
