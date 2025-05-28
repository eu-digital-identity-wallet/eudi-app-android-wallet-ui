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
import org.gradle.kotlin.dsl.kotlin
import project.convention.logic.configureGradleManagedDevices
import project.convention.logic.libs

class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("project.android.library")
            }
            extensions.configure<LibraryExtension> {
                defaultConfig {
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                configureGradleManagedDevices(this)
            }

            dependencies {
                add("api", kotlin("test"))
                add(
                    "api",
                    libs.findLibrary("androidx-test-orchestrator").get()
                )
                add("api", libs.findLibrary("androidx-test-rules").get())
                add("api", libs.findLibrary("androidx-test-runner").get())
                add("api", libs.findLibrary("androidx-work-testing").get())
                add("api", libs.findLibrary("kotlinx-coroutines-test").get())
                add("api", libs.findLibrary("turbine").get())
                add("api", libs.findLibrary("mockito-core").get())
                add("api", libs.findLibrary("mockito-kotlin").get())
                add("api", libs.findLibrary("robolectric").get())
            }
        }
    }
}
