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

package eu.europa.ec.dashboardfeature.interactor

import eu.europa.ec.authenticationlogic.controller.session.SessionController
import eu.europa.ec.corelogic.controller.ResetWalletPartialState
import eu.europa.ec.corelogic.controller.WalletCoreResetController

sealed class ResetWalletInteractorPartialState {
    data object Success : ResetWalletInteractorPartialState()
    data class Failure(val errorMessage: String) : ResetWalletInteractorPartialState()
}

/** Runs the factory reset the user confirmed in Settings. */
interface ResetWalletInteractor {

    /**
     * True while the wallet is unlocked. The reset screen is reached only after the PIN or biometric
     * check; restored after the process ended, the wallet is locked and the reset does not start.
     */
    fun isSessionUnlocked(): Boolean

    suspend fun resetWallet(): ResetWalletInteractorPartialState
}

class ResetWalletInteractorImpl(
    private val walletCoreResetController: WalletCoreResetController,
    private val sessionController: SessionController,
) : ResetWalletInteractor {

    override fun isSessionUnlocked(): Boolean = sessionController.isUnlocked

    override suspend fun resetWallet(): ResetWalletInteractorPartialState =
        when (val state = walletCoreResetController.resetWallet()) {
            is ResetWalletPartialState.Success -> ResetWalletInteractorPartialState.Success
            is ResetWalletPartialState.Failure -> ResetWalletInteractorPartialState.Failure(
                errorMessage = state.errorMessage
            )
        }
}