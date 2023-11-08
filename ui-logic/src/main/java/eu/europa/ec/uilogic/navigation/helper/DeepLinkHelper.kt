/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.uilogic.navigation.helper

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.navigation.NavController
import eu.europa.ec.businesslogic.BuildConfig
import eu.europa.ec.uilogic.container.EudiComponentActivity
import eu.europa.ec.uilogic.navigation.PresentationScreens
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

fun generateComposableDeepLinkUri(screen: Screen, arguments: String): Uri =
    generateComposableDeepLinkUri(screen.screenName, arguments)

fun generateComposableDeepLinkUri(screen: String, arguments: String): Uri =
    "${BuildConfig.DEEPLINK}/${screen}$arguments".toUri()

fun generateComposableNavigationLink(screen: Screen, arguments: String): String =
    generateComposableNavigationLink(screen.screenName, arguments)

fun generateComposableNavigationLink(screen: String, arguments: String): String =
    "${screen}$arguments"

fun generateNewTaskDeepLink(
    context: Context,
    screen: Screen,
    arguments: String = "",
    flags: Int = 0
): Intent =
    generateNewTaskDeepLink(context, screen.screenName, arguments, flags)

fun generateNewTaskDeepLink(
    context: Context,
    screen: String,
    arguments: String = "",
    flags: Int = 0
): Intent =
    Intent(
        Intent.ACTION_VIEW,
        generateComposableDeepLinkUri(screen, arguments),
        context,
        EudiComponentActivity::class.java
    ).apply {
        addFlags(flags)
    }

fun hasDeepLink(deepLinkUri: Uri?): DeepLinkAction? {
    return deepLinkUri?.let { uri ->
        DeepLinkType.parse(uri.host)?.let {
            DeepLinkAction(link = uri, type = it)
        }
    }
}

fun handleDeepLinkAction(navController: NavController, uri: Uri) {
    hasDeepLink(uri)?.let {
        val screen: Screen = when (it.type) {
            DeepLinkType.AUTHORIZATION -> PresentationScreens.CrossDeviceRequest
        }
        navController.navigate(screen.screenRoute) {
            popUpTo(screen.screenRoute) { inclusive = true }
        }
    }
}

data class DeepLinkAction(val link: Uri, val type: DeepLinkType)
enum class DeepLinkType(val type: String) {
    AUTHORIZATION("authorization");

    companion object {
        fun parse(type: String?): DeepLinkType? = entries.firstOrNull { it.type == type }
    }
}