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

package eu.europa.ec.businesslogic.controller.biometry

import android.content.Context
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider

interface UserAuthenticationController {
    fun deviceSupportsBiometrics(listener: (BiometricsAvailability) -> Unit)
    fun authenticate(context: Context, payload: BiometricPromptPayload)
}

class UserAuthenticationControllerImpl(
    private val resourceProvider: ResourceProvider,
    private val biometricController: BiometricController
) : UserAuthenticationController {
    override fun deviceSupportsBiometrics(listener: (BiometricsAvailability) -> Unit) {
        biometricController.deviceSupportsBiometrics(listener)
    }

    override fun authenticate(context: Context, payload: BiometricPromptPayload) {
        context as FragmentActivity

        val prompt = BiometricPrompt(
            context,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    payload.onCancel()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    payload.onSuccess()
                }

                override fun onAuthenticationFailed() {
                    payload.onFailure()
                }
            }
        )

        if (payload.cryptoObject != null) {
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(resourceProvider.getString(R.string.biometric_prompt_title))
                    .setSubtitle(resourceProvider.getString(R.string.biometric_prompt_subtitle))
                    .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                    .build(),
                payload.cryptoObject
            )
        } else {
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(resourceProvider.getString(R.string.biometric_prompt_title))
                    .setSubtitle(resourceProvider.getString(R.string.biometric_prompt_subtitle))
                    .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                    .build()
            )
        }
    }
}

data class BiometricPromptPayload(
    val cryptoObject: BiometricPrompt.CryptoObject?,
    val onSuccess: () -> Unit,
    val onCancel: () -> Unit,
    val onFailure: () -> Unit,
)
