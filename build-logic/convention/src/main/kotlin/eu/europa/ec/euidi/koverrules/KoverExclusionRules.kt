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

sealed interface KoverExclusionRules {
    val classes: ArrayList<String>
        get() = arrayListOf()
    val packages: ArrayList<String>
        get() = arrayListOf()

    object AssemblyLogic : KoverExclusionRules {
        override val classes: ArrayList<String>
            get() = arrayListOf(
                "org.koin.ksp.generated.*",
            )
        override val packages: ArrayList<String>
            get() = arrayListOf(
                "eu.europa.ec.assemblylogic",
            )
    }

    object PresentationFeature : KoverExclusionRules {
        override val classes: ArrayList<String>
            get() = arrayListOf(
                "eu.europa.ec.presentationfeature.ui.*.*Screen*",
            )
    }
}