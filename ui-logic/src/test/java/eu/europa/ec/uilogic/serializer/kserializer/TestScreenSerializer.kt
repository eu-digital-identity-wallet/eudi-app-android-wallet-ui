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

import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.Screen
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class TestScreenSerializer {

    private val json = Json

    @Test
    fun `roundtrip a Screen with parameters preserves screenName and screenRoute`() {
        val original = IssuanceScreens.AddDocument
        val encoded = json.encodeToString(ScreenSerializer, original)
        val decoded = json.decodeFromString(ScreenSerializer, encoded)
        assertEquals(original.screenName, decoded.screenName)
        assertEquals(original.screenRoute, decoded.screenRoute)
    }

    @Test
    fun `roundtrip a Screen without parameters preserves screenName and screenRoute`() {
        val original = Screen("BARE_SCREEN")
        val encoded = json.encodeToString(ScreenSerializer, original)
        val decoded = json.decodeFromString(ScreenSerializer, encoded)
        assertEquals("BARE_SCREEN", decoded.screenName)
        assertEquals("BARE_SCREEN", decoded.screenRoute)
    }

    @Test
    fun `roundtrip a Screen with explicit parameters string preserves it`() {
        val original = Screen("FOO", "?bar={bar}")
        val encoded = json.encodeToString(ScreenSerializer, original)
        val decoded = json.decodeFromString(ScreenSerializer, encoded)
        assertEquals("FOO", decoded.screenName)
        assertEquals("FOO?bar={bar}", decoded.screenRoute)
    }

    /**
     * `name` is required on the Surrogate; decoding without it exercises the
     * compiler-generated `missing required field` branch in `$$serializer`.
     */
    @Test(expected = Exception::class)
    fun `decode payload missing required name throws`() {
        json.decodeFromString(ScreenSerializer, """{"parameters":"?x={x}"}""")
    }

    /**
     * Explicit `parameters` field in the JSON (even when it equals the default `""`)
     * exercises the `field was provided, do not use default` branch in the Surrogate's
     * compiler-generated `$$serializer`. Without this we leave one branch of the
     * default-handling logic untested.
     */
    @Test
    fun `decode with explicit empty parameters reads the explicit value`() {
        val decoded = json.decodeFromString(
            ScreenSerializer,
            """{"name":"X","parameters":""}"""
        )
        assertEquals("X", decoded.screenName)
        assertEquals("X", decoded.screenRoute)
    }
}