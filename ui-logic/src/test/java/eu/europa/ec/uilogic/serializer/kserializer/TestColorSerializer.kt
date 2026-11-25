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
    fun `Given opaque white, When it is serialized and deserialized, Then it is unchanged`() {
        // Given
        val color = Color.White

        // When
        val encoded = json.encodeToString(ColorSerializer, color)
        val decoded = json.decodeFromString(ColorSerializer, encoded)

        // Then
        assertEquals(color, decoded)
    }

    @Test
    fun `Given fully transparent black, When it is serialized and deserialized, Then it is unchanged`() {
        // Given
        val color = Color(0x00000000)

        // When
        val encoded = json.encodeToString(ColorSerializer, color)
        val decoded = json.decodeFromString(ColorSerializer, encoded)

        // Then
        assertEquals(color, decoded)
    }

    @Test
    fun `Given an arbitrary ARGB color, When it is serialized and deserialized, Then it is unchanged`() {
        // Given
        val color = Color(red = 0.42f, green = 0.13f, blue = 0.7f, alpha = 0.85f)

        // When
        val encoded = json.encodeToString(ColorSerializer, color)
        val decoded = json.decodeFromString(ColorSerializer, encoded)

        // Then
        assertEquals(color, decoded)
    }

    @Test
    fun `Given Color Unspecified, When it is serialized and deserialized, Then it is unchanged`() {
        // Given
        val color = Color.Unspecified

        // When
        val encoded = json.encodeToString(ColorSerializer, color)
        val decoded = json.decodeFromString(ColorSerializer, encoded)

        // Then
        assertEquals(color, decoded)
    }

    /**
     * ULong-to-Long reinterpret cast: top bit may be set so we just assert
     * the payload is a plain numeric literal (no quotes / no object braces).
     */
    @Test
    fun `Given a color, When it is serialized, Then the form is a plain JSON number`() {
        // Given
        val color = Color(0xFFAABBCC.toInt())

        // When
        val encoded = json.encodeToString(ColorSerializer, color)

        // Then
        assert(encoded.toLongOrNull() != null) {
            "Expected a plain JSON Long, got: $encoded"
        }
    }
}