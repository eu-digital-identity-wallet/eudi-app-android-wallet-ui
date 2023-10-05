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

package eu.europa.ec.commonfeature.config

import androidx.annotation.DrawableRes
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.serializer.UiSerializable
import eu.europa.ec.uilogic.serializer.UiSerializableParser

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
    }
}