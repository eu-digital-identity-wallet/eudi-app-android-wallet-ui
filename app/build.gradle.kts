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

import project.convention.logic.AppBuildType
import project.convention.logic.config.LibraryModule
import project.convention.logic.getProperty
import java.util.Properties
import kotlin.apply

plugins {
    id("project.android.application")
    id("project.android.application.compose")
}

android {

    val propsFile = rootProject.file("keystore.properties")
    val hasReleaseSigningConfig = propsFile.isFile

    signingConfigs {
        /*create("release") {

            storeFile = file("${rootProject.projectDir}/sign")

            keyAlias = getProperty("androidKeyAlias") ?: System.getenv("ANDROID_KEY_ALIAS")
            keyPassword = getProperty("androidKeyPassword") ?: System.getenv("ANDROID_KEY_PASSWORD")
            storePassword =
                getProperty("androidKeyPassword") ?: System.getenv("ANDROID_KEY_PASSWORD")

            enableV2Signing = true
        }*/

        if (hasReleaseSigningConfig) {
            val props = Properties().apply {
                load(propsFile.reader())
            }
            create("release") {
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyPassword = props.getProperty("keyPassword")
                keyAlias = props.getProperty("keyAlias")
                enableV2Signing = true
            }
        }
    }

    defaultConfig {
        applicationId = "net.eidas2sandkasse.demolommebok"
        versionName = "1.0.0"
        versionCode = 1

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = AppBuildType.DEBUG.applicationIdSuffix
        }
        /*release {
            isDebuggable = false
            isMinifyEnabled = true
            applicationIdSuffix = AppBuildType.RELEASE.applicationIdSuffix
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }*/
    }

    namespace = "net.eidas2sandkasse.demolommebok"
}

dependencies {
    implementation(project(LibraryModule.AssemblyLogic.path))
    "baselineProfile"(project(LibraryModule.BaselineProfileLogic.path))
}
