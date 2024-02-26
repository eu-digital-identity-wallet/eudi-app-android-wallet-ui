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

import eu.europa.ec.euidi.config.LibraryModule
import eu.europa.ec.euidi.kover.KoverExclusionRules
import eu.europa.ec.euidi.kover.excludeFromKoverReport

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

plugins {
    id("eudi.android.library")
}

android {
    namespace = "eu.europa.ec.networklogic"
}

moduleConfig {
    module = LibraryModule.NetworkLogic
}

dependencies {
    implementation(project(":business-logic"))

    api(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp.mockwebserver)

    debugImplementation(libs.chucker.android)
    releaseImplementation(libs.chucker.android.no.op)

    testImplementation(project(":test-logic"))
}

excludeFromKoverReport(
    excludedClasses = KoverExclusionRules.NetworkLogic.classes,
    excludedPackages = KoverExclusionRules.NetworkLogic.packages,
)