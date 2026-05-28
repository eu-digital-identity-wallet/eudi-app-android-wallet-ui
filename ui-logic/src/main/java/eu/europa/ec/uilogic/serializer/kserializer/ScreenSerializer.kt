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

import eu.europa.ec.uilogic.navigation.Screen
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializes [Screen] (which is an `open class` with singleton subclasses) by writing the
 * `screenName` and `parameters` strings used to construct it. On the read side we just
 * reconstruct a plain [Screen] — callers only ever consume `screenName` and `screenRoute`,
 * so identity with the original singleton instance does not matter.
 */
object ScreenSerializer : KSerializer<Screen> {

    @Serializable
    private data class ScreenSurrogate(val name: String, val parameters: String = "")

    private val delegate = ScreenSurrogate.serializer()

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: Screen) {
        // screenRoute is the concatenation of (name + parameters) at Screen.kt construction.
        val parameters = value.screenRoute.removePrefix(value.screenName)
        delegate.serialize(encoder, ScreenSurrogate(value.screenName, parameters))
    }

    override fun deserialize(decoder: Decoder): Screen {
        val s = delegate.deserialize(decoder)
        return Screen(s.name, s.parameters)
    }
}