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

import androidx.compose.ui.text.style.TextAlign
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class TestTextAlignSerializer {

    private val json = Json

    @Test
    fun `Given TextAlign Start, When it is serialized and deserialized, Then it round-trips as its name`() {
        // Then
        assertRoundtrip(TextAlign.Start, "\"Start\"")
    }

    @Test
    fun `Given TextAlign End, When it is serialized and deserialized, Then it round-trips as its name`() {
        // Then
        assertRoundtrip(TextAlign.End, "\"End\"")
    }

    @Test
    fun `Given TextAlign Left, When it is serialized and deserialized, Then it round-trips as its name`() {
        // Then
        assertRoundtrip(TextAlign.Left, "\"Left\"")
    }

    @Test
    fun `Given TextAlign Right, When it is serialized and deserialized, Then it round-trips as its name`() {
        // Then
        assertRoundtrip(TextAlign.Right, "\"Right\"")
    }

    @Test
    fun `Given TextAlign Center, When it is serialized and deserialized, Then it round-trips as its name`() {
        // Then
        assertRoundtrip(TextAlign.Center, "\"Center\"")
    }

    @Test
    fun `Given TextAlign Justify, When it is serialized and deserialized, Then it round-trips as its name`() {
        // Then
        assertRoundtrip(TextAlign.Justify, "\"Justify\"")
    }

    @Test
    fun `Given TextAlign Unspecified, When it is serialized and deserialized, Then it round-trips as its name`() {
        // Then
        assertRoundtrip(TextAlign.Unspecified, "\"Unspecified\"")
    }

    @Test
    fun `Given an unknown JSON string, When it is decoded, Then it falls back to Unspecified`() {
        // When
        val decoded = json.decodeFromString(TextAlignSerializer, "\"NotARealAlignment\"")

        // Then
        assertEquals(TextAlign.Unspecified, decoded)
    }

    private fun assertRoundtrip(value: TextAlign, expectedJson: String) {
        val encoded = json.encodeToString(TextAlignSerializer, value)
        assertEquals(expectedJson, encoded)
        val decoded = json.decodeFromString(TextAlignSerializer, encoded)
        assertEquals(value, decoded)
    }
}