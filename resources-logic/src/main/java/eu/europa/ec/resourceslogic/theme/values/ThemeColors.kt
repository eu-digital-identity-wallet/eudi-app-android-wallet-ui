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

        internal const val eudiw_theme_light_chipBackground: Long = 0xFFE0E7F0
        internal const val eudiw_theme_dark_chipBackground: Long = 0xFF006495
        internal const val eudiw_theme_light_infoBackground: Long = 0xFFFFAE3
        internal const val eudiw_theme_dark_infoBackground: Long = 0xFFFFAE3

        internal const val eudiw_theme_light_primaryMain: Long = 0xFF004494
        internal const val eudiw_theme_light_primaryDark: Long = 0xFF003D84
        internal const val eudiw_theme_light_primaryLight: Long = 0xFF4073AF
        private const val eudiw_theme_light_secondary: Long = 0xFFFFD617
        internal const val eudiw_theme_light_success: Long = 0xFF467A39
        internal const val eudiw_theme_light_warning: Long = 0xFFF29527
        private const val eudiw_theme_light_error: Long = 0xFFDA2131
        internal const val eudiw_theme_light_info: Long = 0xFF006FB4

        internal const val eudiw_theme_dark_primaryMain: Long = 0xFFADC6FF
        internal const val eudiw_theme_dark_primaryDark: Long = 0xFFACC7FF
        internal const val eudiw_theme_dark_primaryLight: Long = 0xFFA2C9FF
        private const val eudiw_theme_dark_secondary: Long = 0xFFEAC300
        internal const val eudiw_theme_dark_success: Long = 0xFF97D783
        internal const val eudiw_theme_dark_warning: Long = 0xFFFFB872
        private const val eudiw_theme_dark_error: Long = 0xFFFFB3AF
        internal const val eudiw_theme_dark_info: Long = 0xFF9BCBFF

        internal const val eudiw_theme_light_onSuccess: Long = white
        internal const val eudiw_theme_dark_onSuccess: Long = 0xFF033900

        internal const val eudiw_theme_light_textPrimaryDark: Long = 0xDE000000
        internal const val eudiw_theme_light_textSecondaryDark: Long = 0x8A000000
        internal const val eudiw_theme_light_textDisabledDark: Long = 0x61000000
        internal const val eudiw_theme_light_dividerDark: Long = 0x1F000000
        internal const val eudiw_theme_light_backgroundDefault: Long = 0x0A000000
        const val eudiw_theme_light_backgroundPaper: Long = 0xFFFFFFFF
        internal const val eudiw_theme_light_textSecondaryLight: Long = 0xB3FFFFFF
        internal const val eudiw_theme_light_textDisabledLight: Long = 0x80FFFFFF

        internal const val eudiw_theme_dark_textPrimaryDark: Long = 0xDEFFFFFF
        internal const val eudiw_theme_dark_textSecondaryDark: Long = 0x8AFFFFFF
        internal const val eudiw_theme_dark_textDisabledDark: Long = 0x61FFFFFF
        internal const val eudiw_theme_dark_dividerDark: Long = 0x1FFFFFFF
        internal const val eudiw_theme_dark_backgroundDefault: Long = 0x0AFFFFFF
        const val eudiw_theme_dark_backgroundPaper: Long = 0xFF000000
        internal const val eudiw_theme_dark_textSecondaryLight: Long = 0xB3000000
        internal const val eudiw_theme_dark_textDisabledLight: Long = 0x80000000

        private const val eudiw_theme_light_primary: Long = eudiw_theme_light_primaryMain
        private const val eudiw_theme_light_onPrimary: Long = white
        private const val eudiw_theme_light_primaryContainer: Long = 0xFFD8E2FF
        private const val eudiw_theme_light_onPrimaryContainer: Long = 0xFF001A41

        private const val eudiw_theme_light_onSecondary: Long = white
        private const val eudiw_theme_light_secondaryContainer: Long = 0xFFFFE173
        private const val eudiw_theme_light_onSecondaryContainer: Long = 0xFF221B00
        private const val eudiw_theme_light_tertiary: Long = eudiw_theme_light_secondary
        private const val eudiw_theme_light_onTertiary: Long = eudiw_theme_light_onSecondary
        private const val eudiw_theme_light_tertiaryContainer: Long =
            eudiw_theme_light_secondaryContainer
        private const val eudiw_theme_light_onTertiaryContainer: Long =
            eudiw_theme_light_onSecondaryContainer

        private const val eudiw_theme_light_errorContainer: Long = 0xFFFFDAD7
        private const val eudiw_theme_light_onError: Long = white
        private const val eudiw_theme_light_onErrorContainer: Long = 0xFF410005
        private const val eudiw_theme_light_background: Long = white
        private const val eudiw_theme_light_onBackground: Long = black
        private const val eudiw_theme_light_surface: Long = white
        private const val eudiw_theme_light_onSurface: Long = black
        private const val eudiw_theme_light_surfaceVariant: Long = 0xFFE1E2EC
        private const val eudiw_theme_light_onSurfaceVariant: Long = 0xFF44474F
        private const val eudiw_theme_light_outline: Long = 0xFF74777F
        private const val eudiw_theme_light_inverseOnSurface: Long = 0xFFF1EFFF
        private const val eudiw_theme_light_inverseSurface: Long = 0xFF1E2578
        private const val eudiw_theme_light_inversePrimary: Long = 0xFFADC6FF
        private const val eudiw_theme_light_surfaceTint: Long = 0xFF2B5CAD
        private const val eudiw_theme_light_outlineVariant: Long = 0xFFC4C6D0
        private const val eudiw_theme_light_scrim: Long = black

        private const val eudiw_theme_dark_primary: Long = eudiw_theme_dark_primaryMain
        private const val eudiw_theme_dark_onPrimary: Long = 0xFF002E69
        private const val eudiw_theme_dark_primaryContainer: Long = 0xFF004494
        private const val eudiw_theme_dark_onPrimaryContainer: Long = 0xFFD8E2FF

        private const val eudiw_theme_dark_onSecondary: Long = 0xFF3B2F00
        private const val eudiw_theme_dark_secondaryContainer: Long = 0xFF554500
        private const val eudiw_theme_dark_onSecondaryContainer: Long = 0xFFFFE173
        private const val eudiw_theme_dark_tertiary: Long = eudiw_theme_dark_secondary
        private const val eudiw_theme_dark_onTertiary: Long = eudiw_theme_dark_onSecondary
        private const val eudiw_theme_dark_tertiaryContainer: Long =
            eudiw_theme_dark_secondaryContainer
        private const val eudiw_theme_dark_onTertiaryContainer: Long =
            eudiw_theme_dark_onSecondaryContainer

        private const val eudiw_theme_dark_errorContainer: Long = 0xFF930017
        private const val eudiw_theme_dark_onError: Long = 0xFF68000D
        private const val eudiw_theme_dark_onErrorContainer: Long = 0xFFFFDAD7
        private const val eudiw_theme_dark_background: Long = black
        private const val eudiw_theme_dark_onBackground: Long = white
        private const val eudiw_theme_dark_surface: Long = black
        private const val eudiw_theme_dark_onSurface: Long = white
        private const val eudiw_theme_dark_surfaceVariant: Long = 0xFF44474F
        private const val eudiw_theme_dark_onSurfaceVariant: Long = 0xFFC4C6D0
        private const val eudiw_theme_dark_outline: Long = 0xFF8E9099
        private const val eudiw_theme_dark_inverseOnSurface: Long = black
        private const val eudiw_theme_dark_inverseSurface: Long = white
        private const val eudiw_theme_dark_inversePrimary: Long = 0xFF2B5CAD
        private const val eudiw_theme_dark_surfaceTint: Long = 0xFFADC6FF
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

        val primaryMain: Color
            get() = if (isInDarkMode) {
                Color(eudiw_theme_dark_primaryMain)
            } else {
                Color(eudiw_theme_light_primaryMain)
            }

        val primaryDark: Color
            get() = if (isInDarkMode) {
                Color(eudiw_theme_dark_primaryDark)
            } else {
                Color(eudiw_theme_light_primaryDark)
            }

        val primaryLight: Color
            get() = if (isInDarkMode) {
                Color(eudiw_theme_dark_primaryLight)
            } else {
                Color(eudiw_theme_light_primaryLight)
            }

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

    }
}

