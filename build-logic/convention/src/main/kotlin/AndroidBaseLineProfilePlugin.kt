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
import androidx.baselineprofile.gradle.producer.BaselineProfileProducerExtension
import com.android.build.api.variant.TestAndroidComponentsExtension
import com.android.build.gradle.TestExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import project.convention.logic.configureFlavors
import project.convention.logic.configureGradleManagedDevices
import project.convention.logic.configureKotlinAndroid
import project.convention.logic.libs

@Suppress("UnstableApiUsage")
class AndroidBaseLineProfilePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.test")
                apply("org.jetbrains.kotlin.android")
                apply("androidx.baselineprofile")
            }

            extensions.configure<TestExtension> {
                configureKotlinAndroid(this)
                with(defaultConfig) {
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                configureFlavors(this)
                configureGradleManagedDevices(this)
                targetProjectPath = ":app"
            }

            extensions.configure<BaselineProfileProducerExtension> {
                managedDevices += "pixel6api34google"
                useConnectedDevices = false
            }

            extensions.configure<TestAndroidComponentsExtension> {
                onVariants { v ->
                    v.instrumentationRunnerArguments.put(
                        "targetAppId",
                        v.testedApks.map {
                            v.artifacts.getBuiltArtifactsLoader().load(it)?.applicationId.orEmpty()
                        }
                    )
                }
            }

            dependencies {
                add("implementation", libs.findLibrary("androidx-test-orchestrator").get())
                add("implementation", libs.findLibrary("androidx-test-uiautomator").get())
                add("implementation", libs.findLibrary("androidx-test-espresso-core").get())
                add("implementation", libs.findLibrary("androidx-benchmark-macro").get())
            }
        }
    }
}