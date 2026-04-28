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

import eu.europa.ec.authenticationlogic.controller.storage.PinStorageController
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.businesslogic.model.SecurePin
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface QuickPinInteractor {
    fun setPin(newPin: SecurePin, initialPin: SecurePin): Flow<QuickPinInteractorSetPinPartialState>

    fun changePin(
        newPin: SecurePin
    ): Flow<QuickPinInteractorSetPinPartialState>

    fun isCurrentPinValid(pin: SecurePin): Flow<QuickPinInteractorPinValidPartialState>
    suspend fun hasPin(): Boolean
}

class QuickPinInteractorImpl(
    private val pinStorageController: PinStorageController,
    private val resourceProvider: ResourceProvider,
) : QuickPinInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override suspend fun hasPin(): Boolean = pinStorageController.hasPin()

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
                pinStorageController.setPin(newPin)
                shouldClearNewPin = false
                shouldClearInitialPin = true
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

sealed class QuickPinInteractorSetPinPartialState {
    data object Success : QuickPinInteractorSetPinPartialState()
    data class Failed(val errorMessage: String) : QuickPinInteractorSetPinPartialState()
}

sealed class QuickPinInteractorPinValidPartialState {
    data object Success : QuickPinInteractorPinValidPartialState()
    data class Failed(val errorMessage: String) : QuickPinInteractorPinValidPartialState()
}