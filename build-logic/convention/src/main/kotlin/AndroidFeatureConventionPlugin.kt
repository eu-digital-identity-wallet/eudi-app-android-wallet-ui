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

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import project.convention.logic.config.LibraryModule
import project.convention.logic.configureGradleManagedDevices

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("project.android.library")
                apply("project.android.library.compose")
            }
            extensions.configure<LibraryExtension> {
                defaultConfig {
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                configureGradleManagedDevices(this)
            }

            dependencies {
                add("implementation", project(LibraryModule.BusinessLogic.path))
                add("implementation", project(LibraryModule.UiLogic.path))
                add("implementation", project(LibraryModule.NetworkLogic.path))
                add("implementation", project(LibraryModule.ResourcesLogic.path))
                add("implementation", project(LibraryModule.AnalyticsLogic.path))
                add("implementation", project(LibraryModule.AuthenticationLogic.path))
                add("implementation", project(LibraryModule.CoreLogic.path))
                add("testImplementation", project(LibraryModule.TestFeatureLogic.path))
                add("androidTestImplementation", project(LibraryModule.TestFeatureLogic.path))
            }
        }
    }
}
