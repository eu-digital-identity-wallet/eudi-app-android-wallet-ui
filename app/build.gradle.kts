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

import eu.europa.ec.euidi.EudiBuildType

plugins {
    id("eudi.android.application")
    id("eudi.android.application.compose")
}

android {

    signingConfigs {
        create("release") {

            storeFile = file("${rootProject.projectDir}/sign")

            keyAlias = (project.findProperty("androidKeyAlias") as? String)
                ?: System.getenv("ANDROID_KEY_ALIAS")
            keyPassword = (project.findProperty("androidKeyPassword") as? String)
                ?: System.getenv("ANDROID_KEY_PASSWORD")
            storePassword = (project.findProperty("androidKeyPassword") as? String)
                ?: System.getenv("ANDROID_KEY_PASSWORD")

            enableV2Signing = true
        }
    }

    defaultConfig {
        applicationId = "eu.europa.ec.euidi"
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        manifestPlaceholders["deepLinkScheme"] = "eudi-wallet"
        manifestPlaceholders["deepLinkHost"] = "*"
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = EudiBuildType.DEBUG.applicationIdSuffix
        }
        val release by getting {
            isDebuggable = false
            isMinifyEnabled = true
            applicationIdSuffix = EudiBuildType.RELEASE.applicationIdSuffix
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    namespace = "eu.europa.ec.euidi"
}

dependencies {
    implementation(project(":assembly-logic"))
}