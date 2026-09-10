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
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationResult
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import eu.europa.ec.authenticationlogic.controller.storage.BiometryStorageController
import eu.europa.ec.authenticationlogic.model.BiometricAuthentication
import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.businesslogic.controller.crypto.CryptoController
import eu.europa.ec.businesslogic.extension.decodeFromPemBase64String
import eu.europa.ec.businesslogic.extension.encodeToPemBase64String
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import kotlin.coroutines.resume

enum class BiometricsAuthError(val code: Int) {
    Cancel(BiometricPrompt.ERROR_USER_CANCELED),
    CancelByUser(BiometricPrompt.ERROR_NEGATIVE_BUTTON),
    CancelBySystem(BiometricPrompt.ERROR_CANCELED)
}

interface BiometricAuthenticationController {
    fun getBiometricsAvailability(authenticators: Int): BiometricsAvailability

    fun authenticate(
        context: Context,
        notifyOnAuthenticationFailure: Boolean,
        listener: (BiometricsAuthenticate) -> Unit
    )

    suspend fun authenticate(
        activity: FragmentActivity,
        biometryCrypto: BiometricCrypto,
        promptInfo: BiometricPrompt.PromptInfo,
        notifyOnAuthenticationFailure: Boolean,
    ): BiometricPromptData

    fun launchBiometricSystemScreen(authenticators: Int = BIOMETRIC_WEAK)
}

