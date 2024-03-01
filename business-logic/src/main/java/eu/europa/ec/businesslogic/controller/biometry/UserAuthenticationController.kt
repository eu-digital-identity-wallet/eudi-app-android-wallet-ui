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
    fun authenticate(
        context: Context,
        biometryCrypto: BiometryCrypto,
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
        biometryCrypto: BiometryCrypto,
        userAuthenticationBiometricResult: UserAuthenticationResult
    ) {
        context as FragmentActivity

        val prompt = BiometricPrompt(
            context,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    userAuthenticationBiometricResult.onAuthenticationError()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    userAuthenticationBiometricResult.onAuthenticationSuccess()
                }

                override fun onAuthenticationFailed() {
                    userAuthenticationBiometricResult.onAuthenticationFailure()
                }
            }
        )

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(resourceProvider.getString(R.string.biometric_prompt_title))
            .setSubtitle(resourceProvider.getString(R.string.biometric_prompt_subtitle))
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()

        biometryCrypto.cryptoObject?.let {
            prompt.authenticate(
                builder,
                it
            )
        } ?: prompt.authenticate(builder)
    }
}

data class UserAuthenticationResult(
    val onAuthenticationSuccess: () -> Unit = {},
    val onAuthenticationError: () -> Unit = {},
    val onAuthenticationFailure: () -> Unit = {},
)

data class BiometryCrypto(val cryptoObject: BiometricPrompt.CryptoObject?)