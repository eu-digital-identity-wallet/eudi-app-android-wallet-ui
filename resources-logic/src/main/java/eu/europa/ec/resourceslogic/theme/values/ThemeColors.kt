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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import eu.europa.ec.resourceslogic.theme.ThemeManager
import eu.europa.ec.resourceslogic.theme.templates.ThemeColorsTemplate

private val isInDarkMode: Boolean
    get() {
        return ThemeManager.instance.set.isInDarkMode
    }

class ThemeColors {
    companion object {
        private const val white = 0xFFFFFFFF
        private const val black = 0xFF000000
        private const val primary = 0xFF803589
        private const val secondary = 0xFF2F1E5C
        private const val tertiary = 0xFFF3C126
        private const val error = 0xFFC56519
        private const val success = 0xFF02797C
        const val primaryMainGradientStart = secondary
        const val primaryMainGradientEnd = primary
        internal const val eudiw_theme_light_success = success
        internal const val eudiw_theme_light_onSuccess = white
        internal const val eudiw_theme_dark_success = success
        internal const val eudiw_theme_dark_onSuccess = white
        internal const val eudiw_theme_light_textPrimaryDark: Long = 0xDE000000
        internal const val eudiw_theme_light_textSecondaryDark: Long = 0x8A000000
        internal const val eudiw_theme_light_textDisabledDark: Long = 0x61000000
        internal const val eudiw_theme_light_dividerDark: Long = 0x1F000000
        internal const val eudiw_theme_light_backgroundDefault: Long = 0x0A000000
        internal const val eudiw_theme_light_backgroundPaper: Long = 0xFFFFFFFF
        internal const val eudiw_theme_light_textSecondaryLight: Long = 0xB3FFFFFF
        internal const val eudiw_theme_light_textDisabledLight: Long = 0x80FFFFFF
        internal const val eudiw_theme_dark_textPrimaryDark: Long = 0xDEFFFFFF
        internal const val eudiw_theme_dark_textSecondaryDark: Long = 0x8AFFFFFF
        internal const val eudiw_theme_dark_textDisabledDark: Long = 0x61FFFFFF
        internal const val eudiw_theme_dark_dividerDark: Long = 0x1FFFFFFF
        internal const val eudiw_theme_dark_backgroundDefault: Long = 0x0AFFFFFF
        internal const val eudiw_theme_dark_backgroundPaper: Long = 0xFF000000
        internal const val eudiw_theme_dark_textSecondaryLight: Long = 0xB3000000
        internal const val eudiw_theme_dark_textDisabledLight: Long = 0x80000000

        private const val eudiw_theme_light_primary = primary
        private const val eudiw_theme_light_onPrimary = white
        private const val eudiw_theme_light_primaryContainer = 0xFFFFD6FD
        private const val eudiw_theme_light_onPrimaryContainer = 0xFF36003E
        private const val eudiw_theme_light_secondary = secondary
        private const val eudiw_theme_light_onSecondary = white
        private const val eudiw_theme_light_secondaryContainer = 0xFFE8DDFF
        private const val eudiw_theme_light_onSecondaryContainer = 0xFF21005D
        private const val eudiw_theme_light_tertiary = tertiary
        private const val eudiw_theme_light_onTertiary = black
        private const val eudiw_theme_light_tertiaryContainer = 0xFFFFDF92
        private const val eudiw_theme_light_onTertiaryContainer = 0xFF241A00
        private const val eudiw_theme_light_error = error
        private const val eudiw_theme_light_errorContainer = 0xFFffdbc8
        private const val eudiw_theme_light_onError = white
        private const val eudiw_theme_light_onErrorContainer = 0xFF311300
        private const val eudiw_theme_light_background = white
        private const val eudiw_theme_light_onBackground = black
        private const val eudiw_theme_light_surface = white
        private const val eudiw_theme_light_onSurface = black
        private const val eudiw_theme_light_surfaceVariant = 0xFFEDDFE8
        private const val eudiw_theme_light_onSurfaceVariant = 0xFF4D444C
        private const val eudiw_theme_light_outline = 0xFF7F747C
        private const val eudiw_theme_light_inverseOnSurface = 0xFFF7EEF2
        private const val eudiw_theme_light_inverseSurface = 0xFF332F32
        private const val eudiw_theme_light_inversePrimary = 0xFFFBAAFF
        private const val eudiw_theme_light_surfaceTint = 0xFF8B3F94
        private const val eudiw_theme_light_outlineVariant = 0xFFD0C3CC
        private const val eudiw_theme_light_scrim = black

        private const val eudiw_theme_dark_primary = 0xFFFBAAFF
        private const val eudiw_theme_dark_onPrimary = 0xFF560761
        private const val eudiw_theme_dark_primaryContainer = 0xFF70267A
        private const val eudiw_theme_dark_onPrimaryContainer = 0xFFFFD6FD
        private const val eudiw_theme_dark_secondary = 0xFFCEBDFF
        private const val eudiw_theme_dark_onSecondary = 0xFF371E73
        private const val eudiw_theme_dark_secondaryContainer = 0xFF4E378B
        private const val eudiw_theme_dark_onSecondaryContainer = 0xFFE8DDFF
        private const val eudiw_theme_dark_tertiary = 0xFFF2C025
        private const val eudiw_theme_dark_onTertiary = 0xFF3E2E00
        private const val eudiw_theme_dark_tertiaryContainer = 0xFF594400
        private const val eudiw_theme_dark_onTertiaryContainer = 0xFFFFDF92
        private const val eudiw_theme_dark_error = 0xFFffb689
        private const val eudiw_theme_dark_errorContainer = 0xFF743500
        private const val eudiw_theme_dark_onError = 0xFF512300
        private const val eudiw_theme_dark_onErrorContainer = 0xFFffdbc8
        private const val eudiw_theme_dark_background = black
        private const val eudiw_theme_dark_onBackground = white
        private const val eudiw_theme_dark_surface = black
        private const val eudiw_theme_dark_onSurface = white
        private const val eudiw_theme_dark_surfaceVariant = 0xFF4D444C
        private const val eudiw_theme_dark_onSurfaceVariant = 0xFFD0C3CC
        private const val eudiw_theme_dark_outline = 0xFF998D96
        private const val eudiw_theme_dark_inverseOnSurface = black
        private const val eudiw_theme_dark_inverseSurface = white
        private const val eudiw_theme_dark_inversePrimary = 0xFF8B3F94
        private const val eudiw_theme_dark_surfaceTint = 0xFFFBAAFF
        private const val eudiw_theme_dark_outlineVariant = 0xFF4D444C
        private const val eudiw_theme_dark_scrim = black

        val lightColors = ThemeColorsTemplate(
            primary = eudiw_theme_light_primary,
            onPrimary = eudiw_theme_light_onPrimary,
            primaryContainer = eudiw_theme_light_primaryContainer,
            onPrimaryContainer = eudiw_theme_light_onPrimaryContainer,
            secondary = eudiw_theme_light_secondary,
            onSecondary = eudiw_theme_light_onSecondary,
            secondaryContainer = eudiw_theme_light_secondaryContainer,
            onSecondaryContainer = eudiw_theme_light_onSecondaryContainer,
            tertiary = eudiw_theme_light_tertiary,
            onTertiary = eudiw_theme_light_onTertiary,
            tertiaryContainer = eudiw_theme_light_tertiaryContainer,
            onTertiaryContainer = eudiw_theme_light_onTertiaryContainer,
            error = eudiw_theme_light_error,
            errorContainer = eudiw_theme_light_errorContainer,
            onError = eudiw_theme_light_onError,
            onErrorContainer = eudiw_theme_light_onErrorContainer,
            background = eudiw_theme_light_background,
            onBackground = eudiw_theme_light_onBackground,
            surface = eudiw_theme_light_surface,
            onSurface = eudiw_theme_light_onSurface,
            surfaceVariant = eudiw_theme_light_surfaceVariant,
            onSurfaceVariant = eudiw_theme_light_onSurfaceVariant,
            outline = eudiw_theme_light_outline,
            inverseOnSurface = eudiw_theme_light_inverseOnSurface,
            inverseSurface = eudiw_theme_light_inverseSurface,
            inversePrimary = eudiw_theme_light_inversePrimary,
            surfaceTint = eudiw_theme_light_surfaceTint,
            outlineVariant = eudiw_theme_light_outlineVariant,
            scrim = eudiw_theme_light_scrim,
        )

        val darkColors = ThemeColorsTemplate(
            primary = eudiw_theme_dark_primary,
            onPrimary = eudiw_theme_dark_onPrimary,
            primaryContainer = eudiw_theme_dark_primaryContainer,
            onPrimaryContainer = eudiw_theme_dark_onPrimaryContainer,
            secondary = eudiw_theme_dark_secondary,
            onSecondary = eudiw_theme_dark_onSecondary,
            secondaryContainer = eudiw_theme_dark_secondaryContainer,
            onSecondaryContainer = eudiw_theme_dark_onSecondaryContainer,
            tertiary = eudiw_theme_dark_tertiary,
            onTertiary = eudiw_theme_dark_onTertiary,
            tertiaryContainer = eudiw_theme_dark_tertiaryContainer,
            onTertiaryContainer = eudiw_theme_dark_onTertiaryContainer,
            error = eudiw_theme_dark_error,
            errorContainer = eudiw_theme_dark_errorContainer,
            onError = eudiw_theme_dark_onError,
            onErrorContainer = eudiw_theme_dark_onErrorContainer,
            background = eudiw_theme_dark_background,
            onBackground = eudiw_theme_dark_onBackground,
            surface = eudiw_theme_dark_surface,
            onSurface = eudiw_theme_dark_onSurface,
            surfaceVariant = eudiw_theme_dark_surfaceVariant,
            onSurfaceVariant = eudiw_theme_dark_onSurfaceVariant,
            outline = eudiw_theme_dark_outline,
            inverseOnSurface = eudiw_theme_dark_inverseOnSurface,
            inverseSurface = eudiw_theme_dark_inverseSurface,
            inversePrimary = eudiw_theme_dark_inversePrimary,
            surfaceTint = eudiw_theme_dark_surfaceTint,
            outlineVariant = eudiw_theme_dark_outlineVariant,
            scrim = eudiw_theme_dark_scrim,
        )

        val textPrimaryDark: Color
            get() = if (isInDarkMode) {
                Color(eudiw_theme_dark_textPrimaryDark)
            } else {
                Color(eudiw_theme_light_textPrimaryDark)
            }

        val textSecondaryDark: Color
            get() = if (isInDarkMode) {
                Color(eudiw_theme_dark_textSecondaryDark)
            } else {
                Color(eudiw_theme_light_textSecondaryDark)
            }

        val textDisabledDark: Color
            get() = if (isInDarkMode) {
                Color(eudiw_theme_dark_textDisabledDark)
            } else {
                Color(eudiw_theme_light_textDisabledDark)
            }

        val dividerDark: Color
            get() = if (isInDarkMode) {
                Color(eudiw_theme_dark_dividerDark)
            } else {
                Color(eudiw_theme_light_dividerDark)
            }

        val backgroundDefault: Color
            get() = if (isInDarkMode) {
                Color(eudiw_theme_dark_backgroundDefault)
            } else {
                Color(eudiw_theme_light_backgroundDefault)
            }

        val backgroundPaper: Color
            get() = if (isInDarkMode) {
                Color(eudiw_theme_dark_backgroundPaper)
            } else {
                Color(eudiw_theme_light_backgroundPaper)
            }

        val textSecondaryLight: Color
            get() = if (isInDarkMode) {
                Color(eudiw_theme_dark_textSecondaryLight)
            } else {
                Color(eudiw_theme_light_textSecondaryLight)
            }

        val textDisabledLight: Color
            get() = if (isInDarkMode) {
                Color(eudiw_theme_dark_textDisabledLight)
            } else {
                Color(eudiw_theme_light_textDisabledLight)
            }

        val gradientBackground: Brush
            @Composable get() = Brush.linearGradient(
                colors = listOf(
                    Color(primaryMainGradientStart),
                    Color(primaryMainGradientEnd)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
    }
}

val ColorScheme.textPrimaryDark: Color
    @Composable get() = if (isSystemInDarkTheme()) {
        Color(ThemeColors.eudiw_theme_dark_textPrimaryDark)
    } else {
        Color(ThemeColors.eudiw_theme_light_textPrimaryDark)
    }

val ColorScheme.textSecondaryDark: Color
    @Composable get() = if (isSystemInDarkTheme()) {
        Color(ThemeColors.eudiw_theme_dark_textSecondaryDark)
    } else {
        Color(ThemeColors.eudiw_theme_light_textSecondaryDark)
    }

val ColorScheme.textDisabledDark: Color
    @Composable get() = if (isSystemInDarkTheme()) {
        Color(ThemeColors.eudiw_theme_dark_textDisabledDark)
    } else {
        Color(ThemeColors.eudiw_theme_light_textDisabledDark)
    }

val ColorScheme.dividerDark: Color
    @Composable get() = if (isSystemInDarkTheme()) {
        Color(ThemeColors.eudiw_theme_dark_dividerDark)
    } else {
        Color(ThemeColors.eudiw_theme_light_dividerDark)
    }

val ColorScheme.backgroundDefault: Color
    @Composable get() = if (isSystemInDarkTheme()) {
        Color(ThemeColors.eudiw_theme_dark_backgroundDefault)
    } else {
        Color(ThemeColors.eudiw_theme_light_backgroundDefault)
    }

val ColorScheme.backgroundPaper: Color
    @Composable get() = if (isSystemInDarkTheme()) {
        Color(ThemeColors.eudiw_theme_dark_backgroundPaper)
    } else {
        Color(ThemeColors.eudiw_theme_light_backgroundPaper)
    }

val ColorScheme.textSecondaryLight: Color
    @Composable get() = if (isSystemInDarkTheme()) {
        Color(ThemeColors.eudiw_theme_dark_textSecondaryLight)
    } else {
        Color(ThemeColors.eudiw_theme_light_textSecondaryLight)
    }

val ColorScheme.textDisabledLight: Color
    @Composable get() = if (isSystemInDarkTheme()) {
        Color(ThemeColors.eudiw_theme_dark_textDisabledLight)
    } else {
        Color(ThemeColors.eudiw_theme_light_textDisabledLight)
    }

val ColorScheme.colorSuccess: Color
    @Composable get() = if (isSystemInDarkTheme()) {
        Color(ThemeColors.eudiw_theme_light_success)
    } else {
        Color(ThemeColors.eudiw_theme_dark_success)
    }

val ColorScheme.colorOnSuccess: Color
    @Composable get() = if (isSystemInDarkTheme()) {
        Color(ThemeColors.eudiw_theme_light_onSuccess)
    } else {
        Color(ThemeColors.eudiw_theme_dark_onSuccess)
    }