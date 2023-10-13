/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.commonfeature.interactor

import android.content.Context
import eu.europa.ec.businesslogic.controller.biometry.BiometricController
import eu.europa.ec.businesslogic.controller.biometry.BiometricsAuthenticate
import eu.europa.ec.businesslogic.controller.biometry.BiometricsAvailability
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import kotlinx.coroutines.flow.Flow

interface BiometricInteractor {
    fun getBiometricsAvailability(listener: (BiometricsAvailability) -> Unit)
    fun getBiometricUserSelection(): Boolean
    fun storeBiometricsUsageDecision(shouldUseBiometrics: Boolean)
    fun authenticateWithBiometrics(
        context: Context,
        listener: (BiometricsAuthenticate) -> Unit
    )

    fun launchBiometricSystemScreen()
    fun isPinValid(pin: String): Flow<QuickPinInteractorPinValidPartialState>
}

class BiometricInteractorImpl constructor(
    private val prefKeys: PrefKeys,
    private val biometricController: BiometricController,
    private val quickPinInteractor: QuickPinInteractor,
) : BiometricInteractor {

    override fun isPinValid(pin: String): Flow<QuickPinInteractorPinValidPartialState> =
        quickPinInteractor.isCurrentPinValid(pin)

    override fun storeBiometricsUsageDecision(shouldUseBiometrics: Boolean) {
        prefKeys.setUseBiometricsAuth(shouldUseBiometrics)
    }

    override fun getBiometricUserSelection(): Boolean {
        return prefKeys.getUseBiometricsAuth()
    }

    override fun getBiometricsAvailability(listener: (BiometricsAvailability) -> Unit) {
        biometricController.deviceSupportsBiometrics(listener)
    }

    override fun authenticateWithBiometrics(
        context: Context,
        listener: (BiometricsAuthenticate) -> Unit
    ) {
        biometricController.authenticate(context, listener)
    }

    override fun launchBiometricSystemScreen() {
        biometricController.launchBiometricSystemScreen()
    }
}