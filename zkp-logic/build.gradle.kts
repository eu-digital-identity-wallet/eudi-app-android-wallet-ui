/*
 * Copyright (c) 2026 European Commission
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

import com.android.build.api.dsl.LibraryExtension
import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.library")
    // Brings eudi-wallet-core (api) + Multipaz transitively, so this module can implement
    // org.multipaz.mdoc.zkp.ZkSystem and consume MdocDocument — same setup as :core-logic.
    id("project.wallet.core")
}

extensions.configure<LibraryExtension>("android") {
    namespace = "eu.europa.ec.zkplogic"

    testOptions {
        // Host JVM unit tests load the SDK's host-native via JNA; don't fail on incidental
        // Android API calls.
        unitTests.isReturnDefaultValues = true
    }
}

moduleConfig {
    module = LibraryModule.ZkpLogic
}

dependencies {
    // The Rust STWO ZK SDK (UniFFI/JNA + native .so bundled in the AAR). Resolved from mavenLocal().
    implementation(libs.euid.zk.sdk)

    // Host JVM unit tests: the same SDK with native libs for desktop platforms (mac/linux/win),
    // so JNA loads the real (stubbed) prover/verifier on the host — no emulator needed.
    testImplementation(libs.junit4)
    testImplementation(libs.euid.zk.sdk.jvm)
}
