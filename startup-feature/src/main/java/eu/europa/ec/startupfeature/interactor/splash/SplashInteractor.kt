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

package eu.europa.ec.startupfeature.interactor.splash

import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.commonfeature.config.BiometricUiConfig
import eu.europa.ec.commonfeature.model.PinFlow
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.LoginScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer

interface SplashInteractor {
    fun getAfterSplashRoute(): String
}

class SplashInteractorImpl(
    private val prefKeys: PrefKeys,
    private val uiSerializer: UiSerializer,
    private val resourceProvider: ResourceProvider,
) : SplashInteractor {

    override fun getAfterSplashRoute(): String {
        val alreadyHasPin = getDevicePin().isNotBlank()

        val nextScreen = if (alreadyHasPin) {
            getBiometricsConfig()
        } else {
            getQuickPinConfig()
        }

        return nextScreen
    }

    private fun getDevicePin(): String {
        return prefKeys.getDevicePin()
    }

    private fun getQuickPinConfig(): String {
        return generateComposableNavigationLink(
            screen = LoginScreens.QuickPin,
            arguments = generateComposableArguments(mapOf("pinFlow" to PinFlow.CREATE))
        )
    }

    private fun getBiometricsConfig(): String {
        return generateComposableNavigationLink(
            screen = CommonScreens.Biometric,
            arguments = generateComposableArguments(
                mapOf(
                    BiometricUiConfig.serializedKeyName to uiSerializer.toBase64(
                        BiometricUiConfig(
                            title = resourceProvider.getString(R.string.biometric_login_prompt_title),
                            subTitle = resourceProvider.getString(R.string.biometric_login_prompt_subtitle),
                            quickPinOnlySubTitle = resourceProvider.getString(R.string.biometric_login_prompt_quickPinOnlySubTitle),
                            isPreAuthorization = false,
                            shouldInitializeBiometricAuthOnCreate = true,
                            onSuccessNavigation = ConfigNavigation(
                                navigationType = NavigationType.PUSH,
                                screenToNavigate = DashboardScreens.Dashboard
                            ),
                            onBackNavigation = null
                        ),
                        BiometricUiConfig.Parser
                    ).orEmpty()
                )
            )
        )
    }
}