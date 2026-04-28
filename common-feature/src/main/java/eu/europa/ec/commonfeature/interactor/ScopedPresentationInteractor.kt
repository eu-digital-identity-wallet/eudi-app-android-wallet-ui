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

import eu.europa.ec.corelogic.controller.WalletCorePresentationController
import eu.europa.ec.corelogic.di.WalletPresentationScope
import eu.europa.ec.corelogic.di.getOrCreateKoinScope

interface ScopedPresentationInteractor {
    val presentationScopeId: String
    fun setScopeId(scopeId: String)
}

abstract class ScopedPresentationInteractorDelegate(
    walletCorePresentationController: WalletCorePresentationController? = null
) : ScopedPresentationInteractor {

    override var presentationScopeId: String = "DefaultPresentationScopeId"

    private var _walletCorePresentationController: WalletCorePresentationController? =
        walletCorePresentationController

    protected val walletCorePresentationController: WalletCorePresentationController
        get() = _walletCorePresentationController
            ?: getOrCreateKoinScope<WalletPresentationScope>(presentationScopeId).get<WalletCorePresentationController>()
                .also {
                    _walletCorePresentationController = it
                }

    override fun setScopeId(scopeId: String) {
        presentationScopeId = scopeId
    }
}