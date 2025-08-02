/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.resourceslogic.theme.templates

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import eu.europa.ec.resourceslogic.theme.templates.ThemeTextStyle.Companion.toTextStyle
import eu.europa.ec.resourceslogic.theme.templates.structures.ThemeFont
import eu.europa.ec.resourceslogic.theme.templates.structures.ThemeFont.Companion.toFont
import eu.europa.ec.resourceslogic.theme.templates.structures.ThemeFontStyle
import eu.europa.ec.resourceslogic.theme.templates.structures.ThemeFontStyle.Companion.toFontStyle
import eu.europa.ec.resourceslogic.theme.templates.structures.ThemeFontWeight
import eu.europa.ec.resourceslogic.theme.templates.structures.ThemeFontWeight.Companion.toFontWeight
import eu.europa.ec.resourceslogic.theme.templates.structures.ThemeTextAlign
import eu.europa.ec.resourceslogic.theme.templates.structures.ThemeTextAlign.Companion.toTextAlign
import eu.europa.ec.resourceslogic.theme.templates.structures.ThemeTextDecoration
import eu.europa.ec.resourceslogic.theme.templates.structures.ThemeTextDecoration.Companion.toTextDecoration

data class ThemeTypographyTemplate(
    val displayLarge: ThemeTextStyle,
    val displayMedium: ThemeTextStyle,
    val displaySmall: ThemeTextStyle,
    val headlineLarge: ThemeTextStyle,
    val headlineMedium: ThemeTextStyle,
    val headlineSmall: ThemeTextStyle,
    val titleLarge: ThemeTextStyle,
    val titleMedium: ThemeTextStyle,
    val titleSmall: ThemeTextStyle,
    val bodyLarge: ThemeTextStyle,
    val bodyMedium: ThemeTextStyle,
    val bodySmall: ThemeTextStyle,
    val labelLarge: ThemeTextStyle,
    val labelMedium: ThemeTextStyle,
    val labelSmall: ThemeTextStyle
) {
    companion object {
        fun ThemeTypographyTemplate.toTypography(): Typography = Typography(
            displayLarge = displayLarge.toTextStyle(),
            displayMedium = displayMedium.toTextStyle(),
            displaySmall = displaySmall.toTextStyle(),
            headlineLarge = headlineLarge.toTextStyle(),
            headlineMedium = headlineMedium.toTextStyle(),
            headlineSmall = headlineSmall.toTextStyle(),
            titleLarge = titleLarge.toTextStyle(),
            titleMedium = titleMedium.toTextStyle(),
            titleSmall = titleSmall.toTextStyle(),
            bodyLarge = bodyLarge.toTextStyle(),
            bodyMedium = bodyMedium.toTextStyle(),
            bodySmall = bodySmall.toTextStyle(),
            labelLarge = labelLarge.toTextStyle(),
            labelMedium = labelMedium.toTextStyle(),
            labelSmall = labelSmall.toTextStyle()
        )
    }
}

data class ThemeTextStyle(
    val color: Long? = null,
    val fontSize: Long? = null,
    val fontWeight: ThemeFontWeight? = null,
    val fontStyle: ThemeFontStyle? = null,
    val fontFamily: List<ThemeFont>? = null,
    val letterSpacing: Float? = null,
    val background: Long? = null,
    val textDecoration: ThemeTextDecoration? = null,
    val textAlign: ThemeTextAlign? = null,
) {
    companion object {
        fun ThemeTextStyle.toTextStyle(): TextStyle {
            // Transform values.
            val mColor = try {
                when (color) {
                    null -> Color.Unspecified
                    else -> Color(color)
                }
            } catch (exception: Exception) {
                throw IllegalArgumentException("Invalid color $color")
            }
            val mFontSize = try {
                when (fontSize) {
                    null -> TextUnit.Unspecified
                    else -> TextUnit(
                        value = fontSize.toFloat(),
                        type = TextUnitType.Sp
                    )
                }
            } catch (exception: Exception) {
                throw IllegalArgumentException("Invalid fontSize $fontSize")
            }
            val mLetterSpacing = try {
                when (letterSpacing) {
                    null -> TextUnit.Unspecified
                    else -> TextUnit(
                        value = letterSpacing,
                        type = TextUnitType.Sp
                    )
                }
            } catch (exception: Exception) {
                throw IllegalArgumentException("Invalid letterSpacing $letterSpacing")
            }
            val mBackground = try {
                when (color) {
                    null -> Color.Unspecified
                    else -> Color(color)
                }
            } catch (exception: Exception) {
                throw IllegalArgumentException("Invalid background $background")
            }

            val mFontFamily = when {
                fontFamily.isNullOrEmpty() -> {
                    FontFamily.Default
                }

                else -> FontFamily(
                    fontFamily.map { font -> font.toFont() }
                )
            }

            return TextStyle(
                color = mColor,
                fontSize = mFontSize,
                fontWeight = fontWeight?.toFontWeight(),
                fontStyle = fontStyle?.toFontStyle(),
                fontFamily = mFontFamily,
                letterSpacing = mLetterSpacing,
                background = mBackground,
                textDecoration = textDecoration?.toTextDecoration(),
                textAlign = textAlign?.toTextAlign() ?: TextAlign.Unspecified
            )
        }
    }
}