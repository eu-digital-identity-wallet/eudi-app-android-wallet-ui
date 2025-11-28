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

package eu.europa.ec.corelogic.provider

import eu.europa.ec.corelogic.config.WalletCoreConfig
import eu.europa.ec.eudi.openid4vci.Nonce
import eu.europa.ec.eudi.wallet.provider.WalletAttestationsProvider
import eu.europa.ec.networklogic.repository.WalletAttestationRepository
import org.multipaz.securearea.KeyInfo

interface WalletCoreAttestationProvider : WalletAttestationsProvider

class WalletCoreAttestationProviderImpl(
    private val walletCoreConfig: WalletCoreConfig,
    private val walletAttestationRepository: WalletAttestationRepository
) : WalletCoreAttestationProvider {

    override suspend fun getWalletAttestation(
        keyInfo: KeyInfo
    ): Result<String> = walletAttestationRepository.getWalletAttestation(
        baseUrl = walletCoreConfig.walletProviderHost,
        keyInfo = keyInfo.publicKey.toJwk()
    )

    override suspend fun getKeyAttestation(
        keys: List<KeyInfo>,
        nonce: Nonce?
    ): Result<String> = walletAttestationRepository.getKeyAttestation(
        baseUrl = walletCoreConfig.walletProviderHost,
        keys = keys.map { it.publicKey.toJwk() },
        nonce = nonce?.value
    )
}