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
import androidx.navigation.NavController
import eu.europa.ec.businesslogic.util.safeLet
import eu.europa.ec.corelogic.util.CoreActions
import eu.europa.ec.eudi.rqesui.domain.entities.error.EudiRQESUiError
import eu.europa.ec.eudi.rqesui.infrastructure.EudiRQESUi
import eu.europa.ec.eudi.rqesui.infrastructure.RemoteUri
import eu.europa.ec.uilogic.extension.openInBrowser
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
                host = uri.host,
                path = uri.path
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

/**
 * Opens the flow [action] starts. A link of no known flow ([DeepLinkType.EXTERNAL]) is opened only
 * when it is an `https` link, and then only in a browser: as a plain view intent it could come back
 * to the wallet through one of its own link filters.
 */
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
                bundle = Bundle().apply { putString("uri", action.link.toString()) }
            )
            return
        }

        DeepLinkType.EXTERNAL -> {
            if (isOpenableExternalLink(action.link.toString())) {
                navController.context.openInBrowser(action.link)
            }
            return
        }

        DeepLinkType.DYNAMIC_PRESENTATION -> {
            notify(
                context = navController.context,
                action = CoreActions.VCI_DYNAMIC_PRESENTATION,
                bundle = Bundle().apply { putString("uri", action.link.toString()) }
            )
            return
        }

        DeepLinkType.RQES -> {
            action.link.getQueryParameter("code")?.let { authorizationCode ->
                ignoreRqesSdkRefusal {
                    EudiRQESUi.resume(
                        context = navController.context,
                        authorizationCode = authorizationCode
                    )
                }
            }
            return
        }

        DeepLinkType.RQES_DOC_RETRIEVAL -> {
            ignoreRqesSdkRefusal {
                EudiRQESUi.initiate(
                    context = navController.context,
                    remoteUri = RemoteUri(action.link)
                )
            }
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

/**
 * Runs an RQES SDK call made for a link from outside the app. The SDK throws [EudiRQESUiError] when
 * the link does not fit its state, for example a sign-in callback when no signing session was
 * started in this process, or a document link it cannot decode. Any app or web page can send such
 * a link, so it is ignored instead of ending the wallet's process.
 */
private inline fun ignoreRqesSdkRefusal(block: () -> Unit) {
    try {
        block()
    } catch (_: EudiRQESUiError) {
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