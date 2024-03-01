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
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationResult
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import eu.europa.ec.businesslogic.controller.crypto.CryptoController
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.businesslogic.extension.decodeFromPemBase64String
import eu.europa.ec.businesslogic.extension.encodeToPemBase64String
import eu.europa.ec.businesslogic.model.BiometricData
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import kotlin.coroutines.resume

enum class BiometricsAuthError(val code: Int) {
    Cancel(10), CancelByUser(13)
}

interface BiometricController {
    fun deviceSupportsBiometrics(listener: (BiometricsAvailability) -> Unit)
    fun authenticate(context: Context, listener: (BiometricsAuthenticate) -> Unit)
    fun launchBiometricSystemScreen()
}

class BiometricControllerImpl(
    private val resourceProvider: ResourceProvider,
    private val cryptoController: CryptoController,
    private val prefsKeys: PrefKeys
) : BiometricController {

    override fun deviceSupportsBiometrics(listener: (BiometricsAvailability) -> Unit) {
        val biometricManager = BiometricManager.from(resourceProvider.provideContext())
        when (biometricManager.canAuthenticate(BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> listener.invoke(BiometricsAvailability.CanAuthenticate)
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> listener.invoke(BiometricsAvailability.NonEnrolled)
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE, BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> listener.invoke(
                BiometricsAvailability.Failure(resourceProvider.getString(R.string.biometric_no_hardware))
            )

            else -> listener.invoke(BiometricsAvailability.Failure(resourceProvider.getString(R.string.biometric_unknown_error)))
        }
    }

    override fun authenticate(context: Context, listener: (BiometricsAuthenticate) -> Unit) {
        (context as? FragmentActivity)?.let { activity ->

            activity.lifecycleScope.launch {

                val storedCrypto = retrieveCrypto()
                val biometricData = storedCrypto.first
                val cipher = storedCrypto.second

                if (cipher == null) {
                    listener.invoke(
                        BiometricsAuthenticate.Failed(context.getString(R.string.generic_error_description))
                    )
                    return@launch
                }

                val data = authenticate(activity = activity, cipher = cipher)

                if (data.authenticationResult != null) {
                    val state = verifyCrypto(
                        context = context,
                        result = data.authenticationResult,
                        biometricData = biometricData
                    )
                    listener.invoke(state)
                } else if (
                    data.errorCode != BiometricsAuthError.Cancel.code &&
                    data.errorCode != BiometricsAuthError.CancelByUser.code
                ) {
                    listener.invoke(BiometricsAuthenticate.Failed(data.errorString.toString()))
                } else {
                    listener.invoke(BiometricsAuthenticate.Cancelled)
                }
            }
        }
    }

    override fun launchBiometricSystemScreen() {
        val enrollIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BIOMETRIC_WEAK
                )
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        enrollIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        resourceProvider.provideContext().startActivity(enrollIntent)
    }

    private suspend fun authenticate(
        activity: FragmentActivity,
        cipher: Cipher,
    ): BiometricPromptData = suspendCancellableCoroutine { continuation ->
        BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (continuation.isActive) {
                        continuation.resume(
                            BiometricPromptData(null, errorCode, errString)
                        )
                    }
                }

                override fun onAuthenticationSucceeded(result: AuthenticationResult) {
                    if (continuation.isActive) {
                        continuation.resume(BiometricPromptData(result))
                    }
                }

                override fun onAuthenticationFailed() {
                    if (continuation.isActive) {
                        continuation.resume(BiometricPromptData(null))
                    }
                }
            }
        ).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.biometric_prompt_title))
                .setSubtitle(activity.getString(R.string.biometric_prompt_subtitle))
                .setNegativeButtonText(activity.getString(R.string.generic_cancel))
                .build(),
            BiometricPrompt.CryptoObject(cipher)
        )
    }

    private suspend fun retrieveCrypto(): Pair<BiometricData?, Cipher?> =
        withContext(Dispatchers.IO) {
            val biometricData = prefsKeys.getBiometricData()
            val cipher = cryptoController.getBiometricCipher(
                encrypt = biometricData == null,
                ivBytes = biometricData?.ivString?.decodeFromPemBase64String() ?: ByteArray(0)
            )
            Pair(biometricData, cipher)
        }

    private suspend fun verifyCrypto(
        context: Context,
        result: AuthenticationResult?,
        biometricData: BiometricData?
    ): BiometricsAuthenticate = withContext(Dispatchers.IO) {
        result?.cryptoObject?.cipher?.let {
            if (biometricData == null) {
                val randomString = cryptoController.generateCodeVerifier()
                prefsKeys.setBiometricData(
                    BiometricData(
                        randomString = randomString,
                        encryptedString = cryptoController.encryptDecryptBiometric(
                            cipher = it,
                            byteArray = randomString.toByteArray(StandardCharsets.UTF_8)
                        ).encodeToPemBase64String().orEmpty(),
                        ivString = it.iv.encodeToPemBase64String().orEmpty()
                    )
                )
                BiometricsAuthenticate.Success
            } else {
                if (biometricData.randomString
                        .toByteArray(StandardCharsets.UTF_8)
                        .contentEquals(
                            cryptoController.encryptDecryptBiometric(
                                cipher = it,
                                byteArray = biometricData.encryptedString
                                    .decodeFromPemBase64String() ?: ByteArray(0)
                            )
                        )
                ) {
                    BiometricsAuthenticate.Success
                } else {
                    BiometricsAuthenticate.Failed(context.getString(R.string.generic_error_description))
                }
            }
        } ?: BiometricsAuthenticate.Failed(context.getString(R.string.generic_error_description))
    }
}

sealed class BiometricsAuthenticate {
    data object Success : BiometricsAuthenticate()
    data class Failed(val errorMessage: String) : BiometricsAuthenticate()
    data object Cancelled : BiometricsAuthenticate()
}

sealed class BiometricsAvailability {
    data object CanAuthenticate : BiometricsAvailability()
    data object NonEnrolled : BiometricsAvailability()
    data class Failure(val errorMessage: String) : BiometricsAvailability()
}

data class BiometricPromptData(
    val authenticationResult: AuthenticationResult?,
    val errorCode: Int = -1,
    val errorString: CharSequence = "",
)