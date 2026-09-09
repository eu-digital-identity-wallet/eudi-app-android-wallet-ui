/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.authenticationlogic.controller.authentication

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

interface DeviceAuthenticationController {
    fun deviceSupportsBiometrics(crypto: BiometricCrypto): BiometricsAvailability
    fun authenticate(
        context: Context,
        biometryCrypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        result: DeviceAuthenticationResult
    )

    fun launchBiometricSystemScreen(crypto: BiometricCrypto)
}

class DeviceAuthenticationControllerImpl(
    private val resourceProvider: ResourceProvider,
    private val biometricAuthenticationController: BiometricAuthenticationController
) : DeviceAuthenticationController {

    override fun deviceSupportsBiometrics(crypto: BiometricCrypto): BiometricsAvailability {
        return biometricAuthenticationController.getBiometricsAvailability(
            getAllowedAuthenticators(crypto)
        )
    }

    override fun authenticate(
        context: Context,
        biometryCrypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        result: DeviceAuthenticationResult
    ) {
        val activity = context as? FragmentActivity
        if (activity == null) {
            result.onAuthenticationError()
            return
        }

        var resultDelivered = false
        activity.lifecycleScope.launch {

            val data = try {
                val authenticators = getAllowedAuthenticators(biometryCrypto)
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(resourceProvider.getString(R.string.biometric_prompt_title))
                    .setSubtitle(resourceProvider.getString(R.string.biometric_prompt_subtitle))
                    .setAllowedAuthenticators(authenticators)
                    .apply {
                        if (authenticators and DEVICE_CREDENTIAL == 0) {
                            setNegativeButtonText(resourceProvider.getString(R.string.generic_cancel))
                        }
                    }
                    .build()

                biometricAuthenticationController.authenticate(
                    activity = activity,
                    biometryCrypto = biometryCrypto,
                    promptInfo = promptInfo,
                    notifyOnAuthenticationFailure = notifyOnAuthenticationFailure
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                resultDelivered = true
                result.onAuthenticationError()
                return@launch
            }

            resultDelivered = true
            if (data.authenticationResult != null) {
                result.onAuthenticationSuccess()
            } else if (data.hasError) {
                result.onAuthenticationError()
            } else {
                result.onAuthenticationFailure()
            }
        }.invokeOnCompletion { cause ->
            if (!resultDelivered && cause is CancellationException) {
                resultDelivered = true
                result.onAuthenticationError()
            }
        }
    }

    override fun launchBiometricSystemScreen(crypto: BiometricCrypto) {
        biometricAuthenticationController.launchBiometricSystemScreen(
            getAllowedAuthenticators(crypto)
        )
    }

    private fun getAllowedAuthenticators(crypto: BiometricCrypto): Int = when {
        crypto.cryptoObject == null -> BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        else -> BIOMETRIC_STRONG
    }
}

data class DeviceAuthenticationResult(
    val onAuthenticationSuccess: suspend () -> Unit = {},
    val onAuthenticationError: () -> Unit = {},
    val onAuthenticationFailure: () -> Unit = {},
)