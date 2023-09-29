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

import eu.europa.ec.resourceslogic.theme.templates.ThemeColorsTemplate

class ThemeColors {
    companion object {
        private const val eudiw_theme_light_darkGrey = 0xFF303030
        private const val eudiw_theme_light_lightGrey = 0xFFC2C4C4
        private const val eudiw_theme_light_blue = 0xFF1A438F
        private const val eudiw_theme_light_black = 0xFF000000
        private const val eudiw_theme_light_white = 0xFFFFFFFF
        private const val eudiw_theme_light_red = 0xFFFF0000

        private const val eudiw_theme_light_tertiaryMain = 0xFFF3C126
        private const val eudiw_theme_light_secondaryMain = 0xFF803589
        private const val eudiw_theme_light_successText = 0xFF02797C
        private const val eudiw_theme_light_backgroundDefault = 0xFF0000000A
        private const val eudiw_theme_light_text_secondaryDark = 0xFF0000008A
        private const val eudiw_theme_light_dividerDark = 0xFF0000001F
        private const val eudiw_theme_light_backgroundPaper = 0xFFFFFFFF
        private const val eudiw_theme_light_text_disabledDark = 0xFF00000061
        private const val eudiw_theme_light_lightBlue = 0xFF1266D8
        private const val eudiw_theme_light_gradientStart = 0xFF443187
        private const val eudiw_theme_light_gradientEnd = 0xFF803589
        private const val eudiw_theme_light_text_primaryDark = 0xFF000000DE
        private const val eudiw_theme_light_onError = 0xFFB00020

        private const val eudiw_theme_dark_tertiaryMain = 0xFFF3C126
        private const val eudiw_theme_dark_secondaryMain = 0xFF803589
        private const val eudiw_theme_dark_successText = 0xFF02797C
        private const val eudiw_theme_dark_backgroundDefault = 0xFF0000000A
        private const val eudiw_theme_dark_text_secondaryDark = 0xFF0000008A
        private const val eudiw_theme_dark_dividerDark = 0xFF0000001F
        private const val eudiw_theme_dark_backgroundPaper = 0xFFFFFFFF
        private const val eudiw_theme_dark_text_disabledDark = 0xFF00000061
        private const val eudiw_theme_dark_lightBlue = 0xFF1266D8
        private const val eudiw_theme_dark_gradientStart = 0xFF443187
        private const val eudiw_theme_dark_gradientEnd = 0xFF803589
        private const val eudiw_theme_dark_text_primaryDark = 0xFF000000DE
        private const val eudiw_theme_dark_onError = 0xFFB00020


        val lightColors = ThemeColorsTemplate(
            primary = eudiw_theme_light_secondaryMain,
            onPrimary = eudiw_theme_light_white,
            primaryContainer = eudiw_theme_light_blue,
            onPrimaryContainer = eudiw_theme_light_white,
            inversePrimary = eudiw_theme_light_blue,
            secondary = eudiw_theme_light_blue,
            onSecondary = eudiw_theme_light_blue,
            secondaryContainer = eudiw_theme_light_blue,
            onSecondaryContainer = eudiw_theme_light_blue,
            tertiary = eudiw_theme_light_tertiaryMain,
            onTertiary = eudiw_theme_light_blue,
            tertiaryContainer = eudiw_theme_light_blue,
            onTertiaryContainer = eudiw_theme_light_blue,
            background = eudiw_theme_light_white,
            onBackground = eudiw_theme_light_black,
            surface = eudiw_theme_light_white,
            onSurface = eudiw_theme_light_black,
            surfaceVariant = eudiw_theme_light_blue,
            onSurfaceVariant = eudiw_theme_light_blue,
            surfaceTint = eudiw_theme_light_blue,
            inverseSurface = eudiw_theme_light_blue,
            inverseOnSurface = eudiw_theme_light_blue,
            error = eudiw_theme_light_blue,
            onError = eudiw_theme_light_blue,
            errorContainer = eudiw_theme_light_blue,
            onErrorContainer = eudiw_theme_light_blue,
            outline = eudiw_theme_light_blue,
            outlineVariant = eudiw_theme_light_blue,
            scrim = eudiw_theme_light_blue,
        )

        val darkColors = ThemeColorsTemplate(
            primary = eudiw_theme_light_red,
            onPrimary = eudiw_theme_light_white,
            primaryContainer = eudiw_theme_light_blue,
            onPrimaryContainer = eudiw_theme_light_white,
            inversePrimary = eudiw_theme_light_blue,
            secondary = eudiw_theme_light_blue,
            onSecondary = eudiw_theme_light_blue,
            secondaryContainer = eudiw_theme_light_blue,
            onSecondaryContainer = eudiw_theme_light_blue,
            tertiary = eudiw_theme_light_blue,
            onTertiary = eudiw_theme_light_blue,
            tertiaryContainer = eudiw_theme_light_blue,
            onTertiaryContainer = eudiw_theme_light_blue,
            background = eudiw_theme_light_black,
            onBackground = eudiw_theme_light_white,
            surface = eudiw_theme_light_darkGrey,
            onSurface = eudiw_theme_light_white,
            surfaceVariant = eudiw_theme_light_blue,
            onSurfaceVariant = eudiw_theme_light_blue,
            surfaceTint = eudiw_theme_light_blue,
            inverseSurface = eudiw_theme_light_blue,
            inverseOnSurface = eudiw_theme_light_blue,
            error = eudiw_theme_light_blue,
            onError = eudiw_theme_light_blue,
            errorContainer = eudiw_theme_light_blue,
            onErrorContainer = eudiw_theme_light_blue,
            outline = eudiw_theme_light_blue,
            outlineVariant = eudiw_theme_light_blue,
            scrim = eudiw_theme_light_blue,
        )
    }
}