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

package eu.europa.ec.uilogic.serializer

import eu.europa.ec.businesslogic.extension.encodeToBase64
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.IconDataUi
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Robolectric is required because [UiSerializerImpl] base64-encodes the JSON payload
 * via `android.util.Base64`, which is unavailable on a plain JVM.
 */
@RunWith(RobolectricTestRunner::class)
class TestUiSerializer {

    private val serializer: UiSerializer = UiSerializerImpl()

    @Serializable
    data class TestModel(val value: String, val count: Int = 0) : UiSerializable {
        companion object Parser : UiSerializableParser {
            override val serializedKeyName = "testConfig"
        }
    }

    @Serializable
    data class IconWrapper(val icon: IconDataUi) : UiSerializable {
        companion object Parser : UiSerializableParser {
            override val serializedKeyName = "iconWrapperConfig"
        }
    }

    /**
     * A [UiSerializable] that is deliberately NOT `@Serializable` — used to exercise
     * the runCatching error path in `UiSerializerImpl.toBase64` (kotlinx's
     * `serializer(Class<*>)` lookup throws for non-Serializable classes).
     */
    class NonSerializableConfig : UiSerializable {
        companion object Parser : UiSerializableParser {
            override val serializedKeyName = "nonSerializableConfig"
        }
    }

    @Test
    fun `roundtrip preserves the model`() {
        val original = TestModel(value = "hello", count = 42)

        val encoded = serializer.toBase64(original, TestModel.Parser)
        assertNotNull(encoded)

        val decoded = serializer.fromBase64(encoded, TestModel::class.java, TestModel.Parser)
        assertEquals(original, decoded)
    }

    @Test
    fun `fromBase64 with null payload returns null`() {
        val decoded = serializer.fromBase64(null, TestModel::class.java, TestModel.Parser)
        assertNull(decoded)
    }

    @Test
    fun `fromBase64 with malformed base64 returns null`() {
        // '!' and '$' are outside the URL-safe Base64 alphabet → throws inside
        // decodeFromBase64 → runCatching catches → returns null.
        val decoded = serializer.fromBase64(
            "!!not%base64\$\$",
            TestModel::class.java,
            TestModel.Parser
        )
        assertNull(decoded)
    }

    @Test
    fun `fromBase64 with valid base64 but invalid JSON returns null`() {
        val notJson = "not a real json document".encodeToBase64()
        val decoded = serializer.fromBase64(notJson, TestModel::class.java, TestModel.Parser)
        assertNull(decoded)
    }

    @Test
    fun `fromBase64 of a payload encoded for a different shape returns null`() {
        // Encoded JSON is missing the required `value` field.
        val mismatched = "{\"count\":1}".encodeToBase64()
        val decoded = serializer.fromBase64(mismatched, TestModel::class.java, TestModel.Parser)
        assertNull(decoded)
    }

    @Test
    fun `toBase64 returns null for a UiSerializable that is not Serializable`() {
        // kotlinx's `serializer(Class)` lookup throws SerializationException for any
        // class missing @Serializable. UiSerializerImpl wraps it in runCatching and
        // surfaces null. This is the only realistic toBase64 failure path now that
        // IconDataUi is type-safe.
        val encoded = serializer.toBase64(
            NonSerializableConfig(),
            NonSerializableConfig.Parser,
        )
        assertNull(encoded)
    }

    @Test
    fun `roundtrip of a nested AppIcons constant succeeds`() {
        val wrapper = IconWrapper(icon = AppIcons.WalletSecured)

        val encoded = serializer.toBase64(wrapper, IconWrapper.Parser)
        assertNotNull(encoded)

        val decoded = serializer.fromBase64(
            encoded,
            IconWrapper::class.java,
            IconWrapper.Parser
        )
        assertEquals(wrapper, decoded)
    }

    @Test
    fun `roundtrip of a nested imageVector-only AppIcon preserves the imageVector`() {
        // Under the original Gson-based design ArrowBack would have lost its
        // imageVector across the nav boundary. With the AppIconKey enum identity it
        // round-trips because the wire carries only the key — the imageVector is
        // looked up locally from the enum on the destination side.
        val wrapper = IconWrapper(icon = AppIcons.ArrowBack)
        assertNotNull(wrapper.icon.imageVector)
        assertNull(wrapper.icon.resourceId)

        val encoded = serializer.toBase64(wrapper, IconWrapper.Parser)
        assertNotNull(encoded)

        val decoded = serializer.fromBase64(
            encoded,
            IconWrapper::class.java,
            IconWrapper.Parser
        )
        assertEquals(wrapper, decoded)
        assertNotNull(decoded?.icon?.imageVector)
        assertTrue(
            "imageVector reference should match the AppIconKey enum entry",
            decoded?.icon?.imageVector === wrapper.icon.imageVector,
        )
    }
}