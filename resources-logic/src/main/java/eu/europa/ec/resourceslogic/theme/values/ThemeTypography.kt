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
                        fontFamily = listOf(RobotoLight),
                        fontSize = 96,
                        letterSpacing = -1.5f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    displayMedium = ThemeTextStyle(
                        fontFamily = listOf(RobotoLight),
                        fontSize = 60,
                        letterSpacing = -0.5f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    displaySmall = ThemeTextStyle(
                        fontFamily = listOf(RobotoRegular),
                        fontSize = 48,
                        letterSpacing = 0f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    headlineLarge = ThemeTextStyle(),
                    headlineMedium = ThemeTextStyle(
                        fontFamily = listOf(RobotoRegular),
                        fontSize = 30,
                        letterSpacing = 0.25f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    headlineSmall = ThemeTextStyle(
                        fontFamily = listOf(RobotoMedium),
                        fontSize = 24,
                        letterSpacing = 0f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    titleLarge = ThemeTextStyle(
                        fontFamily = listOf(RobotoMedium),
                        fontSize = 20,
                        letterSpacing = 0.15f,
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
                    bodyLarge = ThemeTextStyle(
                        fontFamily = listOf(RobotoRegular),
                        fontSize = 16,
                        letterSpacing = 0.5f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    bodyMedium = ThemeTextStyle(
                        fontFamily = listOf(RobotoRegular),
                        fontSize = 16,
                        letterSpacing = 0.25f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    bodySmall = ThemeTextStyle(
                        fontFamily = listOf(RobotoRegular),
                        fontSize = 12,
                        letterSpacing = 0.4f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    labelLarge = ThemeTextStyle(
                        fontFamily = listOf(RobotoMedium),
                        fontSize = 14,
                        letterSpacing = 1.25f,
                        textAlign = ThemeTextAlign.Start
                    ),
                    labelMedium = ThemeTextStyle(),
                    labelSmall = ThemeTextStyle(
                        fontFamily = listOf(RobotoRegular),
                        fontSize = 10,
                        letterSpacing = 1.5f,
                        textAlign = ThemeTextAlign.Start
                    )
                )
            }
    }
}

internal val RobotoLight = ThemeFont(
    res = R.font.roboto_light,
    weight = ThemeFontWeight.W300,
    style = ThemeFontStyle.Normal,
)
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

/*
--M2--         --M3--                  --DS--
h1          displayLarge        H1
h2	        displayMedium       H2
h3	        displaySmall        H3
N/A	        headlineLarge       N/A
h4	        headlineMedium      H4
h5	        headlineSmall       H5
h6	        titleLarge          Roboto Medium 20dp
subtitle1   titleMedium         Roboto Medium 16dp
subtitle2   titleSmall          Roboto Medium 14dp
body1	    bodyLarge           Body 1
body2	    bodyMedium          Body 2
caption	    bodySmall           Roboto Regular 12dp
button	    labelLarge          BUTTON
N/A	        labelMedium         N/A
overline    labelSmall          OVERLINE
*/