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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializes [TextOverflow] as a stable name string. [TextOverflow] is a Compose
 * `value class` with an internal Int constructor, so we map by the public companion
 * constants instead of the raw int.
 */
object TextOverflowSerializer : KSerializer<TextOverflow> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(
            "androidx.compose.ui.text.style.TextOverflow",
            PrimitiveKind.STRING,
        )

    override fun serialize(encoder: Encoder, value: TextOverflow) {
        encoder.encodeString(
            when (value) {
                TextOverflow.Clip -> "Clip"
                TextOverflow.Ellipsis -> "Ellipsis"
                TextOverflow.Visible -> "Visible"
                TextOverflow.StartEllipsis -> "StartEllipsis"
                TextOverflow.MiddleEllipsis -> "MiddleEllipsis"
                else -> "Ellipsis"
            }
        )
    }

    override fun deserialize(decoder: Decoder): TextOverflow =
        when (decoder.decodeString()) {
            "Clip" -> TextOverflow.Clip
            "Ellipsis" -> TextOverflow.Ellipsis
            "Visible" -> TextOverflow.Visible
            "StartEllipsis" -> TextOverflow.StartEllipsis
            "MiddleEllipsis" -> TextOverflow.MiddleEllipsis
            else -> TextOverflow.Ellipsis
        }
}