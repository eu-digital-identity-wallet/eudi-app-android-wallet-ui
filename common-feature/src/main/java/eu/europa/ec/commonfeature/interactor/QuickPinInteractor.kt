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

package eu.europa.ec.commonfeature.interactor

import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.businesslogic.validator.FormValidator
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface QuickPinInteractor : FormValidator {
    fun setPin(newPin: String, initialPin: String): Flow<QuickPinInteractorSetPinPartialState>
    fun changePin(
        newPin: String
    ): Flow<QuickPinInteractorSetPinPartialState>

    fun isCurrentPinValid(pin: String): Flow<QuickPinInteractorPinValidPartialState>
    fun isPinMatched(
        currentPin: String,
        newPin: String
    ): Flow<QuickPinInteractorPinValidPartialState>

    fun hasPin(): Boolean
}

class QuickPinInteractorImpl constructor(
    private val formValidator: FormValidator,
    private val prefKeys: PrefKeys,
    private val resourceProvider: ResourceProvider,
) : FormValidator by formValidator, QuickPinInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun hasPin(): Boolean = prefKeys.getDevicePin().isNotBlank()

    override fun setPin(
        newPin: String,
        initialPin: String
    ): Flow<QuickPinInteractorSetPinPartialState> =
        flow {
            isPinMatched(initialPin, newPin).collect {
                when (it) {
                    is QuickPinInteractorPinValidPartialState.Failed -> {
                        emit(
                            QuickPinInteractorSetPinPartialState.Failed(
                                resourceProvider.getString(R.string.quick_pin_non_match)
                            )
                        )
                    }

                    is QuickPinInteractorPinValidPartialState.Success -> {
                        prefKeys.setDevicePin(newPin)
                        emit(QuickPinInteractorSetPinPartialState.Success)
                    }
                }
            }
        }.safeAsync {
            QuickPinInteractorSetPinPartialState.Failed(
                it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun changePin(
        newPin: String
    ): Flow<QuickPinInteractorSetPinPartialState> =
        flow {
            prefKeys.setDevicePin(newPin)
            emit(QuickPinInteractorSetPinPartialState.Success)
        }.safeAsync {
            QuickPinInteractorSetPinPartialState.Failed(
                it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun isCurrentPinValid(pin: String): Flow<QuickPinInteractorPinValidPartialState> =
        flow {
            if (prefKeys.getDevicePin() == pin) {
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
        }.safeAsync {
            QuickPinInteractorPinValidPartialState.Failed(
                it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun isPinMatched(
        currentPin: String,
        newPin: String
    ): Flow<QuickPinInteractorPinValidPartialState> =
        flow {
            if (currentPin == newPin) {
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
        }.safeAsync {
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