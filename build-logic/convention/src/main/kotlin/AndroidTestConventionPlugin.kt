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

import com.android.build.gradle.LibraryExtension
import eu.europa.ec.euidi.configureGradleManagedDevices
import eu.europa.ec.euidi.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin

class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("eudi.android.library")
            }
            extensions.configure<LibraryExtension> {
                defaultConfig {
                    testInstrumentationRunner =
                        "androidx.test.runner.AndroidJUnitRunner"
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
                add("api", libs.findLibrary("mockito-inline").get())
                add("api", libs.findLibrary("robolectric").get())
            }
        }
    }
}
