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
    fun `Start roundtrips losslessly`() = assertRoundtrip(TextAlign.Start, "\"Start\"")

    @Test
    fun `End roundtrips losslessly`() = assertRoundtrip(TextAlign.End, "\"End\"")

    @Test
    fun `Left roundtrips losslessly`() = assertRoundtrip(TextAlign.Left, "\"Left\"")

    @Test
    fun `Right roundtrips losslessly`() = assertRoundtrip(TextAlign.Right, "\"Right\"")

    @Test
    fun `Center roundtrips losslessly`() = assertRoundtrip(TextAlign.Center, "\"Center\"")

    @Test
    fun `Justify roundtrips losslessly`() = assertRoundtrip(TextAlign.Justify, "\"Justify\"")

    @Test
    fun `Unspecified roundtrips losslessly`() =
        assertRoundtrip(TextAlign.Unspecified, "\"Unspecified\"")

    @Test
    fun `unknown JSON string falls back to Unspecified`() {
        val decoded = json.decodeFromString(TextAlignSerializer, "\"NotARealAlignment\"")
        assertEquals(TextAlign.Unspecified, decoded)
    }

    private fun assertRoundtrip(value: TextAlign, expectedJson: String) {
        val encoded = json.encodeToString(TextAlignSerializer, value)
        assertEquals(expectedJson, encoded)
        val decoded = json.decodeFromString(TextAlignSerializer, encoded)
        assertEquals(value, decoded)
    }
}