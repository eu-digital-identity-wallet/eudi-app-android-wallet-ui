/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.europa.ec.eudi.wallet.ui.userauth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import eu.europa.ec.eudi.wallet.ui.util.log

class UserAuthPromptBuilder private constructor(private val fragment: Fragment) {

    private var title: String = ""
    private var subtitle: String = ""
    private var description: String = ""
    private var negativeButton: String = ""
    private var forceLskf: Boolean = false
    private var onSuccess: () -> Unit = {}
    private var onFailure: () -> Unit = {}
    private var onCancelled: () -> Unit = {}

    private val biometricAuthCallback = object : BiometricPrompt.AuthenticationCallback() {

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            // reached max attempts to authenticate the user, or authentication dialog was cancelled
            if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                onCancelled.invoke()
            } else {
                log("User authentication failed $errorCode - $errString")
                onFailure.invoke()
            }
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            log("User authentication succeeded")
            onSuccess.invoke()
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            log("User authentication failed")
            onFailure.invoke()
        }
    }

    fun withTitle(title: String) = apply {
        this.title = title
    }

    fun withSubtitle(subtitle: String) = apply {
        this.subtitle = subtitle
    }

    fun withDescription(description: String) = apply {
        this.description = description
    }

    fun withNegativeButton(negativeButton: String) = apply {
        this.negativeButton = negativeButton
    }

    fun setForceLskf(forceLskf: Boolean) = apply {
        this.forceLskf = forceLskf
    }

    fun withSuccessCallback(onSuccess: () -> Unit) = apply {
        this.onSuccess = onSuccess
    }

    fun withFailureCallback(onFailure: () -> Unit) = apply {
        this.onFailure = onFailure
    }

    fun withCancelledCallback(onCancelled: () -> Unit) = apply {
        this.onCancelled = onCancelled
    }

    fun build(): BiometricUserAuthPrompt {
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setConfirmationRequired(false)

        if (forceLskf) {
            // TODO: this works only on Android 11 or later but for now this is fine
            //   as this is just a reference/test app and this path is only hit if
            //   the user actually presses the "Use PIN" button.  Longer term, we should
            //   fall back to using KeyGuard which will work on all Android versions.
            promptInfoBuilder.setAllowedAuthenticators(DEVICE_CREDENTIAL)
        } else {
            val canUseBiometricAuth = BiometricManager
                .from(fragment.requireContext())
                .canAuthenticate(BIOMETRIC_STRONG) == BIOMETRIC_SUCCESS
            if (canUseBiometricAuth) {
                promptInfoBuilder.setNegativeButtonText(negativeButton)
            } else {
                // No biometrics enrolled, force use of LSKF
                promptInfoBuilder.setDeviceCredentialAllowed(true)
            }
        }

        val promptInfo = promptInfoBuilder.build()
        val executor = ContextCompat.getMainExecutor(fragment.requireContext())
        val prompt = BiometricPrompt(fragment, executor, biometricAuthCallback)
        return BiometricUserAuthPrompt(prompt, promptInfo)
    }

    companion object {
        fun requestUserAuth(fragment: Fragment): UserAuthPromptBuilder {
            return UserAuthPromptBuilder(fragment)
        }
    }
}