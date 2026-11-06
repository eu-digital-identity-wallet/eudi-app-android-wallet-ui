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

package eu.europa.ec.corelogic.provider

import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.corelogic.config.WalletCoreConfig

/**
 * The user's setting for the Digital Credential API: whether the wallet offers its documents to
 * other apps and websites through Android's credential registry. On by default. While it is off,
 * `WalletCoreDCAPIRegistrationController` keeps the registry empty and the wallet declines requests
 * that arrive through the API.
 */
interface DCAPISettingProvider {

    /**
     * True when the build enables the Digital Credential API (`configureDCAPI { withEnabled(true) }`
     * in `WalletCoreConfig`). The user's setting is offered only then.
     */
    val isAvailable: Boolean

    suspend fun isEnabled(): Boolean
    suspend fun setEnabled(enabled: Boolean)
}

class DCAPISettingProviderImpl(
    private val prefKeys: PrefKeys,
    private val walletCoreConfig: WalletCoreConfig,
) : DCAPISettingProvider {

    override val isAvailable: Boolean
        get() = walletCoreConfig.config.dcapiConfig?.enabled == true

    override suspend fun isEnabled(): Boolean = prefKeys.getDCAPIEnabled()

    override suspend fun setEnabled(enabled: Boolean) {
        prefKeys.setDCAPIEnabled(value = enabled)
    }
}