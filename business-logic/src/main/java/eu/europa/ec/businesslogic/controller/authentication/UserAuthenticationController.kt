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

package eu.europa.ec.businesslogic.controller.authentication

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import eu.europa.ec.businesslogic.controller.biometry.BiometricController
import eu.europa.ec.businesslogic.controller.biometry.BiometricsAvailability
import eu.europa.ec.businesslogic.model.BiometricCrypto
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.launch

interface UserAuthenticationController {
    fun deviceSupportsBiometrics(listener: (BiometricsAvailability) -> Unit)
    fun authenticate(
        context: Context,
        biometryCrypto: BiometricCrypto,
        userAuthenticationBiometricResult: UserAuthenticationResult
    )
}

class UserAuthenticationControllerImpl(
    private val resourceProvider: ResourceProvider,
    private val biometricController: BiometricController
) : UserAuthenticationController {
    override fun deviceSupportsBiometrics(listener: (BiometricsAvailability) -> Unit) {
        biometricController.deviceSupportsBiometrics(listener)
    }

    override fun authenticate(
        context: Context,
        biometryCrypto: BiometricCrypto,
        userAuthenticationBiometricResult: UserAuthenticationResult
    ) {
        (context as? FragmentActivity)?.let { activity ->

            activity.lifecycleScope.launch {

                val data = biometricController.authenticate(
                    activity = activity,
                    biometryCrypto = biometryCrypto,
                    promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle(resourceProvider.getString(R.string.biometric_prompt_title))
                        .setSubtitle(resourceProvider.getString(R.string.biometric_prompt_subtitle))
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                        .build()
                )

                if (data.authenticationResult != null) {
                    userAuthenticationBiometricResult.onAuthenticationSuccess()
                } else if (data.hasError) {
                    userAuthenticationBiometricResult.onAuthenticationError()
                } else {
                    userAuthenticationBiometricResult.onAuthenticationFailure()
                }
            }
        }
    }
}

data class UserAuthenticationResult(
    val onAuthenticationSuccess: () -> Unit = {},
    val onAuthenticationError: () -> Unit = {},
    val onAuthenticationFailure: () -> Unit = {},
)