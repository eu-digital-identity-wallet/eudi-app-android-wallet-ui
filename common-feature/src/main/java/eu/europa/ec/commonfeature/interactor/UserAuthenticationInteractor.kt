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

package eu.europa.ec.commonfeature.interactor

import android.content.Context
import eu.europa.ec.businesslogic.controller.biometry.BiometricsAvailability
import eu.europa.ec.businesslogic.controller.biometry.BiometryCrypto
import eu.europa.ec.businesslogic.controller.biometry.UserAuthenticationController
import eu.europa.ec.businesslogic.controller.biometry.UserAuthenticationResult

interface UserAuthenticationInteractor {
    fun getBiometricsAvailability(listener: (BiometricsAvailability) -> Unit)
    fun authenticateWithBiometrics(
        context: Context,
        crypto: BiometryCrypto,
        resultHandler: UserAuthenticationResult
    )
}

class UserAuthenticationInteractorImpl(private val userAuthenticationController: UserAuthenticationController) :
    UserAuthenticationInteractor {
    override fun getBiometricsAvailability(listener: (BiometricsAvailability) -> Unit) {
        userAuthenticationController.deviceSupportsBiometrics(listener)
    }

    override fun authenticateWithBiometrics(
        context: Context,
        crypto: BiometryCrypto,
        resultHandler: UserAuthenticationResult
    ) {
        userAuthenticationController.authenticate(
            context,
            crypto,
            resultHandler
        )
    }
}