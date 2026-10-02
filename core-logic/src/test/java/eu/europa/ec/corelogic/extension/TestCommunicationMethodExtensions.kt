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

package eu.europa.ec.corelogic.extension

import eu.europa.ec.corelogic.model.CommunicationMethodDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TestCommunicationMethodExtensions {

    //region toStoredCommunicationMethod

    // Case 1:
    // 1. Every communication method has a stable storage value.
    //
    // Case 1 Expected Result:
    // The value is independent of enum names/ordinals and decodes back to the same method.
    @Test
    fun `Given all methods, When encoded and decoded, Then stable storage values round trip`() {
        // Given
        val expected = mapOf(
            CommunicationMethodDomain.Website to "website",
            CommunicationMethodDomain.Email to "email",
            CommunicationMethodDomain.Phone to "phone",
        )

        // When
        val encoded = CommunicationMethodDomain.entries.associateWith { method ->
            method.toStoredCommunicationMethod()
        }
        val decoded = encoded.mapValues { (_, value) -> value.toCommunicationMethodDomainOrNull() }

        // Then
        assertEquals(expected, encoded)
        assertEquals(expected.keys.associateWith { method -> method }, decoded)
    }

    //endregion

    //region toCommunicationMethodDomainOrNull

    // Case 1:
    // 1. Stored methods are missing, unknown, translated, ordinal or noncanonical.
    //
    // Case 1 Expected Result:
    // No method is guessed from unsupported metadata.
    @Test
    fun `Given unsupported stored methods, When decoded, Then no method is inferred`() {
        // Given
        val values = listOf(null, "", " ", "Email", "email ", "0", "fax", "Web form")

        // When
        val results = values.map { value -> value.toCommunicationMethodDomainOrNull() }

        // Then
        results.forEach { result -> assertNull(result) }
    }

    //endregion
}