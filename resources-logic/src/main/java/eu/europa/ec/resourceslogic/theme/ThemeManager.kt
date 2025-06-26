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
package eu.europa.ec.resourceslogic.theme

import android.app.Activity
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import eu.europa.ec.resourceslogic.theme.sets.ThemeSet
import eu.europa.ec.resourceslogic.theme.templates.ThemeColorsTemplate
import eu.europa.ec.resourceslogic.theme.templates.ThemeColorsTemplate.Companion.toColorScheme
import eu.europa.ec.resourceslogic.theme.templates.ThemeDimensTemplate
import eu.europa.ec.resourceslogic.theme.templates.ThemeShapesTemplate
import eu.europa.ec.resourceslogic.theme.templates.ThemeShapesTemplate.Companion.toShapes
import eu.europa.ec.resourceslogic.theme.templates.ThemeTypographyTemplate
import eu.europa.ec.resourceslogic.theme.templates.ThemeTypographyTemplate.Companion.toTypography
import eu.europa.ec.resourceslogic.theme.values.ThemeColors
import eu.europa.ec.resourceslogic.theme.values.ThemeShapes
import eu.europa.ec.resourceslogic.theme.values.ThemeTypography

class ThemeManager {
    /**
     * Contains the data of the theme like colors, shapes, typography etc.
     */
    lateinit var set: ThemeSet
        private set

    /**
     * Defines if dynamic theming is supported. Notice that Dynamic color is available on Android 12+.
     */
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    val dynamicThemeSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    @Composable
    fun Theme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        disableDynamicTheming: Boolean = true,
        styleStatusBar: Boolean = true,
        content: @Composable () -> Unit
    ) {
        val lightColorScheme = set.lightColors
        val darkColorScheme = set.darkColors

        val colorScheme = when {
            !disableDynamicTheming && dynamicThemeSupported -> {
                when {
                    darkTheme -> dynamicDarkColorScheme(LocalContext.current)
                    else -> dynamicLightColorScheme(LocalContext.current)
                }
            }

            darkTheme -> darkColorScheme
            else -> lightColorScheme
        }

        if (styleStatusBar) {
            val view = LocalView.current
            if (!LocalView.current.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    window.statusBarColor = colorScheme.secondary.toArgb()
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                        darkTheme
                }
            }
        }

        set = set.copy(isInDarkMode = darkTheme)

        MaterialTheme(
            colorScheme = colorScheme,
            shapes = set.shapes,
            typography = set.typo,
            content = content
        )
    }

    companion object {
        /**
         * Private instance of manager.
         */
        private lateinit var _instance: ThemeManager

        /**
         * Instance of theme manager. Built from [Builder].
         */
        val instance: ThemeManager
            get() {
                if (this::_instance.isInitialized.not()) {
                    _instance = Builder()
                        .withLightColors(ThemeColors.lightColors)
                        .withDarkColors(ThemeColors.darkColors)
                        .withTypography(ThemeTypography.typo)
                        .withShapes(ThemeShapes.shapes)
                        .withDimensions(
                            ThemeDimensTemplate(
                                screenPadding = 10.0
                            )
                        ).build()
                }

                return _instance
            }

        /**
         * Initializes the theme manager using the builder provided. This initializes the
         * static instance of the manager.
         */
        fun ThemeManager.build(builder: Builder): ThemeManager {
            set = ThemeSet(
                isInDarkMode = builder.isInDarkMode == true,
                lightColors = builder.lightColors.toColorScheme(),
                darkColors = builder.darkColors.toColorScheme(),
                typo = builder.typography.toTypography(),
                shapes = builder.shapes.toShapes(),
                dimens = builder.dimensions
            )
            return this
        }
    }

    class Builder(val isInDarkMode: Boolean? = null) {
        lateinit var lightColors: ThemeColorsTemplate
        lateinit var darkColors: ThemeColorsTemplate
        lateinit var typography: ThemeTypographyTemplate
        lateinit var shapes: ThemeShapesTemplate
        lateinit var dimensions: ThemeDimensTemplate

        /**
         * Set the colors set for the theme configuration. These colors refer to the light theme
         * colors. You can set the dark mode colors by calling [withDarkColors]. If no dark colors are
         * set, light will be used for both modes.
         *
         * @param colors Set of colors to be used to construct the theme.
         *
         * @return This instance for chaining.
         */
        fun withLightColors(colors: ThemeColorsTemplate): Builder {
            this.lightColors = colors
            return this
        }

        /**
         * Set the colors set for the theme configuration. These colors refer to the light theme
         * colors. You can set the dark mode colors by calling [withDarkColors]. If no dark colors are
         * set, light will be used for both modes.
         *
         * @param colors Set of colors to be used to construct the theme.
         *
         * @return This instance for chaining.
         */
        fun withDarkColors(colors: ThemeColorsTemplate): Builder {
            this.darkColors = colors
            return this
        }

        /**
         * Set the typography for theme.
         *
         * @param typography Set of typography to be used to construct the theme.
         *
         * @return This instance for chaining.
         */
        fun withTypography(typography: ThemeTypographyTemplate): Builder {
            this.typography = typography
            return this
        }

        /**
         * Set the shapes for theme.
         *
         * @param shapes Set of shapes to be used to construct the theme.
         *
         * @return This instance for chaining.
         */
        fun withShapes(shapes: ThemeShapesTemplate): Builder {
            this.shapes = shapes
            return this
        }

        /**
         * Set the dimensions for theme.
         *
         * @param dimens Set of dimensions to be used to construct the theme.
         *
         * @return This instance for chaining.
         */
        fun withDimensions(dimens: ThemeDimensTemplate): Builder {
            this.dimensions = dimens
            return this
        }

        fun build(
            buildStatic: Boolean = true
        ): ThemeManager {
            // Check light colors.
            if (this::lightColors.isInitialized.not()) {
                throw RuntimeException("lightColors is not initialized. Can not build theme manager.")
            }

            // If dark colors not initialized, set as light.
            if (this::darkColors.isInitialized.not()) {
                darkColors = lightColors
            }

            // Check typography.
            if (this::typography.isInitialized.not()) {
                throw RuntimeException("typography is not initialized. Can not build theme manager.")
            }

            // Check shapes.
            if (this::shapes.isInitialized.not()) {
                throw RuntimeException("shapes is not initialized. Can not build theme manager.")
            }

            // Check dimensions.
            if (this::dimensions.isInitialized.not()) {
                throw RuntimeException("dimensions is not initialized. Can not build theme manager.")
            }

            // Initialize instance.
            if (buildStatic) _instance = ThemeManager()

            // Initialize manager.
            return when (buildStatic) {
                true -> _instance.build(this)
                false -> ThemeManager().build(this)
            }
        }
    }
}