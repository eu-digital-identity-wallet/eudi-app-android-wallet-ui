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

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.gms) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.secrets) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.org.jetbrains.kotlin.android) apply false
    alias(libs.plugins.owasp.dependencycheck) apply false
    alias(libs.plugins.kotlinx.kover) apply false
    alias(libs.plugins.sonar) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.androidx.room) apply false
}
true
