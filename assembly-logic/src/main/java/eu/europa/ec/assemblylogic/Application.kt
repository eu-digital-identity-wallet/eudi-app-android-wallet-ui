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

package eu.europa.ec.assemblylogic

import android.app.Application
import eu.europa.ec.assemblylogic.di.setupKoin
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.resourceslogic.theme.ThemeManager
import eu.europa.ec.resourceslogic.theme.templates.ThemeDimensTemplate
import eu.europa.ec.resourceslogic.theme.values.ThemeColors
import eu.europa.ec.resourceslogic.theme.values.ThemeShapes
import eu.europa.ec.resourceslogic.theme.values.ThemeTypography
import org.koin.android.ext.android.inject

class Application : Application() {

    private val logController: LogController by inject()

    override fun onCreate() {
        super.onCreate()
        setupKoin()
        initializeLogging()
        initializeTheme()
    }

    private fun initializeLogging() {
        logController.install()
    }

    private fun initializeTheme() {
        ThemeManager.Builder()
            .withLightColors(ThemeColors.lightColors)
            .withDarkColors(ThemeColors.darkColors)
            .withTypography(ThemeTypography.typo)
            .withShapes(ThemeShapes.shapes)
            .withDimensions(
                ThemeDimensTemplate(
                    screenPadding = 10.0
                )
            )
            .build()
    }
}