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
    fun `https URI roundtrips losslessly`() {
        val uri = URI("https://example.com/path?query=value#frag")
        val encoded = json.encodeToString(UriSerializer, uri)
        val decoded = json.decodeFromString(UriSerializer, encoded)
        assertEquals(uri, decoded)
    }

    @Test
    fun `opaque URI roundtrips losslessly`() {
        val uri = URI("mailto:test@example.com")
        val encoded = json.encodeToString(UriSerializer, uri)
        val decoded = json.decodeFromString(UriSerializer, encoded)
        assertEquals(uri, decoded)
    }

    @Test
    fun `relative URI roundtrips losslessly`() {
        val uri = URI("relative/path")
        val encoded = json.encodeToString(UriSerializer, uri)
        val decoded = json.decodeFromString(UriSerializer, encoded)
        assertEquals(uri, decoded)
    }
}