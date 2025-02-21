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

import project.convention.logic.config.LibraryModule
import project.convention.logic.kover.KoverExclusionRules
import project.convention.logic.kover.excludeFromKoverReport

plugins {
    id("project.android.library")
    id("project.wallet.core")
}

android {
    namespace = "eu.europa.ec.corelogic"
}

moduleConfig {
    module = LibraryModule.CoreLogic
}

var WALTID_VERSION = "0.11.0"

dependencies {
    implementation(project(LibraryModule.ResourcesLogic.path))
    implementation(project(LibraryModule.BusinessLogic.path))
    implementation(project(LibraryModule.AuthenticationLogic.path))

    implementation(libs.androidx.biometric)

    implementation(libs.waltid.did) {
        exclude("com.google.crypto.tink")
        exclude("org.bouncycastle")
    }
    implementation(libs.waltid.crypto) {
        exclude("com.google.crypto.tink")
        exclude("org.bouncycastle")
    }
    implementation("id.walt.sdjwt:waltid-sdjwt:${WALTID_VERSION}") {
        exclude("com.google.crypto.tink")
        exclude("org.bouncycastle")
    // from: https://github.com/walt-id/waltid-identity/blob/main/waltid-libraries/sdjwt/waltid-sdjwt/README.md =>
    // https://github.com/walt-id/waltid-examples/blob/main/build.gradle.kts

    // required dependencies for running the example project
//    implementation("id.walt.crypto:waltid-crypto:${WALTID_VERSION}")
//    implementation("id.walt.credentials:waltid-verifiable-credentials:${WALTID_VERSION}")
//    implementation("id.walt.did:waltid-did:${WALTID_VERSION}")
    }
//    implementation("id.walt.openid4vc:waltid-openid4vc:${WALTID_VERSION}")
//    implementation("id.walt.policies:waltid-verification-policies:${WALTID_VERSION}")
//    implementation("id.walt.dif-definitions-parser:waltid-dif-definitions-parser:${WALTID_VERSION}")

    // all walt.id dependencies (not required for this project)
//    implementation("id.walt.mdoc-credentials:waltid-mdoc-credentials:${WALTID_VERSION}")
//    implementation(libs.waltid.service.commons)
}

excludeFromKoverReport(
    excludedClasses = KoverExclusionRules.CoreLogic.classes,
    excludedPackages = KoverExclusionRules.CoreLogic.packages,
)
