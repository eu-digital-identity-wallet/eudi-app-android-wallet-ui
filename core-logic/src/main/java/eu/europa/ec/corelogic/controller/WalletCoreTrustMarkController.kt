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

package eu.europa.ec.corelogic.controller

import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.corelogic.di.WalletCoreScope
import eu.europa.ec.corelogic.di.getOrCreateKoinScope
import eu.europa.ec.corelogic.extension.getLocalizedText
import eu.europa.ec.corelogic.model.TrustMarkDomain
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.resourceslogic.provider.ResourceProvider

interface WalletCoreTrustMarkController {
    suspend fun getTrustMark(): Result<TrustMarkDomain>
}

class WalletCoreTrustMarkControllerImpl(
    private val prefKeys: PrefKeys,
    private val resourceProvider: ResourceProvider,
) : WalletCoreTrustMarkController {

    override suspend fun getTrustMark(): Result<TrustMarkDomain> = runCatching {
        val sessionId = prefKeys.getSessionId()
        if (sessionId.isBlank()) {
            return Result.failure(IllegalStateException("Missing wallet session"))
        }
        val wallet = getOrCreateKoinScope<WalletCoreScope>(sessionId).get<EudiWallet>()
        val manager = wallet.trustMarkManager
            ?: return Result.failure(IllegalStateException("Missing Trust Mark manager"))
        val trustMark = manager.getTrustMark().getOrThrow()
        TrustMarkDomain(
            resourceUrl = trustMark.information.trustMarkResourceURL,
            imageName = trustMark.resource.image.name,
            imageUrl = trustMark.resource.image.url,
            localisedText = trustMark.resource.text.getLocalizedText(
                userLocale = resourceProvider.getLocale()
            ),
            certifiedWalletsUrl = trustMark.information.listOfCertifiedWalletsURL,
            walletSolutionUrl = trustMark.information.walletSolutionInfoPageURL,
        )
    }
}