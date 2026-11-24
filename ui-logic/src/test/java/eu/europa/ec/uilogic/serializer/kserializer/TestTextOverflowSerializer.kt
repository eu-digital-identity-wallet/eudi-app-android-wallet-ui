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

import androidx.compose.ui.text.style.TextOverflow
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class TestTextOverflowSerializer {

    private val json = Json

    @Test
    fun `Given TextOverflow Clip, When it is serialized and deserialized, Then it round-trips as its name`() {
        // Then
        assertRoundtrip(TextOverflow.Clip, "\"Clip\"")
    }

    @Test
    fun `Given TextOverflow Ellipsis, When it is serialized and deserialized, Then it round-trips as its name`() {
        // Then
        assertRoundtrip(TextOverflow.Ellipsis, "\"Ellipsis\"")
    }

    @Test
    fun `Given TextOverflow Visible, When it is serialized and deserialized, Then it round-trips as its name`() {
        // Then
        assertRoundtrip(TextOverflow.Visible, "\"Visible\"")
    }

    @Test
    fun `Given TextOverflow StartEllipsis, When it is serialized and deserialized, Then it round-trips as its name`() {
        // Then
        assertRoundtrip(TextOverflow.StartEllipsis, "\"StartEllipsis\"")
    }

    @Test
    fun `Given TextOverflow MiddleEllipsis, When it is serialized and deserialized, Then it round-trips as its name`() {
        // Then
        assertRoundtrip(TextOverflow.MiddleEllipsis, "\"MiddleEllipsis\"")
    }

    @Test
    fun `Given an unknown JSON string, When it is decoded, Then it falls back to Ellipsis`() {
        // When
        val decoded = json.decodeFromString(TextOverflowSerializer, "\"NotARealOverflow\"")

        // Then
        assertEquals(TextOverflow.Ellipsis, decoded)
    }

    /**
     * `TextOverflow` is a Compose value class with an `internal` Int constructor and
     * a fixed set of public companion constants. The `else` branch in
     * [TextOverflowSerializer.serialize] is therefore unreachable from a normal
     * caller — but we exercise it here via reflection on `box-impl` (Kotlin's value
     * class boxing helper) so the serializer's defensive fallback is still covered.
     */
    @Test
    fun `Given an unknown value class instance, When it is serialized, Then it is encoded as Ellipsis`() {
        // Given
        val boxImpl = TextOverflow::class.java.declaredMethods
            .single { it.name == "box-impl" }
            .apply { isAccessible = true }
        val unknown = boxImpl.invoke(null, 9999) as TextOverflow

        // When
        val encoded = json.encodeToString(TextOverflowSerializer, unknown)

        // Then
        assertEquals("\"Ellipsis\"", encoded)
    }

    private fun assertRoundtrip(value: TextOverflow, expectedJson: String) {
        val encoded = json.encodeToString(TextOverflowSerializer, value)
        assertEquals(expectedJson, encoded)
        val decoded = json.decodeFromString(TextOverflowSerializer, encoded)
        assertEquals(value, decoded)
    }
}