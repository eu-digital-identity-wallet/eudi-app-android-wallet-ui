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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import eu.europa.ec.businesslogic.util.safeLet
import eu.europa.ec.corelogic.util.CoreActions
import eu.europa.ec.eudi.rqesui.infrastructure.EudiRQESUi
import eu.europa.ec.eudi.rqesui.infrastructure.RemoteUri
import eu.europa.ec.uilogic.extension.openUrl
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.PresentationScreens
import eu.europa.ec.uilogic.navigation.Screen

fun hasDeepLink(deepLinkUri: Uri?): DeepLinkAction? {
    return safeLet(
        deepLinkUri,
        deepLinkUri?.scheme
    ) { uri, scheme ->
        DeepLinkAction(
            link = uri,
            type = DeepLinkType.parse(
                scheme = scheme,
                host = uri.host
            )
        )
    }
}

fun handleDeepLinkAction(
    navController: NavController,
    uri: Uri,
    arguments: String? = null
) {
    hasDeepLink(uri)?.let { action ->
        handleDeepLinkAction(
            navController = navController,
            action = action,
            arguments = arguments
        )
    }
}

fun handleDeepLinkAction(
    navController: NavController,
    action: DeepLinkAction,
    arguments: String? = null
) {
    val screen: Screen = when (action.type) {
        DeepLinkType.OPENID4VP -> {
            PresentationScreens.PresentationRequest
        }

        DeepLinkType.CREDENTIAL_OFFER -> {
            IssuanceScreens.DocumentOffer
        }

        DeepLinkType.ISSUANCE -> {
            notify(
                context = navController.context,
                action = CoreActions.VCI_RESUME_ACTION,
                bundle = bundleOf(Pair("uri", action.link.toString()))
            )
            return
        }

        DeepLinkType.EXTERNAL -> {
            navController.context.openUrl(action.link)
            return
        }

        DeepLinkType.DYNAMIC_PRESENTATION -> {
            notify(
                context = navController.context,
                action = CoreActions.VCI_DYNAMIC_PRESENTATION,
                bundle = bundleOf(Pair("uri", action.link.toString()))
            )
            return
        }

        DeepLinkType.RQES -> {
            action.link.getQueryParameter("code")?.let { authorizationCode ->
                EudiRQESUi.resume(
                    context = navController.context,
                    authorizationCode = authorizationCode
                )
            }
            return
        }

        DeepLinkType.RQES_DOC_RETRIEVAL -> {
            EudiRQESUi.initiate(
                context = navController.context,
                remoteUri = RemoteUri(action.link)
            )
            return
        }
    }

    val navigationLink = arguments?.let {
        generateComposableNavigationLink(
            screen = screen,
            arguments = arguments
        )
    } ?: screen.screenRoute

    navController.navigate(navigationLink) {
        popUpTo(screen.screenRoute) {
            inclusive = true
        }
    }
}

private fun notify(
    context: Context,
    action: String,
    bundle: Bundle? = null
) {
    Intent().also { intent ->
        intent.action = action
        intent.setPackage(context.packageName)
        bundle?.let { intent.putExtras(it) }
        context.sendBroadcast(intent)
    }
}