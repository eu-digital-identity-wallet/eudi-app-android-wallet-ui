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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "project.build.convention.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
tasks.withType<KotlinCompile>().configureEach {
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.secrets.gradlePlugin)
    compileOnly(libs.owasp.dependencycheck.gradlePlugin)
    compileOnly(libs.kotlinx.kover.gradlePlugin)
    compileOnly(libs.baselineprofile.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplicationCompose") {
            id = "project.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidApplication") {
            id = "project.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "project.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = "project.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "project.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidLibraryKover") {
            id = "project.android.library.kover"
            implementationClass = "AndroidLibraryKoverConventionPlugin"
        }
        register("androidTest") {
            id = "project.android.test"
            implementationClass = "AndroidTestConventionPlugin"
        }
        register("androidFeatureTest") {
            id = "project.android.feature.test"
            implementationClass = "AndroidFeatureTestConventionPlugin"
        }
        register("androidKoin") {
            id = "project.android.koin"
            implementationClass = "AndroidKoinConventionPlugin"
        }
        register("androidFlavors") {
            id = "project.android.application.flavors"
            implementationClass = "AndroidApplicationFlavorsConventionPlugin"
        }
        register("androidLint") {
            id = "project.android.lint"
            implementationClass = "AndroidLintConventionPlugin"
        }
        register("jvmLibrary") {
            id = "project.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("eudiWalletCore") {
            id = "project.wallet.core"
            implementationClass = "EudiWalletCorePlugin"
        }
        register("owaspDependencyCheck") {
            id = "project.owasp.dependency.check"
            implementationClass = "OwaspDependencyCheckPlugin"
        }
        register("sonar") {
            id = "project.sonar"
            implementationClass = "SonarPlugin"
        }
        register("androidBaseProfile") {
            id = "project.android.base.profile"
            implementationClass = "AndroidBaseLineProfilePlugin"
        }
        register("eudiRqes") {
            id = "project.rqes.sdk"
            implementationClass = "EudiRqesPlugin"
        }
        register("kotlinRealm") {
            id = "project.kotlin.realm"
            implementationClass = "RealmPlugin"
        }
        register("eudiStorage") {
            id = "project.wallet.storage"
            implementationClass = "EudiWalletStoragePlugin"
        }
    }
}
