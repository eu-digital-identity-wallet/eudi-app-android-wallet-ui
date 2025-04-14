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

    data object QuickPin :
        Screen(name = "QUICK_PIN", parameters = "?pinFlow={pinFlow}")

    data object QrScan : Screen(
        name = "QR_SCAN",
        parameters = "?qrScanConfig={qrScanConfig}"
    )
}

sealed class DashboardScreens {
    data object Dashboard : Screen(name = "DASHBOARD")
    data object SignDocument :
        Screen(name = "SIGN_DOCUMENT")
}

sealed class PresentationScreens {
    data object PresentationRequest : Screen(
        name = "PRESENTATION_REQUEST",
        parameters = "?requestUriConfig={requestUriConfig}"
    )

    data object PresentationLoading : Screen(name = "PRESENTATION_LOADING")

    data object PresentationSuccess : Screen(name = "PRESENTATION_SUCCESS")
}

sealed class ProximityScreens {
    data object QR : Screen(
        name = "PROXIMITY_QR",
        parameters = "?requestUriConfig={requestUriConfig}"
    )

    data object Request : Screen(
        name = "PROXIMITY_REQUEST",
        parameters = "?requestUriConfig={requestUriConfig}"
    )

    data object Loading : Screen(name = "PROXIMITY_LOADING")

    data object Success : Screen(name = "PROXIMITY_SUCCESS")
}

sealed class IssuanceScreens {
    data object AddDocument : Screen(
        name = "ISSUANCE_ADD_DOCUMENT",
        parameters = "?flowType={flowType}"
    )

    data object DocumentDetails : Screen(
        name = "ISSUANCE_DOCUMENT_DETAILS",
        parameters = "?documentId={documentId}"
    )

    data object TransactionDetails : Screen(
        name = "ISSUANCE_TRANSACTION_DETAILS",
        parameters = "?transactionId={transactionId}"
    )

    data object DocumentOffer : Screen(
        name = "ISSUANCE_DOCUMENT_OFFER",
        parameters = "?offerConfig={offerConfig}"
    )

    data object DocumentOfferCode : Screen(
        name = "ISSUANCE_DOCUMENT_OFFER_CODE",
        parameters = "?offerCodeConfig={offerCodeConfig}"
    )

    data object DocumentIssuanceSuccess : Screen(
        name = "ISSUANCE_DOCUMENT_SUCCESS",
        parameters = "?issuanceSuccessConfig={issuanceSuccessConfig}"
    )
}

sealed class ModuleRoute(val route: String) : NavigatableItem {
    data object StartupModule : ModuleRoute("STARTUP_MODULE")
    data object CommonModule : ModuleRoute("COMMON_MODULE")
    data object DashboardModule : ModuleRoute("DASHBOARD_MODULE")
    data object PresentationModule : ModuleRoute("PRESENTATION_MODULE")
    data object ProximityModule : ModuleRoute("PROXIMITY_MODULE")
    data object IssuanceModule : ModuleRoute("ISSUANCE_MODULE")
}