/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
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
    fun setPin(newPin: String): Flow<QuickPinInteractorSetPinPartialState>
    fun changePin(
        currentPin: String,
        newPin: String
    ): Flow<QuickPinInteractorSetPinPartialState>

    fun isCurrentPinValid(pin: String): Flow<QuickPinInteractorPinValidPartialState>
}

class QuickPinInteractorImpl constructor(
    private val formValidator: FormValidator,
    private val prefKeys: PrefKeys,
    private val resourceProvider: ResourceProvider,
) : FormValidator by formValidator, QuickPinInteractor {

    override fun setPin(newPin: String): Flow<QuickPinInteractorSetPinPartialState> =
        flow {
            prefKeys.setDevicePin(newPin)
            emit(QuickPinInteractorSetPinPartialState.Success)
        }.safeAsync {
            QuickPinInteractorSetPinPartialState.Failed(
                it.localizedMessage ?: resourceProvider.genericErrorMessage()
            )
        }

    override fun changePin(
        currentPin: String,
        newPin: String
    ): Flow<QuickPinInteractorSetPinPartialState> =
        flow {
            isCurrentPinValid(currentPin).collect {
                when (it) {
                    is QuickPinInteractorPinValidPartialState.Failed -> {
                        emit(
                            QuickPinInteractorSetPinPartialState.Failed(
                                errorMessage = it.errorMessage
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
                it.localizedMessage ?: resourceProvider.genericErrorMessage()
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
                it.localizedMessage ?: resourceProvider.genericErrorMessage()
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