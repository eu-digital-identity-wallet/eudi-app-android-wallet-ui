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

package eu.europa.ec.uilogic.serializer.kserializer

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI

class TestUriSerializer {

    private val json = Json

    @Test
    fun `Given an https URI, When it is serialized and deserialized, Then it is unchanged`() {
        // Given
        val uri = URI("https://example.com/path?query=value#frag")

        // When
        val encoded = json.encodeToString(UriSerializer, uri)
        val decoded = json.decodeFromString(UriSerializer, encoded)

        // Then
        assertEquals(uri, decoded)
    }

    @Test
    fun `Given an opaque URI, When it is serialized and deserialized, Then it is unchanged`() {
        // Given
        val uri = URI("mailto:test@example.com")

        // When
        val encoded = json.encodeToString(UriSerializer, uri)
        val decoded = json.decodeFromString(UriSerializer, encoded)

        // Then
        assertEquals(uri, decoded)
    }

    @Test
    fun `Given a relative URI, When it is serialized and deserialized, Then it is unchanged`() {
        // Given
        val uri = URI("relative/path")

        // When
        val encoded = json.encodeToString(UriSerializer, uri)
        val decoded = json.decodeFromString(UriSerializer, encoded)

        // Then
        assertEquals(uri, decoded)
    }
}