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

import project.build.convention.config.LibraryModule.AnalyticsLogic
import project.build.convention.config.LibraryModule.AssemblyLogic
import project.build.convention.config.LibraryModule.AuthenticationLogic
import project.build.convention.config.LibraryModule.BusinessLogic
import project.build.convention.config.LibraryModule.CommonFeature
import project.build.convention.config.LibraryModule.DashboardFeature
import project.build.convention.config.LibraryModule.IssuanceFeature
import project.build.convention.config.LibraryModule.LoginFeature
import project.build.convention.config.LibraryModule.NetworkLogic
import project.build.convention.config.LibraryModule.PresentationFeature
import project.build.convention.config.LibraryModule.ProximityFeature
import project.build.convention.config.LibraryModule.ResourcesLogic
import project.build.convention.config.LibraryModule.StartupFeature
import project.build.convention.config.LibraryModule.UiLogic
import project.build.convention.config.LibraryModule.CoreLogic
import project.build.convention.kover.KoverExclusionRules
import project.build.convention.kover.excludeFromKoverReport
import project.build.convention.kover.koverModules

plugins {
    id("project.android.library")
    id("project.android.library.compose")
}

android {
    namespace = "eu.europa.ec.assemblylogic"

    defaultConfig {
        // App name
        manifestPlaceholders["appName"] = "EUDI Wallet"
    }
}

moduleConfig {
    module = AssemblyLogic
}

dependencies {

    // Logic Modules
    api(project(ResourcesLogic.path))
    api(project(BusinessLogic.path))
    api(project(UiLogic.path))
    api(project(NetworkLogic.path))
    api(project(AnalyticsLogic.path))
    api(project(AuthenticationLogic.path))
    api(project(CoreLogic.path))

    // Feature Modules
    api(project(CommonFeature.path))
    api(project(StartupFeature.path))
    api(project(LoginFeature.path))
    api(project(DashboardFeature.path))
    api(project(PresentationFeature.path))
    api(project(ProximityFeature.path))
    api(project(IssuanceFeature.path))

    // Test Cover Report
    koverModules.forEach {
        kover(project(it.key.path)) {
            excludeFromKoverReport(
                excludedClasses = it.value.classes,
                excludedPackages = it.value.packages,
            )
        }
    }
}

excludeFromKoverReport(
    excludedClasses = KoverExclusionRules.AssemblyLogic.classes,
    excludedPackages = KoverExclusionRules.AssemblyLogic.packages,
)