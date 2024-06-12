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

import project.build.convention.config.LibraryModule
import project.build.convention.kover.KoverExclusionRules
import project.build.convention.kover.excludeFromKoverReport

plugins {
    id("project.android.library")
    id("project.appcenter")
}

android {
    namespace = "eu.europa.ec.analyticslogic"
}

moduleConfig {
    module = LibraryModule.AnalyticsLogic
}

excludeFromKoverReport(
    excludedClasses = KoverExclusionRules.AnalyticsLogic.classes,
    excludedPackages = KoverExclusionRules.AnalyticsLogic.packages,
)