class BiometricAuthenticationControllerImpl(
    private val resourceProvider: ResourceProvider,
    private val cryptoController: CryptoController,
    private val biometryStorageController: BiometryStorageController,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BiometricAuthenticationController {

    override fun getBiometricsAvailability(authenticators: Int): BiometricsAvailability {
        val biometricManager = BiometricManager.from(resourceProvider.provideContext())
        val canAuthenticate = biometricManager.canAuthenticate(authenticators)
        val canAuthenticateWeak =
            if (authenticators == BIOMETRIC_STRONG && canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
                biometricManager.canAuthenticate(BIOMETRIC_WEAK)
            } else {
                BiometricManager.BIOMETRIC_SUCCESS
            }

        return resolveBiometricsAvailability(
            canAuthenticate = canAuthenticate,
            requireStrong = authenticators == BIOMETRIC_STRONG,
            canAuthenticateWeak = canAuthenticateWeak
        )
    }

    override fun authenticate(
        context: Context,
        notifyOnAuthenticationFailure: Boolean,
        listener: (BiometricsAuthenticate) -> Unit
    ) {
        val activity = context as? FragmentActivity
        if (activity == null) {
            listener.invoke(
                BiometricsAuthenticate.Failed(context.getString(R.string.generic_error_description))
            )
            return
        }

        activity.lifecycleScope.launch {
            val result = try {
                authenticateWithCrypto(activity, notifyOnAuthenticationFailure)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                BiometricsAuthenticate.Failed(context.getString(R.string.biometric_authentication_error))
            }
            listener(result)
        }.invokeOnCompletion { cause ->
            if (cause is CancellationException) {
                listener(BiometricsAuthenticate.Cancelled)
            }
        }
    }

    override fun launchBiometricSystemScreen(authenticators: Int) {
        val enrollIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    authenticators
                )
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        enrollIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        resourceProvider.provideContext().startActivity(enrollIntent)
    }

    override suspend fun authenticate(
        activity: FragmentActivity,
        biometryCrypto: BiometricCrypto,
        promptInfo: BiometricPrompt.PromptInfo,
        notifyOnAuthenticationFailure: Boolean
    ): BiometricPromptData = suspendCancellableCoroutine { continuation ->
        if (activity.isFinishing || activity.isDestroyed || activity.supportFragmentManager.isStateSaved) {
            continuation.resume(BiometricPromptData(null, BiometricPrompt.ERROR_CANCELED))
            return@suspendCancellableCoroutine
        }

        lateinit var prompt: BiometricPrompt
        var completed = false

        fun complete(data: BiometricPromptData, cancelPrompt: Boolean = false) {
            if (completed || !continuation.isActive) return
            completed = true
            if (cancelPrompt) prompt.cancelAuthentication()
            continuation.resume(data)
        }

        prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    complete(BiometricPromptData(null, errorCode, errString))
                }

                override fun onAuthenticationSucceeded(result: AuthenticationResult) {
                    complete(BiometricPromptData(result))
                }

                override fun onAuthenticationFailed() {
                    if (notifyOnAuthenticationFailure) {
                        complete(BiometricPromptData(null), cancelPrompt = true)
                    }
                }
            }
        )
        continuation.invokeOnCancellation {
            completed = true
            prompt.cancelAuthentication()
        }
        biometryCrypto.cryptoObject?.let {
            prompt.authenticate(
                promptInfo,
                it
            )
        } ?: prompt.authenticate(promptInfo)
    }

    private fun resolveBiometricsAvailability(
        canAuthenticate: Int,
        requireStrong: Boolean,
        canAuthenticateWeak: Int,
    ): BiometricsAvailability = when (canAuthenticate) {
        BiometricManager.BIOMETRIC_SUCCESS,
        BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricsAvailability.CanAuthenticate

        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
            if (requireStrong && canAuthenticateWeak == BiometricManager.BIOMETRIC_SUCCESS) {
                BiometricsAvailability.Failure(resourceProvider.getString(R.string.biometric_strong_required))
            } else {
                BiometricsAvailability.NonEnrolled
            }

        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
            if (requireStrong && canAuthenticateWeak == BiometricManager.BIOMETRIC_SUCCESS) {
                BiometricsAvailability.Failure(resourceProvider.getString(R.string.biometric_strong_required))
            } else {
                BiometricsAvailability.Failure(resourceProvider.getString(R.string.biometric_no_hardware))
            }

        else -> BiometricsAvailability.Failure(resourceProvider.getString(R.string.biometric_unknown_error))
    }

    private suspend fun authenticateWithCrypto(
        activity: FragmentActivity,
        notifyOnAuthenticationFailure: Boolean,
    ): BiometricsAuthenticate {
        val storedCrypto = retrieveCrypto()
        val biometricData = storedCrypto.first
        val cipher = storedCrypto.second ?: return BiometricsAuthenticate.Failed(
            activity.getString(
                R.string.biometric_authentication_error
            )
        )

        val data = authenticate(
            activity = activity,
            biometryCrypto = BiometricCrypto(BiometricPrompt.CryptoObject(cipher)),
            promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.biometric_prompt_title))
                .setSubtitle(activity.getString(R.string.biometric_prompt_subtitle))
                .setAllowedAuthenticators(BIOMETRIC_STRONG)
                .setNegativeButtonText(activity.getString(R.string.generic_cancel))
                .build(),
            notifyOnAuthenticationFailure = notifyOnAuthenticationFailure
        )

        return if (data.authenticationResult != null) {
            verifyCrypto(
                context = activity,
                result = data.authenticationResult,
                biometricAuthentication = biometricData
            )
        } else if (BiometricsAuthError.entries.any { it.code == data.errorCode }) {
            BiometricsAuthenticate.Cancelled
        } else {
            BiometricsAuthenticate.Failed(
                data.errorString.toString().ifBlank {
                    activity.getString(R.string.biometric_authentication_error)
                }
            )
        }
    }

    private suspend fun retrieveCrypto(): Pair<BiometricAuthentication?, Cipher?> =
        withContext(dispatcher) {
            val biometricData = biometryStorageController.getBiometricAuthentication()
            val cipher = cryptoController.getCipher(
                encrypt = biometricData == null,
                ivBytes = biometricData?.ivString?.decodeFromPemBase64String() ?: ByteArray(0)
            )
            Pair(biometricData, cipher)
        }

    private suspend fun verifyCrypto(
        context: Context,
        result: AuthenticationResult?,
        biometricAuthentication: BiometricAuthentication?
    ): BiometricsAuthenticate = withContext(dispatcher) {
        result?.cryptoObject?.cipher?.let {
            if (biometricAuthentication == null) {
                val randomString = cryptoController.generateCodeVerifier()
                biometryStorageController.setBiometricAuthentication(
                    BiometricAuthentication(
                        randomString = randomString,
                        encryptedString = cryptoController.encryptDecrypt(
                            cipher = it,
                            byteArray = randomString.toByteArray(StandardCharsets.UTF_8)
                        ).encodeToPemBase64String().orEmpty(),
                        ivString = it.iv.encodeToPemBase64String().orEmpty()
                    )
                )
                BiometricsAuthenticate.Success
            } else {
                if (biometricAuthentication.randomString
                        .toByteArray(StandardCharsets.UTF_8)
                        .contentEquals(
                            cryptoController.encryptDecrypt(
                                cipher = it,
                                byteArray = biometricAuthentication.encryptedString
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
) {
    val hasError: Boolean get() = errorCode != -1
}