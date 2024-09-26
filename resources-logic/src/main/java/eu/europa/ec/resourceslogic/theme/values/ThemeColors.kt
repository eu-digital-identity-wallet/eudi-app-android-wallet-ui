/*
 * Copyright (c) 2023 European Commission
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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import eu.europa.ec.resourceslogic.theme.ThemeManager
import eu.europa.ec.resourceslogic.theme.templates.ThemeColorsTemplate

private val isInDarkMode: Boolean
    get() {
        return ThemeManager.instance.set.isInDarkMode
    }

class ThemeColors {
    companion object {
        private const val white: Long = 0xFFFFFFFF
        private const val black: Long = 0xFF000000

        private const val eudiw_theme_light_primary: Long = 0xFF2A5ED9
        private const val eudiw_theme_light_secondary: Long = 0xFFD6D9F9
        internal const val eudiw_theme_light_success: Long = 0xFF55953B
        internal const val eudiw_theme_light_warning: Long = 0xFFF39626
        private const val eudiw_theme_light_error: Long = 0xFFDA2C27
        internal const val eudiw_theme_light_info: Long = 0xFF8145BD

        private const val eudiw_theme_dark_primary: Long = eudiw_theme_light_primary
        private const val eudiw_theme_dark_secondary: Long = eudiw_theme_light_secondary
        internal const val eudiw_theme_dark_success: Long = 0xFF93D875
        internal const val eudiw_theme_dark_warning: Long = 0xFFFFB871
        private const val eudiw_theme_dark_error: Long = 0xFFFFB4AA
        internal const val eudiw_theme_dark_info: Long = 0xFFDCB8FF

        internal const val eudiw_theme_light_onSuccess: Long = white
        internal const val eudiw_theme_dark_onSuccess: Long = 0xFF0E3900

        internal const val eudiw_theme_light_textPrimaryDark: Long = 0xDE000000
        internal const val eudiw_theme_light_textSecondaryDark: Long = 0x8A000000
        internal const val eudiw_theme_light_textDisabledDark: Long = 0x61000000
        internal const val eudiw_theme_light_dividerDark: Long = 0x1F000000
        internal const val eudiw_theme_light_backgroundDefault: Long = 0xFFF5F5F5
        internal const val eudiw_theme_light_textSecondaryLight: Long = 0xB3FFFFFF
        internal const val eudiw_theme_light_textDisabledLight: Long = 0x80FFFFFF

        internal const val eudiw_theme_dark_textPrimaryDark: Long = 0xDEFFFFFF
        internal const val eudiw_theme_dark_textSecondaryDark: Long = 0x8AFFFFFF
        internal const val eudiw_theme_dark_textDisabledDark: Long = 0xFF646670
        internal const val eudiw_theme_dark_dividerDark: Long = 0x1FFFFFFF
        internal const val eudiw_theme_dark_backgroundDefault: Long = 0xFF44474F
        internal const val eudiw_theme_dark_textSecondaryLight: Long = 0xB3000000
        internal const val eudiw_theme_dark_textDisabledLight: Long = 0x80000000

        private const val eudiw_theme_light_onPrimary: Long = white
        private const val eudiw_theme_light_primaryContainer: Long = 0xFFDBE1FF
        private const val eudiw_theme_light_onPrimaryContainer: Long = 0xFF00174B

        private const val eudiw_theme_light_onSecondary: Long = white
        private const val eudiw_theme_light_secondaryContainer: Long = 0xFFDEE0FF
        private const val eudiw_theme_light_onSecondaryContainer: Long = 0xFF00115A
        private const val eudiw_theme_light_tertiary: Long = eudiw_theme_light_secondary
        private const val eudiw_theme_light_onTertiary: Long = eudiw_theme_light_onSecondary
        private const val eudiw_theme_light_tertiaryContainer: Long =
            eudiw_theme_light_secondaryContainer
        private const val eudiw_theme_light_onTertiaryContainer: Long =
            eudiw_theme_light_onSecondaryContainer

        private const val eudiw_theme_light_errorContainer: Long = 0xFFFFDAD5
        private const val eudiw_theme_light_onError: Long = white
        private const val eudiw_theme_light_onErrorContainer: Long = 0xFF410002
        const val eudiw_theme_light_background: Long = white
        private const val eudiw_theme_light_onBackground: Long = black
        private const val eudiw_theme_light_surface: Long = white
        private const val eudiw_theme_light_onSurface: Long = black
        private const val eudiw_theme_light_surfaceVariant: Long = 0xFFE2E2EC
        private const val eudiw_theme_light_onSurfaceVariant: Long = 0xFF45464F
        private const val eudiw_theme_light_outline: Long = 0xFF757680
        private const val eudiw_theme_light_inverseOnSurface: Long = 0xFFF1EFFF
        private const val eudiw_theme_light_inverseSurface: Long = 0xFF1E2578
        private const val eudiw_theme_light_inversePrimary: Long = 0xFFADC6FF
        private const val eudiw_theme_light_surfaceTint: Long = eudiw_theme_light_surface
        private const val eudiw_theme_light_outlineVariant: Long = 0xFFC4C6D0
        private const val eudiw_theme_light_scrim: Long = black

        private const val eudiw_theme_dark_onPrimary: Long = 0xFF002A78
        private const val eudiw_theme_dark_primaryContainer: Long = 0xFF003DA9
        private const val eudiw_theme_dark_onPrimaryContainer: Long = 0xFFDBE1FF

        private const val eudiw_theme_dark_onSecondary: Long = 0xFF162778
        private const val eudiw_theme_dark_secondaryContainer: Long = 0xFF303F90
        private const val eudiw_theme_dark_onSecondaryContainer: Long = 0xFFDEE0FF
        private const val eudiw_theme_dark_tertiary: Long = eudiw_theme_dark_secondary
        private const val eudiw_theme_dark_onTertiary: Long = eudiw_theme_dark_onSecondary
        private const val eudiw_theme_dark_tertiaryContainer: Long =
            eudiw_theme_dark_secondaryContainer
        private const val eudiw_theme_dark_onTertiaryContainer: Long =
            eudiw_theme_dark_onSecondaryContainer

        private const val eudiw_theme_dark_errorContainer: Long = 0xFF930009
        private const val eudiw_theme_dark_onError: Long = 0xFF690004
        private const val eudiw_theme_dark_onErrorContainer: Long = 0xFFFFDAD5
        const val eudiw_theme_dark_background: Long = black
        private const val eudiw_theme_dark_onBackground: Long = white
        private const val eudiw_theme_dark_surface: Long = black
        private const val eudiw_theme_dark_onSurface: Long = white
        private const val eudiw_theme_dark_surfaceVariant: Long = 0xFF45464F
        private const val eudiw_theme_dark_onSurfaceVariant: Long = 0xFFC5C6D0
        private const val eudiw_theme_dark_outline: Long = 0xFF8F909A
        private const val eudiw_theme_dark_inverseOnSurface: Long = black
        private const val eudiw_theme_dark_inverseSurface: Long = white
        private const val eudiw_theme_dark_inversePrimary: Long = 0xFF2B5CAD
        private const val eudiw_theme_dark_surfaceTint: Long = eudiw_theme_dark_surface
        private const val eudiw_theme_dark_outlineVariant: Long = 0xFF44474F
        private const val eudiw_theme_dark_scrim: Long = white

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

        val secondary: Color
            get() = if (isInDarkMode) {
                Color(eudiw_theme_dark_secondary)
            } else {
                Color(eudiw_theme_light_secondary)
            }

        val success: Color
            get() = if (isInDarkMode) {
                Color(eudiw_theme_dark_success)
            } else {
                Color(eudiw_theme_light_success)
            }

        val onSuccess: Color
            get() = if (isInDarkMode) {
                Color(eudiw_theme_dark_onSuccess)
            } else {
                Color(eudiw_theme_light_onSuccess)
            }

        val error: Color
            get() = if (isInDarkMode) {
                Color(eudiw_theme_dark_error)
            } else {
                Color(eudiw_theme_light_error)
            }

        val onError: Color
            get() = if (isInDarkMode) {
                Color(eudiw_theme_dark_onError)
            } else {
                Color(eudiw_theme_light_onError)
            }

        val warning: Color
            get() = if (isInDarkMode) {
                Color(eudiw_theme_dark_warning)
            } else {
                Color(eudiw_theme_light_warning)
            }

        val info: Color
            get() = if (isInDarkMode) {
                Color(eudiw_theme_dark_info)
            } else {
                Color(eudiw_theme_light_info)
            }

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

val ColorScheme.success: Color
    @Composable get() = if (isSystemInDarkTheme()) {
        Color(ThemeColors.eudiw_theme_dark_success)
    } else {
        Color(ThemeColors.eudiw_theme_light_success)
    }

val ColorScheme.onSuccess: Color
    @Composable get() = if (isSystemInDarkTheme()) {
        Color(ThemeColors.eudiw_theme_dark_onSuccess)
    } else {
        Color(ThemeColors.eudiw_theme_light_onSuccess)
    }

val ColorScheme.warning: Color
    @Composable get() = if (isSystemInDarkTheme()) {
        Color(ThemeColors.eudiw_theme_dark_warning)
    } else {
        Color(ThemeColors.eudiw_theme_light_warning)
    }

val ColorScheme.info: Color
    @Composable get() = if (isSystemInDarkTheme()) {
        Color(ThemeColors.eudiw_theme_dark_info)
    } else {
        Color(ThemeColors.eudiw_theme_light_info)
    }