/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.proximityfeature.interactor

import android.content.Context
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAvailability
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractor
import eu.europa.ec.corelogic.controller.SendRequestedDocumentsPartialState
import eu.europa.ec.corelogic.controller.WalletCorePartialState
import eu.europa.ec.corelogic.controller.WalletCorePresentationController
import eu.europa.ec.corelogic.model.AuthenticationData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

sealed class ProximityLoadingObserveResponsePartialState {
    data class UserAuthenticationRequired(
        val authenticationData: List<AuthenticationData>,
    ) : ProximityLoadingObserveResponsePartialState()

    data class Failure(val error: String) : ProximityLoadingObserveResponsePartialState()
    data object Success : ProximityLoadingObserveResponsePartialState()
    data object RequestReadyToBeSent : ProximityLoadingObserveResponsePartialState()
}

sealed class ProximityLoadingSendRequestedDocumentPartialState {
    data class Failure(val error: String) : ProximityLoadingSendRequestedDocumentPartialState()
    data object Success : ProximityLoadingSendRequestedDocumentPartialState()
}

interface ProximityLoadingInteractor {
    fun observeResponse(): Flow<ProximityLoadingObserveResponsePartialState>
    fun sendRequestedDocuments(): ProximityLoadingSendRequestedDocumentPartialState
    fun handleUserAuthentication(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult,
    )
}

class ProximityLoadingInteractorImpl(
    private val walletCorePresentationController: WalletCorePresentationController,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
) : ProximityLoadingInteractor {

    override fun observeResponse(): Flow<ProximityLoadingObserveResponsePartialState> =
        walletCorePresentationController.observeSentDocumentsRequest().mapNotNull { response ->
            when (response) {
                is WalletCorePartialState.Failure -> ProximityLoadingObserveResponsePartialState.Failure(
                    error = response.error
                )

                is WalletCorePartialState.Redirect -> null

                is WalletCorePartialState.Success -> ProximityLoadingObserveResponsePartialState.Success
                is WalletCorePartialState.UserAuthenticationRequired -> {
                    ProximityLoadingObserveResponsePartialState.UserAuthenticationRequired(
                        response.authenticationData
                    )
                }

                is WalletCorePartialState.RequestIsReadyToBeSent -> ProximityLoadingObserveResponsePartialState.RequestReadyToBeSent
            }
        }

    override fun handleUserAuthentication(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult,
    ) {
        deviceAuthenticationInteractor.getBiometricsAvailability {
            when (it) {
                is BiometricsAvailability.CanAuthenticate -> {
                    deviceAuthenticationInteractor.authenticateWithBiometrics(
                        context = context,
                        crypto = crypto,
                        notifyOnAuthenticationFailure = notifyOnAuthenticationFailure,
                        resultHandler = resultHandler
                    )
                }

                is BiometricsAvailability.NonEnrolled -> {
                    deviceAuthenticationInteractor.launchBiometricSystemScreen()
                }

                is BiometricsAvailability.Failure -> {
                    resultHandler.onAuthenticationFailure()
                }
            }
        }
    }

    override fun sendRequestedDocuments(): ProximityLoadingSendRequestedDocumentPartialState {
        return when (val result = walletCorePresentationController.sendRequestedDocuments()) {
            is SendRequestedDocumentsPartialState.RequestSent -> ProximityLoadingSendRequestedDocumentPartialState.Success
            is SendRequestedDocumentsPartialState.Failure -> ProximityLoadingSendRequestedDocumentPartialState.Failure(
                result.error
            )
        }
    }
}