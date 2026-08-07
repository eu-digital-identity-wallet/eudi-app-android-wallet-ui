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

/**
 * The setting behind the registration-certificate feature. Both flavors feed it to Wallet Core's
 * issuer and relying-party registration policies, so it decides in one move whether registrations
 * are evaluated at all — issuers and verifiers alike.
 *
 * Read once per process, where the wallet configuration is built: Wallet Core reads both policies
 * while creating its managers, so a change applies on the next app start. Enforcement must consult
 * `WalletCoreConfig.isRegistrationCheckEnabled` — the value the configuration was actually built
 * with — and never this provider, which reports a flip Wallet Core has not seen yet.
 */
interface RegistrationCheckProvider {
    suspend fun isEnabled(): Boolean
    suspend fun setEnabled(enabled: Boolean)
}

class RegistrationCheckProviderImpl(
    private val prefKeys: PrefKeys,
) : RegistrationCheckProvider {

    override suspend fun isEnabled(): Boolean {
        return prefKeys.getRegistrationCheckEnabled()
    }

    override suspend fun setEnabled(enabled: Boolean) {
        prefKeys.setRegistrationCheckEnabled(value = enabled)
    }
}