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

plugins {
    id("eudi.android.library")
    id("eudi.android.library.compose")
}

android {
    namespace = "eu.europa.ec.uilogic"
}

dependencies {
    implementation(project(":resources-logic"))
    implementation(project(":business-logic"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.gson)

    debugImplementation(libs.chucker.android)
    releaseImplementation(libs.chucker.android.no.op)

    testImplementation(project(":test-logic"))
}