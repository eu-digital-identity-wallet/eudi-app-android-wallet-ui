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

import eu.europa.ec.euidi.config.LibraryModule.AnalyticsLogic
import eu.europa.ec.euidi.config.LibraryModule.AssemblyLogic
import eu.europa.ec.euidi.config.LibraryModule.AuthenticationLogic
import eu.europa.ec.euidi.config.LibraryModule.BusinessLogic
import eu.europa.ec.euidi.config.LibraryModule.CommonFeature
import eu.europa.ec.euidi.config.LibraryModule.DashboardFeature
import eu.europa.ec.euidi.config.LibraryModule.IssuanceFeature
import eu.europa.ec.euidi.config.LibraryModule.LoginFeature
import eu.europa.ec.euidi.config.LibraryModule.NetworkLogic
import eu.europa.ec.euidi.config.LibraryModule.PresentationFeature
import eu.europa.ec.euidi.config.LibraryModule.ProximityFeature
import eu.europa.ec.euidi.config.LibraryModule.ResourcesLogic
import eu.europa.ec.euidi.config.LibraryModule.StartupFeature
import eu.europa.ec.euidi.config.LibraryModule.UiLogic
import eu.europa.ec.euidi.config.LibraryModule.CoreLogic
import eu.europa.ec.euidi.kover.KoverExclusionRules
import eu.europa.ec.euidi.kover.excludeFromKoverReport
import eu.europa.ec.euidi.kover.koverModules

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