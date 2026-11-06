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

package eu.europa.ec.uilogic.navigation.helper

import eu.europa.ec.uilogic.navigation.Screen

fun <T> generateComposableArguments(arguments: Map<String, T>): String {
    if (arguments.isEmpty()) return ""
    return StringBuilder().apply {
        append("?")
        arguments.onEachIndexed { index, entry ->
            if (index > 0) {
                append("&")
            }
            append("${entry.key}=${entry.value}")
        }
    }.toString()
}

fun generateComposableNavigationLink(screen: Screen, arguments: String): String =
    generateComposableNavigationLink(screen.screenName, arguments)

fun generateComposableNavigationLink(screen: String, arguments: String): String =
    "$screen$arguments"

/**
 * True when [route] is the route of one of [protectedFlowScreens].
 */
internal fun isProtectedRoute(route: String?, protectedFlowScreens: List<Screen>): Boolean =
    route != null && protectedFlowScreens.any { it.screenRoute == route }

/**
 * True when any of [backStackRoutes] is not the route of one of [loginScreens], so a locked wallet
 * showing this back stack must return to login.
 */
internal fun requiresLogin(backStackRoutes: List<String>, loginScreens: List<Screen>): Boolean =
    backStackRoutes.any { route -> loginScreens.none { it.screenRoute == route } }