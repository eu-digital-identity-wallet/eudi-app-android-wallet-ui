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

package project.convention.logic.config

enum class LibraryModule(val path: String) {
    Unspecified(""),
    TestLogic(":test-logic"),
    TestFeatureLogic(":test-feature"),
    AnalyticsLogic(":analytics-logic"),
    AssemblyLogic(":assembly-logic"),
    BusinessLogic(":business-logic"),
    CoreLogic(":core-logic"),
    AuthenticationLogic(":authentication-logic"),
    UiLogic(":ui-logic"),
    NetworkLogic(":network-logic"),
    ResourcesLogic(":resources-logic"),
    StorageLogic(":storage-logic"),
    BaselineProfileLogic(":baseline-profile"),
    CommonFeature(":common-feature"),
    StartupFeature(":startup-feature"),
    DashboardFeature(":dashboard-feature"),
    PresentationFeature(":presentation-feature"),
    ProximityFeature(":proximity-feature"),
    IssuanceFeature(":issuance-feature");

    val isLogicModule: Boolean
        get() {
            return this.name.contains("Logic")
        }

    val isFeatureCommon: Boolean get() = this == CommonFeature
}

open class LibraryPluginConfig(var module: LibraryModule)