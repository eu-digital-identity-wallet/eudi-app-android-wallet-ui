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

package eu.europa.ec.resourceslogic.theme.templates

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Data class that contains the colors of the theme used to initialize the
 * theme manager.
 */
data class ThemeColorsTemplate(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val inversePrimary: Long,
    val secondary: Long,
    val onSecondary: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
    val tertiary: Long,
    val onTertiary: Long,
    val tertiaryContainer: Long,
    val onTertiaryContainer: Long,
    val background: Long,
    val onBackground: Long,
    val surface: Long,
    val onSurface: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val surfaceTint: Long,
    val inverseSurface: Long,
    val inverseOnSurface: Long,
    val error: Long,
    val onError: Long,
    val errorContainer: Long,
    val onErrorContainer: Long,
    val outline: Long,
    val outlineVariant: Long,
    val scrim: Long
) {
    companion object {
        fun ThemeColorsTemplate.toColorScheme(): ColorScheme = ColorScheme(
            primary = Color(primary),
            onPrimary = Color(onPrimary),
            primaryContainer = Color(primaryContainer),
            onPrimaryContainer = Color(onPrimaryContainer),
            inversePrimary = Color(inversePrimary),
            secondary = Color(secondary),
            onSecondary = Color(onSecondary),
            secondaryContainer = Color(secondaryContainer),
            onSecondaryContainer = Color(onSecondaryContainer),
            tertiary = Color(tertiary),
            onTertiary = Color(onTertiary),
            tertiaryContainer = Color(tertiaryContainer),
            onTertiaryContainer = Color(onTertiaryContainer),
            background = Color(background),
            onBackground = Color(onBackground),
            surface = Color(surface),
            onSurface = Color(onSurface),
            surfaceVariant = Color(surfaceVariant),
            onSurfaceVariant = Color(onSurfaceVariant),
            surfaceTint = Color(surfaceTint),
            inverseSurface = Color(inverseSurface),
            inverseOnSurface = Color(inverseOnSurface),
            error = Color(error),
            onError = Color(onError),
            errorContainer = Color(errorContainer),
            onErrorContainer = Color(onErrorContainer),
            outline = Color(outline),
            outlineVariant = Color(outlineVariant),
            scrim = Color(scrim),
        )
    }
}