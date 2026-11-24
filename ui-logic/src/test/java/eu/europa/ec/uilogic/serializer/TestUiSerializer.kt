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
    fun `Given a model, When it is encoded and decoded, Then the decoded model equals the original`() {
        // Given
        val original = TestModel(value = "hello", count = 42)

        // When
        val encoded = serializer.toBase64(original, TestModel.Parser)

        // Then
        assertNotNull(encoded)

        // When
        val decoded = serializer.fromBase64(encoded, TestModel::class.java, TestModel.Parser)

        // Then
        assertEquals(original, decoded)
    }

    @Test
    fun `Given a null payload, When it is decoded, Then null is returned`() {
        // When
        val decoded = serializer.fromBase64(null, TestModel::class.java, TestModel.Parser)

        // Then
        assertNull(decoded)
    }

    /**
     * '!' and '$' are outside the URL-safe Base64 alphabet → throws inside
     * decodeFromBase64 → runCatching catches → returns null.
     */
    @Test
    fun `Given a malformed base64 payload, When it is decoded, Then null is returned`() {
        // When
        val decoded = serializer.fromBase64(
            "!!not%base64\$\$",
            TestModel::class.java,
            TestModel.Parser
        )

        // Then
        assertNull(decoded)
    }

    @Test
    fun `Given valid base64 that is not JSON, When it is decoded, Then null is returned`() {
        // Given
        val notJson = "not a real json document".encodeToBase64()

        // When
        val decoded = serializer.fromBase64(notJson, TestModel::class.java, TestModel.Parser)

        // Then
        assertNull(decoded)
    }

    /**
     * Encoded JSON is missing the required `value` field.
     */
    @Test
    fun `Given a payload missing a required field, When it is decoded, Then null is returned`() {
        // Given
        val mismatched = "{\"count\":1}".encodeToBase64()

        // When
        val decoded = serializer.fromBase64(mismatched, TestModel::class.java, TestModel.Parser)

        // Then
        assertNull(decoded)
    }

    /**
     * kotlinx's `serializer(Class)` lookup throws SerializationException for any
     * class missing @Serializable. UiSerializerImpl wraps it in runCatching and
     * surfaces null. This is the only realistic toBase64 failure path now that
     * IconDataUi is type-safe.
     */
    @Test
    fun `Given a UiSerializable that is not Serializable, When it is encoded, Then null is returned`() {
        // When
        val encoded = serializer.toBase64(
            NonSerializableConfig(),
            NonSerializableConfig.Parser,
        )

        // Then
        assertNull(encoded)
    }

    @Test
    fun `Given a wrapper with an AppIcons constant, When it is encoded and decoded, Then the wrapper is restored`() {
        // Given
        val wrapper = IconWrapper(icon = AppIcons.WalletSecured)

        // When
        val encoded = serializer.toBase64(wrapper, IconWrapper.Parser)

        // Then
        assertNotNull(encoded)

        // When
        val decoded = serializer.fromBase64(
            encoded,
            IconWrapper::class.java,
            IconWrapper.Parser
        )

        // Then
        assertEquals(wrapper, decoded)
    }

    /**
     * Under the original Gson-based design ArrowBack would have lost its
     * imageVector across the nav boundary. With the AppIconKey enum identity it
     * round-trips because the wire carries only the key — the imageVector is
     * looked up locally from the enum on the destination side.
     */
    @Test
    fun `Given an imageVector-only AppIcon, When it is encoded and decoded, Then the imageVector is preserved`() {
        // Given
        val wrapper = IconWrapper(icon = AppIcons.ArrowBack)
        assertNotNull(wrapper.icon.imageVector)
        assertNull(wrapper.icon.resourceId)

        // When
        val encoded = serializer.toBase64(wrapper, IconWrapper.Parser)

        // Then
        assertNotNull(encoded)

        // When
        val decoded = serializer.fromBase64(
            encoded,
            IconWrapper::class.java,
            IconWrapper.Parser
        )

        // Then
        assertEquals(wrapper, decoded)
        assertNotNull(decoded?.icon?.imageVector)
        assertTrue(
            "imageVector reference should match the AppIconKey enum entry",
            decoded?.icon?.imageVector === wrapper.icon.imageVector,
        )
    }
}