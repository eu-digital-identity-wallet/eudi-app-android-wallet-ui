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
    id("eudi.wallet.core")
}

android {
    namespace = "eu.europa.ec.businesslogic"
}

moduleConfig {
    module = LibraryModule.BusinessLogic
}

dependencies {
    implementation(project(":resources-logic"))
    implementation(libs.gson)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security)
    implementation(libs.androidx.appAuth)
    implementation(libs.logcat)
    implementation(libs.google.phonenumber)
    implementation(libs.rootbeer)

    testImplementation(project(":test-logic"))
    androidTestImplementation(project(":test-logic"))
}

excludeFromKoverReport(
    excludedClasses = KoverExclusionRules.BusinessLogic.classes,
    excludedPackages = KoverExclusionRules.BusinessLogic.packages,
)