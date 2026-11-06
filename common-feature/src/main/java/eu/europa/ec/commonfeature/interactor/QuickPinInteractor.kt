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

package eu.europa.ec.commonfeature.interactor

import eu.europa.ec.authenticationlogic.config.AuthenticationConfig
import eu.europa.ec.authenticationlogic.controller.device.DeviceLockController
import eu.europa.ec.authenticationlogic.controller.session.SessionController
import eu.europa.ec.authenticationlogic.controller.storage.PinStorageController
import eu.europa.ec.authenticationlogic.controller.throttle.PinThrottleController
import eu.europa.ec.authenticationlogic.provider.PinLockoutState
import eu.europa.ec.authenticationlogic.secure.SecurePin
import eu.europa.ec.authenticationlogic.util.isWeakPin
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface QuickPinInteractor {
    val maxFailedPinAttempts: Int

    /**
     * The message to show when [pin] is too easy to guess (see [isWeakPin] and
     * `AuthenticationConfig.commonPins`), or null when it may be used.
     */
    fun getWeakPinError(pin: SecurePin): String?

    /**
     * What to tell the user, at setup and at each login, on a device without a screen lock, or null
     * when the device has one. Setup and login are blocked when
     * `AuthenticationConfig.requireDeviceSecure` is true, and only warned about otherwise.
     */
    fun getDeviceLockNotice(): DeviceLockNotice?

    /**
     * Stores [newPin] once it matches [initialPin] and is not too easy to guess. Creating the PIN
     * unlocks the session, as a login does.
     */
    fun setPin(newPin: SecurePin, initialPin: SecurePin): Flow<QuickPinInteractorSetPinPartialState>

    /** Replaces the stored PIN with [newPin] unless it is too easy to guess. */
    fun changePin(
        newPin: SecurePin
    ): Flow<QuickPinInteractorSetPinPartialState>

    fun isCurrentPinValid(pin: SecurePin): Flow<QuickPinInteractorPinValidPartialState>
    suspend fun hasPin(): Boolean

    suspend fun getPinLockoutState(): PinLockoutState
    suspend fun recordPinFailure(): PinLockoutState
    suspend fun resetPinThrottle()
}

class QuickPinInteractorImpl(
    private val pinStorageController: PinStorageController,
    private val resourceProvider: ResourceProvider,
    private val pinThrottleController: PinThrottleController,
    private val authenticationConfig: AuthenticationConfig,
    private val sessionController: SessionController,
    private val deviceLockController: DeviceLockController,
) : QuickPinInteractor {

    override val maxFailedPinAttempts: Int
        get() = authenticationConfig.maxFailedPinAttempts

    override suspend fun getPinLockoutState(): PinLockoutState =
        pinThrottleController.getState()

    override suspend fun recordPinFailure(): PinLockoutState =
        pinThrottleController.recordFailure()

    override suspend fun resetPinThrottle() {
        pinThrottleController.recordSuccess()
    }

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override suspend fun hasPin(): Boolean = pinStorageController.hasPin()

    override fun getWeakPinError(pin: SecurePin): String? =
        if (isWeakPin(pin, authenticationConfig.commonPins)) {
            resourceProvider.getString(R.string.quick_pin_weak)
        } else {
            null
        }

    override fun getDeviceLockNotice(): DeviceLockNotice? = when {
        deviceLockController.isDeviceSecure() -> null
        authenticationConfig.requireDeviceSecure -> DeviceLockNotice.Blocking(
            resourceProvider.getString(R.string.quick_pin_device_lock_required)
        )

        else -> DeviceLockNotice.Warning(
            resourceProvider.getString(R.string.quick_pin_device_lock_missing)
        )
    }

    override fun setPin(
        newPin: SecurePin,
        initialPin: SecurePin
    ): Flow<QuickPinInteractorSetPinPartialState> =
        flow {
            var shouldClearNewPin = true
            var shouldClearInitialPin = false
            try {
                if (!initialPin.contentEquals(newPin)) {
                    emit(
                        QuickPinInteractorSetPinPartialState.Failed(
                            resourceProvider.getString(R.string.quick_pin_non_match)
                        )
                    )
                    return@flow
                }
                getWeakPinError(newPin)?.let { error ->
                    emit(QuickPinInteractorSetPinPartialState.Failed(error))
                    return@flow
                }
                pinStorageController.setPin(newPin)
                shouldClearNewPin = false
                shouldClearInitialPin = true
                sessionController.unlock()
                emit(QuickPinInteractorSetPinPartialState.Success)
            } finally {
                if (shouldClearInitialPin) {
                    initialPin.close()
                }
                if (shouldClearNewPin) {
                    newPin.close()
                }
            }
        }.safeAsync {
            newPin.close()
            QuickPinInteractorSetPinPartialState.Failed(
                it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun changePin(
        newPin: SecurePin
    ): Flow<QuickPinInteractorSetPinPartialState> =
        flow {
            getWeakPinError(newPin)?.let { error ->
                newPin.close()
                emit(QuickPinInteractorSetPinPartialState.Failed(error))
                return@flow
            }
            pinStorageController.setPin(newPin)
            emit(QuickPinInteractorSetPinPartialState.Success)
        }.safeAsync {
            newPin.close()
            QuickPinInteractorSetPinPartialState.Failed(
                it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun isCurrentPinValid(pin: SecurePin): Flow<QuickPinInteractorPinValidPartialState> =
        flow {
            pin.use { pin ->
                if (pinStorageController.isPinValid(pin)) {
                    emit(QuickPinInteractorPinValidPartialState.Success)
                } else {
                    emit(
                        QuickPinInteractorPinValidPartialState.Failed(
                            resourceProvider.getString(
                                R.string.quick_pin_invalid_error
                            )
                        )
                    )
                }
            }
        }.safeAsync {
            pin.close()
            QuickPinInteractorPinValidPartialState.Failed(
                it.localizedMessage ?: genericErrorMsg
            )
        }
}

/**
 * What the PIN and login screens say about a missing device screen lock; [Blocking] stops the
 * setup or the login.
 */
sealed interface DeviceLockNotice {
    val message: String

    data class Warning(override val message: String) : DeviceLockNotice
    data class Blocking(override val message: String) : DeviceLockNotice
}

sealed class QuickPinInteractorSetPinPartialState {
    data object Success : QuickPinInteractorSetPinPartialState()
    data class Failed(val errorMessage: String) : QuickPinInteractorSetPinPartialState()
}

sealed class QuickPinInteractorPinValidPartialState {
    data object Success : QuickPinInteractorPinValidPartialState()
    data class Failed(val errorMessage: String) : QuickPinInteractorPinValidPartialState()
}