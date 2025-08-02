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

package eu.europa.ec.resourceslogic.theme.values

import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.templates.ThemeTextStyle
import eu.europa.ec.resourceslogic.theme.templates.ThemeTypographyTemplate
import eu.europa.ec.resourceslogic.theme.templates.structures.ThemeFont
import eu.europa.ec.resourceslogic.theme.templates.structures.ThemeFontStyle
import eu.europa.ec.resourceslogic.theme.templates.structures.ThemeFontWeight
import eu.europa.ec.resourceslogic.theme.templates.structures.ThemeTextAlign

internal class ThemeTypography {
    companion object {
        val typo: ThemeTypographyTemplate
            get() {
                return ThemeTypographyTemplate(
                    displayLarge = ThemeTextStyle(
                        fontFamily = listOf(RobotoRegular),
                        fontSize = 57,
                        letterSpacing = -0.25f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    displayMedium = ThemeTextStyle(
                        fontFamily = listOf(RobotoRegular),
                        fontSize = 45,
                        letterSpacing = 0f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    displaySmall = ThemeTextStyle(
                        fontFamily = listOf(RobotoRegular),
                        fontSize = 36,
                        letterSpacing = 0f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    headlineLarge = ThemeTextStyle(
                        fontFamily = listOf(RobotoRegular),
                        fontSize = 32,
                        letterSpacing = 0f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    headlineMedium = ThemeTextStyle(
                        fontFamily = listOf(RobotoRegular),
                        fontSize = 28,
                        letterSpacing = 0f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    headlineSmall = ThemeTextStyle(
                        fontFamily = listOf(RobotoRegular),
                        fontSize = 24,
                        letterSpacing = 0f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    titleLarge = ThemeTextStyle(
                        fontFamily = listOf(RobotoRegular),
                        fontSize = 22,
                        letterSpacing = 0f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    titleMedium = ThemeTextStyle(
                        fontFamily = listOf(RobotoMedium),
                        fontSize = 16,
                        letterSpacing = 0.15f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    titleSmall = ThemeTextStyle(
                        fontFamily = listOf(RobotoMedium),
                        fontSize = 14,
                        letterSpacing = 0.1f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    labelLarge = ThemeTextStyle(
                        fontFamily = listOf(RobotoMedium),
                        fontSize = 14,
                        letterSpacing = 0.1f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    labelMedium = ThemeTextStyle(
                        fontFamily = listOf(RobotoMedium),
                        fontSize = 12,
                        letterSpacing = 0.5f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    labelSmall = ThemeTextStyle(
                        fontFamily = listOf(RobotoMedium),
                        fontSize = 11,
                        letterSpacing = 0.5f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    bodyLarge = ThemeTextStyle(
                        fontFamily = listOf(RobotoRegular),
                        fontSize = 16,
                        letterSpacing = 0.5f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    bodyMedium = ThemeTextStyle(
                        fontFamily = listOf(RobotoRegular),
                        fontSize = 14,
                        letterSpacing = 0.25f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    bodySmall = ThemeTextStyle(
                        fontFamily = listOf(RobotoRegular),
                        fontSize = 12,
                        letterSpacing = 0.4f,
                        textAlign = ThemeTextAlign.Start
                    )
                )
            }
    }
}

internal val RobotoRegular = ThemeFont(
    res = R.font.roboto_regular,
    weight = ThemeFontWeight.W400,
    style = ThemeFontStyle.Normal,
)
internal val RobotoMedium = ThemeFont(
    res = R.font.roboto_medium,
    weight = ThemeFontWeight.W500,
    style = ThemeFontStyle.Normal,
)