/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.uilogic.serializer.adapter

import com.google.gson.*
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
