/*
 * Copyright (c) 2025 European Commission
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
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAvailability
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationController
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.authenticationlogic.model.BiometricCrypto

interface DeviceAuthenticationInteractor {
    fun getBiometricsAvailability(listener: (BiometricsAvailability) -> Unit)
    fun authenticateWithBiometrics(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult
    )

    fun launchBiometricSystemScreen()
}

class DeviceAuthenticationInteractorImpl(
    private val deviceAuthenticationController: DeviceAuthenticationController,
) : DeviceAuthenticationInteractor {

    override fun launchBiometricSystemScreen() {
        deviceAuthenticationController.launchBiometricSystemScreen()
    }

    override fun getBiometricsAvailability(listener: (BiometricsAvailability) -> Unit) {
        deviceAuthenticationController.deviceSupportsBiometrics(listener)
    }

    override fun authenticateWithBiometrics(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult
    ) {
        deviceAuthenticationController.authenticate(
            context,
            crypto,
            notifyOnAuthenticationFailure,
            resultHandler
        )
    }
}