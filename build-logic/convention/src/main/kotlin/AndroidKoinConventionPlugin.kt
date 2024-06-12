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

import com.android.build.api.dsl.AndroidSourceSet
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import project.convention.logic.libs

class AndroidKoinConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")

            when {
                pluginManager.hasPlugin("com.android.application") ->
                    configure<ApplicationExtension> { addVariantOptions(this.sourceSets) }

                pluginManager.hasPlugin("com.android.library") ->
                    configure<LibraryExtension> { addVariantOptions(this.sourceSets) }

                else -> {}
            }

            dependencies {
                add("implementation", libs.findLibrary("koin-android").get())
                add("implementation", libs.findLibrary("koin-annotations").get())
                add("implementation", libs.findLibrary("koin-compose").get())
                add("ksp", libs.findLibrary("koin-ksp").get())
            }
        }
    }

    private fun addVariantOptions(sourceSets: NamedDomainObjectContainer<out AndroidSourceSet>) {
        apply {
            sourceSets.all {
                kotlin.srcDir("build/generated/ksp/$name/kotlin")
            }
        }
    }
}