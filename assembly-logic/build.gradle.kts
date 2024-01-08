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
    id("eudi.android.library.compose")
}

android {
    namespace = "eu.europa.ec.assemblylogic"

    defaultConfig {
        manifestPlaceholders["deepLinkScheme"] = "eudi-wallet"
        manifestPlaceholders["openid4vpScheme"] = "eudi-openid4vp"
        manifestPlaceholders["deepLinkHost"] = "*"
    }
}

dependencies {

    // Logic Modules
    api(project(":resources-logic"))
    api(project(":business-logic"))
    api(project(":ui-logic"))
    api(project(":network-logic"))

    // Feature Modules
    api(project(":common-feature"))
    api(project(":startup-feature"))
    api(project(":login-feature"))
    api(project(":dashboard-feature"))
    api(project(":presentation-feature"))
    api(project(":proximity-feature"))
    api(project(":issuance-feature"))
}