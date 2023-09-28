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

package eu.europa.ec.resourceslogic.theme.values

import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.templates.ThemeTextStyle
import eu.europa.ec.resourceslogic.theme.templates.ThemeTypographyTemplate
import eu.europa.ec.resourceslogic.theme.templates.structures.ThemeFont
import eu.europa.ec.resourceslogic.theme.templates.structures.ThemeFontStyle
import eu.europa.ec.resourceslogic.theme.templates.structures.ThemeFontWeight
import eu.europa.ec.resourceslogic.theme.templates.structures.ThemeTextAlign

class ThemeTypography {
    companion object {
        val typo: ThemeTypographyTemplate
            get() {
                return ThemeTypographyTemplate(
                    displayLarge = ThemeTextStyle(
                        color = null,
                        fontSize = 20,
                        fontFamily = listOf(
                            ThemeFont(
                                res = RobotoMedium,
                                weight = ThemeFontWeight.W400,
                                style = ThemeFontStyle.Normal,
                            )
                        ),
                        textAlign = ThemeTextAlign.Left
                    ),
                    displayMedium = ThemeTextStyle(
                        color = null,
                        fontSize = 20,
                        fontFamily = listOf(
                            ThemeFont(
                                res = RobotoMedium,
                                weight = ThemeFontWeight.W400,
                                style = ThemeFontStyle.Normal,
                            )
                        ),
                        textAlign = ThemeTextAlign.Left
                    ),
                    displaySmall = ThemeTextStyle(
                        color = null,
                        fontSize = 16,
                        fontFamily = listOf(
                            ThemeFont(
                                res = RobotoRegular,
                                weight = ThemeFontWeight.W400,
                                style = ThemeFontStyle.Normal,
                            )
                        ),
                        textAlign = ThemeTextAlign.Left
                    ),
                    headlineLarge = ThemeTextStyle(),
                    headlineMedium = ThemeTextStyle(
                        color = null,
                        fontSize = 12,
                        fontFamily = listOf(
                            ThemeFont(
                                res = RobotoRegular,
                                weight = ThemeFontWeight.W400,
                                style = ThemeFontStyle.Normal,
                            )
                        ),
                        textAlign = ThemeTextAlign.Left
                    ),
                    headlineSmall = ThemeTextStyle(
                        color = null,
                        fontSize = 12,
                        fontFamily = listOf(
                            ThemeFont(
                                res = RobotoBold,
                                weight = ThemeFontWeight.W400,
                                style = ThemeFontStyle.Normal,
                            )
                        ),
                        textAlign = ThemeTextAlign.Left
                    ),
                    titleLarge = ThemeTextStyle(
                        color = null,
                        fontSize = 32,
                        fontFamily = listOf(
                            ThemeFont(
                                res = RobotoMedium,
                                weight = ThemeFontWeight.W400,
                                style = ThemeFontStyle.Normal,
                            )
                        ),
                        textAlign = ThemeTextAlign.Left
                    ),
                    titleMedium = ThemeTextStyle(
                        color = null,
                        fontSize = 24,
                        fontFamily = listOf(
                            ThemeFont(
                                res = RobotoMedium,
                                weight = ThemeFontWeight.W400,
                                style = ThemeFontStyle.Normal,
                            )
                        ),
                        textAlign = ThemeTextAlign.Left
                    ),
                    titleSmall = ThemeTextStyle(
                        color = null,
                        fontSize = 24,
                        fontFamily = listOf(
                            ThemeFont(
                                res = RobotoMedium,
                                weight = ThemeFontWeight.W400,
                                style = ThemeFontStyle.Normal,
                            )
                        ),
                        textAlign = ThemeTextAlign.Left
                    ),
                    bodyLarge = ThemeTextStyle(
                        color = null,
                        fontSize = 14,
                        fontFamily = listOf(
                            ThemeFont(
                                res = RobotoRegular,
                                weight = ThemeFontWeight.W400,
                                style = ThemeFontStyle.Normal,
                            )
                        ),
                        textAlign = ThemeTextAlign.Left
                    ),
                    bodyMedium = ThemeTextStyle(
                        color = null,
                        fontSize = 14,
                        fontFamily = listOf(
                            ThemeFont(
                                res = RobotoRegular,
                                weight = ThemeFontWeight.W400,
                                style = ThemeFontStyle.Normal,
                            )
                        ),
                        textAlign = ThemeTextAlign.Left
                    ),
                    bodySmall = ThemeTextStyle(),
                    labelLarge = ThemeTextStyle(),
                    labelMedium = ThemeTextStyle(),
                    labelSmall = ThemeTextStyle()
                )
            }
    }
}

internal val RobotoRegular = R.font.roboto_regular
internal val RobotoMedium = R.font.roboto_medium
internal val RobotoBold = R.font.roboto_bold