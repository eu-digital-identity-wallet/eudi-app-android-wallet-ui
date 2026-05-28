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

package eu.europa.ec.uilogic.config

import eu.europa.ec.uilogic.navigation.Screen
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigNavigation(
    val navigationType: NavigationType,
    val indicateFlowCompletion: FlowCompletion = FlowCompletion.NONE
)

@Serializable
sealed interface NavigationType {
    @Serializable
    @SerialName("Pop")
    data object Pop : NavigationType

    @Serializable
    @SerialName("Finish")
    data object Finish : NavigationType

    @Serializable
    @SerialName("PushScreen")
    data class PushScreen(
        @Contextual val screen: Screen,
        val arguments: Map<String, String?> = emptyMap(),
        @Contextual val popUpToScreen: Screen? = null
    ) : NavigationType

    @Serializable
    @SerialName("PushRoute")
    data class PushRoute(
        val route: String,
        val popUpToRoute: String? = null
    ) : NavigationType

    @Serializable
    @SerialName("PopTo")
    data class PopTo(@Contextual val screen: Screen) : NavigationType

    @Serializable
    @SerialName("Deeplink")
    data class Deeplink(val link: String, val routeToPop: String? = null) : NavigationType
}

@Serializable
enum class FlowCompletion {
    CANCEL,
    SUCCESS,
    NONE
}