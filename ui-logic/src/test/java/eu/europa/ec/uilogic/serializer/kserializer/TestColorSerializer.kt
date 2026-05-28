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

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class TestColorSerializer {

    private val json = Json

    @Test
    fun `opaque white roundtrips losslessly`() {
        val color = Color.White
        val encoded = json.encodeToString(ColorSerializer, color)
        val decoded = json.decodeFromString(ColorSerializer, encoded)
        assertEquals(color, decoded)
    }

    @Test
    fun `fully transparent black roundtrips losslessly`() {
        val color = Color(0x00000000)
        val encoded = json.encodeToString(ColorSerializer, color)
        val decoded = json.decodeFromString(ColorSerializer, encoded)
        assertEquals(color, decoded)
    }

    @Test
    fun `arbitrary ARGB roundtrips losslessly`() {
        val color = Color(red = 0.42f, green = 0.13f, blue = 0.7f, alpha = 0.85f)
        val encoded = json.encodeToString(ColorSerializer, color)
        val decoded = json.decodeFromString(ColorSerializer, encoded)
        assertEquals(color, decoded)
    }

    @Test
    fun `Color Unspecified roundtrips losslessly`() {
        val color = Color.Unspecified
        val encoded = json.encodeToString(ColorSerializer, color)
        val decoded = json.decodeFromString(ColorSerializer, encoded)
        assertEquals(color, decoded)
    }

    @Test
    fun `serialized form is a JSON number`() {
        val color = Color(0xFFAABBCC.toInt())
        val encoded = json.encodeToString(ColorSerializer, color)
        // ULong-to-Long reinterpret cast: top bit may be set so we just assert
        // the payload is a plain numeric literal (no quotes / no object braces).
        assert(encoded.toLongOrNull() != null) {
            "Expected a plain JSON Long, got: $encoded"
        }
    }
}