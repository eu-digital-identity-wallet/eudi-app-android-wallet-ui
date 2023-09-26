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
        private const val DarkGrey = 0xFF303030
        private const val LightGrey = 0xFFC2C4C4
        private const val Blue = 0xFF1A438F
        private const val Black = 0xFF000000
        private const val White = 0xFFFFFFFF
        private const val Red = 0xFFFF0000

        val lightColors = ThemeColorsTemplate(
            primary = Blue,
            onPrimary = White,
            primaryContainer = Blue,
            onPrimaryContainer = White,
            inversePrimary = Blue,
            secondary = Blue,
            onSecondary = Blue,
            secondaryContainer = Blue,
            onSecondaryContainer = Blue,
            tertiary = Blue,
            onTertiary = Blue,
            tertiaryContainer = Blue,
            onTertiaryContainer = Blue,
            background = White,
            onBackground = Black,
            surface = White,
            onSurface = Black,
            surfaceVariant = Blue,
            onSurfaceVariant = Blue,
            surfaceTint = Blue,
            inverseSurface = Blue,
            inverseOnSurface = Blue,
            error = Blue,
            onError = Blue,
            errorContainer = Blue,
            onErrorContainer = Blue,
            outline = Blue,
            outlineVariant = Blue,
            scrim = Blue,
        )

        val darkColors = ThemeColorsTemplate(
            primary = Red,
            onPrimary = White,
            primaryContainer = Blue,
            onPrimaryContainer = White,
            inversePrimary = Blue,
            secondary = Blue,
            onSecondary = Blue,
            secondaryContainer = Blue,
            onSecondaryContainer = Blue,
            tertiary = Blue,
            onTertiary = Blue,
            tertiaryContainer = Blue,
            onTertiaryContainer = Blue,
            background = Black,
            onBackground = White,
            surface = DarkGrey,
            onSurface = White,
            surfaceVariant = Blue,
            onSurfaceVariant = Blue,
            surfaceTint = Blue,
            inverseSurface = Blue,
            inverseOnSurface = Blue,
            error = Blue,
            onError = Blue,
            errorContainer = Blue,
            onErrorContainer = Blue,
            outline = Blue,
            outlineVariant = Blue,
            scrim = Blue,
        )
    }
}