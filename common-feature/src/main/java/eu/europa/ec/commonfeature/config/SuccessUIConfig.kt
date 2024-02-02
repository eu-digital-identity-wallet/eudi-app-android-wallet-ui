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

package eu.europa.ec.commonfeature.config

import androidx.annotation.DrawableRes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.serializer.UiSerializable
import eu.europa.ec.uilogic.serializer.UiSerializableParser
import eu.europa.ec.uilogic.serializer.adapter.SerializableTypeAdapter

data class SuccessUIConfig(
    val header: String?,
    val content: String,
    val imageConfig: ImageConfig,
    val buttonConfig: List<ButtonConfig>,
    val onBackScreenToNavigate: ConfigNavigation
) : UiSerializable {

    data class ImageConfig(
        val type: Type,
        @DrawableRes val drawableRes: Int? = null,
        val contentDescription: String? = null
    ) {
        enum class Type {
            DRAWABLE, DEFAULT
        }
    }

    data class ButtonConfig(
        val text: String,
        val style: Style,
        val navigation: ConfigNavigation,
    ) {
        enum class Style {
            PRIMARY, OUTLINE
        }
    }

    companion object Parser : UiSerializableParser {
        override val serializedKeyName = "successConfig"
        override fun provideParser(): Gson {
            return GsonBuilder().registerTypeAdapter(
                NavigationType::class.java,
                SerializableTypeAdapter<NavigationType>()
            ).create()
        }
    }
}