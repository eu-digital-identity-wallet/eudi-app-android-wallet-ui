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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import eu.europa.ec.businesslogic.extension.decodeFromBase64ToString
import eu.europa.ec.businesslogic.extension.encodeToBase64
import eu.europa.ec.uilogic.navigation.Screen
import eu.europa.ec.uilogic.serializer.kserializer.ColorSerializer
import eu.europa.ec.uilogic.serializer.kserializer.ScreenSerializer
import eu.europa.ec.uilogic.serializer.kserializer.TextAlignSerializer
import eu.europa.ec.uilogic.serializer.kserializer.TextOverflowSerializer
import eu.europa.ec.uilogic.serializer.kserializer.UriSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import java.net.URI

/**
 * Shared [Json] used by [UiSerializerImpl] for every [UiSerializable] config.
 *
 * Configured to:
 *  - tolerate older payloads that contain unknown keys (`ignoreUnknownKeys`),
 *  - always emit defaulted fields so downstream readers see a stable schema,
 *  - and resolve Compose / `java.net` types via the contextual serializers below.
 */
internal val UiJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    classDiscriminator = "type"
    serializersModule = SerializersModule {
        contextual(Color::class, ColorSerializer)
        contextual(Screen::class, ScreenSerializer)
        contextual(URI::class, UriSerializer)
        contextual(TextAlign::class, TextAlignSerializer)
        contextual(TextOverflow::class, TextOverflowSerializer)
    }
}

/**
 * Round-trips a [UiSerializable] through a Base64-encoded JSON payload so it can be
 * embedded in a Compose navigation argument string.
 *
 * Implementations use `kotlinx.serialization` under the hood; every concrete
 * [UiSerializable] type must therefore be annotated with `@Serializable` and reachable
 * by the shared [UiJson] instance.
 */
interface UiSerializer {
    fun <M : UiSerializable> toBase64(
        model: M,
        parser: UiSerializableParser,
    ): String?

    fun <M : UiSerializable> fromBase64(
        payload: String?,
        model: Class<M>,
        parser: UiSerializableParser,
    ): M?
}

class UiSerializerImpl : UiSerializer {

    override fun <M : UiSerializable> toBase64(
        model: M,
        parser: UiSerializableParser,
    ): String? = runCatching {
        @Suppress("UNCHECKED_CAST")
        val serializer = serializer(model::class.java) as KSerializer<M>
        UiJson.encodeToString(serializer, model).encodeToBase64()
    }.getOrNull()

    override fun <M : UiSerializable> fromBase64(
        payload: String?,
        model: Class<M>,
        parser: UiSerializableParser,
    ): M? {
        if (payload == null) return null
        return runCatching {
            @Suppress("UNCHECKED_CAST")
            val serializer = serializer(model) as KSerializer<M>
            UiJson.decodeFromString(serializer, payload.decodeFromBase64ToString())
        }.getOrNull()
    }
}