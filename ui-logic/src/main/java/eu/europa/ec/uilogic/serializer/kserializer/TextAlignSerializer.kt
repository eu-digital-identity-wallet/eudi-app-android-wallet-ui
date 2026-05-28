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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializes [TextAlign] as a stable name string. [TextAlign] is a Compose `value class`
 * with an internal Int constructor, so we cannot rebuild it from the raw int; we map
 * by the public companion constants instead.
 */
object TextAlignSerializer : KSerializer<TextAlign> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("androidx.compose.ui.text.style.TextAlign", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: TextAlign) {
        encoder.encodeString(
            when (value) {
                TextAlign.Start -> "Start"
                TextAlign.End -> "End"
                TextAlign.Left -> "Left"
                TextAlign.Right -> "Right"
                TextAlign.Center -> "Center"
                TextAlign.Justify -> "Justify"
                else -> "Unspecified"
            }
        )
    }

    override fun deserialize(decoder: Decoder): TextAlign =
        when (decoder.decodeString()) {
            "Start" -> TextAlign.Start
            "End" -> TextAlign.End
            "Left" -> TextAlign.Left
            "Right" -> TextAlign.Right
            "Center" -> TextAlign.Center
            "Justify" -> TextAlign.Justify
            else -> TextAlign.Unspecified
        }
}