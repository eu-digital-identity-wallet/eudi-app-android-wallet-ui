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

package project.convention.logic.kover

import project.convention.logic.config.LibraryModule

private const val KOIN = "*.ksp.*"
private const val BUILD_CONFIG = "*BuildConfig*"
private const val SCREEN_COMPOSABLES = "*ScreenKt*"
private const val ACTIVITIES = "*Activity"
private const val ACTIVITY_MEMBERS = "*Activity$*"
private const val SERVICES = "*Service"
private const val SERVICE_MEMBERS = "*Service$*"
private const val ROOM_GENERATED = "*_Impl*"
private const val MODELS = "eu.europa.ec.*.model"
private const val DOMAIN_MODELS = "eu.europa.ec.*.domain"
private const val CONFIGS = "eu.europa.ec.*.config"
private const val COMPONENTS = "eu.europa.ec.*.component"
private const val PROVIDERS = "eu.europa.ec.*.provider"
private const val DI = "eu.europa.ec.*.di"
private const val ROUTER_GRAPH = "eu.europa.ec.*.router"

val koverModules: Map<LibraryModule, KoverExclusionRules> = mapOf(
    LibraryModule.BusinessLogic to KoverExclusionRules.BusinessLogic,
    LibraryModule.UiLogic to KoverExclusionRules.UiLogic,
    LibraryModule.AuthenticationLogic to KoverExclusionRules.AuthenticationLogic,
    LibraryModule.NetworkLogic to KoverExclusionRules.NetworkLogic,
    LibraryModule.CoreLogic to KoverExclusionRules.CoreLogic,
    LibraryModule.StorageLogic to KoverExclusionRules.StorageLogic,
    LibraryModule.CommonFeature to KoverExclusionRules.CommonFeature,
    LibraryModule.StartupFeature to KoverExclusionRules.StartupFeature,
    LibraryModule.DashboardFeature to KoverExclusionRules.DashboardFeature,
    LibraryModule.PresentationFeature to KoverExclusionRules.PresentationFeature,
    LibraryModule.ProximityFeature to KoverExclusionRules.ProximityFeature,
    LibraryModule.IssuanceFeature to KoverExclusionRules.IssuanceFeature
)

sealed interface KoverExclusionRules {
    val commonClasses: List<String>
        get() = listOf(
            BUILD_CONFIG,
            SCREEN_COMPOSABLES,
            ACTIVITIES,
            ACTIVITY_MEMBERS,
            SERVICES,
            SERVICE_MEMBERS,
            ROOM_GENERATED
        )

    val commonPackages: List<String>
        get() = listOf(
            KOIN,
            DI,
            MODELS,
            DOMAIN_MODELS,
            ROUTER_GRAPH,
            CONFIGS,
            COMPONENTS,
            PROVIDERS
        )

    val classes: List<String>
    val packages: List<String>

    sealed interface LogicModule : KoverExclusionRules
    sealed interface FeatureModule : KoverExclusionRules

    object AssemblyLogic : LogicModule {
        override val classes: List<String>
            get() = commonClasses

        override val packages: List<String>
            get() = listOf(
                "eu.europa.ec.assemblylogic",
            )
    }

    object BusinessLogic : LogicModule {
        override val classes: List<String>
            get() = commonClasses

        override val packages: List<String>
            get() = commonPackages + listOf(
                "eu.europa.ec.businesslogic.controller.crypto"
            )
    }

    object UiLogic : LogicModule {
        override val classes: List<String>
            get() = commonClasses

        override val packages: List<String>
            get() = commonPackages + listOf(
                "eu.europa.ec.uilogic.mvi"
            )
    }

    object NetworkLogic : LogicModule {
        override val classes: List<String>
            get() = commonClasses

        override val packages: List<String>
            get() = commonPackages
    }

    object CommonFeature : FeatureModule {
        override val classes: List<String>
            get() = commonClasses

        override val packages: List<String>
            get() = commonPackages
    }

    object StartupFeature : FeatureModule {
        override val classes: List<String>
            get() = commonClasses
        override val packages: List<String>
            get() = commonPackages
    }

    object DashboardFeature : FeatureModule {
        override val classes: List<String>
            get() = commonClasses

        override val packages: List<String>
            get() = commonPackages
    }

    object PresentationFeature : FeatureModule {
        override val classes: List<String>
            get() = commonClasses

        override val packages: List<String>
            get() = commonPackages
    }

    object ProximityFeature : FeatureModule {
        override val classes: List<String>
            get() = commonClasses

        override val packages: List<String>
            get() = commonPackages
    }

    object IssuanceFeature : FeatureModule {
        override val classes: List<String>
            get() = commonClasses

        override val packages: List<String>
            get() = commonPackages
    }

    object AuthenticationLogic : FeatureModule {
        override val classes: List<String>
            get() = commonClasses

        override val packages: List<String>
            get() = commonPackages
    }

    object AnalyticsLogic : FeatureModule {
        override val classes: List<String>
            get() = commonClasses

        override val packages: List<String>
            get() = listOf(
                "eu.europa.ec.analyticslogic"
            )
    }

    object CoreLogic : FeatureModule {
        override val classes: List<String>
            get() = commonClasses

        override val packages: List<String>
            get() = commonPackages
    }

    object StorageLogic : LogicModule {
        override val classes: List<String>
            get() = commonClasses

        override val packages: List<String>
            get() = commonPackages
    }
}