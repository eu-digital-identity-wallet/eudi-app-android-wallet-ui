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
import eu.europa.ec.businesslogic.controller.biometry.BiometricPromptPayload
import eu.europa.ec.businesslogic.controller.biometry.BiometricsAvailability
import eu.europa.ec.businesslogic.controller.biometry.DeviceBiometricController

interface DeviceBiometricInteractor {
    fun getBiometricsAvailability(listener: (BiometricsAvailability) -> Unit)
    fun authenticateWithBiometrics(
        context: Context,
        payload: BiometricPromptPayload
    )
}

class DeviceBiometricInteractorImpl(private val deviceBiometricController: DeviceBiometricController) :
    DeviceBiometricInteractor {
    override fun getBiometricsAvailability(listener: (BiometricsAvailability) -> Unit) {
        deviceBiometricController.deviceSupportsBiometrics(listener)
    }

    override fun authenticateWithBiometrics(context: Context, payload: BiometricPromptPayload) {
        deviceBiometricController.authenticate(context, payload)
    }

}