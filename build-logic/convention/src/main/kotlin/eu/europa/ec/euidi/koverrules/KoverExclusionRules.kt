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

package eu.europa.ec.euidi.koverrules

private const val KOIN = "org.koin.*"
private const val BUILD_CONFIG = "eu.europa.ec.*.BuildConfig"
private const val MODELS = "eu.europa.ec.*.model"
private const val DI = "eu.europa.ec.*.di"

sealed interface KoverExclusionRules {
    val classes: List<String>
        get() = listOf()
    val packages: List<String>
        get() = listOf()

    object AssemblyLogic : KoverExclusionRules {
        override val classes: List<String>
            get() = listOf(
                KOIN,
            )
        override val packages: List<String>
            get() = listOf(
                "eu.europa.ec.assemblylogic",
            )
    }

    object BusinessLogic : KoverExclusionRules {
        override val classes: List<String>
            get() = listOf(
                KOIN,
                BUILD_CONFIG,
                "eu.europa.ec.businesslogic.controller.security.AntiHookController",
                "eu.europa.ec.businesslogic.controller.security.RootController",
                "eu.europa.ec.businesslogic.controller.security.AndroidInstaller",
                "eu.europa.ec.businesslogic.controller.security.AndroidPackageController*",
                "eu.europa.ec.businesslogic.controller.security.AntiHookController*",
                "eu.europa.ec.businesslogic.controller.security.RootControllerImpl",
                "eu.europa.ec.businesslogic.controller.security.SecurityErrorCode",
                "eu.europa.ec.businesslogic.controller.security.SecurityValidation",
                "eu.europa.ec.businesslogic.extension.ByteArrayExtensions*",
                "eu.europa.ec.businesslogic.extension.FlowExtensions*",
                "eu.europa.ec.businesslogic.util.EudiWalletListenerWrapper",
                "eu.europa.ec.businesslogic.util.SafeLet*",
            )
        override val packages: List<String>
            get() = listOf(
                DI,
                MODELS,
                "eu.europa.ec.businesslogic.config",
                "eu.europa.ec.businesslogic.controller.biometry",
                "eu.europa.ec.businesslogic.controller.crypto",
                "eu.europa.ec.businesslogic.controller.log",
                "eu.europa.ec.businesslogic.controller.storage",
            )
    }

    object UiLogic : KoverExclusionRules {
        override val classes: List<String>
            get() = listOf(
                KOIN,
                BUILD_CONFIG,
                "eu.europa.ec.uilogic.navigation.*Screen*",
                "eu.europa.ec.uilogic.navigation.ModuleRoute*",
                "eu.europa.ec.uilogic.navigation.RouterHost*",
                "eu.europa.ec.uilogic.serializer.UiSerializableParser*",
                "eu.europa.ec.uilogic.serializer.UiSerializerImpl",
            )
        override val packages: List<String>
            get() = listOf(
                DI,
                "eu.europa.ec.uilogic.component",
                "eu.europa.ec.uilogic.config",
                "eu.europa.ec.uilogic.container",
                "eu.europa.ec.uilogic.extension",
                "eu.europa.ec.uilogic.mvi",
            )
    }

    object PresentationFeature : KoverExclusionRules {
        override val classes: List<String>
            get() = listOf(
                "eu.europa.ec.presentationfeature.ui.*.*Screen*",
            )
    }
}