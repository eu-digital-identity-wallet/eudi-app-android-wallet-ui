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
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.LoginScreens
import eu.europa.ec.uilogic.navigation.Screen

interface SplashInteractor {
    fun getAfterSplashRoute(): String
    fun getAfterSplashRoute(): Screen
    fun getBiometricsScreenRoute(): Screen
    fun getDevicePin(): String
}

class SplashInteractorImpl(
    private val prefKeys: PrefKeys,
) : SplashInteractor {

    override fun getAfterSplashRoute(): String {
        return LoginScreens.QuickPin.screenRoute
    }

    override fun getBiometricsScreenRoute(): Screen {
        return CommonScreens.Biometric
    }

    override fun getDevicePin(): String {
        return prefKeys.getDevicePin()
    }
}