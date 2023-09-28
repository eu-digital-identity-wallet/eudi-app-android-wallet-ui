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

import eu.europa.ec.euidi.addConfigField

plugins {
    id("eudi.android.library")
}

android {
    namespace = "eu.europa.ec.businesslogic"

    defaultConfig {
        addConfigField("DEEPLINK", "eudi-wallet://")
    }
}

dependencies {
    implementation(project(":resources-logic"))
    implementation(libs.gson)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security)
    implementation(libs.androidx.appAuth)
    implementation(libs.logcat)

    testImplementation(project(":test-logic"))
}