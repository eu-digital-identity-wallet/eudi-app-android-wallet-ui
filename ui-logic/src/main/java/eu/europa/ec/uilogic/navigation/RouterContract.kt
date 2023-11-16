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

package eu.europa.ec.uilogic.navigation

interface NavigatableItem

open class Screen(name: String, parameters: String = "") : NavigatableItem {
    val screenRoute: String = name + parameters
    val screenName = name
}

sealed class StartupScreens {
    data object Splash : Screen(name = "SPLASH")
}

sealed class CommonScreens {
    data object Success : Screen(name = "SUCCESS", parameters = "?successConfig={successConfig}")
    data object Biometric : Screen(
        name = "BIOMETRIC",
        parameters = "?biometricConfig={biometricConfig}"
    )
}

sealed class DashboardScreens {
    data object Dashboard : Screen(name = "DASHBOARD")
}

sealed class LoginScreens {
    data object Welcome : Screen(name = "WELCOME")
    data object Faq : Screen(name = "FAQ")

    data object QuickPin : Screen(name = "WIZARD_LOGIN_QUICK_PIN_SCREEN", parameters = "?payload={payload}")

    data object Pin : Screen(name = "WIZARD_LOGIN_PIN_SCREEN", parameters = "?payload={payload}")

    data object RePin :
        Screen(name = "WIZARD_LOGIN_RE_PIN_SCREEN", parameters = "?payload={payload}")

}

sealed class PresentationScreens {
    data object CrossDeviceRequest : Screen(name = "PRESENTATION_CROSS_DEVICE_REQUEST")
    data object CrossDeviceLoading : Screen(name = "PRESENTATION_CROSS_DEVICE_LOADING")

    data object SameDeviceRequest : Screen(name = "PRESENTATION_SAME_DEVICE_REQUEST")
    data object SameDeviceLoading : Screen(name = "PRESENTATION_SAME_DEVICE_LOADING")
}

sealed class ProximityScreens {
    data object QR : Screen(name = "PROXIMITY_QR")
    data object Request : Screen(name = "PROXIMITY_REQUEST")
    data object Loading : Screen(name = "PROXIMITY_LOADING")
}

sealed class IssuanceScreens {
    data object AddDocument : Screen(
        name = "ISSUANCE_ADD_DOCUMENT",
        parameters = "?flowType={flowType}"
    )

    data object Authenticate : Screen(
        name = "ISSUANCE_AUTHENTICATE",
        parameters = "?flowType={flowType}"
                + "&documentType={documentType}"
    )

    data object Success : Screen(
        name = "ISSUANCE_SUCCESS",
        parameters = "?flowType={flowType}"
                + "&documentType={documentType}"
    )

    data object DocumentDetails : Screen(
        name = "ISSUANCE_DOCUMENT_DETAILS",
        parameters = "?detailsType={detailsType}"
                + "&documentId={documentId}"
    )
}

sealed class ModuleRoute(val route: String) : NavigatableItem {
    data object StartupModule : ModuleRoute("STARTUP_MODULE")
    data object CommonModule : ModuleRoute("COMMON_MODULE")
    data object LoginModule : ModuleRoute("LOGIN_MODULE")
    data object DashboardModule : ModuleRoute("DASHBOARD_MODULE")
    data object PresentationModule : ModuleRoute("PRESENTATION_MODULE")
    data object ProximityModule : ModuleRoute("PROXIMITY_MODULE")
    data object IssuanceModule : ModuleRoute("ISSUANCE_MODULE")
}