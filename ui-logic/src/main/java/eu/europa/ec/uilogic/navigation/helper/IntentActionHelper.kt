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

import android.content.Intent
import androidx.navigation.NavController
import eu.europa.ec.uilogic.extension.navigateWithIntentAction
import eu.europa.ec.uilogic.navigation.PresentationScreens
import eu.europa.ec.uilogic.navigation.Screen

fun hasIntentAction(intent: Intent?): IntentAction? {
    return intent?.toIntentAction()
}

fun handleIntentAction(
    navController: NavController,
    action: IntentAction,
    arguments: String? = null
) {
    val screen: Screen = when (action.type) {
        IntentType.DC_API -> PresentationScreens.PresentationRequest
    }

    val navigationLink = arguments?.let {
        generateComposableNavigationLink(
            screen = screen,
            arguments = arguments
        )
    } ?: screen.screenRoute

    navController.navigateWithIntentAction(
        route = navigationLink,
        intentAction = action,
        builder = {
            popUpTo(screen.screenRoute) {
                inclusive = true
            }
        }
    )
}

private fun Intent.toIntentAction(): IntentAction? {
    return IntentType
        .entries
        .firstOrNull {
            it.associatedActions.contains(this.action?.lowercase())
        }?.let { matchedType ->
            IntentAction(intent = this, type = matchedType)
        }
}