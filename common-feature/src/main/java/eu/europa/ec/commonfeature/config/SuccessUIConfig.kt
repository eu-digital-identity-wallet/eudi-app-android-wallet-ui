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

package eu.europa.ec.commonfeature.config

import androidx.compose.ui.graphics.Color
import eu.europa.ec.resourceslogic.theme.values.ThemeColors
import eu.europa.ec.uilogic.component.AppIconAndTextDataUi
import eu.europa.ec.uilogic.component.IconDataUi
import eu.europa.ec.uilogic.component.content.ContentHeaderConfig
import eu.europa.ec.uilogic.component.utils.PERCENTAGE_60
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.serializer.UiSerializable
import eu.europa.ec.uilogic.serializer.UiSerializableParser
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SuccessUIConfig(
    val textElementsConfig: TextElementsConfig,
    val headerConfig: ContentHeaderConfig = ContentHeaderConfig(
        appIconAndTextData = AppIconAndTextDataUi(),
        description = null,
    ),
    val imageConfig: ImageConfig,
    val buttonConfig: List<ButtonConfig>,
    val onBackScreenToNavigate: ConfigNavigation
) : UiSerializable {

    @Serializable
    data class ImageConfig(
        val type: Type = Type.Default,
        @Contextual val tint: Color? = ThemeColors.success,
        val screenPercentageSize: Float = PERCENTAGE_60,
    ) {
        @Serializable
        sealed class Type {
            @Serializable
            @SerialName("Default")
            data object Default : Type()

            @Serializable
            @SerialName("Drawable")
            data class Drawable(val icon: IconDataUi) : Type()
        }
    }

    @Serializable
    data class ButtonConfig(
        val text: String,
        val style: Style,
        val navigation: ConfigNavigation,
    ) {
        @Serializable
        enum class Style {
            PRIMARY, OUTLINE
        }
    }

    @Serializable
    data class TextElementsConfig(
        val text: String,
        val description: String,
        @Contextual val color: Color = ThemeColors.success
    )

    companion object Parser : UiSerializableParser {
        override val serializedKeyName = "successConfig"
    }
}