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

import eu.europa.ec.euidi.excludeFromKoverReport
import eu.europa.ec.euidi.koverrules.KoverExclusionRules

plugins {
    id("eudi.android.library")
    id("eudi.android.library.compose")
}

android {
    namespace = "eu.europa.ec.assemblylogic"

    defaultConfig {
        // App name
        manifestPlaceholders["appName"] = "EUDI Wallet"
    }
}

dependencies {

    // Logic Modules
    api(project(":resources-logic"))
    api(project(":business-logic"))
    api(project(":ui-logic"))
    api(project(":network-logic"))
    api(project(":analytics-logic"))

    // Feature Modules
    api(project(":common-feature"))
    api(project(":startup-feature"))
    api(project(":login-feature"))
    api(project(":dashboard-feature"))
    api(project(":presentation-feature"))
    api(project(":proximity-feature"))
    api(project(":issuance-feature"))

    // Test Cover Report
    kover(project(":business-logic")) {
        excludeFromKoverReport(
            excludedClasses = KoverExclusionRules.BusinessLogic.classes,
            excludedPackages = KoverExclusionRules.BusinessLogic.packages,
        )
    }
    kover(project(":ui-logic")) {
        excludeFromKoverReport(
            excludedClasses = KoverExclusionRules.UiLogic.classes,
            excludedPackages = KoverExclusionRules.UiLogic.packages,
        )
    }
    kover(project(":network-logic")) {
        excludeFromKoverReport(
            excludedClasses = KoverExclusionRules.NetworkLogic.classes,
            excludedPackages = KoverExclusionRules.NetworkLogic.packages,
        )
    }
    kover(project(":common-feature")) {
        excludeFromKoverReport(
            excludedClasses = KoverExclusionRules.CommonFeature.classes,
            excludedPackages = KoverExclusionRules.CommonFeature.packages,
        )
    }
    kover(project(":startup-feature")) {
        excludeFromKoverReport(
            excludedClasses = KoverExclusionRules.StartupFeature.classes,
            excludedPackages = KoverExclusionRules.StartupFeature.packages,
        )
    }
    kover(project(":login-feature")) {
        excludeFromKoverReport(
            excludedClasses = KoverExclusionRules.LoginFeature.classes,
            excludedPackages = KoverExclusionRules.LoginFeature.packages,
        )
    }
    kover(project(":dashboard-feature")) {
        excludeFromKoverReport(
            excludedClasses = KoverExclusionRules.DashboardFeature.classes,
            excludedPackages = KoverExclusionRules.DashboardFeature.packages,
        )
    }
    kover(project(":presentation-feature")) {
        excludeFromKoverReport(
            excludedClasses = KoverExclusionRules.PresentationFeature.classes,
            excludedPackages = KoverExclusionRules.PresentationFeature.packages,
        )
    }
    kover(project(":proximity-feature")) {
        excludeFromKoverReport(
            excludedClasses = KoverExclusionRules.ProximityFeature.classes,
            excludedPackages = KoverExclusionRules.ProximityFeature.packages,
        )
    }
    kover(project(":issuance-feature")) {
        excludeFromKoverReport(
            excludedClasses = KoverExclusionRules.IssuanceFeature.classes,
            excludedPackages = KoverExclusionRules.IssuanceFeature.packages,
        )
    }
}

excludeFromKoverReport(
    excludedClasses = KoverExclusionRules.AssemblyLogic.classes,
    excludedPackages = KoverExclusionRules.AssemblyLogic.packages,
)