val ColorScheme.primaryMain: Color
    @Composable get() = if (isSystemInDarkTheme()) {
        Color(ThemeColors.eudiw_theme_dark_primaryMain)
    } else {
        Color(ThemeColors.eudiw_theme_light_primaryMain)
    }

val ColorScheme.primaryDark: Color
    @Composable get() = if (isSystemInDarkTheme()) {
        Color(ThemeColors.eudiw_theme_dark_primaryDark)
    } else {
        Color(ThemeColors.eudiw_theme_light_primaryDark)
    }

val ColorScheme.primaryLight: Color
    @Composable get() = if (isSystemInDarkTheme()) {
        Color(ThemeColors.eudiw_theme_dark_primaryLight)
    } else {
        Color(ThemeColors.eudiw_theme_light_primaryLight)
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

val ColorScheme.chipBackground: Color
    @Composable get() = if (isSystemInDarkTheme()) {
        Color(ThemeColors.eudiw_theme_dark_chipBackground)
    } else {
        Color(ThemeColors.eudiw_theme_light_chipBackground)
    }

val ColorScheme.infoBackground: Color
    @Composable get() = if (isSystemInDarkTheme()) {
        Color(ThemeColors.eudiw_theme_dark_infoBackground)
    } else {
        Color(ThemeColors.eudiw_theme_light_infoBackground)
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