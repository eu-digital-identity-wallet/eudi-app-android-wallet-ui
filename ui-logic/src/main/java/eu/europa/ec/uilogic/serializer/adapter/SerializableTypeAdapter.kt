/*
 * Copyright (c) 2023 European Commission
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

package eu.europa.ec.uilogic.serializer.adapter

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

interface SerializableAdapterType

class SerializableTypeAdapter<T : Any> : JsonSerializer<T>, JsonDeserializer<T>,
    SerializableAdapterType {

    private val CLASSNAME = "CLASSNAME"
    private val DATA = "DATA"

    @Throws(JsonParseException::class, RuntimeException::class)
    override fun serialize(
        src: T?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        if (src != null && context != null) {
            return JsonObject().apply {
                addProperty(CLASSNAME, src::class.java.name)
                add(DATA, context.serialize(src))
            }
        }
        throw RuntimeException("SerializableTypeAdapter:: Failed to serialize: ${src?.javaClass?.name}")
    }

    @Throws(JsonParseException::class, RuntimeException::class)
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): T {
        val jsonObject = json?.asJsonObject
        val className = jsonObject?.get(CLASSNAME)?.asString
        val clazz = getObjectClass(className)
        return context?.deserialize(jsonObject?.get(DATA), clazz)
            ?: throw RuntimeException("SerializableTypeAdapter:: Failed to deserialize: $className")
    }

    @Throws(JsonParseException::class)
    private fun getObjectClass(className: String?): Class<*> {
        try {
            return Class.forName(className.toString())
        } catch (e: ClassNotFoundException) {
            throw JsonParseException(e)
        }
    }
}
