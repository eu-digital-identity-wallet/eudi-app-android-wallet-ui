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

package eu.europa.ec.uilogic.config

import eu.europa.ec.uilogic.navigation.Screen

data class ConfigNavigation(
    val navigationType: NavigationType,
    val indicateFlowCompletion: FlowCompletion = FlowCompletion.NONE
)

sealed interface NavigationType {
    data object Pop : NavigationType
    data object Finish : NavigationType
    data class PushScreen(
        val screen: Screen,
        val arguments: Map<String, String?> = emptyMap(),
        val popUpToScreen: Screen? = null
    ) : NavigationType

    data class PushRoute(
        val route: String,
        val popUpToRoute: String? = null
    ) : NavigationType

    data class PopTo(val screen: Screen) : NavigationType
    data class Deeplink(val link: String, val routeToPop: String? = null) : NavigationType
}

enum class FlowCompletion {
    CANCEL,
    SUCCESS,
    NONE
}